package com.tokenphage.api.feature.sync.dto.response;

/**
 * 동기화 응답 본문. 모든 요청에 실행시간 처리 결과를 주며, runtimes 칸이 없거나 비면 0건이다.
 */
public record SyncResponse(RuntimeSyncResultResponse runtimes) {
}
