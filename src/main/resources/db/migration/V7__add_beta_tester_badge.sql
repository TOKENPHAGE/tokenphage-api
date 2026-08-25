-- beta-tester 데이터 적재 3종: 스냅샷 + 자격 + 이력 (카탈로그 등록은 V6).
-- 베타 참여자의 표시 값을 스냅샷으로 굳힌다. 이 배지는 값이 변하지 않으므로 여기서 한 번만 계산한다.
-- 대상: 2026-08-08 이하 가입자 (created_at < 2026-08-09 00:00 +09)
-- 집계 창: 2026-07-01 ~ 2026-08-07 — 표시 라벨 "CONTRIBUTED DURING BETA"와 범위를 맞춘다
-- 누적 토큰 정의는 DailyTokenUsageRepository.sumTotalTokens와 동일하다 (input_tok + output_tok, 캐시 제외)
INSERT INTO badge_snapshot (github_id, badge_code, payload)
SELECT b.github_id,
       'beta-tester',
       jsonb_build_object(
           'signupRank',  b.signup_rank,
           'period',      '2026.07 – 2026.08',
           'syncsRun',    b.syncs_run::text,
           'badgeServed', badge_compact_number(b.badge_served),
           'tokensAdded', badge_compact_number(b.tokens_added),
           'isClaudeUse', b.is_claude_use,
           'isGptUse',    b.is_gpt_use)
FROM (
    SELECT u.github_id,
           -- 필터된 집합에서 계산해도 전체 기준과 같다 — 컷오프 이후 가입자는 항상 더 큰 순위다.
           (ROW_NUMBER() OVER (ORDER BY u.created_at, u.github_id))::int AS signup_rank,
           (SELECT count(*) FROM request_audit_log a
             WHERE a.github_id   = u.github_id
               AND a.feature     = 'sync'
               AND a.status_code < 400
               AND (a.occurred_at AT TIME ZONE 'Asia/Seoul')::date
                   BETWEEN DATE '2026-07-01' AND DATE '2026-08-07')          AS syncs_run,
           -- request_path 접두 비교는 인덱스를 타지 않지만 이 마이그레이션에서 한 번만 돈다.
           (SELECT count(*) FROM request_audit_log a
             WHERE a.request_path LIKE '/badge/' || u.username || '%'
               AND a.status_code  < 400
               AND (a.occurred_at AT TIME ZONE 'Asia/Seoul')::date
                   BETWEEN DATE '2026-07-01' AND DATE '2026-08-07')          AS badge_served,
           -- sum(bigint)은 numeric을 반환하므로 badge_compact_number(bigint) 시그니처에 맞춰 캐스팅한다.
           (SELECT COALESCE(sum(d.input_tok + d.output_tok), 0)::bigint
              FROM daily_token_usage d
             WHERE d.github_id  = u.github_id
               AND d.usage_date BETWEEN DATE '2026-07-01' AND DATE '2026-08-07') AS tokens_added,
           EXISTS (SELECT 1 FROM daily_token_usage d
                    WHERE d.github_id  = u.github_id
                      AND d.model      ILIKE '%claude%'
                      AND d.usage_date BETWEEN DATE '2026-07-01' AND DATE '2026-08-07') AS is_claude_use,
           EXISTS (SELECT 1 FROM daily_token_usage d
                    WHERE d.github_id  = u.github_id
                      AND d.model      ILIKE '%gpt%'
                      AND d.usage_date BETWEEN DATE '2026-07-01' AND DATE '2026-08-07') AS is_gpt_use
    FROM users u
    WHERE u.created_at < TIMESTAMPTZ '2026-08-09 00:00:00+09'
) b;

-- 베타 참여자 전원에게 자격을 부여한다. 대상 집합은 스냅샷 적재와 동일해야 한다
-- (어긋나면 "자격은 있는데 스냅샷이 없는" 사용자가 마이그레이션 시점부터 생긴다).
INSERT INTO user_badge_grant (github_id, badge_code, granted_by, grant_note)
SELECT u.github_id, 'beta-tester', 'TKPG-ADMIN', 'closed beta 2026-07~08 participant (V7 bulk grant)'
FROM users u
WHERE u.created_at < TIMESTAMPTZ '2026-08-09 00:00:00+09';

-- 부여 이력. 자격 행과 항상 세트로 남긴다 (append-only 감사, action은 CHECK 제약상 GRANT/REVOKE만).
INSERT INTO badge_grant_history (github_id, badge_code, action, actor, reason)
SELECT u.github_id, 'beta-tester', 'GRANT', 'TKPG-ADMIN', 'closed beta 2026-07~08 participant (V7 bulk grant)'
FROM users u
WHERE u.created_at < TIMESTAMPTZ '2026-08-09 00:00:00+09';
