package com.tokenphage.api.feature.badge.svg.theme.betatester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.SvgText;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 터미널 창 배지의 테마 계약·SVG 구조·팔레트 3종·스냅샷 표기를 검증한다.
 * <p>
 * 좌표·표기는 프로토타입 실측값으로 고정한다. 스냅샷 문자열은 변환 없이 그대로 나가야 한다.
 */
class BetaTesterBadgeThemeTest {

    /** 프로토타입 기준 사용자명(7자) — 커서 x=176 실측의 전제. */
    private static final String PROTO_NAME = "lys9908";

    private static final String FULL_JSON = snapshotJson(10, "2026.07 – 2026.08", "324T", true, true);

    private final BetaTesterBadgeTheme theme = new BetaTesterBadgeTheme();

    private static String snapshotJson(int signupRank, String period, String tokensAdded,
                                       boolean isClaudeUse, boolean isGptUse) {
        return """
                {"signupRank":%d,"period":"%s","syncsRun":"128","badgeServed":"4.2K",
                 "tokensAdded":"%s","isClaudeUse":%s,"isGptUse":%s}"""
                .formatted(signupRank, period, tokensAdded, isClaudeUse, isGptUse);
    }

    private static BadgeResponse data(String username, String snapshotJson) {
        return new BadgeResponse(username, 0L, List.of(), List.of(), 0.0, 0L, 0, List.of(), snapshotJson);
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

    @Nested
    @DisplayName("테마 계약")
    class ThemeContract {

        @Test
        @DisplayName("테마이름_조회_BadgeCode상수와일치")
        void 테마이름_조회_BadgeCode상수와일치() {
            // given

            // when
            String result = theme.name();

            // then
            assertThat(result).isEqualTo(BadgeCode.BETA_TESTER.getCode());
            assertThat(result).isEqualTo("beta-tester");
        }

        @Test
        @DisplayName("필요한데이터_조회_스냅샷하나")
        void 필요한데이터_조회_스냅샷하나() {
            // given
            // 표시 항목 전부가 고정 스냅샷 한 행에서 온다. 라이브 집계 needs가 섞이면 안 된다.

            // when
            // then
            assertThat(theme.needs()).containsExactly(BadgeDataNeed.BADGE_SNAPSHOT);
        }

        @Test
        @DisplayName("지원모드_조회_악센트3종")
        void 지원모드_조회_악센트3종() {
            // given

            // when
            // then
            assertThat(theme.supportedModes()).containsExactlyInAnyOrder(
                    BadgeMode.CYAN, BadgeMode.GREEN, BadgeMode.PURPLE);
        }

        @Test
        @DisplayName("기본모드_조회_cyan")
        void 기본모드_조회_cyan() {
            // given

            // when
            // then
            assertThat(theme.defaultMode()).isEqualTo(BadgeMode.CYAN);
        }
    }

    @Nested
    @DisplayName("SVG 구조")
    class SvgStructure {

        @Test
        @DisplayName("기본구조_루트속성_표시크기410x161와앵커랩")
        void 기본구조_루트속성_표시크기410x161와앵커랩() {
            // given

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("width=\"410\"").contains("height=\"161\"")
                    .contains("viewBox=\"0 0 310 122\"")
                    .contains("xmlns:xlink=\"http://www.w3.org/1999/xlink\"")
                    .contains("role=\"img\"")
                    .contains("aria-label=\"Tokenphage beta tester lys9908\"")
                    .contains("<a href=\"" + SvgText.LINK_URL + "\"")
                    .contains("</a>");
            assertThat(countOccurrences(svg, "<a ")).isEqualTo(1);
        }

        @Test
        @DisplayName("터미널크롬_타이틀바신호등헤더_실측값")
        void 터미널크롬_타이틀바신호등헤더_실측값() {
            // given

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("fill=\"#2D2D2F\"")
                    .contains("cx=\"11\" cy=\"9\" r=\"3\" fill=\"#FF5F57\"")
                    .contains("cx=\"22\" cy=\"9\" r=\"3\" fill=\"#FEBC2E\"")
                    .contains("cx=\"33\" cy=\"9\" r=\"3\" fill=\"#28C840\"")
                    .contains("tokenphage — beta")
                    .contains("x1=\"83.5\"")
                    .contains("rx=\"7\"")
                    .contains("stroke=\"#48484A\"");
        }

        @Test
        @DisplayName("애니메이션_모션축소_정지규칙포함")
        void 애니메이션_모션축소_정지규칙포함() {
            // given

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("prefers-reduced-motion")
                    .contains("tp-blink-cyan")
                    .contains("tp-scan-cyan")
                    .contains("tp-type-cyan");
        }

        @Test
        @DisplayName("외부참조_없음")
        void 외부참조_없음() {
            // given
            // GitHub camo <img> 임베드에서는 외부 CSS·스크립트가 차단된다.

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .doesNotContain("<script")
                    .doesNotContain("@import")
                    .doesNotContain("url(http");
        }
    }

    @Nested
    @DisplayName("팔레트 3종")
    class Palette {

        @ParameterizedTest(name = "[{index}] {0} → accent {1}")
        @CsvSource({
                "CYAN,   #64D2FF",
                "GREEN,  #64DC82",
                "PURPLE, #D08BF5"
        })
        @DisplayName("팔레트_각모드_해당악센트만포함")
        void 팔레트_각모드_해당악센트만포함(BadgeMode mode, String accent) {
            // given
            List<String> allAccents = List.of("#64D2FF", "#64DC82", "#D08BF5");

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), mode);

            // then
            assertThat(svg).contains(accent);
            allAccents.stream()
                    .filter(hex -> !hex.equals(accent))
                    .forEach(other -> assertThat(svg).doesNotContain(other));
        }

