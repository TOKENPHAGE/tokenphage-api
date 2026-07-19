package com.tokenphage.api.feature.badge.svg;

/**
 * 배지 테마가 렌더링에 필요로 하는 데이터 종류를 선언하는 enum.
 * <p>
 * 테마가 {@link BadgeTheme#needs()}로 요구 데이터를 선언하면, 조회 서비스는 선언된 것만 실행한다.
 * (모든 테마가 동일 데이터를 조회한다는 가정을 제거하기 위한 계약)
 * <p>
 * 각 상수는 자신을 채우는 데 필요한 일별 조회 창을 {@code dailyWindowDays}로 갖는다.
 * 여러 need가 같은 일별 데이터를 공유하므로(예: DAILY_1Y·STREAK_DAYS·YEAR_TOKENS는 모두 365일 창),
 * 조회 서비스는 요구된 need들의 창 최댓값으로 일별 데이터를 한 번만 조회한다.
 */
public enum BadgeDataNeed {

    /** 전 기간 누적 토큰 합계 */
    TOTAL_TOKENS(0),

    /** 최근 30일 일별 사용량 (일별 30일 창) → BadgeResponse.daily30d */
    DAILY_30D(30),

    /** 모델별 토큰 사용량 상위 5개 */
    TOP_MODELS(0),

    /** 캐시 적중률 */
    CACHE_HIT_RATE(0),

    /** 최근 1년 일별 사용량 (일별 365일 창) → BadgeResponse.daily1y */
    DAILY_1Y(365),

    /** 연속 사용일(streak) — 1년 일별 데이터에서 파생 → BadgeResponse.streakDays */
    STREAK_DAYS(365),

    /** 최근 1년 총 토큰 — 1년 일별 데이터에서 파생 → BadgeResponse.yearTokens */
    YEAR_TOKENS(365);

    /** 이 데이터를 채우는 데 필요한 일별 조회 창(일). 0이면 일별 조회가 필요 없다. */
    private final int dailyWindowDays;

    BadgeDataNeed(int dailyWindowDays) {
        this.dailyWindowDays = dailyWindowDays;
    }

    /**
     * 이 데이터가 필요로 하는 일별 조회 창(일수)을 반환한다.
     * <p>
     * 조회 서비스는 요구된 need들의 이 값 최댓값으로 일별 데이터를 한 번만 조회해 공유한다.
     * 일별 데이터가 필요 없는 종류(누적·모델·캐시)는 0이다.
     *
     * @return 일별 조회 창 (일), 일별 데이터가 필요 없으면 0
     * @Since 2026-07-16
     */
    public int getDailyWindowDays() {
        return dailyWindowDays;
    }
}
