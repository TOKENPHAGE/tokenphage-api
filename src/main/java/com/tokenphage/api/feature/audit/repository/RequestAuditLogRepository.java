package com.tokenphage.api.feature.audit.repository;
import com.tokenphage.api.feature.audit.repository.entity.RequestAuditLog;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 요청 감사 로그 영속 리포지토리.
 * <p>
 * 현재는 상속받은 save()만 사용한다. 조회/집계는 운영자가 직접 SQL로 수행하므로
 * 커스텀 쿼리 메서드를 두지 않는다.
 */
public interface RequestAuditLogRepository extends JpaRepository<RequestAuditLog, Long> {
}
