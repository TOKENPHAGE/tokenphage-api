-- 요청 감사 로그: 컨트롤러에 도달한 요청의 메타데이터만 append-only로 적재한다.
-- 본문(프롬프트/응답 바디)은 저장하지 않는다(privacy-by-design). 무기한 보관, 파티션 미적용(후속 판단).
-- 적재 경로(AOP Aspect + 비동기 서비스)는 후속 단계에서 추가한다.
CREATE TABLE request_audit_log (
    id            BIGSERIAL    PRIMARY KEY,
    occurred_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    feature       VARCHAR(40)  NOT NULL,      -- 서비스(슬라이스)명: auth/sync/badge/reset
    action        VARCHAR(60)  NOT NULL,      -- 기능명: 컨트롤러 메서드명
    http_method   VARCHAR(10)  NOT NULL,
    request_path  VARCHAR(255) NOT NULL,
    status_code   INTEGER      NOT NULL,
    latency_ms    INTEGER      NOT NULL,
    client_ip     VARCHAR(45),                -- 프록시 헤더에서 추출한 실 IP (IPv6 최대 45자)
    github_id     BIGINT,                     -- 미인증 요청은 NULL (FK 미설정: 감사 로그 독립 보존)
    username      VARCHAR(40),
    user_agent    VARCHAR(255),
    outcome       VARCHAR(40)                 -- 성공/실패 결과코드 (예: gist_owner_mismatch)
);

CREATE INDEX idx_audit_github_time  ON request_audit_log(github_id, occurred_at DESC);
CREATE INDEX idx_audit_feature_time ON request_audit_log(feature, action, occurred_at DESC);
