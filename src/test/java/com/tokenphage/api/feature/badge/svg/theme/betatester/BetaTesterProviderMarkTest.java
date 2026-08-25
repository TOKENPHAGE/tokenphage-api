package com.tokenphage.api.feature.badge.svg.theme.betatester;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 사용 모델 로고 조합·배치를 검증한다.
 * <p>
 * 로고 표시는 스냅샷 boolean 두 개로만 결정된다. path d 시작점 문자열로 어느 로고인지 구분한다.
 */
class BetaTesterProviderMarkTest {

    /** Claude 로고 path d 시작점 (프로토타입 실측). */
    private static final String CLAUDE_MARK_HEAD = "M4.709";

    /** OpenAI 로고 path d 시작점 (프로토타입 실측). */
    private static final String OPENAI_MARK_HEAD = "M22.2819";

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }

    @Nested
    @DisplayName("로고 조합")
    class Combination {

        @Test
        @DisplayName("로고_claude만_클로드로고만")
        void 로고_claude만_클로드로고만() {
            // given

            // when
            String svg = BetaTesterProviderMark.render(true, false, BetaTesterColors.CYAN);

            // then
            assertThat(svg).contains(CLAUDE_MARK_HEAD).doesNotContain(OPENAI_MARK_HEAD);
        }

        @Test
        @DisplayName("로고_gpt만_오픈AI로고만")
        void 로고_gpt만_오픈AI로고만() {
            // given

            // when
            String svg = BetaTesterProviderMark.render(false, true, BetaTesterColors.CYAN);

            // then
            assertThat(svg).contains(OPENAI_MARK_HEAD).doesNotContain(CLAUDE_MARK_HEAD);
        }

        @Test
        @DisplayName("로고_둘다_로고2개")
        void 로고_둘다_로고2개() {
            // given

            // when
            String svg = BetaTesterProviderMark.render(true, true, BetaTesterColors.CYAN);

            // then
            assertThat(countOccurrences(svg, "<path ")).isEqualTo(2);
            assertThat(svg).contains(CLAUDE_MARK_HEAD).contains(OPENAI_MARK_HEAD);
        }

        @Test
        @DisplayName("로고_둘다false_로고없음")
        void 로고_둘다false_로고없음() {
            // given
            // 판정 규칙상 gpt/claude 문자열이 없는 모델만 쓴 사용자는 로고가 빈다 (경계).

            // when
            String svg = BetaTesterProviderMark.render(false, false, BetaTesterColors.CYAN);

            // then
            assertThat(svg).isEmpty();
        }
    }

    @Nested
    @DisplayName("로고 배치")
    class Placement {

        @Test
        @DisplayName("로고배치_2개_실측좌표")
        void 로고배치_2개_실측좌표() {
            // given
            // 프로토타입 실측: Claude 31.5 / OpenAI 44.5, y=88, scale 0.3333

            // when
            String svg = BetaTesterProviderMark.render(true, true, BetaTesterColors.CYAN);

            // then
            assertThat(svg)
                    .contains("translate(31.5,88) scale(0.3333)")
                    .contains("translate(44.5,88) scale(0.3333)");
        }

        @Test
        @DisplayName("로고배치_1개_중심정렬x38")
        void 로고배치_1개_중심정렬x38() {
            // given
            // 로고 폭 8px(24 × 0.3333) 기준 중심 42 유지 → x=38

            // when
            String claudeOnly = BetaTesterProviderMark.render(true, false, BetaTesterColors.CYAN);
            String gptOnly = BetaTesterProviderMark.render(false, true, BetaTesterColors.CYAN);

            // then
            assertThat(claudeOnly).contains("translate(38,88) scale(0.3333)");
            assertThat(gptOnly).contains("translate(38,88) scale(0.3333)");
        }
    }

    @Nested
    @DisplayName("로고 색")
    class Fill {

        @Test
        @DisplayName("로고색_claude_브랜드색고정")
        void 로고색_claude_브랜드색고정() {
            // given
            // Claude 로고는 모드와 무관하게 브랜드색이다.

            // when
            String cyan = BetaTesterProviderMark.render(true, false, BetaTesterColors.CYAN);
            String green = BetaTesterProviderMark.render(true, false, BetaTesterColors.GREEN);

            // then
            assertThat(cyan).contains("fill=\"#D97757\"");
            assertThat(green).contains("fill=\"#D97757\"");
        }

        @Test
        @DisplayName("로고색_openai_텍스트주색")
        void 로고색_openai_텍스트주색() {
            // given

            // when
            String svg = BetaTesterProviderMark.render(false, true, BetaTesterColors.CYAN);

            // then
            assertThat(svg).contains("fill=\"" + BetaTesterColors.CYAN.textPrimary() + "\"");
        }
    }
}
