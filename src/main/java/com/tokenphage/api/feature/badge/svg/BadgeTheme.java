package com.tokenphage.api.feature.badge.svg;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;

import java.util.Set;

public interface BadgeTheme {

    /**
     * 테마 식별자를 반환한다. URL ?theme= 파라미터 값과 소문자로 일치해야 한다.
     *
     * @return 소문자 테마 식별자 (예: "gpu")
     * @Since 2026-05-27
     */
    String name();

    /**
     * 배지 데이터와 색상 모드로 SVG 문자열을 생성한다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @param mode 색상 모드 (이 테마의 지원 집합 안 값, null 불허)
     * @return 완성된 SVG 마크업 문자열
     * @Since 2026-05-27
     */
    String build(BadgeResponse data, BadgeMode mode);

    /**
     * 이 테마가 지원하는 색상 모드 집합을 선언한다.
     * <p>
     * 지원하지 않는 mode 값은 디스패처가 {@link #defaultMode()}로 접는다.
     * <b>기본 구현을 두지 않는다(정책)</b> — needs()와 같다.
     *
     * @return 지원 모드 집합
     * @Since 2026-08-23
     */
    Set<BadgeMode> supportedModes();

    /**
     * 미지원 mode 값이 들어왔을 때 쓸 기본 모드를 반환한다.
     *
     * @return 기본 모드
     * @Since 2026-08-23
     */
    BadgeMode defaultMode();

    /**
     * 이 테마가 렌더링에 필요로 하는 데이터 종류를 선언한다.
     * <p>
     * 조회 서비스는 이 선언에 포함된 데이터만 조회한다. <b>기본 구현을 두지 않는다(정책)</b> —
     * 모든 구현체가 자신이 쓰는 데이터를 명시적으로 선언해야 한다.
     *
     * @return 필요 데이터 종류 집합
     * @Since 2026-07-16
     */
    Set<BadgeDataNeed> needs();
}
