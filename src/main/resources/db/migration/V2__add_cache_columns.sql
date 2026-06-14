ALTER TABLE daily_token_usage
    ADD COLUMN cache_read_tok   BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN cache_create_tok BIGINT NOT NULL DEFAULT 0;
