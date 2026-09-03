package com.tokenphage.api.feature.auth.service;

import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.auth.dto.request.ChallengeRequest;
import com.tokenphage.api.feature.auth.exception.AuthErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * GitHub 사용자명 정책 검증을, 공개 진입점 createChallenge 를 통해 확인하는 테스트.
 *
 * 정책: 영문/숫자/하이픈만, 하이픈 시작·끝·연속 불가, 1~39자.
 * - 거부 케이스: 검증이 createChallenge 첫 줄에서 throw → 의존성 mock 불필요
 * - 통과 케이스: 검증 후 redis 저장까지 진행 → redis 만 mock
 */
@DisplayName("AuthService - GitHub 사용자명 정책")
class AuthServiceUsernameValidationTest {

    @ParameterizedTest(name = "유효: \"{0}\"")
    @ValueSource(strings = {
        "octocat",                                  // 일반
        "a",                                        // 최소 1자
        "Lee9908",                                  // 대문자 + 숫자
        "123",                                      // 숫자만
        "user-name",                                // 하이픈 1개
        "a-b-c-d",                                  // 하이픈 여러 개(연속 아님)
        "abcdefghijklmnopqrstuvwxyz0123456789abc"   // 39자(경계값)
    })
    void 유효한_사용자명은_통과한다(String username) {
        AuthService authService = new AuthService(null, null, null, redisMock());

        assertThatCode(() -> authService.createChallenge(new ChallengeRequest(username)))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "무효: \"{0}\"")
    @NullSource
    @ValueSource(strings = {
        "-octocat",                                  // 하이픈으로 시작
        "octocat-",                                  // 하이픈으로 끝
        "oct--ocat",                                 // 하이픈 연속
        "-",                                         // 하이픈만
        "hong_gildong",                              // 언더스코어 불허
        "user name",                                 // 공백 불허
        "user@name",                                 // 특수문자 불허
        "user.name",                                 // 점 불허
        "김철수",                                     // 비ASCII(한글) 불허
        "abcdefghijklmnopqrstuvwxyz0123456789abcd",  // 40자(길이 초과)
        "", "   ", "\t"                              // blank
    })
    void 정책_위반_사용자명은_INVALID_USERNAME_으로_거부한다(String username) {
        // 거부는 createChallenge 진입 즉시 발생하므로 의존성은 사용되지 않는다
        AuthService authService = new AuthService(null, null, null, null);

        assertThatThrownBy(() -> authService.createChallenge(new ChallengeRequest(username)))
            .isInstanceOf(AppException.class)
            .extracting(e -> ((AppException) e).getErrorCode())
            .isEqualTo(AuthErrorCode.INVALID_USERNAME);
    }

    /** 통과 케이스에서 challenge 저장 단계만 통과시키기 위한 최소 redis mock. */
    @SuppressWarnings("unchecked")
    private StringRedisTemplate redisMock() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        given(redis.opsForValue()).willReturn(mock(ValueOperations.class));
        return redis;
    }
}
