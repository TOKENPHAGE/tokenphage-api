package com.tokenphage.api.feature.sync;

import com.tokenphage.api.domain.badge.BadgeCacheInvalidator;
import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.token.repository.projection.ModelUsageRow;
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
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLI → API 전체 흐름 통합 테스트
 *
 * 흐름: CLI parser 출력(TokenRecord) → POST /api/sync → DB 저장 → GET /badge/{username} → SVG
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "badge.jwt-secret=dev-secret-change-in-production-x"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SyncFlowIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired private JwtIssuer jwtIssuer;
    @Autowired private DailyTokenUsageRepository tokenRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private BadgeCacheInvalidator badgeCacheInvalidator;
    @Autowired private PlatformTransactionManager txManager;

    private TestRestTemplate rest;

    // GitHub user ID는 항상 양수(1부터 순차 발급)이므로, 음수는 실제 사용자와 절대 충돌하지 않는 안전한 테스트 전용 값이다.
    private static final long   GITHUB_ID  = -99901L;
    // username도 GitHub 정책상 불가능한 값(언더스코어는 GitHub 사용자명에 허용되지 않음) → 실유저와 UNIQUE 충돌 불가
    private static final String USERNAME   = "cli_test_user";
    private static final String DEVICE_ID  = "c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33";

    private static final List<TokenRecordRequest> PARSED_RECORDS = List.of(
        new TokenRecordRequest("2026-05-01", "claude-sonnet-4-6",         3500, 2350, 1200, 400),
        new TokenRecordRequest("2026-05-02", "claude-opus-4-7",           5000, 3200, 2100, 600),
        new TokenRecordRequest("2026-05-03", "claude-haiku-4-5-20251001",  800,  400,  300,  80),
        new TokenRecordRequest("2026-05-03", "claude-sonnet-4-6",         3100, 1800,  900, 200)
    );
    private static final long EXPECTED_TOTAL = 20_150L;

    @BeforeEach
    void setUp() {
        rest = new TestRestTemplate();
    }

    @AfterEach
    void cleanUp() {
        // 실 DB에 남긴 테스트 fixture를 삭제해 격리를 보장한다.
        // @Modifying 삭제는 트랜잭션이 필요하므로 커밋 트랜잭션으로 감싼다 (FK상 토큰 → 사용자 순서).
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            tokenRepo.deleteAllByGithubId(GITHUB_ID);
            userRepo.findById(GITHUB_ID).ifPresent(userRepo::delete);
        });
        // 배지 렌더 테스트가 남긴 Redis 캐시(badge:cli_test_user:*)도 정리한다.
        badgeCacheInvalidator.evict(USERNAME);
    }

    @Test @Order(1)
    @DisplayName("[JWT] JwtIssuer.issue() — header.payload.sig 구조")
    void issueJwt_returnsValidStructure() {
        String token = jwtIssuer.issue(GITHUB_ID, USERNAME);
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test @Order(2)
    @DisplayName("[SYNC] CLI 파서 레코드 전송 → HTTP 200")
    void postSync_returnsOk() {
        assertThat(doSync().getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test @Order(3)
    @DisplayName("[SYNC] 인증 없이 요청 → HTTP 401")
    void postSync_withoutAuth_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Void> res = rest.exchange(
            url("/api/sync"), HttpMethod.POST,
            new HttpEntity<>(new SyncRequest(DEVICE_ID, PARSED_RECORDS), headers),
            Void.class
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test @Order(4)
    @DisplayName("[DB] 전송 레코드가 daily_token_usage에 저장됨 — 토큰 합계 검증")
    void db_totalTokensStoredCorrectly() {
        doSync();
        assertThat(tokenRepo.sumTotalTokens(GITHUB_ID)).isEqualTo(EXPECTED_TOTAL);
    }

    @Test @Order(5)
    @DisplayName("[DB] Top5 쿼리 — sonnet이 1위 (2일치 합산)")
    void db_top5Models_sonnetRanksFirst() {
        doSync();
        List<ModelUsageRow> top5 = tokenRepo.findTop5Models(GITHUB_ID);
        assertThat(top5).isNotEmpty();
        assertThat(top5.get(0).getModel()).contains("sonnet");
    }

    @Test @Order(7)
    @DisplayName("[DB] UPSERT — 동일 레코드 재전송 시 중복 합산 없음")
    void db_upsertDoesNotDuplicate() {
        doSync();
        Long before = tokenRepo.sumTotalTokens(GITHUB_ID);
        doSync();
        assertThat(tokenRepo.sumTotalTokens(GITHUB_ID)).isEqualTo(before);
    }

    @Test @Order(8)
    @DisplayName("[BADGE] GET /badge/cli_test_user — 실 DB 기반 SVG 반환")
    void badge_lightTheme_svgFromRealData() {
        doSync();
        ResponseEntity<String> res = rest.getForEntity(url("/badge/" + USERNAME), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentType().toString()).contains("svg");
        assertThat(res.getBody()).contains("<svg ").contains("</svg>")
            .contains(USERNAME).contains("sonnet");
    }

    @Test @Order(9)
    @DisplayName("[BADGE] GET /badge/cli_test_user?theme=gpu&mode=dark — 다크 배경 포함")
    void badge_darkTheme_svgRendered() {
        doSync();
        ResponseEntity<String> res = rest.getForEntity(url("/badge/" + USERNAME + "?theme=gpu&mode=dark"), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("#0f172a");
    }

    @Test @Order(10)
    @DisplayName("[BADGE] GET /badge/nonexistent — HTTP 404")
    void badge_unknownUser_returns404() {
        ResponseEntity<String> res = rest.getForEntity(url("/badge/nobody-xyz"), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Void> doSync() {
        String token = jwtIssuer.issue(GITHUB_ID, USERNAME);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return rest.exchange(
            url("/api/sync"), HttpMethod.POST,
            new HttpEntity<>(new SyncRequest(DEVICE_ID, PARSED_RECORDS), headers),
            Void.class
        );
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
