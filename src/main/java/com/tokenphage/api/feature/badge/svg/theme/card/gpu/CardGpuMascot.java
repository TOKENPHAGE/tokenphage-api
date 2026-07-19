package com.tokenphage.api.feature.badge.svg.theme.card.gpu;

/**
 * GPU 마스코트를 활동 레벨(1~5)에 따라 분기 생성한다. ("과열되는 GPU" 컨셉)
 * <p>
 * 레벨이 올라갈수록 코어(팬) 회전이 빨라지고, 과열 글로우가 강해지며, 칩 색이 녹색→황록→주황→빨강으로 변하고,
 * 상단 열파 입자가 늘어난다. 최고 레벨대(Lv4~5)에서는 전기 스파크(픽셀 번개)가 가끔 번쩍인다.
 * CardClaudeMascot과 동일한 levelFor + LEVELS + render 패턴을 따른다.
 */
final class CardGpuMascot {

    private CardGpuMascot() {
    }

    // 누적 토큰 → 레벨 임계값 (CardClaudeMascot과 동일)
    private static final long LV2_MIN = 10_000_000L;    // 10M
    private static final long LV3_MIN = 100_000_000L;   // 100M
    private static final long LV4_MIN = 500_000_000L;   // 500M
    private static final long LV5_MIN = 1_000_000_000L; // 1B

    /**
     * 레벨별로 달라지는 마스코트 파라미터. (glowBlur가 빈 문자열이면 글로우 없음)
     */
    private record GpuLevel(
            String fanDur,      // 코어(팬) 회전 주기
            String ledDur,      // 상태 LED 깜빡임 주기
            String glowBlur,    // 빈 문자열이면 글로우 없음
            String glowColor,
            String main,        // 칩 주색
            String dark,        // 칩 음영색
            String light,       // 칩 하이라이트색
            String mid,         // 칩 중간 프레임색
            int heatCount,      // 상단 열파 입자 개수
            int sparkCount) {   // 전기 스파크(번개) 개수
    }

    // 인덱스 1~5 사용 (0번은 자리표시)
    private static final GpuLevel[] LEVELS = {
            null,
            new GpuLevel("1.3s",  "0.5s",  "",    "",        "#16a34a", "#166534", "#22c55e", "#15803d", 1, 0),
            new GpuLevel("1.0s",  "0.45s", "0.6", "#22c55e", "#16a34a", "#166534", "#22c55e", "#15803d", 2, 0),
            new GpuLevel("0.75s", "0.35s", "1.2", "#84cc16", "#65a30d", "#3f6212", "#84cc16", "#4d7c0f", 3, 0),
            new GpuLevel("0.5s",  "0.28s", "2.0", "#f97316", "#ea580c", "#9a3412", "#f97316", "#c2410c", 4, 1),
            new GpuLevel("0.3s",  "0.2s",  "3.0", "#ef4444", "#dc2626", "#991b1b", "#ef4444", "#b91c1c", 5, 2)
    };

    // 상단 열파 입자 마스터 — 레벨별 앞에서 heatCount개만 사용
    private static final int[][] HEAT_XY = {{22, -6}, {14, -8}, {30, -8}, {8, -6}, {38, -7}};
    private static final String[] HEAT_FILL = {"#f97316", "#ef4444", "#ef4444", "#f97316", "#ef4444"};
    private static final String[] HEAT_OPACITY = {"0.55", "0.5", "0.45", "0.4", "0.4"};

    // 전기 스파크(번개) 마스터 — {x, y, dur, begin, scale}. 레벨별 앞에서 sparkCount개만 사용.
    // x는 마스코트 본체(절대 x 24~76) 위쪽에 걸치도록 잡는다. [0]=우측, [1]=좌측(본체 안쪽).
    private static final String[][] SPARK = {
            {"58", "2", "2.4s", "0s", "1"},
            {"34", "4", "2.0s", "1.1s", "0.85"}
    };

