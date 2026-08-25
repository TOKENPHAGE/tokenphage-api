package com.tokenphage.api.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenphage.api.domain.badge.repository.BadgeSnapshotRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findPayload 의 native 쿼리와 V6 DB 함수 badge_compact_number 를 실제 PostgreSQL 에 태워 검증한다.
 * <p>
 * 컴파일러가 못 잡는 계약이 대상이다 — 복합 PK WHERE 절, payload::text 캐스팅, jsonb 왕복,
 * 축약 표시 문자열. 컨테이너 DB 를 공유하고 롤백이 없어 AfterEach 에서 FK 역순으로 정리한다.
 */
@Tag("integration")
@SpringBootTest
@DisplayName("BadgeSnapshotRepository native 쿼리와 badge_compact_number")
class BadgeSnapshotRepositoryIntegrationTest extends ContainerSupport {

    /** V7 이 시드하는 자격 필요 배지 코드. 시드 행이므로 절대 삭제하지 않는다. */
    private static final String BADGE_CODE = "beta-tester";
    /** V5 가 시드하는 공개 배지 코드 — 복합키의 badge_code 축 격리 검증용. */
    private static final String OTHER_BADGE_CODE = "gpu";

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다
    // (Sync 99901, Reset 99902, BadgeGrant 99903·99904, Auth 99905).
    private static final long   GITHUB_ID       = 99906L;
    private static final String USERNAME        = "cli_snapshot_test";
    private static final long   OTHER_GITHUB_ID = 99907L;
    private static final String OTHER_USERNAME  = "cli_snapshot_other";

    /** readTree 전용이라 모듈 설정이 필요 없어 스프링 빈 대신 정적으로 둔다. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * V7 적재와 같은 7키 스키마. period 의 en dash(–)로 비ASCII 왕복까지 검증한다.
     * jsonb 가 키 순서·공백을 정규화하므로 일부러 비정규 형태(줄바꿈·들여쓰기)로 둔다.
     */
    private static final String PAYLOAD_JSON = """
            {"signupRank": 7,
             "period":      "2026.07 – 2026.08",
             "syncsRun":    "42",
             "badgeServed": "1.2K",
             "tokensAdded": "324T",
             "isClaudeUse": true,
             "isGptUse":    false}
            """;

