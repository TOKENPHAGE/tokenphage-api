package com.tokenphage.api.domain.badge.repository;

import com.tokenphage.api.domain.badge.repository.entity.BadgeSnapshot;
import com.tokenphage.api.domain.badge.repository.entity.BadgeSnapshotId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BadgeSnapshotRepository extends JpaRepository<BadgeSnapshot, BadgeSnapshotId> {

    /**
     * 스냅샷 payload JSON 원문을 복합 PK로 조회한다.
     * <p>
     * jsonb는 ::text로 꺼내야 드라이버가 String으로 반환한다.
     *
     * @param githubId  배지 주인 GitHub 숫자 ID (null 불허)
     * @param badgeCode 배지 코드 (null 불허)
     * @return payload JSON 원문, 행이 없으면 null
     * @Since 2026-08-23
     */
    @Query(value = """
            SELECT bs.payload::text
            FROM badge_snapshot bs
            WHERE bs.github_id  = :githubId
              AND bs.badge_code = :badgeCode
            """, nativeQuery = true)
    String findPayload(@Param("githubId") Long githubId, @Param("badgeCode") String badgeCode);
}
