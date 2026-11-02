package com.tokenphage.api.domain.runtime.repository.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.UUID;

/**
 * AI 실행시간 일별 합계 엔티티. 한 줄은 사용자·기기·날짜·도구·모델별 실행시간(ms)이다.
 * <p>
 * 저장은 저장소의 네이티브 upsert 로만 해서 setter 가 없다.
 * 앱 기동 때 ddl-auto validate 가 이 매핑과 V8 표를 대조한다.
 */
@Entity
@Table(name = "daily_agent_runtime")
@IdClass(DailyAgentRuntimeId.class)
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyAgentRuntime {

    @Id
    @Column(name = "github_id")
    private Long githubId;

    @Id
    @Column(name = "device_id")
    private UUID deviceId;

    @Id
    @Column(name = "usage_date")
    private LocalDate usageDate;

    /** 로그를 읽은 도구 (claude, codex, opencode). */
    @Id
    @Column(name = "source", length = 16)
    private String source;

    @Id
    @Column(name = "model", length = 80)
    private String model;

    @Column(name = "runtime_ms", nullable = false)
    private long runtimeMs;
}
