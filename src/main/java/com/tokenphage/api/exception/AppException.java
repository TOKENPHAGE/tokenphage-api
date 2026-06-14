package com.tokenphage.api.exception;

import com.tokenphage.api.exception.error.BaseErrorCode;

/**
 * 애플리케이션 전역 비즈니스 예외.
 * 모든 feature는 이 클래스를 throw하며, 에러 코드로 상태와 메시지를 전달한다.
 */
public class AppException extends RuntimeException {

    private final BaseErrorCode errorCode;

    /**
     * 에러 코드를 기반으로 비즈니스 예외를 생성한다.
     *
     * @param errorCode HTTP 상태와 메시지를 제공하는 에러 코드 (null 불허)
     * @Since 2026-05-24
     */
    public AppException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 이 예외에 연결된 에러 코드를 반환한다.
     *
     * @return 에러 코드
     * @Since 2026-05-24
     */
    public BaseErrorCode getErrorCode() {
        return errorCode;
    }
}
