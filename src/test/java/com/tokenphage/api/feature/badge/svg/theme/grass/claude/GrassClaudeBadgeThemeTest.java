package com.tokenphage.api.feature.badge.svg.theme.grass.claude;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.SvgText;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GrassClaudeBadgeTheme 단위 테스트")
class GrassClaudeBadgeThemeTest {

    /** 고정 기준일: 2026-07-15(수) → 마지막 열 셀 4개(일~수), 총 361셀. */
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);
    private static final int EXPECTED_CELLS = 51 * 7 + 3 + 1;

    private final GrassClaudeBadgeTheme theme = new GrassClaudeBadgeTheme();

    @Nested
    @DisplayName("테마 계약")
    class ThemeContract {

        @Test
        @DisplayName("잔디뱃지_테마이름_grassclaude")
        void 잔디뱃지_테마이름_grassclaude() {
            // given // when // then
            assertThat(theme.name()).isEqualTo("grass-claude");
        }

        @Test
        @DisplayName("잔디뱃지_needs선언_연간3종")
        void 잔디뱃지_needs선언_연간3종() {
            // given // when // then: grass는 연간 데이터만 요구한다 (통계 4종 미요구)
            assertThat(theme.needs()).containsExactlyInAnyOrder(
                    BadgeDataNeed.DAILY_1Y, BadgeDataNeed.STREAK_DAYS, BadgeDataNeed.YEAR_TOKENS);
        }
    }

    @Nested
    @DisplayName("SVG 기본 구조")
    class SvgStructure {

        @Test
        @DisplayName("잔디뱃지_기본구조_700x190앵커랩")
        void 잔디뱃지_기본구조_700x190앵커랩() {
            // given
            BadgeResponse data = sampleData(42, 2_543_891L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 루트는 CardBadgeTheme 미러링(xmlns:xlink 포함 — camo XML 파싱 필수)
            assertThat(svg)
                    .contains("width=\"700\" height=\"190\"")
                    .contains("xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
                    .contains("aria-label=\"Tokenphage grass lee-daily\"")
                    .contains("<a href=\"" + SvgText.LINK_URL + "\"")
                    .contains("</a>");
        }

        @Test
        @DisplayName("잔디뱃지_셀수_오늘요일까지렌더")
        void 잔디뱃지_셀수_오늘요일까지렌더() {
            // given
            BadgeResponse data = sampleData(0, 0L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 51주*7 + (수요일 4개) = 361셀, 오늘 이후 셀 미렌더
            assertThat(countOccurrences(svg, "rx=\"2.5\"")).isEqualTo(EXPECTED_CELLS);
        }

        @Test
        @DisplayName("잔디뱃지_최신날짜_마지막열좌표")
        void 잔디뱃지_최신날짜_마지막열좌표() {
            // given: 오늘(수)만 사용량 존재
            BadgeResponse data = sampleData(1, 100L, Map.of(TODAY, 100L));
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 오늘 셀 = 마지막 열(x=53+51*12=665), 수요일 행(y=88+3*12=124), 최고 레벨 색
            assertThat(svg).contains("<rect x=\"665\" y=\"124\" width=\"9\" height=\"9\" rx=\"2.5\" fill=\"#8a3a1f\"/>");
        }

        @Test
        @DisplayName("잔디뱃지_월요일라벨_표기")
        void 잔디뱃지_월요일라벨_표기() {
            // given
            BadgeResponse data = sampleData(0, 0L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 요일 라벨 3종 + 월 라벨(창 내 Aug 확정 포함)
            assertThat(svg)
                    .contains(">Mon<").contains(">Wed<").contains(">Fri<")
                    .contains(">Aug<");
        }
    }

    @Nested
    @DisplayName("잔디 팔레트")
    class GridPalette {

        @Test
        @DisplayName("잔디뱃지_라이트팔레트_hex포함")
        void 잔디뱃지_라이트팔레트_hex포함() {
            // given: 레벨 1~5를 전부 타는 분포 (max=100 → 10/30/50/70/100)
            BadgeResponse data = sampleData(1, 260L, Map.of(
                    TODAY, 100L, TODAY.minusDays(1), 70L, TODAY.minusDays(2), 50L,
                    TODAY.minusDays(3), 30L, TODAY.minusDays(4), 10L));
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then
            assertThat(svg)
                    .contains("#f6c9a8").contains("#ec9463").contains("#D97757")
                    .contains("#b8562f").contains("#8a3a1f").contains("#e9e4db");
        }

        @Test
        @DisplayName("잔디뱃지_다크팔레트_hex포함")
        void 잔디뱃지_다크팔레트_hex포함() {
            // given
            BadgeResponse data = sampleData(1, 260L, Map.of(
                    TODAY, 100L, TODAY.minusDays(1), 70L, TODAY.minusDays(2), 50L,
                    TODAY.minusDays(3), 30L, TODAY.minusDays(4), 10L));
            // when
            String svg = theme.build(data, BadgeMode.DARK);
            // then: 추출 스크립트가 좌표 매핑으로 확정한 dark 5단계 + empty
            assertThat(svg)
                    .contains("#5a2f1e").contains("#c05a30")
                    .contains("#e58a5f").contains("#f6c9a8").contains("#1c2536");
        }

        @Test
        @DisplayName("잔디뱃지_빈데이터_전부empty색")
        void 잔디뱃지_빈데이터_전부empty색() {
            // given: 신규 유저 — 365일 전부 0
            BadgeResponse data = sampleData(0, 0L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 모든 셀이 empty 색, 레벨 색 미등장 (light 장식/마스코트와 hex 비충돌 확인됨)
            assertThat(countOccurrences(svg, "fill=\"#e9e4db\"")).isEqualTo(EXPECTED_CELLS);
            assertThat(svg).doesNotContain("#f6c9a8").doesNotContain("#ec9463")
                    .doesNotContain("#b8562f");
        }
    }

    @Nested
    @DisplayName("헤더 — streak·연간 토큰·username")
    class Header {

        @Test
        @DisplayName("잔디뱃지_streak표기_Nday")
        void 잔디뱃지_streak표기_Nday() {
            // given
            BadgeResponse data = sampleData(42, 2_543_891L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 녹색 streak 텍스트
            assertThat(svg).contains(">42-day</text>").contains("#2ea043");
        }

        @Test
        @DisplayName("잔디뱃지_연간토큰_콤마포맷")
        void 잔디뱃지_연간토큰_콤마포맷() {
            // given
            BadgeResponse data = sampleData(42, 2_543_891L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 픽셀 숫자 그룹의 aria-label로 값 접근 가능(숫자는 rect로 그려짐)
            assertThat(svg).contains("aria-label=\"2,543,891 tokens/year\"");
        }

        @Test
        @DisplayName("잔디뱃지_연간토큰_픽셀숫자_큰수배율축소")
        void 잔디뱃지_연간토큰_픽셀숫자_큰수배율축소() {
            // given // when: 작은 수는 최대 배율(4.2px), 아주 큰 수(1조)는 축소되어 하늘 영역 침범 방지
            String small = theme.build(sampleData(1, 1_000L), BadgeMode.LIGHT);
            String huge = theme.build(sampleData(1, 1_000_000_000_000L), BadgeMode.LIGHT);
            // then: 값은 aria-label로 접근 가능, 큰 수는 픽셀 칸이 최대 배율보다 작다
            assertThat(small).contains("aria-label=\"1,000 tokens/year\"").contains("width=\"4.20\"");
            assertThat(huge).contains("aria-label=\"1,000,000,000,000 tokens/year\"")
                    .doesNotContain("width=\"4.20\"");
        }

        @Test
        @DisplayName("잔디뱃지_불꽃_opacity애니메이션")
        void 잔디뱃지_불꽃_opacity애니메이션() {
            // given: streak >= 1 — 주황 불꽃 + 깜빡임
            BadgeResponse data = sampleData(42, 100L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 불꽃 고유색(#ff3d00)은 light에서 불꽃에만 존재
            assertThat(svg)
                    .contains("attributeName=\"opacity\"")
                    .contains("#ff3d00");
        }

        @Test
        @DisplayName("잔디뱃지_streak0_회색불꽃정적")
        void 잔디뱃지_streak0_회색불꽃정적() {
            // given: 신규 유저 — 회색 불꽃, 깜빡임 없음 (결정 5)
            BadgeResponse data = sampleData(0, 0L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then
            assertThat(svg)
                    .contains(">0-day</text>")
                    .doesNotContain("#ff3d00")
                    // 불꽃 깜빡임(FLICKER_DUR=0.9s)이 없어야 한다 (바닥 토큰 먹기 opacity와 무관)
                    .doesNotContain("dur=\"0.9s\"");
        }

        @Test
        @DisplayName("잔디뱃지_username특수문자_이스케이프")
        void 잔디뱃지_username특수문자_이스케이프() {
            // given
            BadgeResponse data = sampleData("a<script>", 1, 100L, Map.of(TODAY, 100L));
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: XSS 방지
            assertThat(svg).contains("@a&lt;script&gt;").doesNotContain("<script>");
        }

        @Test
        @DisplayName("잔디뱃지_username장문_말줄임")
        void 잔디뱃지_username장문_말줄임() {
            // given: GitHub 최대 39자 — streak 그룹이 하늘 영역(x=321)을 침범하지 않아야 한다
            String longName = "a".repeat(39);
            BadgeResponse data = sampleData(longName, 1, 100L, Map.of(TODAY, 100L));
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 15자 표시(14자 + 말줄임표)
            assertThat(svg)
                    .contains("@" + "a".repeat(14) + "…")
                    .doesNotContain("@" + longName);
        }
    }

    @Nested
    @DisplayName("마스코트")
    class Mascot {

        @Test
        @DisplayName("잔디뱃지_마스코트_walk애니메이션포함")
        void 잔디뱃지_마스코트_walk애니메이션포함() {
            // given
            BadgeResponse data = sampleData(42, 100L);
            // when
            String svg = theme.build(data, BadgeMode.LIGHT);
            // then: 걷기 translate + 방향 flip scale + 픽셀 스프라이트
            assertThat(svg)
                    .contains("type=\"translate\"")
                    .contains("type=\"scale\"")
                    .contains("shape-rendering=\"crispEdges\"");
        }
    }

    // ==== 테스트 데이터 헬퍼 ====

    private static BadgeResponse sampleData(int streakDays, long yearTokens) {
        return sampleData("lee-daily", streakDays, yearTokens, Map.of());
    }

    private static BadgeResponse sampleData(int streakDays, long yearTokens, Map<LocalDate, Long> usage) {
        return sampleData("lee-daily", streakDays, yearTokens, usage);
    }

    /** TODAY 종료 365일 0-채움 daily1y를 만든다 (서비스 조립 형태와 동일). */
    private static BadgeResponse sampleData(String username, int streakDays, long yearTokens,
                                            Map<LocalDate, Long> usage) {
        List<DailyCountResponse> daily1y = new ArrayList<>();
        for (int i = 364; i >= 0; i--) {
            LocalDate date = TODAY.minusDays(i);
            daily1y.add(new DailyCountResponse(date.toString(), usage.getOrDefault(date, 0L)));
        }
        return new BadgeResponse(username, 0L, List.of(), List.of(), 0.0,
                yearTokens, streakDays, daily1y, "");
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
