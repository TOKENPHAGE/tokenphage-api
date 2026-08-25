package com.tokenphage.api.domain.badge;

import static org.assertj.core.api.Assertions.assertThat;

import com.tokenphage.api.feature.badge.svg.BadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.betatester.BetaTesterBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.card.claude.CardClaudeBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.card.gpu.CardGpuBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.grass.claude.GrassClaudeBadgeTheme;
import com.tokenphage.api.feature.badge.svg.theme.locked.LockedBadgeTheme;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 배지 코드가 enum · 테마 클래스 · DB 초기 데이터(마이그레이션 시드) 세 곳에서 같은지 검증한다.
 * <p>
 * 하나라도 빠뜨리면 예외 없이 잠금 배지만 나온다.
 * 서버 없이 확인 가능하므로 운영 코드가 아닌 테스트로 잡는다.
 */
class BadgeCatalogConsistencyTest {

    /** badge_catalog 시드 INSERT를 담은 마이그레이션 목록. 새 배지의 시드 V파일을 여기에 추가한다. */
    private static final List<String> MIGRATION_PATHS = List.of(
            "/db/migration/V5__create_badge_catalog_and_grant.sql",
            "/db/migration/V6__create_badge_snapshot.sql");

    /** INSERT 문 각 행에서 첫 번째 컬럼(code)만 뽑는다. 예: ('gpu', 'GPU Card', false, NULL) */
    private static final Pattern SEED_ROW = Pattern.compile("\\(\\s*'([a-z0-9-]+)'\\s*,");

    /**
     * 코드에 등록된 테마 목록. 스프링 없이 직접 생성해 유닛 테스트로 유지한다.
     * 새 테마 추가 시 여기에도 넣어야 하며, 빠뜨리면 아래 테스트가 실패한다.
     */
    private List<BadgeTheme> themeBeans() {
        return List.of(
                new CardGpuBadgeTheme(),
                new CardClaudeBadgeTheme(),
                new GrassClaudeBadgeTheme(),
                new BetaTesterBadgeTheme(),
                new LockedBadgeTheme());
    }

