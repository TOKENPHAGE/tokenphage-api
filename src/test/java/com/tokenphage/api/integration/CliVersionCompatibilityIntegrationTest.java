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
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시된 CLI 버전이 보내는 /api/sync 요청 본문을 최신 서버에 그대로 보내 하위호환을 확인한다.
 * <p>
 * 본문은 직렬화하지 않은 원문 문자열이라 runtimes 칸이 아예 없는 2026.8.1까지의 요청을 그대로 재현한다.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("CLI 버전별 /api/sync 하위호환")
class CliVersionCompatibilityIntegrationTest extends ContainerSupport {

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다 (99901~99908, 99910~99914 사용 중).
    private static final long   GITHUB_ID = 99909L;
    private static final String USERNAME  = "cli_contract_test";
    private static final String DEVICE_ID = "7d3c2a1b-0f4e-4c8d-9a6b-5e2f1c0d9b8a";

    private static final String RECORDS_JSON = """
            [{"date":"2026-09-01","model":"claude-sonnet-4-5","inputTok":200,"outputTok":550,"cacheReadTok":18500,"cacheCreateTok":500},
             {"date":"2026-09-01","model":"claude-opus-4-1","inputTok":300,"outputTok":650,"cacheReadTok":12000,"cacheCreateTok":800},
             {"date":"2026-09-02","model":"claude-opus-4-1","inputTok":650,"outputTok":1020,"cacheReadTok":31000,"cacheCreateTok":1200},
             {"date":"2026-09-02","model":"claude-haiku-4-5","inputTok":160,"outputTok":240,"cacheReadTok":6200,"cacheCreateTok":400},
             {"date":"2026-09-03","model":"claude-sonnet-4-5","inputTok":210,"outputTok":800,"cacheReadTok":14600,"cacheCreateTok":900},
             {"date":"2026-09-03","model":"claude-opus-4-1","inputTok":500,"outputTok":260,"cacheReadTok":20000,"cacheCreateTok":1500}]""";

    /** 2026.7.0-BETA, 2026.7.1-BETA, 2026.7.2-BETA, 2026.8.1 이 보내는 본문. 네 버전 모두 같다. */
    private static final String BODY_2026_8_1 = """
            {"deviceId":"%s","records":%s}""".formatted(DEVICE_ID, RECORDS_JSON);

    /** 2026.8.1까지는 User-Agent 를 직접 붙이지 않아 Node fetch 기본값이 간다. */
    private static final String USER_AGENT_2026_8_1 = "node";

    /** 2026.10.1 이 보내는 본문. 2026.8.1 본문에 runtimes 칸이 더해졌다. */
    private static final String BODY_2026_10_1 = """
            {"deviceId":"%s","records":%s,"runtimes":[
             {"date":"2026-09-01","source":"claude","model":"claude-sonnet-4-5","runtimeMs":35000},
             {"date":"2026-09-01","source":"claude","model":"claude-opus-4-1","runtimeMs":9000},
             {"date":"2026-09-02","source":"claude","model":"claude-opus-4-1","runtimeMs":190000},
             {"date":"2026-09-02","source":"claude","model":"claude-haiku-4-5","runtimeMs":60000},
             {"date":"2026-09-03","source":"claude","model":"claude-sonnet-4-5","runtimeMs":80000},
             {"date":"2026-09-03","source":"claude","model":"claude-opus-4-1","runtimeMs":15000}]}""".formatted(DEVICE_ID, RECORDS_JSON);

    private static final String USER_AGENT_2026_10_1 = "tokenphage-cli/2026.10.1 (darwin; arm64) node/22.17.1";

    private static final List<TokenRow> EXPECTED_TOKEN_ROWS = List.of(
            new TokenRow("2026-09-01", "claude-sonnet-4-5", 200, 550, 18_500, 500),
            new TokenRow("2026-09-01", "claude-opus-4-1", 300, 650, 12_000, 800),
            new TokenRow("2026-09-02", "claude-opus-4-1", 650, 1_020, 31_000, 1_200),
            new TokenRow("2026-09-02", "claude-haiku-4-5", 160, 240, 6_200, 400),
            new TokenRow("2026-09-03", "claude-sonnet-4-5", 210, 800, 14_600, 900),
            new TokenRow("2026-09-03", "claude-opus-4-1", 500, 260, 20_000, 1_500));

