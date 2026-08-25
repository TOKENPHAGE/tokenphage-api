package com.tokenphage.api.feature.badge.svg.theme.betatester;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * 스냅샷 payload JSON 파싱을 검증한다.
 * <p>
 * JSON 키는 DB에 쌓인 문자열 계약이라 컴파일러가 못 잡는다. 키 이름·결손 처리 전부를 값으로 고정한다.
 */
class BetaTesterSnapshotTest {

    private static final String FULL_JSON = """
            {"signupRank":10,"period":"2026.07 – 2026.08","syncsRun":"128",
             "badgeServed":"4.2K","tokensAdded":"324T","isClaudeUse":true,"isGptUse":true}
            """;

    @Nested
    @DisplayName("parse() 정상 경로")
    class ParseHappyPath {

        @Test
        @DisplayName("스냅샷파싱_정상JSON_필드매핑")
        void 스냅샷파싱_정상JSON_필드매핑() {
            // given

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.parse(FULL_JSON);

            // then
            assertThat(result.signupRank()).isEqualTo(10);
            assertThat(result.period()).isEqualTo("2026.07 – 2026.08");
            assertThat(result.syncsRun()).isEqualTo("128");
            assertThat(result.badgeServed()).isEqualTo("4.2K");
            assertThat(result.tokensAdded()).isEqualTo("324T");
            assertThat(result.isClaudeUse()).isTrue();
            assertThat(result.isGptUse()).isTrue();
        }

        @Test
        @DisplayName("스냅샷파싱_통계문자열_그대로유지")
        void 스냅샷파싱_통계문자열_그대로유지() {
            // given
            // 표시 포맷은 적재 시점에 고정된다. 파싱이 단위를 재변환하면 안 된다.

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.parse(FULL_JSON);

            // then
            assertThat(result.tokensAdded()).isEqualTo("324T").isNotEqualTo("324.0T");
        }

        @Test
        @DisplayName("스냅샷파싱_모르는키포함_무시하고매핑")
        void 스냅샷파싱_모르는키포함_무시하고매핑() {
            // given
            // 스키마가 앞으로 늘어도 기존 렌더가 깨지면 안 된다.
            String json = """
                    {"signupRank":3,"futureKey":"whatever","syncsRun":"7"}
                    """;

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.parse(json);

            // then
            assertThat(result.signupRank()).isEqualTo(3);
            assertThat(result.syncsRun()).isEqualTo("7");
        }
    }

    @Nested
    @DisplayName("parse() 결손·실패 처리")
    class ParseDegradedPath {

        @Test
        @DisplayName("스냅샷파싱_빈문자열_빈객체")
        void 스냅샷파싱_빈문자열_빈객체() {
            // given
            // 자격은 있는데 스냅샷 행이 없으면 조회 계층이 빈 문자열을 싣는다 (경계).

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.parse("");

            // then
            assertThat(result).isEqualTo(BetaTesterSnapshot.empty());
        }

        @Test
        @DisplayName("스냅샷파싱_null_빈객체")
        void 스냅샷파싱_null_빈객체() {
            // given

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.parse(null);

            // then
            assertThat(result).isEqualTo(BetaTesterSnapshot.empty());
        }

        @Test
        @DisplayName("스냅샷파싱_깨진JSON_빈객체와예외없음")
        void 스냅샷파싱_깨진JSON_빈객체와예외없음() {
            // given
            // 파싱 실패로 예외가 나가면 배지 이미지 자체가 깨진다.
            String broken = "{signupRank: not-json";

            // when
            // then
            assertThatCode(() -> BetaTesterSnapshot.parse(broken)).doesNotThrowAnyException();
            assertThat(BetaTesterSnapshot.parse(broken)).isEqualTo(BetaTesterSnapshot.empty());
        }

        @Test
        @DisplayName("스냅샷파싱_boolean키누락_false")
        void 스냅샷파싱_boolean키누락_false() {
            // given
            String json = """
                    {"signupRank":5,"period":"2026.07 – 2026.08","syncsRun":"1","badgeServed":"2","tokensAdded":"3"}
                    """;

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.parse(json);

            // then
            assertThat(result.isClaudeUse()).isFalse();
            assertThat(result.isGptUse()).isFalse();
        }

        @Test
        @DisplayName("스냅샷파싱_통계null_대시로정규화")
        void 스냅샷파싱_통계null_대시로정규화() {
            // given
            // 테마가 null 문자열을 만나지 않도록 파싱 단계에서 표시 가능한 값으로 접는다 (경계).
            String json = """
                    {"signupRank":5,"syncsRun":null,"badgeServed":"","tokensAdded":null}
                    """;

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.parse(json);

            // then
            assertThat(result.syncsRun()).isEqualTo("-");
            assertThat(result.badgeServed()).isEqualTo("-");
            assertThat(result.tokensAdded()).isEqualTo("-");
            assertThat(result.period()).isEmpty();
        }
    }

    @Nested
    @DisplayName("empty() 검증")
    class EmptyTest {

        @Test
        @DisplayName("빈스냅샷_생성_표시가능한기본값")
        void 빈스냅샷_생성_표시가능한기본값() {
            // given

            // when
            BetaTesterSnapshot result = BetaTesterSnapshot.empty();

            // then
            // 순번·기간은 미표시 판정값(0/""), 통계 3칸은 자리 표시 "-"
            assertThat(result.signupRank()).isZero();
            assertThat(result.period()).isEmpty();
            assertThat(result.syncsRun()).isEqualTo("-");
            assertThat(result.badgeServed()).isEqualTo("-");
            assertThat(result.tokensAdded()).isEqualTo("-");
            assertThat(result.isClaudeUse()).isFalse();
            assertThat(result.isGptUse()).isFalse();
        }
    }
}
