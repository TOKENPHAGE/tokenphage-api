package com.tokenphage.api.feature.badge.dto.response;

import java.util.List;

/**
 * 배지 렌더링에 필요한 집계 데이터 (프로세스 내부 캐리어 — wire/캐시 직렬화 대상 아님).
 * <p>
 * 각 필드는 {@link BadgeDataNeed} 1개와 <b>이름으로 1:1 대응</b>한다 (예: {@code daily30d} ↔ {@code DAILY_30D}).
 * 각 테마의 {@link BadgeTheme#needs()} 에 필요한 field 선언,
 * 여기에 테마 목록을 두지 않는다 (테마가 늘어도 이 파일은 무수정). 요구되지 않은 필드는 빈값(0 / 빈 리스트 / 0.0)이다.
 * {@code daily30d}·{@code daily1y}는 윈도우(30일·1년)만 다른 같은 종류의 일별 시계열이다.
 *
 * @param username     GitHub 사용자명 (공통)
 * @param totalTokens  전 기간 누적 토큰 합계 — {@code TOTAL_TOKENS}
 * @param daily30d     최근 30일 일별 사용량 — {@code DAILY_30D}
 * @param topModels    모델별 사용량 상위 5개 — {@code TOP_MODELS}
 * @param cacheHitRate 캐시 적중률 0.0~1.0 — {@code CACHE_HIT_RATE}
 * @param yearTokens   최근 1년 총 토큰 — {@code YEAR_TOKENS}
 * @param streakDays   연속 사용일 — {@code STREAK_DAYS}
 * @param daily1y      최근 1년 일별 사용량 — {@code DAILY_1Y}
 * @param snapshot     고정 스냅샷 payload JSON 원문 — {@code BADGE_SNAPSHOT} (없으면 빈 문자열, 해석은 배지 패키지 소유)
 */
public record BadgeResponse(
    // ── 공통 ──
    String username,
    // ── 통계 카드 데이터 (gpu · claude) ──
    long totalTokens,
    List<DailyCountResponse> daily30d,
    List<ModelCountResponse> topModels,
    double cacheHitRate,
    // ── 1년 활동 데이터 (grass 계열) ──
    long yearTokens,
    int streakDays,
    List<DailyCountResponse> daily1y,
    // ── 고정 스냅샷 (beta-tester 등 값이 변하지 않는 배지) ──
    String snapshot
) {}
