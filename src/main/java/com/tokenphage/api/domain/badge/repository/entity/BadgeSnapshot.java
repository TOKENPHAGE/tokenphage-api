package com.tokenphage.api.domain.badge.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 값이 고정된 배지의 미리 계산된 표시 데이터(스냅샷) 엔티티.
 * <p>
 * payload는 String으로 매핑한다 — 엔티티가 JSON 구조를 몰라야 여러 배지가 공유한다.
 * 스키마 해석은 각 배지 패키지가 한다. 적재는 마이그레이션이 하므로 setter가 없다.
 */
@Entity
@Table(name = "badge_snapshot")
@IdClass(BadgeSnapshotId.class)
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BadgeSnapshot {

    @Id
    @Column(name = "github_id")
    private Long githubId;

    @Id
    @Column(name = "badge_code", length = 40)
    private String badgeCode;

    /** 표시할 값 전부를 담은 JSON 원문. */
    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
