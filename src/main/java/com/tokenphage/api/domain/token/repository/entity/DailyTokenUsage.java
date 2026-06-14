package com.tokenphage.api.domain.token.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "daily_token_usage")
@IdClass(DailyTokenUsageId.class)
@Getter @Setter @NoArgsConstructor
public class DailyTokenUsage {

    @Id
    @Column(name = "github_id")
    private Long githubId;

    @Id
    @Column(name = "device_id")
    private UUID deviceId;

    @Id
    @Column(name = "usage_date")
    private LocalDate usageDate;

    @Id
    @Column(length = 80)
    private String model;

    @Column(name = "input_tok", nullable = false)
    private long inputTok;

    @Column(name = "output_tok", nullable = false)
    private long outputTok;

    @Column(name = "cache_read_tok", nullable = false)
    private long cacheReadTok;

    @Column(name = "cache_create_tok", nullable = false)
    private long cacheCreateTok;
}
