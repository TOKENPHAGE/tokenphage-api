package com.tokenphage.api.feature.sync.service;

import com.tokenphage.api.domain.runtime.repository.DailyAgentRuntimeRepository;
import com.tokenphage.api.feature.sync.dto.request.RuntimeRecordRequest;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.response.RuntimeSyncResultResponse;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class RuntimeRecordService {

    private final DailyAgentRuntimeRepository runtimeRepo;
    private final Validator validator;

    /**
     * 실행시간 항목을 하나씩 검사해 통과한 것만 daily_agent_runtime 에 저장하고 처리 수를 돌려준다.
     * <p>
     * 잘못된 항목은 그 항목만 버리고 수를 세어 경고 로그 한 줄을 남긴다.
     * 토큰 저장과 다른 트랜잭션이라 여기서 실패해도 토큰은 남는다. 저장 중 DB 예외는 삼키지 않는다.
     *
     * @param jwt JWT 토큰 (githubId 추출에 사용)
     * @param req 동기화 요청 (deviceId, runtimes 포함. runtimes 는 null 불허)
     * @return 저장한 항목 수와 거부한 항목 수
     * @Since 2026-10-11
     */
    @Transactional
    RuntimeSyncResultResponse saveRecords(Jwt jwt, SyncRequest req) {
        Long githubId = Long.parseLong(jwt.getSubject());
        UUID deviceId = UUID.fromString(req.deviceId());
        int accepted = 0;
        int rejected = 0;
        for (RuntimeRecordRequest r : req.runtimes()) {
            LocalDate usageDate = validDateOf(r);
            if (usageDate == null) {
                rejected++;
                continue;
            }
            runtimeRepo.upsertRecord(githubId, deviceId, usageDate, r.source(), r.model(), r.runtimeMs());
            accepted++;
        }
        if (rejected > 0) {
            log.warn("Runtime records rejected: githubId={}, rejected={}, accepted={}", githubId, rejected, accepted);
        }
        return new RuntimeSyncResultResponse(accepted, rejected);
    }

    /** 제약을 통과하고 실제 있는 날짜면 그 날짜를, 아니면 null 을 돌려준다. */
    private LocalDate validDateOf(RuntimeRecordRequest r) {
        if (r == null || !validator.validate(r).isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(r.date());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
