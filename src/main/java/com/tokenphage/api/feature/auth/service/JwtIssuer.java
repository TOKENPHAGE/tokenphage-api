package com.tokenphage.api.feature.auth.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtIssuer {

    @Value("${badge.jwt-secret}")
    private String jwtSecret;

    /**
     * GitHub 사용자 정보를 담은 JWT를 발급한다.
     * <p>
     * sub=githubId, username 클레임을 포함한다.
     * 만료(exp)는 의도적으로 설정하지 않아 토큰이 영구 유효하다 — tokenphage는 저위험 배지 도구이고
     * JWT는 githubId 식별용일 뿐이라, 만료로 인한 재인증(Gist 소유권 증명 재수행) UX 비용을 피하기 위함이다.
     * 토큰 유출 대응은 만료가 아니라 badge.jwt-secret 회전으로 한다.
     *
     * @param githubId JWT subject에 설정할 GitHub 사용자 ID (null 불허)
     * @param username JWT claim에 설정할 GitHub 사용자명 (null 불허)
     * @return 서명된 JWT 문자열
     * @Since 2026-05-24
     */
    public String issue(Long githubId, String username) {
        log.debug("Issuing JWT for githubId={}, username={}", githubId, username);
        var key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(String.valueOf(githubId))
                .claim("username", username)
                .issuedAt(new Date())
                // .expiration(new Date(System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000))
                .signWith(key)
                .compact();
    }
}
