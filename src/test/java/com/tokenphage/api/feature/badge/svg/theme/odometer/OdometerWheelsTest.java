package com.tokenphage.api.feature.badge.svg.theme.odometer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 누적 가동시간을 6자리 숫자로 바꾸는 계산과, 바퀴마다 굴러 내려오는 숫자 띠의 조립을 검증한다.
 * <p>
 * 띠의 숫자·개수·dur는 코드가 계산해 만드는 문자열이라 컴파일러가 잡지 못한다. 기대값은 프로토타입 실측값이다.
 */
class OdometerWheelsTest {

    private static final String ACCENT = "#D97757";

    /** 1,284.6시간. 바퀴가 0 1 2 8 4 6에서 멈춘다. */
    private static final long SAMPLE_RUNTIME_MS = 4_624_560_000L;

    /** 바퀴 하나의 숫자 띠: 애니메이션 한 개와 그 뒤의 숫자 text 묶음. 음수가 그려졌는지도 봐야 해서 -를 허용한다. */
    private static final Pattern STRIP = Pattern.compile(
            "<g><animateTransform[^>]* values=\"([^\"]+)\"[^>]* dur=\"([^\"]+)\"[^>]*/>((?:<text [^>]*>-?\\d+</text>)+)</g>");

    private static final Pattern DIGIT = Pattern.compile(">(-?\\d+)</text>");

    private record Strip(String values, String dur, String digits) {
    }

    private static List<Strip> stripsOf(String svg) {
        return STRIP.matcher(svg).results()
                .map(r -> new Strip(r.group(1), r.group(2), digitsOf(r.group(3))))
                .toList();
    }

    private static String digitsOf(String texts) {
        StringBuilder sb = new StringBuilder();
        Matcher m = DIGIT.matcher(texts);
        while (m.find()) {
            sb.append(m.group(1));
        }
        return sb.toString();
    }

    @Nested
    @DisplayName("digits() - 가동시간을 6자리로 환산")
    class Digits {

        @ParameterizedTest(name = "{0}ms → {1}")
        @CsvSource({
                "4624560000,          012846",
                "0,                   000000",
                "359999,              000000",
                "360000,              000001",
                "3600000,             000010",
                "359999639999,        999998",
                "359999640000,        999999",
                "9223372036854775807, 999999"
        })
        @DisplayName("환산_0.1시간단위내림_상한999999")
        void 환산_가동시간_내림한6자리와상한(long runtimeMs, String expected) {
            // given
            // 0.1시간 = 360,000ms. 반올림하지 않아 표시값이 실제 값을 넘지 않는다.

            // when
            String digits = OdometerWheels.digits(runtimeMs);

            // then
            assertThat(digits).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}ms → 000000")
        @CsvSource({ "-1", "-360000", "-9223372036854775808" })
        @DisplayName("환산_음수_000000")
        void 환산_음수_000000(long runtimeMs) {
            // given
            // DB CHECK로 음수는 들어오지 않지만, 들어와도 음수 숫자를 그리지 않는다.

            // when
            String digits = OdometerWheels.digits(runtimeMs);

            // then
            assertThat(digits).isEqualTo("000000");
        }
    }

    @Nested
    @DisplayName("render() - 바퀴와 숫자 띠")
    class Render {

        @Test
        @DisplayName("띠_표본값_최종숫자에서1씩내려가고0다음은9")
        void 띠_표본값_최종숫자에서1씩내려가고0다음은9() {
            // given
            // 띠의 첫 숫자가 멈췄을 때 보이는 최종 숫자다. 바퀴 0·1·5의 최종 숫자는 0·1·6이다.

            // when
            List<Strip> strips = stripsOf(OdometerWheels.render(SAMPLE_RUNTIME_MS, ACCENT));

            // then
            assertThat(strips).hasSize(6);
            assertThat(strips.get(0).digits()).isEqualTo("098765432");
            assertThat(strips.get(1).digits()).isEqualTo("1098765432109");
            assertThat(strips.get(5).digits()).isEqualTo("65432109876543210987654321098");
        }

