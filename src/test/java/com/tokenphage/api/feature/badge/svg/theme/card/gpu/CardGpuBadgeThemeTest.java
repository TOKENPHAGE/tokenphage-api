package com.tokenphage.api.feature.badge.svg.theme.card.gpu;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.SvgText;
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

class CardGpuBadgeThemeTest {

    private CardGpuBadgeTheme gpuTheme;

    @BeforeEach
    void setUp() {
        gpuTheme = new CardGpuBadgeTheme();
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
            new ModelCountResponse("claude-sonnet-4-6",          1_200_000),
            new ModelCountResponse("claude-opus-4-7",              450_000),
            new ModelCountResponse("claude-haiku-4-5-20251001",    300_000),
            new ModelCountResponse("claude-sonnet-4-5",            120_000),
            new ModelCountResponse("gpt-4o",                        80_000)
        );
        return new BadgeResponse("leeyoungseok", 15_430_000L, daily30d, topModels, 0.87, 0L, 0, List.of(), "");
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }

    @Nested @DisplayName("SVG 기본 구조")
    class SvgStructure {

        @Test @DisplayName("라이트 모드 — SVG 루트 태그 및 540×210 크기 포함")
        void lightMode_svgTagWithCorrectDimensions() {
            String svg = gpuTheme.build(sampleData(), BadgeMode.LIGHT);
            assertThat(svg).contains("<svg ").contains("</svg>")
                .contains("width=\"540\"").contains("height=\"210\"");
        }

        @Test @DisplayName("다크 모드 — SVG 정상 생성")
        void darkMode_svgGenerated() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.DARK)).contains("<svg ").contains("</svg>");
        }

        @Test @DisplayName("라이트 모드 — 흰색 배경 (#ffffff)")
        void lightMode_whiteBackground() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("#ffffff");
        }

        @Test @DisplayName("다크 모드 — 다크 배경 (#0f172a)")
        void darkMode_darkBackground() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.DARK)).contains("#0f172a");
        }

        @Test @DisplayName("aria-label에 사용자명 포함 (접근성)")
        void svg_ariaLabelContainsUsername() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT))
                .contains("aria-label=\"TokenBadge leeyoungseok\"");
        }

        @Test @DisplayName("가로/세로 구분선 포함")
        void svg_containsDividers() {
            String svg = gpuTheme.build(sampleData(), BadgeMode.LIGHT);
            assertThat(svg).contains("x1=\"20\" y1=\"76\"")
                .contains("x1=\"280\"").contains("x2=\"280\"");
        }
    }

    @Nested @DisplayName("사용자 정보 렌더링")
    class UserInfo {

        @Test @DisplayName("사용자명이 @ 접두사와 함께 SVG에 포함됨")
        void username_renderedInSvg() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("@leeyoungseok");
        }

        @Test @DisplayName("HTML 특수문자 사용자명 → 이스케이프 (XSS 방어)")
        void username_htmlEscaped() {
            BadgeResponse data = new BadgeResponse("<script>xss</script>", 0L, List.of(), List.of(), 0.0, 0L, 0, List.of(), "");
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            assertThat(svg).doesNotContain("<script>").contains("&lt;script&gt;");
        }

        @Test @DisplayName("앰퍼샌드 포함 사용자명 → &amp; 이스케이프")
        void username_ampersandEscaped() {
            BadgeResponse data = new BadgeResponse("user&dev", 0L, List.of(), List.of(), 0.0, 0L, 0, List.of(), "");
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            assertThat(svg).doesNotContain("user&dev").contains("user&amp;dev");
        }
    }

    @Nested @DisplayName("토큰 수 포맷 (formatTokens)")
    class TokenFormatting {

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
            assertThat(SvgText.formatTokens(tokens)).isEqualTo(expected);
        }

        @Test @DisplayName("총 토큰 15.4M이 SVG에 렌더링됨")
        void totalTokens_renderedAsMega() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("15.4M");
        }

        @Test @DisplayName("'tokens' 단위 레이블 포함")
        void tokensUnitLabel_present() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("tokens");
        }

        @Test @DisplayName("'TOKEN 누적' 섹션 레이블 포함")
        void tokenSectionLabel_present() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("TOKEN 누적");
        }
    }

    @Nested @DisplayName("히트바 (최근 30일)")
    class Heatbar {

        @Test @DisplayName("히트바 막대가 정확히 30개 (rx=2 기준)")
        void heatbar_exactly30Rects() {
            assertThat(countOccurrences(gpuTheme.build(sampleData(), BadgeMode.LIGHT), "rx=\"2\"")).isEqualTo(30);
        }

        @Test @DisplayName("'최근 30일' 레이블 포함")
        void heatbar_30daysLabel() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("최근 30일");
        }

        @Test @DisplayName("데이터 없어도 막대 30개 생성 (all min-height)")
        void emptyHeatbar_still30Rects() {
            BadgeResponse data = new BadgeResponse("newuser", 0L, List.of(), List.of(), 0.0, 0L, 0, List.of(), "");
            assertThat(countOccurrences(gpuTheme.build(data, BadgeMode.LIGHT), "rx=\"2\"")).isEqualTo(30);
        }

        @Test @DisplayName("히트바 translate 그룹 포함 (왼쪽 패널 기준선 정렬)")
        void heatbar_containsTranslateGroup() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("translate(30, 164)");
        }
    }

    @Nested @DisplayName("Top 5 모델 렌더링")
    class TopModels {

        @Test @DisplayName("'Top 5 Models' 섹션 레이블 포함")
        void topModels_sectionLabel() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.LIGHT)).contains("Top 5 Models");
        }

        @Test @DisplayName("모델명에서 claude- 접두사 제거됨")
        void modelName_claudePrefixStripped() {
            String svg = gpuTheme.build(sampleData(), BadgeMode.LIGHT);
            assertThat(svg).contains("sonnet-4-6").contains("opus-4-7");
        }

        @Test @DisplayName("모델명에서 -latest 접미사 제거됨")
        void modelName_latestSuffixStripped() {
            BadgeResponse data = new BadgeResponse("u", 100_000L, List.of(),
                List.of(new ModelCountResponse("claude-sonnet-latest", 100_000)), 0.0, 0L, 0, List.of(), "");
            assertThat(gpuTheme.build(data, BadgeMode.LIGHT)).doesNotContain("-latest");
        }

        @Test @DisplayName("모델 토큰이 K/M 포맷으로 표시됨")
        void modelTokens_formattedWithUnit() {
            String svg = gpuTheme.build(sampleData(), BadgeMode.LIGHT);
            assertThat(svg).contains("1.2M").contains("450.0K");
        }

        @Test @DisplayName("5개 미만 모델 — 빈 슬롯은 '--' 표시")
        void fewerThan5Models_emptySlotsDashDash() {
            BadgeResponse data = new BadgeResponse("u", 100_000L, List.of(),
                List.of(new ModelCountResponse("claude-sonnet-4-6", 100_000)), 0.0, 0L, 0, List.of(), "");
            assertThat(gpuTheme.build(data, BadgeMode.LIGHT)).contains("--");
        }

        @Test @DisplayName("항상 5행 렌더링 — 순위 배지 5개 포함")
        void always5RankBadges() {
            BadgeResponse data = new BadgeResponse("u", 0L, List.of(), List.of(), 0.0, 0L, 0, List.of(), "");
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            int circles = 0, idx = 0;
            while ((idx = svg.indexOf("<circle", idx)) != -1) { circles++; idx++; }
            assertThat(circles).isEqualTo(5);
        }
    }

    @Nested @DisplayName("캐시 적중률 렌더링")
    class CacheHitRate {

        @Test @DisplayName("87% 적중률 → 'CACHE HIT RATE' 레이블 및 '87.0%' 표시")
        void highRate_renderedGreen() {
            String svg = gpuTheme.build(sampleData(), BadgeMode.LIGHT);
            assertThat(svg).contains("CACHE HIT RATE").contains("87.0%").contains("#16a34a");
        }

        @Test @DisplayName("0% 적중률 → '0.0%' 표시 (회색)")
        void zeroRate_renderedGray() {
            BadgeResponse data = new BadgeResponse("u", 0L, List.of(), List.of(), 0.0, 0L, 0, List.of(), "");
            assertThat(gpuTheme.build(data, BadgeMode.LIGHT)).contains("0.0%");
        }

        @Test @DisplayName("중간 적중률(50%) → 주황색 (#d97706) 표시")
        void midRate_renderedOrange() {
            BadgeResponse data = new BadgeResponse("u", 0L, List.of(), List.of(), 0.5, 0L, 0, List.of(), "");
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            assertThat(svg).contains("50.0%").contains("#d97706");
        }

        @Test @DisplayName("다크 모드 고적중률 → 초록색 (#4ade80)")
        void darkHighRate_renderedGreen() {
            assertThat(gpuTheme.build(sampleData(), BadgeMode.DARK)).contains("#4ade80");
        }
    }

    @Nested @DisplayName("GPU 마스코트 레벨링")
    class GpuMascotTest {

        // 레벨 분기를 타도록 누적 토큰만 지정한 최소 데이터 (캐시율 0.5 → 캐시색 #d97706, 마스코트 색과 비충돌)
        private BadgeResponse levelData(long totalTokens) {
            return new BadgeResponse("u", totalTokens, List.of(), List.of(), 0.5, 0L, 0, List.of(), "");
        }

        @Test @DisplayName("애니메이션 요소(animateTransform, repeatCount) 포함")
        void mascot_containsAnimationElements() {
            // given / when
            String svg = gpuTheme.build(sampleData(), BadgeMode.LIGHT);
            // then
            assertThat(svg).contains("animateTransform").contains("repeatCount=\"indefinite\"");
        }

        @Test @DisplayName("Lv.1(<10M) — 글로우·번개 없고 팬이 가장 느림(1.3s), 녹색(#16a34a)")
        void mascotLv1_noGlowNoSparkSlowFanGreen() {
            // given
            BadgeResponse data = levelData(5_000_000L);
            // when
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            // then
            assertThat(svg)
                .doesNotContain("glow-gpu-l1")
                .doesNotContain("gpu-spark")
                .contains("dur=\"1.3s\"")
                .contains("#16a34a");
        }

        @Test @DisplayName("Lv.3(<500M) — 황록(#65a30d), 팬 0.75s, 글로우 존재, 번개 없음")
        void mascotLv3_yellowGreenMidFanGlowNoSpark() {
            // given
            BadgeResponse data = levelData(300_000_000L);
            // when
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            // then
            assertThat(svg)
                .contains("#65a30d")
                .contains("dur=\"0.75s\"")
                .contains("glow-gpu-l3")
                .doesNotContain("gpu-spark");
        }

        @Test @DisplayName("Lv.4(<1B) — 주황(#ea580c), 번개 1개, 글로우 존재")
        void mascotLv4_orangeOneSparkGlow() {
            // given
            BadgeResponse data = levelData(800_000_000L);
            // when
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            // then
            assertThat(svg).contains("#ea580c").contains("glow-gpu-l4");
            assertThat(countOccurrences(svg, "gpu-spark")).isEqualTo(1);
        }

        @Test @DisplayName("Lv.5(≥1B) — 빨강(#dc2626), 팬 가장 빠름(0.3s), 글로우, 번개 2개")
        void mascotLv5_redFastFanGlowTwoSparks() {
            // given
            BadgeResponse data = levelData(5_000_000_000L);
            // when
            String svg = gpuTheme.build(data, BadgeMode.LIGHT);
            // then
            assertThat(svg)
                .contains("#dc2626")
                .contains("dur=\"0.3s\"")
                .contains("glow-gpu-l5");
            assertThat(countOccurrences(svg, "gpu-spark")).isEqualTo(2);
        }

        @ParameterizedTest @DisplayName("누적 토큰 → 팬 회전 dur 매핑 (레벨↑ 가속)")
        @CsvSource({
            "5000000,    1.3s",
            "50000000,   1.0s",
            "300000000,  0.75s",
            "800000000,  0.5s",
            "5000000000, 0.3s"
        })
        void mascot_fanDurByLevel(long totalTokens, String expectedDur) {
            // given / when
            String svg = gpuTheme.build(levelData(totalTokens), BadgeMode.LIGHT);
            // then
            assertThat(svg).contains("dur=\"" + expectedDur + "\"");
        }

        @Test @DisplayName("열파 입자는 레벨이 오를수록 늘어난다 (Lv.1=1개 < Lv.5=5개)")
        void mascot_heatParticlesIncreaseWithLevel() {
            // given / when
            int lv1 = countOccurrences(gpuTheme.build(levelData(5_000_000L), BadgeMode.LIGHT), "gpu-heat");
            int lv5 = countOccurrences(gpuTheme.build(levelData(5_000_000_000L), BadgeMode.LIGHT), "gpu-heat");
            // then
            assertThat(lv1).isEqualTo(1);
            assertThat(lv5).isEqualTo(5);
        }
    }

    @Nested @DisplayName("GPU 테마 식별자")
    class ThemeName {

        @Test @DisplayName("name()은 'gpu'를 반환한다")
        void name_returnsGpu() {
            assertThat(gpuTheme.name()).isEqualTo("gpu");
        }
    }

}
