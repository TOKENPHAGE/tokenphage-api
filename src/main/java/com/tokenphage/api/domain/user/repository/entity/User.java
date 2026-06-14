package com.tokenphage.api.domain.user.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.Instant;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor
public class User {

    @Id
    @Column(name = "github_id")
    private Long githubId;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public User(Long githubId, String username) {
        this.githubId = githubId;
        this.username = username;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }
}
