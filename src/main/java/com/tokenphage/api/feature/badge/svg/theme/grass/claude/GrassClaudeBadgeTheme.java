package com.tokenphage.api.feature.badge.svg.theme.grass.claude;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.theme.grass.GrassBadgeTheme;
import org.springframework.stereotype.Component;

/**
 * 클로드 마스코트가 하늘 놀이터를 걸어 다니는 잔디 스타일 뱃지 테마.
 * <p>
 * URL 파라미터 ?theme=grass-claude 로 선택한다. 레이아웃·팔레트는
 * {@link GrassBadgeTheme} 기본값을 그대로 쓰고 마스코트만 공급한다.
 */
@Component
public class GrassClaudeBadgeTheme extends GrassBadgeTheme {

    /**
     * 테마 식별자를 반환한다.
     *
     * @return "grass-claude"
     * @Since 2026-07-15
     */
    @Override
    public String name() {
        return "grass-claude";
    }

    /**
     * 랜덤 좌우 걷기 애니메이션이 포함된 클로드 마스코트를 반환한다.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark true면 다크 모드
     * @return 마스코트 SVG 조각
     * @Since 2026-07-15
     */
    @Override
    protected String mascot(BadgeResponse data, boolean isDark) {
        return GrassClaudeMascot.render(isDark);
    }
}
