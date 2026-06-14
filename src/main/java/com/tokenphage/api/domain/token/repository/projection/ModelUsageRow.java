package com.tokenphage.api.domain.token.repository.projection;

/**
 * 모델별 토큰 사용량 집계 쿼리 결과 프로젝션.
 * {@code findTop5Models} native query의 결과 행에 매핑된다.
 */
public interface ModelUsageRow {

    /** 모델 식별자 */
    String getModel();

    /** 해당 모델의 총 토큰 수 (input + output) */
    Long getTotal();
}
