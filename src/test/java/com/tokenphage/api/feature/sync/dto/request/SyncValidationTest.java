package com.tokenphage.api.feature.sync.dto.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SyncRequest / TokenRecordRequest의 Bean Validation 제약을 Validator로 직접 검증한다.
 * <p>
 * Spring 컨텍스트나 DB/Redis를 띄우지 않는 순수 단위 테스트 — 메모리 DTO만 사용한다.
 */
@DisplayName("Sync 요청 DTO Bean Validation")
class SyncValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private TokenRecordRequest validRecord() {
        return new TokenRecordRequest("2026-06-11", "claude-opus-4", 1, 1, 0, 0);
    }

    private static boolean hasViolationOn(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Nested
    @DisplayName("SyncRequest")
    class SyncReq {

        @Test
        @DisplayName("정상 요청 → 위반 없음")
        void 동기화요청_정상_위반없음() {
            // given
            SyncRequest req = new SyncRequest(VALID_UUID, List.of(validRecord()));

            // when
            var violations = validator.validate(req);

            // then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest(name = "deviceId=\"{0}\" → 거부")
        @DisplayName("deviceId가 blank/비UUID/null → deviceId 위반")
        @NullSource
        @ValueSource(strings = {"", "   ", "not-a-uuid", "550e8400"})
        void 동기화요청_deviceId무효_위반발생(String deviceId) {
            // given
            SyncRequest req = new SyncRequest(deviceId, List.of(validRecord()));

            // when
            var violations = validator.validate(req);

            // then
            assertThat(hasViolationOn(violations, "deviceId")).isTrue();
        }

        @Test
        @DisplayName("records=null → records 위반")
        void 동기화요청_records널_위반발생() {
            // given
            SyncRequest req = new SyncRequest(VALID_UUID, null);

            // when
            var violations = validator.validate(req);

            // then
            assertThat(hasViolationOn(violations, "records")).isTrue();
        }

        @Test
        @DisplayName("records 비어있음 → records 위반")
        void 동기화요청_records비어있음_위반발생() {
            // given
            SyncRequest req = new SyncRequest(VALID_UUID, List.of());

            // when
            var violations = validator.validate(req);

            // then
            assertThat(hasViolationOn(violations, "records")).isTrue();
        }

        @Test
        @DisplayName("records 내부 레코드가 무효 → 중첩 검증(@Valid) 전파")
        void 동기화요청_중첩레코드무효_위반전파() {
            // given
            TokenRecordRequest bad = new TokenRecordRequest("2026/06/11", "m", -1, 0, 0, 0);
            SyncRequest req = new SyncRequest(VALID_UUID, List.of(bad));

            // when
            var violations = validator.validate(req);

            // then
            assertThat(violations).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("TokenRecordRequest")
    class RecordReq {

        @ParameterizedTest(name = "date=\"{0}\" → 거부")
        @DisplayName("date가 blank/비ISO/null → date 위반")
        @NullSource
        @ValueSource(strings = {"", "   ", "06/11/2026", "2026.06.11", "june"})
        void 레코드_date무효_위반발생(String date) {
            // given
            TokenRecordRequest r = new TokenRecordRequest(date, "claude", 1, 1, 0, 0);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "date")).isTrue();
        }

        @Test
        @DisplayName("date가 정확한 ISO(yyyy-MM-dd) → 위반 없음")
        void 레코드_dateISO_위반없음() {
            // given
            TokenRecordRequest r = new TokenRecordRequest("2026-06-11", "claude", 0, 0, 0, 0);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest(name = "model=\"{0}\" → 거부")
        @DisplayName("model이 blank/null → model 위반")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void 레코드_model무효_위반발생(String model) {
            // given
            TokenRecordRequest r = new TokenRecordRequest("2026-06-11", model, 1, 1, 0, 0);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "model")).isTrue();
        }

        @Test
        @DisplayName("토큰 4필드가 음수 → 각 필드 위반")
        void 레코드_토큰음수_각필드위반발생() {
            // given
            TokenRecordRequest r = new TokenRecordRequest("2026-06-11", "claude", -1, -1, -1, -1);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "inputTok")).isTrue();
            assertThat(hasViolationOn(violations, "outputTok")).isTrue();
            assertThat(hasViolationOn(violations, "cacheReadTok")).isTrue();
            assertThat(hasViolationOn(violations, "cacheCreateTok")).isTrue();
        }

        @Test
        @DisplayName("토큰 4필드가 0(경계값) → 위반 없음")
        void 레코드_토큰0_위반없음() {
            // given
            TokenRecordRequest r = new TokenRecordRequest("2026-06-11", "claude", 0, 0, 0, 0);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(violations).isEmpty();
        }
    }
}
