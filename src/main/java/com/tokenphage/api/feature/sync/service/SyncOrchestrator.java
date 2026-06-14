package com.tokenphage.api.feature.sync.service;

import com.tokenphage.api.domain.BadgeCacheInvalidator;
import com.tokenphage.api.domain.user.service.UserService;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
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

    /**
     * 토큰 사용 기록 동기화 전체 흐름을 조율한다.
     * <p>
     * 사용자 저장 → 토큰 레코드 저장 → 레벨 재계산 → 배지 캐시 무효화 순서로 실행한다.
     *
     * @param jwt JWT 토큰 (githubId, username 추출에 사용)
     * @param req 동기화 요청 (deviceId, 토큰 레코드 목록 포함)
     * @Since 2026-05-24
     */
    public void sync(Jwt jwt, SyncRequest req) {
        Long githubId = Long.parseLong(jwt.getSubject());
        String username = jwt.getClaimAsString("username");
        log.info("Sync started: githubId={}, username={}, deviceId={}, records={}", githubId, username, req.deviceId(), req.records().size());

        userService.saveUser(githubId, username);
        tokenUsageRecordService.saveRecords(jwt, req);
        badgeCacheInvalidator.evict(username);
        log.info("Sync completed: githubId={}, username={}", githubId, username);
    }
}
