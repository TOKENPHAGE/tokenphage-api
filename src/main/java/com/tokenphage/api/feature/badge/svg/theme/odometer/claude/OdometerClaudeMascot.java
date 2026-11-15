package com.tokenphage.api.feature.badge.svg.theme.odometer.claude;

import java.util.List;

/**
 * 주행거리계 배지의 Claude 마스코트.
 * <p>
 * 바퀴가 도는 동안 달리고, 멈추면 한 번 점프한 뒤 숨쉬기·곁눈질·깜빡임을 반복한다.
 * 프레임 rect와 타이밍은 프로토타입 추출값이다. 손으로 수정하지 않는다.
 * animateTransform은 요소당 하나만 적용되므로 모션마다 그룹을 하나씩 겹친다.
 */
final class OdometerClaudeMascot {

    /** 서 있는 몸통 (rect 62개, 좌우 대칭). */
    static final String STAND = """
            <rect x="4" y="0" width="4" height="1" fill="#FFFFFF"/><rect x="10" y="0" width="4" height="1" fill="#FFFFFF"/>\
            <rect x="4" y="1" width="1" height="1" fill="#FFFFFF"/><rect x="5" y="1" width="2" height="1" fill="#D97757"/>\
            <rect x="7" y="1" width="1" height="1" fill="#FFFFFF"/><rect x="10" y="1" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="11" y="1" width="2" height="1" fill="#D97757"/><rect x="13" y="1" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="2" y="2" width="3" height="1" fill="#FFFFFF"/><rect x="5" y="2" width="2" height="1" fill="#D97757"/>\
            <rect x="7" y="2" width="4" height="1" fill="#FFFFFF"/><rect x="11" y="2" width="2" height="1" fill="#D97757"/>\
            <rect x="13" y="2" width="3" height="1" fill="#FFFFFF"/><rect x="1" y="3" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="3" y="3" width="12" height="1" fill="#D97757"/><rect x="15" y="3" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="4" width="2" height="1" fill="#FFFFFF"/><rect x="2" y="4" width="14" height="1" fill="#D97757"/>\
            <rect x="16" y="4" width="2" height="1" fill="#FFFFFF"/><rect x="0" y="5" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="5" width="16" height="1" fill="#D97757"/><rect x="17" y="5" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="6" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="6" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="6" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="7" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="7" width="16" height="1" fill="#D97757"/><rect x="17" y="7" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="8" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="8" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="8" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="9" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="9" width="16" height="1" fill="#D97757"/><rect x="17" y="9" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="10" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="10" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="10" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="11" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="11" width="16" height="1" fill="#D97757"/><rect x="17" y="11" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="12" width="2" height="1" fill="#FFFFFF"/><rect x="2" y="12" width="14" height="1" fill="#D97757"/>\
            <rect x="16" y="12" width="2" height="1" fill="#FFFFFF"/><rect x="1" y="13" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="2" y="13" width="2" height="1" fill="#D97757"/><rect x="4" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="6" y="13" width="2" height="1" fill="#D97757"/><rect x="8" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="10" y="13" width="2" height="1" fill="#D97757"/><rect x="12" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="14" y="13" width="2" height="1" fill="#D97757"/><rect x="16" y="13" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="14" width="1" height="1" fill="#FFFFFF"/><rect x="2" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="4" y="14" width="2" height="1" fill="#FFFFFF"/><rect x="6" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="8" y="14" width="2" height="1" fill="#FFFFFF"/><rect x="10" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="12" y="14" width="2" height="1" fill="#FFFFFF"/><rect x="14" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="16" y="14" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="15" width="16" height="1" fill="#FFFFFF"/>""";

