package com.tokenphage.api.domain.badge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * 배지 코드 문자열이 BadgeCode enum 한 곳에만 정의되는지 검증한다.
 * <p>
 * URL ?theme=, badge_catalog.code, 테마 name()이 공유한다.
 * 값이 바뀌면 기존 배지 주소가 깨지므로 문자열을 그대로 단언해 고정한다.
 */
class BadgeCodeTest {

    @Nested
    @DisplayName("getCode() 검증")
    class GetCodeTest {

        @ParameterizedTest(name = "[{index}] {0} → \"{1}\"")
        @CsvSource({
                "GPU, gpu",
                "CLAUDE, claude",
                "GRASS_CLAUDE, grass-claude",
                "LOCKED, locked"
        })
        @DisplayName("코드조회_각상수_정해진문자열반환")
        void 코드조회_각상수_정해진문자열반환(BadgeCode badgeCode, String expected) {
            // given

            // when
            String result = badgeCode.getCode();

            // then
            assertThat(result).isEqualTo(expected);
        }

        @Test
        @DisplayName("코드조회_모든상수_DB가허용하는형식")
        void 코드조회_모든상수_DB가허용하는형식() {
            // given
            // badge_catalog.code는 소문자·숫자·하이픈 40자까지만 허용한다.

            // when
            // then
            for (BadgeCode badgeCode : BadgeCode.values()) {
                assertThat(badgeCode.getCode()).matches("^[a-z0-9][a-z0-9-]{0,39}$");
            }
        }
    }

    @Nested
    @DisplayName("allCodes() 검증")
    class AllCodesTest {

        @Test
        @DisplayName("전체코드_호출_등록된4종반환")
        void 전체코드_호출_등록된4종반환() {
            // given

            // when
            Set<String> result = BadgeCode.allCodes();

            // then
            assertThat(result).containsExactlyInAnyOrder("gpu", "claude", "grass-claude", "locked");
        }

        @Test
        @DisplayName("전체코드_상수개수와동일_중복없음")
        void 전체코드_상수개수와동일_중복없음() {
            // given
            int constantCount = BadgeCode.values().length;

            // when
            Set<String> result = BadgeCode.allCodes();

            // then
            assertThat(result).hasSize(constantCount);
        }

        @Test
        @DisplayName("전체코드_바깥에서값추가시도_변경불가라예외발생")
        void 전체코드_바깥에서값추가시도_변경불가라예외발생() {
            // given
            Set<String> result = BadgeCode.allCodes();

            // when
            // then
            assertThatThrownBy(() -> result.add("injected"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
