package com.tokenphage.api.domain.runtime.repository;

import com.tokenphage.api.domain.runtime.repository.entity.DailyAgentRuntime;
import com.tokenphage.api.domain.runtime.repository.entity.DailyAgentRuntimeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.UUID;

public interface DailyAgentRuntimeRepository extends JpaRepository<DailyAgentRuntime, DailyAgentRuntimeId> {

    /**
     * 실행시간 한 줄을 저장한다. 같은 키가 이미 있으면 새 값으로 덮어쓴다.
     * <p>
     * 값이 줄어도 덮어쓴다. 계산 방식을 고친 CLI 가 다시 보낸 값이 반영되어야 하기 때문이다.
     *
     * @param githubId  대상 사용자 (null 불허)
     * @param deviceId  기기 식별자 (null 불허)
     * @param usageDate 실행시간이 발생한 날짜 (null 불허)
     * @param source    로그를 읽은 도구 (claude, codex, opencode)
     * @param model     모델 이름 (최대 80자)
     * @param runtimeMs 그 날짜·도구·모델의 실행시간 합(ms, 0 이상)
     * @Since 2026-10-11
     */
    @Modifying
    @Query(value = """
        INSERT INTO daily_agent_runtime (github_id, device_id, usage_date, source, model, runtime_ms)
        VALUES (:githubId, :deviceId, :usageDate, :source, :model, :runtimeMs)
        ON CONFLICT (github_id, device_id, usage_date, source, model)
        DO UPDATE SET runtime_ms = EXCLUDED.runtime_ms
        """, nativeQuery = true)
    void upsertRecord(
        @Param("githubId")  Long githubId,
        @Param("deviceId")  UUID deviceId,
        @Param("usageDate") LocalDate usageDate,
        @Param("source")    String source,
        @Param("model")     String model,
        @Param("runtimeMs") long runtimeMs
    );

    /**
     * 특정 사용자의 모든 실행시간 행을 삭제한다.
     * <p>
     * idx_dar_github_date 가 github_id 선두라 인덱스로 찾는다.
     * 벌크 DELETE 전에 영속성 컨텍스트를 flush 하고, 끝나면 1차 캐시를 비운다.
     *
     * @param githubId 대상 사용자 (JWT sub에서 파생, null 불허)
     * @return 삭제된 행 수
     * @Since 2026-10-11
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM daily_agent_runtime WHERE github_id = :githubId", nativeQuery = true)
    int deleteAllByGithubId(@Param("githubId") Long githubId);
}
