package com.tokenphage.api.feature.reset.service;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 전체 초기화의 DB 쓰기(토큰 사용량 삭제)를 트랜잭션으로 수행하는 서비스. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResetService {

    private final DailyTokenUsageRepository tokenRepo;

    /**
     * 사용자의 모든 토큰 사용량을 삭제한다.
     * <p>
     * 부분 삭제 후 재-sync로 인한 재오염을 방지하기 위해 트랜잭션 경계를 둔다.
     *
     * @param githubId 대상 사용자 (JWT sub에서 파생, null 불허)
     * @Since 2026-06-06
     */
    @Transactional
    public void resetUsage(Long githubId) {
        int deleted = tokenRepo.deleteAllByGithubId(githubId);
        log.info("Reset usage: githubId={}, deletedRows={}", githubId, deleted);
    }
}
