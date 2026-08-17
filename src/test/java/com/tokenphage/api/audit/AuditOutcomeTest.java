package com.tokenphage.api.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 감사 로그 outcome 값을 검증한다.
 * <p>
 * DB에 이미 쌓인 문자열이라 값이 바뀌면 과거 이력 조회가 끊긴다. 컴파일러가 잡아주지 못하므로
 * 문자열을 그대로 단언해 고정한다. ATTRIBUTE_KEY는 양쪽이 같은 상수를 참조해 컴파일러가
 * 일치를 보장하므로 테스트하지 않는다.
 */
class AuditOutcomeTest {

    /** request_audit_log.outcome 컬럼 길이. */
    private static final int OUTCOME_MAX_LENGTH = 40;

    @Nested
    @DisplayName("결과 코드")
    class CodeTest {

        @ParameterizedTest(name = "[{index}] {0} → \"{1}\"")
        @CsvSource({
                "SUCCESS, success",
                "UNKNOWN, unknown",
                "BADGE_GRANT_DENIED, badge_grant_denied"
        })
        @DisplayName("코드조회_각상수_정해진문자열반환")
        void 코드조회_각상수_정해진문자열반환(AuditOutcome outcome, String expected) {
            // given

            // when
            String result = outcome.getCode();

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("코드조회_모든상수_40자이하")
        void 코드조회_모든상수_40자이하() {
            // given
            // 넘치면 RequestAuditService가 조용히 잘라 조회가 어긋난다.

            // when
            // then
            for (AuditOutcome outcome : AuditOutcome.values()) {
                assertThat(outcome.getCode()).hasSizeLessThanOrEqualTo(OUTCOME_MAX_LENGTH);
            }
        }

        @Test
        @DisplayName("코드조회_모든상수_중복없음")
        void 코드조회_모든상수_중복없음() {
            // given
            // 값이 겹치면 로그에서 원인을 구분할 수 없다.

            // when
            Set<String> codes = Arrays.stream(AuditOutcome.values())
                    .map(AuditOutcome::getCode)
                    .collect(Collectors.toSet());

            // then
            assertThat(codes).hasSize(AuditOutcome.values().length);
        }
    }

}
