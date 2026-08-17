package com.tokenphage.api.feature.badge.svg.theme.locked;

import static org.assertj.core.api.Assertions.assertThat;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 자격이 없을 때 보여주는 잠금 안내 배지를 검증한다.
 * <p>
 * 문구가 DB(badge_catalog.locked_message)에서 오므로 특수문자 처리와 기본 문구 대체가 핵심이다.
 */
class LockedBadgeThemeTest {

    private final LockedBadgeTheme sut = new LockedBadgeTheme();

    /** 잠금 배지는 데이터를 그리지 않지만 메서드 시그니처상 필요한 빈 데이터. */
    private BadgeResponse emptyData() {
        return new BadgeResponse("example-user", 0L, List.of(), List.of(), 0.0, 0L, 0, List.of());
    }

    @Nested
    @DisplayName("테마 기본 정보")
    class BasicInfoTest {

        @Test
        @DisplayName("테마이름_조회_BadgeCode상수와일치")
        void 테마이름_조회_BadgeCode상수와일치() {
            // given

            // when
            String result = sut.name();

            // then
            assertThat(result).isEqualTo(BadgeCode.LOCKED.getCode());
            assertThat(result).isEqualTo("locked");
        }

        @Test
        @DisplayName("필요한데이터_조회_하나도없음")
        void 필요한데이터_조회_하나도없음() {
            // given
            // 잠금 안내는 집계를 그리지 않으므로 조회 서비스가 쿼리를 실행하지 않아야 한다.

            // when
            Set<BadgeDataNeed> result = sut.needs();

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("안내 문구 표시")
    class RenderTest {

        @Test
        @DisplayName("잠금배지_문구전달_SVG에표시")
        void 잠금배지_문구전달_SVG에표시() {
            // given
            String title = "Contributor";
            String message = "PR을 보내고 이 배지를 받아보세요";

            // when
            String svg = sut.render(title, message, false);

            // then
            assertThat(svg).contains(title);
            assertThat(svg).contains(message);
        }

        @ParameterizedTest(name = "[{index}] 공백 문구는 기본값으로 대체")
        @ValueSource(strings = {"", "   "})
        @DisplayName("잠금배지_문구공백_기본문구대체")
        void 잠금배지_문구공백_기본문구대체(String blank) {
            // given

            // when
            String svg = sut.render(blank, blank, false);

            // then
            assertThat(svg).contains(LockedBadgeTheme.DEFAULT_TITLE);
            assertThat(svg).contains(LockedBadgeTheme.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("잠금배지_문구NULL_기본문구대체")
        void 잠금배지_문구NULL_기본문구대체() {
            // given
            // locked_message를 등록하지 않은 배지는 null이 전달된다.

            // when
            String svg = sut.render(null, null, false);

            // then
            assertThat(svg).contains(LockedBadgeTheme.DEFAULT_TITLE);
            assertThat(svg).contains(LockedBadgeTheme.DEFAULT_MESSAGE);
        }

        @Test
        @DisplayName("잠금배지_build호출_기본문구를그린다")
        void 잠금배지_build호출_기본문구를그린다() {
            // given
            // ?theme=locked 직접 호출 경로. 어떤 배지 때문인지 알 수 없어 기본 문구를 쓴다.

            // when
            String svg = sut.build(emptyData(), false);

            // then
            assertThat(svg).contains(LockedBadgeTheme.DEFAULT_TITLE);
            assertThat(svg).contains(LockedBadgeTheme.DEFAULT_MESSAGE);
        }
    }

    @Nested
    @DisplayName("SVG 구조")
    class StructureTest {

        @ParameterizedTest(name = "[{index}] isDark={0}")
        @ValueSource(booleans = {true, false})
        @DisplayName("잠금배지_두모드_카드규격SVG반환")
        void 잠금배지_두모드_카드규격SVG반환(boolean isDark) {
            // given

            // when
            String svg = sut.render("Contributor", "안내 문구", isDark);

            // then
            assertThat(svg).startsWith("<svg");
            assertThat(svg).contains("width=\"540\"");
            assertThat(svg).contains("height=\"210\"");
            assertThat(svg.trim()).endsWith("</svg>");
        }

        @Test
        @DisplayName("잠금배지_라이트와다크_서로다른마크업")
        void 잠금배지_라이트와다크_서로다른마크업() {
            // given

            // when
            String light = sut.render("T", "M", false);
            String dark = sut.render("T", "M", true);

            // then
            assertThat(light).isNotEqualTo(dark);
        }
    }

    @Nested
    @DisplayName("특수문자 처리")
    class EscapeTest {

        @Test
        @DisplayName("잠금배지_문구에태그포함_글자로바꿔서표시")
        void 잠금배지_문구에태그포함_글자로바꿔서표시() {
            // given
            // 문구는 운영자가 DB에 직접 넣는 값이라 태그가 섞일 수 있다.
            String malicious = "<script>alert(1)</script>";

            // when
            String svg = sut.render(malicious, malicious, false);

            // then
            assertThat(svg).doesNotContain("<script>");
            assertThat(svg).contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("잠금배지_문구에앰퍼샌드포함_글자로바꿔서표시")
        void 잠금배지_문구에앰퍼샌드포함_글자로바꿔서표시() {
            // given
            String withAmp = "A & B";

            // when
            String svg = sut.render(withAmp, withAmp, false);

            // then
            assertThat(svg).contains("A &amp; B");
        }
    }
}
