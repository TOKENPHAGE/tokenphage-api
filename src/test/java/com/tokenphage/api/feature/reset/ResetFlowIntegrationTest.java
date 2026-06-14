package com.tokenphage.api.feature.reset;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.user.repository.UserRepository;
import com.tokenphage.api.feature.auth.service.JwtIssuer;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전체 초기화(reset) 통합 테스트
 *
 * 흐름: POST /api/sync(상태 적재) → POST /api/reset → DB row 삭제 + 쿨다운(429) 검증.
 * 상태는 실 엔드포인트로만 적재한다(no-mock-data 규칙: 직접 INSERT 금지).
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "badge.jwt-secret=dev-secret-change-in-production-x"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResetFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired private JwtIssuer jwtIssuer;
    @Autowired private DailyTokenUsageRepository tokenRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PlatformTransactionManager txManager;

    private TestRestTemplate rest;

    // GitHub user ID는 항상 양수(1부터 순차 발급)이므로, 음수는 실제 사용자와 절대 충돌하지 않는 안전한 테스트 전용 값이다.
    private static final long   GITHUB_ID = -99902L;
    // username도 GitHub 정책상 불가능한 값(언더스코어는 GitHub 사용자명에 허용되지 않음) → 실유저와 UNIQUE 충돌 불가
    private static final String USERNAME  = "cli_reset_test";
    private static final String DEVICE_ID = "d3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44";

    // reset이 토큰 사용량을 실제로 비우는지 검증하기 위한 적재 데이터(합계 110,000).
    private static final List<TokenRecordRequest> BIG_RECORDS = List.of(
        new TokenRecordRequest("2026-06-01", "claude-opus-4-8", 80_000, 30_000, 5_000, 2_000)
    );

    @BeforeEach
    void setUp() {
        rest = new TestRestTemplate();
        // 테스트 격리: 이전 테스트가 남긴 쿨다운 키 제거
        redis.delete("cooldown:reset:" + GITHUB_ID);
    }

    @AfterEach
    void cleanUp() {
        // 실 DB/Redis에 남긴 테스트 fixture를 삭제해 격리를 보장한다 (reset은 토큰만 지우고 users 행은 남김).
        // @Modifying 삭제는 트랜잭션이 필요하므로 커밋 트랜잭션으로 감싼다 (FK상 토큰 → 사용자 순서).
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            tokenRepo.deleteAllByGithubId(GITHUB_ID);
            userRepo.findById(GITHUB_ID).ifPresent(userRepo::delete);
        });
        redis.delete("cooldown:reset:" + GITHUB_ID);
    }

    @Test @Order(1)
    @DisplayName("[RESET] 인증 없이 요청 → HTTP 401")
    void postReset_withoutAuth_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Void> res = rest.exchange(
            url("/api/reset"), HttpMethod.POST, new HttpEntity<>(headers), Void.class
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(2)
    @DisplayName("[RESET] sync 후 reset → 200, 토큰 0")
    void postReset_wipesUsage() {
        // Arrange: 실 sync로 데이터 적재
        assertThat(doSync().getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tokenRepo.sumTotalTokens(GITHUB_ID)).isEqualTo(110_000L);

        // Act
        assertThat(doReset().getStatusCode()).isEqualTo(HttpStatus.OK);

        // Assert: row 전삭제(SUM=null)
        assertThat(tokenRepo.sumTotalTokens(GITHUB_ID)).isNull();
    }

    @Test @Order(3)
    @DisplayName("[RESET] 24h 쿨다운 → 두 번째 호출 429 + RESET_001")
    void postReset_secondCall_returns429() {
        assertThat(doReset().getStatusCode()).isEqualTo(HttpStatus.OK); // 1st: 쿨다운 선점
        ResponseEntity<String> second = doResetRaw();                   // 2nd: 쿨다운 적중
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(second.getBody()).contains("RESET_001");
    }

    private ResponseEntity<Void> doSync() {
        return rest.exchange(
            url("/api/sync"), HttpMethod.POST,
            new HttpEntity<>(new SyncRequest(DEVICE_ID, BIG_RECORDS), bearerHeaders()),
            Void.class
        );
    }

    private ResponseEntity<Void> doReset() {
        return rest.exchange(url("/api/reset"), HttpMethod.POST, new HttpEntity<>(bearerHeaders()), Void.class);
    }

    private ResponseEntity<String> doResetRaw() {
        return rest.exchange(url("/api/reset"), HttpMethod.POST, new HttpEntity<>(bearerHeaders()), String.class);
    }

    private HttpHeaders bearerHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtIssuer.issue(GITHUB_ID, USERNAME));
        return headers;
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
