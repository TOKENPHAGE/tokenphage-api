package com.tokenphage.api.integration;

import com.tokenphage.api.domain.runtime.repository.DailyAgentRuntimeRepository;
import com.tokenphage.api.feature.auth.service.JwtIssuer;
import com.tokenphage.api.feature.sync.dto.request.RuntimeRecordRequest;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;

/**
 * 실행시간 저장이 DB 예외로 실패해도 같은 요청의 토큰 행은 남는지 검증한다.
 * <p>
 * 실행시간은 토큰과 다른 트랜잭션이다. 예외는 삼키지 않아 500 이 나가고, CLI 는 다음 실행 때 다시 보낸다.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("실행시간 저장 실패와 토큰 저장 분리")
class SyncRuntimeStorageFailureIntegrationTest extends ContainerSupport {

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다 (99901~99913 사용 중).
    private static final long   GITHUB_ID = 99914L;
    private static final String USERNAME  = "cli_runtime_failure";
    private static final String DEVICE_ID = "d9eebc99-9c0b-4ef8-bb6d-6bb9bd380aaa";

    @LocalServerPort
    private int port;

    @Autowired private JwtIssuer jwtIssuer;
    @Autowired private JdbcTemplate jdbc;

    @MockitoBean
    private DailyAgentRuntimeRepository runtimeRepo;

    private final TestRestTemplate rest = new TestRestTemplate();

    @AfterEach
    void cleanUp() {
        // RANDOM_PORT + 실제 HTTP라 롤백이 통하지 않아 직접 지운다 (FK 역순)
        jdbc.update("DELETE FROM daily_agent_runtime WHERE github_id = ?", GITHUB_ID);
        jdbc.update("DELETE FROM daily_token_usage WHERE github_id = ?", GITHUB_ID);
        jdbc.update("DELETE FROM users WHERE github_id = ?", GITHUB_ID);
    }

    @Test
    @DisplayName("실행시간 저장소가 예외를 던지면 500 이지만 토큰 행은 남는다")
    void 동기화_실행시간저장실패_500이고토큰행유지() {
        // given
        willThrow(new DataAccessResourceFailureException("runtime table unavailable"))
                .given(runtimeRepo).upsertRecord(anyLong(), any(UUID.class), any(LocalDate.class),
                        anyString(), anyString(), anyLong());
        SyncRequest req = new SyncRequest(DEVICE_ID,
                List.of(new TokenRecordRequest("2026-10-09", "claude-opus-4-1", 100, 200, 300, 400),
                        new TokenRecordRequest("2026-10-10", "claude-sonnet-4-5", 10, 20, 30, 40)),
                List.of(new RuntimeRecordRequest("2026-10-09", "claude", "claude-opus-4-1", 7_200_000L)));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(jwtIssuer.issue(GITHUB_ID, USERNAME));

        // when
        ResponseEntity<String> res = rest.exchange("http://localhost:" + port + "/api/sync", HttpMethod.POST,
                new HttpEntity<>(req, headers), String.class);

        // then
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM daily_token_usage WHERE github_id = ?", Long.class, GITHUB_ID))
                .isEqualTo(2L);
    }
}
