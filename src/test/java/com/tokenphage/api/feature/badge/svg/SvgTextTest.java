package com.tokenphage.api.feature.badge.svg;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SvgText 단위 테스트")
class SvgTextTest {

    @ParameterizedTest
    @DisplayName("토큰 단위 변환 — K / M / B / T")
    @CsvSource({
        "0,               0",        "999,             999",
        "1000,            1.0K",     "1500,            1.5K",
        "999000,          999.0K",   "1000000,         1.0M",
        "15430000,        15.4M",    "1000000000,      1.0B",
        "2000000000,      2.0B",     "1000000000000,   1.0T",
        "2500000000000,   2.5T"
    })
    void formatTokens_correctUnit(long tokens, String expected) {
        assertThat(SvgText.formatTokens(tokens)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "[{index}] {0} → {1}")
    @DisplayName("소수 첫째 자리에서 자르고 반올림하지 않는다 — 표시값이 실제 값을 넘지 않는다")
    @CsvSource({
        "1760000000,    1.7B",      // 반올림이면 1.8B
        "1960000000,    1.9B",      // 반올림이면 2.0B
        "999960000,     999.9M",    // 반올림이면 1000.0M (단위 넘침)
        "99960000,      99.9M",     // Lv.3 임계(100M) 직전 — 반올림이면 100.0M
        "999950,        999.9K",    // 반올림이면 1000.0K
        "999999999999,  999.9B"     // T 단위 직전 — 반올림이면 1000.0B
    })
    void 토큰표기_반올림경계값_소수첫째자리에서자름(long tokens, String expected) {
        // given: 반올림하면 한 자리가 올라가는 경계값

        // when
        String actual = SvgText.formatTokens(tokens);

        // then: 레벨은 원값으로 판정하므로, 표시가 올라가면 임계값 근처에서 숫자와 레벨이 어긋난다
        assertThat(actual).isEqualTo(expected);
    }

    @ParameterizedTest
    @DisplayName("XML 특수문자를 이스케이프한다")
    @CsvSource({
        "abc,            abc",
        "'a<script>',    'a&lt;script&gt;'",
        "'a&b',          'a&amp;b'"
    })
    void escape_xmlSpecialChars(String raw, String expected) {
        assertThat(SvgText.escape(raw)).isEqualTo(expected);
    }
}
