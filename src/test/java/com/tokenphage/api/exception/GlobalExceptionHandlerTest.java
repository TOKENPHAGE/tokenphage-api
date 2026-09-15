package com.tokenphage.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * GlobalExceptionHandler의 예외 → HTTP 응답 매핑을 검증한다.
 */
@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("검증 실패(MethodArgumentNotValidException) → 400 INVALID_INPUT, 위반 필드 메시지 포함")
    void 검증실패_400과INVALID_INPUT반환() {
        // given
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        given(ex.getBindingResult()).willReturn(bindingResult);
        given(bindingResult.getFieldErrors()).willReturn(List.of(
            new FieldError("syncRequest", "deviceId", "deviceId must be a valid UUID")));

        // when
        ResponseEntity<ErrorResponse> response = handler.handleValidationException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().message()).contains("deviceId");
    }

    @Test
    @DisplayName("깨진/읽을 수 없는 요청 본문(HttpMessageNotReadableException) → 400 MALFORMED_REQUEST")
    void 깨진본문_400과MALFORMED_REQUEST반환() {
        // given
        HttpMessageNotReadableException ex =
            new HttpMessageNotReadableException("JSON parse error", mock(HttpInputMessage.class));

        // when
        ResponseEntity<ErrorResponse> response = handler.handleMalformedRequest(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("MALFORMED_REQUEST");
    }

    @Test
    @DisplayName("유효하지 않은 값(DateTimeParseException, 예: 2026-13-99) → 400 INVALID_ARGUMENT")
    void 유효하지않은값_400과INVALID_ARGUMENT반환() {
        // given
        DateTimeParseException ex =
            new DateTimeParseException("Invalid value for MonthOfYear", "2026-13-99", 0);

        // when
        ResponseEntity<ErrorResponse> response = handler.handleInvalidArgument(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_ARGUMENT");
    }

    @Test
    @DisplayName("라우팅 미스(NoResourceFoundException) → 404 NOT_FOUND, 예외 메시지 비노출")
    void 라우팅미스_404와NOT_FOUND반환() {
        // given
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/badge/", "badge");

        // when
        ResponseEntity<ErrorResponse> response = handler.handleWebRequestException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Not Found.");
        // 예외 메시지("No static resource ...")는 내부 구현이라 응답에 실리지 않는다
        assertThat(response.getBody().message()).doesNotContain("static resource");
    }

    @Test
    @DisplayName("메서드 불일치(HttpRequestMethodNotSupportedException) → 405 METHOD_NOT_ALLOWED")
    void 메서드불일치_405와METHOD_NOT_ALLOWED반환() {
        // given
        HttpRequestMethodNotSupportedException ex =
            new HttpRequestMethodNotSupportedException("POST", List.of("GET"));

        // when
        ResponseEntity<ErrorResponse> response = handler.handleWebRequestException(ex);

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("METHOD_NOT_ALLOWED");
        assertThat(response.getBody().message()).isEqualTo("Method Not Allowed.");
    }
}
