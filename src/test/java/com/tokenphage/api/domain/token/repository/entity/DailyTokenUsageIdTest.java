package com.tokenphage.api.domain.token.repository.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * DailyTokenUsageId(@IdClass 복합키)의 equals()/hashCode() 동치성 계약을 검증한다.
 */
class DailyTokenUsageIdTest {

    // 동일 값 비교에 사용하는 고정 기준값 (네 필드)
    private static final Long GITHUB_ID = -1L;
    private static final UUID DEVICE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDate USAGE_DATE = LocalDate.of(2026, 5, 31);
    private static final String MODEL = "claude-opus-4";

    /**
     * 생성자/세터/빌더가 없는 복합키 클래스이므로 reflection으로 네 필드를 주입해 인스턴스를 만든다.
     */
    private DailyTokenUsageId newId(Long githubId, UUID deviceId, LocalDate usageDate, String model) {
        DailyTokenUsageId id = new DailyTokenUsageId();
        ReflectionTestUtils.setField(id, "githubId", githubId);
        ReflectionTestUtils.setField(id, "deviceId", deviceId);
        ReflectionTestUtils.setField(id, "usageDate", usageDate);
        ReflectionTestUtils.setField(id, "model", model);
        return id;
    }

    /**
     * 기준값(네 필드 모두 동일)으로 채운 인스턴스를 만든다.
     */
    private DailyTokenUsageId baseId() {
        return newId(GITHUB_ID, DEVICE_ID, USAGE_DATE, MODEL);
    }

    @Nested
    @DisplayName("equals() 검증")
    class EqualsTest {

        @Test
        @DisplayName("equals_네필드모두동일_true반환")
        void equals_네필드모두동일_true반환() {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = baseId();

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("equals_자기자신비교_true반환(반사성)")
        void equals_자기자신비교_true반환() {
            // given
            DailyTokenUsageId a = baseId();

            // when
            boolean result = a.equals(a);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("equals_a와b상호비교_결과동일(대칭성)")
        void equals_a와b상호비교_결과동일() {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = baseId();

            // when
            boolean aEqualsB = a.equals(b);
            boolean bEqualsA = b.equals(a);

            // then
            assertThat(aEqualsB).isEqualTo(bEqualsA);
            assertThat(aEqualsB).isTrue();
        }

        @Test
        @DisplayName("equals_githubId만다름_false반환")
        void equals_githubId만다름_false반환() {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = newId(-999L, DEVICE_ID, USAGE_DATE, MODEL);

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("equals_deviceId만다름_false반환")
        void equals_deviceId만다름_false반환() {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = newId(
                    GITHUB_ID,
                    UUID.fromString("22222222-2222-2222-2222-222222222222"),
                    USAGE_DATE,
                    MODEL);

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("equals_usageDate만다름_false반환")
        void equals_usageDate만다름_false반환() {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = newId(GITHUB_ID, DEVICE_ID, LocalDate.of(2026, 6, 1), MODEL);

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("equals_model만다름_false반환")
        void equals_model만다름_false반환() {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = newId(GITHUB_ID, DEVICE_ID, USAGE_DATE, "claude-sonnet-4");

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("equals_null비교_false반환")
        void equals_null비교_false반환() {
            // given
            DailyTokenUsageId a = baseId();

            // when
            boolean result = a.equals(null);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("equals_다른타입비교_false반환")
        void equals_다른타입비교_false반환() {
            // given
            DailyTokenUsageId a = baseId();
            Object other = "not-an-id";

            // when
            boolean result = a.equals(other);

            // then
            assertThat(result).isFalse();
        }

        @ParameterizedTest(name = "[{index}] {0} 필드만 다르면 false")
        @ValueSource(strings = {"githubId", "deviceId", "usageDate", "model"})
        @DisplayName("equals_각필드별로하나만다름_false반환")
        void equals_각필드별로하나만다름_false반환(String differingField) {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = baseId();
            // 지정된 한 필드만 다른 값으로 덮어쓴다
            switch (differingField) {
                case "githubId" -> ReflectionTestUtils.setField(b, "githubId", -999L);
                case "deviceId" -> ReflectionTestUtils.setField(
                        b, "deviceId", UUID.fromString("33333333-3333-3333-3333-333333333333"));
                case "usageDate" -> ReflectionTestUtils.setField(b, "usageDate", LocalDate.of(2026, 12, 25));
                case "model" -> ReflectionTestUtils.setField(b, "model", "claude-haiku-4");
                default -> throw new IllegalArgumentException("Unknown field: " + differingField);
            }

            // when
            boolean result = a.equals(b);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hashCode() 검증")
    class HashCodeTest {

        @Test
        @DisplayName("hashCode_네필드모두동일_동일한해시반환")
        void hashCode_네필드모두동일_동일한해시반환() {
            // given
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = baseId();

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
            DailyTokenUsageId a = baseId();

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
            DailyTokenUsageId a = baseId();
            DailyTokenUsageId b = newId(-999L, DEVICE_ID, USAGE_DATE, "claude-sonnet-4");

            // when
            int hashA = a.hashCode();
            int hashB = b.hashCode();

            // then
            // 동치성 계약상 필수는 아니나, 서로 다른 키는 대체로 다른 해시를 가져야 분산이 좋다.
            assertThat(hashA).isNotEqualTo(hashB);
        }
    }
}
