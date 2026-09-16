package com.tokenphage.api.feature.sync.service;

import com.tokenphage.api.domain.badge.BadgeCacheInvalidator;
import com.tokenphage.api.domain.user.service.UserService;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

/**
 * SyncOrchestrator.sync 의 조율 흐름(사용자 저장 → 레코드 저장 → 배지 캐시 무효화)과
 * 호출 순서, 빈 레코드 경계, 중간 실패 시 후속 호출 차단 및 예외 전파를 검증한다.
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
        @DisplayName("정상 동기화 시 saveUser → saveRecords → evict 순서로 호출된다")
        void 동기화_정상흐름_네단계순서대로호출() {
            // given
            SyncRequest req = requestWithOneRecord();

            // when
            syncOrchestrator.sync(jwt, req);

            // then
            InOrder order =
                inOrder(userService, tokenUsageRecordService, badgeCacheInvalidator);
            then(userService).should(order).saveUser(GITHUB_ID, USERNAME);
            then(tokenUsageRecordService).should(order).saveRecords(jwt, req);
            then(badgeCacheInvalidator).should(order).evict(USERNAME);
            order.verifyNoMoreInteractions();
        }

        @Test
        @DisplayName("records가 빈 리스트여도 예외 없이 전체 흐름이 동작한다")
        void 동기화_빈레코드_예외없이흐름정상() {
            // given
            SyncRequest req = new SyncRequest("디바이스UUID문자열", List.of());

            // when / then
            assertThatCode(() -> syncOrchestrator.sync(jwt, req)).doesNotThrowAnyException();
            InOrder order =
                inOrder(userService, tokenUsageRecordService, badgeCacheInvalidator);
            then(userService).should(order).saveUser(GITHUB_ID, USERNAME);
            then(tokenUsageRecordService).should(order).saveRecords(jwt, req);
            then(badgeCacheInvalidator).should(order).evict(USERNAME);
        }
    }

    @Nested
    @DisplayName("sync 실패 전파")
    class SyncFailure {

        @Test
        @DisplayName("saveRecords가 예외를 던지면 evict는 호출되지 않고 예외가 전파된다")
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
    }
}
