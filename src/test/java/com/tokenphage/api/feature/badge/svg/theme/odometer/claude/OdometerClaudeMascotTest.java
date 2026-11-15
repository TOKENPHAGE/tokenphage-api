package com.tokenphage.api.feature.badge.svg.theme.odometer.claude;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주행거리계 Claude 마스코트의 픽셀 무결성(대칭·반전·개수·색)과 모션 시간표를 검증한다.
 * <p>
 * 프레임 rect는 프로토타입에서 추출한 문자열 상수라 컴파일러가 손상을 못 잡는다.
 * 픽셀 전체를 기대값으로 두면 같은 출처를 두 번 적는 것이라, 도형이 지켜야 할 불변식으로 대조한다.
 * 렌더 결과에는 몸통 3종과 눈 3종이 겹쳐 있어 한 격자에 칠하면 서로 덮으므로 프레임 상수별로 칠한다.
 */
class OdometerClaudeMascotTest {

    /** 픽셀 격자 폭·높이 (프로토타입 실측 18×16). */
    private static final int GRID_W = 18;
    private static final int GRID_H = 16;

    /** 반짝이는 몸통 밖(음수 좌표)에 있어 -를 허용한다. */
    private static final Pattern RECT = Pattern.compile(
            "<rect x=\"(-?\\d+)\" y=\"(-?\\d+)\" width=\"(\\d+)\" height=\"(\\d+)\" fill=\"(#[0-9A-Fa-f]{6})\"/>");

