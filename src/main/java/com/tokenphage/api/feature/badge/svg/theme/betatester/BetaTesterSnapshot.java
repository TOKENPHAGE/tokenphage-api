package com.tokenphage.api.feature.badge.svg.theme.betatester;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * beta-tester 배지의 스냅샷 payload 스키마 (badge_snapshot.payload JSON).
 * <p>
 * 스키마 해석은 이 배지 패키지가 소유한다 — 조회 계층은 JSON을 문자열로만 다룬다.
 * 키 이름은 V7 적재 쿼리의 jsonb_build_object와 1:1로 일치해야 한다.
 *
 * @param signupRank  가입 순위 (users.created_at 오름차순, 0 이하면 미표시)
 * @param period      베타 기간 표시 문자열 (빈 문자열이면 미표시)
 * @param syncsRun    동기화 횟수 표시 문자열
 * @param badgeServed 배지 서빙 횟수 표시 문자열 (단위 축약 완료 상태)
 * @param tokensAdded 누적 토큰 표시 문자열 (단위 축약 완료 상태)
 * @param isClaudeUse 베타 기간 claude 계열 모델 사용 여부
 * @param isGptUse    베타 기간 gpt 계열 모델 사용 여부
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record BetaTesterSnapshot(int signupRank, String period, String syncsRun, String badgeServed,
                          String tokensAdded, boolean isClaudeUse, boolean isGptUse) {

    private static final Logger log = LoggerFactory.getLogger(BetaTesterSnapshot.class);

    /** 스프링 빈 대신 정적 보관 — 테마 생성자에 인자가 붙으면 "new로 생성" 테스트 정책이 깨진다. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 통계 결손 시 자리 표시 문자열. */
    private static final String PLACEHOLDER = "-";

    /**
     * payload JSON을 파싱한다. null·공백·깨진 JSON이면 {@link #empty()}로 접고 예외를 던지지 않는다.
     *
     * @param json badge_snapshot.payload 원문 (null 허용)
     * @return 정규화된 스냅샷 (문자열 필드는 null이 아님)
     * @Since 2026-08-23
     */
    static BetaTesterSnapshot parse(String json) {
        if (json == null || json.isBlank()) {
            return empty();
        }
        try {
            return MAPPER.readValue(json, BetaTesterSnapshot.class).normalized();
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse beta tester snapshot payload, rendering empty: {}", e.getMessage());
            return empty();
        }
    }

    /**
     * 스냅샷이 없거나 깨졌을 때 쓰는 빈 스냅샷을 반환한다.
     * <p>
     * 순번·기간은 미표시 판정값(0/""), 통계 3칸은 자리 표시 "-"다.
     * 없는 값을 0이라고 주장하지 않기 위해 0을 쓰지 않는다.
     *
     * @return 빈 스냅샷
     * @Since 2026-08-23
     */
    static BetaTesterSnapshot empty() {
        return new BetaTesterSnapshot(0, "", PLACEHOLDER, PLACEHOLDER, PLACEHOLDER, false, false);
    }

    /**
     * null·공백 문자열 필드를 표시 가능한 값으로 접는다 (테마가 null을 만나지 않게).
     */
    private BetaTesterSnapshot normalized() {
        return new BetaTesterSnapshot(
                signupRank,
                period == null ? "" : period,
                blankToPlaceholder(syncsRun),
                blankToPlaceholder(badgeServed),
                blankToPlaceholder(tokensAdded),
                isClaudeUse,
                isGptUse);
    }

    private static String blankToPlaceholder(String value) {
        if (value == null || value.isBlank()) {
            return PLACEHOLDER;
        }
        return value;
    }
}
