package com.tokenphage.api.domain.badge.repository.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 스냅샷 복합키(github_id + badge_code)의 equals()/hashCode()를 검증한다.
 * <p>
 * JPA가 같은 행인지 구분하는 기준이다. 어긋나면 중복 조회·중복 저장이 발생한다.
 */
class BadgeSnapshotIdTest {

    // 두 필드가 모두 같은 기준 인스턴스를 만들 때 쓰는 값
    private static final Long GITHUB_ID = -1L;
    private static final String BADGE_CODE = "beta-tester";

    private BadgeSnapshotId newId(Long githubId, String badgeCode) {
        return new BadgeSnapshotId(githubId, badgeCode);
    }

    /**
     * 기준값(두 필드 모두 동일)으로 채운 인스턴스를 만든다.
     */
    private BadgeSnapshotId baseId() {
        return newId(GITHUB_ID, BADGE_CODE);
    }

    @Nested
    @DisplayName("equals() 검증")
    class EqualsTest {

        @Test
        @DisplayName("equals_두필드모두동일_true반환")
        void equals_두필드모두동일_true반환() {
            // given
            BadgeSnapshotId a = baseId();
            BadgeSnapshotId b = baseId();

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("equals_자기자신비교_true반환")
        void equals_자기자신비교_true반환() {
            // given
            BadgeSnapshotId a = baseId();

            // when
            boolean result = a.equals(a);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("equals_a와b상호비교_결과동일")
        void equals_a와b상호비교_결과동일() {
            // given
            BadgeSnapshotId a = baseId();
            BadgeSnapshotId b = baseId();

            // when
            boolean aEqualsB = a.equals(b);
            boolean bEqualsA = b.equals(a);

            // then
            assertThat(aEqualsB).isEqualTo(bEqualsA);
            assertThat(aEqualsB).isTrue();
        }

        @ParameterizedTest(name = "[{index}] {0} 필드만 다르면 false")
        @ValueSource(strings = {"githubId", "badgeCode"})
        @DisplayName("equals_각필드별로하나만다름_false반환")
        void equals_각필드별로하나만다름_false반환(String differingField) {
            // given
            BadgeSnapshotId a = baseId();
            // 지정된 한 필드만 다른 값으로 만든다
            BadgeSnapshotId b = switch (differingField) {
                case "githubId" -> newId(-999L, BADGE_CODE);
                case "badgeCode" -> newId(GITHUB_ID, "gpu");
                default -> throw new IllegalArgumentException("Unknown field: " + differingField);
            };

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("equals_null비교_false반환")
        void equals_null비교_false반환() {
            // given
            BadgeSnapshotId a = baseId();

            // when
            boolean result = a.equals(null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("equals_다른타입비교_false반환")
        void equals_다른타입비교_false반환() {
            // given
            BadgeSnapshotId a = baseId();
            Object other = "not-an-id";

            // when
            boolean result = a.equals(other);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hashCode() 검증")
    class HashCodeTest {

        @Test
        @DisplayName("hashCode_두필드모두동일_동일한해시반환")
        void hashCode_두필드모두동일_동일한해시반환() {
            // given
            BadgeSnapshotId a = baseId();
            BadgeSnapshotId b = baseId();

            // when
            int hashA = a.hashCode();
            int hashB = b.hashCode();

            // then
            assertThat(hashA).isEqualTo(hashB);
        }

        @Test
        @DisplayName("hashCode_동일인스턴스반복호출_일관된값반환")
        void hashCode_동일인스턴스반복호출_일관된값반환() {
            // given
            BadgeSnapshotId a = baseId();

            // when
            int first = a.hashCode();
            int second = a.hashCode();

            // then
            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("hashCode_필드값다름_다른해시반환")
        void hashCode_필드값다름_다른해시반환() {
            // given
            BadgeSnapshotId a = baseId();
            BadgeSnapshotId b = newId(-999L, "gpu");

            // when
            int hashA = a.hashCode();
            int hashB = b.hashCode();

            // then
            // 필수는 아니나, 다른 키가 다른 해시를 가져야 조회 성능이 나온다.
            assertThat(hashA).isNotEqualTo(hashB);
        }
    }
}
