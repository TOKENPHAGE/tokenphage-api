package com.tokenphage.api.feature.sync.service;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * TokenUsageRecordService.saveRecords의 레코드 저장(upsert) 흐름과 입력 파싱 실패를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class TokenUsageRecordServiceTest {

    /** 유효한 UUID 형식의 디바이스 식별자 */
    private static final String VALID_DEVICE_ID = "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11";

    @Mock
    private DailyTokenUsageRepository tokenRepo;

    @InjectMocks
    private TokenUsageRecordService service;

    @Nested
    @DisplayName("saveRecords - 정상 저장")
    class SaveRecordsSuccess {

        @Test
        @DisplayName("레코드저장_정상2건_upsertRecord2회호출")
        void 레코드저장_정상2건_upsert2회호출() {
            // given
            Jwt jwt = mock(Jwt.class);
            given(jwt.getSubject()).willReturn("-42");
            TokenRecordRequest r1 = new TokenRecordRequest("2026-06-01", "claude-opus", 10L, 20L, 30L, 40L);
            TokenRecordRequest r2 = new TokenRecordRequest("2026-06-02", "claude-sonnet", 1L, 2L, 3L, 4L);
            SyncRequest req = new SyncRequest(VALID_DEVICE_ID, List.of(r1, r2));

            // when
            service.saveRecords(jwt, req);

            // then
            then(tokenRepo).should(times(2)).upsertRecord(
                anyLong(), any(UUID.class), any(LocalDate.class), anyString(),
                anyLong(), anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("레코드저장_첫레코드인자_githubId·deviceId·date·model·토큰값일치")
        void 레코드저장_첫레코드인자_정확히전달() {
            // given
            Jwt jwt = mock(Jwt.class);
            given(jwt.getSubject()).willReturn("-42");
            TokenRecordRequest r1 = new TokenRecordRequest("2026-06-01", "claude-opus", 10L, 20L, 30L, 40L);
            TokenRecordRequest r2 = new TokenRecordRequest("2026-06-02", "claude-sonnet", 1L, 2L, 3L, 4L);
            SyncRequest req = new SyncRequest(VALID_DEVICE_ID, List.of(r1, r2));

            // when
            service.saveRecords(jwt, req);

            // then
            ArgumentCaptor<Long> githubIdCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<UUID> deviceIdCaptor = ArgumentCaptor.forClass(UUID.class);
            ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Long> inputCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> outputCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> cacheReadCaptor = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> cacheCreateCaptor = ArgumentCaptor.forClass(Long.class);
            then(tokenRepo).should(times(2)).upsertRecord(
                githubIdCaptor.capture(),
                deviceIdCaptor.capture(),
                dateCaptor.capture(),
                modelCaptor.capture(),
                inputCaptor.capture(),
                outputCaptor.capture(),
                cacheReadCaptor.capture(),
                cacheCreateCaptor.capture());
            // 첫 번째 호출 인자가 r1과 일치하는지 검증
            assertThat(githubIdCaptor.getAllValues().get(0)).isEqualTo(-42L);
            assertThat(deviceIdCaptor.getAllValues().get(0)).isEqualTo(UUID.fromString(VALID_DEVICE_ID));
            assertThat(dateCaptor.getAllValues().get(0)).isEqualTo(LocalDate.parse("2026-06-01"));
            assertThat(modelCaptor.getAllValues().get(0)).isEqualTo("claude-opus");
            assertThat(inputCaptor.getAllValues().get(0)).isEqualTo(10L);
            assertThat(outputCaptor.getAllValues().get(0)).isEqualTo(20L);
            assertThat(cacheReadCaptor.getAllValues().get(0)).isEqualTo(30L);
            assertThat(cacheCreateCaptor.getAllValues().get(0)).isEqualTo(40L);
        }
    }

    @Nested
    @DisplayName("saveRecords - 경계값")
    class SaveRecordsBoundary {

        @Test
        @DisplayName("레코드저장_빈목록_upsertRecord미호출")
        void 레코드저장_빈목록_호출없음() {
            // given
            Jwt jwt = mock(Jwt.class);
            given(jwt.getSubject()).willReturn("-42");
            SyncRequest req = new SyncRequest(VALID_DEVICE_ID, List.of());

            // when
            service.saveRecords(jwt, req);

            // then
            then(tokenRepo).should(never()).upsertRecord(
                anyLong(), any(UUID.class), any(LocalDate.class), anyString(),
                anyLong(), anyLong(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("레코드저장_단건경계_upsertRecord정확히1회호출")
        void 레코드저장_단건_정확히1회호출() {
            // given
            Jwt jwt = mock(Jwt.class);
            given(jwt.getSubject()).willReturn("-42");
            TokenRecordRequest only = new TokenRecordRequest("2026-06-01", "claude-opus", 5L, 6L, 7L, 8L);
            SyncRequest req = new SyncRequest(VALID_DEVICE_ID, List.of(only));

            // when
            service.saveRecords(jwt, req);

            // then
            then(tokenRepo).should(times(1)).upsertRecord(
                eq(-42L),
                eq(UUID.fromString(VALID_DEVICE_ID)),
                eq(LocalDate.parse("2026-06-01")),
                eq("claude-opus"),
                eq(5L),
                eq(6L),
                eq(7L),
                eq(8L));
        }
    }

    @Nested
    @DisplayName("saveRecords - 입력 파싱 실패")
    class SaveRecordsFailure {

        @Test
        @DisplayName("레코드저장_잘못된deviceId_IllegalArgumentException")
        void 레코드저장_잘못된UUID_예외발생() {
            // given
            Jwt jwt = mock(Jwt.class);
            given(jwt.getSubject()).willReturn("-42");
            TokenRecordRequest r = new TokenRecordRequest("2026-06-01", "claude-opus", 10L, 20L, 30L, 40L);
            SyncRequest req = new SyncRequest("not-a-valid-uuid", List.of(r));

            // when / then
            assertThatThrownBy(() -> service.saveRecords(jwt, req))
                .isInstanceOf(IllegalArgumentException.class);
            then(tokenRepo).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("레코드저장_파싱불가date_DateTimeParseException")
        void 레코드저장_잘못된날짜_예외발생() {
            // given
            Jwt jwt = mock(Jwt.class);
            given(jwt.getSubject()).willReturn("-42");
            TokenRecordRequest badDate = new TokenRecordRequest("2026-13-99", "claude-opus", 10L, 20L, 30L, 40L);
            SyncRequest req = new SyncRequest(VALID_DEVICE_ID, List.of(badDate));

            // when / then
            assertThatThrownBy(() -> service.saveRecords(jwt, req))
                .isInstanceOf(DateTimeParseException.class);
            then(tokenRepo).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("레코드저장_subject비숫자_NumberFormatException")
        void 레코드저장_subject비숫자_예외발생() {
            // given
            Jwt jwt = mock(Jwt.class);
            given(jwt.getSubject()).willReturn("not-a-number");
            TokenRecordRequest r = new TokenRecordRequest("2026-06-01", "claude-opus", 10L, 20L, 30L, 40L);
            SyncRequest req = new SyncRequest(VALID_DEVICE_ID, List.of(r));

            // when / then
            assertThatThrownBy(() -> service.saveRecords(jwt, req))
                .isInstanceOf(NumberFormatException.class);
            then(tokenRepo).shouldHaveNoInteractions();
        }
    }
}
