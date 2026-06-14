package com.tokenphage.api.feature.audit.dto;

/**
 * 단일 요청의 감사 메타데이터를 서비스로 전달하는 불변 커맨드.
 * <p>
 * HTTP 요청/응답 DTO가 아니라 audit 슬라이스 내부 캐리어이므로 request/response가 아닌 dto 직하에 둔다.
 * 생성은 요청을 가로채는 Aspect가 담당한다. 미인증 요청은 githubId/username/clientIp가 null일 수 있다.
 */
public record RequestAuditCommand(
    String feature,
    String action,
    String httpMethod,
    String requestPath,
    int statusCode,
    int latencyMs,
    String clientIp,
    Long githubId,
    String username,
    String userAgent,
    String outcome
) {}