    /** 달리기 프레임 A (rect 59개). 혼자서는 비대칭이고 {@link #RUN_B}의 좌우 반전이다. */
    static final String RUN_A = """
            <rect x="4" y="0" width="4" height="1" fill="#FFFFFF"/><rect x="10" y="0" width="4" height="1" fill="#FFFFFF"/>\
            <rect x="4" y="1" width="1" height="1" fill="#FFFFFF"/><rect x="5" y="1" width="2" height="1" fill="#D97757"/>\
            <rect x="7" y="1" width="1" height="1" fill="#FFFFFF"/><rect x="10" y="1" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="11" y="1" width="2" height="1" fill="#D97757"/><rect x="13" y="1" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="2" y="2" width="3" height="1" fill="#FFFFFF"/><rect x="5" y="2" width="2" height="1" fill="#D97757"/>\
            <rect x="7" y="2" width="4" height="1" fill="#FFFFFF"/><rect x="11" y="2" width="2" height="1" fill="#D97757"/>\
            <rect x="13" y="2" width="3" height="1" fill="#FFFFFF"/><rect x="1" y="3" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="3" y="3" width="12" height="1" fill="#D97757"/><rect x="15" y="3" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="4" width="2" height="1" fill="#FFFFFF"/><rect x="2" y="4" width="14" height="1" fill="#D97757"/>\
            <rect x="16" y="4" width="2" height="1" fill="#FFFFFF"/><rect x="0" y="5" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="5" width="16" height="1" fill="#D97757"/><rect x="17" y="5" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="6" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="6" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="6" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="7" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="7" width="16" height="1" fill="#D97757"/><rect x="17" y="7" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="8" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="8" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="8" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="9" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="9" width="16" height="1" fill="#D97757"/><rect x="17" y="9" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="10" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="10" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="10" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="11" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="11" width="16" height="1" fill="#D97757"/><rect x="17" y="11" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="12" width="2" height="1" fill="#FFFFFF"/><rect x="2" y="12" width="14" height="1" fill="#D97757"/>\
            <rect x="16" y="12" width="2" height="1" fill="#FFFFFF"/><rect x="1" y="13" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="2" y="13" width="2" height="1" fill="#D97757"/><rect x="4" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="6" y="13" width="2" height="1" fill="#D97757"/><rect x="8" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="10" y="13" width="2" height="1" fill="#D97757"/><rect x="12" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="14" y="13" width="2" height="1" fill="#D97757"/><rect x="16" y="13" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="14" width="5" height="1" fill="#FFFFFF"/><rect x="6" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="8" y="14" width="6" height="1" fill="#FFFFFF"/><rect x="14" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="16" y="14" width="1" height="1" fill="#FFFFFF"/><rect x="5" y="15" width="4" height="1" fill="#FFFFFF"/>\
            <rect x="13" y="15" width="4" height="1" fill="#FFFFFF"/>""";

    /** 달리기 프레임 B (rect 59개). {@link #RUN_A}의 좌우 반전이다. */
    static final String RUN_B = """
            <rect x="4" y="0" width="4" height="1" fill="#FFFFFF"/><rect x="10" y="0" width="4" height="1" fill="#FFFFFF"/>\
            <rect x="4" y="1" width="1" height="1" fill="#FFFFFF"/><rect x="5" y="1" width="2" height="1" fill="#D97757"/>\
            <rect x="7" y="1" width="1" height="1" fill="#FFFFFF"/><rect x="10" y="1" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="11" y="1" width="2" height="1" fill="#D97757"/><rect x="13" y="1" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="2" y="2" width="3" height="1" fill="#FFFFFF"/><rect x="5" y="2" width="2" height="1" fill="#D97757"/>\
            <rect x="7" y="2" width="4" height="1" fill="#FFFFFF"/><rect x="11" y="2" width="2" height="1" fill="#D97757"/>\
            <rect x="13" y="2" width="3" height="1" fill="#FFFFFF"/><rect x="1" y="3" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="3" y="3" width="12" height="1" fill="#D97757"/><rect x="15" y="3" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="4" width="2" height="1" fill="#FFFFFF"/><rect x="2" y="4" width="14" height="1" fill="#D97757"/>\
            <rect x="16" y="4" width="2" height="1" fill="#FFFFFF"/><rect x="0" y="5" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="5" width="16" height="1" fill="#D97757"/><rect x="17" y="5" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="6" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="6" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="6" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="7" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="7" width="16" height="1" fill="#D97757"/><rect x="17" y="7" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="8" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="8" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="8" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="9" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="9" width="16" height="1" fill="#D97757"/><rect x="17" y="9" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="10" width="1" height="1" fill="#FFFFFF"/><rect x="1" y="10" width="16" height="1" fill="#D97757"/>\
            <rect x="17" y="10" width="1" height="1" fill="#FFFFFF"/><rect x="0" y="11" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="11" width="16" height="1" fill="#D97757"/><rect x="17" y="11" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="0" y="12" width="2" height="1" fill="#FFFFFF"/><rect x="2" y="12" width="14" height="1" fill="#D97757"/>\
            <rect x="16" y="12" width="2" height="1" fill="#FFFFFF"/><rect x="1" y="13" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="2" y="13" width="2" height="1" fill="#D97757"/><rect x="4" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="6" y="13" width="2" height="1" fill="#D97757"/><rect x="8" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="10" y="13" width="2" height="1" fill="#D97757"/><rect x="12" y="13" width="2" height="1" fill="#FFFFFF"/>\
            <rect x="14" y="13" width="2" height="1" fill="#D97757"/><rect x="16" y="13" width="1" height="1" fill="#FFFFFF"/>\
            <rect x="1" y="14" width="1" height="1" fill="#FFFFFF"/><rect x="2" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="4" y="14" width="6" height="1" fill="#FFFFFF"/><rect x="10" y="14" width="2" height="1" fill="#D97757"/>\
            <rect x="12" y="14" width="5" height="1" fill="#FFFFFF"/><rect x="1" y="15" width="4" height="1" fill="#FFFFFF"/>\
            <rect x="9" y="15" width="4" height="1" fill="#FFFFFF"/>""";

