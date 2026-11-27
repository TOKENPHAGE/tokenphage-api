package com.tokenphage.api.domain.user.repository;

import com.tokenphage.api.domain.user.repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    /**
     * 사용자를 저장한다. 이미 있으면 username과 갱신 시각을 새로 쓴다.
     * <p>
     * 로그인과 sync가 부를 때마다 updated_at을 DB 시각으로 갱신한다. username이 같아도 갱신한다.
     * 같은 계정의 요청이 겹쳐도 실패하지 않도록 ON CONFLICT 한 문장으로 처리한다.
     *
     * @param githubId GitHub 사용자 고유 ID (null 불허)
     * @param username GitHub 사용자명 (null 불허)
     * @Since 2026-11-11
     */
    @Modifying
    @Query("""
        INSERT INTO User (githubId, username, createdAt, updatedAt)
        VALUES (:githubId, :username, instant, instant)
        ON CONFLICT (githubId) DO UPDATE SET username = excluded.username, updatedAt = instant
        """)
    void upsert(@Param("githubId") Long githubId, @Param("username") String username);
}
