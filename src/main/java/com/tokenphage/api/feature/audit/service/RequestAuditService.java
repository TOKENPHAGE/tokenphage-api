package com.tokenphage.api.feature.audit.service;
import com.tokenphage.api.feature.audit.dto.RequestAuditCommand;
import com.tokenphage.api.feature.audit.repository.RequestAuditLogRepository;
import com.tokenphage.api.feature.audit.repository.entity.RequestAuditLog;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 요청 감사 로그를 비동기로 적재하는 서비스.
 * <p>
 * 전용 스레드풀(auditTaskExecutor)에서 실행되어 본 요청 처리와 분리된다.
 * 적재 실패는 본 요청에 영향을 주지 않도록 삼키고 에러 로그만 남긴다(best-effort).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequestAuditService {

    // request_audit_log 컬럼 최대 길이. 초과 입력으로 인한 INSERT 실패(감사 유실)를 막기 위해 저장 직전 자른다.
    private static final int MAX_FEATURE = 40;
    private static final int MAX_ACTION = 60;
    private static final int MAX_HTTP_METHOD = 10;
    private static final int MAX_REQUEST_PATH = 255;
    private static final int MAX_CLIENT_IP = 45;
    private static final int MAX_USERNAME = 40;
    private static final int MAX_USER_AGENT = 255;
    private static final int MAX_OUTCOME = 40;

    private final RequestAuditLogRepository auditLogRepo;

    /**
     * 요청 감사 커맨드를 엔티티로 변환해 비동기로 저장한다.
     * <p>
     * 호출 즉시 반환되며 실제 저장은 auditTaskExecutor 스레드에서 수행된다.
     * DB 장애 등으로 저장이 실패해도 예외를 전파하지 않는다(best-effort).
     *
     * @param command 적재할 요청 감사 메타데이터 (null 불허)
     * @Since 2026-06-09
     */
    @Async("auditTaskExecutor")
    public void record(RequestAuditCommand command) {
        try {
            auditLogRepo.save(toEntity(command));
        } catch (Exception e) {
            log.error("Failed to persist request audit log: feature={}, action={}",
                command.feature(), command.action(), e);
        }
    }

    /**
     * 커맨드를 영속 엔티티로 매핑한다.
     *
     * @param c 요청 감사 커맨드
     * @return 저장 대상 엔티티 (occurredAt은 엔티티 기본값으로 설정됨)
     */
    private RequestAuditLog toEntity(RequestAuditCommand c) {
        RequestAuditLog entity = new RequestAuditLog();
        entity.setFeature(truncate(c.feature(), MAX_FEATURE));
        entity.setAction(truncate(c.action(), MAX_ACTION));
        entity.setHttpMethod(truncate(c.httpMethod(), MAX_HTTP_METHOD));
        entity.setRequestPath(truncate(c.requestPath(), MAX_REQUEST_PATH));
        entity.setStatusCode(c.statusCode());
        entity.setLatencyMs(c.latencyMs());
        entity.setClientIp(truncate(c.clientIp(), MAX_CLIENT_IP));
        entity.setGithubId(c.githubId());
        entity.setUsername(truncate(c.username(), MAX_USERNAME));
        entity.setUserAgent(truncate(c.userAgent(), MAX_USER_AGENT));
        entity.setOutcome(truncate(c.outcome(), MAX_OUTCOME));
        return entity;
    }

    /**
     * 문자열을 컬럼 최대 길이로 자른다.
     * <p>
     * 컬럼 한계를 넘는 입력(긴 User-Agent/경로 등)으로 INSERT가 실패해 감사 row가 유실되는 것을 방지한다.
     *
     * @param value 원본 값 (null 허용)
     * @param maxLength 컬럼 최대 길이
     * @return null이거나 한계 이하면 원본, 초과면 maxLength로 자른 문자열
     */
    private static String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
