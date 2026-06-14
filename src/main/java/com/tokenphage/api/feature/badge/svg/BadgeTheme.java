package com.tokenphage.api.feature.badge.svg;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;

public interface BadgeTheme {

    /**
     * 테마 식별자를 반환한다. URL ?theme= 파라미터 값과 소문자로 일치해야 한다.
     *
     * @return 소문자 테마 식별자 (예: "gpu")
     * @Since 2026-05-27
     */
    String name();

    /**
     * 배지 데이터와 모드로 SVG 문자열을 생성한다.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark true면 다크 모드, false면 라이트 모드
     * @return 완성된 SVG 마크업 문자열
     * @Since 2026-05-27
     */
    String build(BadgeResponse data, boolean isDark);
}
