package com.tokenphage.api.exception;

import com.tokenphage.api.exception.error.BaseErrorCode;

/** 전역 에러 응답 DTO */
public record ErrorResponse(String code, String message) {

    /**
     * BaseErrorCode로부터 에러 응답 DTO를 생성한다.
     *
     * @param errorCode 에러 코드와 메시지를 제공하는 에러 코드 (null 불허)
     * @return 에러 코드와 메시지를 담은 응답 DTO
     * @Since 2026-05-24
     */
    public static ErrorResponse from(BaseErrorCode errorCode) {
        return new ErrorResponse(errorCode.getCode(), errorCode.getMessage());
    }
}
