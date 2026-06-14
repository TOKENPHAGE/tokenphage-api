package com.tokenphage.api.feature.badge.svg.theme;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeColors;
import com.tokenphage.api.feature.badge.svg.BaseBadgeTheme;
import org.springframework.stereotype.Component;

@Component
public class GpuBadgeTheme extends BaseBadgeTheme {

    // 누적 토큰 숫자 그라데이션 stop (모든 레벨·모드 공통).
    // 30일 히트바(BadgeColors 기본 블루 heat 팔레트)와 통일한 블루 톤.
    private static final String GRAD_STOPS =
            "<stop offset=\"0%\" stop-color=\"#60a5fa\"/>"
                    + "<stop offset=\"45%\" stop-color=\"#3b82f6\"/>"
                    + "<stop offset=\"100%\" stop-color=\"#1e40af\"/>";

    /**
     * GPU 테마 식별자를 반환한다.
     *
     * @return "gpu"
     * @Since 2026-05-27
     */
    @Override
    public String name() {
        return "gpu";
    }

    /**
     * 누적 토큰으로 결정한 활동 레벨의 GPU 마스코트 SVG 조각을 반환한다.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark 색상 모드 (글로우 필터 id 충돌 방지용)
     * @return GPU 마스코트 SVG 조각
     * @Since 2026-06-16
     */
    @Override
    protected String mascot(BadgeResponse data, boolean isDark) {
        int level = GpuMascot.levelFor(data.totalTokens());
        return GpuMascot.render(level, isDark);
    }

    /**
     * 누적 토큰 숫자에 쓸 그라데이션 정의를 반환한다.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark 색상 모드 (그라데이션 id 충돌 방지용)
     * @return linearGradient를 담은 defs 조각
     * @Since 2026-06-17
     */
    @Override
    protected String extraDefs(BadgeResponse data, boolean isDark) {
        return "<defs><linearGradient id=\"%s\" x1=\"0\" y1=\"0\" x2=\"1\" y2=\"1\">%s</linearGradient></defs>"
                .formatted(gradientId(isDark), GRAD_STOPS);
    }

    /**
     * 누적 토큰 숫자의 fill을 그라데이션 참조로 반환한다.
     *
     * @param data      배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark    색상 모드
     * @param modeColor 현재 색상 팔레트 (미사용)
     * @return url(#gradientId)
     * @Since 2026-06-17
     */
    @Override
    protected String tokenFill(BadgeResponse data, boolean isDark, BadgeColors modeColor) {
        return "url(#%s)".formatted(gradientId(isDark));
    }

    private String gradientId(boolean isDark) {
        return "gpuTotalGrad-" + (isDark ? "dark" : "light");
    }
}