    /** 뜬 눈 (rect 4개). 기본 표정이다. */
    static final String EYES_OPEN = """
            <rect x="5" y="7" width="2" height="1" fill="#1A0F0A"/><rect x="11" y="7" width="2" height="1" fill="#1A0F0A"/>\
            <rect x="5" y="8" width="2" height="1" fill="#1A0F0A"/><rect x="11" y="8" width="2" height="1" fill="#1A0F0A"/>""";

    /** 감은 눈 (rect 2개). 달리는 동안과 깜빡일 때 쓴다. */
    static final String EYES_CLOSED = """
            <rect x="5" y="8" width="2" height="1" fill="#1A0F0A"/><rect x="11" y="8" width="2" height="1" fill="#1A0F0A"/>""";

    /** {@code > <} 눈 (rect 10개). 점프할 때 쓴다. */
    static final String EYES_JOY = """
            <rect x="3" y="7" width="2" height="1" fill="#1A0F0A"/><rect x="13" y="7" width="2" height="1" fill="#1A0F0A"/>\
            <rect x="4" y="8" width="2" height="1" fill="#1A0F0A"/><rect x="12" y="8" width="2" height="1" fill="#1A0F0A"/>\
            <rect x="5" y="9" width="2" height="1" fill="#1A0F0A"/><rect x="11" y="9" width="2" height="1" fill="#1A0F0A"/>\
            <rect x="4" y="10" width="2" height="1" fill="#1A0F0A"/><rect x="12" y="10" width="2" height="1" fill="#1A0F0A"/>\
            <rect x="3" y="11" width="2" height="1" fill="#1A0F0A"/><rect x="13" y="11" width="2" height="1" fill="#1A0F0A"/>""";

    /** 점프할 때 머리 주변에서 차례로 터지는 반짝이 3개 (rect 3개씩). 몸통 밖이라 좌표에 음수가 있다. */
    static final List<String> SPARKLES = List.of(
            """
                    <rect x="-3" y="1" width="1" height="3" fill="#FFD234"/><rect x="-4" y="2" width="3" height="1" fill="#FFD234"/>\
                    <rect x="-3" y="2" width="1" height="1" fill="#FFF6B0"/>""",
            """
                    <rect x="19" y="-3" width="1" height="3" fill="#FFD234"/><rect x="18" y="-2" width="3" height="1" fill="#FFD234"/>\
                    <rect x="19" y="-2" width="1" height="1" fill="#FFF6B0"/>""",
            """
                    <rect x="15" y="-7" width="1" height="3" fill="#FFD234"/><rect x="14" y="-6" width="3" height="1" fill="#FFD234"/>\
                    <rect x="15" y="-6" width="1" height="1" fill="#FFF6B0"/>""");

    /** 반짝이가 터지는 시각. 점프 시작(3.1초)부터 0.09초 간격이다. */
    private static final List<String> SPARKLE_BEGINS = List.of("3.10s", "3.19s", "3.28s");

    /** 달리는 2.6초 동안 숨긴다 (서 있는 몸통, 뜬 눈). */
    private static final String HIDE_WHILE_RUNNING =
            "<set attributeName=\"visibility\" to=\"hidden\" begin=\"0s\" dur=\"2.6s\"/>";

    private OdometerClaudeMascot() {
    }