    // 픽셀 번개 스프라이트 4col×7row — {col, row, shade}. shade: 0 기본 / 1 하이라이트 / 2 음영
    private static final int[][] BOLT = {
            {2, 0, 1}, {3, 0, 0},
            {1, 1, 0}, {2, 1, 1},
            {0, 2, 2}, {1, 2, 0},
            {0, 3, 1}, {1, 3, 1}, {2, 3, 1}, {3, 3, 1},
            {2, 4, 0}, {3, 4, 2},
            {1, 5, 1}, {2, 5, 0},
            {0, 6, 2}, {1, 6, 0}
    };
    private static final double BOLT_CELL = 2.5;

    // 마스코트 베이스 SVG (열파 입자 제외 본체). @MAIN@/@DARK@/@LIGHT@/@MID@/@FAN@/@LED@ 는 render()에서 치환한다.
    private static final String BASE = """
            <g transform="translate(24,24)">
              <animateTransform attributeName="transform" type="translate" additive="sum" values="0 0; 0 -2; 0 0" dur="1.2s" repeatCount="indefinite"/>
              <g><animate attributeName="opacity" values="1; 0.2; 1" dur="@LED@" repeatCount="indefinite"/>
                <rect x="40" y="-12" width="2" height="2" fill="@LIGHT@"/></g>
              <g><animate attributeName="opacity" values="0.2; 1; 0.2" dur="@LED@" repeatCount="indefinite"/>
                <rect x="44" y="-12" width="2" height="2" fill="#ef4444"/></g>
              <rect x="4" y="0" width="44" height="4" fill="@MAIN@"/>
              <rect x="0" y="4" width="4" height="24" fill="@MAIN@"/>
              <rect x="48" y="4" width="4" height="24" fill="@MAIN@"/>
              <g><animateTransform attributeName="transform" type="rotate" from="0 16 16" to="360 16 16" dur="@FAN@" repeatCount="indefinite"/>
                <rect x="8" y="8" width="4" height="4" fill="@DARK@"/>
                <rect x="12" y="8" width="4" height="4" fill="@MAIN@"/>
                <rect x="16" y="8" width="4" height="4" fill="@MAIN@"/>
                <rect x="20" y="8" width="4" height="4" fill="@DARK@"/>
                <rect x="8" y="12" width="4" height="4" fill="@MAIN@"/>
                <rect x="12" y="12" width="4" height="4" fill="#f97316"/>
                <rect x="16" y="12" width="4" height="4" fill="#fbbf24"/>
                <rect x="20" y="12" width="4" height="4" fill="@MAIN@"/>
                <rect x="8" y="16" width="4" height="4" fill="@MAIN@"/>
                <rect x="12" y="16" width="4" height="4" fill="#fbbf24"/>
                <rect x="16" y="16" width="4" height="4" fill="#f97316"/>
                <rect x="20" y="16" width="4" height="4" fill="@MAIN@"/>
                <rect x="8" y="20" width="4" height="4" fill="@DARK@"/>
                <rect x="12" y="20" width="4" height="4" fill="@MAIN@"/>
                <rect x="16" y="20" width="4" height="4" fill="@MAIN@"/>
                <rect x="20" y="20" width="4" height="4" fill="@DARK@"/>
              </g>
              <rect x="28" y="8" width="4" height="4" fill="@LIGHT@"/>
              <rect x="36" y="8" width="4" height="4" fill="#ef4444"/>
              <rect x="32" y="12" width="4" height="4" fill="@MID@"/>
              <rect x="40" y="12" width="4" height="4" fill="@MID@"/>
              <rect x="32" y="16" width="4" height="4" fill="@MID@"/>
              <rect x="40" y="16" width="4" height="4" fill="@MID@"/>
              <rect x="28" y="20" width="4" height="4" fill="@LIGHT@"/>
              <rect x="36" y="20" width="4" height="4" fill="#ef4444"/>
              <rect x="4" y="28" width="44" height="4" fill="@MID@"/>
              <rect x="8" y="32" width="4" height="4" fill="#fbbf24"/>
              <rect x="16" y="32" width="4" height="4" fill="#fbbf24"/>
              <rect x="24" y="32" width="4" height="4" fill="#fbbf24"/>
              <rect x="32" y="32" width="4" height="4" fill="#fbbf24"/>
              <rect x="40" y="32" width="4" height="4" fill="#fbbf24"/>
              <g><animateTransform attributeName="transform" type="translate" values="0 0; 1 -1; 0 0" dur="0.5s" repeatCount="indefinite"/>
                <rect x="56" y="4" width="4" height="4" fill="#fbbf24"/>
                <rect x="56" y="8" width="4" height="4" fill="#fde047"/>
                <rect x="60" y="8" width="4" height="4" fill="#fbbf24"/>
                <rect x="52" y="12" width="4" height="4" fill="#fbbf24"/>
                <rect x="56" y="12" width="4" height="4" fill="#fde047"/>
                <rect x="52" y="16" width="4" height="4" fill="#fbbf24"/>
                <rect x="56" y="16" width="4" height="4" fill="#fde047"/>
                <rect x="56" y="20" width="4" height="4" fill="#fbbf24"/>
              </g>
            </g>""";

