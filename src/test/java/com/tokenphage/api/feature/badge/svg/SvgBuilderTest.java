package com.tokenphage.api.feature.badge.svg;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import com.tokenphage.api.feature.badge.svg.theme.ClaudeBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.GpuBadgeTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SvgBuilderTest {

    private SvgBuilder svgBuilder;

    @BeforeEach
    void setUp() {
        // gpu + claude 두 테마를 등록 (실제 @Component 빈 주입과 동일한 구성)
        svgBuilder = new SvgBuilder(List.of(new GpuBadgeTheme(), new ClaudeBadgeTheme()));
    }

    private BadgeResponse sampleData() {
        LocalDate today = LocalDate.now();
        long[] daily = {
            5000, 12000, 0, 8000, 23000, 45000, 3000, 0, 7000, 15000,
            0, 30000, 12000, 8000, 0, 50000, 22000, 4000, 18000, 9000,
            0, 35000, 11000, 6000, 25000, 0, 14000, 42000, 7000, 19000
        };
        List<DailyCountResponse> heatbar = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            heatbar.add(new DailyCountResponse(today.minusDays(i).toString(), daily[29 - i]));
        }
        List<ModelCountResponse> topModels = List.of(
            new ModelCountResponse("claude-sonnet-4-6", 1_200_000),
            new ModelCountResponse("claude-opus-4-7",     450_000)
        );
        return new BadgeResponse("leeyoungseok", 15_430_000L, heatbar, topModels, 0.87);
    }

    private static int count(String text, String token) {
        int c = 0;
        int i = 0;
        while ((i = text.indexOf(token, i)) != -1) {
            c++;
            i += token.length();
        }
        return c;
    }

    @Nested @DisplayName("뱃지 링크 (모든 테마·모드 공통 <a> 앵커)")
    class BadgeLink {

        // 링크 래핑은 BaseBadgeTheme.build 한 곳(공유 코드)에 있으므로 테마별로 나누지 않고
        // 디스패처를 통해 모든 테마·모드 조합을 한 번에 검증한다("모든 뱃지 공통" 요구사항).
        private static final String REPO_URL = "https://github.com/TOKENPHAGE/tokenphage-api";

        @ParameterizedTest(name = "theme={0}, mode={1} → 저장소 링크 <a> 앵커 1개")
        @DisplayName("모든 테마·모드의 뱃지는 저장소 URL <a> 앵커로 정확히 한 번 감싸진다")
        @CsvSource({ "gpu, light", "gpu, dark", "claude, light", "claude, dark" })
        void badge_모든테마모드_저장소앵커로감쌈(String theme, String mode) {
            // given / when
            String svg = svgBuilder.build(sampleData(), theme, mode);
            // then
            assertThat(svg)
                .contains("<a href=\"" + REPO_URL + "\"")
                .contains("xlink:href=\"" + REPO_URL + "\"")
                .contains("</a>");
            assertThat(count(svg, "<a ")).isEqualTo(1);
            assertThat(count(svg, "</a>")).isEqualTo(1);
        }
    }

    @Nested @DisplayName("디스패처 동작")
    class Dispatch {

        @Test @DisplayName("gpu + light 모드 → SVG 정상 생성")
        void dispatch_gpuTheme_lightMode() {
            String svg = svgBuilder.build(sampleData(), "gpu", "light");
            assertThat(svg).contains("<svg ").contains("</svg>");
        }

        @Test @DisplayName("gpu + dark 모드 → 다크 배경 포함")
        void dispatch_gpuTheme_darkMode() {
            String svg = svgBuilder.build(sampleData(), "gpu", "dark");
            assertThat(svg).contains("#0f172a");
        }

        @Test @DisplayName("알 수 없는 theme → 기본 테마(gpu)로 fallback")
        void dispatch_unknownTheme_fallbackToDefault() {
            String svg = svgBuilder.build(sampleData(), "unknown-theme", "light");
            assertThat(svg).contains("<svg ").contains("</svg>");
        }

        @Test @DisplayName("mode 대소문자 무관 — 'DARK'도 다크 모드로 처리")
        void dispatch_modeUppercase_darkApplied() {
            String svg = svgBuilder.build(sampleData(), "gpu", "DARK");
            assertThat(svg).contains("#0f172a");
        }
    }

    @Nested @DisplayName("formatTokens (정적 유틸)")
    class FormatTokens {

        @ParameterizedTest
        @DisplayName("토큰 단위 변환 — K / M / B")
        @CsvSource({
            "0,           0",        "999,         999",
            "1000,        1.0K",     "1500,        1.5K",
            "999000,      999.0K",   "1000000,     1.0M",
            "15430000,    15.4M",    "1000000000,  1.0B",
            "2000000000,  2.0B"
        })
        void formatTokens_correctUnit(long tokens, String expected) {
            assertThat(SvgBuilder.formatTokens(tokens)).isEqualTo(expected);
        }
    }

    @Nested @DisplayName("normalizeTheme / normalizeMode (캐시 키 정규화)")
    class Normalize {

        @Test @DisplayName("등록된 테마 → 소문자 식별자 그대로 반환")
        void normalizeTheme_등록된테마_식별자반환() {
            assertThat(svgBuilder.normalizeTheme("gpu")).isEqualTo("gpu");
            assertThat(svgBuilder.normalizeTheme("claude")).isEqualTo("claude");
        }

        @Test @DisplayName("대소문자 혼합 → 소문자로 정규화")
        void normalizeTheme_대소문자혼합_소문자정규화() {
            assertThat(svgBuilder.normalizeTheme("GPU")).isEqualTo("gpu");
            assertThat(svgBuilder.normalizeTheme("Claude")).isEqualTo("claude");
        }

        @Test @DisplayName("미등록 테마 → 기본 테마(gpu)로 폴백")
        void normalizeTheme_미등록테마_기본테마폴백() {
            assertThat(svgBuilder.normalizeTheme("zzz999")).isEqualTo("gpu");
        }

        @Test @DisplayName("null theme → 기본 테마(gpu)")
        void normalizeTheme_null_기본테마() {
            assertThat(svgBuilder.normalizeTheme(null)).isEqualTo("gpu");
        }

        @ParameterizedTest(name = "mode=\"{0}\" → \"{1}\"")
        @DisplayName("dark는 대소문자 무관하게 dark, 그 외/null은 light")
        @CsvSource(value = {
            "dark, dark", "DARK, dark", "Dark, dark",
            "light, light", "garbage, light", "'', light", "NULL, light"
        }, nullValues = "NULL")
        void normalizeMode_정규화(String input, String expected) {
            assertThat(svgBuilder.normalizeMode(input)).isEqualTo(expected);
        }
    }
}
