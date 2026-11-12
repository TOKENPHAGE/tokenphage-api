package com.tokenphage.api.domain.token.repository;

import com.tokenphage.api.domain.token.repository.entity.DailyTokenUsage;
import com.tokenphage.api.domain.token.repository.entity.DailyTokenUsageId;
import com.tokenphage.api.domain.token.repository.projection.CacheTokenSum;
import com.tokenphage.api.domain.token.repository.projection.DailyUsageRow;
import com.tokenphage.api.domain.token.repository.projection.ModelUsageRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DailyTokenUsageRepository extends JpaRepository<DailyTokenUsage, DailyTokenUsageId> {

    /**
     * 사용량 한 줄을 저장한다. 같은 키가 이미 있으면 토큰 4종을 새 값으로 덮어쓴다.
     * <p>
     * CLI가 합산해 보낸 값으로 교체하므로 같은 구간을 다시 sync해도 합이 부풀지 않는다 (ADR-0002).
     * 같은 키의 저장이 겹쳐도 실패하지 않도록 ON CONFLICT 한 문장으로 처리한다.
     *
     * @param githubId       대상 사용자 (null 불허)
     * @param deviceId       기기 식별자 (null 불허)
     * @param usageDate      사용량이 발생한 날짜 (null 불허)
     * @param model          모델 이름 (최대 80자)
     * @param inputTok       입력 토큰 수
     * @param outputTok      출력 토큰 수
     * @param cacheReadTok   캐시에서 재사용한 토큰 수
     * @param cacheCreateTok 캐시에 새로 쓴 토큰 수
     * @Since 2026-11-11
     */
    @Modifying
    @Query("""
        INSERT INTO DailyTokenUsage
            (githubId, deviceId, usageDate, model, inputTok, outputTok, cacheReadTok, cacheCreateTok)
        VALUES (:githubId, :deviceId, :usageDate, :model, :inputTok, :outputTok, :cacheReadTok, :cacheCreateTok)
        ON CONFLICT (githubId, deviceId, usageDate, model)
        DO UPDATE SET
            inputTok       = excluded.inputTok,
            outputTok      = excluded.outputTok,
            cacheReadTok   = excluded.cacheReadTok,
            cacheCreateTok = excluded.cacheCreateTok
        """)
    void upsertRecord(
        @Param("githubId")       Long githubId,
        @Param("deviceId")       UUID deviceId,
        @Param("usageDate")      LocalDate usageDate,
        @Param("model")          String model,
        @Param("inputTok")       long inputTok,
        @Param("outputTok")      long outputTok,
        @Param("cacheReadTok")   long cacheReadTok,
        @Param("cacheCreateTok") long cacheCreateTok
    );

    /**
     * 특정 사용자의 모든 토큰 사용량 row를 삭제한다.
     * <p>
     * 파생 deleteBy는 행을 전부 읽고 한 줄씩 지우므로 벌크 DELETE로 한 번에 지운다.
     * idx_dtu_github_date / idx_dtu_github_model 인덱스가 github_id 선두라 인덱스 기반으로 동작한다.
     * 호출 전 영속성 컨텍스트의 변경사항을 먼저 flush한 뒤 벌크 DELETE를 수행하고 1차 캐시를 비운다.
     *
     * @param githubId 대상 사용자 (JWT sub에서 파생, null 불허)
     * @return 삭제된 row 수
     * @Since 2026-11-11
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DailyTokenUsage d WHERE d.githubId = :githubId")
    int deleteAllByGithubId(@Param("githubId") Long githubId);

    /**
     * 사용자의 누적 사용량을 조회한다.
     * <p>
     * 사용량 = input + cache_create + output. 캐시에서 재사용한 cache_read는 제외한다 (ADR-0005).
     *
     * @param githubId 대상 사용자 (null 불허)
     * @return 누적 사용량 (기록이 없으면 null)
     * @Since 2026-09-30
     */
    @Query("SELECT SUM(d.inputTok + d.cacheCreateTok + d.outputTok) FROM DailyTokenUsage d WHERE d.githubId = :githubId")
    Long sumTotalTokens(@Param("githubId") Long githubId);

    /**
     * 사용자의 [from, to] 기간 일별 사용량을 날짜 오름차순으로 조회한다.
     * <p>
     * 창 길이는 호출자가 정한다(예: 30일 히트바, 365일 잔디). 사용 이력이 있는 날짜만 반환한다.
     * 사용량 정의는 {@link #sumTotalTokens}와 같다.
     *
     * @param githubId 대상 사용자 (null 불허)
     * @param from     조회 시작일(포함, null 불허)
     * @param to       조회 종료일(포함, null 불허)
     * @return 날짜(yyyy-MM-dd 문자열)·총합 프로젝션 목록 (오름차순)
     * @Since 2026-11-11
     */
    @Query("""
        SELECT CAST(d.usageDate AS String) AS date, SUM(d.inputTok + d.cacheCreateTok + d.outputTok) AS total
        FROM DailyTokenUsage d
        WHERE d.githubId = :githubId AND d.usageDate BETWEEN :from AND :to
        GROUP BY d.usageDate ORDER BY d.usageDate
        """)
    List<DailyUsageRow> findDailyTotalsBetween(@Param("githubId") Long githubId,
                                               @Param("from") LocalDate from,
                                               @Param("to") LocalDate to);

    /**
     * 사용자의 모델별 사용량 상위 5개를 내림차순으로 조회한다.
     * <p>
     * 사용량 정의는 {@link #sumTotalTokens}와 같다.
     *
     * @param githubId 대상 사용자 (null 불허)
     * @return 모델명·사용량 프로젝션 목록 (최대 5개)
     * @Since 2026-11-11
     */
    @Query("""
        SELECT d.model AS model, SUM(d.inputTok + d.cacheCreateTok + d.outputTok) AS total
        FROM DailyTokenUsage d WHERE d.githubId = :githubId
        GROUP BY d.model ORDER BY total DESC LIMIT 5
        """)
    List<ModelUsageRow> findTop5Models(@Param("githubId") Long githubId);

    /**
     * 사용자의 캐시 히트율 계산에 쓰는 토큰 합계를 조회한다.
     * <p>
     * 집계라 항상 한 행을 반환한다. 기록이 없으면 세 값이 모두 null이다.
     *
     * @param githubId 대상 사용자 (null 불허)
     * @return cache_read·cache_create·input 합계 프로젝션 (한 행)
     * @Since 2026-11-11
     */
    @Query("""
        SELECT SUM(d.cacheReadTok)   AS cacheRead,
               SUM(d.cacheCreateTok) AS cacheCreate,
               SUM(d.inputTok)       AS inputTok
        FROM DailyTokenUsage d WHERE d.githubId = :githubId
        """)
    List<CacheTokenSum> sumCacheTokens(@Param("githubId") Long githubId);
}
