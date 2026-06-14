-- users.level 컬럼 제거.
-- 레벨은 배지 테마별로 totalTokens에서 계산하는 표현 계층 개념(ClaudeMascot.levelFor)이라
-- 단일 영속 컬럼은 의미가 없고, 실제로 어디서도 읽히지 않는 죽은 상태였다.
ALTER TABLE users DROP COLUMN level;
