package com.tokenphage.api.integration;

import com.tokenphage.api.domain.runtime.repository.DailyAgentRuntimeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실행시간 저장소의 네이티브 SQL(덮어쓰기, 행 분리, 삭제)을 실제 PostgreSQL 에 태워 검증한다.
 * <p>
 * 테스트마다 트랜잭션을 롤백해 공유 컨테이너 DB 에 행을 남기지 않는다.
 */
@Tag("integration")
@SpringBootTest
@Transactional
@DisplayName("DailyAgentRuntimeRepository 네이티브 SQL")
class DailyAgentRuntimeRepositoryIntegrationTest extends ContainerSupport {

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다 (99901~99909 사용 중).
    private static final long      GITHUB_ID       = 99910L;
    private static final long      OTHER_GITHUB_ID = 99911L;
    private static final UUID      DEVICE_ID       = UUID.fromString("f5eebc99-9c0b-4ef8-bb6d-6bb9bd380a66");
    private static final UUID      OTHER_DEVICE_ID = UUID.fromString("a6eebc99-9c0b-4ef8-bb6d-6bb9bd380a77");
    private static final LocalDate DAY             = LocalDate.of(2026, 10, 9);
    private static final String    SOURCE          = "claude";
    private static final String    MODEL           = "claude-opus-4-1";

    @Autowired
    private DailyAgentRuntimeRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (github_id, username) VALUES (?, ?)", GITHUB_ID, "cli_runtime_repo");
        jdbc.update("INSERT INTO users (github_id, username) VALUES (?, ?)", OTHER_GITHUB_ID, "cli_runtime_other");
    }

    private List<RuntimeRow> rowsOf(long githubId) {
        return jdbc.query("""
                SELECT device_id, usage_date, source, model, runtime_ms
                FROM daily_agent_runtime WHERE github_id = ?
                """,
                (rs, i) -> new RuntimeRow(rs.getObject("device_id", UUID.class), rs.getObject("usage_date", LocalDate.class),
                        rs.getString("source"), rs.getString("model"), rs.getLong("runtime_ms")),
                githubId);
    }

    record RuntimeRow(UUID deviceId, LocalDate usageDate, String source, String model, long runtimeMs) {
    }

    @Nested
    @DisplayName("upsertRecord() - 덮어쓰기 저장")
    class UpsertRecord {

        @Test
        @DisplayName("새 키면 한 줄을 저장한다")
        void 저장_새키_한줄저장() {
            // given
            long runtimeMs = 7_200_000L;

            // when
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY, SOURCE, MODEL, runtimeMs);

            // then
            assertThat(rowsOf(GITHUB_ID)).containsExactly(new RuntimeRow(DEVICE_ID, DAY, SOURCE, MODEL, runtimeMs));
        }

        @ParameterizedTest(name = "{0}ms → {1}ms")
        @CsvSource({"3600000, 7200000", "7200000, 3600000"})
        @DisplayName("같은 키로 두 번 저장하면 한 줄이고 나중 값이다 (값이 줄어도 덮어쓴다)")
        void 저장_같은키두번_한줄에나중값(long first, long second) {
            // given
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY, SOURCE, MODEL, first);

            // when
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY, SOURCE, MODEL, second);

            // then
            assertThat(rowsOf(GITHUB_ID)).containsExactly(new RuntimeRow(DEVICE_ID, DAY, SOURCE, MODEL, second));
        }

        @ParameterizedTest(name = "{0}만 다름")
        @CsvSource({
                "source, opencode,        claude-opus-4-1, f5eebc99-9c0b-4ef8-bb6d-6bb9bd380a66",
                "model,  claude,          gpt-5.5,         f5eebc99-9c0b-4ef8-bb6d-6bb9bd380a66",
                "device, claude,          claude-opus-4-1, a6eebc99-9c0b-4ef8-bb6d-6bb9bd380a77"
        })
        @DisplayName("source·model·기기 중 하나라도 다르면 별도 행이다")
        void 저장_키한칸다름_별도행(String differs, String source, String model, UUID deviceId) {
            // given
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY, SOURCE, MODEL, 1_000L);

            // when
            repository.upsertRecord(GITHUB_ID, deviceId, DAY, source, model, 2_000L);

            // then
            assertThat(rowsOf(GITHUB_ID)).containsExactlyInAnyOrder(
                    new RuntimeRow(DEVICE_ID, DAY, SOURCE, MODEL, 1_000L),
                    new RuntimeRow(deviceId, DAY, source, model, 2_000L));
        }
    }

    @Nested
    @DisplayName("deleteAllByGithubId() - 사용자별 삭제")
    class DeleteAllByGithubId {

        @Test
        @DisplayName("그 사용자의 행만 지우고 다른 사용자의 행은 남긴다")
        void 삭제_사용자지정_다른사용자행유지() {
            // given
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY, SOURCE, MODEL, 1_000L);
            repository.upsertRecord(GITHUB_ID, OTHER_DEVICE_ID, DAY, "codex", "gpt-5.5", 2_000L);
            repository.upsertRecord(OTHER_GITHUB_ID, DEVICE_ID, DAY, SOURCE, MODEL, 3_000L);

            // when
            int deleted = repository.deleteAllByGithubId(GITHUB_ID);

            // then
            assertThat(deleted).isEqualTo(2);
            assertThat(rowsOf(GITHUB_ID)).isEmpty();
            assertThat(rowsOf(OTHER_GITHUB_ID)).containsExactly(new RuntimeRow(DEVICE_ID, DAY, SOURCE, MODEL, 3_000L));
        }
    }
}
