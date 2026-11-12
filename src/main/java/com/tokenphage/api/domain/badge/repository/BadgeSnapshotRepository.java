package com.tokenphage.api.domain.badge.repository;

import com.tokenphage.api.domain.badge.repository.entity.BadgeSnapshot;
import com.tokenphage.api.domain.badge.repository.entity.BadgeSnapshotId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 배지 스냅샷 조회 리포지토리.
 * <p>
 * 복합 PK(github_id, badge_code) 한 건 조회뿐이라 상속받은 findById만 쓴다.
 */
public interface BadgeSnapshotRepository extends JpaRepository<BadgeSnapshot, BadgeSnapshotId> {
}
