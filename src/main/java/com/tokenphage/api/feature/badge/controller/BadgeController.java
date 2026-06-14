package com.tokenphage.api.feature.badge.controller;

import com.tokenphage.api.feature.badge.service.BadgeRenderService;
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
     *
     * @param username 배지를 조회할 GitHub 사용자명
     * @param theme    배지 스킨 종류 (예: "gpu", 기본값: "gpu")
     * @param mode     색상 모드 ("light" 또는 "dark", 기본값: "light")
     * @return SVG 형식의 배지 이미지
     * @Since 2026-05-27
     */
    @GetMapping("/badge/{username}")
    public ResponseEntity<String> badge(@PathVariable String username,
                                        @RequestParam(defaultValue = "gpu") String theme,
                                        @RequestParam(defaultValue = "light") String mode) {
        String svg = badgeRenderService.getSvg(username, theme, mode);
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .header("Cache-Control", "no-cache, max-age=0")
                .body(svg);
    }
}
