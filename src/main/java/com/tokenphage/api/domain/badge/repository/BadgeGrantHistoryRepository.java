package com.tokenphage.api.domain.badge.repository;

import com.tokenphage.api.domain.badge.repository.entity.BadgeGrantHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 배지 자격 부여·회수 이력 리포지토리.
 * <p>
 * 이력은 추가만 한다. 현재 호출자 없음 — 자격 변경은 운영 SQL, 조회는 향후 백오피스용.
 */
public interface BadgeGrantHistoryRepository extends JpaRepository<BadgeGrantHistory, Long> {
}
