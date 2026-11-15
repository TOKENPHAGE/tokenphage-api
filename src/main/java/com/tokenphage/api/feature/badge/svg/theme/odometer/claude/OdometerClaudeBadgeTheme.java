package com.tokenphage.api.feature.badge.svg.theme.odometer.claude;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.theme.odometer.OdometerBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.odometer.OdometerColors;
import org.springframework.stereotype.Component;

@Component
public class OdometerClaudeBadgeTheme extends OdometerBadgeTheme {

    /** Claude 브랜드색. RUNTIME 라벨은 배경에 맞춰 dark에서 밝히고 light에서 어둡게 한 색을 쓴다. */
    private static final String ACCENT = "#D97757";

    private static final OdometerColors DARK = OdometerColors.dark(ACCENT, "#E9A589");
    private static final OdometerColors LIGHT = OdometerColors.light(ACCENT, "#C4623F");

    /**
     * 주행거리계 Claude 테마 식별자를 반환한다.
     *
     * @return "odometer-claude"
     * @Since 2026-11-13
     */
    @Override
    public String name() {
        return BadgeCode.ODOMETER_CLAUDE.getCode();
    }

    /**
     * 달리다 멈춰 뛰어오르는 Claude 마스코트를 반환한다. 가동시간·모드와 무관하게 같은 그림이다.
     *
     * @param data   배지에 표시할 사용자 데이터 (사용하지 않음)
     * @param isDark 색상 모드 (사용하지 않음)
     * @return Claude 마스코트 SVG 조각
     * @Since 2026-11-13
     */
    @Override
    protected String mascot(BadgeResponse data, boolean isDark) {
        return OdometerClaudeMascot.render();
    }

    /**
     * Claude 테마의 모드별 색을 반환한다.
     *
     * @param isDark true면 dark 모드, false면 light 모드
     * @return 해당 모드의 색
     * @Since 2026-11-13
     */
    @Override
    protected OdometerColors colors(boolean isDark) {
        return isDark ? DARK : LIGHT;
    }
}
