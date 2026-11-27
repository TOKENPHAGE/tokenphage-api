package com.tokenphage.api.feature.badge.svg.theme.odometer.gpt;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.theme.odometer.OdometerBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.odometer.OdometerColors;
import org.springframework.stereotype.Component;

@Component
public class OdometerGptBadgeTheme extends OdometerBadgeTheme {

    /** OpenAI 브랜드색. RUNTIME 라벨은 배경에 맞춰 dark에서 밝히고 light에서 어둡게 한 색을 쓴다. */
    private static final String ACCENT = "#10A37F";

    private static final OdometerColors DARK = OdometerColors.dark(ACCENT, "#6EE7C5");
    private static final OdometerColors LIGHT = OdometerColors.light(ACCENT, "#0B7A5F");

    /**
     * 주행거리계 OpenAI 테마 식별자를 반환한다.
     *
     * @return "odometer-gpt"
     * @Since 2026-11-13
     */
    @Override
    public String name() {
        return BadgeCode.ODOMETER_GPT.getCode();
    }

    /**
     * 회전하는 OpenAI 로고를 반환한다. dark에서는 흰색, light에서는 흰 배경에 묻히지 않게 브랜드색으로 그린다.
     *
     * @param data   배지에 표시할 사용자 데이터 (사용하지 않음)
     * @param isDark true면 dark 모드, false면 light 모드
     * @return OpenAI 로고 SVG 조각
     * @Since 2026-11-13
     */
    @Override
    protected String mascot(BadgeResponse data, boolean isDark) {
        return OdometerGptMascot.render(isDark ? "#ffffff" : ACCENT);
    }

    /**
     * GPT 테마의 모드별 색을 반환한다.
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
