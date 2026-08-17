package com.tokenphage.api.feature.badge.controller;

import com.tokenphage.api.audit.AuditOutcome;
import com.tokenphage.api.feature.badge.dto.response.BadgeSvgResponse;
import com.tokenphage.api.feature.badge.service.BadgeRenderService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeRenderService badgeRenderService;

    /**
     * 사용자 배지 SVG 이미지를 반환한다.
     * <p>
     * 배지 주인에게 자격이 없으면 잠금 안내 SVG를 200으로 반환한다.
     * 4xx는 깨진 이미지로 보인다. 대신 결과 값을 요청 속성에 넣어 접근 기록에 남긴다.
     *
     * @param request  결과 값을 실어 보낼 현재 요청
     * @param username 배지를 조회할 GitHub 사용자명
     * @param theme    배지 스킨 종류 (예: "gpu", 기본값: "gpu")
     * @param mode     색상 모드 ("light" 또는 "dark", 기본값: "light")
     * @return SVG 형식의 배지 이미지
     * @Since 2026-05-27
     */
    @GetMapping("/badge/{username}")
    public ResponseEntity<String> badge(HttpServletRequest request,
                                        @PathVariable String username,
                                        @RequestParam(defaultValue = "gpu") String theme,
                                        @RequestParam(defaultValue = "light") String mode) {

        BadgeSvgResponse result = badgeRenderService.getSvg(username, theme, mode);

        if (!result.granted()) {
            request.setAttribute(AuditOutcome.ATTRIBUTE_KEY, AuditOutcome.BADGE_GRANT_DENIED);
        }

        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .header("Cache-Control", "no-cache, max-age=0")
                .body(result.svg());
    }
}
