package com.tokenphage.api.feature.badge.svg;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 색상 모드 코드 문자열(URL·캐시 키 계약)과 테마별 정규화를 검증한다.
 */
class BadgeModeTest {

    private static final Set<BadgeMode> ACCENT_MODES =
            EnumSet.of(BadgeMode.CYAN, BadgeMode.GREEN, BadgeMode.PURPLE);
    private static final Set<BadgeMode> LIGHT_DARK_MODES =
            EnumSet.of(BadgeMode.LIGHT, BadgeMode.DARK);

    @Nested
    @DisplayName("getCode() 검증")
    class GetCodeTest {

        @ParameterizedTest(name = "[{index}] {0} → \"{1}\"")
        @CsvSource({
                "LIGHT, light",
                "DARK, dark",
                "CYAN, cyan",
                "GREEN, green",
                "PURPLE, purple"
        })
        @DisplayName("코드조회_각상수_정해진문자열반환")
        void 코드조회_각상수_정해진문자열반환(BadgeMode badgeMode, String expected) {
            // given

            // when
            String result = badgeMode.getCode();

            // then
            assertThat(result).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("from() 정규화 검증")
    class FromTest {

        @Test
        @DisplayName("정규화_지원값_그대로반환")
        void 정규화_지원값_그대로반환() {
            // given
            String raw = "green";

            // when
            BadgeMode result = BadgeMode.from(raw, ACCENT_MODES, BadgeMode.CYAN);

            // then
            assertThat(result).isEqualTo(BadgeMode.GREEN);
        }

        @Test
        @DisplayName("정규화_대소문자혼합_지원값매칭")
        void 정규화_대소문자혼합_지원값매칭() {
            // given
            String raw = "GrEeN";

            // when
            BadgeMode result = BadgeMode.from(raw, ACCENT_MODES, BadgeMode.CYAN);

            // then
            assertThat(result).isEqualTo(BadgeMode.GREEN);
        }

        @Test
        @DisplayName("정규화_미지원값_폴백반환")
        void 정규화_미지원값_폴백반환() {
            // given
            // dark는 존재하는 모드지만 악센트 테마의 지원 집합에는 없다 (경계).

            // when
            BadgeMode accentResult = BadgeMode.from("dark", ACCENT_MODES, BadgeMode.CYAN);
            BadgeMode cardResult = BadgeMode.from("cyan", LIGHT_DARK_MODES, BadgeMode.LIGHT);

            // then
            assertThat(accentResult).isEqualTo(BadgeMode.CYAN);
            assertThat(cardResult).isEqualTo(BadgeMode.LIGHT);
        }

        @Test
        @DisplayName("정규화_쓰레기값_폴백반환")
        void 정규화_쓰레기값_폴백반환() {
            // given
            String raw = "garbage";

            // when
            BadgeMode result = BadgeMode.from(raw, ACCENT_MODES, BadgeMode.CYAN);

            // then
            assertThat(result).isEqualTo(BadgeMode.CYAN);
        }

        @Test
        @DisplayName("정규화_null_폴백반환")
        void 정규화_null_폴백반환() {
            // given

            // when
            BadgeMode result = BadgeMode.from(null, LIGHT_DARK_MODES, BadgeMode.LIGHT);

            // then
            assertThat(result).isEqualTo(BadgeMode.LIGHT);
        }

        @ParameterizedTest(name = "[{index}] 공백 \"{0}\"은 폴백")
        @CsvSource(value = {"''", "'   '"})
        @DisplayName("정규화_공백_폴백반환")
        void 정규화_공백_폴백반환(String blank) {
            // given

            // when
            BadgeMode result = BadgeMode.from(blank, ACCENT_MODES, BadgeMode.CYAN);

            // then
            assertThat(result).isEqualTo(BadgeMode.CYAN);
        }
    }
}
