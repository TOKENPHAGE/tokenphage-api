package com.tokenphage.api.feature.sync.service;

import com.tokenphage.api.domain.badge.BadgeCacheInvalidator;
import com.tokenphage.api.domain.user.service.UserService;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.response.RuntimeSyncResultResponse;
import com.tokenphage.api.feature.sync.dto.response.SyncResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SyncOrchestrator {

    private final UserService userService;
    private final TokenUsageRecordService tokenUsageRecordService;
    private final BadgeCacheInvalidator badgeCacheInvalidator;
    private final RuntimeRecordService runtimeRecordService;

    /**
     * 토큰 사용 기록과 실행시간 동기화 전체 흐름을 조율한다.
     * <p>
     * 사용자 저장 → 토큰 레코드 저장 → 배지 캐시 무효화 → 실행시간 저장 순서로 실행한다.
     * 실행시간은 토큰과 다른 트랜잭션이라 실패해도 토큰은 남는다. runtimes 칸이 없으면 빈 목록이라 실행시간 표에 쓰지 않는다.
     *
     * @param jwt JWT 토큰 (githubId, username 추출에 사용)
     * @param req 동기화 요청 (deviceId, 토큰 레코드 목록, 실행시간 목록 포함)
     * @return 실행시간 처리 결과 (runtimes 칸이 없거나 비면 0건)
     * @Since 2026-10-29
     */
    public SyncResponse sync(Jwt jwt, SyncRequest req) {
        Long githubId = Long.parseLong(jwt.getSubject());
        String username = jwt.getClaimAsString("username");
        log.info("Sync started: githubId={}, username={}, deviceId={}, records={}, runtimes={}",
                githubId, username, req.deviceId(), req.records().size(), req.runtimes().size());

        userService.saveUser(githubId, username);
        tokenUsageRecordService.saveRecords(jwt, req);
        badgeCacheInvalidator.evict(username);
        RuntimeSyncResultResponse runtimes = runtimeRecordService.saveRecords(jwt, req);
        log.info("Sync completed: githubId={}, username={}, runtimesAccepted={}, runtimesRejected={}",
                githubId, username, runtimes.accepted(), runtimes.rejected());
        return new SyncResponse(runtimes);
    }
}
