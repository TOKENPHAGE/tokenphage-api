package com.tokenphage.api.feature.reset.service;

import com.tokenphage.api.domain.BadgeCacheInvalidator;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.reset.exception.ResetErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ResetOrchestrator.reset(Jwt) 검증: 쿨다운 게이트 → DB 삭제 → 배지 캐시 무효화 흐름과 실패 격리 동작.
 */
@ExtendWith(MockitoExtension.class)
class ResetOrchestratorTest {

    private static final String GITHUB_ID = "-12345";
    private static final String USERNAME = "octocat";
    private static final String COOLDOWN_KEY = "cooldown:reset:-12345";

    @Mock
    private ResetService resetService;

    @Mock
    private BadgeCacheInvalidator badgeCacheInvalidator;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Captor
    private ArgumentCaptor<String> keyCaptor;

    private Jwt jwt;
    private ResetOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        // 생성자 인자 순서 = 필드 선언 순서: ResetService, BadgeCacheInvalidator, StringRedisTemplate
        orchestrator = new ResetOrchestrator(resetService, badgeCacheInvalidator, redis);

        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(GITHUB_ID);
        when(jwt.getClaimAsString("username")).thenReturn(USERNAME);
    }

    @Nested
    @DisplayName("reset - 정상 흐름")
    class ResetSuccess {

        @Test
        @DisplayName("전체초기화_쿨다운선점성공_삭제와캐시무효화수행")
        void 전체초기화_쿨다운선점성공_삭제와캐시무효화수행() {
            // given
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq(COOLDOWN_KEY), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);

            // when
            orchestrator.reset(jwt);

            // then
            verify(resetService).resetUsage(-12345L);
            verify(badgeCacheInvalidator).evict(USERNAME);
            // 정상 흐름에서는 쿨다운 해제(delete)를 호출하지 않는다.
            verify(redis, never()).delete(anyString());
        }

        @Test
        @DisplayName("전체초기화_쿨다운키검증_githubId기반키사용")
        void 전체초기화_쿨다운키검증_githubId기반키사용() {
            // given
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(keyCaptor.capture(), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);

            // when
            orchestrator.reset(jwt);

            // then
            assertThat(keyCaptor.getValue()).isEqualTo(COOLDOWN_KEY);
        }
    }

    @Nested
    @DisplayName("reset - 쿨다운 활성화")
    class ResetCooldown {

        @Test
        @DisplayName("전체초기화_쿨다운미선점_RESET_COOLDOWN발생")
        void 전체초기화_쿨다운미선점_RESET_COOLDOWN발생() {
            // given
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq(COOLDOWN_KEY), anyString(), any(Duration.class))).thenReturn(Boolean.FALSE);

            // when & then
            assertThatThrownBy(() -> orchestrator.reset(jwt))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ResetErrorCode.RESET_COOLDOWN);

            // 쿨다운으로 거부되면 DB 삭제와 캐시 무효화는 일어나지 않는다.
            verifyNoInteractions(resetService);
            verifyNoInteractions(badgeCacheInvalidator);
        }

        @Test
        @DisplayName("전체초기화_setIfAbsent반환null_쿨다운거부")
        void 전체초기화_setIfAbsent반환null_쿨다운거부() {
            // given
            // setIfAbsent가 null을 반환해도 Boolean.TRUE.equals(null)==false 이므로 거부되어야 한다(경계 케이스).
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq(COOLDOWN_KEY), anyString(), any(Duration.class))).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> orchestrator.reset(jwt))
                .isInstanceOf(AppException.class);

            verify(resetService, never()).resetUsage(any());
        }
    }

    @Nested
    @DisplayName("reset - DB 삭제 실패")
    class ResetDbFailure {

        @Test
        @DisplayName("전체초기화_DB삭제실패_쿨다운해제와예외전파")
        void 전체초기화_DB삭제실패_쿨다운해제와예외전파() {
            // given
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq(COOLDOWN_KEY), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);
            RuntimeException dbError = new RuntimeException("DB write failed");
            doThrow(dbError).when(resetService).resetUsage(-12345L);

            // when & then
            assertThatThrownBy(() -> orchestrator.reset(jwt))
                .isSameAs(dbError);

            // 서버 오류로 24시간 잠기지 않도록 쿨다운 키를 해제하고, 캐시 무효화는 수행하지 않는다.
            verify(redis).delete(COOLDOWN_KEY);
            verifyNoInteractions(badgeCacheInvalidator);
        }
    }

    @Nested
    @DisplayName("reset - 캐시 무효화 실패는 무시")
    class ResetCacheEvictionFailure {

        @Test
        @DisplayName("전체초기화_캐시무효화실패_예외없이완료")
        void 전체초기화_캐시무효화실패_예외없이완료() {
            // given
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.setIfAbsent(eq(COOLDOWN_KEY), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);
            doThrow(new RuntimeException("cache eviction failed")).when(badgeCacheInvalidator).evict(USERNAME);

            // when & then
            // DB 삭제는 이미 커밋됐고 캐시 무효화는 best-effort이므로 reset 자체는 성공해야 한다.
            assertThatCode(() -> orchestrator.reset(jwt)).doesNotThrowAnyException();

            // 삭제는 정상 수행, 캐시 실패로 쿨다운을 해제하지 않는다(delete 미호출).
            verify(resetService).resetUsage(-12345L);
            verify(redis, never()).delete(anyString());
        }
    }
}
