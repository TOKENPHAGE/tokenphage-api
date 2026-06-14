package com.tokenphage.api.feature.reset.controller;

import com.tokenphage.api.feature.reset.service.ResetOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class ResetController {

    private final ResetOrchestrator resetOrchestrator;

    /**
     * 인증된 사용자 본인의 토큰 사용량을 전체 초기화한다.
     * <p>
     * 요청 본문이 없다 — githubId 등 대상 식별 파라미터를 받지 않아 IDOR이 구조적으로 불가능하다.
     * 대상 사용자는 Spring Security가 주입한 JWT 주체에서만 파생한다.
     *
     * @param jwt JWT 인증 토큰 (Spring Security에서 주입)
     * @return 200 OK
     * @Since 2026-06-06
     */
    @PostMapping("/api/reset")
    public ResponseEntity<Void> reset(@AuthenticationPrincipal Jwt jwt) {
        resetOrchestrator.reset(jwt);
        return ResponseEntity.ok().build();
    }
}
