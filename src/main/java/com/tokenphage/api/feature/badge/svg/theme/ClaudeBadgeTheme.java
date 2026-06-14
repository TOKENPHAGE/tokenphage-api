package com.tokenphage.api.feature.badge.svg.theme;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeColors;
import com.tokenphage.api.feature.badge.svg.BaseBadgeTheme;
import org.springframework.stereotype.Component;

@Component
public class ClaudeBadgeTheme extends BaseBadgeTheme {

    // 누적 토큰 숫자 그라데이션 stop (모든 레벨·모드 공통)
    private static final String GRAD_STOPS =
            "<stop offset=\"0%\" stop-color=\"#FFC85E\"/>"
                    + "<stop offset=\"40%\" stop-color=\"#FF8A3D\"/>"
                    + "<stop offset=\"100%\" stop-color=\"#D9534F\"/>";

    // 히트바 오렌지 팔레트 (낮음→높음)
    private static final String[] HEAT_LIGHT = {"#ffe7d8", "#f9c39a", "#ed976d", "#D97757", "#8a3a1f"};
    private static final String[] HEAT_DARK = {"#3a1e10", "#6b3a1f", "#a55733", "#D97757", "#ffb38d"};

    /**
     * Claude 테마 식별자를 반환한다.
     *
     * @return "claude"
     * @Since 2026-05-27
     */
    @Override
    public String name() {
        return "claude";
    }

    /**
     * 누적 토큰으로 결정한 활동 레벨의 Claude 마스코트 SVG 조각을 반환한다.
     * <p>
     * 팝업(Lv4·5)에 표시할 값은 히트바 맨 오른쪽(최근 1일) 토큰량을 사용한다.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark 색상 모드 (글로우 필터 id 충돌 방지용)
     * @return Claude 마스코트 SVG 조각
     * @Since 2026-05-31
     */
    @Override
    protected String mascot(BadgeResponse data, boolean isDark) {
        int level = ClaudeMascot.levelFor(data.totalTokens());
        long recentDayTokens = data.heatbar().isEmpty()
                ? 0L
                : data.heatbar().getLast().total();
        return ClaudeMascot.render(level, recentDayTokens, isDark);
    }

    /**
     * 활동 레벨의 대표 도서 대비 누적 토큰 비교 캡션을 반환한다. (예: "Bible 609권 갉아먹는 중")
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @return "{도서명} {배수}권 갉아먹는 중" 형태의 캡션
     * @Since 2026-06-01
     */
    @Override
    protected String subCaption(BadgeResponse data) {
        int level = ClaudeMascot.levelFor(data.totalTokens());
        return TokenBookScale.describe(level, data.totalTokens());
    }

    /**
     * 누적 토큰 숫자에 쓸 그라데이션 정의를 반환한다.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark 색상 모드
     * @return linearGradient를 담은 defs 조각
     * @Since 2026-05-31
     */
    @Override
    protected String extraDefs(BadgeResponse data, boolean isDark) {
        return "<defs><linearGradient id=\"%s\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">%s</linearGradient></defs>"
                .formatted(gradientId(data, isDark), GRAD_STOPS);
    }

    /**
     * 누적 토큰 숫자의 fill을 그라데이션 참조로 반환한다.
     *
     * @param data      배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark    색상 모드
     * @param modeColor 현재 색상 팔레트 (미사용)
     * @return url(#gradientId)
     * @Since 2026-05-31
     */
    @Override
    protected String tokenFill(BadgeResponse data, boolean isDark, BadgeColors modeColor) {
        return "url(#%s)".formatted(gradientId(data, isDark));
    }

    /**
     * 히트바 한 칸의 색을 Claude 오렌지 팔레트에서 비율에 따라 반환한다.
     *
     * @param val       해당 일의 토큰 수
     * @param max       30일 중 최대 토큰 수
     * @param isDark    색상 모드
     * @param modeColor 기본 색상 팔레트 (미사용)
     * @return 막대 fill 색
     * @Since 2026-05-31
     */
    @Override
    protected String heatColor(long val, long max, boolean isDark, BadgeColors modeColor) {
        String[] palette = isDark ? HEAT_DARK : HEAT_LIGHT;
        if (max == 0 || val == 0) {
            return palette[0];
        }
        double ratio = (double) val / max;
        int idx;
        if (ratio < 0.2) {
            idx = 0;
        } else if (ratio < 0.4) {
            idx = 1;
        } else if (ratio < 0.6) {
            idx = 2;
        } else if (ratio < 0.8) {
            idx = 3;
        } else {
            idx = 4;
        }
        return palette[idx];
    }

    private String gradientId(BadgeResponse data, boolean isDark) {
        int level = ClaudeMascot.levelFor(data.totalTokens());
        return "totalGrad-" + (isDark ? "dark" : "light") + "-" + level;
    }
}
