package com.tokenphage.api.feature.badge.exception;

import com.tokenphage.api.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

/** 배지(badge) 도메인 에러 코드 */
public enum BadgeErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "BADGE_001", "User not found.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BadgeErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus getStatus()  { return status; }
    @Override public String getCode()        { return code; }
    @Override public String getMessage()     { return message; }
}
