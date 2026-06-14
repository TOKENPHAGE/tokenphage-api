package com.tokenphage.api.domain.token.repository.projection;

/**
 * 일별 토큰 사용량 집계 쿼리 결과 프로젝션.
 * {@code findLast30Days} native query의 결과 행에 매핑된다.
 */
public interface DailyUsageRow {

    /** 사용 날짜 (yyyy-MM-dd) */
    String getDate();

    /** 해당 날짜의 총 토큰 수 (input + output) */
    Long getTotal();
}
