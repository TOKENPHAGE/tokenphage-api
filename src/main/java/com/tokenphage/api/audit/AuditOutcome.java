package com.tokenphage.api.audit;

/**
 * 감사 로그에 남길 고정 결과 값과 전달용 요청 속성 키.
 * <p>
 * feature 슬라이스끼리 공유하는 계약이라 최상위에 둔다(exception 패키지와 동일 성격).
 * AppException 에러코드·예외 클래스명은 런타임에 정해지므로 여기 담지 않는다.
 * <p>
 * 코드 문자열은 이미 쌓인 로그와 대조되는 값이라 바꾸지 않는다. 새 값은 40자를 넘기지 않는다 —
 * 넘치면 RequestAuditService가 잘라낸다.
 */
public enum AuditOutcome {

    /** 정상 처리. 컨트롤러가 아무것도 지정하지 않으면 이 값이 기록된다. */
    SUCCESS("success"),

    /** 결과를 판별하기 전에 중단된 경우의 안전값. */
    UNKNOWN("unknown"),

    /** 자격 없이 배지를 요청해 잠금 안내를 반환함. 응답이 200이라 상태코드로는 구분되지 않는다. */
    BADGE_GRANT_DENIED("badge_grant_denied");

    /**
     * 컨트롤러 → RequestAuditAspect 전달용 요청 속성 키.
     * <p>
     * 서블릿 요청 속성은 스프링·톰캣과 이름 공간을 공유하므로 프로젝트 접두어를 붙인다.
     */
    public static final String ATTRIBUTE_KEY = "tokenphage.audit.outcome";

    private final String code;

    AuditOutcome(String code) {
        this.code = code;
    }

    /**
     * request_audit_log.outcome 에 저장되는 문자열을 반환한다.
     *
     * @return 결과 코드 (예: "badge_grant_denied")
     * @Since 2026-08-12
     */
    public String getCode() {
        return code;
    }
}
