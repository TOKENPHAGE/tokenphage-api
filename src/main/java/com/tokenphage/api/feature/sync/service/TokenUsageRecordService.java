package com.tokenphage.api.feature.sync.service;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
class TokenUsageRecordService {

    private final DailyTokenUsageRepository tokenRepo;

    /**
     * JWT와 SyncRequest에서 githubId, deviceId, records를 추출하여 daily_token_usage에 저장한다.
     *
     * @param jwt JWT 토큰 (githubId 추출에 사용)
     * @param req 동기화 요청 (deviceId, 토큰 레코드 목록 포함)
     * @Since 2026-05-25
     */
    @Transactional
    void saveRecords(Jwt jwt, SyncRequest req) {
        Long githubId = Long.parseLong(jwt.getSubject());
        UUID deviceId = UUID.fromString(req.deviceId());
        log.debug("Upserting {} token records for githubId={}", req.records().size(), githubId);
        for (TokenRecordRequest r : req.records()) {
            tokenRepo.upsertRecord(
                githubId,
                deviceId,
                LocalDate.parse(r.date()),
                r.model(),
                r.inputTok(),
                r.outputTok(),
                r.cacheReadTok(),
                r.cacheCreateTok()
            );
        }
        log.debug("Upsert complete: githubId={}, count={}", githubId, req.records().size());
    }
}
