package com.tokenphage.api.integration;

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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 전체 초기화(reset) 통합 테스트.
 * <p>
 * /api/sync 로 상태를 적재한 뒤 /api/reset 을 호출해 행 삭제와 쿨다운(429)을 검증한다.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResetControllerIntegrationTest extends ContainerSupport {

    @LocalServerPort
    private int port;

    @Autowired private JwtIssuer jwtIssuer;
    @Autowired private DailyTokenUsageRepository tokenRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private StringRedisTemplate redis;
    @Autowired private PlatformTransactionManager txManager;

    private TestRestTemplate rest;

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다
    // (SyncControllerIntegrationTest는 99901 / cli_test_user).
    private static final long   GITHUB_ID = 99902L;
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
        // 컨테이너는 실행 단위로 일회용이지만 테스트 간에는 공유되므로 직접 정리한다
        // (reset은 토큰만 지우고 users 행은 남김). RANDOM_PORT + 실제 HTTP라 @Transactional 롤백은 통하지 않는다.
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