        @Test
        @DisplayName("글로우_모드별_팔레트필터적용")
        void 글로우_모드별_팔레트필터적용() {
            // given
            // 세 모드는 같은 이중 그림자 수치를 쓰고 글로우 색만 다르다.

            // when
            String cyan = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);
            String green = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.GREEN);

            // then
            assertThat(cyan).contains(BetaTesterColors.CYAN.mascotGlow());
            assertThat(countOccurrences(cyan, "drop-shadow(")).isEqualTo(2);
            assertThat(green).contains(BetaTesterColors.GREEN.mascotGlow());
            assertThat(countOccurrences(green, "drop-shadow(")).isEqualTo(2);
        }

        @Test
        @DisplayName("모드접미사_defs와클래스_모드별유니크")
        void 모드접미사_defs와클래스_모드별유니크() {
            // given
            // 한 페이지에 두 모드를 함께 임베드해도 id·클래스가 충돌하면 안 된다.

            // when
            String cyan = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);
            String green = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.GREEN);

            // then
            assertThat(cyan).contains("tp-bt-clip-cyan").contains("tp-bt-scanline-cyan");
            assertThat(green)
                    .contains("tp-bt-clip-green").contains("tp-bt-scanline-green")
                    .doesNotContain("tp-blink-cyan");
        }

        @Test
        @DisplayName("비지원모드_직접전달_cyan으로렌더")
        void 비지원모드_직접전달_cyan으로렌더() {
            // given
            // 디스패처를 거치지 않은 직접 호출 경로 (경계).

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.DARK);

            // then
            assertThat(svg).contains("#64D2FF").contains("tp-blink-cyan");
        }
    }

    @Nested
    @DisplayName("유저명·커서")
    class Username {

        @Test
        @DisplayName("유저명_7자_커서실측x176")
        void 유저명_7자_커서실측x176() {
            // given
            // 95 + (@ 포함 8글자) × 10.125 = 176 — 프로토타입 실측과 일치해야 한다.

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("@</tspan>lys9908</text>")
                    .contains("x=\"176\" y=\"42\" width=\"5\" height=\"14\"");
        }

        @Test
        @DisplayName("유저명_19자_말줄임없이경계커서")
        void 유저명_19자_말줄임없이경계커서() {
            // given
            // 표시 상한 19자 직전값 — 커서 x=95+20×10.125=297.5로 우측 경계(298) 안 (경계).
            String name = "a".repeat(19);

            // when
            String svg = theme.build(data(name, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("@</tspan>" + name + "</text>")
                    .contains("x=\"297.5\" y=\"42\"");
        }

        @Test
        @DisplayName("유저명_20자_말줄임과경계커서")
        void 유저명_20자_말줄임과경계커서() {
            // given
            // 상한 직후값 — 18자 + 말줄임표로 접는다 (경계).
            String name = "a".repeat(20);

            // when
            String svg = theme.build(data(name, FULL_JSON), BadgeMode.CYAN);

            // then
            // aria-label에는 원본 전체 이름이 남으므로 부재 단언은 표시 텍스트(@ tspan 뒤)로 좁힌다
            assertThat(svg)
                    .contains("@</tspan>" + "a".repeat(18) + "…</text>")
                    .doesNotContain("@</tspan>" + "a".repeat(19))
                    .contains("x=\"297.5\" y=\"42\"");
        }

        @Test
        @DisplayName("유저명_40자_우측경계이내")
        void 유저명_40자_우측경계이내() {
            // given
            // users.username 최대 길이(VARCHAR 40)에서도 커서가 본문 밖으로 나가면 안 된다.
            String name = "b".repeat(40);

            // when
            String svg = theme.build(data(name, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("b".repeat(18) + "…</text>")
                    .contains("x=\"297.5\" y=\"42\"");
        }

        @Test
        @DisplayName("유저명_특수문자_이스케이프")
        void 유저명_특수문자_이스케이프() {
            // given
            String name = "u<s>&x";

            // when
            String svg = theme.build(data(name, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg).contains("u&lt;s&gt;&amp;x").doesNotContain("<s>");
        }
    }

    @Nested
    @DisplayName("스냅샷 표기")
    class SnapshotDisplay {

        @Test
        @DisplayName("통계3칸_문자열그대로_변환없음")
        void 통계3칸_문자열그대로_변환없음() {
            // given
            // 표시 포맷은 적재 시점에 고정됐다. formatTokens를 거치면 324T가 324.0T로 바뀐다.

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains(">128</text>")
                    .contains(">4.2K</text>")
                    .contains(">324T</text>")
                    .doesNotContain("324.0T");
        }

        @Test
        @DisplayName("통계라벨_3종_고정문구")
        void 통계라벨_3종_고정문구() {
            // given

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("CONTRIBUTED DURING BETA")
                    .contains("SYNCS RUN")
                    .contains("BADGE SERVED")
                    .contains("TOKENS ADDED");
        }

        @Test
        @DisplayName("통계문자열_특수문자_이스케이프")
        void 통계문자열_특수문자_이스케이프() {
            // given
            // 스냅샷은 DB에서 온 자유 문자열이라 주입 표면이다.
            String json = snapshotJson(10, "2026.07 – 2026.08", "<b>1&2</b>", true, true);

            // when
            String svg = theme.build(data(PROTO_NAME, json), BadgeMode.CYAN);

            // then
            assertThat(svg).contains("&lt;b&gt;1&amp;2&lt;/b&gt;").doesNotContain("<b>");
        }

        @Test
        @DisplayName("가입순번_양수_표시")
        void 가입순번_양수_표시() {
            // given

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg).contains(">#10</text>").contains("text-anchor=\"middle\"");
        }

        @Test
        @DisplayName("가입순번_0_미표시")
        void 가입순번_0_미표시() {
            // given
            // 없는 값을 0위라고 주장하지 않는다 (경계).
            String json = snapshotJson(0, "2026.07 – 2026.08", "324T", true, true);

            // when
            String svg = theme.build(data(PROTO_NAME, json), BadgeMode.CYAN);

            // then
            assertThat(svg).doesNotContain(">#");
        }

        @Test
        @DisplayName("기간_값있음_배너옆표시")
        void 기간_값있음_배너옆표시() {
            // given
            // 우측 끝 정렬에서 배너 옆으로 옮겼다 — 배너와 같은 베이스라인(y=49)에 이어 붙는다.

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains(">2026.07 – 2026.08</text>")
                    .contains("x=\"169\" y=\"37\"");
        }

        @Test
        @DisplayName("기간_빈값_미표시")
        void 기간_빈값_미표시() {
            // given
            String json = snapshotJson(10, "", "324T", true, true);

            // when
            String svg = theme.build(data(PROTO_NAME, json), BadgeMode.CYAN);

            // then
            // letter-spacing=".4"는 기간 텍스트에만 쓰인다 (타이틀바 .3 / 라벨 .6 / 섹션 .9).
            assertThat(svg).doesNotContain("letter-spacing=\".4\"");
        }

        @Test
        @DisplayName("통계3칸_배치_열간격확보")
        void 통계3칸_배치_열간격확보() {
            // given
            // 라벨이 7px로 커져 프로토타입 간격(95/139/194)으로는 열이 겹친다.

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then: BADGE SERVED(12자 × 4.8 = 57.6)가 다음 열 224를 침범하지 않는 x
            assertThat(svg)
                    .contains("x=\"95\" y=\"93\"")
                    .contains("x=\"152\" y=\"93\"")
                    .contains("x=\"224\" y=\"93\"");
        }

        @Test
        @DisplayName("셸프롬프트_고정문구_타이핑클래스")
        void 셸프롬프트_고정문구_타이핑클래스() {
            // given

            // when
            String svg = theme.build(data(PROTO_NAME, FULL_JSON), BadgeMode.CYAN);

            // then
            assertThat(svg)
                    .contains("echo \"Thanks for shaping the beta.\"")
                    .contains("tp-type-cyan");
        }
    }

    @Nested
    @DisplayName("사용 모델 로고")
    class ProviderMarks {

        @ParameterizedTest(name = "[{index}] claude={0}, gpt={1} → claude로고 {2}개, openai로고 {3}개")
        @CsvSource({
                "true,  false, 1, 0",
                "false, true,  0, 1",
                "true,  true,  1, 1",
                "false, false, 0, 0"
        })
        @DisplayName("로고_조합4가지_해당로고만표시")
        void 로고_조합4가지_해당로고만표시(boolean claude, boolean gpt, int claudeCount, int openaiCount) {
            // given
            String json = snapshotJson(10, "2026.07 – 2026.08", "324T", claude, gpt);

            // when
            String svg = theme.build(data(PROTO_NAME, json), BadgeMode.CYAN);

            // then
            assertThat(countOccurrences(svg, "M4.709")).isEqualTo(claudeCount);
            assertThat(countOccurrences(svg, "M22.2819")).isEqualTo(openaiCount);
        }
    }

    @Nested
    @DisplayName("스냅샷 결손")
    class MissingSnapshot {

        @Test
        @DisplayName("스냅샷빈문자열_예외없이_대시와미표시렌더")
        void 스냅샷빈문자열_예외없이_대시와미표시렌더() {
            // given
            // 자격은 있는데 스냅샷 적재가 누락된 사용자 (경계).
            BadgeResponse missing = data("newbie", "");

            // when
            // then
            assertThatCode(() -> theme.build(missing, BadgeMode.CYAN)).doesNotThrowAnyException();

            String svg = theme.build(missing, BadgeMode.CYAN);
            assertThat(countOccurrences(svg, ">-</text>")).isEqualTo(3);
            assertThat(svg)
                    .doesNotContain(">#")
                    .doesNotContain("letter-spacing=\".4\"")
                    .contains("@</tspan>newbie</text>");
        }
    }
}
