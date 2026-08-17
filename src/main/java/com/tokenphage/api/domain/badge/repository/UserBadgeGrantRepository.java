package com.tokenphage.api.domain.badge.repository;

import com.tokenphage.api.domain.badge.repository.entity.UserBadgeGrant;
import com.tokenphage.api.domain.badge.repository.entity.UserBadgeGrantId;
import com.tokenphage.api.domain.badge.repository.projection.BadgeGrantRow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserBadgeGrantRepository
        extends JpaRepository<UserBadgeGrant, UserBadgeGrantId> {

    /**
     * 배지 사용 가능 여부를 DB 왕복 한 번으로 확인한다.
     * <p>
     * 자격 필요 여부와 자격 보유를 한 쿼리에서 본다. 공개 배지면 EXISTS는 실행되지 않는다.
     * 자격은 github_id 기준이고 요청은 username이라 users를 조인한다.
     *
     * @param username  배지 주인 GitHub 사용자명 (null 불허)
     * @param badgeCode 정리된 배지 코드 (null 불허)
     * @return 사용 가능 여부와 안내 문구. 등록되지 않은 배지 코드면 null
     * @Since 2026-08-10
     */
    @Query(value = """
            SELECT (NOT c.require_grant) OR EXISTS (
                       SELECT 1
                       FROM user_badge_grant ubg
                       JOIN users u ON u.github_id = ubg.github_id
                       WHERE u.username     = :username
                         AND ubg.badge_code = c.code
                   )                AS granted,
                   c.display_name   AS displayName,
                   c.locked_message AS lockedMessage
            FROM badge_catalog c
            WHERE c.code = :badgeCode
            """, nativeQuery = true)
    BadgeGrantRow findGrant(@Param("username") String username, @Param("badgeCode") String badgeCode);
}
