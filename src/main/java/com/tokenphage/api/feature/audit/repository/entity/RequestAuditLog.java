package com.tokenphage.api.feature.audit.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

/**
 * 컨트롤러에 도달한 단일 요청의 감사 메타데이터를 담는 append-only 엔티티.
 * <p>
 * 본문(요청/응답 바디)은 저장하지 않는다(privacy-by-design). 실제 적재는 후속 단계의
 * 비동기 서비스가 수행하며, 이 엔티티는 영속 매핑만 정의한다.
 */
@Entity
@Table(name = "request_audit_log")
@Getter @Setter @NoArgsConstructor
public class RequestAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    @Column(nullable = false, length = 40)
    private String feature;

    @Column(nullable = false, length = 60)
    private String action;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "request_path", nullable = false, length = 255)
    private String requestPath;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "latency_ms", nullable = false)
    private int latencyMs;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "github_id")
    private Long githubId;

    @Column(length = 40)
    private String username;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(length = 40)
    private String outcome;
}
