package com.tokenphage.api.feature.badge.svg.theme.grass;

/**
 * grass 계열 뱃지의 팔레트 (프로토타입 실측값).
 * <p>
 * 잔디 5단계(grid1 연함 → grid5 진함)와 크롬(배경·텍스트·구분선)·하늘 색을 담는다.
 * dark의 잔디 순서는 추출 스크립트(.dev/grass-svg-extract.py)가 light↔dark 동일 좌표
 * 매핑으로 확정한 값이다. 테마가 색을 바꾸려면 GrassBadgeTheme.colors()를 오버라이드한다.
 *
 * @param bg            카드 배경
 * @param border        카드 테두리
 * @param textPrimary   기본 텍스트 (@username)
 * @param textSecondary 보조 텍스트 (라벨·total 문구)
 * @param accent        연간 토큰 숫자 강조색
 * @param divider       구분선·하늘 테두리
 * @param skyBg         하늘 놀이터 배경
 * @param groundStrip   잔디띠 (하늘 하단)
 * @param gridEmpty     잔디 empty 셀
 * @param grid1         잔디 레벨 1 (가장 연함)
 * @param grid2         잔디 레벨 2
 * @param grid3         잔디 레벨 3
 * @param grid4         잔디 레벨 4
 * @param grid5         잔디 레벨 5 (가장 진함)
 */
public record GrassColors(
        String bg, String border, String textPrimary, String textSecondary, String accent,
        String divider, String skyBg, String groundStrip, String gridEmpty,
        String grid1, String grid2, String grid3, String grid4, String grid5) {

    public static final GrassColors LIGHT = new GrassColors(
            "#ffffff", "#e5e7eb", "#1a1a1a", "#6b7280", "#D97757",
            "#e5e7eb", "#eaf5ff", "#cdeaa8", "#e9e4db",
            "#f6c9a8", "#ec9463", "#D97757", "#b8562f", "#8a3a1f");

    public static final GrassColors DARK = new GrassColors(
            "#0f1526", "#243049", "#e8e6f2", "#8a93ad", "#ff9d6c",
            "#243049", "#1a2440", "#233152", "#1c2536",
            "#5a2f1e", "#8a3a1f", "#c05a30", "#e58a5f", "#f6c9a8");

    /**
     * 잔디 강도 레벨(0..5)의 셀 색을 반환한다.
     *
     * @param level GrassGrid.levelFor 결과 (0=empty, 1..5)
     * @return 셀 fill 색
     * @Since 2026-07-15
     */
    public String gridColor(int level) {
        return switch (level) {
            case 1 -> grid1;
            case 2 -> grid2;
            case 3 -> grid3;
            case 4 -> grid4;
            case 5 -> grid5;
            default -> gridEmpty;
        };
    }
}
