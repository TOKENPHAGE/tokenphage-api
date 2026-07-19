package com.tokenphage.api.feature.badge.svg.theme.card.claude;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CardClaudeBadgeThemeTest {

    private final CardClaudeBadgeTheme theme = new CardClaudeBadgeTheme();

    private BadgeResponse dataWith(long totalTokens) {
        List<DailyCountResponse> daily30d = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            daily30d.add(new DailyCountResponse("2026-05-" + (i + 1), i == 29 ? 19000 : 1000));
        }
        List<ModelCountResponse> models = List.of(
            new ModelCountResponse("claude-sonnet-4-6", 1_200_000),
            new ModelCountResponse("claude-opus-4-7", 450_000));
        return new BadgeResponse("leeyoungseok", totalTokens, daily30d, models, 0.87, 0L, 0, List.of());
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

    private static final String CRUMB_KEY = "keyTimes=\"0;0.25;0.45;0.7;1\"";
    private static final String SPARKLE_KEY = "keyTimes=\"0;0.75;0.78;0.88;0.94\"";

    @Test
    @DisplayName("name()은 'claude'를 반환한다")
    void name_isClaude() {
        assertThat(theme.name()).isEqualTo("claude");
    }

    @Nested
    @DisplayName("공통 레이아웃 재사용")
    class Layout {

        @Test
        @DisplayName("배지 골격·30 히트바·Top5·30일 레이블 포함")
        void reusesBaseLayout() {
            String svg = theme.build(dataWith(5_000_000L), false);
            assertThat(svg)
                .contains("width=\"540\"").contains("</svg>")
                .contains("TOKEN 누적").contains("Top 5 Models").contains("최근 30일")
                .contains("aria-label=\"TokenBadge leeyoungseok\"");
            assertThat(count(svg, "rx=\"2\"")).isEqualTo(30);
        }

        @Test
        @DisplayName("마스코트 픽셀 group에 crispEdges가 적용된다(안티앨리어싱 seam 방지)")
        void mascotUsesCrispEdges() {
            // 1×1 픽셀 rect들이 안티앨리어싱으로 가는 틈(격자무늬)을 만들지 않도록 crispEdges가 걸려야 한다.
            String svg = theme.build(dataWith(50_000L), false);
            assertThat(svg).contains("shape-rendering=\"crispEdges\"");
        }
    }

    @Nested
    @DisplayName("누적 토큰 → 활동 레벨 분기")
    class Levels {

        @Test
        @DisplayName("Lv1(5만): 글로우 없음, chew 4.80s, 팝업 없음, 스파클 3")
        void lv1() {
            String svg = theme.build(dataWith(50_000L), false);
            assertThat(svg).doesNotContain("<filter").contains("4.80s")
                .doesNotContain("plus-pop-group");
            assertThat(count(svg, SPARKLE_KEY)).isEqualTo(3);
            assertThat(count(svg, CRUMB_KEY)).isZero();
        }

        @Test
        @DisplayName("Lv2(5천만): glow 0.67/#D97757, chew 3.40s, 부스러기 0, 스파클 4")
        void lv2() {
            String svg = theme.build(dataWith(50_000_000L), false);
            assertThat(svg).contains("stdDeviation=\"0.67\"")
                .contains("flood-color=\"#D97757\"").contains("3.40s")
                .doesNotContain("plus-pop-group");
            assertThat(count(svg, CRUMB_KEY)).isZero();
            assertThat(count(svg, SPARKLE_KEY)).isEqualTo(4);
        }

        @Test
        @DisplayName("Lv3(2억): glow 1.67, chew 2.40s, 부스러기 3, 스파클 6")
        void lv3() {
            String svg = theme.build(dataWith(200_000_000L), false);
            assertThat(svg).contains("stdDeviation=\"1.67\"").contains("2.40s");
            assertThat(count(svg, CRUMB_KEY)).isEqualTo(3);
            assertThat(count(svg, SPARKLE_KEY)).isEqualTo(6);
        }

        @Test
        @DisplayName("Lv4(7억): glow 2.67/#ff6133, chew 1.60s, 팝업 있음, 스파클 8, 부스러기 5")
        void lv4() {
            String svg = theme.build(dataWith(700_000_000L), false);
            assertThat(svg).contains("stdDeviation=\"2.67\"")
                .contains("flood-color=\"#ff6133\"").contains("1.60s")
                .contains("plus-pop-group");
            assertThat(count(svg, SPARKLE_KEY)).isEqualTo(8);
            assertThat(count(svg, CRUMB_KEY)).isEqualTo(5);
        }

        @Test
        @DisplayName("Lv5(20억): glow 4.00/#ff2d6b, 몸색 #ff5c47, chew 1.10s, 스파클 10, 부스러기 8")
        void lv5() {
            String svg = theme.build(dataWith(2_000_000_000L), false);
            assertThat(svg).contains("stdDeviation=\"4.00\"")
                .contains("flood-color=\"#ff2d6b\"").contains("#ff5c47").contains("1.10s")
                .contains("plus-pop-group");
            assertThat(count(svg, SPARKLE_KEY)).isEqualTo(10);
            assertThat(count(svg, CRUMB_KEY)).isEqualTo(8);
        }
    }

    @Nested
    @DisplayName("팝업·캡션 실데이터")
    class FactsAndPop {

        @Test
        @DisplayName("Lv4 팝업이 최근일 토큰(19000)을 +19.0K Tk로 표시")
        void popUsesRecentDayTokens() {
            assertThat(theme.build(dataWith(700_000_000L), false)).contains("+19.0K Tk");
        }

        @Test
        @DisplayName("2억(Lv3) → 'Crime and Punishment ...권 갉아먹는 중' 캡션 + 토큰 그라데이션")
        void caption_and_gradient_lv3() {
            String svg = theme.build(dataWith(200_000_000L), false);
            assertThat(svg).contains("Crime and Punishment").contains("권 ")
                .contains("url(#totalGrad-light-3)")
                .contains("stop-color=\"#FFC85E\"");
        }

        @Test
        @DisplayName("5만(Lv1) → 'The Little Prince ...권 갉아먹는 중' 캡션, 코드네임/LV. 미노출")
        void caption_lv1() {
            String svg = theme.build(dataWith(50_000L), false);
            assertThat(svg).contains("The Little Prince").contains("권 ")
                .doesNotContain("LV.").doesNotContain("DOZING").doesNotContain("BERSERK");
        }

        @Test
        @DisplayName("히트바가 오렌지 팔레트(#8a3a1f 등)를 사용")
        void heatbar_orangePalette() {
            String svg = theme.build(dataWith(5_000_000L), false);
            assertThat(svg).contains("fill=\"#8a3a1f\"");
        }
    }
}
