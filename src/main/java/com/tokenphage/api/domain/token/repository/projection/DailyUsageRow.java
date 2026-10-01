package com.tokenphage.api.domain.token.repository.projection;

/**
 * 일별 토큰 사용량 집계 쿼리 결과 프로젝션.
 * {@code findDailyTotalsBetween} native query의 결과 행에 매핑된다.
 */
public interface DailyUsageRow {

    /** 사용 날짜 (yyyy-MM-dd) */
    String getDate();

    /** 해당 날짜의 사용량 (input + cache_create + output) */
    Long getTotal();
}