    /**
     * 누적 토큰 수로 마스코트 활동 레벨(1~5)을 구한다.
     *
     * @param totalTokens 누적 토큰 수
     * @return 1(낮음) ~ 5(최고)
     * @Since 2026-06-16
     */
    static int levelFor(long totalTokens) {
        if (totalTokens < LV2_MIN) {
            return 1;
        }
        if (totalTokens < LV3_MIN) {
            return 2;
        }
        if (totalTokens < LV4_MIN) {
            return 3;
        }
        if (totalTokens < LV5_MIN) {
            return 4;
        }
        return 5;
    }

    /**
     * 활동 레벨에 맞는 GPU 마스코트 SVG 조각을 생성한다.
     *
     * @param level  활동 레벨 (1~5)
     * @param isDark 다크 모드 여부 (글로우 필터 id 충돌 방지용)
     * @return GPU 마스코트 SVG 조각
     * @Since 2026-06-16
     */
    static String render(int level, boolean isDark) {
        GpuLevel lv = LEVELS[level];
        boolean hasGlow = !lv.glowBlur().isBlank();
        String filterId = "glow-gpu-l" + level + (isDark ? "d" : "l");

        String body = paint(BASE, lv);
        String heat = heatParticles(lv);

        StringBuilder sb = new StringBuilder();
        sb.append("<g>");
        if (hasGlow) {
            sb.append(glowFilter(filterId, lv));
            sb.append("<g filter=\"url(#").append(filterId).append(")\">");
        }
        sb.append(heat);
        sb.append(body);
        if (hasGlow) {
            sb.append("</g>");
        }
        sb.append(sparks(lv, isDark));
        sb.append("</g>");
        return sb.toString();
    }

    // 베이스 SVG의 색·속도 placeholder를 레벨값으로 치환한다.
    private static String paint(String tpl, GpuLevel lv) {
        return tpl
                .replace("@MAIN@", lv.main())
                .replace("@DARK@", lv.dark())
                .replace("@LIGHT@", lv.light())
                .replace("@MID@", lv.mid())
                .replace("@FAN@", lv.fanDur())
                .replace("@LED@", lv.ledDur());
    }

