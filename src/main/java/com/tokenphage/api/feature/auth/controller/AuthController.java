package com.tokenphage.api.feature.auth.controller;

import com.tokenphage.api.feature.auth.dto.request.ChallengeRequest;
import com.tokenphage.api.feature.auth.dto.request.VerifyRequest;
import com.tokenphage.api.feature.auth.dto.response.ChallengeResponse;
import com.tokenphage.api.feature.auth.dto.response.TokenResponse;
import com.tokenphage.api.feature.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * GitHub 사용자 소유권 검증을 위한 일회용 챌린지를 발급한다.
     *
     * @param req 챌린지를 요청할 사용자 정보
     * @return 챌린지 문자열과 만료 시각
     * @Since 2026-05-24
     */
    @PostMapping("/auth/challenge")
    public ResponseEntity<ChallengeResponse> challenge(@RequestBody ChallengeRequest req) {
        return ResponseEntity.ok(authService.createChallenge(req));
    }

    /**
     * Gist 소유권 검증 후 JWT를 발급한다.
     *
     * @param req 검증에 필요한 사용자명과 Gist ID
     * @return GitHub 사용자 정보와 JWT 토큰
     * @Since 2026-05-24
     */
    @PostMapping("/auth/verify")
    public ResponseEntity<TokenResponse> verify(@RequestBody VerifyRequest req) {
        return ResponseEntity.ok(authService.verify(req));
    }
}
