package com.tokenphage.api.domain.token.repository;

import com.tokenphage.api.domain.token.repository.entity.DailyTokenUsage;
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

public interface DailyTokenUsageRepository extends JpaRepository<DailyTokenUsage, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO daily_token_usage
            (github_id, device_id, usage_date, model, input_tok, output_tok, cache_read_tok, cache_create_tok)
        VALUES (:githubId, :deviceId, :usageDate, :model, :inputTok, :outputTok, :cacheReadTok, :cacheCreateTok)
        ON CONFLICT (github_id, device_id, usage_date, model)
        DO UPDATE SET
            input_tok        = EXCLUDED.input_tok,
            output_tok       = EXCLUDED.output_tok,
            cache_read_tok   = EXCLUDED.cache_read_tok,
            cache_create_tok = EXCLUDED.cache_create_tok
        """, nativeQuery = true)
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
     * PK가 (github_id, device_id, usage_date, model) 복합키라 상속 deleteById로는 불가하다.
     * idx_dtu_github_date / idx_dtu_github_model 인덱스가 github_id 선두라 인덱스 기반으로 동작한다.
     * 호출 전 영속성 컨텍스트의 변경사항을 먼저 flush한 뒤 벌크 DELETE를 수행하고 1차 캐시를 비운다.
     *
     * @param githubId 대상 사용자 (JWT sub에서 파생, null 불허)
     * @return 삭제된 row 수
     * @Since 2026-06-06
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM daily_token_usage WHERE github_id = :githubId", nativeQuery = true)
    int deleteAllByGithubId(@Param("githubId") Long githubId);

    @Query("SELECT SUM(d.inputTok + d.outputTok) FROM DailyTokenUsage d WHERE d.githubId = :githubId")
    Long sumTotalTokens(@Param("githubId") Long githubId);

    @Query(value = """
        SELECT usage_date::text AS date, SUM(input_tok + output_tok) AS total
        FROM daily_token_usage
        WHERE github_id = :githubId AND usage_date BETWEEN :from AND :to
        GROUP BY usage_date ORDER BY usage_date
        """, nativeQuery = true)
    List<DailyUsageRow> findLast30Days(@Param("githubId") Long githubId,
                                       @Param("from") LocalDate from,
                                       @Param("to") LocalDate to);

    @Query(value = """
        SELECT model, SUM(input_tok + output_tok) AS total
        FROM daily_token_usage WHERE github_id = :githubId
        GROUP BY model ORDER BY total DESC LIMIT 5
        """, nativeQuery = true)
    List<ModelUsageRow> findTop5Models(@Param("githubId") Long githubId);

    @Query(value = """
        SELECT
            SUM(cache_read_tok)   AS cacheRead,
            SUM(cache_create_tok) AS cacheCreate,
            SUM(input_tok)        AS inputTok
        FROM daily_token_usage WHERE github_id = :githubId
        """, nativeQuery = true)
    List<CacheTokenSum> sumCacheTokens(@Param("githubId") Long githubId);
}
