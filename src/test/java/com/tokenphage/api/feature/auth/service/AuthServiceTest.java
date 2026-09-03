package com.tokenphage.api.feature.auth.service;

import com.tokenphage.api.domain.user.service.UserService;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.auth.dto.request.ChallengeRequest;
import com.tokenphage.api.feature.auth.dto.request.VerifyRequest;
import com.tokenphage.api.feature.auth.dto.response.ChallengeResponse;
import com.tokenphage.api.feature.auth.dto.response.GistOwnerResponse;
import com.tokenphage.api.feature.auth.dto.response.GistResponse;
import com.tokenphage.api.feature.auth.dto.response.TokenResponse;
import com.tokenphage.api.feature.auth.exception.AuthErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

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

/**
 * AuthService 의 createChallenge "저장 동작"과 verify() 플로우를 검증한다.
 * (사용자명 정책 검증은 AuthServiceUsernameValidationTest 가 담당하므로 여기서는 다루지 않는다.)
 */
@DisplayName("AuthService - createChallenge 저장 / verify 플로우")
class AuthServiceTest {

    private static final int TTL_MINUTES = 10;

    private UserService userService;
    private GistVerificationService gistVerification;
    private JwtIssuer jwtIssuer;
    private StringRedisTemplate redis;
    private ValueOperations<String, String> valueOps;

    private AuthService authService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        // given: 모든 의존성을 mock 으로 구성하고 ValueOperations stub 을 연결한다
        userService = mock(UserService.class);
        gistVerification = mock(GistVerificationService.class);
        jwtIssuer = mock(JwtIssuer.class);
        redis = mock(StringRedisTemplate.class);
        valueOps = mock(ValueOperations.class);
        given(redis.opsForValue()).willReturn(valueOps);

