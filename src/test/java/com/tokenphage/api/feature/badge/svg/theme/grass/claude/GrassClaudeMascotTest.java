package com.tokenphage.api.feature.badge.svg.theme.grass.claude;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GrassClaudeMascot 단위 테스트")
class GrassClaudeMascotTest {

    private static final String SPRITE_MARKER =
            "<g transform=\"translate(-13.0,0) scale(1.444)\" shape-rendering=\"crispEdges\">";

    @Nested
    @DisplayName("결정적 렌더 — 걷기 애니메이션")
    class DeterministicWalk {

        @Test
        @DisplayName("마스코트_결정적웨이포인트_translate값정확")
        void 마스코트_결정적웨이포인트_translate값정확() {
            // given: 400→500→450→(귀환 400), 구간 거리 100/50/50 → 등속 keyTimes 0/0.5/0.75/1
            int[] waypoints = {400, 500, 450};
            // when
            String svg = GrassClaudeMascot.render(false, waypoints);
            // then: values는 "x 35" 쌍(공중부양 방지), keyTimes는 거리 비례 등속
            assertThat(svg)
                    .contains("type=\"translate\"")
                    .contains("values=\"400 35;500 35;450 35;400 35\"")
                    .contains("keyTimes=\"0;0.5000;0.7500;1\"")
                    .contains("dur=\"30s\"")
                    .contains("repeatCount=\"indefinite\"");
        }

        @Test
        @DisplayName("마스코트_좌향이동_scale마이너스1")
        void 마스코트_좌향이동_scale마이너스1() {
            // given: 우향(400→500) 후 좌향(500→450→400)
            int[] waypoints = {400, 500, 450};
            // when
            String svg = GrassClaudeMascot.render(false, waypoints);
            // then: 방향 플립이 구간별 discrete scale로 동기화된다
            assertThat(svg)
                    .contains("type=\"scale\"")
                    .contains("calcMode=\"discrete\"")
                    .contains("values=\"1 1;-1 1;-1 1;-1 1\"");
        }

        @Test
        @DisplayName("마스코트_정적초기위치_첫웨이포인트")
        void 마스코트_정적초기위치_첫웨이포인트() {
            // given
            int[] waypoints = {400, 500, 450};
            // when
            String svg = GrassClaudeMascot.render(false, waypoints);
            // then: SMIL 미동작 환경 대비 정적 transform이 첫 웨이포인트에 있다
            assertThat(svg).contains("<g transform=\"translate(400,35)\">");
        }

