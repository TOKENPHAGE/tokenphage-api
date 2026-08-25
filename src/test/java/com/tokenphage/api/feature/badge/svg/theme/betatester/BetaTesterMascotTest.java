package com.tokenphage.api.feature.badge.svg.theme.betatester;

import static org.assertj.core.api.Assertions.assertThat;

import com.tokenphage.api.feature.badge.svg.BadgeMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 픽셀 파지 마스코트의 무결성(개수·대칭·배치)과 모드별 글로우를 검증한다.
 * <p>
 * rect 블록은 프로토타입에서 추출한 문자열 상수라 컴파일러가 손상을 못 잡는다. 픽셀 격자로 대조한다.
 */
class BetaTesterMascotTest {

    /** 프로토타입 실측 rect 개수. */
    private static final int PIXEL_RECT_COUNT = 42;

    /** 픽셀 격자 폭·높이 (프로토타입 실측 16×14). */
    private static final int GRID_W = 16;
    private static final int GRID_H = 14;

    private static final Pattern RECT = Pattern.compile(
            "<rect x=\"(\\d+)\" y=\"(\\d+)\" width=\"(\\d+)\" height=\"(\\d+)\" fill=\"(#[0-9A-Fa-f]{6})\"/>");

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(token, idx)) != -1) {
            count++;
            idx += token.length();
        }
        return count;
    }

    /**
     * 렌더된 rect들을 픽셀 격자에 칠한다. 뒤에 나오는 rect가 앞을 덮는다(음영 픽셀 처리와 동일).
     */
    private static String[][] paintGrid(String svg) {
        String[][] grid = new String[GRID_H][GRID_W];
        Matcher m = RECT.matcher(svg);
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

    @Nested
    @DisplayName("픽셀 무결성")
    class PixelIntegrity {

        @Test
        @DisplayName("마스코트_렌더_rect42개")
        void 마스코트_렌더_rect42개() {
            // given

            // when
            String svg = BetaTesterMascot.render(BadgeMode.CYAN);

            // then
            assertThat(countOccurrences(svg, "<rect ")).isEqualTo(PIXEL_RECT_COUNT);
        }

        @Test
        @DisplayName("마스코트_렌더_실측배치변환")
        void 마스코트_렌더_실측배치변환() {
            // given
            // 프로토타입 실측 translate(11,42)에서 상단 여백 정리로 12 올림

            // when
            String svg = BetaTesterMascot.render(BadgeMode.CYAN);

            // then
            assertThat(svg).contains("translate(11,30) scale(3.8125)");
        }

        @Test
        @DisplayName("마스코트_픽셀_좌우대칭")
        void 마스코트_픽셀_좌우대칭() {
            // given
            // 파지는 좌우 대칭 도형이다. (x,y)와 (15-x,y)의 색이 다르면 전사 오류다.

            // when
            String[][] grid = paintGrid(BetaTesterMascot.render(BadgeMode.CYAN));

            // then
            for (int y = 0; y < GRID_H; y++) {
                for (int x = 0; x < GRID_W / 2; x++) {
                    assertThat(grid[y][x])
                            .as("픽셀 (%d,%d) ↔ (%d,%d) 대칭", x, y, GRID_W - 1 - x, y)
                            .isEqualTo(grid[y][GRID_W - 1 - x]);
                }
            }
        }

        @Test
        @DisplayName("마스코트_픽셀_본체와음영두색만사용")
        void 마스코트_픽셀_본체와음영두색만사용() {
            // given
            // 프로토타입 팔레트: 본체 #F5F5F7, 음영(눈) #1E1E1E

            // when
            String svg = BetaTesterMascot.render(BadgeMode.CYAN);

            // then
            Matcher m = RECT.matcher(svg);
            while (m.find()) {
                assertThat(m.group(5)).isIn("#F5F5F7", "#1E1E1E");
            }
        }
    }

    @Nested
    @DisplayName("모드별 글로우")
    class Glow {

        @Test
        @DisplayName("글로우_cyan_이중그림자")
        void 글로우_cyan_이중그림자() {
            // given

            // when
            String svg = BetaTesterMascot.render(BadgeMode.CYAN);

            // then
            assertThat(svg).contains(BetaTesterColors.CYAN.mascotGlow());
            assertThat(countOccurrences(svg, "drop-shadow(")).isEqualTo(2);
        }

        @Test
        @DisplayName("글로우_green_이중그림자")
        void 글로우_green_이중그림자() {
            // given

            // when
            String svg = BetaTesterMascot.render(BadgeMode.GREEN);

            // then
            assertThat(svg).contains(BetaTesterColors.GREEN.mascotGlow());
            assertThat(countOccurrences(svg, "drop-shadow(")).isEqualTo(2);
        }

        @Test
        @DisplayName("글로우_purple_이중그림자")
        void 글로우_purple_이중그림자() {
            // given

            // when
            String svg = BetaTesterMascot.render(BadgeMode.PURPLE);

            // then
            assertThat(svg).contains(BetaTesterColors.PURPLE.mascotGlow());
            assertThat(countOccurrences(svg, "drop-shadow(")).isEqualTo(2);
        }

        @Test
        @DisplayName("글로우_적용위치_transform바깥그룹")
        void 글로우_적용위치_transform바깥그룹() {
            // given
            // 같은 그룹에 걸면 scale(3.8125)이 반경에 곱해져 사이드바 칸을 넘는다.

            // when
            String svg = BetaTesterMascot.render(BadgeMode.CYAN);

            // then
            assertThat(svg).startsWith("<g style=\"filter:");
            assertThat(svg.indexOf("filter:")).isLessThan(svg.indexOf("transform="));
        }

        @Test
        @DisplayName("글로우_비지원모드_cyan폴백")
        void 글로우_비지원모드_cyan폴백() {
            // given
            // 악센트 3종 밖의 모드가 직접 전달되면 기본(cyan) 팔레트로 접는다 (경계).

            // when
            String svg = BetaTesterMascot.render(BadgeMode.LIGHT);

            // then
            assertThat(svg).contains(BetaTesterColors.CYAN.mascotGlow());
        }
    }
}
