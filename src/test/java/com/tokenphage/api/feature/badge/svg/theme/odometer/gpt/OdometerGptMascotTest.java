package com.tokenphage.api.feature.badge.svg.theme.odometer.gpt;

import com.tokenphage.api.feature.badge.svg.SvgLogo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 주행거리계 GPT 테마의 OpenAI 로고 회전을 검증한다.
 * <p>
 * 로고 path는 텍스트 블록을 줄 이어 붙인 문자열 상수라 한 글자만 빠져도 도형이 깨지는데 컴파일러는 모른다.
 * 회전은 감속하는 1차와 일정한 2차가 각도·시각으로 이어져야 끊겨 보이지 않는다.
 */
class OdometerGptMascotTest {

    private static final Pattern FIRST_SPIN = Pattern.compile(
            "<animateTransform attributeName=\"transform\" type=\"rotate\" values=\"([^\"]+)\"[^>]* dur=\"([^\"]+)\"/>");

    private static final Pattern SECOND_SPIN = Pattern.compile(
            "<animateTransform attributeName=\"transform\" type=\"rotate\" from=\"([0-9.]+) 12 12\" to=\"([0-9.]+) 12 12\""
                    + " dur=\"([^\"]+)\" begin=\"([^\"]+)\" repeatCount=\"indefinite\"/>");

    @Nested
    @DisplayName("OpenAI 로고 path")
    class LogoPath {

        @Test
        @DisplayName("로고path_상수_1461자이고시작과끝이프로토타입과같음")
        void 로고path_상수_1461자이고시작과끝이프로토타입과같음() {
            // given
            // beta-tester 배지와 함께 쓰는 상수다. 줄 잇기가 깨지면 길이가 달라진다.

            // when
            String path = SvgLogo.OPENAI_PATH;

            // then
            assertThat(path).hasSize(1461).startsWith("M22.2819 9.8211").endsWith("Z").doesNotContain("\n");
        }
    }

    @Nested
    @DisplayName("render() - 회전")
    class Spin {

        @Test
        @DisplayName("렌더_흰색지정_실측배치에지정한색의로고")
        void 렌더_흰색지정_실측배치에지정한색의로고() {
            // given
            String fill = "#ffffff";

            // when
            String svg = OdometerGptMascot.render(fill);

            // then
            // 24×24 로고를 1.3333배(32px)로 키워 (14, 16)에 둔다.
            assertThat(svg)
                    .startsWith("<g transform=\"translate(14 16) scale(1.3333)\"><g><animateTransform ")
                    .endsWith("<path d=\"" + SvgLogo.OPENAI_PATH + "\" fill=\"#ffffff\"/></g></g>");
        }

        @Test
        @DisplayName("회전_1차_빠르게돌다감속하는4.8초")
        void 회전_1차_빠르게돌다감속하는4점8초() {
            // given
            // 1.4초 동안 초당 640도 → 1.4초 동안 160도까지 감속 → 2.0초 동안 25.7도까지 감속

            // when
            String svg = OdometerGptMascot.render("#ffffff");

            // then
            assertThat(svg).contains("<animateTransform attributeName=\"transform\" type=\"rotate\""
                    + " values=\"0 12 12;896.00 12 12;1456.00 12 12;1641.71 12 12\" keyTimes=\"0;0.2917;0.5833;1\""
                    + " calcMode=\"spline\" keySplines=\".3333 0.3333 .6667 0.6667;.3333 0.5333 .6667 0.8667;.3333 0.5744 .6667 0.9077\""
                    + " dur=\"4.80s\"/>");
        }

        @Test
        @DisplayName("회전_2차_1차가끝난각도와시각에서이어받아한바퀴씩")
        void 회전_2차_1차가끝난각도와시각에서이어받아한바퀴씩() {
            // given
            // 각도가 어긋나면 4.8초에 로고가 튀고, begin이 어긋나면 잠깐 멈추거나 겹친다.

            // when
            String svg = OdometerGptMascot.render("#ffffff");

            // then
            Matcher first = FIRST_SPIN.matcher(svg);
            Matcher second = SECOND_SPIN.matcher(svg);
            assertThat(first.find()).as("1차 회전").isTrue();
            assertThat(second.find()).as("2차 회전").isTrue();
            String[] firstStops = first.group(1).split(";");
            BigDecimal firstEndAngle = new BigDecimal(firstStops[firstStops.length - 1].split(" ")[0]);
            BigDecimal secondFrom = new BigDecimal(second.group(1));
            BigDecimal secondTo = new BigDecimal(second.group(2));
            assertThat(secondFrom).isEqualByComparingTo(firstEndAngle.remainder(BigDecimal.valueOf(360)));
            assertThat(secondTo.subtract(secondFrom)).isEqualByComparingTo("360");
            assertThat(second.group(4)).isEqualTo(first.group(2)).isEqualTo("4.80s");
            assertThat(second.group(3)).isEqualTo("14s");
        }
    }
}
