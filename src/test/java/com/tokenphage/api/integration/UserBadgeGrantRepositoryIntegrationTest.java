package com.tokenphage.api.integration;

import com.tokenphage.api.domain.badge.repository.UserBadgeGrantRepository;
import com.tokenphage.api.domain.badge.repository.projection.BadgeGrantRow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * findGrant 의 native 쿼리를 실제 PostgreSQL 에 태워 검증한다.
 * <p>
 * 컴파일러가 못 잡는 계약이 대상이다 — 컬럼명, 조인 조건, SQL 별칭 ↔ BadgeGrantRow getter 매핑.
 * 컨테이너 DB 를 공유하고 롤백이 없어 AfterEach 에서 FK 역순으로 정리한다.
 */
@Tag("integration")
@SpringBootTest
@DisplayName("UserBadgeGrantRepository native 쿼리")
class UserBadgeGrantRepositoryIntegrationTest extends ContainerSupport {

    /** V5 가 시드하는 공개 배지 코드 (require_grant = false). */
    private static final String PUBLIC_CODE = "gpu";
    /** badge_catalog 에 없는 코드. code CHECK 제약(^[a-z0-9][a-z0-9-]{0,39}$)은 만족한다. */
    private static final String UNKNOWN_CODE = "ghost";
    /** 테스트가 직접 넣는 자격 필요 배지. 시드에는 require_grant = true 인 배지가 없다. */
    private static final String PRIVATE_CODE = "test-private";
    private static final String DISPLAY_NAME = "Test Private";
    private static final String LOCKED_MESSAGE = "테스트 전용 잠금 문구";

    // 싱글턴 컨테이너 DB를 통합테스트 클래스들이 공유하므로 다른 클래스와 겹치지 않는 값을 쓴다
    // (Sync는 99901, Reset은 99902).
    private static final long   GITHUB_ID       = 99903L;
    private static final String USERNAME        = "cli_grant_test";
    private static final long   OTHER_GITHUB_ID = 99904L;
    private static final String OTHER_USERNAME  = "cli_grant_other";

    @Autowired
    private UserBadgeGrantRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanUp() {
        // FK 역순: user_badge_grant → users → badge_catalog
        jdbc.update("DELETE FROM user_badge_grant WHERE badge_code = ?", PRIVATE_CODE);
        jdbc.update("DELETE FROM users WHERE github_id IN (?, ?)", GITHUB_ID, OTHER_GITHUB_ID);
        jdbc.update("DELETE FROM badge_catalog WHERE code = ?", PRIVATE_CODE);
    }

    private void insertPrivateBadge() {
        jdbc.update("""
            INSERT INTO badge_catalog (code, display_name, require_grant, locked_message)
            VALUES (?, ?, true, ?)
            """, PRIVATE_CODE, DISPLAY_NAME, LOCKED_MESSAGE);
    }

    private void insertUser(long githubId, String username) {
        jdbc.update("INSERT INTO users (github_id, username) VALUES (?, ?)", githubId, username);
    }

    private void insertGrant(long githubId) {
        jdbc.update("INSERT INTO user_badge_grant (github_id, badge_code) VALUES (?, ?)",
            githubId, PRIVATE_CODE);
    }

    @Nested
    @DisplayName("findGrant() - 공개 배지 및 미등록 코드")
    class PublicAndUnknown {

        @Test
        @DisplayName("공개 배지는 자격이 없어도 granted가 true다")
        void 자격조회_공개배지_granted참() {
            // given: V5가 시드한 gpu는 require_grant = false 이고, 이 사용자는 자격이 없다

            // when
            BadgeGrantRow row = repository.findGrant(USERNAME, PUBLIC_CODE);

            // then: NOT c.require_grant 가 참이라 EXISTS 평가 없이 true
            assertThat(row).isNotNull();
            assertThat(row.getGranted()).isTrue();
            assertThat(row.getDisplayName()).isEqualTo("GPU Card");
        }

        @Test
        @DisplayName("badge_catalog에 없는 코드는 결과 자체가 null이다")
        void 자격조회_미등록코드_null반환() {
            // given: badge_catalog에 등록되지 않은 코드

            // when
            BadgeGrantRow row = repository.findGrant(USERNAME, UNKNOWN_CODE);

            // then: WHERE c.code 가 매칭되지 않아 row가 없다
            assertThat(row).isNull();
        }
    }

    @Nested
    @DisplayName("findGrant() - 자격이 필요한 배지")
    class RequireGrant {

        @Test
        @DisplayName("자격이 없으면 granted가 false다")
        void 자격조회_자격필요배지_자격없음_granted거짓() {
            // given: require_grant = true 배지만 있고 자격 행은 없다
            insertPrivateBadge();
            insertUser(GITHUB_ID, USERNAME);

            // when
            BadgeGrantRow row = repository.findGrant(USERNAME, PRIVATE_CODE);

            // then: EXISTS 서브쿼리가 실제로 평가되어 false
            assertThat(row).isNotNull();
            assertThat(row.getGranted()).isFalse();
        }

        @Test
        @DisplayName("자격을 보유하면 granted가 true다")
        void 자격조회_자격필요배지_자격보유_granted참() {
            // given: 자격 필요 배지 + 사용자 + 자격 행
            insertPrivateBadge();
            insertUser(GITHUB_ID, USERNAME);
            insertGrant(GITHUB_ID);

            // when
            BadgeGrantRow row = repository.findGrant(USERNAME, PRIVATE_CODE);

            // then: badge_catalog ⋈ user_badge_grant ⋈ users 3-테이블 조인이 성립한다
            assertThat(row).isNotNull();
            assertThat(row.getGranted()).isTrue();
        }

        @Test
        @DisplayName("타인만 자격을 보유하면 조회 대상은 granted가 false다 (경계)")
        void 자격조회_타인만자격보유_granted거짓() {
            // given: 같은 배지를 다른 사용자만 보유한다
            insertPrivateBadge();
            insertUser(GITHUB_ID, USERNAME);
            insertUser(OTHER_GITHUB_ID, OTHER_USERNAME);
            insertGrant(OTHER_GITHUB_ID);

            // when
            BadgeGrantRow row = repository.findGrant(USERNAME, PRIVATE_CODE);

            // then: u.username = :username 조인 조건이 타인의 자격을 걸러낸다
            assertThat(row).isNotNull();
            assertThat(row.getGranted()).isFalse();
        }

        @Test
        @DisplayName("SQL 별칭이 BadgeGrantRow getter에 매핑된다")
        void 자격조회_projection별칭_displayName과lockedMessage매핑() {
            // given
            insertPrivateBadge();
            insertUser(GITHUB_ID, USERNAME);

            // when
            BadgeGrantRow row = repository.findGrant(USERNAME, PRIVATE_CODE);

            // then: alias displayName/lockedMessage 가 getter로 바인딩된다 (불일치 시 null)
            assertThat(row).isNotNull();
            assertThat(row.getDisplayName()).isEqualTo(DISPLAY_NAME);
            assertThat(row.getLockedMessage()).isEqualTo(LOCKED_MESSAGE);
        }
    }
}
