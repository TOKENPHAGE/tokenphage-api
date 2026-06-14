package com.tokenphage.api.domain.token.repository.projection;

/**
 * 캐시 토큰 합계 쿼리 결과 프로젝션.
 * {@code sumCacheTokens} native query의 결과 행에 매핑된다.
 */
public interface CacheTokenSum {

    /** 캐시 읽기 토큰 합계 */
    Long getCacheRead();

    /** 캐시 생성 토큰 합계 */
    Long getCacheCreate();

    /** 일반 입력 토큰 합계 */
    Long getInputTok();
}