        @Test
        @DisplayName("띠_최종숫자9_9부터내려감")
        void 띠_최종숫자9_9부터내려감() {
            // given
            long maxedOut = Long.MAX_VALUE;

            // when
            List<Strip> strips = stripsOf(OdometerWheels.render(maxedOut, ACCENT));

            // then
            assertThat(strips.get(0).digits()).isEqualTo("987654321");
        }

        @ParameterizedTest(name = "{0}ms")
        @CsvSource({ "0", "4624560000", "9223372036854775807" })
        @DisplayName("띠_어떤값이든_바퀴i는숫자9더하기4i개이고음수없음")
        void 띠_어떤값이든_바퀴i는숫자9더하기4i개이고음수없음(long runtimeMs) {
            // given
            // 뒤 바퀴일수록 더 많이 굴러야 늦게 멈춘다.

            // when
            List<Strip> strips = stripsOf(OdometerWheels.render(runtimeMs, ACCENT));

            // then
            assertThat(strips).extracting(s -> s.digits().length()).containsExactly(9, 13, 17, 21, 25, 29);
            assertThat(strips).allSatisfy(s -> assertThat(s.digits()).matches("[0-9]+"));
        }

        @Test
        @DisplayName("애니메이션_바퀴6개_시작위치와dur이프로토타입값")
        void 애니메이션_바퀴6개_시작위치와dur이프로토타입값() {
            // given
            // 시작 위치는 띠 길이만큼 내려간 곳(34px × 굴릴 칸 수)이고, dur는 0.28초씩 늘어난다.

            // when
            List<Strip> strips = stripsOf(OdometerWheels.render(SAMPLE_RUNTIME_MS, ACCENT));

            // then
            assertThat(strips).extracting(Strip::values).containsExactly(
                    "0 272;0 -4;0 0", "0 408;0 -4;0 0", "0 544;0 -4;0 0",
                    "0 680;0 -4;0 0", "0 816;0 -4;0 0", "0 952;0 -4;0 0");
            assertThat(strips).extracting(Strip::dur).containsExactly(
                    "1.10s", "1.38s", "1.66s", "1.94s", "2.22s", "2.50s");
        }

        @Test
        @DisplayName("조각_첫바퀴_프로토타입과같은마크업")
        void 조각_첫바퀴_프로토타입과같은마크업() {
            // given
            // calcMode="spline"이 빠지면 keySplines가 무시되어 선형으로 움직인다.

            // when
            String svg = OdometerWheels.render(SAMPLE_RUNTIME_MS, ACCENT);

            // then
            assertThat(svg).startsWith("<rect x=\"124\" y=\"13\" width=\"21\" height=\"38\" fill=\"#2b2b2b\"/>"
                    + "<g><animateTransform attributeName=\"transform\" type=\"translate\" values=\"0 272;0 -4;0 0\""
                    + " keyTimes=\"0;.88;1\" calcMode=\"spline\" keySplines=\".1 .6 .2 1;.4 0 .6 1\" dur=\"1.10s\" fill=\"freeze\"/>"
                    + "<text x=\"134.5\" y=\"40\" font-family=\"'JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,monospace\""
                    + " font-size=\"22\" font-weight=\"800\" fill=\"#f5f5f5\" text-anchor=\"middle\">0</text>"
                    + "<text x=\"134.5\" y=\"6\" ");
        }

        @Test
        @DisplayName("조각_마지막바퀴_강조색바탕에흰숫자")
        void 조각_마지막바퀴_강조색바탕에흰숫자() {
            // given
            // 소수 자리 바퀴만 강조색이다. 강조색 hex는 마스코트에도 나오므로 조각 전체로 본다.

            // when
            String svg = OdometerWheels.render(SAMPLE_RUNTIME_MS, ACCENT);

            // then
            assertThat(svg)
                    .contains("<rect x=\"239\" y=\"13\" width=\"21\" height=\"38\" fill=\"#D97757\"/>")
                    .contains("<text x=\"249.5\" y=\"40\" font-family=\"'JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,monospace\""
                            + " font-size=\"22\" font-weight=\"800\" fill=\"#ffffff\" text-anchor=\"middle\">6</text>")
                    .endsWith(" y=\"-912\" font-family=\"'JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,monospace\""
                            + " font-size=\"22\" font-weight=\"800\" fill=\"#ffffff\" text-anchor=\"middle\">8</text></g>");
        }
    }
}
