package com.tokenphage.api.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String jwtSecret;

    /**
     * JWT 서명 검증에 사용할 비밀 키를 주입받아 SecurityConfig를 생성한다.
     *
     * @param jwtSecret JWT HMAC-SHA 서명 비밀 키 (badge.jwt-secret 설정값)
     * @Since 2026-06-13
     */
    public SecurityConfig(@Value("${badge.jwt-secret}") String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    /**
     * Spring Security 필터 체인을 구성하고 등록한다.
     * <p>
     * 공개 경로(/badge/**, /auth/challenge, /auth/verify)는 인증 없이 접근 가능하며,
     * /api/sync는 JWT 인증이 필요하다. CSRF는 stateless JWT 구조이므로 비활성화한다.
     * CORS는 배지 SVG 조회(/badge/**)에 한해 허용한다 — 미리보기 HTML(file:// → origin "null")이나
     * 외부 페이지에서 fetch로 SVG를 가져올 수 있도록 한다. (CLI가 호출하는 /api/sync 등 인증 경로는 미적용)
     *
     * @param http Spring Security HttpSecurity 빌더
     * @return 구성된 SecurityFilterChain
     * @throws Exception HttpSecurity 빌드 실패 시
     * @Since 2026-06-13
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/badge/**", "/auth/challenge", "/auth/verify").permitAll()
                        .requestMatchers("/api/sync").authenticated()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(rs -> rs.jwt(jwt -> jwt.decoder(jwtDecoder())));
        return http.build();
    }

    /**
     * 배지 SVG 조회 경로(/badge/**)에만 CORS를 허용하는 설정을 등록한다.
     * <p>
     * 배지 미리보기 HTML(file:// → origin "null")이나 외부 페이지가 fetch로 SVG를 가져올 수 있어야 한다.
     * 공개 읽기 전용이고 credentials를 쓰지 않으므로 모든 origin("*")을 GET/OPTIONS에 한해 허용한다.
     * /api/sync 등 인증 경로는 등록하지 않아 CORS가 적용되지 않는다(브라우저 비대상).
     *
     * @return /badge/** 에만 적용되는 CORS 설정 소스
     * @Since 2026-06-16
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("*"));
        config.setAllowedMethods(List.of("GET", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/badge/**", config);
        return source;
    }

    /**
     * jjwt 기반의 커스텀 JwtDecoder를 Bean으로 등록한다.
     * <p>
     * Spring Security 기본 JwtDecoder(nimbus) 대신 jjwt로 직접 파싱한다.
     * 만료 시각(exp)이 없는 토큰은 Instant.MAX로 폴백해 무만료 토큰을 허용한다.
     * (무만료는 의도된 설계 — JWT_SECRET 교체로 폐기 대응)
     *
     * @return HMAC-SHA 서명 검증을 수행하는 JwtDecoder
     * @Since 2026-06-13
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        JwtParser parser = Jwts.parser().verifyWith(key).build();
        return token -> {
            try {
                Jws<Claims> jws = parser.parseSignedClaims(token);
                Claims claims = jws.getPayload();

                Map<String, Object> headers = new HashMap<>();
                headers.put("alg", jws.getHeader().getAlgorithm());
                if (jws.getHeader().getType() != null) {
                    headers.put("typ", jws.getHeader().getType());
                }

                Map<String, Object> claimsMap = new HashMap<>(claims);
                Instant issuedAt = toInstant(claims.getIssuedAt());
                // exp 클레임이 없으면 Instant.MAX로 폴백 (무만료 토큰 허용)
                Instant expiresAt = Objects.requireNonNullElse(toInstant(claims.getExpiration()), Instant.MAX);

                return new Jwt(token, issuedAt, expiresAt, headers, claimsMap);
            } catch (io.jsonwebtoken.JwtException e) {
                throw new BadJwtException("JWT validation failed: " + e.getMessage(), e);
            }
        };
    }

    // java.util.Date → Instant 변환, null 입력 시 null 반환
    private static Instant toInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }
}
