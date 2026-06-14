package com.tokenphage.api.feature.reset.exception;

import com.tokenphage.api.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

/** 전체 초기화(reset) 도메인 에러 코드 */
public enum ResetErrorCode implements BaseErrorCode {

    RESET_COOLDOWN(HttpStatus.TOO_MANY_REQUESTS, "RESET_001", "A full reset is allowed only once every 24 hours. Please try again later.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ResetErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus getStatus()  { return status; }
    @Override public String getCode()        { return code; }
    @Override public String getMessage()     { return message; }
}
