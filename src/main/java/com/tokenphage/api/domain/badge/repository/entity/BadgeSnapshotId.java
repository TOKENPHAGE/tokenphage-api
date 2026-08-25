package com.tokenphage.api.domain.badge.repository.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * badge_snapshot 복합키(@IdClass) 클래스.
 * <p>
 * 값 생성자는 findById 호출용이다.
 */
public class BadgeSnapshotId implements Serializable {
    private Long githubId;
    private String badgeCode;

    public BadgeSnapshotId() {
    }

    /**
     * 조회에 쓸 복합키를 만든다.
     *
     * @param githubId  GitHub 숫자 ID
     * @param badgeCode 배지 코드 (badge_catalog.code)
     * @Since 2026-08-23
     */
    public BadgeSnapshotId(Long githubId, String badgeCode) {
        this.githubId = githubId;
        this.badgeCode = badgeCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BadgeSnapshotId that)) {
            return false;
        }
        return Objects.equals(githubId, that.githubId) &&
                Objects.equals(badgeCode, that.badgeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(githubId, badgeCode);
    }
}
