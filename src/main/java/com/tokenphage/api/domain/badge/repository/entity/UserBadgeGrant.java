package com.tokenphage.api.domain.badge.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

/**
 * 사용자-배지 자격(grant) 조인 엔티티.
 * <p>
 * setter 없음 — 자격은 수정 대상이 아니라 부여 또는 회수(DELETE)만 된다.
 * 기본 생성자는 JPA 전용이라 protected다.
 * <p>
 * 연관 매핑(@ManyToOne) 대신 스칼라 githubId를 쓴다(DailyTokenUsage와 동일).
 * username이 아닌 github_id 기준인 이유: GitHub 개명 시 username이 바뀐다.
 */
@Entity
@Table(name = "user_badge_grant")
@IdClass(UserBadgeGrantId.class)
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBadgeGrant {

    @Id
    @Column(name = "github_id")
    private Long githubId;

    @Id
    @Column(name = "badge_code", length = 40)
    private String badgeCode;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private Instant grantedAt = Instant.now();

    /** 자격을 부여한 운영자 GitHub username. */
    @Column(name = "granted_by", length = 40)
    private String grantedBy;

    /** 부여 사유 (예: 'PR tokenphage-api#42'). */
    @Column(name = "grant_note", length = 255)
    private String grantNote;
}
