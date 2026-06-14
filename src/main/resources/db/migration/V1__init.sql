CREATE TABLE users (
    github_id   BIGINT      PRIMARY KEY,
    username    VARCHAR(40) NOT NULL UNIQUE,
    level       SMALLINT    NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE daily_token_usage (
    github_id   BIGINT      NOT NULL REFERENCES users(github_id),
    device_id   UUID        NOT NULL,
    usage_date  DATE        NOT NULL,
    model       VARCHAR(80) NOT NULL,
    input_tok   BIGINT      NOT NULL DEFAULT 0,
    output_tok  BIGINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (github_id, device_id, usage_date, model)
);

CREATE INDEX idx_dtu_github_date  ON daily_token_usage(github_id, usage_date DESC);
CREATE INDEX idx_dtu_github_model ON daily_token_usage(github_id, model);
