package com.tokenphage.api.domain.badge.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 배지 자격 부여·회수 이력 엔티티. 추가만 하고 수정하지 않는다.
 * <p>
 * user_badge_grant 행이 회수로 DELETE되어도 "누가/언제/왜"는 여기 남는다.
 * FK를 걸지 않는 것은 request_audit_log와 동일 정책이다(기록 독립 보존).
 * <p>
 * setter 없음 — 기록된 이력은 수정 대상이 아니다. 기본 생성자는 JPA 전용이라 protected다.
 */
@Entity
@Table(name = "badge_grant_history")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadgeGrantHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt = Instant.now();

    @Column(name = "github_id", nullable = false)
    private Long githubId;

    @Column(name = "badge_code", nullable = false, length = 40)
    private String badgeCode;

    /** GRANT 또는 REVOKE. DB CHECK 제약과 값이 일치해야 한다. */
    @Column(nullable = false, length = 10)
    private String action;

    /** 부여/회수를 수행한 GitHub username. */
    @Column(length = 40)
    private String actor;

    @Column(length = 255)
    private String reason;
}
