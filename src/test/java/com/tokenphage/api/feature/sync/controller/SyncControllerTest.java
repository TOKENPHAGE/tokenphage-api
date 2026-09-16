package com.tokenphage.api.feature.sync.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tokenphage.api.config.SecurityConfig;
import com.tokenphage.api.feature.sync.dto.request.SyncRequest;
import com.tokenphage.api.feature.sync.dto.request.TokenRecordRequest;
import com.tokenphage.api.feature.sync.service.SyncOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SyncController 의 HTTP 계약을 검증한다.
 * <p>
 * DTO 제약 자체는 SyncValidationTest 가 Validator 로 직접 검증한다. 여기서 보는 것은
 * 그 제약이 실제 요청 경로에 배선되어 400 으로 나가는지, 그리고 미인증이 401 로 막히는지다.
 * 이 배선이 끊기면 잘못된 요청이 서비스까지 내려가 500 으로 바뀐다.
 * <p>
 * SecurityConfig 를 함께 올려 /api/sync 의 authenticated() 규칙까지 검증한다.
 * 오케스트레이터는 mock 이라 DB·Redis 는 쓰지 않는다.
 */
@WebMvcTest(SyncController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "badge.jwt-secret=webmvctest-dummy-secret-key-0123456789")
@DisplayName("POST /api/sync HTTP 계약")
class SyncControllerTest {

    private static final String PATH = "/api/sync";
    private static final String VALID_UUID = "550e8400-e29b-41d4-a716-446655440000";

    @Autowired
    private MockMvc mockMvc;

    /** 슬라이스에 ObjectMapper 빈이 없어 직접 만든다. 평범한 record 직렬화라 Spring 설정이 필요 없다. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockitoBean
    private SyncOrchestrator syncOrchestrator;

    private String body(String deviceId) throws Exception {
        TokenRecordRequest record = new TokenRecordRequest("2026-06-11", "claude-opus-4", 1, 1, 0, 0);
        return OBJECT_MAPPER.writeValueAsString(new SyncRequest(deviceId, List.of(record)));
    }

    @Nested
    @DisplayName("정상 요청")
    class Success {

        @Test
        @DisplayName("동기화_인증된유효요청_200반환")
        void 동기화_인증된유효요청_200반환() throws Exception {
            // given
            String json = body(VALID_UUID);

            // when / then
            mockMvc.perform(post(PATH).with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("필드값 validation")
    class Validation {

        @Test
        @DisplayName("동기화_deviceId가UUID아님_400반환")
        void 동기화_deviceId가UUID아님_400반환() throws Exception {
            // given: @Valid 가 빠지면 이 값이 서비스까지 내려가 UUID.fromString 에서 500 이 된다
            String json = body("not-a-uuid");

            // when / then
            mockMvc.perform(post(PATH).with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("동기화_검증실패_오케스트레이터미호출")
        void 동기화_검증실패_오케스트레이터미호출() throws Exception {
            // given
            String json = body("not-a-uuid");

            // when
            mockMvc.perform(post(PATH).with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));

            // then: 검증이 서비스 앞에서 끊어야 한다
            then(syncOrchestrator).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("동기화_깨진JSON_400반환")
        void 동기화_깨진JSON_400반환() throws Exception {
            // given
            String malformed = "{\"deviceId\": ";

            // when / then
            mockMvc.perform(post(PATH).with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformed))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("인증")
    class Authentication {

        @Test
        @DisplayName("동기화_미인증_401반환")
        void 동기화_미인증_401반환() throws Exception {
            // given
            String json = body(VALID_UUID);

            // when / then: /api/sync 는 authenticated() 다
            mockMvc.perform(post(PATH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("동기화_미인증_오케스트레이터미호출")
        void 동기화_미인증_오케스트레이터미호출() throws Exception {
            // given
            String json = body(VALID_UUID);

            // when
            mockMvc.perform(post(PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json));

            // then
            then(syncOrchestrator).shouldHaveNoInteractions();
        }
    }
}
