package com.tokenphage.api.feature.sync.service;

import com.tokenphage.api.domain.badge.BadgeCacheInvalidator;
import com.tokenphage.api.domain.user.service.UserService;
import com.tokenphage.api.feature.sync.dto.request.RuntimeRecordRequest;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import com.tokenphage.api.feature.sync.dto.response.RuntimeSyncResultResponse;
import com.tokenphage.api.feature.sync.dto.response.SyncResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

/**
 * SyncOrchestrator.sync 의 조율 흐름(사용자 저장 → 레코드 저장 → 실행시간 저장 → 배지 캐시 무효화)과
 * 호출 순서, 빈 레코드 경계, 중간 실패 시 후속 호출 차단·캐시 무효화 보장 및 예외 전파를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SyncOrchestrator 단위 테스트")
class SyncOrchestratorTest {

    private static final long GITHUB_ID = -777L;
    private static final String USERNAME = "octocat";

    @Mock
    private UserService userService;

    @Mock
    private TokenUsageRecordService tokenUsageRecordService;

    @Mock
    private BadgeCacheInvalidator badgeCacheInvalidator;

    @Mock
    private RuntimeRecordService runtimeRecordService;

    @Mock
    private Jwt jwt;

    @InjectMocks
    private SyncOrchestrator syncOrchestrator;

    @BeforeEach
    void setUp() {
        // lenient: 일부 테스트(흐름 미진입 검증 등)에서 사용되지 않을 수 있어 strict stub 경고 회피
        lenient().when(jwt.getSubject()).thenReturn(String.valueOf(GITHUB_ID));
        lenient().when(jwt.getClaimAsString("username")).thenReturn(USERNAME);
    }

    private SyncRequest requestWithOneRecord() {
        // 토큰 레코드 1개를 가진 동기화 요청 (records().size() 로깅에 사용)
        TokenRecordRequest record =
            new TokenRecordRequest("2026-05-31", "claude-opus-4", 100L, 200L, 50L, 30L);
        return new SyncRequest("디바이스UUID문자열", List.of(record));
    }

    @Nested
    @DisplayName("sync 정상 흐름")
    class Sync {

        @Test
        @DisplayName("정상 동기화 시 saveUser → saveRecords → 실행시간 저장 → evict 순서로 호출된다")
        void 동기화_정상흐름_네단계순서대로호출() {
            // given
            SyncRequest req = requestWithOneRecord();
            given(runtimeRecordService.saveRecords(jwt, req)).willReturn(new RuntimeSyncResultResponse(0, 0));

            // when
            syncOrchestrator.sync(jwt, req);

            // then
            InOrder order =
                inOrder(userService, tokenUsageRecordService, badgeCacheInvalidator, runtimeRecordService);
            then(userService).should(order).saveUser(GITHUB_ID, USERNAME);
            then(tokenUsageRecordService).should(order).saveRecords(jwt, req);
            then(runtimeRecordService).should(order).saveRecords(jwt, req);
            then(badgeCacheInvalidator).should(order).evict(USERNAME);
            order.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("records가 빈 리스트여도 예외 없이 전체 흐름이 동작한다")
        void 동기화_빈레코드_예외없이흐름정상() {
            // given
            SyncRequest req = new SyncRequest("디바이스UUID문자열", List.of());
            given(runtimeRecordService.saveRecords(jwt, req)).willReturn(new RuntimeSyncResultResponse(0, 0));

            // when / then
            assertThatCode(() -> syncOrchestrator.sync(jwt, req)).doesNotThrowAnyException();
            InOrder order =
                inOrder(userService, tokenUsageRecordService, badgeCacheInvalidator, runtimeRecordService);
            then(userService).should(order).saveUser(GITHUB_ID, USERNAME);
            then(tokenUsageRecordService).should(order).saveRecords(jwt, req);
            then(runtimeRecordService).should(order).saveRecords(jwt, req);
            then(badgeCacheInvalidator).should(order).evict(USERNAME);
        }
    }

    @Nested
    @DisplayName("sync 실패 전파")
    class SyncFailure {

        @Test
        @DisplayName("토큰 saveRecords가 예외를 던지면 evict는 호출되지 않고 예외가 전파된다")
        void 동기화_레코드저장실패_후속호출없이예외전파() {
            // given
            SyncRequest req = requestWithOneRecord();
            willThrow(new RuntimeException("DB write failed"))
                .given(tokenUsageRecordService).saveRecords(jwt, req);

            // when / then
            assertThatThrownBy(() -> syncOrchestrator.sync(jwt, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB write failed");

            // then: saveUser는 실패 이전이므로 호출됨, 이후 단계는 호출되지 않아야 함
            then(userService).should().saveUser(GITHUB_ID, USERNAME);
            then(badgeCacheInvalidator).should(never()).evict(USERNAME);
        }

        @Test
        @DisplayName("실행시간 저장이 예외를 던져도 그 뒤에 캐시를 비우고 원래 예외를 전파한다")
        void 동기화_실행시간저장실패_캐시비운뒤원래예외전파() {
            // given
            // 토큰은 이미 저장됐다. 캐시를 비우지 않으면 토큰 배지가 최대 60분 이전 값으로 남는다.
            SyncRequest req = requestWithOneRecord();
            RuntimeException runtimeFailure = new RuntimeException("runtime write failed");
            given(runtimeRecordService.saveRecords(jwt, req)).willThrow(runtimeFailure);

            // when
            Throwable thrown = catchThrowable(() -> syncOrchestrator.sync(jwt, req));

            // then
            assertThat(thrown).isSameAs(runtimeFailure);
            InOrder order = inOrder(runtimeRecordService, badgeCacheInvalidator);
            then(runtimeRecordService).should(order).saveRecords(jwt, req);
            then(badgeCacheInvalidator).should(order).evict(USERNAME);
        }

        @Test
        @DisplayName("실행시간 저장과 캐시 비우기가 모두 실패하면 캐시 예외가 전파된다")
        void 동기화_실행시간저장과캐시비우기모두실패_캐시예외전파() {
            // given
            // 저장 예외는 error 로그에 남는다.
            SyncRequest req = requestWithOneRecord();
            RuntimeException runtimeFailure = new RuntimeException("runtime write failed");
            RuntimeException evictFailure = new RuntimeException("redis down");
            given(runtimeRecordService.saveRecords(jwt, req)).willThrow(runtimeFailure);
            willThrow(evictFailure).given(badgeCacheInvalidator).evict(USERNAME);

            // when
            Throwable thrown = catchThrowable(() -> syncOrchestrator.sync(jwt, req));

            // then
            assertThat(thrown).isSameAs(evictFailure);
            then(badgeCacheInvalidator).should().evict(USERNAME);
        }

        @Test
        @DisplayName("캐시 비우기가 예외를 던지면 실행시간까지 저장한 뒤 그 예외를 전파한다")
        void 동기화_캐시비우기실패_실행시간저장뒤예외전파() {
            // given
            // Redis 장애. 저장은 upsert라 CLI가 같은 요청을 다시 보내도 중복되지 않는다.
            SyncRequest req = requestWithOneRecord();
            RuntimeException evictFailure = new RuntimeException("redis down");
            given(runtimeRecordService.saveRecords(jwt, req)).willReturn(new RuntimeSyncResultResponse(0, 0));
            willThrow(evictFailure).given(badgeCacheInvalidator).evict(USERNAME);

            // when
            Throwable thrown = catchThrowable(() -> syncOrchestrator.sync(jwt, req));

            // then
            assertThat(thrown).isSameAs(evictFailure);
            then(runtimeRecordService).should().saveRecords(jwt, req);
            then(badgeCacheInvalidator).should().evict(USERNAME);
        }
    }

    @Nested
    @DisplayName("sync 실행시간 분기")
    class SyncRuntimes {

        private SyncRequest requestWithRuntimes(List<RuntimeRecordRequest> runtimes) {
            TokenRecordRequest record =
                new TokenRecordRequest("2026-10-09", "claude-opus-4-1", 100L, 200L, 50L, 30L);
            return new SyncRequest("디바이스UUID문자열", List.of(record), runtimes);
        }

        @Test
        @DisplayName("runtimes가 있으면 토큰 저장 뒤 실행시간을 저장하고, 캐시를 비운 뒤 결과를 돌려준다")
        void 동기화_runtimes있음_토큰다음실행시간저장하고결과반환() {
            // given
            SyncRequest req = requestWithRuntimes(
                List.of(new RuntimeRecordRequest("2026-10-09", "claude", "claude-opus-4-1", 7_200_000L)));
            RuntimeSyncResultResponse saved = new RuntimeSyncResultResponse(1, 0);
            given(runtimeRecordService.saveRecords(jwt, req)).willReturn(saved);

            // when
            SyncResponse result = syncOrchestrator.sync(jwt, req);

            // then
            assertThat(result).isEqualTo(new SyncResponse(saved));
            InOrder order = inOrder(userService, tokenUsageRecordService, badgeCacheInvalidator, runtimeRecordService);
            then(userService).should(order).saveUser(GITHUB_ID, USERNAME);
            then(tokenUsageRecordService).should(order).saveRecords(jwt, req);
            then(runtimeRecordService).should(order).saveRecords(jwt, req);
            then(badgeCacheInvalidator).should(order).evict(USERNAME);
        }

        @Test
        @DisplayName("runtimes 칸이 없으면(2026.8.1까지) 빈 목록으로 실행시간 서비스를 호출하고 0건 결과를 돌려준다")
        void 동기화_runtimes칸없음_빈목록으로실행시간서비스호출하고0건결과() {
            // given
            SyncRequest req = requestWithOneRecord();
            RuntimeSyncResultResponse none = new RuntimeSyncResultResponse(0, 0);
            given(runtimeRecordService.saveRecords(jwt, req)).willReturn(none);

            // when
            SyncResponse result = syncOrchestrator.sync(jwt, req);

            // then
            assertThat(req.runtimes()).isEmpty();
            assertThat(result).isEqualTo(new SyncResponse(none));
            then(runtimeRecordService).should().saveRecords(jwt, req);
        }

        @Test
        @DisplayName("토큰 저장이 예외를 던지면 실행시간 서비스를 호출하지 않는다")
        void 동기화_토큰저장실패_실행시간서비스미호출() {
            // given
            SyncRequest req = requestWithRuntimes(
                List.of(new RuntimeRecordRequest("2026-10-09", "claude", "claude-opus-4-1", 7_200_000L)));
            willThrow(new RuntimeException("DB write failed"))
                .given(tokenUsageRecordService).saveRecords(jwt, req);

            // when / then
            assertThatThrownBy(() -> syncOrchestrator.sync(jwt, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB write failed");
            then(runtimeRecordService).shouldHaveNoInteractions();
        }
    }
}
