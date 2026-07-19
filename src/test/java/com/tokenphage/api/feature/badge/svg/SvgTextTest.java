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
