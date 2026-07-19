package com.tokenphage.api.feature.badge.svg.theme.card.claude;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("TokenBookScale 단위 테스트")
class TokenBookScaleTest {

    private static final List<String> VERBS = List.of("갉아먹는 중", "냠냠", "꿀꺽", "와구와구", "뇸뇸");

    @Nested
    @DisplayName("describe(level, totalTokens, verbIndex) — 결정적")
    class Deterministic {

        @ParameterizedTest
        @DisplayName("describe_레벨1~5_대표책과배수캡션")
        @CsvSource({
                "1, 1000000,    'The Little Prince 50권 갉아먹는 중'",
                "2, 50000000,   'Harry Potter 455권 갉아먹는 중'",
                "3, 200000000,  'Crime and Punishment 690권 갉아먹는 중'",
                "4, 700000000,  'Bible 609권 갉아먹는 중'",
                "5, 2000000000, 'Harry Potter series 1307권 갉아먹는 중'"
        })
        void describe_레벨1_5_대표책배수(int level, long tokens, String expected) {
            // given: 레벨 1~5, verbIndex 0 고정
            // when
            String caption = TokenBookScale.describe(level, tokens, 0);
            // then: 레벨별 대표 책 + 반올림 배수
            assertThat(caption).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("describe_verbIndex0~4_해당trailing문구")
        @CsvSource({
                "0, 'Bible 609권 갉아먹는 중'",
                "1, 'Bible 609권 냠냠'",
                "2, 'Bible 609권 꿀꺽'",
                "3, 'Bible 609권 와구와구'",
                "4, 'Bible 609권 뇸뇸'"
        })
        void describe_verbIndex_문구선택(int verbIndex, String expected) {
            // given: level 4(Bible 609권) 고정, verbIndex 0~4
            // when
            String caption = TokenBookScale.describe(4, 700_000_000L, verbIndex);
            // then: 인덱스에 해당하는 trailing 문구
            assertThat(caption).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("describe_배수10경계_소수또는정수표기")
        @CsvSource({
                "190000, 'The Little Prince 9.5권 갉아먹는 중'",   // <10 → 소수 1자리
                "200000, 'The Little Prince 10권 갉아먹는 중'",    // =10(경계) → 정수
                "4000,   'The Little Prince 0.2권 갉아먹는 중'",   // <1 → 소수
                "0,      'The Little Prince 0.0권 갉아먹는 중'"    // 0 → 0.0
        })
        void describe_배수경계_표기(long tokens, String expected) {
            // given: level 1(The Little Prince, 20,000 토큰), 배수 경계값들
            // when
            String caption = TokenBookScale.describe(1, tokens, 0);
            // then: 배수 <10은 소수 1자리, ≥10은 정수
            assertThat(caption).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("describe_level범위밖_예외없이_최근접레벨클램프")
        @CsvSource({
                "0,           1000000,    'The Little Prince 50권 갉아먹는 중'",     // 0 → level 1
                "-100,        1000000,    'The Little Prince 50권 갉아먹는 중'",     // 음수 → level 1
                "-2147483648, 1000000,    'The Little Prince 50권 갉아먹는 중'",     // Integer.MIN → level 1
                "6,           2000000000, 'Harry Potter series 1307권 갉아먹는 중'", // 6 → level 5
                "2147483647,  2000000000, 'Harry Potter series 1307권 갉아먹는 중'"  // Integer.MAX → level 5
        })
        void describe_level범위밖_클램프(int level, long tokens, String expected) {
            // given: 유효 범위(1~5) 밖 level (NPE/AIOOBE 유발 가능 값들)
            // when
            String caption = TokenBookScale.describe(level, tokens, 0);
            // then: 예외 없이 가장 가까운 유효 레벨로 폴백
            assertThat(caption).isEqualTo(expected);
        }

        @Test
        @DisplayName("describe_level범위밖_예외를던지지않는다")
        void describe_level범위밖_예외없음() {
            // given // when // then: 극단 level도 throw 없이 반환
            assertThatCode(() -> {
                TokenBookScale.describe(0, 1_000L, 0);
                TokenBookScale.describe(-1, 1_000L, 0);
                TokenBookScale.describe(999, 1_000L, 0);
                TokenBookScale.describe(Integer.MIN_VALUE, 1_000L, 0);
                TokenBookScale.describe(Integer.MAX_VALUE, 1_000L, 0);
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("describe(level, totalTokens) — 랜덤 trailing 문구")
    class RandomVerb {

        @Test
        @DisplayName("describe_무인자_책배수prefix고정_trailing5종중하나")
        void describe_무인자_trailing5종() {
            // given: level 4, 700M → "Bible 609권 " prefix
            String prefix = "Bible 609권 ";
            for (int i = 0; i < 50; i++) {
                // when
                String caption = TokenBookScale.describe(4, 700_000_000L);
                // then: prefix 고정 + trailing은 VERBS 5종 중 하나
                assertThat(caption).startsWith(prefix);
                assertThat(VERBS).contains(caption.substring(prefix.length()));
            }
        }

        @Test
        @DisplayName("describe_무인자_level범위밖_예외없이_폴백")
        void describe_무인자_범위밖_폴백() {
            // given: 범위 밖 level(0) — 랜덤 오버로드도 클램프 경로를 타야 한다
            for (int i = 0; i < 20; i++) {
                // when // then
                String caption = TokenBookScale.describe(0, 1_000_000L);
                assertThat(caption).startsWith("The Little Prince 50권 ");
                assertThat(VERBS).contains(caption.substring("The Little Prince 50권 ".length()));
            }
        }
    }
}
