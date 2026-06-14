package com.tokenphage.api.feature.badge.svg.theme;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TokenBookScaleTest {

    private static final List<String> VERBS = List.of("갉아먹는 중", "냠냠", "꿀꺽", "와구와구", "뇸뇸");

    @Test
    @DisplayName("레벨별 대표 책 + 배수 문구를 생성한다 (문구 인덱스 0 고정)")
    void describe_perLevel() {
        assertThat(TokenBookScale.describe(1, 1_000_000L, 0)).isEqualTo("The Little Prince 50권 갉아먹는 중");
        assertThat(TokenBookScale.describe(2, 50_000_000L, 0)).isEqualTo("Harry Potter 455권 갉아먹는 중");
        assertThat(TokenBookScale.describe(3, 200_000_000L, 0)).isEqualTo("Crime and Punishment 690권 갉아먹는 중");
        assertThat(TokenBookScale.describe(4, 700_000_000L, 0)).isEqualTo("Bible 609권 갉아먹는 중");
        assertThat(TokenBookScale.describe(5, 2_000_000_000L, 0)).isEqualTo("Harry Potter series 1307권 갉아먹는 중");
    }

    @Test
    @DisplayName("verbIndex로 trailing 문구를 고정한다")
    void describe_verbIndex() {
        assertThat(TokenBookScale.describe(4, 700_000_000L, 1)).isEqualTo("Bible 609권 냠냠");
        assertThat(TokenBookScale.describe(4, 700_000_000L, 4)).isEqualTo("Bible 609권 뇸뇸");
    }

    @Test
    @DisplayName("배수 10 미만은 소수 1자리, 10 이상은 정수로 표기한다")
    void describe_decimalVsInteger() {
        assertThat(TokenBookScale.describe(1, 180_000L, 0)).isEqualTo("The Little Prince 9.0권 갉아먹는 중");
        assertThat(TokenBookScale.describe(1, 200_000L, 0)).isEqualTo("The Little Prince 10권 갉아먹는 중");
    }

    @Test
    @DisplayName("한 권 미만(배수<1)도 소수로 노출한다")
    void describe_lessThanOne() {
        assertThat(TokenBookScale.describe(1, 4_000L, 0)).isEqualTo("The Little Prince 0.2권 갉아먹는 중");
    }

    @Test
    @DisplayName("무인자 describe는 책+배수로 시작하고 trailing은 5종 중 하나다")
    void describe_randomVerb() {
        String prefix = "Bible 609권 ";
        for (int i = 0; i < 50; i++) {
            String caption = TokenBookScale.describe(4, 700_000_000L);
            assertThat(caption).startsWith(prefix);
            assertThat(VERBS).contains(caption.substring(prefix.length()));
        }
    }
}