        // 생성자 인자 순서 = 필드 선언 순서: UserService, GistVerificationService, JwtIssuer, StringRedisTemplate
        authService = new AuthService(userService, gistVerification, jwtIssuer, redis);
        // @Value 주입 필드 challengeTtlMinutes 를 리플렉션으로 주입
        ReflectionTestUtils.setField(authService, "challengeTtlMinutes", TTL_MINUTES);
    }

    @Nested
    @DisplayName("createChallenge")
    class CreateChallenge {

        @Test
        @DisplayName("정상 username 이면 challenge 를 TTL 과 함께 Redis 에 저장하고 tknphg_ 접두 토큰을 반환한다")
        void 챌린지생성_정상요청_토큰저장및반환() {
            // given
            ChallengeRequest req = new ChallengeRequest("octocat");

            // when
            ChallengeResponse response = authService.createChallenge(req);

            // then: 반환 challenge 는 tknphg_ 로 시작
            assertThat(response.challenge()).startsWith("tknphg_");
            assertThat(response.expiresAt()).isNotNull();
            // Redis set 이 키 prefix + TTL 과 함께 호출됐는지 확인
            then(valueOps).should().set(
                eq("auth:challenge:octocat"),
                eq(response.challenge()),
                eq(Duration.ofMinutes(TTL_MINUTES))
            );
        }

        @Test
        @DisplayName("Redis 저장 키의 username 부분이 소문자로 정규화된다 (OctoCat -> octocat)")
        void 챌린지생성_대문자포함username_키소문자화() {
            // given
            ChallengeRequest req = new ChallengeRequest("OctoCat");
            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            authService.createChallenge(req);

            // then: 캡처한 키가 소문자화된 형태인지 확인
            then(valueOps).should().set(keyCaptor.capture(), anyString(), any(Duration.class));
            assertThat(keyCaptor.getValue()).isEqualTo("auth:challenge:octocat");
        }

        @Test
        @DisplayName("유효한 challenge 가 이미 있으면 새로 발급하지 않고 동일 값을 재사용하며 TTL 만 갱신한다")
        void 챌린지생성_기존챌린지존재_동일값재사용() {
            // given: 동일 사용자의 기존 challenge 가 Redis 에 남아 있다 (검증 실패 후 재시도 상황)
            ChallengeRequest req = new ChallengeRequest("octocat");
            String existing = "tknphg_existing0challenge";
            given(valueOps.get("auth:challenge:octocat")).willReturn(existing);

            // when
            ChallengeResponse response = authService.createChallenge(req);

            // then: 반환 challenge 가 기존 값과 동일하고, set 은 같은 값 + 갱신된 TTL 로 호출된다
            assertThat(response.challenge()).isEqualTo(existing);
            then(valueOps).should().set(
                eq("auth:challenge:octocat"),
                eq(existing),
                eq(Duration.ofMinutes(TTL_MINUTES))
            );
        }
    }

    @Nested
    @DisplayName("verify")
    class Verify {

        @Test
        @DisplayName("Redis 에 challenge 가 없으면 CHALLENGE_EXPIRED 로 거부한다")
        void 검증_챌린지없음_CHALLENGE_EXPIRED() {
            // given: get 이 null 을 반환하도록 stub
            VerifyRequest req = new VerifyRequest("octocat", "gist-123");
            given(valueOps.get("auth:challenge:octocat")).willReturn(null);

            // when & then
            assertThatThrownBy(() -> authService.verify(req))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.CHALLENGE_EXPIRED);

            // gist 검증/저장/발급 단계는 진입하지 않아야 한다
            then(gistVerification).shouldHaveNoInteractions();
            then(jwtIssuer).shouldHaveNoInteractions();
            then(userService).shouldHaveNoInteractions();
            then(redis).should(never()).delete(anyString());
        }

        @ParameterizedTest(name = "무효 입력: username=\"{0}\", gistId=\"{1}\"")
        @CsvSource(value = {
            "NULL,    gist-123",   // username null
            "'',      gist-123",   // username blank
            "'   ',   gist-123",   // username whitespace
            "octocat, NULL",       // gistId null
            "octocat, ''",         // gistId blank
            "octocat, '   '"       // gistId whitespace
        }, nullValues = "NULL")
        @DisplayName("username/gistId 가 null 또는 blank 면 INVALID_USERNAME 으로 거부한다")
        void 검증_필수입력누락_INVALID_USERNAME(String username, String gistId) {
            // given: 입력 검증은 진입 즉시 throw 되므로 Redis 조회 이전에 차단된다
            VerifyRequest req = new VerifyRequest(username, gistId);

            // when & then
            assertThatThrownBy(() -> authService.verify(req))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.INVALID_USERNAME);

            // 어떤 의존성도 사용되지 않아야 한다 (Redis get 도 호출 안 됨)
            then(gistVerification).shouldHaveNoInteractions();
            then(jwtIssuer).shouldHaveNoInteractions();
            then(userService).shouldHaveNoInteractions();
            then(valueOps).should(never()).get(anyString());
        }

        @Test
        @DisplayName("정상 검증 시 challenge 삭제, 사용자 저장, JWT 발급 후 발급 토큰을 그대로 반환한다")
        void 검증_정상플로우_토큰발급및반환() {
            // given
            VerifyRequest req = new VerifyRequest("octocat", "gist-123");
            String challenge = "tknphg_deadbeefcafe1234";
            String issuedToken = "issued.jwt.token";
            GistResponse gist = new GistResponse(
                "gist-123",
                Boolean.TRUE,
                new GistOwnerResponse(-42L, "octocat"),
                null
            );
            given(valueOps.get("auth:challenge:octocat")).willReturn(challenge);
            given(gistVerification.verify(req, challenge)).willReturn(gist);
            given(jwtIssuer.issue(-42L, "octocat")).willReturn(issuedToken);

            // when
            TokenResponse response = authService.verify(req);

            // then: 반환 토큰이 issue 반환값과 일치
            assertThat(response.token()).isEqualTo(issuedToken);
            assertThat(response.githubId()).isEqualTo(-42L);
            assertThat(response.username()).isEqualTo("octocat");
            // Redis 챌린지 삭제 + 사용자 저장 + JWT 발급 호출 검증
            then(redis).should().delete("auth:challenge:octocat");
            then(userService).should().saveUser(-42L, "octocat");
            then(jwtIssuer).should().issue(-42L, "octocat");
        }

        @Test
        @DisplayName("Gist 검증이 실패(AppException)하면 챌린지 삭제/사용자 저장/JWT 발급으로 진행하지 않는다")
        void 검증_Gist검증실패_후속단계미수행() {
            // given: gistVerification 이 OWNER_MISMATCH 를 던지도록 stub
            VerifyRequest req = new VerifyRequest("octocat", "gist-123");
            String challenge = "tknphg_deadbeefcafe1234";
            given(valueOps.get("auth:challenge:octocat")).willReturn(challenge);
            given(gistVerification.verify(req, challenge))
                .willThrow(new AppException(AuthErrorCode.OWNER_MISMATCH));

            // when & then
            assertThatThrownBy(() -> authService.verify(req))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(AuthErrorCode.OWNER_MISMATCH);

            // 검증 실패 이후 단계는 수행되지 않아야 한다
            then(redis).should(never()).delete(anyString());
            then(userService).should(never()).saveUser(anyLong(), anyString());
            then(jwtIssuer).should(never()).issue(anyLong(), anyString());
        }
    }
}
