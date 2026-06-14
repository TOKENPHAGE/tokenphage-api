package com.tokenphage.api.feature.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtIssuer.issue() 가 HS256 서명 JWT를 올바르게 발급하는지 검증한다.
 */
class JwtIssuerTest {

    // HS256은 최소 32byte secret이 필요하므로 32byte 이상 문자열 사용
    private static final String JWT_SECRET = "dev-secret-change-in-production-x";

    private JwtIssuer jwtIssuer;

    @BeforeEach
    void setUp() {
        // @Value 주입 필드를 리플렉션으로 세팅
        jwtIssuer = new JwtIssuer();
        ReflectionTestUtils.setField(jwtIssuer, "jwtSecret", JWT_SECRET);
    }

    /**
     * 주어진 secret 문자열로 HS256 검증용 SecretKey를 생성한다.
     */
    private SecretKey keyOf(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 발급된 토큰을 동일 secret으로 파싱해 Claims를 반환한다.
     */
    private Claims parseClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(keyOf(secret))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    @Nested
    @DisplayName("issue() - 정상 발급")
    class IssueSuccess {

        @Test
        @DisplayName("정상 발급 시 토큰이 점(.)으로 구분된 3파트 구조를 가진다")
        void JWT발급_정상발급_토큰3파트반환() {
            // given
            Long githubId = -12345L;
            String username = "octocat";

            // when
            String token = jwtIssuer.issue(githubId, username);

            // then
            assertThat(token).isNotBlank();
            assertThat(token.split("\\.")).hasSize(3);
        }

        @Test
        @DisplayName("정상 발급 시 동일 secret으로 검증하면 subject=githubId, claim username이 일치한다")
        void JWT발급_정상발급_subject와username클레임일치() {
            // given
            Long githubId = -999L;
            String username = "hubot";

            // when
            String token = jwtIssuer.issue(githubId, username);
            Claims claims = parseClaims(token, JWT_SECRET);

            // then
            assertThat(claims.getSubject()).isEqualTo(String.valueOf(githubId));
            assertThat(claims.get("username", String.class)).isEqualTo(username);
        }

        @Test
        @DisplayName("발급 시각(iat) 클레임이 세팅된다")
        void JWT발급_정상발급_발급시각클레임존재() {
            // given
            Long githubId = -1L;
            String username = "alice";

            // when
            String token = jwtIssuer.issue(githubId, username);
            Claims claims = parseClaims(token, JWT_SECRET);

            // then
            assertThat(claims.getIssuedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("issue() - 실패 및 경계")
    class IssueFailureAndBoundary {

        @Test
        @DisplayName("다른 secret으로 검증하면 서명 불일치로 SignatureException이 발생한다")
        void JWT검증_다른secret_SignatureException발생() {
            // given
            String token = jwtIssuer.issue(-54321L, "mona");
            String wrongSecret = "another-wrong-secret-key-32bytes!";

            // when & then
            assertThatThrownBy(() -> parseClaims(token, wrongSecret))
                    .isInstanceOf(SignatureException.class);
        }

        @Test
        @DisplayName("githubId가 Long.MAX_VALUE여도 subject로 정상 직렬화된다 (경계값)")
        void JWT발급_githubId최댓값_subject정상직렬화() {
            // given
            Long githubId = Long.MAX_VALUE;
            String username = "edge";

            // when
            String token = jwtIssuer.issue(githubId, username);
            Claims claims = parseClaims(token, JWT_SECRET);

            // then
            assertThat(claims.getSubject()).isEqualTo(String.valueOf(Long.MAX_VALUE));
            assertThat(claims.getSubject()).isEqualTo("9223372036854775807");
        }

        @Test
        @DisplayName("githubId가 Long.MIN_VALUE여도 subject로 정상 직렬화된다 (경계값)")
        void JWT발급_githubId최솟값_subject정상직렬화() {
            // given
            Long githubId = Long.MIN_VALUE;
            String username = "edge-min";

            // when
            String token = jwtIssuer.issue(githubId, username);
            Claims claims = parseClaims(token, JWT_SECRET);

            // then
            assertThat(claims.getSubject()).isEqualTo(String.valueOf(Long.MIN_VALUE));
        }
    }
}
