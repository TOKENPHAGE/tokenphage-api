package com.tokenphage.api.feature.badge.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tokenphage.api.audit.AuditOutcome;
import com.tokenphage.api.config.SecurityConfig;
import com.tokenphage.api.feature.badge.dto.response.BadgeSvgResponse;
import com.tokenphage.api.feature.badge.service.BadgeRenderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * BadgeController의 HTTP 계약을 검증한다.
 * <p>
 * 자격 거부는 200으로 응답해 상태코드로 구분되지 않는다. 컨트롤러가 AuditOutcome을 요청 속성에
 * 남기는지 확인한다 — 빠지면 거부가 success로 기록된다.
 * <p>
 * SecurityConfig를 함께 올려 /badge/** 의 permitAll까지 검증한다. 서비스는 mock이라 DB·Redis는 쓰지 않는다.
 */
@WebMvcTest(BadgeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "badge.jwt-secret=webmvctest-dummy-secret-key-0123456789")
class BadgeControllerTest {

    private static final String USERNAME = "octocat";
    private static final String SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\"><rect/></svg>";
    private static final String LOCKED_SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\">LOCKED BADGE</svg>";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BadgeRenderService badgeRenderService;

    @Nested
    @DisplayName("감사 결과 기록")
    class AuditOutcomeTest {

        @Test
        @DisplayName("배지조회_자격없음_요청속성에거부값설정")
        void 배지조회_자격없음_요청속성에거부값설정() throws Exception {
            // given
            // 키와 값 모두 AuditOutcome에서 가져온다. 리터럴을 다시 적으면 복제가 늘어난다.
            given(badgeRenderService.getSvg(USERNAME, "gpu", "light"))
                    .willReturn(new BadgeSvgResponse(LOCKED_SVG, false));

            // when
            // then
            mockMvc.perform(get("/badge/{username}", USERNAME))
                    .andExpect(status().isOk())
                    .andExpect(request().attribute(
                            AuditOutcome.ATTRIBUTE_KEY, AuditOutcome.BADGE_GRANT_DENIED));
        }

        @Test
        @DisplayName("배지조회_자격있음_요청속성미설정")
        void 배지조회_자격있음_요청속성미설정() throws Exception {
            // given
            // 정상 요청이 거부로 기록되면 안 된다.
            given(badgeRenderService.getSvg(USERNAME, "gpu", "light"))
                    .willReturn(new BadgeSvgResponse(SVG, true));

            // when
            // then
            mockMvc.perform(get("/badge/{username}", USERNAME))
                    .andExpect(status().isOk())
                    .andExpect(request().attribute(
                            AuditOutcome.ATTRIBUTE_KEY, nullValue()));
        }
    }

    @Nested
    @DisplayName("응답 계약")
    class ResponseTest {

        @Test
        @DisplayName("배지조회_자격없음_상태200과잠금SVG반환")
        void 배지조회_자격없음_상태200과잠금SVG반환() throws Exception {
            // given
            // 4xx로 응답하면 README에서 깨진 이미지로 보인다.
            given(badgeRenderService.getSvg(USERNAME, "gpu", "light"))
                    .willReturn(new BadgeSvgResponse(LOCKED_SVG, false));

            // when
            // then
            mockMvc.perform(get("/badge/{username}", USERNAME))
                    .andExpect(status().isOk())
                    .andExpect(content().string(LOCKED_SVG));
        }

        @Test
        @DisplayName("배지조회_정상_svg타입과no캐시헤더반환")
        void 배지조회_정상_svg타입과no캐시헤더반환() throws Exception {
            // given
            given(badgeRenderService.getSvg(USERNAME, "gpu", "light"))
                    .willReturn(new BadgeSvgResponse(SVG, true));

            // when
            // then
            mockMvc.perform(get("/badge/{username}", USERNAME))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.valueOf("image/svg+xml")))
                    .andExpect(header().string("Cache-Control", "no-cache, max-age=0"));
        }

        @Test
        @DisplayName("배지조회_theme과mode미지정_기본값으로서비스호출")
        void 배지조회_theme과mode미지정_기본값으로서비스호출() throws Exception {
            // given
            // @RequestParam(defaultValue) 바인딩은 컨트롤러를 직접 호출해서는 검증되지 않는다.
            given(badgeRenderService.getSvg(anyString(), anyString(), anyString()))
                    .willReturn(new BadgeSvgResponse(SVG, true));
            ArgumentCaptor<String> themeCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> modeCaptor = ArgumentCaptor.forClass(String.class);

            // when
            mockMvc.perform(get("/badge/{username}", USERNAME))
                    .andExpect(status().isOk());

            // then
            then(badgeRenderService).should().getSvg(eq(USERNAME), themeCaptor.capture(), modeCaptor.capture());
            assertThat(themeCaptor.getValue()).isEqualTo("gpu");
            assertThat(modeCaptor.getValue()).isEqualTo("light");
        }

        @Test
        @DisplayName("배지조회_theme과mode지정_그값으로서비스호출")
        void 배지조회_theme과mode지정_그값으로서비스호출() throws Exception {
            // given
            given(badgeRenderService.getSvg(anyString(), anyString(), anyString()))
                    .willReturn(new BadgeSvgResponse(SVG, true));

            // when
            mockMvc.perform(get("/badge/{username}", USERNAME)
                            .param("theme", "grass-claude")
                            .param("mode", "dark"))
                    .andExpect(status().isOk());

            // then
            then(badgeRenderService).should().getSvg(USERNAME, "grass-claude", "dark");
        }

        @Test
        @DisplayName("배지조회_악센트모드지정_그값으로서비스호출")
        void 배지조회_악센트모드지정_그값으로서비스호출() throws Exception {
            // given
            // 컨트롤러는 mode를 해석하지 않는다 — 악센트 값도 원문 그대로 서비스에 넘긴다.
            given(badgeRenderService.getSvg(anyString(), anyString(), anyString()))
                    .willReturn(new BadgeSvgResponse(SVG, true));

            // when
            mockMvc.perform(get("/badge/{username}", USERNAME)
                            .param("theme", "beta-tester")
                            .param("mode", "green"))
                    .andExpect(status().isOk());

            // then
            then(badgeRenderService).should().getSvg(USERNAME, "beta-tester", "green");
        }
    }

    @Nested
    @DisplayName("접근 제어")
    class AccessTest {

        @Test
        @DisplayName("배지조회_인증없음_200반환")
        void 배지조회_인증없음_200반환() throws Exception {
            // given
            // README에 박히는 공개 이미지라 인증을 요구하면 안 된다(SecurityConfig의 permitAll).
            given(badgeRenderService.getSvg(anyString(), anyString(), anyString()))
                    .willReturn(new BadgeSvgResponse(SVG, true));

            // when
            // then
            mockMvc.perform(get("/badge/{username}", USERNAME))
                    .andExpect(status().isOk());
        }
    }
}
