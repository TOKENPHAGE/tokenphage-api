package com.tokenphage.api.feature.sync.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tokenphage.api.domain.runtime.repository.DailyAgentRuntimeRepository;
import com.tokenphage.api.feature.sync.dto.request.RuntimeRecordRequest;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.response.RuntimeSyncResultResponse;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;

/**
 * RuntimeRecordService.saveRecords 의 항목별 거부 분기, 저장 호출, 경고 로그, 예외 전파를 검증한다.
 * <p>
 * 검증기는 실물을 쓴다. 제약 자체는 RuntimeRecordRequestValidationTest 가 보고, 여기서는 대표 항목만 쓴다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RuntimeRecordService 단위 테스트")
class RuntimeRecordServiceTest {

    private static final long GITHUB_ID = -42L;
    private static final String DEVICE_ID = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";

    private static ValidatorFactory validatorFactory;

    @Mock
    private DailyAgentRuntimeRepository runtimeRepo;

    @Mock
    private Jwt jwt;

    private RuntimeRecordService service;
    private Logger serviceLogger;
    private ListAppender<ILoggingEvent> logs;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        service = new RuntimeRecordService(runtimeRepo, validatorFactory.getValidator());
        given(jwt.getSubject()).willReturn(String.valueOf(GITHUB_ID));
        serviceLogger = (Logger) LoggerFactory.getLogger(RuntimeRecordService.class);
        logs = new ListAppender<>();
        logs.start();
        serviceLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logs);
    }

    private static RuntimeRecordRequest runtime(String date, String model, long runtimeMs) {
        return new RuntimeRecordRequest(date, "claude", model, runtimeMs);
    }

    private static SyncRequest request(RuntimeRecordRequest... runtimes) {
        return new SyncRequest(DEVICE_ID, List.of(), Arrays.asList(runtimes));
    }

    private List<ILoggingEvent> warnings() {
        return logs.list.stream().filter(event -> event.getLevel() == Level.WARN).toList();
    }

    @Nested
    @DisplayName("saveRecords - 정상 저장")
    class SaveRecordsSuccess {

        @Test
        @DisplayName("정상 항목을 모두 저장하고 accepted 수를 돌려준다")
        void 실행시간저장_정상2건_모두저장하고accepted2() {
            // given
            SyncRequest req = request(
                    runtime("2026-10-09", "claude-opus-4-1", 7_200_000L),
                    runtime("2026-10-10", "claude-sonnet-4-5", 0L));

            // when
            RuntimeSyncResultResponse result = service.saveRecords(jwt, req);

            // then
            assertThat(result).isEqualTo(new RuntimeSyncResultResponse(2, 0));
            then(runtimeRepo).should().upsertRecord(GITHUB_ID, UUID.fromString(DEVICE_ID),
                    LocalDate.of(2026, 10, 9), "claude", "claude-opus-4-1", 7_200_000L);
            then(runtimeRepo).should().upsertRecord(GITHUB_ID, UUID.fromString(DEVICE_ID),
                    LocalDate.of(2026, 10, 10), "claude", "claude-sonnet-4-5", 0L);
        }

        @Test
        @DisplayName("거부가 0건이면 경고 로그를 남기지 않는다")
        void 실행시간저장_거부0건_경고로그없음() {
            // given
            SyncRequest req = request(runtime("2026-10-09", "claude-opus-4-1", 1_000L));

            // when
            service.saveRecords(jwt, req);

            // then
            assertThat(warnings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("saveRecords - 항목 거부")
    class SaveRecordsRejected {

        @Test
        @DisplayName("null 항목과 없는 날짜(2026-02-30)는 거부로 세고 나머지만 저장한다")
        void 실행시간저장_null항목과없는날짜_거부로집계() {
            // given: 2026-02-30 은 형식 검사는 통과하지만 실제 날짜가 아니다
            SyncRequest req = request(
                    runtime("2026-10-09", "claude-opus-4-1", 1_000L),
                    null,
                    runtime("2026-02-30", "claude-opus-4-1", 2_000L));

            // when
            RuntimeSyncResultResponse result = service.saveRecords(jwt, req);

            // then
            assertThat(result).isEqualTo(new RuntimeSyncResultResponse(1, 2));
            then(runtimeRepo).should(times(1)).upsertRecord(anyLong(), any(UUID.class), any(LocalDate.class),
                    anyString(), anyString(), anyLong());
            assertThat(warnings()).singleElement()
                    .extracting(ILoggingEvent::getFormattedMessage)
                    .isEqualTo("Runtime records rejected: githubId=-42, rejected=2, accepted=1");
        }

        @Test
        @DisplayName("전부 거부되면 저장소를 호출하지 않는다")
        void 실행시간저장_전부거부_저장소미호출() {
            // given: 대표 위반 1건(허용 목록 밖 source)과 null 항목
            SyncRequest req = request(new RuntimeRecordRequest("2026-10-09", "cursor", "gpt-5.5", 1_000L), null);

            // when
            RuntimeSyncResultResponse result = service.saveRecords(jwt, req);

            // then
            assertThat(result).isEqualTo(new RuntimeSyncResultResponse(0, 2));
            then(runtimeRepo).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("saveRecords - 저장 실패")
    class SaveRecordsFailure {

        @Test
        @DisplayName("저장소 예외는 삼키지 않고 그대로 전파한다")
        void 실행시간저장_저장소예외_그대로전파() {
            // given
            DataIntegrityViolationException dbError = new DataIntegrityViolationException("runtime write failed");
            willThrow(dbError).given(runtimeRepo).upsertRecord(anyLong(), any(UUID.class), any(LocalDate.class),
                    anyString(), anyString(), anyLong());
            SyncRequest req = request(runtime("2026-10-09", "claude-opus-4-1", 1_000L));

            // when / then
            assertThatThrownBy(() -> service.saveRecords(jwt, req)).isSameAs(dbError);
        }
    }
}
