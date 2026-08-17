package com.tokenphage.api.domain.badge.repository.entity;

import java.io.Serializable;
import java.util.Objects;

/**
 * user_badge_grant 복합키(@IdClass) 클래스.
 * <p>
 * 값 생성자는 findById/deleteById 호출용이다.
 */
public class UserBadgeGrantId implements Serializable {
    private Long githubId;
    private String badgeCode;

    public UserBadgeGrantId() {
    }

    /**
     * 조회·삭제에 쓸 복합키를 만든다.
     *
     * @param githubId  GitHub 숫자 ID
     * @param badgeCode 배지 코드 (badge_catalog.code)
     * @Since 2026-08-10
     */
    public UserBadgeGrantId(Long githubId, String badgeCode) {
        this.githubId = githubId;
        this.badgeCode = badgeCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserBadgeGrantId that)) {
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
