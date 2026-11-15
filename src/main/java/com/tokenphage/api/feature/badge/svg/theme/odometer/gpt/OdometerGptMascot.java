package com.tokenphage.api.feature.badge.svg.theme.odometer.gpt;

import com.tokenphage.api.feature.badge.svg.SvgLogo;

/**
 * 주행거리계 GPT 테마의 회전하는 OpenAI 로고.
 * <p>
 * 바퀴가 도는 동안 빠르게 돌다 감속하고, 이후 14초에 한 바퀴씩 계속 돈다.
 * 회전 값은 프로토타입 실측값이다.
 */
final class OdometerGptMascot {

    /**
     * 1차 회전 (0~4.8초, 한 번).
     * <p>
     * 1.4초 동안 초당 640도 → 1.4초 동안 초당 160도까지 감속 → 2.0초 동안 초당 25.7도까지 감속.
     * 끝 속도가 2차 회전의 속도(360도 / 14초)와 같아 이어질 때 속도가 튀지 않는다.
     */
    private static final String FIRST_SPIN =
            "<animateTransform attributeName=\"transform\" type=\"rotate\""
                    + " values=\"0 12 12;896.00 12 12;1456.00 12 12;1641.71 12 12\" keyTimes=\"0;0.2917;0.5833;1\""
                    + " calcMode=\"spline\" keySplines=\".3333 0.3333 .6667 0.6667;.3333 0.5333 .6667 0.8667;.3333 0.5744 .6667 0.9077\""
                    + " dur=\"4.80s\"/>";

    /** 2차 회전 (4.8초~, 무한). 1차가 끝난 각도(1641.71도 = 201.71도)에서 이어받아 한 바퀴씩 돈다. */
    private static final String SECOND_SPIN =
            "<animateTransform attributeName=\"transform\" type=\"rotate\" from=\"201.71 12 12\" to=\"561.71 12 12\""
                    + " dur=\"14s\" begin=\"4.80s\" repeatCount=\"indefinite\"/>";

    private OdometerGptMascot() {
    }

    /**
     * 회전하는 OpenAI 로고 SVG 조각을 반환한다.
     *
     * @param fill 로고 색 (#RRGGBB)
     * @return 배지 왼쪽(14, 16)에 32px로 놓이는 로고 SVG 조각
     * @Since 2026-11-13
     */
    static String render(String fill) {
        return "<g transform=\"translate(14 16) scale(1.3333)\"><g>" + FIRST_SPIN + SECOND_SPIN
                + "<path d=\"%s\" fill=\"%s\"/></g></g>".formatted(SvgLogo.OPENAI_PATH, fill);
    }
}
