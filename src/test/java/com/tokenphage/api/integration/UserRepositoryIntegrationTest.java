package com.tokenphage.api.integration;

import com.tokenphage.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사용자 저장 쿼리(upsert)를 실제 PostgreSQL 에 태워 검증한다.
 * <p>
 * 로그인과 sync 가 부를 때마다 updated_at 을 갱신하는 동작이 대상이다. username 이 같아도 갱신한다.
 * 테스트마다 트랜잭션을 롤백해 공유 컨테이너 DB 에 행을 남기지 않는다.
 */
@Tag("integration")
@SpringBootTest
@Transactional
@DisplayName("UserRepository 사용자 저장 쿼리")
class UserRepositoryIntegrationTest extends ContainerSupport {

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다 (99901~99914 사용 중).
    private static final long   GITHUB_ID = 99915L;
    private static final String USERNAME  = "cli_user_repo";

    /** 갱신 여부가 드러나도록 과거로 둔 시각. */
    private static final OffsetDateTime PAST = OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);

    @Autowired
    private UserRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    private void insertPastUser(String username) {
        jdbc.update("INSERT INTO users (github_id, username, created_at, updated_at) VALUES (?, ?, ?, ?)",
                GITHUB_ID, username, PAST, PAST);
    }

    private UserRow rowOf() {
        return jdbc.queryForObject("SELECT username, created_at, updated_at FROM users WHERE github_id = ?",
                (rs, i) -> new UserRow(rs.getString("username"),
                        rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                        rs.getObject("updated_at", OffsetDateTime.class).toInstant()),
                GITHUB_ID);
    }

    record UserRow(String username, Instant createdAt, Instant updatedAt) {
    }

    @Nested
    @DisplayName("upsert() - 사용자 저장")
    class Upsert {

        @Test
        @DisplayName("처음 보는 사용자면 행을 만들고 생성·갱신 시각을 같은 값으로 넣는다")
        void 사용자저장_신규_행생성과시각기록() {
            // given: users 에 행이 없다

            // when
            repository.upsert(GITHUB_ID, USERNAME);

            // then
            UserRow row = rowOf();
            assertThat(row.username()).isEqualTo(USERNAME);
            assertThat(row.createdAt()).isAfter(PAST.toInstant());
            assertThat(row.updatedAt()).isEqualTo(row.createdAt());
        }

        @Test
        @DisplayName("username이 바뀌었으면 이름을 바꾸고 갱신 시각을 새로 쓴다")
        void 사용자저장_username변경_이름교체와갱신시각변경() {
            // given
            insertPastUser("cli_user_old");

            // when
            repository.upsert(GITHUB_ID, USERNAME);

            // then: 생성 시각은 그대로다
            UserRow row = rowOf();
            assertThat(row.username()).isEqualTo(USERNAME);
            assertThat(row.updatedAt()).isAfter(PAST.toInstant());
            assertThat(row.createdAt()).isEqualTo(PAST.toInstant());
        }

        @Test
        @DisplayName("username이 같아도 갱신 시각을 새로 쓴다")
        void 사용자저장_username동일_갱신시각변경() {
            // given
            insertPastUser(USERNAME);

            // when
            repository.upsert(GITHUB_ID, USERNAME);

            // then: 로그인·sync 마다 갱신되므로 사실상 마지막 활동 시각이다
            UserRow row = rowOf();
            assertThat(row.username()).isEqualTo(USERNAME);
            assertThat(row.updatedAt()).isAfter(PAST.toInstant());
            assertThat(row.createdAt()).isEqualTo(PAST.toInstant());
        }
    }
}
