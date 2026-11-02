package com.tokenphage.api.feature.reset.service;

import com.tokenphage.api.domain.runtime.repository.DailyAgentRuntimeRepository;
import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 전체 초기화의 DB 쓰기(토큰 사용량·실행시간 삭제)를 트랜잭션으로 수행하는 서비스. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResetService {

    private final DailyTokenUsageRepository tokenRepo;
    private final DailyAgentRuntimeRepository runtimeRepo;

    /**
     * 사용자의 모든 토큰 사용량과 실행시간을 삭제한다.
     * <p>
     * 부분 삭제 후 재-sync로 인한 재오염을 방지하기 위해 두 표를 한 트랜잭션에서 지운다.
     *
     * @param githubId 대상 사용자 (JWT sub에서 파생, null 불허)
     * @Since 2026-10-11
     */
    @Transactional
    public void resetUsage(Long githubId) {
        int deleted = tokenRepo.deleteAllByGithubId(githubId);
        int deletedRuntimes = runtimeRepo.deleteAllByGithubId(githubId);
        log.info("Reset usage: githubId={}, deletedRows={}, deletedRuntimeRows={}", githubId, deleted, deletedRuntimes);
    }
}
