package com.tokenphage.api.feature.badge.svg.theme.odometer;

/**
 * 주행거리계 배지의 모드별 색.
 *
 * @param fg           라벨 AGENT와 단위 h의 글자색
 * @param windowStroke 숫자 창 테두리색
 * @param accent       테마 강조색. 마지막 바퀴(소수 자리)와 dark 몸통 테두리에 쓴다
 * @param tint         라벨 RUNTIME의 글자색. 강조색을 배경에 맞춰 밝히거나 어둡게 한 색이다
 */
public record OdometerColors(String fg, String windowStroke, String accent, String tint) {

    /**
     * dark 모드 색을 만든다. 글자색과 창 테두리색은 테마와 무관하게 같다.
     *
     * @param accent 테마 강조색 (#RRGGBB)
     * @param tint   dark 배경용 RUNTIME 라벨색 (#RRGGBB)
     * @return dark 모드 색
     * @Since 2026-11-13
     */
    public static OdometerColors dark(String accent, String tint) {
        return new OdometerColors("#ffffff", "#444b58", accent, tint);
    }

    /**
     * light 모드 색을 만든다. 글자색과 창 테두리색은 테마와 무관하게 같다.
     *
     * @param accent 테마 강조색 (#RRGGBB)
     * @param tint   light 배경용 RUNTIME 라벨색 (#RRGGBB)
     * @return light 모드 색
     * @Since 2026-11-13
     */
    public static OdometerColors light(String accent, String tint) {
        return new OdometerColors("#111827", "#111827", accent, tint);
    }
}
