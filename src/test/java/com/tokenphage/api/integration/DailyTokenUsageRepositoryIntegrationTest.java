package com.tokenphage.api.integration;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.token.repository.projection.CacheTokenSum;
import com.tokenphage.api.domain.token.repository.projection.DailyUsageRow;
import com.tokenphage.api.domain.token.repository.projection.ModelUsageRow;
import org.junit.jupiter.api.AfterEach;
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
import static org.assertj.core.api.Assertions.tuple;

/**
 * 사용량 저장(upsert)과 집계 쿼리(누적·일별·모델별·캐시)를 실제 PostgreSQL 에 태워 검증한다.
 * <p>
 * 사용량 = input + cache_create + output (cache_read 제외). 새 입력이 input·cache_create 에 어떻게 나뉘든
 * (Claude는 cache_create 중심, Codex는 input 중심) 같은 작업은 같은 값이어야 한다.
 * 컨테이너 DB 를 공유하고 롤백이 없어 AfterEach 에서 FK 역순으로 정리한다.
 */
@Tag("integration")
@SpringBootTest
@DisplayName("DailyTokenUsageRepository 사용량 저장·집계 쿼리")
class DailyTokenUsageRepositoryIntegrationTest extends ContainerSupport {

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다 (99901~99907 사용 중).
    private static final long      GITHUB_ID = 99908L;
    private static final String    USERNAME  = "cli_usage_test";
    private static final UUID      DEVICE_ID = UUID.fromString("e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55");
    private static final LocalDate DAY1      = LocalDate.of(2026, 9, 1);
    private static final LocalDate DAY2      = LocalDate.of(2026, 9, 2);

