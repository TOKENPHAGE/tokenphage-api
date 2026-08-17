package com.tokenphage.api.domain.badge.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 배지 종류를 정의하는 카탈로그 엔티티.
 * <p>
 * code는 BadgeCode enum 및 BadgeTheme의 name()과 일치해야 한다.
 * requireGrant가 true면 자격을 부여받은 사용자만 쓸 수 있다.
 * <p>
 * setter 없음 — 변경은 운영 SQL(docs/runbook/badge-grant.md)로만 한다.
 * 기본 생성자는 JPA 전용이라 protected다.
 */
@Entity
@Table(name = "badge_catalog")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadgeCatalog {

    @Id
    @Column(length = 40)
    private String code;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    /** true면 자격을 부여받은 사용자만 사용 가능. Lombok이 isRequireGrant()를 생성한다. */
    @Column(name = "require_grant", nullable = false)
    private boolean requireGrant;

    /** 자격이 없을 때 보여줄 안내 문구. NULL이면 잠금 배지가 기본 문구를 쓴다. */
    @Column(name = "locked_message", length = 120)
    private String lockedMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
