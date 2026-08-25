-- 값이 고정된 배지의 미리 계산된 표시 데이터. 렌더 시점 집계를 대체한다.
CREATE TABLE badge_snapshot (
    github_id   BIGINT       NOT NULL REFERENCES users(github_id),
    badge_code  VARCHAR(40)  NOT NULL REFERENCES badge_catalog(code),
    payload     JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (github_id, badge_code)
);

-- Beta Tester 배지 등록. code는 BadgeCode 상수 및 BadgeTheme.name()과 일치해야 한다.
INSERT INTO badge_catalog (code, display_name, require_grant, locked_message) VALUES
    ('beta-tester', 'Beta Tester', true, '베타 테스터 전용 배지입니다');

-- 스냅샷 표시 문자열용 단위 축약. 소수 첫째 자리를 유지하되 .0이면 떼어낸다.
-- 프로토타입 표기가 "324.0T"가 아니라 "324T"이므로 SvgText.formatTokens와 결과가 다르다.
CREATE FUNCTION badge_compact_number(v BIGINT) RETURNS TEXT
LANGUAGE sql IMMUTABLE AS $fn$
    SELECT CASE
        WHEN v >= 1000000000000 THEN rtrim(rtrim(to_char(v / 1e12, 'FM999999990.9'), '0'), '.') || 'T'
        WHEN v >= 1000000000    THEN rtrim(rtrim(to_char(v / 1e9,  'FM999999990.9'), '0'), '.') || 'B'
        WHEN v >= 1000000       THEN rtrim(rtrim(to_char(v / 1e6,  'FM999999990.9'), '0'), '.') || 'M'
        WHEN v >= 1000          THEN rtrim(rtrim(to_char(v / 1e3,  'FM999999990.9'), '0'), '.') || 'K'
        ELSE v::text
    END
$fn$;

