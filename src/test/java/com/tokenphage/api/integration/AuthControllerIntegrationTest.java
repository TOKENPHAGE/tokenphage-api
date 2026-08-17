package com.tokenphage.api.integration;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.user.repository.UserRepository;
import com.tokenphage.api.domain.user.repository.entity.User;
import com.tokenphage.api.feature.auth.dto.request.ChallengeRequest;
import com.tokenphage.api.feature.auth.dto.request.VerifyRequest;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Gist 인증 흐름 통합 테스트.
 * <p>
 * /auth/challenge(Redis 저장) → /auth/verify(users 저장 + JWT 발급) → 그 JWT 로 /api/sync 인가.
 * 발급된 토큰이 실제 인가를 통과하는지는 여기서만 검증한다. GitHub API 는 MockWebServer 가 받는다.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthControllerIntegrationTest extends ContainerSupport {

    @LocalServerPort
    private int port;

    @Autowired private StringRedisTemplate redis;
    @Autowired private UserRepository userRepo;
    @Autowired private DailyTokenUsageRepository tokenRepo;
    @Autowired private PlatformTransactionManager txManager;
    @Autowired private JwtDecoder jwtDecoder;

    private TestRestTemplate rest;

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다
    // (Sync 99901, Reset 99902, BadgeGrant 99903·99904).
    private static final long GITHUB_ID = 99905L;
    // /auth 경로는 AuthService.USERNAME_REGEX(영숫자+하이픈)를 통과해야 한다. 다른 통합테스트가 쓰는
    // 언더스코어는 여기서 400(AUTH_001)이 된다. 요청값의 대소문자 혼용은 의도적이다 —
    // 챌린지 발급·검증 양쪽의 소문자 정규화를 한 번에 태운다.
    private static final String REQUEST_USERNAME = "Cli-Auth-Test";
    // 최종 저장되는 username 은 요청값이 아니라 Gist owner.login 이다(소유자 비교가 equalsIgnoreCase).
    private static final String OWNER_LOGIN = "cli-auth-test";
    // AuthService.REDIS_KEY_PREFIX 가 package-private 이라 값을 인라인한다.
    private static final String REDIS_KEY = "auth:challenge:cli-auth-test";
    private static final String GIST_ID = "a1b2c3d4e5f60718293a4b5c6d7e8f90";
    private static final String DEVICE_ID = "e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55";

    // auth.gist.challenge-ttl-minutes 기본값 10분. 상한은 초과가 불가능하고,
    // 하한은 HTTP 왕복 지연을 감안한 여유값이다.
    private static final long TTL_UPPER_BOUND = 600L;
    private static final long TTL_LOWER_BOUND = 540L;

    // upsert 가 ON CONFLICT (github_id, device_id, usage_date, model) 로 덮어쓰므로(합산 아님)
    // 한 요청 안에서 (날짜, 모델) 조합이 겹치면 앞 레코드가 사라진다. 5개 모두 조합을 다르게 둔다.
    // 같은 날짜에 다른 모델(7-01), 캐시 미사용(7-02), 사용량 급증일(7-03)을 섞어 실제 사용 패턴에 가깝게 만든다.
    private static final List<TokenRecordRequest> SYNC_RECORDS = List.of(
        new TokenRecordRequest("2026-07-01", "claude-sonnet-4-6",          12_000,  4_800, 3_200,   900),
        new TokenRecordRequest("2026-07-01", "claude-opus-4-8",             8_500,  6_200, 1_500,   400),
        new TokenRecordRequest("2026-07-02", "claude-haiku-4-5-20251001",     900,    350,     0,     0),
        new TokenRecordRequest("2026-07-03", "claude-sonnet-4-6",          25_400, 11_600, 9_800, 2_100),
        new TokenRecordRequest("2026-07-04", "claude-opus-4-7",             3_100,  1_450,   700,   150)
    );
    /** sumTotalTokens 는 input + output 만 더한다 (cacheRead/cacheCreate 는 제외). 레코드별 소계로 적어 검산이 쉽도록 둔다. */
    private static final long EXPECTED_TOTAL = 16_800L + 14_700L + 1_250L + 37_000L + 4_550L;

    @BeforeEach
    void setUp() throws InterruptedException {
        rest = new TestRestTemplate();
        // 스텁을 "무조건 실패" 상태로 되돌려 이전 테스트(또는 이전 테스트 클래스)의 응답이 새지 않게 한다.
        respondWith(500, "{}");
        drainRecordedRequests();
        redis.delete(REDIS_KEY);
    }

    @AfterEach
    void cleanUp() {
        // 컨테이너는 실행 단위로 일회용이지만 테스트 간에는 공유되므로 직접 정리한다.
        // RANDOM_PORT + 실제 HTTP라 @Transactional 롤백은 통하지 않는다 (서버가 다른 스레드에서 커밋한다).
        // @Modifying 삭제는 트랜잭션이 필요하므로 커밋 트랜잭션으로 감싼다 (FK상 토큰 → 사용자 순서).
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            tokenRepo.deleteAllByGithubId(GITHUB_ID);
            userRepo.findById(GITHUB_ID).ifPresent(userRepo::delete);
        });
        // 실패 경로는 챌린지를 소비하지 않아 키가 남는다(성공 시에만 AuthService 가 지운다).
        redis.delete(REDIS_KEY);
    }

    @Test @Order(1)
    @DisplayName("[AUTH] POST /auth/challenge → 200, 실 Redis에 챌린지 저장 + TTL 적용")
    void postChallenge_storesChallengeInRedisWithTtl() {
        // given: 챌린지가 없는 상태 (@BeforeEach 가 보장)

        // when
        ResponseEntity<String> res = rest.exchange(
            url("/auth/challenge"), HttpMethod.POST,
            new HttpEntity<>(new ChallengeRequest(REQUEST_USERNAME), jsonHeaders()), String.class
        );

        // then: 인증 없이 통과하고(permitAll), 요청값 대소문자와 무관하게 소문자 키로 저장된다
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        String stored = redis.opsForValue().get(REDIS_KEY);
        assertThat(stored).startsWith("tknphg_");
        // 응답 본문의 값과 Redis 실제 저장값이 같아야 한다 — 직렬화 템플릿이 바뀌면 여기서 깨진다.
        assertThat(jsonField(res.getBody(), "challenge")).isEqualTo(stored);

        // then: TTL 이 실제로 걸린다. -1(TTL 미설정)·-2(키 없음)·단위 오류·배수 오류가 모두 걸린다.
        Long ttl = redis.getExpire(REDIS_KEY, TimeUnit.SECONDS);
        assertThat(ttl).isBetween(TTL_LOWER_BOUND, TTL_UPPER_BOUND);
    }

    @Test @Order(2)
    @DisplayName("[AUTH] 챌린지 없이 verify → 401 + AUTH_002, Gist 호출 없음")
    void postVerify_withoutChallenge_returns401() {
        // given: Redis 에 챌린지가 없는 상태
        int callsBefore = GIST_API.getRequestCount();

        // when
        ResponseEntity<String> res = postVerify();

        // then: AuthErrorCode → 실 HTTP status 매핑이 살아 있어야 한다
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(res.getBody()).contains("AUTH_002");
        // 챌린지가 없으면 외부 API 를 때리기 전에 끊어야 한다
        assertThat(GIST_API.getRequestCount()).isEqualTo(callsBefore);
    }

    @Test @Order(3)
    @DisplayName("[AUTH] Gist 조회가 GET /gists/{id} + GitHub 계약 헤더로 나간다")
    void postVerify_callsGistApiWithGitHubContract() throws InterruptedException {
        // given
        String challenge = requestChallenge();
        respondWith(200, gistJson(challenge));

        // when
        postVerify();

        // then: 유닛테스트가 uri(anyString(), any()) 로 우회하는 구간을 실제 소켓으로 확인한다
        RecordedRequest recorded = GIST_API.takeRequest(2, TimeUnit.SECONDS);
        assertThat(recorded).isNotNull();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/gists/" + GIST_ID);
        assertThat(recorded.getHeader("Accept")).isEqualTo("application/vnd.github+json");
        assertThat(recorded.getHeader("X-GitHub-Api-Version")).isEqualTo("2022-11-28");
        assertThat(recorded.getHeader("Authorization")).isEqualTo("Bearer " + GIST_API_TOKEN);
    }

    @Test @Order(4)
    @DisplayName("[AUTH] verify 성공 → 200 + users 행 생성")
    void postVerify_createsUserRow() {
        // given
        String challenge = requestChallenge();
        respondWith(200, gistJson(challenge));

        // when
        ResponseEntity<String> res = postVerify();

        // then: GitHub JSON → record 역직렬화 전 구간이 걸린다.
        // "public" 키가 isPublic 에 매핑되지 않으면 null 이 되어 AUTH_006 401 로 떨어진다.
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("\"githubId\":" + GITHUB_ID);
        assertThat(jsonField(res.getBody(), "username")).isEqualTo(OWNER_LOGIN);

        // then: 요청값(Cli-Auth-Test)이 아니라 Gist owner.login 이 저장된다
        assertThat(userRepo.findById(GITHUB_ID))
            .get().extracting(User::getUsername).isEqualTo(OWNER_LOGIN);
    }

    @Test @Order(5)
    @DisplayName("[AUTH] verify 성공 후 챌린지 소멸 → 동일 요청 재시도 401")
    void postVerify_consumesChallenge_replayReturns401() {
        // given: 한 번 성공시킨다
        String challenge = requestChallenge();
        respondWith(200, gistJson(challenge));
        assertThat(postVerify().getStatusCode()).isEqualTo(HttpStatus.OK);

        // when: 삭제가 저장과 같은 정규화 키를 지웠는지 확인한다 (한쪽만 소문자면 키가 남아 리플레이가 성공한다)
        assertThat(redis.hasKey(REDIS_KEY)).isFalse();
        ResponseEntity<String> replay = postVerify();

        // then: 챌린지는 일회용이다
        assertThat(replay.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(replay.getBody()).contains("AUTH_002");
    }

    @Test @Order(6)
    @DisplayName("[JWT] verify가 발급한 토큰으로 POST /api/sync → 200, sub·username 클레임 왕복")
    void verifyIssuedToken_authorizesSync_andCarriesClaims() {
        // given: jwtIssuer 를 직접 부르지 않고 실제 인증 흐름으로 토큰을 얻는다 — 이게 이 테스트의 요점이다
        String token = obtainTokenViaAuthFlow();

        // then: 앱이 실제로 쓰는 디코더로 풀어 발급 ↔ 검증 계약을 잠근다
        Jwt decoded = jwtDecoder.decode(token);
        assertThat(decoded.getSubject()).isEqualTo(String.valueOf(GITHUB_ID));
        assertThat(decoded.getClaimAsString("username")).isEqualTo(OWNER_LOGIN);
        // 무만료는 의도된 정책이다 (JwtIssuer 가 exp 를 넣지 않고 SecurityConfig 가 Instant.MAX 로 폴백).
        assertThat(decoded.getExpiresAt()).isAfter(Instant.now().plus(3650, ChronoUnit.DAYS));

        // when: verify 가 만든 users 행을 지운다. 이렇게 해야 아래 단언이 판별력을 갖는다 —
        // 행이 남아 있으면 username 이 클레임을 타고 왔는지 확인할 수 없다.
        new TransactionTemplate(txManager).executeWithoutResult(status ->
            userRepo.findById(GITHUB_ID).ifPresent(userRepo::delete));

        ResponseEntity<String> res = rest.exchange(
            url("/api/sync"), HttpMethod.POST,
            new HttpEntity<>(new SyncRequest(DEVICE_ID, SYNC_RECORDS), bearerHeaders(token)), String.class
        );

        // then: 인증 통과 + 두 클레임이 소비단(SyncOrchestrator)까지 온전히 도달했다
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(userRepo.findById(GITHUB_ID))
            .get().extracting(User::getUsername).isEqualTo(OWNER_LOGIN);
        assertThat(tokenRepo.sumTotalTokens(GITHUB_ID)).isEqualTo(EXPECTED_TOTAL);
    }

    // ── 헬퍼 ────────────────────────────────────────────────

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders bearerHeaders(String token) {
        HttpHeaders headers = jsonHeaders();
        headers.setBearerAuth(token);
        return headers;
    }

    private ResponseEntity<String> postVerify() {
        return rest.exchange(
            url("/auth/verify"), HttpMethod.POST,
            new HttpEntity<>(new VerifyRequest(REQUEST_USERNAME, GIST_ID), jsonHeaders()), String.class
        );
    }

    /** POST /auth/challenge 를 호출하고 발급된 챌린지를 돌려준다. 챌린지가 필요한 테스트의 arrange. */
    private String requestChallenge() {
        ResponseEntity<String> res = rest.exchange(
            url("/auth/challenge"), HttpMethod.POST,
            new HttpEntity<>(new ChallengeRequest(REQUEST_USERNAME), jsonHeaders()), String.class
        );
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return jsonField(res.getBody(), "challenge");
    }

    /** 챌린지 발급 → Gist 응답 스텁 → verify 까지 실제 흐름을 태워 JWT 를 얻는다. */
    private String obtainTokenViaAuthFlow() {
        String challenge = requestChallenge();
        respondWith(200, gistJson(challenge));
        ResponseEntity<String> res = postVerify();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        return jsonField(res.getBody(), "token");
    }

    /**
     * MockWebServer 가 이후 모든 요청에 같은 응답을 주도록 디스패처를 교체한다.
     * <p>
     * enqueue() 큐 방식을 쓰지 않는 이유: QueueDispatcher 는 큐가 비면 무한 블로킹하는데
     * gistRestClient 에 read timeout 이 없어 테스트 JVM 이 영구 hang 된다. 항상 응답하는
     * 디스패처로 그 경로를 아예 없앤다.
     */
    private void respondWith(int code, String body) {
        GIST_API.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                // Content-Type 이 없으면 Jackson 컨버터가 매칭되지 않아 RestClientException →
                // AUTH_004(503) 로 오분류된다. 반드시 붙인다.
                return new MockResponse()
                    .setResponseCode(code)
                    .setHeader("Content-Type", "application/json")
                    .setBody(body);
            }
        });
    }

    /**
     * 기록된 요청을 비운다.
     * <p>
     * takeRequest 큐는 Dispatcher 가 아니라 MockWebServer 인스턴스에 누적되므로 디스패처를
     * 교체해도 걷히지 않는다. 무인자 takeRequest() 는 블로킹이라 타임아웃 오버로드를 쓴다.
     */
    private void drainRecordedRequests() throws InterruptedException {
        while (GIST_API.takeRequest(0, TimeUnit.MILLISECONDS) != null) {
            // 이전 테스트가 소비하지 않은 기록 폐기
        }
    }

    /**
     * GitHub /gists/{id} 응답 형태의 raw JSON.
     * <p>
     * record 를 직렬화하지 않고 실물 필드명을 그대로 쓴다 — record 직렬화는 매핑이 깨져도 왕복이
     * 성립해 초록불이 된다. "public" 키는 GistResponse 의 @JsonProperty("public") 에 매핑되고,
     * 모르는 필드(html_url, type)는 @JsonIgnoreProperties 가 흡수한다.
     * content 끝의 개행은 의도적이다 — 검증이 trim() 후 정확 일치라 그 경로를 실제로 태운다.
     */
    private String gistJson(String challenge) {
        return """
            {
              "id": "%s",
              "public": true,
              "html_url": "https://gist.github.com/%s",
              "owner": { "id": %d, "login": "%s", "type": "User" },
              "files": {
                "tokenphage.txt": { "filename": "tokenphage.txt", "type": "text/plain", "content": "%s\\n" }
              }
            }
            """.formatted(GIST_ID, GIST_ID, GITHUB_ID, OWNER_LOGIN, challenge);
    }

    /**
     * 응답 JSON 에서 문자열 필드 값을 뽑는다.
     * <p>
     * DTO 역직렬화 대신 와이어 포맷을 직접 읽는다. TestRestTemplate 의 컨버터 설정에 의존하지 않고,
     * 덤으로 CLI 가 보는 실제 필드명을 계약으로 고정한다.
     */
    private static String jsonField(String body, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]*)\"").matcher(body);
        assertThat(matcher.find()).as("응답에 %s 필드가 없다: %s", field, body).isTrue();
        return matcher.group(1);
    }
}