        @Test
        @DisplayName("마스코트_웨이포인트부족_예외")
        void 마스코트_웨이포인트부족_예외() {
            // given // when // then
            assertThatThrownBy(() -> GrassClaudeMascot.render(false, new int[]{400}))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("마스코트_연속중복웨이포인트_예외")
        void 마스코트_연속중복웨이포인트_예외() {
            // given: 0길이 구간은 keyTimes 중복을 만들므로 거부한다
            // when // then
            assertThatThrownBy(() -> GrassClaudeMascot.render(false, new int[]{400, 400, 500}))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("랜덤 렌더")
    class RandomWalk {

        @Test
        @DisplayName("마스코트_랜덤렌더_필수SMIL요소포함")
        void 마스코트_랜덤렌더_필수SMIL요소포함() {
            // given // when: 랜덤 경로여도 구조 불변식은 항상 성립해야 한다
            for (int i = 0; i < 20; i++) {
                String svg = GrassClaudeMascot.render(false);
                // then
                assertThat(svg)
                        .contains("type=\"translate\"")
                        .contains("type=\"scale\"")
                        .contains("dur=\"30s\"")
                        .contains("repeatCount=\"indefinite\"")
                        .contains(SPRITE_MARKER);
                assertWaypointsInRange(svg);
            }
        }

        private void assertWaypointsInRange(String svg) {
            // 걷기 translate("x 35;x 35;...")의 모든 x가 이동 범위(338..652) 안이어야 한다
            // (바닥 토큰 opacity values="1;0;0"와 구분하려고 " 35" 쌍만 매칭)
            Matcher m = Pattern.compile("values=\"((?:\\d+ 35;?)+)\"").matcher(svg);
            assertThat(m.find()).isTrue();
            for (String pair : m.group(1).split(";")) {
                int x = Integer.parseInt(pair.trim().split(" ")[0]);
                assertThat(x).isBetween(338, 652);
            }
        }
    }

    @Nested
    @DisplayName("스프라이트 fidelity — 토큰 무는 합성")
    class SpriteFidelity {

        /** 바닥 금화 토큰 4색 (card CardClaudeMascot.SPRITE_RECTS 유래). */
        private static final String[] TOKEN_COLORS = {"#7C5A0E", "#C99110", "#FFD234", "#FFF6B0"};

        @Test
        @DisplayName("마스코트_얼굴스프라이트_rect수256")
        void 마스코트_얼굴스프라이트_rect수256() {
            // given // when: 토큰 없이 걷는 얼굴만 (바닥 토큰은 별도)
            String svg = GrassClaudeMascot.render(false, new int[]{400, 500});
            // then: 얼굴 256개 (1x1 픽셀), 입에 문 토큰 없음
            assertThat(countOccurrences(svg, "<rect")).isEqualTo(256);
        }

        @Test
        @DisplayName("마스코트_바닥토큰_금화색포함")
        void 마스코트_바닥토큰_금화색포함() {
            // given // when: 바닥 토큰 1개 배치
            String svg = GrassClaudeMascot.render(false, new int[]{400, 600}, new int[]{500});
            // then: 바닥 코인의 금화 4색이 모두 존재한다
            assertThat(svg).contains(TOKEN_COLORS);
        }
    }

    @Nested
    @DisplayName("바닥 토큰 먹기")
    class EatTokens {

        @Test
        @DisplayName("마스코트_토큰통과_사라짐애니메이션")
        void 마스코트_토큰통과_사라짐애니메이션() {
            // given: 400↔600 경로 중간(500) 토큰 → 통과 시점 0.25에 사라짐
            String svg = GrassClaudeMascot.render(false, new int[]{400, 600}, new int[]{500});
            // then: 500은 한 바퀴에 두 번 통과(0.25, 0.75). 입이 닿는 순간(통과-PHASE)에 사라지고 +5초에 재생성
            //       → opacity 1→0→1→0→1 (0.237 사라짐 / 0.4037 재생성 / 0.737 사라짐 / 0.9037 재생성)
            assertThat(svg)
                    .contains("values=\"1;0;1;0;1;1\"")
                    .contains("keyTimes=\"0;0.2370;0.4037;0.7370;0.9037;1\"")
                    .contains("calcMode=\"discrete\"");
        }

        @Test
        @DisplayName("마스코트_먹을때_입벌림오버레이")
        void 마스코트_먹을때_입벌림오버레이() {
            // given // when: 토큰이 있으면 먹는 순간 입벌림 오버레이가 붙는다
            String svg = GrassClaudeMascot.render(false, new int[]{400, 600}, new int[]{500});
            String noToken = GrassClaudeMascot.render(false, new int[]{400, 600}, new int[0]);
            // then: 입벌림 오버레이(opacity 펄스)는 토큰이 있을 때만 존재
            assertThat(svg).contains("<g opacity=\"0\"><animate attributeName=\"opacity\"");
            assertThat(noToken).doesNotContain("<g opacity=\"0\"><animate attributeName=\"opacity\"");
        }

        @Test
        @DisplayName("마스코트_경로밖토큰_안사라짐")
        void 마스코트_경로밖토큰_안사라짐() {
            // given: 경로(400..600) 밖 토큰(700)은 통과하지 않아 계속 남는다
            String svg = GrassClaudeMascot.render(false, new int[]{400, 600}, new int[]{700});
            // then: 먹기·재생성·오버레이 애니메이션이 전혀 없다 (통과하지 않으므로)
            assertThat(svg).doesNotContain("attributeName=\"opacity\"");
        }
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
}
