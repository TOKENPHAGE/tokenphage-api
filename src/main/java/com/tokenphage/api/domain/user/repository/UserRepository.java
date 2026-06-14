package com.tokenphage.api.domain.user.repository;

import com.tokenphage.api.domain.user.repository.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    @Modifying
    @Query(value = """
        INSERT INTO users (github_id, username, created_at, updated_at)
        VALUES (:githubId, :username, NOW(), NOW())
        ON CONFLICT (github_id) DO UPDATE SET username = EXCLUDED.username, updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("githubId") Long githubId, @Param("username") String username);
}