    @Autowired
    private BadgeSnapshotRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        // FK 역순: badge_snapshot → users. badge_catalog 는 시드(V5 gpu, V7 beta-tester)라 건드리지 않는다.
        jdbc.update("DELETE FROM badge_snapshot WHERE github_id IN (?, ?)", GITHUB_ID, OTHER_GITHUB_ID);
        jdbc.update("DELETE FROM users WHERE github_id IN (?, ?)", GITHUB_ID, OTHER_GITHUB_ID);
    }

    private void insertUser(long githubId, String username) {
        jdbc.update("INSERT INTO users (github_id, username) VALUES (?, ?)", githubId, username);
    }

    private void insertSnapshot(long githubId, String badgeCode, String payloadJson) {
        // ?::jsonb 캐스팅 필수 — 없으면 character varying 타입 에러로 INSERT 자체가 실패한다.
        jdbc.update("""
            INSERT INTO badge_snapshot (github_id, badge_code, payload)
            VALUES (?, ?, ?::jsonb)
            """, githubId, badgeCode, payloadJson);
    }

    /** 격리 테스트용 최소 payload. findPayload 는 스키마를 검증하지 않으므로 marker 하나면 충분하다. */
    private static String markerPayload(int rank) {
        return "{\"signupRank\": %d}".formatted(rank);
    }

    @Nested
    @DisplayName("findPayload() - 조회 성공과 jsonb 왕복")
    class RoundTrip {

        @Test
        @DisplayName("저장한 payload가 값·타입 그대로 조회된다")
        void 페이로드조회_스냅샷존재_JSON왕복일치() throws Exception {
            // given: V7 스키마와 동일한 7키 payload
            insertUser(GITHUB_ID, USERNAME);
            insertSnapshot(GITHUB_ID, BADGE_CODE, PAYLOAD_JSON);

            // when
            String found = repository.findPayload(GITHUB_ID, BADGE_CODE);

            // then: jsonb는 저장 시 키 순서·공백을 정규화하므로 원문 문자열 비교 대신 트리 동등성으로 검증한다
            assertThat(found).isNotNull();
            assertThat(MAPPER.readTree(found)).isEqualTo(MAPPER.readTree(PAYLOAD_JSON));
        }
    }

    @Nested
    @DisplayName("findPayload() - 행 없음과 복합키 격리")
    class MissingAndIsolation {

        @Test
        @DisplayName("스냅샷 행이 없으면 null이다")
        void 페이로드조회_스냅샷없음_null반환() {
            // given: 사용자만 있고 스냅샷은 없다 — 자격은 있는데 적재가 누락된 경로.
            //        V7의 스냅샷 적재는 컨테이너 기동 직후(users 0행)에 끝났으므로 실행 순서와 무관하다.
            insertUser(GITHUB_ID, USERNAME);

            // when
            String found = repository.findPayload(GITHUB_ID, BADGE_CODE);

            // then: WHERE 복합 PK가 매칭되지 않아 행이 없다
            assertThat(found).isNull();
        }

        @Test
        @DisplayName("같은 사용자의 다른 배지 스냅샷과 섞이지 않는다")
        void 페이로드조회_같은사용자_다른배지코드_해당배지행만반환() throws Exception {
            // given: 한 사용자가 배지 코드 2개의 스냅샷을 보유한다
            insertUser(GITHUB_ID, USERNAME);
            insertSnapshot(GITHUB_ID, BADGE_CODE, markerPayload(1));
            insertSnapshot(GITHUB_ID, OTHER_BADGE_CODE, markerPayload(2));

            // when
            String betaTester = repository.findPayload(GITHUB_ID, BADGE_CODE);
            String gpu = repository.findPayload(GITHUB_ID, OTHER_BADGE_CODE);

            // then: bs.badge_code = :badgeCode 조건이 행을 구분한다
            assertThat(MAPPER.readTree(betaTester).path("signupRank").asInt()).isEqualTo(1);
            assertThat(MAPPER.readTree(gpu).path("signupRank").asInt()).isEqualTo(2);
        }

        @Test
        @DisplayName("같은 배지의 타인 스냅샷과 섞이지 않는다")
        void 페이로드조회_같은배지_다른사용자_본인행만반환() throws Exception {
            // given: 두 사용자가 같은 배지의 스냅샷을 각자 보유한다
            insertUser(GITHUB_ID, USERNAME);
            insertUser(OTHER_GITHUB_ID, OTHER_USERNAME);
            insertSnapshot(GITHUB_ID, BADGE_CODE, markerPayload(1));
            insertSnapshot(OTHER_GITHUB_ID, BADGE_CODE, markerPayload(2));

            // when
            String mine = repository.findPayload(GITHUB_ID, BADGE_CODE);
            String others = repository.findPayload(OTHER_GITHUB_ID, BADGE_CODE);

            // then: bs.github_id = :githubId 조건이 타인의 행을 걸러낸다
            assertThat(MAPPER.readTree(mine).path("signupRank").asInt()).isEqualTo(1);
            assertThat(MAPPER.readTree(others).path("signupRank").asInt()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("badge_compact_number() - 표시 문자열 계약")
    class CompactNumber {

        @ParameterizedTest(name = "[{index}] {0} → \"{1}\"")
        @CsvSource({
                "0,               0",
                "128,             128",
                "999,             999",
                "1000,            1K",
                "4200,            4.2K",
                "1000000,         1M",
                "2500000000,      2.5B",
                "324000000000000, 324T"
        })
        @DisplayName("단위별 대표값과 임계 경계(999/1000)가 기대 표기로 축약된다")
        void 축약표시_단위별대표값과경계_기대문자열반환(long input, String expected) {
            // given: V6가 만든 함수는 V7 적재와 다음 고정값 배지가 공유하는 코드 밖 약속이다

            // when: Long 바인딩 = bigint 시그니처 계약 (sum()::numeric 타입 불일치 전례)
            String actual = jdbc.queryForObject("SELECT badge_compact_number(?)", String.class, input);

            // then: 소수 첫째 자리 유지, .0은 제거 (1000 → "1.0K"가 아니라 "1K")
            assertThat(actual).isEqualTo(expected);
        }
    }
}
