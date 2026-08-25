package com.tokenphage.api.feature.badge.svg;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import com.tokenphage.api.feature.badge.svg.theme.betatester.BetaTesterBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.card.claude.CardClaudeBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.card.gpu.CardGpuBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.grass.claude.GrassClaudeBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.locked.LockedBadgeTheme;
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
        // 등록 테마 5종 전부 (실제 @Component 빈 주입과 동일한 구성)
        // 미등록 시 normalizeTheme·resolveMode가 gpu로 폴백해 신규 케이스가 공허 통과하므로 반드시 포함한다.
        svgBuilder = new SvgBuilder(List.of(
                new CardGpuBadgeTheme(), new CardClaudeBadgeTheme(), new GrassClaudeBadgeTheme(),
                new BetaTesterBadgeTheme(), new LockedBadgeTheme()));
    }

    private BadgeResponse sampleData() {
        LocalDate today = LocalDate.now();
        long[] daily = {
            5000, 12000, 0, 8000, 23000, 45000, 3000, 0, 7000, 15000,
            0, 30000, 12000, 8000, 0, 50000, 22000, 4000, 18000, 9000,
            0, 35000, 11000, 6000, 25000, 0, 14000, 42000, 7000, 19000
        };
        List<DailyCountResponse> daily30d = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            daily30d.add(new DailyCountResponse(today.minusDays(i).toString(), daily[29 - i]));
        }
        List<ModelCountResponse> topModels = List.of(
            new ModelCountResponse("claude-sonnet-4-6", 1_200_000),
            new ModelCountResponse("claude-opus-4-7",     450_000)
        );
        return new BadgeResponse("leeyoungseok", 15_430_000L, daily30d, topModels, 0.87, 0L, 0, List.of(), "");
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

        // 링크 래핑은 카드 계열(CardBadgeTheme.build)과 잔디 계열(GrassBadgeTheme.build) 각 베이스에서
        // 뱃지 전체를 저장소 URL <a>로 감싸므로, 테마별로 나누지 않고 디스패처를 통해 모든 테마·모드 조합을 한 번에 검증한다("모든 뱃지 공통" 요구사항).
        private static final String REPO_URL = "https://github.com/TOKENPHAGE/tokenphage-api";

        @ParameterizedTest(name = "theme={0}, mode={1} → 저장소 링크 <a> 앵커 1개")
        @DisplayName("모든 테마·모드의 뱃지는 저장소 URL <a> 앵커로 정확히 한 번 감싸진다")
        @CsvSource({ "gpu, light", "gpu, dark", "claude, light", "claude, dark",
                     "grass-claude, light", "grass-claude, dark",
                     "beta-tester, cyan", "beta-tester, green", "beta-tester, purple",
                     "locked, light" })
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

    @Nested @DisplayName("normalizeTheme / resolveMode (캐시 키 정규화)")
    class Normalize {

        @Test @DisplayName("등록된 테마 → 소문자 식별자 그대로 반환")
        void normalizeTheme_등록된테마_식별자반환() {
            assertThat(svgBuilder.normalizeTheme("gpu")).isEqualTo("gpu");
            assertThat(svgBuilder.normalizeTheme("claude")).isEqualTo("claude");
            // 하이픈 포함 식별자도 정규화를 그대로 통과한다
            assertThat(svgBuilder.normalizeTheme("grass-claude")).isEqualTo("grass-claude");
            assertThat(svgBuilder.normalizeTheme("GRASS-Claude")).isEqualTo("grass-claude");
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

        @ParameterizedTest(name = "theme={0}, mode=\"{1}\" → {2}")
        @DisplayName("resolveMode는 테마별 지원 집합으로 정규화한다")
        @CsvSource(value = {
            "gpu, dark, DARK", "gpu, DARK, DARK", "gpu, Dark, DARK",
            "gpu, light, LIGHT", "gpu, garbage, LIGHT", "gpu, '', LIGHT", "gpu, NULL, LIGHT",
            "gpu, cyan, LIGHT",
            "beta-tester, cyan, CYAN", "beta-tester, green, GREEN", "beta-tester, PURPLE, PURPLE",
            "beta-tester, light, CYAN", "beta-tester, dark, CYAN", "beta-tester, NULL, CYAN"
        }, nullValues = "NULL")
        void resolveMode_테마별정규화(String theme, String input, BadgeMode expected) {
            // 카드 테마는 악센트 3색 미지원 → LIGHT, beta-tester는 light/dark 미지원 → CYAN 폴백
            assertThat(svgBuilder.resolveMode(theme, input)).isEqualTo(expected);
        }

        @Test @DisplayName("resolveMode_미등록테마_기본테마기준정규화")
        void resolveMode_미등록테마_기본테마기준정규화() {
            // 미등록 theme은 normalizeTheme과 같은 폴백(gpu) 기준으로 모드를 정한다
            assertThat(svgBuilder.resolveMode("zzz999", "dark")).isEqualTo(BadgeMode.DARK);
            assertThat(svgBuilder.resolveMode("zzz999", "green")).isEqualTo(BadgeMode.LIGHT);
        }
    }

    @Nested @DisplayName("needsOf (테마별 데이터 요구사항 조회)")
    class NeedsOf {

        @Test @DisplayName("needsOf_grass테마_연간3종반환")
        void needsOf_grass테마_연간3종반환() {
            // given // when // then
            assertThat(svgBuilder.needsOf("grass-claude")).containsExactlyInAnyOrder(
                    BadgeDataNeed.DAILY_1Y, BadgeDataNeed.STREAK_DAYS, BadgeDataNeed.YEAR_TOKENS);
        }

        @Test @DisplayName("needsOf_통계테마_기본4종반환")
        void needsOf_통계테마_기본4종반환() {
            // given // when // then: gpu/claude는 default needs (통계 4종)
            assertThat(svgBuilder.needsOf("gpu")).containsExactlyInAnyOrder(
                    BadgeDataNeed.TOTAL_TOKENS, BadgeDataNeed.DAILY_30D,
                    BadgeDataNeed.TOP_MODELS, BadgeDataNeed.CACHE_HIT_RATE);
        }

        @Test @DisplayName("needsOf_미등록테마_gpu기본폴백")
        void needsOf_미등록테마_gpu기본폴백() {
            // given // when // then: normalizeTheme 폴백 경로와 동일하게 gpu의 needs
            assertThat(svgBuilder.needsOf("zzz999"))
                    .isEqualTo(svgBuilder.needsOf("gpu"));
        }
    }
}