    private static final List<RuntimeRow> EXPECTED_RUNTIME_ROWS = List.of(
            new RuntimeRow("2026-09-01", "claude", "claude-sonnet-4-5", 35_000L),
            new RuntimeRow("2026-09-01", "claude", "claude-opus-4-1", 9_000L),
            new RuntimeRow("2026-09-02", "claude", "claude-opus-4-1", 190_000L),
            new RuntimeRow("2026-09-02", "claude", "claude-haiku-4-5", 60_000L),
            new RuntimeRow("2026-09-03", "claude", "claude-sonnet-4-5", 80_000L),
            new RuntimeRow("2026-09-03", "claude", "claude-opus-4-1", 15_000L));

    private static final JsonMapper JSON = JsonMapper.builder().build();

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
    @DisplayName("2026.7.0-BETA~2026.8.1 요청(runtimes 칸 없음) → 200, 0건 결과 본문, 토큰 행 저장, 실행시간 표는 그대로")
    void 하위호환_2026_8_1요청_200과0건결과본문과토큰저장() {
        // given
        String body = BODY_2026_8_1;

        // when
        ResponseEntity<String> res = send(body, USER_AGENT_2026_8_1);

        // then: 옛 CLI 는 본문을 무시하므로 결과 본문이 생겨도 호환된다
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JSON.readTree(res.getBody())).isEqualTo(JSON.readTree("{\"runtimes\":{\"accepted\":0,\"rejected\":0}}"));
        assertThat(tokenRows()).containsExactlyInAnyOrderElementsOf(EXPECTED_TOKEN_ROWS);
        assertThat(runtimeRows()).isEmpty();
    }

    @Test
    @DisplayName("2026.10.1 요청(runtimes 칸 있음) → 200, 결과 본문, 두 표가 본문과 같다")
    void 하위호환_2026_10_1요청_200결과본문과두표저장() {
        // given
        String body = BODY_2026_10_1;

        // when
        ResponseEntity<String> res = send(body, USER_AGENT_2026_10_1);

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(JSON.readTree(res.getBody())).isEqualTo(JSON.readTree("{\"runtimes\":{\"accepted\":6,\"rejected\":0}}"));
        assertThat(tokenRows()).containsExactlyInAnyOrderElementsOf(EXPECTED_TOKEN_ROWS);
        assertThat(runtimeRows()).containsExactlyInAnyOrderElementsOf(EXPECTED_RUNTIME_ROWS);
    }

    @Test
    @DisplayName("같은 기기에서 2026.10.1 요청 뒤 2026.8.1 요청이 와도 실행시간은 그대로다")
    void 하위호환_같은기기2026_10_1뒤2026_8_1요청_실행시간유지() {
        // given: 같은 맥에서 2026.10.1 이 실행시간을 보낸 뒤 2026.8.1 배치가 같은 deviceId 로 보낸 상황
        assertThat(send(BODY_2026_10_1, USER_AGENT_2026_10_1).getStatusCode()).isEqualTo(HttpStatus.OK);

        // when
        ResponseEntity<String> res = send(BODY_2026_8_1, USER_AGENT_2026_8_1);

        // then: '칸 없음'은 0 으로 덮지 않는다
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(runtimeRows()).containsExactlyInAnyOrderElementsOf(EXPECTED_RUNTIME_ROWS);
    }

    private ResponseEntity<String> send(String body, String userAgent) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        headers.setBearerAuth(jwtIssuer.issue(GITHUB_ID, USERNAME));
        return rest.exchange("http://localhost:" + port + "/api/sync", HttpMethod.POST,
                new HttpEntity<>(body, headers), String.class);
    }

    private List<TokenRow> tokenRows() {
        return jdbc.query("""
                SELECT usage_date::text AS date, model, input_tok, output_tok, cache_read_tok, cache_create_tok
                FROM daily_token_usage
                WHERE github_id = ? AND device_id = ?::uuid
                """,
                (rs, i) -> new TokenRow(rs.getString("date"), rs.getString("model"),
                        rs.getLong("input_tok"), rs.getLong("output_tok"),
                        rs.getLong("cache_read_tok"), rs.getLong("cache_create_tok")),
                GITHUB_ID, DEVICE_ID);
    }

    private List<RuntimeRow> runtimeRows() {
        return jdbc.query("""
                SELECT usage_date::text AS date, source, model, runtime_ms
                FROM daily_agent_runtime
                WHERE github_id = ? AND device_id = ?::uuid
                """,
                (rs, i) -> new RuntimeRow(rs.getString("date"), rs.getString("source"),
                        rs.getString("model"), rs.getLong("runtime_ms")),
                GITHUB_ID, DEVICE_ID);
    }

    record TokenRow(String date, String model, long inputTok, long outputTok, long cacheReadTok, long cacheCreateTok) {
    }

    record RuntimeRow(String date, String source, String model, long runtimeMs) {
    }
}