    private static final String BODY = "#D97757";
    private static final String OUTLINE = "#FFFFFF";
    private static final String EYE = "#1A0F0A";

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }

    private static String sparkles() {
        return String.join("", OdometerClaudeMascot.SPARKLES);
    }

    /** 프레임 하나의 rect들을 18×16 격자에 칠한다. 격자를 벗어나는 좌표가 있으면 예외로 드러난다. */
    private static String[][] paintGrid(String frame) {
        String[][] grid = new String[GRID_H][GRID_W];
        Matcher m = RECT.matcher(frame);
        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            int w = Integer.parseInt(m.group(3));
            int h = Integer.parseInt(m.group(4));
            for (int dy = 0; dy < h; dy++) {
                for (int dx = 0; dx < w; dx++) {
                    grid[y + dy][x + dx] = m.group(5);
                }
            }
        }
        return grid;
    }

    /** 격자를 좌우로 뒤집는다: (x, y) → (17 - x, y). */
    private static String[][] mirror(String[][] grid) {
        String[][] mirrored = new String[GRID_H][GRID_W];
        for (int y = 0; y < GRID_H; y++) {
            for (int x = 0; x < GRID_W; x++) {
                mirrored[y][GRID_W - 1 - x] = grid[y][x];
            }
        }
        return mirrored;
    }

    private static Set<String> colorsOf(String frame) {
        return RECT.matcher(frame).results().map(r -> r.group(5)).collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("픽셀 무결성")
    class PixelIntegrity {

        @Test
        @DisplayName("프레임_일곱상수_프로토타입실측rect개수")
        void 프레임_일곱상수_프로토타입실측rect개수() {
            // given
            // 서 있는 몸통 62, 달리기 A·B 59, 뜬 눈 4, 감은 눈 2, > < 눈 10, 반짝이 3묶음 9

            // when
            // then
            assertThat(countOccurrences(OdometerClaudeMascot.STAND, "<rect ")).isEqualTo(62);
            assertThat(countOccurrences(OdometerClaudeMascot.RUN_A, "<rect ")).isEqualTo(59);
            assertThat(countOccurrences(OdometerClaudeMascot.RUN_B, "<rect ")).isEqualTo(59);
            assertThat(countOccurrences(OdometerClaudeMascot.EYES_OPEN, "<rect ")).isEqualTo(4);
            assertThat(countOccurrences(OdometerClaudeMascot.EYES_CLOSED, "<rect ")).isEqualTo(2);
            assertThat(countOccurrences(OdometerClaudeMascot.EYES_JOY, "<rect ")).isEqualTo(10);
            assertThat(OdometerClaudeMascot.SPARKLES).hasSize(3)
                    .allSatisfy(sparkle -> assertThat(countOccurrences(sparkle, "<rect ")).isEqualTo(3));
        }

        @Test
        @DisplayName("프레임_일곱상수_정해진색만사용")
        void 프레임_일곱상수_정해진색만사용() {
            // given
            // 정규식이 못 읽는 rect(손상)가 섞이면 개수가 맞아도 색 집합이 어긋난다.

            // when
            // then
            assertThat(colorsOf(OdometerClaudeMascot.STAND)).containsExactlyInAnyOrder(BODY, OUTLINE);
            assertThat(colorsOf(OdometerClaudeMascot.RUN_A)).containsExactlyInAnyOrder(BODY, OUTLINE);
            assertThat(colorsOf(OdometerClaudeMascot.RUN_B)).containsExactlyInAnyOrder(BODY, OUTLINE);
            assertThat(colorsOf(OdometerClaudeMascot.EYES_OPEN)).containsExactly(EYE);
            assertThat(colorsOf(OdometerClaudeMascot.EYES_CLOSED)).containsExactly(EYE);
            assertThat(colorsOf(OdometerClaudeMascot.EYES_JOY)).containsExactly(EYE);
            assertThat(colorsOf(sparkles())).containsExactlyInAnyOrder("#FFD234", "#FFF6B0");
            assertThat(RECT.matcher(sparkles()).results().count()).isEqualTo(9);
        }

        @Test
        @DisplayName("픽셀_서있는몸통과눈세종류_각각좌우대칭")
        void 픽셀_서있는몸통과눈세종류_각각좌우대칭() {
            // given
            // 마스코트는 좌우 대칭 도형이다. (x, y)와 (17 - x, y)의 색이 다르면 추출 오류다.
            Map<String, String> frames = Map.of(
                    "STAND", OdometerClaudeMascot.STAND,
                    "EYES_OPEN", OdometerClaudeMascot.EYES_OPEN,
                    "EYES_CLOSED", OdometerClaudeMascot.EYES_CLOSED,
                    "EYES_JOY", OdometerClaudeMascot.EYES_JOY);

            // when
            // then
            frames.forEach((name, frame) -> {
                String[][] grid = paintGrid(frame);
                assertThat(Arrays.stream(grid).flatMap(Arrays::stream).anyMatch(cell -> cell != null))
                        .as("%s에 칠해진 픽셀이 있다", name).isTrue();
                assertThat(grid).as("%s 좌우 대칭", name).isDeepEqualTo(mirror(grid));
            });
        }

        @Test
        @DisplayName("픽셀_달리기AB_각자비대칭이고서로좌우반전")
        void 픽셀_달리기AB_각자비대칭이고서로좌우반전() {
            // given
            // 달리기는 한 발씩 내딛는 모습이라 프레임 하나만 보면 대칭이 아니다. 전사 오류로 보고 고치면 안 된다.
            String[][] runA = paintGrid(OdometerClaudeMascot.RUN_A);
            String[][] runB = paintGrid(OdometerClaudeMascot.RUN_B);

            // when
            String[][] mirroredB = mirror(runB);

            // then
            assertThat(runA).isDeepEqualTo(mirroredB);
            assertThat(Arrays.deepEquals(runA, mirror(runA))).as("달리기 A는 좌우 비대칭").isFalse();
        }
    }

    @Nested
    @DisplayName("render() - 배치와 모션 시간표")
    class Motion {

        @Test
        @DisplayName("렌더_호출_실측배치에픽셀205개")
        void 렌더_호출_실측배치에픽셀205개() {
            // given
            // 62 + 59 + 59 + 4 + 2 + 10 + 9

            // when
            String svg = OdometerClaudeMascot.render();

            // then
            assertThat(svg).startsWith("<g transform=\"translate(12 16) scale(2)\" shape-rendering=\"crispEdges\">");
            assertThat(countOccurrences(svg, "<rect ")).isEqualTo(205);
        }

        @Test
        @DisplayName("렌더_애니메이션없는환경_서있는몸통과뜬눈만보임")
        void 렌더_애니메이션없는환경_서있는몸통과뜬눈만보임() {
            // given
            // 숨겨 두는 것은 달리기 A·B, 감은 눈, > < 눈 네 묶음뿐이다.

            // when
            String svg = OdometerClaudeMascot.render();

            // then
            assertThat(countOccurrences(svg, "<g visibility=\"hidden\">")).isEqualTo(4);
            assertThat(svg)
                    .contains("<g><set attributeName=\"visibility\" to=\"hidden\" begin=\"0s\" dur=\"2.6s\"/>" + OdometerClaudeMascot.STAND + "</g>")
                    .contains("repeatCount=\"indefinite\"/>" + OdometerClaudeMascot.EYES_OPEN + "</g>");
        }

        @Test
        @DisplayName("시간표_0에서2.6초_달리기프레임교대와감은눈")
        void 시간표_0에서2점6초_달리기프레임교대와감은눈() {
            // given
            // 0.2초 주기 13회 = 2.6초. 그동안 서 있는 몸통과 뜬 눈을 숨긴다.

            // when
            String svg = OdometerClaudeMascot.render();

            // then
            assertThat(svg)
                    .contains("<animate attributeName=\"visibility\" calcMode=\"discrete\" values=\"visible;hidden\" dur=\"0.2s\" repeatCount=\"13\"/>" + OdometerClaudeMascot.RUN_A)
                    .contains("<animate attributeName=\"visibility\" calcMode=\"discrete\" values=\"hidden;visible\" dur=\"0.2s\" repeatCount=\"13\"/>" + OdometerClaudeMascot.RUN_B)
                    .contains("<animateTransform attributeName=\"transform\" type=\"translate\" calcMode=\"discrete\" values=\"0 0;0 -1\" dur=\"0.2s\" repeatCount=\"13\"/>")
                    .contains("<g visibility=\"hidden\"><set attributeName=\"visibility\" to=\"visible\" begin=\"0s\" dur=\"2.6s\"/>");
            assertThat(countOccurrences(svg, "<set attributeName=\"visibility\" to=\"hidden\" begin=\"0s\" dur=\"2.6s\"/>")).isEqualTo(2);
        }

        @Test
        @DisplayName("시간표_3.1초_기울기를풀고점프하며눈이기쁜모양")
        void 시간표_3점1초_기울기를풀고점프하며눈이기쁜모양() {
            // given
            // 발(y=16)을 축으로 앞으로 기울어 달리다 3.1초에 바로 서고, 0.4초 점프한다. > < 눈은 3.1~4.5초.

            // when
            String svg = OdometerClaudeMascot.render();

            // then
            assertThat(svg)
                    .contains("<g transform=\"translate(0 16)\"><g><animateTransform attributeName=\"transform\" type=\"skewX\""
                            + " values=\"-14;-14;9;0\" keyTimes=\"0;.84;.92;1\" dur=\"3.1s\"/><g transform=\"translate(0 -16)\">")
                    .contains("<animateTransform attributeName=\"transform\" type=\"translate\" values=\"0 0;0 -4;0 0\" keyTimes=\"0;.45;1\""
                            + " calcMode=\"spline\" keySplines=\".2 .7 .4 1;.6 0 .8 .4\" begin=\"3.1s\" dur=\"0.4s\"/>")
                    .contains("<set attributeName=\"visibility\" to=\"hidden\" begin=\"3.1s\" dur=\"1.4s\"/>")
                    .contains("<g visibility=\"hidden\"><set attributeName=\"visibility\" to=\"visible\" begin=\"3.1s\" dur=\"1.4s\"/>" + OdometerClaudeMascot.EYES_JOY + "</g>");
        }

        @Test
        @DisplayName("시간표_점프순간_반짝이셋이0.09초간격")
        void 시간표_점프순간_반짝이셋이0점09초간격() {
            // given

            // when
            String svg = OdometerClaudeMascot.render();

            // then
            String sparkle = "<g opacity=\"0\"><animate attributeName=\"opacity\" values=\"0;1;1;0\" keyTimes=\"0;.2;.6;1\" dur=\"0.7s\" begin=\"%s\"/>%s</g>";
            assertThat(svg)
                    .contains(sparkle.formatted("3.10s", OdometerClaudeMascot.SPARKLES.get(0)))
                    .contains(sparkle.formatted("3.19s", OdometerClaudeMascot.SPARKLES.get(1)))
                    .endsWith(sparkle.formatted("3.28s", OdometerClaudeMascot.SPARKLES.get(2)) + "</g>");
        }

        @Test
        @DisplayName("시간표_멈춘뒤_숨쉬기와곁눈질과깜빡임무한반복")
        void 시간표_멈춘뒤_숨쉬기와곁눈질과깜빡임무한반복() {
            // given
            // 숨쉬기 3.6초~, 곁눈질 4초~, 깜빡임 4.5초~ (> < 눈이 끝나는 시각)

            // when
            String svg = OdometerClaudeMascot.render();

            // then
            assertThat(svg)
                    .contains("<animateTransform attributeName=\"transform\" type=\"translate\" calcMode=\"discrete\" values=\"0 0;0 -1\""
                            + " dur=\"1.6s\" begin=\"3.6s\" repeatCount=\"indefinite\"/>")
                    .contains("<animateTransform attributeName=\"transform\" type=\"translate\" calcMode=\"discrete\" values=\"0 0;1 0;0 0\""
                            + " keyTimes=\"0;.7;.9\" dur=\"5s\" begin=\"4s\" repeatCount=\"indefinite\"/>")
                    .contains("<animate attributeName=\"visibility\" calcMode=\"discrete\" values=\"visible;hidden;visible\""
                            + " keyTimes=\"0;.93;.97\" dur=\"4.2s\" begin=\"4.50s\" repeatCount=\"indefinite\"/>")
                    .contains("<animate attributeName=\"visibility\" calcMode=\"discrete\" values=\"hidden;visible;hidden\""
                            + " keyTimes=\"0;.93;.97\" dur=\"4.2s\" begin=\"4.50s\" repeatCount=\"indefinite\"/>" + OdometerClaudeMascot.EYES_CLOSED + "</g>");
        }
    }
}
