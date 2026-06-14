package com.tokenphage.api.feature.auth.exception;

import com.tokenphage.api.exception.error.BaseErrorCode;
import org.springframework.http.HttpStatus;

/** 인증(auth) 도메인 에러 코드 */
public enum AuthErrorCode implements BaseErrorCode {

    INVALID_USERNAME(HttpStatus.BAD_REQUEST,             "AUTH_001", "Invalid username."),
    CHALLENGE_EXPIRED(HttpStatus.UNAUTHORIZED,           "AUTH_002", "Challenge has expired or does not exist."),
    GIST_NOT_FOUND(HttpStatus.UNAUTHORIZED,              "AUTH_003", "Gist not found."),
    GIST_API_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "AUTH_004", "Gist API is unavailable."),
    GIST_MALFORMED(HttpStatus.UNAUTHORIZED,              "AUTH_005", "Gist response format is invalid."),
    GIST_NOT_PUBLIC(HttpStatus.UNAUTHORIZED,             "AUTH_006", "Gist is not public."),
    OWNER_MISMATCH(HttpStatus.UNAUTHORIZED,              "AUTH_007", "Gist owner does not match."),
    VERIFICATION_FILE_MISSING(HttpStatus.UNAUTHORIZED,   "AUTH_008", "Verification file is missing."),
    CHALLENGE_NOT_FOUND_IN_FILE(HttpStatus.UNAUTHORIZED, "AUTH_009", "Challenge is not included in the file.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    AuthErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override public HttpStatus getStatus()  { return status; }
    @Override public String getCode()        { return code; }
    @Override public String getMessage()     { return message; }
}
