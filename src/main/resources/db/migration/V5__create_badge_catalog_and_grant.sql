-- 배지 카탈로그: URL ?theme= 로 노출되는 배지 종류의 단일 출처(single source of truth).
-- code는 BadgeCode enum 및 BadgeTheme 빈의 name()과 1:1로 일치해야 하며, 부팅 시 3자 대조한다.
CREATE TABLE badge_catalog (
    code            VARCHAR(40)  PRIMARY KEY
                                 CHECK (code ~ '^[a-z0-9][a-z0-9-]{0,39}$'),
                                 -- 캐시 키 구분자 ':' 와 SCAN 와일드카드 '*' 혼입을 DB에서 차단.
                                 -- 대문자·언더스코어·점을 금지하는 의도된 제약이다.
    display_name    VARCHAR(60)  NOT NULL,                  -- README/문서 노출용 표시명. 잠금 안내의 제목으로도 쓰인다
    require_grant   BOOLEAN      NOT NULL DEFAULT false,    -- true면 자격을 부여받은 사용자만 쓸 수 있는 배지
    locked_message  VARCHAR(120),                           -- 자격이 없을 때 보여줄 안내 문구. NULL이면 기본 문구
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 사용자-배지 자격(grant) 다대다: private 배지를 쓸 수 있는 "배지 주인"을 잇는다.
-- 요청자(뷰어)가 아니라 /badge/{username} 경로의 username 주체에 대한 자격이다.
-- GitHub 개명 시 users.upsert의 ON CONFLICT로 username이 갱신되므로 반드시 github_id로 잇는다.
CREATE TABLE user_badge_grant (
    github_id    BIGINT       NOT NULL REFERENCES users(github_id),
    badge_code   VARCHAR(40)  NOT NULL REFERENCES badge_catalog(code),
    granted_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    granted_by   VARCHAR(40),                          -- 자격을 부여한 운영자 GitHub username
    grant_note   VARCHAR(255),                         -- 사유 (예: 'PR tokenphage-api#42')
    PRIMARY KEY (github_id, badge_code)
);

-- 배지별 보유자 역방향 조회(운영 확인용). 렌더 경로의 정방향 조회는 PK 선두 컬럼으로 커버된다.
CREATE INDEX idx_ubg_code_granted ON user_badge_grant(badge_code, granted_at DESC);

-- 자격 부여/회수 이력: append-only. 자격 행이 DELETE되어도 "누가/언제/왜"는 남는다.
-- FK 미설정은 request_audit_log와 동일 정책(감사 기록 독립 보존).
-- actor는 SYSTEM/ADMIN 같은 enum이 아니라 GitHub username이다 — enum이면 '누구'를 기록할 수 없다.
CREATE TABLE badge_grant_history (
    id           BIGSERIAL    PRIMARY KEY,
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    github_id    BIGINT       NOT NULL,
    badge_code   VARCHAR(40)  NOT NULL,
    action       VARCHAR(10)  NOT NULL,                 -- GRANT / REVOKE
    actor        VARCHAR(40),                           -- 부여/회수를 수행한 GitHub username
    reason       VARCHAR(255),
    CHECK (action IN ('GRANT', 'REVOKE'))
);

CREATE INDEX idx_bgh_github_time ON badge_grant_history(github_id, occurred_at DESC);

-- 현재 등록된 BadgeTheme 빈 4종을 시드한다.
-- 이 시드가 없으면 부팅 정합성 검증이 실패해 기동이 멈춘다(의도된 강제).
-- locked는 자격 거부 시 렌더되는 안내 배지이며, 그 자체는 자격을 요구하지 않는다(요구하면 무한 재귀).
-- locked_message가 NULL인 배지는 잠금 안내에서 코드 상수 기본 문구를 쓴다.
INSERT INTO badge_catalog (code, display_name, require_grant, locked_message) VALUES
    ('gpu',          'GPU Card',      false, NULL),
    ('claude',       'Claude Card',   false, NULL),
    ('grass-claude', 'Claude Grass',  false, NULL),
    ('locked',       'Locked Notice', false, NULL);

-- 향후 private 배지를 추가할 때의 예시(V6~에서 테마 클래스·BadgeCode 상수와 함께 등록):
--   INSERT INTO badge_catalog (code, display_name, require_grant, locked_message) VALUES
--       ('contributor', 'Contributor', true, 'PR을 보내고 이 배지를 받아보세요'),
--       ('beta-tester', 'Beta Tester', true, '베타 테스터 전용 배지입니다');