    /**
     * 지정 마이그레이션에서 주석 줄을 제거한 본문을 읽는다.
     * <p>
     * 클래스패스 리소스라 클린 체크아웃·CI에서도 동작한다.
     * 주석 처리된 예시 INSERT를 세지 않도록 주석 줄을 먼저 제거한다.
     */
    private String migrationWithoutComments(String path) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(path)) {
            assertThat(in)
                    .as("마이그레이션 리소스를 찾을 수 없다: %s", path)
                    .isNotNull();
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return sql.lines()
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"));
        }
    }

    /**
     * 모든 마이그레이션의 badge_catalog 시드 INSERT 블록(세미콜론 전까지)을 병합한다.
     */
    private String seedBlocks() throws IOException {
        StringBuilder blocks = new StringBuilder();
        for (String path : MIGRATION_PATHS) {
            String sql = migrationWithoutComments(path);
            int searchFrom = 0;
            boolean found = false;
            int insertAt;
            while ((insertAt = sql.indexOf("INSERT INTO badge_catalog", searchFrom)) >= 0) {
                int end = sql.indexOf(';', insertAt);
                assertThat(end).as("시드 INSERT가 세미콜론으로 끝나지 않는다: %s", path).isNotNegative();
                blocks.append(sql, insertAt, end).append('\n');
                searchFrom = end;
                found = true;
            }
            assertThat(found).as("badge_catalog 시드 INSERT를 찾을 수 없다: %s", path).isTrue();
        }
        return blocks.toString();
    }

    /**
     * DB 초기 데이터에 등록된 배지 코드 전부를 반환한다.
     */
    private Set<String> seedCodes() throws IOException {
        Matcher matcher = SEED_ROW.matcher(seedBlocks());
        return matcher.results()
                .map(r -> r.group(1))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * DB 초기 데이터에서 특정 배지 코드의 require_grant 값을 읽는다.
     */
    private boolean requireGrantOf(String code) throws IOException {
        Pattern row = Pattern.compile(
                "\\(\\s*'" + Pattern.quote(code) + "'\\s*,[^,]+,\\s*(true|false)\\s*,");
        Matcher matcher = row.matcher(seedBlocks());
        assertThat(matcher.find()).as("초기 데이터에서 %s 행을 찾을 수 없다", code).isTrue();
        return Boolean.parseBoolean(matcher.group(1));
    }

    @Nested
    @DisplayName("세 곳의 배지 코드가 같은지")
    class ConsistencyTest {

        @Test
        @DisplayName("배지코드_enum과테마클래스_목록이같음")
        void 배지코드_enum과테마클래스_목록이같음() {
            // given
            Set<String> enumCodes = BadgeCode.allCodes();

            // when
            Set<String> beanCodes = themeBeans().stream()
                    .map(BadgeTheme::name)
                    .collect(Collectors.toUnmodifiableSet());

            // then
            assertThat(beanCodes).isEqualTo(enumCodes);
        }

        @Test
        @DisplayName("배지코드_enum과DB초기데이터_목록이같음")
        void 배지코드_enum과DB초기데이터_목록이같음() throws IOException {
            // given
            Set<String> enumCodes = BadgeCode.allCodes();

            // when
            Set<String> seeded = seedCodes();

            // then
            // DB에만 있으면 그릴 테마가 없고, DB에 없으면 자격 확인 단계에서 막힌다.
            assertThat(seeded).isEqualTo(enumCodes);
        }

        @Test
        @DisplayName("배지코드_DB초기데이터_테이블이허용하는형식")
        void 배지코드_DB초기데이터_테이블이허용하는형식() throws IOException {
            // given
            // badge_catalog.code의 형식 제한과 같아야 INSERT가 성공한다.

            // when
            Set<String> seeded = seedCodes();

            // then
            assertThat(seeded).isNotEmpty();
            assertThat(seeded).allSatisfy(code ->
                    assertThat(code).matches("^[a-z0-9][a-z0-9-]{0,39}$"));
        }
    }

    @Nested
    @DisplayName("기본 테마가 갖춰야 할 조건")
    class DefaultThemeTest {

        @Test
        @DisplayName("기본테마_gpu_테마클래스로등록됨")
        void 기본테마_gpu_테마클래스로등록됨() {
            // given
            // 알 수 없는 theme은 gpu로 대체되므로, gpu 테마가 없으면 모든 요청이 실패한다.
            String defaultCode = BadgeCode.GPU.getCode();

            // when
            Set<String> beanCodes = themeBeans().stream()
                    .map(BadgeTheme::name)
                    .collect(Collectors.toUnmodifiableSet());

            // then
            assertThat(beanCodes).contains(defaultCode);
        }

        @Test
        @DisplayName("기본테마_gpu_누구나쓸수있게등록됨")
        void 기본테마_gpu_누구나쓸수있게등록됨() throws IOException {
            // given
            // 기본 테마가 자격을 요구하면 아무도 배지를 볼 수 없다.
            String defaultCode = BadgeCode.GPU.getCode();

            // when
            Set<String> seeded = seedCodes();

            // then
            assertThat(seeded).contains(defaultCode);
            assertThat(requireGrantOf(defaultCode)).isFalse();
        }

        @Test
        @DisplayName("잠금배지_누구나쓸수있게등록됨")
        void 잠금배지_누구나쓸수있게등록됨() throws IOException {
            // given
            // 잠금 배지가 자격을 요구하면 거부 화면을 그리다 다시 거부되는 무한 반복이 된다.
            String lockedCode = BadgeCode.LOCKED.getCode();

            // when
            Set<String> seeded = seedCodes();

            // then
            assertThat(seeded).contains(lockedCode);
            assertThat(requireGrantOf(lockedCode)).isFalse();
        }
    }

    @Nested
    @DisplayName("자격 배지가 갖춰야 할 조건")
    class GrantBadgeTest {

        @Test
        @DisplayName("자격배지_betaTester_자격을요구함")
        void 자격배지_betaTester_자격을요구함() throws IOException {
            // given
            // 베타 테스터 배지는 운영자 부여 전용이다. 공개로 풀리면 스냅샷 없는 배지가 노출된다.
            String code = BadgeCode.BETA_TESTER.getCode();

            // when
            Set<String> seeded = seedCodes();

            // then
            assertThat(seeded).contains(code);
            assertThat(requireGrantOf(code)).isTrue();
        }
    }
}