    // 과열 글로우 필터 (CardClaudeMascot.glowFilter 미러)
    private static String glowFilter(String id, GpuLevel lv) {
        return """
                <filter id="%s" x="-60%%" y="-60%%" width="220%%" height="220%%">
                <feGaussianBlur stdDeviation="%s" result="blur"/>
                <feFlood flood-color="%s" flood-opacity="0.85" result="color"/>
                <feComposite in="color" in2="blur" operator="in" result="glow"/>
                <feMerge><feMergeNode in="glow"/><feMergeNode in="SourceGraphic"/></feMerge>
                </filter>
                """.formatted(id, lv.glowBlur(), lv.glowColor());
    }

    // 상단 열파 입자 — heatCount개. 마스코트와 같은 translate(24,24) 좌표계에 떠오르는 애니메이션.
    private static String heatParticles(GpuLevel lv) {
        StringBuilder items = new StringBuilder();
        for (int i = 0; i < lv.heatCount(); i++) {
            items.append("<rect class=\"gpu-heat\" x=\"%d\" y=\"%d\" width=\"4\" height=\"4\" fill=\"%s\" opacity=\"%s\"/>"
                    .formatted(HEAT_XY[i][0], HEAT_XY[i][1], HEAT_FILL[i], HEAT_OPACITY[i]));
        }
        return """
                <g transform="translate(24,24)"><g>
                <animateTransform attributeName="transform" type="translate" values="0 0; 0 -2; 0 0" dur="1s" repeatCount="indefinite"/>
                %s</g></g>""".formatted(items);
    }

    // 전기 스파크(픽셀 번개) — sparkCount개. opacity 주기로 "가끔 번쩍"(values 0;0;1;1;0;0).
    // 노란 번개는 dark 배경에선 잘 보이지만 light(흰) 배경에선 대비가 약하므로,
    // 어두운 아웃라인(feMorphology dilate)을 입혀 양쪽 배경에서 윤곽이 드러나게 한다.
    private static String sparks(GpuLevel lv, boolean isDark) {
        if (lv.sparkCount() == 0) {
            return "";
        }
        String outlineId = "bolt-ol-" + (isDark ? "d" : "l");
        String bolt = boltPixels();
        StringBuilder sb = new StringBuilder();
        sb.append(boltOutlineFilter(outlineId));
        for (int i = 0; i < lv.sparkCount(); i++) {
            String[] s = SPARK[i];
            sb.append("""
                    <g class="gpu-spark" transform="translate(%s,%s) scale(%s)" filter="url(#%s)" shape-rendering="crispEdges">
                    <animate attributeName="opacity" calcMode="linear" values="0;0;1;1;0;0" keyTimes="0;0.42;0.48;0.56;0.62;1" dur="%s" begin="%s" repeatCount="indefinite"/>
                    %s</g>
                    """.formatted(s[0], s[1], s[4], outlineId, s[2], s[3], bolt));
        }
        return sb.toString();
    }

    // 번개 픽셀 실루엣을 한 칸 확장해 어두운 외곽선을 만든다 (light 배경 가시성 확보).
    private static String boltOutlineFilter(String id) {
        return """
                <filter id="%s" x="-50%%" y="-50%%" width="200%%" height="200%%">
                <feMorphology operator="dilate" radius="0.7" in="SourceAlpha" result="dil"/>
                <feFlood flood-color="#7c2d12" result="c"/>
                <feComposite in="c" in2="dil" operator="in" result="ol"/>
                <feMerge><feMergeNode in="ol"/><feMergeNode in="SourceGraphic"/></feMerge>
                </filter>
                """.formatted(id);
    }

    // 픽셀 번개 셀들을 rect로 조립한다.
    private static String boltPixels() {
        StringBuilder sb = new StringBuilder();
        for (int[] c : BOLT) {
            String fill = c[2] == 1 ? "#fef08a" : c[2] == 2 ? "#eab308" : "#fde047";
            sb.append("<rect x=\"%s\" y=\"%s\" width=\"2.5\" height=\"2.5\" fill=\"%s\"/>"
                    .formatted(c[0] * BOLT_CELL, c[1] * BOLT_CELL, fill));
        }
        return sb.toString();
    }
}
