package com.tokenphage.api.integration;

import com.tokenphage.api.domain.badge.BadgeCacheInvalidator;
import com.tokenphage.api.domain.runtime.repository.DailyAgentRuntimeRepository;
import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.token.repository.projection.ModelUsageRow;
import com.tokenphage.api.domain.user.repository.UserRepository;
import com.tokenphage.api.feature.auth.service.JwtIssuer;
import com.tokenphage.api.feature.sync.dto.request.RuntimeRecordRequest;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CLI → API 전체 흐름 통합 테스트
 *
 * 흐름: CLI parser 출력(TokenRecord) → POST /api/sync → DB 저장 → GET /badge/{username} → SVG
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SyncControllerIntegrationTest extends ContainerSupport {

    @LocalServerPort
    private int port;

    @Autowired private JwtIssuer jwtIssuer;
    @Autowired private DailyTokenUsageRepository tokenRepo;
    @Autowired private DailyAgentRuntimeRepository runtimeRepo;
    @Autowired private UserRepository userRepo;
    @Autowired private BadgeCacheInvalidator badgeCacheInvalidator;
    @Autowired private PlatformTransactionManager txManager;

    private TestRestTemplate rest;

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다
    // (ResetControllerIntegrationTest는 99902 / cli_reset_test).
    private static final long   GITHUB_ID  = 99901L;
    private static final String USERNAME   = "cli_test_user";
    private static final String DEVICE_ID  = "c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33";

    private static final List<TokenRecordRequest> PARSED_RECORDS = List.of(
        new TokenRecordRequest("2026-05-01", "claude-sonnet-4-6",         3500, 2350, 1200, 400),
        new TokenRecordRequest("2026-05-02", "claude-opus-4-7",           5000, 3200, 2100, 600),
        new TokenRecordRequest("2026-05-03", "claude-haiku-4-5-20251001",  800,  400,  300,  80),
        new TokenRecordRequest("2026-05-03", "claude-sonnet-4-6",         3100, 1800,  900, 200)
    );
    private static final long EXPECTED_TOTAL = 21_430L;

    // 도구 3종에 나눠 보낸 가동시간. 주행거리계는 도구를 가리지 않고 전부 더한다.
    private static final List<RuntimeRecordRequest> PARSED_RUNTIMES = List.of(
        new RuntimeRecordRequest("2026-05-01", "claude",   "claude-sonnet-4-6", 4_000_000_000L),
        new RuntimeRecordRequest("2026-05-02", "codex",    "gpt-5.5",             600_000_000L),
        new RuntimeRecordRequest("2026-05-03", "opencode", "claude-opus-4-7",      24_560_000L)
    );
    // 합 4,624,560,000ms = 1,284.6시간. 0.1시간 단위 12,846을 6자리로 채운 값이다.
    private static final String EXPECTED_ODOMETER_DIGITS = "012846";

    /** 바퀴마다 멈췄을 때 창에 보이는 숫자(y=40) 한 글자. 숫자 띠의 나머지는 y가 다르다. */
    private static final Pattern FINAL_DIGIT = Pattern.compile("<text x=\"[0-9.]+\" y=\"40\"[^>]*>(\\d)</text>");

    @BeforeEach
    void setUp() {
        rest = new TestRestTemplate();
    }

    @AfterEach
    void cleanUp() {
        // 컨테이너는 실행 단위로 일회용이지만 테스트 간에는 공유되므로 직접 정리한다.
        // RANDOM_PORT + 실제 HTTP라 @Transactional 롤백은 통하지 않는다 (서버가 다른 스레드에서 커밋한다).
        // @Modifying 삭제는 트랜잭션이 필요하므로 커밋 트랜잭션으로 감싼다 (FK상 토큰·가동시간 → 사용자 순서).
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            tokenRepo.deleteAllByGithubId(GITHUB_ID);
            runtimeRepo.deleteAllByGithubId(GITHUB_ID);
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

    @Test @Order(11)
    @DisplayName("[BADGE] 가동시간 sync 후 GET /badge/cli_test_user?theme=odometer-claude — 잠금이 아닌 300×64 주행거리계")
    void 주행거리계배지_가동시간sync후조회_300x64배지반환() {
        // given
        doSync(new SyncRequest(DEVICE_ID, PARSED_RECORDS, PARSED_RUNTIMES));

        // when
        ResponseEntity<String> res = rest.getForEntity(url("/badge/" + USERNAME + "?theme=odometer-claude"), String.class);

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody())
            .contains("width=\"300\" height=\"64\"")
            .contains("aria-label=\"Agent runtime odometer " + USERNAME + "\"")
            .doesNotContain("Locked badge");
    }

    @Test @Order(12)
    @DisplayName("[BADGE] 주행거리계 바퀴의 최종 숫자 — 보낸 가동시간 합의 0.1시간 환산값")
    void 주행거리계배지_가동시간sync후조회_합계환산숫자표시() {
        // given
        doSync(new SyncRequest(DEVICE_ID, PARSED_RECORDS, PARSED_RUNTIMES));

        // when
        ResponseEntity<String> res = rest.getForEntity(url("/badge/" + USERNAME + "?theme=odometer-claude"), String.class);

        // then
        assertThat(finalDigitsOf(res.getBody())).isEqualTo(EXPECTED_ODOMETER_DIGITS);
    }

    @Test @Order(13)
    @DisplayName("[BADGE] 주행거리계 mode 미지정 — light 몸통")
    void 주행거리계배지_mode미지정_light몸통() {
        // given
        doSync(new SyncRequest(DEVICE_ID, PARSED_RECORDS, PARSED_RUNTIMES));

        // when
        ResponseEntity<String> res = rest.getForEntity(url("/badge/" + USERNAME + "?theme=odometer-claude"), String.class);

        // then
        assertThat(res.getBody())
            .contains("<rect x=\".5\" y=\".5\" width=\"299\" height=\"63\" rx=\"11.5\" fill=\"#ffffff\" stroke=\"#e5e7eb\"/>")
            .doesNotContain("#2d323c");
    }

    @Test @Order(14)
    @DisplayName("[BADGE] 주행거리계 mode=dark — dark 그라데이션 몸통")
    void 주행거리계배지_modeDark_dark몸통() {
        // given
        doSync(new SyncRequest(DEVICE_ID, PARSED_RECORDS, PARSED_RUNTIMES));

        // when
        ResponseEntity<String> res = rest.getForEntity(
            url("/badge/" + USERNAME + "?theme=odometer-claude&mode=dark"), String.class);

        // then
        assertThat(res.getBody())
            .contains("<stop offset=\"0\" stop-color=\"#2d323c\"/>")
            .contains("width=\"300\" height=\"64\"");
    }

    @Test @Order(15)
    @DisplayName("[BADGE] 주행거리계가 캐시된 뒤 가동시간 sync — 다음 조회가 새 숫자를 반영")
    void 주행거리계배지_캐시된뒤가동시간sync_다음조회에새숫자() {
        // given
        // 가동시간 없이 한 번 조회해 000000 배지를 캐시에 올려 둔 뒤 가동시간을 보낸다.
        String odometerUrl = url("/badge/" + USERNAME + "?theme=odometer-claude");
        doSync();
        String cachedDigits = finalDigitsOf(rest.getForEntity(odometerUrl, String.class).getBody());
        doSync(new SyncRequest(DEVICE_ID, PARSED_RECORDS, PARSED_RUNTIMES));

        // when
        ResponseEntity<String> res = rest.getForEntity(odometerUrl, String.class);

        // then
        // sync가 캐시를 비우지 않으면 TTL(60분) 동안 000000이 그대로 나온다.
        assertThat(cachedDigits).isEqualTo("000000");
        assertThat(finalDigitsOf(res.getBody())).isEqualTo(EXPECTED_ODOMETER_DIGITS);
    }

    /** 바퀴가 멈췄을 때 창에 보이는 숫자 6개를 이어 붙인다. */
    private static String finalDigitsOf(String svg) {
        return FINAL_DIGIT.matcher(svg).results()
            .map(r -> r.group(1))
            .collect(Collectors.joining());
    }

    private ResponseEntity<Void> doSync() {
        return doSync(new SyncRequest(DEVICE_ID, PARSED_RECORDS));
    }

    private ResponseEntity<Void> doSync(SyncRequest body) {
        String token = jwtIssuer.issue(GITHUB_ID, USERNAME);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return rest.exchange(
            url("/api/sync"), HttpMethod.POST,
            new HttpEntity<>(body, headers),
            Void.class
        );
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
