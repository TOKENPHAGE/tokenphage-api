package com.tokenphage.api.feature.reset.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** ResetService.resetUsage 의 토큰 삭제 동작(정상/경계/전파)을 검증한다. */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResetService 단위 테스트")
class ResetServiceTest {

    @Mock
    private DailyTokenUsageRepository tokenRepo;

    @InjectMocks
    private ResetService resetService;

    private static final Long GITHUB_ID = -12345L;

    @Nested
    @DisplayName("resetUsage 메서드")
    class ResetUsage {

        @Test
        @DisplayName("정상 초기화: 사용자의 토큰 사용량을 삭제한다")
        void 초기화_정상호출_토큰삭제() {
            // given
            when(tokenRepo.deleteAllByGithubId(GITHUB_ID)).thenReturn(3);

            // when
            resetService.resetUsage(GITHUB_ID);

            // then
            verify(tokenRepo).deleteAllByGithubId(GITHUB_ID);
        }

        @Test
        @DisplayName("경계값: 삭제된 행이 0이어도 예외 없이 정상 동작한다")
        void 초기화_삭제행0_예외없음() {
            // given
            when(tokenRepo.deleteAllByGithubId(GITHUB_ID)).thenReturn(0);

            // when / then
            assertThatCode(() -> resetService.resetUsage(GITHUB_ID)).doesNotThrowAnyException();
            verify(tokenRepo).deleteAllByGithubId(GITHUB_ID);
        }

        @Test
        @DisplayName("실패 전파: 토큰 삭제가 예외를 던지면 그대로 전파된다")
        void 초기화_삭제예외_전파() {
            // given
            when(tokenRepo.deleteAllByGithubId(GITHUB_ID))
                    .thenThrow(new RuntimeException("DB delete failed"));

            // when / then
            assertThatThrownBy(() -> resetService.resetUsage(GITHUB_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("DB delete failed");
            verify(tokenRepo).deleteAllByGithubId(GITHUB_ID);
        }
    }
}
