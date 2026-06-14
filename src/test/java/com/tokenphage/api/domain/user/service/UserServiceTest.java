package com.tokenphage.api.domain.user.service;

import com.tokenphage.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

/**
 * UserService.saveUser 동작을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("사용자저장_정상호출_upsert위임")
    void 사용자저장_정상호출_upsert위임() {
        // given
        long githubId = -1001L;
        String username = "octocat";

        // when
        userService.saveUser(githubId, username);

        // then
        verify(userRepo).upsert(githubId, username);
    }
}
