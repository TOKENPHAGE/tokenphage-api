package com.tokenphage.api.feature.sync.dto.response;

/**
 * 실행시간 처리 결과. accepted 는 저장한 항목 수, rejected 는 검사에서 걸러 버린 항목 수다.
 */
public record RuntimeSyncResultResponse(int accepted, int rejected) {
}
