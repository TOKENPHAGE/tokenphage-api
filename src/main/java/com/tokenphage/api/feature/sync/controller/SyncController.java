package com.tokenphage.api.feature.sync.controller;

import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.service.SyncOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class SyncController {

    private final SyncOrchestrator syncOrchestrator;

    /**
     * 클라이언트 토큰 사용 기록을 서버에 동기화한다.
     * <p>
     * 요청 본문은 Bean Validation(@Valid)으로 검증되며, 위반 시 400 Bad Request로 응답한다.
     *
     * @param jwt JWT 인증 토큰 (Spring Security에서 주입)
     * @param req 동기화할 토큰 레코드 목록 (검증 대상)
     * @return 200 OK
     * @Since 2026-05-24
     */
    @PostMapping("/api/sync")
    public ResponseEntity<Void> sync(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody SyncRequest req) {
        syncOrchestrator.sync(jwt, req);
        return ResponseEntity.ok().build();
    }
}
