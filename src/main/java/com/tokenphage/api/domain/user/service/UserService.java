package com.tokenphage.api.domain.user.service;

import com.tokenphage.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepo;

    /**
     * GitHub 사용자 정보를 저장한다.
     * <p>
     * DB 레벨의 ON CONFLICT로 원자적으로 처리하므로 race condition이 없다.
     *
     * @param githubId GitHub 사용자 고유 ID (null 불허)
     * @param username GitHub 사용자명 (null 불허)
     * @Since 2026-05-24
     */
    @Transactional
    public void saveUser(Long githubId, String username) {
        log.info("Saving user: githubId={}, username={}", githubId, username);
        userRepo.upsert(githubId, username);
    }
}
