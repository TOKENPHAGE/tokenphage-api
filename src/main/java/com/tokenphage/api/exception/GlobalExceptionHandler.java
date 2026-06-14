package com.tokenphage.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

/**
 * 애플리케이션 전역 예외 처리기.
 * Controller의 try-catch를 제거하고 이 곳에서 일관된 형식으로 응답을 반환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외를 처리한다.
     * <p>
     * AppException의 errorCode에서 HTTP 상태와 응답 본문을 결정한다.
     *
     * @param e 비즈니스 예외
     * @return 에러 코드와 메시지를 담은 응답
     * @Since 2026-05-24
     */
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException e) {
        log.warn("Business exception: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
            .status(e.getErrorCode().getStatus())
            .body(ErrorResponse.from(e.getErrorCode()));
    }

    /**
     * 요청 본문 검증(@Valid) 실패를 처리한다.
     * <p>
     * DTO의 Bean Validation 제약 위반(필수값 누락, 형식 불일치, 음수 등)을 400 Bad Request로 매핑한다.
     * 위반한 필드와 사유를 메시지로 합쳐 클라이언트가 어떤 입력이 잘못됐는지 알 수 있게 한다.
     *
     * @param e 검증 실패 예외
     * @return 400 Bad Request 응답
     * @Since 2026-06-11
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", detail);
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_INPUT", detail));
    }

    /**
     * 읽을 수 없는 요청 본문(깨진 JSON, 타입 불일치 등)을 처리한다.
     * <p>
     * Jackson 역직렬화 단계에서 발생하는 파싱 실패를 400 Bad Request로 매핑한다.
     * 클라이언트(CLI)가 본문 형식 오류임을 식별할 수 있도록 MALFORMED_REQUEST 코드를 반환한다.
     *
     * @param e 본문 파싱 실패 예외
     * @return 400 Bad Request 응답
     * @Since 2026-06-13
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("MALFORMED_REQUEST", "Request body could not be read."));
    }

    /**
     * 형식은 맞으나 값이 유효하지 않은 인자를 처리한다. (예: 존재하지 않는 날짜 2026-13-99)
     * <p>
     * 서비스 레이어의 값 파싱(LocalDate.parse 등)에서 발생하는 예외를 400 Bad Request로 매핑한다.
     * 클라이언트(CLI)가 값 오류임을 식별할 수 있도록 INVALID_ARGUMENT 코드를 반환한다.
     *
     * @param e 값 파싱 실패 예외
     * @return 400 Bad Request 응답
     * @Since 2026-06-13
     */
    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<ErrorResponse> handleInvalidArgument(DateTimeParseException e) {
        log.warn("Invalid argument: {}", e.getMessage());
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_ARGUMENT", "Request contains an invalid value."));
    }

    /**
     * 예상치 못한 예외를 처리한다.
     * <p>
     * 내부 오류 내용을 클라이언트에 노출하지 않고 SERVER_ERROR 코드를 반환한다.
     *
     * @param e 처리되지 않은 예외
     * @return 500 Internal Server Error 응답
     * @Since 2026-05-24
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception occurred", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("SERVER_ERROR", "An internal server error occurred."));
    }
}
