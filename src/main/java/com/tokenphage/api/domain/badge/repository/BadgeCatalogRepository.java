package com.tokenphage.api.domain.badge.repository;

import com.tokenphage.api.domain.badge.repository.entity.BadgeCatalog;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 배지 종류 조회 리포지토리.
 * <p>
 * 자격 확인은 {@code UserBadgeGrantRepository.findGrant}가 함께 처리한다. 전체 조회만 쓴다.
 */
public interface BadgeCatalogRepository extends JpaRepository<BadgeCatalog, String> {
}
