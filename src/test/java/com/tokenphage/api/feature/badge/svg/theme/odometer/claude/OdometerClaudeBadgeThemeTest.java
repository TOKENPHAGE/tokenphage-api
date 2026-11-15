package com.tokenphage.api.feature.badge.svg.theme.odometer.claude;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주행거리계 Claude 테마의 계약(이름·요구 데이터·모드)과 렌더 결과를 검증한다.
 * <p>
 * 기대 조각은 프로토타입 실측값이다. 색 hex는 다른 요소에도 나오므로 조각 전체로 단언한다.
 */
class OdometerClaudeBadgeThemeTest {

    private static final String REPO_URL = "https://github.com/TOKENPHAGE/tokenphage-api";

    /** 프로토타입의 표본 값. 1,284.6시간이라 바퀴가 0 1 2 8 4 6에서 멈춘다. */
    private static final long SAMPLE_RUNTIME_MS = 4_624_560_000L;

    private static final Pattern FINAL_DIGIT = Pattern.compile("<text x=\"[0-9.]+\" y=\"40\"[^>]*>(\\d)</text>");

    private final OdometerClaudeBadgeTheme theme = new OdometerClaudeBadgeTheme();

    private static BadgeResponse dataOf(String username, long totalRuntimeMs) {
        return new BadgeResponse(username, 0L, List.of(), List.of(), 0.0, 0L, 0, List.of(), "", totalRuntimeMs);
    }

    /** 바퀴가 멈췄을 때 창에 보이는 숫자(y=40) 6개를 이어 붙인다. */
    private static String finalDigitsOf(String svg) {
        return FINAL_DIGIT.matcher(svg).results().map(r -> r.group(1)).collect(Collectors.joining());
    }

    @Nested
    @DisplayName("테마 계약")
    class Contract {

        @Test
        @DisplayName("이름_호출_odometer-claude")
        void 이름_호출_배지코드문자열() {
            // given

            // when
            String name = theme.name();

            // then
            assertThat(name).isEqualTo("odometer-claude");
        }

        @Test
        @DisplayName("요구데이터_호출_누적가동시간하나")
        void 요구데이터_호출_누적가동시간하나() {
            // given
            // 여기에 없는 데이터는 조회되지 않아 0으로 그려진다.

            // when
            var needs = theme.needs();

            // then
            assertThat(needs).containsExactly(BadgeDataNeed.TOTAL_RUNTIME_MS);
        }

        @Test
        @DisplayName("지원모드_호출_light와dark이고기본은light")
        void 지원모드_호출_light와dark이고기본은light() {
            // given

            // when
            var modes = theme.supportedModes();

            // then
            assertThat(modes).containsExactlyInAnyOrder(BadgeMode.LIGHT, BadgeMode.DARK);
            assertThat(theme.defaultMode()).isEqualTo(BadgeMode.LIGHT);
        }
    }

    @Nested
    @DisplayName("build() - 골격")
    class Skeleton {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = BadgeMode.class, names = {"LIGHT", "DARK"})
        @DisplayName("빌드_두모드_300x64캔버스를저장소링크로감쌈")
        void 빌드_두모드_300x64캔버스를저장소링크로감쌈(BadgeMode mode) {
            // given
            BadgeResponse data = dataOf("kobenlys", SAMPLE_RUNTIME_MS);

            // when
            String svg = theme.build(data, mode);

            // then
            assertThat(svg)
                    .startsWith("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                            + " width=\"300\" height=\"64\" viewBox=\"0 0 300 64\" role=\"img\""
                            + " aria-label=\"Agent runtime odometer kobenlys\">")
                    .contains("<a href=\"" + REPO_URL + "\" xlink:href=\"" + REPO_URL + "\">")
                    .endsWith("</a></svg>");
        }

