-- AI 실행시간 일별 합계. 한 줄은 사용자·기기·날짜·도구(source)·모델별 실행시간(ms)이다.
CREATE TABLE daily_agent_runtime (
    github_id   BIGINT      NOT NULL REFERENCES users (github_id),
    device_id   UUID        NOT NULL,
    usage_date  DATE        NOT NULL,
    source      VARCHAR(16) NOT NULL,
    model       VARCHAR(80) NOT NULL,
    runtime_ms  BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (github_id, device_id, usage_date, source, model),
    CONSTRAINT chk_dar_source CHECK (source IN ('claude', 'codex', 'opencode')),
    CONSTRAINT chk_dar_runtime_ms CHECK (runtime_ms >= 0)
);

CREATE INDEX idx_dar_github_date ON daily_agent_runtime (github_id, usage_date DESC);
