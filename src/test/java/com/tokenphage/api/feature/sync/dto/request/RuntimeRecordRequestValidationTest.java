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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RuntimeRecordRequest 의 Bean Validation 제약을 Validator 로 직접 검증한다.
 * <p>
 * 이 제약은 컨트롤러의 @Valid 가 아니라 실행시간 서비스가 항목마다 적용한다. 순수 단위 테스트다.
 */
@DisplayName("실행시간 항목 DTO Bean Validation")
class RuntimeRecordRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    // 한계값은 DTO 가 단일 출처다. DDL 과의 대조는 RuntimeRecordSchemaConstraintTest 가 담당한다.
    private static final int MODEL_MAX_LENGTH = RuntimeRecordRequest.MODEL_MAX_LENGTH;
    private static final long RUNTIME_MS_MAX = RuntimeRecordRequest.RUNTIME_MS_MAX;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static RuntimeRecordRequest runtime(String date, String source, String model, Long runtimeMs) {
        return new RuntimeRecordRequest(date, source, model, runtimeMs);
    }

    private static boolean hasViolationOn(Set<? extends ConstraintViolation<?>> violations, String field) {
        return violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(field));
    }

    @Nested
    @DisplayName("정상 항목")
    class Valid {

        @Test
        @DisplayName("날짜·도구·모델·실행시간이 모두 맞으면 위반 없음")
        void 실행시간항목_정상_위반없음() {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", "claude-opus-4-1", 7_200_000L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(violations).isEmpty();
        }
    }

    @Nested
    @DisplayName("date")
    class Date {

        @ParameterizedTest(name = "date=\"{0}\" → 거부")
        @DisplayName("date가 null·빈 값·yyyy-MM-dd가 아님 → date 위반")
        @NullSource
        @ValueSource(strings = {"", "2026-1-1", "2026/10/09"})
        void 실행시간항목_date형식위반_위반발생(String date) {
            // given
            RuntimeRecordRequest r = runtime(date, "claude", "claude-opus-4-1", 1L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "date")).isTrue();
        }
    }

    @Nested
    @DisplayName("source")
    class Source {

        @ParameterizedTest(name = "source=\"{0}\" → 통과")
        @DisplayName("source가 허용 목록 안 → 위반 없음")
        @ValueSource(strings = {"claude", "codex", "opencode"})
        void 실행시간항목_source허용목록_위반없음(String source) {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", source, "claude-opus-4-1", 1L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(violations).isEmpty();
        }

        @ParameterizedTest(name = "source=\"{0}\" → 거부")
        @DisplayName("source가 null·허용 목록 밖 → source 위반")
        @NullSource
        @ValueSource(strings = {"", "cursor", "Claude"})
        void 실행시간항목_source허용목록밖_위반발생(String source) {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", source, "claude-opus-4-1", 1L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "source")).isTrue();
        }
    }

    @Nested
    @DisplayName("model")
    class Model {

        @ParameterizedTest(name = "model=\"{0}\" → 거부")
        @DisplayName("model이 null·빈 값 → model 위반")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void 실행시간항목_model빈값_위반발생(String model) {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", model, 1L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "model")).isTrue();
        }

        @Test
        @DisplayName("model이 컬럼 폭(80자)이면 위반 없음 - 경계")
        void 실행시간항목_model80자_위반없음() {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", "m".repeat(MODEL_MAX_LENGTH), 1L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("model이 81자면 model 위반 - 경계")
        void 실행시간항목_model81자_위반발생() {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", "m".repeat(MODEL_MAX_LENGTH + 1), 1L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "model")).isTrue();
        }
    }

    @Nested
    @DisplayName("runtimeMs")
    class RuntimeMs {

        @Test
        @DisplayName("runtimeMs가 빠지면(null) runtimeMs 위반")
        void 실행시간항목_runtimeMs누락_위반발생() {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", "claude-opus-4-1", null);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "runtimeMs")).isTrue();
        }

        @Test
        @DisplayName("runtimeMs가 -1이면 runtimeMs 위반 - 경계")
        void 실행시간항목_runtimeMs음수_위반발생() {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", "claude-opus-4-1", -1L);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "runtimeMs")).isTrue();
        }

        @ParameterizedTest(name = "runtimeMs={0} → 통과")
        @DisplayName("runtimeMs가 0 또는 상한(8,640,000,000)이면 위반 없음 - 경계")
        @ValueSource(longs = {0L, RUNTIME_MS_MAX})
        void 실행시간항목_runtimeMs경계안_위반없음(long runtimeMs) {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", "claude-opus-4-1", runtimeMs);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(violations).isEmpty();
        }

        @Test
        @DisplayName("runtimeMs가 상한을 1 넘으면(8,640,000,001) runtimeMs 위반 - 경계")
        void 실행시간항목_runtimeMs상한초과_위반발생() {
            // given
            RuntimeRecordRequest r = runtime("2026-10-09", "claude", "claude-opus-4-1", RUNTIME_MS_MAX + 1);

            // when
            var violations = validator.validate(r);

            // then
            assertThat(hasViolationOn(violations, "runtimeMs")).isTrue();
        }
    }
}
