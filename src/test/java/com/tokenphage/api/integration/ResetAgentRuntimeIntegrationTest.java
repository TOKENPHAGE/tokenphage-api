package com.tokenphage.api.integration;

import com.tokenphage.api.feature.auth.service.JwtIssuer;
import com.tokenphage.api.feature.sync.dto.request.RuntimeRecordRequest;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * /api/reset 이 토큰 사용량과 실행시간을 함께 지우는지 검증한다.
 * <p>
 * 실행시간이 남으면 reset 뒤 다시 보낸 값과 섞여 사용자 데이터가 처음 상태로 돌아가지 않는다.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("reset 의 실행시간 삭제")
class ResetAgentRuntimeIntegrationTest extends ContainerSupport {

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다 (99901~99911 사용 중).
    private static final long   GITHUB_ID = 99912L;
    private static final String USERNAME  = "cli_reset_runtime";
    private static final String DEVICE_ID = "b7eebc99-9c0b-4ef8-bb6d-6bb9bd380a88";
    private static final String COOLDOWN_KEY = "cooldown:reset:" + GITHUB_ID;

    @LocalServerPort
    private int port;

    @Autowired private JwtIssuer jwtIssuer;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private StringRedisTemplate redis;

    private final TestRestTemplate rest = new TestRestTemplate();

    @BeforeEach
    void setUp() {
        redis.delete(COOLDOWN_KEY);
    }

    @AfterEach
    void cleanUp() {
        // RANDOM_PORT + 실제 HTTP라 롤백이 통하지 않아 직접 지운다 (FK 역순)
        jdbc.update("DELETE FROM daily_agent_runtime WHERE github_id = ?", GITHUB_ID);
        jdbc.update("DELETE FROM daily_token_usage WHERE github_id = ?", GITHUB_ID);
        jdbc.update("DELETE FROM users WHERE github_id = ?", GITHUB_ID);
        redis.delete(COOLDOWN_KEY);
    }

    private long countRows(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE github_id = ?", Long.class, GITHUB_ID);
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

    @Test
    @DisplayName("sync 로 쌓인 토큰과 실행시간을 reset 이 모두 지운다")
    void 초기화_토큰과실행시간있음_둘다삭제() {
        // given: runtimes 칸이 있는 요청으로 두 표에 행을 쌓는다
        SyncRequest req = new SyncRequest(DEVICE_ID,
                List.of(new TokenRecordRequest("2026-10-09", "claude-opus-4-1", 100, 200, 300, 400)),
                List.of(new RuntimeRecordRequest("2026-10-09", "claude", "claude-opus-4-1", 7_200_000L),
                        new RuntimeRecordRequest("2026-10-09", "opencode", "claude-opus-4-1", 600_000L)));
        ResponseEntity<String> synced = rest.exchange(url("/api/sync"), HttpMethod.POST,
                new HttpEntity<>(req, bearerHeaders()), String.class);
        assertThat(synced.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(countRows("daily_token_usage")).isEqualTo(1L);
        assertThat(countRows("daily_agent_runtime")).isEqualTo(2L);

        // when
        ResponseEntity<Void> reset = rest.exchange(url("/api/reset"), HttpMethod.POST,
                new HttpEntity<>(bearerHeaders()), Void.class);

        // then
        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(countRows("daily_token_usage")).isZero();
        assertThat(countRows("daily_agent_runtime")).isZero();
    }
}
