package com.tokenphage.api.exception;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tokenphage.api.config.SecurityConfig;
import com.tokenphage.api.feature.badge.controller.BadgeController;
import com.tokenphage.api.feature.badge.exception.BadgeErrorCode;
import com.tokenphage.api.feature.badge.service.BadgeRenderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 라우팅·프로토콜 단계의 예외가 올바른 4xx로 나가는지 검증한다.
 * <p>
 * 실제 디스패치를 태워 핸들러 도달 경로를 확인한다. 컨트롤러는 라우팅을 태우기 위한 발판이다.
 */
@WebMvcTest(BadgeController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "badge.jwt-secret=webmvctest-dummy-secret-key-0123456789")
@DisplayName("GlobalExceptionHandler 디스패치 계약")
class GlobalExceptionHandlerWebMvcTest {

    private static final String USERNAME = "octocat";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BadgeRenderService badgeRenderService;

    @Nested
    @DisplayName("라우팅 미스")
    class RoutingMiss {

        @Test
        @DisplayName("라우팅미스_슬래시로끝나는배지경로_404와NOT_FOUND반환")
        void 라우팅미스_슬래시로끝나는배지경로_404와NOT_FOUND반환() throws Exception {
            // given
            // 어떤 핸들러에도 매칭되지 않아 정적 리소스 폴백으로 흘러간다

            // when
            // then
            mockMvc.perform(get("/badge/"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                    .andExpect(jsonPath("$.message").value("Not Found."));
        }

        @Test
        @DisplayName("라우팅미스_username없는배지경로_404와NOT_FOUND반환")
        void 라우팅미스_username없는배지경로_404와NOT_FOUND반환() throws Exception {
            // given

            // when
            // then
            mockMvc.perform(get("/badge"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @DisplayName("라우팅미스_username뒤슬래시_404와NOT_FOUND반환")
        void 라우팅미스_username뒤슬래시_404와NOT_FOUND반환() throws Exception {
            // given
            // /badge/{username} 은 매칭되지만 뒤에 슬래시가 붙으면 매칭되지 않는다

            // when
            // then
            mockMvc.perform(get("/badge/{username}/", USERNAME))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }
    }

    @Nested
    @DisplayName("프로토콜 불일치")
    class ProtocolMismatch {

        @Test
        @DisplayName("메서드불일치_배지경로에POST_405와METHOD_NOT_ALLOWED반환")
        void 메서드불일치_배지경로에POST_405와METHOD_NOT_ALLOWED반환() throws Exception {
            // given
            // 경로는 매칭되지만 GET만 등록돼 있다

            // when
            // then
            mockMvc.perform(post("/badge/{username}", USERNAME))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
        }
    }

    @Nested
    @DisplayName("기존 계약 회귀 방지")
    class NoRegression {

        @Test
        @DisplayName("비즈니스예외_미가입사용자_기존BADGE_001계약유지")
        void 비즈니스예외_미가입사용자_기존BADGE_001계약유지() throws Exception {
            // given
            // 새 핸들러가 handleAppException 을 가리면 안 된다
            given(badgeRenderService.getSvg(USERNAME, "gpu", "light"))
                    .willThrow(new AppException(BadgeErrorCode.USER_NOT_FOUND));

            // when
            // then
            mockMvc.perform(get("/badge/{username}", USERNAME))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("BADGE_001"))
                    .andExpect(jsonPath("$.message").value("User not found."));
        }
    }
}
