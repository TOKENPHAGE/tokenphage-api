package com.tokenphage.api.feature.badge.svg.theme.betatester;

import com.tokenphage.api.feature.badge.svg.BadgeMode;

/**
 * 터미널 배지 팔레트 (badge-{basic,green,purple}.svg.txt 프로토타입 실측).
 * <p>
 * mascotGlow는 필터 문자열 전체 — 세 모드가 같은 글로우 수치를 쓰고 색만 다르다.
 */
public record BetaTesterColors(
        String bg, String titleBar, String border, String divider,
        String textPrimary, String textSecondary, String textMuted, String textBody,
        String accent, String mascotGlow) {

    public static final BetaTesterColors CYAN = new BetaTesterColors(
            "#1E1E1E", "#2D2D2F", "#48484A", "#3A3A3C",
            "#F5F5F7", "#98989D", "#7C7C82", "#C7C7CC",
            "#64D2FF", "drop-shadow(0 0 2px rgba(245,245,247,.3)) drop-shadow(0 0 4px rgba(245,245,247,.2))");

    public static final BetaTesterColors GREEN = new BetaTesterColors(
            "#1E1E1E", "#2D2D2F", "#48484A", "#3A3A3C",
            "#F5F5F7", "#98989D", "#7C7C82", "#C7C7CC",
            "#64DC82", "drop-shadow(0 0 2px rgba(100,220,130,.3)) drop-shadow(0 0 4px rgba(100,220,130,.2))");

    public static final BetaTesterColors PURPLE = new BetaTesterColors(
            "#1E1E1E", "#2D2D2F", "#48484A", "#3A3A3C",
            "#F5F5F7", "#98989D", "#7C7C82", "#C7C7CC",
            "#D08BF5", "drop-shadow(0 0 2px rgba(208,139,245,.3)) drop-shadow(0 0 4px rgba(208,139,245,.2))");

    /**
     * 모드에 대응하는 팔레트를 반환한다. 악센트 3종 밖의 모드는 CYAN으로 접는다.
     *
     * @param mode 색상 모드 (null 불허)
     * @return 모드 팔레트
     * @Since 2026-08-23
     */
    public static BetaTesterColors forMode(BadgeMode mode) {
        return switch (mode) {
            case GREEN -> GREEN;
            case PURPLE -> PURPLE;
            default -> CYAN;
        };
    }
}