    @Autowired
    private DailyTokenUsageRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.update("INSERT INTO users (github_id, username) VALUES (?, ?)", GITHUB_ID, USERNAME);
    }

    @AfterEach
    void cleanUp() {
        // FK 역순: daily_token_usage → users
        jdbc.update("DELETE FROM daily_token_usage WHERE github_id = ?", GITHUB_ID);
        jdbc.update("DELETE FROM users WHERE github_id = ?", GITHUB_ID);
    }

    private void insertUsage(LocalDate date, String model, long input, long output, long cacheRead, long cacheCreate) {
        jdbc.update("""
            INSERT INTO daily_token_usage
                (github_id, device_id, usage_date, model, input_tok, output_tok, cache_read_tok, cache_create_tok)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """, GITHUB_ID, DEVICE_ID, date, model, input, output, cacheRead, cacheCreate);
    }

    private List<UsageRow> rowsOf() {
        return jdbc.query("""
                SELECT device_id, usage_date, model, input_tok, output_tok, cache_read_tok, cache_create_tok
                FROM daily_token_usage WHERE github_id = ?
                """,
                (rs, i) -> new UsageRow(rs.getObject("device_id", UUID.class), rs.getObject("usage_date", LocalDate.class),
                        rs.getString("model"), rs.getLong("input_tok"), rs.getLong("output_tok"),
                        rs.getLong("cache_read_tok"), rs.getLong("cache_create_tok")),
                GITHUB_ID);
    }

    record UsageRow(UUID deviceId, LocalDate usageDate, String model,
                    long inputTok, long outputTok, long cacheReadTok, long cacheCreateTok) {
    }

    @Nested
    @Transactional
    @DisplayName("upsertRecord() - 덮어쓰기 저장")
    class UpsertRecord {

        private static final String MODEL = "claude-opus-5";

        @Test
        @DisplayName("새 키면 토큰 4종이 각자의 컬럼에 저장된다")
        void 저장_새키_토큰4종컬럼별저장() {
            // given: 컬럼마다 자릿수가 달라 서로 바뀌면 드러난다

            // when
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY1, MODEL, 1, 10, 100, 1_000);

            // then
            assertThat(rowsOf()).containsExactly(new UsageRow(DEVICE_ID, DAY1, MODEL, 1, 10, 100, 1_000));
        }

        @Test
        @DisplayName("같은 키로 다시 저장하면 한 줄이고 토큰 4종이 모두 새 값이다 (값이 줄어도 덮어쓴다)")
        void 저장_같은키두번_토큰4종새값으로교체() {
            // given
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY1, MODEL, 1, 10, 100, 1_000);

            // when: 늘어나는 컬럼과 줄어드는 컬럼을 섞는다
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY1, MODEL, 2, 5, 300, 400);

            // then
            assertThat(rowsOf()).containsExactly(new UsageRow(DEVICE_ID, DAY1, MODEL, 2, 5, 300, 400));
        }

        @ParameterizedTest(name = "{0}만 다름")
        @CsvSource({
                "device, a7eebc99-9c0b-4ef8-bb6d-6bb9bd380a88, 2026-09-01, claude-opus-5",
                "date,   e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55, 2026-09-02, claude-opus-5",
                "model,  e4eebc99-9c0b-4ef8-bb6d-6bb9bd380a55, 2026-09-01, gpt-5.5"
        })
        @DisplayName("기기·날짜·모델 중 하나라도 다르면 별도 행이다")
        void 저장_키한칸다름_별도행(String differs, UUID deviceId, LocalDate date, String model) {
            // given
            repository.upsertRecord(GITHUB_ID, DEVICE_ID, DAY1, MODEL, 1, 10, 100, 1_000);

            // when
            repository.upsertRecord(GITHUB_ID, deviceId, date, model, 2, 20, 200, 2_000);

            // then
            assertThat(rowsOf()).containsExactlyInAnyOrder(
                    new UsageRow(DEVICE_ID, DAY1, MODEL, 1, 10, 100, 1_000),
                    new UsageRow(deviceId, date, model, 2, 20, 200, 2_000));
        }
    }

    @Nested
    @DisplayName("sumCacheTokens() - 캐시 토큰 합계")
    class SumCacheTokens {

        @Test
        @DisplayName("cache_read·cache_create·input을 컬럼별로 따로 합산한다")
        void 캐시합계_여러행_컬럼별합산() {
            // given: 컬럼마다 자릿수가 달라 합이 어느 컬럼에서 왔는지 드러난다
            insertUsage(DAY1, "claude-opus-5", 1, 10, 100, 1_000);
            insertUsage(DAY2, "gpt-5.5",       2, 20, 200, 2_000);

            // when
            List<CacheTokenSum> rows = repository.sumCacheTokens(GITHUB_ID);

            // then: output 30은 어느 합에도 섞이지 않는다
            assertThat(rows)
                    .extracting(CacheTokenSum::getCacheRead, CacheTokenSum::getCacheCreate, CacheTokenSum::getInputTok)
                    .containsExactly(tuple(300L, 3_000L, 3L));
        }

        @Test
        @DisplayName("기록이 없으면 한 행이고 합계는 모두 null이다")
        void 캐시합계_기록없음_한행에null() {
            // given: 사용자만 있고 사용량 행은 없다

            // when
            List<CacheTokenSum> rows = repository.sumCacheTokens(GITHUB_ID);

            // then: BadgeQueryService가 getCacheRead() == null 을 "데이터 없음"으로 읽는다
            assertThat(rows)
                    .extracting(CacheTokenSum::getCacheRead, CacheTokenSum::getCacheCreate, CacheTokenSum::getInputTok)
                    .containsExactly(tuple(null, null, null));
        }
    }

    @Nested
    @DisplayName("sumTotalTokens() - 누적 사용량")
    class SumTotalTokens {

        @ParameterizedTest(name = "[{index}] {0}")
        @CsvSource({
                "Claude형(cache_create 중심), claude-opus-5,    3,  997",
                "Codex형(input 중심),         gpt-5.5,       1000,    0",
                "경계(input 0),               claude-opus-5,    0, 1000"
        })
        @DisplayName("새 입력이 input·cache_create에 어떻게 나뉘든 같은 작업이면 누적이 같다")
        void 누적_새입력분할달라도_같은값(String label, String model, long input, long cacheCreate) {
            // given: 새 입력 1,000 · 출력 150 · 캐시 재사용 50,000 인 같은 작업
            insertUsage(DAY1, model, input, 150, 50_000, cacheCreate);

            // when
            Long total = repository.sumTotalTokens(GITHUB_ID);

            // then: 새 입력 1,000 + 출력 150. 재사용분 cache_read 50,000은 빠진다
            assertThat(total).isEqualTo(1_150L);
        }
    }

    @Nested
    @DisplayName("findTop5Models() - 모델별 사용량")
    class FindTop5Models {

        @Test
        @DisplayName("#23 재현: cache_create가 큰 Claude가 input+output이 큰 gpt보다 위다")
        void 모델별_이슈23재현_cacheCreate큰Claude가1위() {
            // given: 이슈 수치를 1/1,000로 줄인 값. input + output만 보면 gpt 40,000 > claude 11,000이다
            insertUsage(DAY1, "gpt-5.5",       30_000, 10_000,   680_000,       0);
            insertUsage(DAY1, "claude-opus-5",    100, 10_900, 3_323_000, 177_000);

            // when
            List<ModelUsageRow> top = repository.findTop5Models(GITHUB_ID);

            // then: cache_create를 새 입력으로 세면 claude 188,000 > gpt 40,000
            assertThat(top)
                    .extracting(ModelUsageRow::getModel, ModelUsageRow::getTotal)
                    .containsExactly(tuple("claude-opus-5", 188_000L), tuple("gpt-5.5", 40_000L));
        }
    }

    @Nested
    @DisplayName("findDailyTotalsBetween() - 일별 사용량")
    class FindDailyTotalsBetween {

        @Test
        @DisplayName("같은 날 Claude형·Codex형 행을 같은 식으로 합산한다")
        void 일별_Claude형과Codex형같은날_합산() {
            // given: 같은 작업(각 1,150)을 두 형식으로 같은 날에 기록
            insertUsage(DAY1, "claude-opus-5",     3, 150, 50_000, 997);
            insertUsage(DAY1, "gpt-5.5",       1_000, 150, 50_000,   0);

            // when
            List<DailyUsageRow> daily = repository.findDailyTotalsBetween(GITHUB_ID, DAY1, DAY1);

            // then: 1,150 + 1,150
            assertThat(daily)
                    .extracting(DailyUsageRow::getDate, DailyUsageRow::getTotal)
                    .containsExactly(tuple("2026-09-01", 2_300L));
        }
    }

    @Nested
    @DisplayName("지표 간 불변식 - 세 쿼리가 같은 식을 쓴다")
    class CrossMetricConsistency {

        @Test
        @DisplayName("일별 합계와 모델별 합계가 누적과 같다")
        void 불변식_일별합계와모델별합계_누적과일치() {
            // given: 필드마다 자릿수를 달리해(I 1 · O 10 · CR 100 · CC 1,000의 1~4배) 합에 포함된 필드가 드러나게 한다
            insertUsage(DAY1, "claude-opus-5", 1, 10, 100, 1_000);
            insertUsage(DAY1, "gpt-5.5",       2, 20, 200, 2_000);
            insertUsage(DAY2, "claude-opus-5", 3, 30, 300, 3_000);
            insertUsage(DAY2, "gpt-5.5",       4, 40, 400, 4_000);

            // when: 같은 데이터에 세 지표 쿼리를 모두 태운다
            Long total = repository.sumTotalTokens(GITHUB_ID);
            List<DailyUsageRow> daily = repository.findDailyTotalsBetween(GITHUB_ID, DAY1, DAY2);
            List<ModelUsageRow> models = repository.findTop5Models(GITHUB_ID);

            // then: 일별 3,033 + 7,077 = 모델별 6,066 + 4,044 = 누적 10,110 (I 10 + CC 10,000 + O 100)
            assertThat(total).isEqualTo(10_110L);
            assertThat(daily).extracting(DailyUsageRow::getTotal).containsExactly(3_033L, 7_077L);
            assertThat(models).extracting(ModelUsageRow::getTotal).containsExactly(6_066L, 4_044L);
        }
    }
}
