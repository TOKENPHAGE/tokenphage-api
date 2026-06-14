package com.tokenphage.api.exception.error;

import org.springframework.http.HttpStatus;

/**
 * 모든 feature별 에러 코드가 구현해야 하는 인터페이스.
 * 각 feature는 이 인터페이스를 구현하는 enum을 정의한다.
 */
public interface BaseErrorCode {

    /**
     * HTTP 응답 상태 코드를 반환한다.
     *
     * @return HTTP 상태 코드
     * @Since 2026-05-24
     */
    HttpStatus getStatus();

    /**
     * 클라이언트에게 전달할 에러 식별 코드를 반환한다. (예: AUTH_001)
     *
     * @return 에러 식별 코드 문자열
     * @Since 2026-05-24
     */
    String getCode();

    /**
     * 에러 설명 메시지를 반환한다.
     *
     * @return 에러 메시지
     * @Since 2026-05-24
     */
    String getMessage();
}