    /**
     * 마스코트 SVG 조각을 반환한다. 가동시간·색상 모드와 무관하게 같은 그림이다.
     *
     * @return 배지 왼쪽(12, 16)에 2배로 놓이는 마스코트 SVG 조각
     * @Since 2026-11-13
     */
    static String render() {
        StringBuilder sb = new StringBuilder();
        sb.append("<g transform=\"translate(12 16) scale(2)\" shape-rendering=\"crispEdges\">");

        // 발(y=16)을 축으로 기울인다. 달리는 동안 앞으로 숙였다가(-14) 멈추며 뒤로 젖히고(9) 바로 선다.
        sb.append("<g transform=\"translate(0 16)\"><g>");
        sb.append("<animateTransform attributeName=\"transform\" type=\"skewX\" values=\"-14;-14;9;0\" keyTimes=\"0;.84;.92;1\" dur=\"3.1s\"/>");
        sb.append("<g transform=\"translate(0 -16)\">");

        // 달리는 동안 0.2초마다 한 픽셀씩 흔든다 (13회 = 2.6초).
        sb.append("<g><animateTransform attributeName=\"transform\" type=\"translate\" calcMode=\"discrete\" values=\"0 0;0 -1\" dur=\"0.2s\" repeatCount=\"13\"/>");
        // 바로 선 순간(3.1초) 한 번 뛰어오른다.
        sb.append("<g><animateTransform attributeName=\"transform\" type=\"translate\" values=\"0 0;0 -4;0 0\" keyTimes=\"0;.45;1\""
                + " calcMode=\"spline\" keySplines=\".2 .7 .4 1;.6 0 .8 .4\" begin=\"3.1s\" dur=\"0.4s\"/>");
        // 착지한 뒤(3.6초~)에는 숨쉬듯 천천히 오르내린다.
        sb.append("<g><animateTransform attributeName=\"transform\" type=\"translate\" calcMode=\"discrete\" values=\"0 0;0 -1\""
                + " dur=\"1.6s\" begin=\"3.6s\" repeatCount=\"indefinite\"/>");

        // 몸통: 달리는 동안 서 있는 몸통을 숨기고 A·B를 번갈아 보여준다.
        sb.append("<g>").append(HIDE_WHILE_RUNNING).append(STAND).append("</g>");
        sb.append(runFrame("visible;hidden", RUN_A));
        sb.append(runFrame("hidden;visible", RUN_B));

        // 눈: 4초부터 5초마다 옆을 한 번 흘끗 본다.
        sb.append("<g><animateTransform attributeName=\"transform\" type=\"translate\" calcMode=\"discrete\" values=\"0 0;1 0;0 0\""
                + " keyTimes=\"0;.7;.9\" dur=\"5s\" begin=\"4s\" repeatCount=\"indefinite\"/>");
        sb.append("<g>").append(HIDE_WHILE_RUNNING)
                .append("<set attributeName=\"visibility\" to=\"hidden\" begin=\"3.1s\" dur=\"1.4s\"/>")
                .append(blink("visible;hidden;visible")).append(EYES_OPEN).append("</g>");
        sb.append("<g visibility=\"hidden\"><set attributeName=\"visibility\" to=\"visible\" begin=\"0s\" dur=\"2.6s\"/>")
                .append(blink("hidden;visible;hidden")).append(EYES_CLOSED).append("</g>");
        sb.append("<g visibility=\"hidden\"><set attributeName=\"visibility\" to=\"visible\" begin=\"3.1s\" dur=\"1.4s\"/>")
                .append(EYES_JOY).append("</g>");
        sb.append("</g>");

        // 숨쉬기 · 점프 · 달리기 흔들림 · 기울기 그룹을 닫는다.
        sb.append("</g></g></g></g></g></g>");

        for (int i = 0; i < SPARKLES.size(); i++) {
            sb.append("<g opacity=\"0\"><animate attributeName=\"opacity\" values=\"0;1;1;0\" keyTimes=\"0;.2;.6;1\" dur=\"0.7s\" begin=\"%s\"/>"
                    .formatted(SPARKLE_BEGINS.get(i)));
            sb.append(SPARKLES.get(i)).append("</g>");
        }

        sb.append("</g>");
        return sb.toString();
    }

    // 달리기 프레임 하나. 기본은 숨김이고 달리는 동안만 0.2초 주기의 절반씩 보인다.
    private static String runFrame(String visibility, String frame) {
        return "<g visibility=\"hidden\"><animate attributeName=\"visibility\" calcMode=\"discrete\" values=\"%s\" dur=\"0.2s\" repeatCount=\"13\"/>%s</g>"
                .formatted(visibility, frame);
    }

    // 4.5초부터 4.2초마다 한 번 깜빡인다. 뜬 눈과 감은 눈이 서로 반대 값으로 같은 박자를 쓴다.
    private static String blink(String visibility) {
        return "<animate attributeName=\"visibility\" calcMode=\"discrete\" values=\"%s\" keyTimes=\"0;.93;.97\" dur=\"4.2s\" begin=\"4.50s\" repeatCount=\"indefinite\"/>"
                .formatted(visibility);
    }
}
