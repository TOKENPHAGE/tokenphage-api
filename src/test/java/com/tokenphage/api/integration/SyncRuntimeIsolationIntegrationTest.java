package com.tokenphage.api.integration;

import com.tokenphage.api.feature.auth.service.JwtIssuer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
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
 * 잘못된 실행시간 항목이 섞여도 그 항목만 버리고 토큰과 나머지 항목은 저장하는지 검증한다.
 * <p>
 * runtimeMs 가 빠진 항목은 래퍼 타입이라 역직렬화에서 요청 전체를 400 으로 만들지 않고 검사에서 걸러져야 한다.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("실행시간 항목 거부와 토큰 저장 분리")
class SyncRuntimeIsolationIntegrationTest extends ContainerSupport {

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다 (99901~99912 사용 중).
    private static final long   GITHUB_ID = 99913L;
    private static final String USERNAME  = "cli_runtime_isolation";
    private static final String DEVICE_ID = "c8eebc99-9c0b-4ef8-bb6d-6bb9bd380a99";

    @LocalServerPort
    private int port;

    @Autowired private JwtIssuer jwtIssuer;
    @Autowired private JdbcTemplate jdbc;

    private final TestRestTemplate rest = new TestRestTemplate();

    @AfterEach
    void cleanUp() {
        // RANDOM_PORT + 실제 HTTP라 롤백이 통하지 않아 직접 지운다 (FK 역순)
        jdbc.update("DELETE FROM daily_agent_runtime WHERE github_id = ?", GITHUB_ID);
        jdbc.update("DELETE FROM daily_token_usage WHERE github_id = ?", GITHUB_ID);
        jdbc.update("DELETE FROM users WHERE github_id = ?", GITHUB_ID);
    }

    @Test
    @DisplayName("runtimeMs 가 빠진 항목 1개가 섞이면 200, rejected=1 이고 토큰과 정상 항목은 저장된다")
    void 동기화_잘못된실행시간1개섞임_그항목만거부() {
        // given
        String json = """
                {"deviceId":"%s",
                 "records":[{"date":"2026-10-09","model":"claude-opus-4-1","inputTok":100,"outputTok":200,"cacheReadTok":300,"cacheCreateTok":400}],
                 "runtimes":[
                   {"date":"2026-10-09","source":"claude","model":"claude-opus-4-1","runtimeMs":7200000},
                   {"date":"2026-10-09","source":"codex","model":"gpt-5.5"},
                   {"date":"2026-10-10","source":"opencode","model":"claude-sonnet-4-5","runtimeMs":60000}]}
                """.formatted(DEVICE_ID);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtIssuer.issue(GITHUB_ID, USERNAME));

        // when
        ResponseEntity<String> res = rest.exchange("http://localhost:" + port + "/api/sync", HttpMethod.POST,
                new HttpEntity<>(json, headers), String.class);

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo("{\"runtimes\":{\"accepted\":2,\"rejected\":1}}");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM daily_token_usage WHERE github_id = ?", Long.class, GITHUB_ID))
                .isEqualTo(1L);
        assertThat(jdbc.queryForList(
                "SELECT source || '/' || model || '/' || runtime_ms FROM daily_agent_runtime WHERE github_id = ?",
                String.class, GITHUB_ID))
                .containsExactlyInAnyOrderElementsOf(List.of(
                        "claude/claude-opus-4-1/7200000",
                        "opencode/claude-sonnet-4-5/60000"));
    }
}