        @Test
        @DisplayName("빌드_username에마크업_ariaLabel에이스케이프")
        void 빌드_username에마크업_ariaLabel에이스케이프() {
            // given
            BadgeResponse data = dataOf("<script>x&y</script>", SAMPLE_RUNTIME_MS);

            // when
            String svg = theme.build(data, BadgeMode.LIGHT);

            // then
            assertThat(svg)
                    .contains("aria-label=\"Agent runtime odometer &lt;script&gt;x&amp;y&lt;/script&gt;\"")
                    .doesNotContain("<script>");
        }

        @Test
        @DisplayName("빌드_username39자_ariaLabel외에는그림이같음")
        void 빌드_username39자_ariaLabel외에는그림이같음() {
            // given
            // GitHub username 최대 길이. 사용자명은 그림에 넣지 않으므로 길이가 레이아웃을 밀지 않는다.
            String longName = "a".repeat(39);
            String shortSvg = theme.build(dataOf("u", SAMPLE_RUNTIME_MS), BadgeMode.DARK);

            // when
            String longSvg = theme.build(dataOf(longName, SAMPLE_RUNTIME_MS), BadgeMode.DARK);

            // then
            assertThat(longSvg.replace("odometer " + longName + "\"", "odometer u\"")).isEqualTo(shortSvg);
        }
    }

    @Nested
    @DisplayName("build() - 바퀴")
    class Wheels {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = BadgeMode.class, names = {"LIGHT", "DARK"})
        @DisplayName("빌드_표본값_바퀴가012846에서멈춤")
        void 빌드_표본값_바퀴가012846에서멈춤(BadgeMode mode) {
            // given
            BadgeResponse data = dataOf("kobenlys", SAMPLE_RUNTIME_MS);

            // when
            String svg = theme.build(data, mode);

            // then
            assertThat(finalDigitsOf(svg)).isEqualTo("012846");
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = BadgeMode.class, names = {"LIGHT", "DARK"})
        @DisplayName("빌드_두모드_마지막바퀴만테마강조색")
        void 빌드_두모드_마지막바퀴만테마강조색(BadgeMode mode) {
            // given
            BadgeResponse data = dataOf("kobenlys", SAMPLE_RUNTIME_MS);

            // when
            String svg = theme.build(data, mode);

            // then
            assertThat(svg)
                    .contains("<rect x=\"239\" y=\"13\" width=\"21\" height=\"38\" fill=\"#D97757\"/>")
                    .contains("<rect x=\"216\" y=\"13\" width=\"21\" height=\"38\" fill=\"#2b2b2b\"/>");
        }

        @Test
        @DisplayName("빌드_가동시간0_000000이고예외없음")
        void 빌드_가동시간0_000000이고예외없음() {
            // given
            // 가동시간을 한 번도 보내지 않은 사용자 (경계)
            BadgeResponse data = dataOf("newuser", 0L);

            // when
            String svg = theme.build(data, BadgeMode.LIGHT);

            // then
            assertThat(finalDigitsOf(svg)).isEqualTo("000000");
        }
    }

    @Nested
    @DisplayName("build() - 몸통·라벨·창")
    class Body {

        private static final String SANS = "Pretendard,system-ui,-apple-system,'Apple SD Gothic Neo',sans-serif";

        @Test
        @DisplayName("빌드_dark_그라데이션몸통과강조색테두리와흰글자")
        void 빌드_dark_그라데이션몸통과강조색테두리와흰글자() {
            // given
            BadgeResponse data = dataOf("kobenlys", SAMPLE_RUNTIME_MS);

            // when
            String svg = theme.build(data, BadgeMode.DARK);

            // then
            assertThat(svg)
                    .contains("<linearGradient id=\"odometerclaudedarkbg\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">"
                            + "<stop offset=\"0\" stop-color=\"#2d323c\"/><stop offset=\"1\" stop-color=\"#1c2027\"/></linearGradient>")
                    .contains("<rect width=\"300\" height=\"64\" rx=\"12\" fill=\"url(#odometerclaudedarkbg)\"/>"
                            + "<rect x=\".5\" y=\".5\" width=\"299\" height=\"63\" rx=\"11.5\" fill=\"none\" stroke=\"#D97757\" stroke-opacity=\".45\"/>")
                    .contains("<text x=\"60\" y=\"29\" font-family=\"" + SANS + "\" font-size=\"10\" font-weight=\"800\""
                            + " fill=\"#ffffff\" text-anchor=\"start\" letter-spacing=\"1.1\">AGENT</text>")
                    .contains("<text x=\"60\" y=\"43\" font-family=\"" + SANS + "\" font-size=\"10\" font-weight=\"800\""
                            + " fill=\"#E9A589\" text-anchor=\"start\" letter-spacing=\"1.1\">RUNTIME</text>")
                    .contains("<rect x=\"121\" y=\"10\" width=\"142\" height=\"44\" rx=\"5\" fill=\"#000\" stroke=\"#444b58\"/>")
                    .contains("<text x=\"271\" y=\"41\" font-family=\"" + SANS + "\" font-size=\"18\" font-weight=\"800\""
                            + " fill=\"#ffffff\" text-anchor=\"start\">h</text>");
        }

        @Test
        @DisplayName("빌드_light_흰몸통과회색테두리와어두운글자")
        void 빌드_light_흰몸통과회색테두리와어두운글자() {
            // given
            BadgeResponse data = dataOf("kobenlys", SAMPLE_RUNTIME_MS);

            // when
            String svg = theme.build(data, BadgeMode.LIGHT);

            // then
            // light에는 몸통 그라데이션이 없다 (쓰지 않는 defs를 싣지 않는다).
            assertThat(svg)
                    .contains("<rect x=\".5\" y=\".5\" width=\"299\" height=\"63\" rx=\"11.5\" fill=\"#ffffff\" stroke=\"#e5e7eb\"/>")
                    .doesNotContain("#2d323c")
                    .contains("<text x=\"60\" y=\"29\" font-family=\"" + SANS + "\" font-size=\"10\" font-weight=\"800\""
                            + " fill=\"#111827\" text-anchor=\"start\" letter-spacing=\"1.1\">AGENT</text>")
                    .contains("<text x=\"60\" y=\"43\" font-family=\"" + SANS + "\" font-size=\"10\" font-weight=\"800\""
                            + " fill=\"#C4623F\" text-anchor=\"start\" letter-spacing=\"1.1\">RUNTIME</text>")
                    .contains("<rect x=\"121\" y=\"10\" width=\"142\" height=\"44\" rx=\"5\" fill=\"#000\" stroke=\"#111827\"/>")
                    .contains("<text x=\"271\" y=\"41\" font-family=\"" + SANS + "\" font-size=\"18\" font-weight=\"800\""
                            + " fill=\"#111827\" text-anchor=\"start\">h</text>");
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = BadgeMode.class, names = {"LIGHT", "DARK"})
        @DisplayName("빌드_두모드_창안에바퀴와음영과반사광")
        void 빌드_두모드_창안에바퀴와음영과반사광(BadgeMode mode) {
            // given
            String id = "odometerclaude" + mode.getCode();

            // when
            String svg = theme.build(dataOf("kobenlys", SAMPLE_RUNTIME_MS), mode);

            // then
            // 반사광의 opacity 애니메이션이 x 애니메이션의 begin을 참조하지 않으면 반사광이 보이지 않는다.
            assertThat(svg)
                    .contains("<linearGradient id=\"" + id + "shade\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">"
                            + "<stop offset=\"0\" stop-color=\"#000\" stop-opacity=\".88\"/><stop offset=\".34\" stop-color=\"#000\" stop-opacity=\"0\"/>"
                            + "<stop offset=\".66\" stop-color=\"#000\" stop-opacity=\"0\"/><stop offset=\"1\" stop-color=\"#000\" stop-opacity=\".88\"/></linearGradient>")
                    .contains("<clipPath id=\"" + id + "win\"><rect x=\"124\" y=\"13\" width=\"136\" height=\"38\" rx=\"2\"/></clipPath>")
                    .contains("<g clip-path=\"url(#" + id + "win)\"><rect x=\"124\" y=\"13\" width=\"21\" height=\"38\" fill=\"#2b2b2b\"/>")
                    .contains("</text></g><rect x=\"124\" y=\"13\" width=\"136\" height=\"38\" fill=\"url(#" + id + "shade)\"/>"
                            + "<g transform=\"skewX(-18)\"><rect x=\"140\" y=\"8\" width=\"14\" height=\"48\" fill=\"#fff\" opacity=\"0\">"
                            + "<animate id=\"" + id + "glint\" attributeName=\"x\" values=\"140;300\" dur=\"0.8s\" begin=\"3.2s;" + id + "glint.end+6s\"/>"
                            + "<animate attributeName=\"opacity\" values=\"0;.18;.18;0\" keyTimes=\"0;.1;.9;1\" dur=\"0.8s\" begin=\"" + id + "glint.begin\"/>"
                            + "</rect></g></g>");
        }
    }

    @Nested
    @DisplayName("build() - id")
    class Ids {

        private static final Pattern ID = Pattern.compile(" id=\"([^\"]+)\"");
        private static final Pattern URL_REF = Pattern.compile("url\\(#([^)]+)\\)");
        private static final Pattern SYNC_REF = Pattern.compile("begin=\"(?:[0-9.]+s;)?([^\".;]+)\\.(?:begin|end)");

        private static Set<String> groupsOf(Pattern pattern, String svg) {
            return pattern.matcher(svg).results().map(r -> r.group(1)).collect(Collectors.toSet());
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = BadgeMode.class, names = {"LIGHT", "DARK"})
        @DisplayName("id_두모드_하이픈이없고참조가전부정의된id")
        void id_두모드_하이픈이없고참조가전부정의된id(BadgeMode mode) {
            // given
            // SMIL은 begin 값의 '-'를 뺄셈으로 읽는다. id에 '-'가 있으면 반사광 반복이 멈춘다.

            // when
            String svg = theme.build(dataOf("kobenlys", SAMPLE_RUNTIME_MS), mode);

            // then
            Set<String> ids = groupsOf(ID, svg);
            assertThat(ids).isNotEmpty().allSatisfy(id -> assertThat(id).matches("[a-z]+"));
            assertThat(groupsOf(URL_REF, svg)).isNotEmpty().isSubsetOf(ids);
            assertThat(groupsOf(SYNC_REF, svg)).containsExactly("odometerclaude" + mode.getCode() + "glint").isSubsetOf(ids);
        }

        @Test
        @DisplayName("id_light와dark_서로겹치지않음")
        void id_light와dark_서로겹치지않음() {
            // given
            // 한 페이지에 두 모드를 함께 넣으면 같은 id가 서로의 그라데이션·clip을 가로챈다.
            BadgeResponse data = dataOf("kobenlys", SAMPLE_RUNTIME_MS);

            // when
            Set<String> lightIds = groupsOf(ID, theme.build(data, BadgeMode.LIGHT));
            Set<String> darkIds = groupsOf(ID, theme.build(data, BadgeMode.DARK));

            // then
            assertThat(lightIds).doesNotContainAnyElementsOf(darkIds);
        }
    }

    @Nested
    @DisplayName("build() - 마스코트")
    class Mascot {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = BadgeMode.class, names = {"LIGHT", "DARK"})
        @DisplayName("빌드_두모드_몸통과라벨사이에같은마스코트")
        void 빌드_두모드_몸통과라벨사이에같은마스코트(BadgeMode mode) {
            // given
            // 마스코트는 가동시간·모드와 무관하게 같은 그림이다.
            String mascot = OdometerClaudeMascot.render();

            // when
            String svg = theme.build(dataOf("kobenlys", SAMPLE_RUNTIME_MS), mode);

            // then
            assertThat(mascot).isNotEmpty();
            assertThat(svg).contains("/>" + mascot + "<text x=\"60\" y=\"29\"");
        }
    }
}
