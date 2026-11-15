-- 가동시간 주행거리계 배지 2종 등록. code는 BadgeCode 상수 및 BadgeTheme.name()과 일치해야 한다.
INSERT INTO badge_catalog (code, display_name, require_grant, locked_message) VALUES
    ('odometer-claude', 'Runtime Odometer Claude', false, NULL),
    ('odometer-gpt',    'Runtime Odometer GPT',    false, NULL);
