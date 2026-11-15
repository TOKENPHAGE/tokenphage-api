package com.tokenphage.api.feature.badge.svg.theme.odometer;

import java.util.Locale;

/**
 * 주행거리계의 숫자 바퀴 6개를 계산하고 그리는 유틸.
 * <p>
 * 앞 5칸은 누적 가동시간의 시간 정수부, 마지막 1칸은 소수 첫째 자리다.
 * 바퀴마다 숫자 띠를 세로로 쌓아 두고 아래로 굴려 내려 최종 숫자에서 멈춘다. 뒤 바퀴일수록 더 많이 굴러 늦게 멈춘다.
 * 좌표·타이밍은 프로토타입 실측값이다.
 */
final class OdometerWheels {

    /** 바퀴 개수. 정수부 5칸 + 소수 1칸. */
    private static final int WHEEL_COUNT = 6;

    /** 0.1시간(6분)의 ms. 표시 단위다. */
    private static final long MS_PER_TENTH_HOUR = 360_000L;

    /** 6자리로 표시할 수 있는 최댓값 (99,999.9시간). */
    private static final long MAX_TENTHS = 999_999L;

    /** 첫 바퀴의 x와 바퀴 간격(폭 21 + 틈 2). */
    private static final int FIRST_WHEEL_X = 124;
    private static final int WHEEL_PITCH = 23;

    /** 숫자 한 칸의 세로 간격과, 멈춘 숫자의 기준선 y. */
    private static final int DIGIT_PITCH = 34;
    private static final int REST_BASELINE_Y = 40;

    /** 첫 바퀴가 굴리는 칸 수와, 바퀴마다 더 굴리는 칸 수. */
    private static final int FIRST_WHEEL_STEPS = 8;
    private static final int STEPS_PER_WHEEL = 4;

    /** 첫 바퀴의 회전 시간과 바퀴마다 늘어나는 시간(1/100초). 정수로 계산해야 "1.94s"가 "1.9400000000000002s"가 되지 않는다. */
    private static final int FIRST_WHEEL_CENTIS = 110;
    private static final int CENTIS_PER_WHEEL = 28;

    private static final String WHEEL_FILL = "#2b2b2b";
    private static final String DIGIT_FILL = "#f5f5f5";
    private static final String ACCENT_DIGIT_FILL = "#ffffff";

    private static final String MONO_FONT = "'JetBrains Mono',ui-monospace,SFMono-Regular,Menlo,monospace";

    private OdometerWheels() {
    }

    /**
     * 누적 가동시간을 바퀴 6개에 표시할 숫자 문자열로 바꾼다.
     * <p>
     * 0.1시간 단위로 내림한다. 999,999를 넘으면 999,999로, 음수는 0으로 고정한다.
     *
     * @param runtimeMs 누적 가동시간(ms)
     * @return 0으로 채운 6자리 숫자 문자열 (예: 4,624,560,000ms → "012846")
     * @Since 2026-11-13
     */
    static String digits(long runtimeMs) {
        long tenths = Math.clamp(runtimeMs / MS_PER_TENTH_HOUR, 0L, MAX_TENTHS);
        return String.format(Locale.ROOT, "%06d", tenths);
    }

    /**
     * 바퀴 6개와 각 바퀴의 숫자 띠 SVG 조각을 반환한다.
     *
     * @param runtimeMs 누적 가동시간(ms)
     * @param accent    마지막 바퀴(소수 자리)의 바탕색 (#RRGGBB)
     * @return 바퀴 SVG 조각. 창 clip 그룹 안에 넣는다
     * @Since 2026-11-13
     */
    static String render(long runtimeMs, String accent) {
        String digits = digits(runtimeMs);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < WHEEL_COUNT; i++) {
            boolean isAccent = i == WHEEL_COUNT - 1;
            int x = FIRST_WHEEL_X + WHEEL_PITCH * i;
            int steps = FIRST_WHEEL_STEPS + STEPS_PER_WHEEL * i;
            int centis = FIRST_WHEEL_CENTIS + CENTIS_PER_WHEEL * i;
            String dur = String.format(Locale.ROOT, "%d.%02ds", centis / 100, centis % 100);

            sb.append("<rect x=\"%d\" y=\"13\" width=\"21\" height=\"38\" fill=\"%s\"/>"
                    .formatted(x, isAccent ? accent : WHEEL_FILL));
            // 띠를 굴릴 칸 수만큼 내려 둔 데서 시작해 살짝 지나쳤다가(-4) 제자리로 돌아온다.
            sb.append("<g><animateTransform attributeName=\"transform\" type=\"translate\" values=\"0 %d;0 -4;0 0\""
                    .formatted(DIGIT_PITCH * steps));
            sb.append(" keyTimes=\"0;.88;1\" calcMode=\"spline\" keySplines=\".1 .6 .2 1;.4 0 .6 1\" dur=\"%s\" fill=\"freeze\"/>"
                    .formatted(dur));
            int finalDigit = digits.charAt(i) - '0';
            for (int k = 0; k <= steps; k++) {
                // 위로 갈수록 1씩 작은 숫자다. % 는 음수를 돌려주므로 floorMod를 쓴다.
                sb.append("<text x=\"%d.5\" y=\"%d\" font-family=\"%s\" font-size=\"22\" font-weight=\"800\" fill=\"%s\" text-anchor=\"middle\">%d</text>"
                        .formatted(x + 10, REST_BASELINE_Y - DIGIT_PITCH * k, MONO_FONT,
                                isAccent ? ACCENT_DIGIT_FILL : DIGIT_FILL, Math.floorMod(finalDigit - k, 10)));
            }
            sb.append("</g>");
        }
        return sb.toString();
    }
}
