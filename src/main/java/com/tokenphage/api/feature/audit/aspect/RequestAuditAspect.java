package com.tokenphage.api.feature.audit.aspect;

import com.tokenphage.api.feature.audit.dto.RequestAuditCommand;
import com.tokenphage.api.feature.audit.service.RequestAuditService;

import com.tokenphage.api.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 컨트롤러 요청을 가로채 감사 메타데이터를 비동기로 적재하는 Aspect.
 * <p>
 * cross-cutting 관심사라 특정 feature 슬라이스가 아닌 top-level audit 패키지에 둔다.
 * 본 요청 동작에 영향을 주지 않도록 적재 호출은 try/catch로 격리하며, 컨트롤러 예외는 그대로 전파한다.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RequestAuditAspect {

    private static final int STATUS_OK = 200;
    private static final int STATUS_UNEXPECTED_ERROR = 500;
    private static final String OUTCOME_SUCCESS = "success";
    private static final String OUTCOME_UNKNOWN = "unknown";
    private static final String CONTROLLER_SUFFIX = "Controller";

    private final RequestAuditService requestAuditService;

    /**
     * feature 슬라이스의 모든 컨트롤러 메서드를 가로채 감사 로그를 적재한다.
     * <p>
     * 성공 시 ResponseEntity의 상태를, AppException 시 에러코드의 상태/코드를 기록한다.
     * 예외는 삼키지 않고 재던져 GlobalExceptionHandler가 정상 처리하게 한다.
     *
     * @param joinPoint 진행 중인 컨트롤러 호출 (null 불허)
     * @return 컨트롤러의 원래 반환값
     * @throws Throwable 컨트롤러가 던진 예외를 그대로 전파
     * @Since 2026-06-09
     */
    @Around("execution(* com.tokenphage.api.feature..*Controller.*(..))")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        long startNanos = System.nanoTime();
        int statusCode = STATUS_UNEXPECTED_ERROR;
        String outcome = OUTCOME_UNKNOWN;
        try {
            Object result = joinPoint.proceed();
            statusCode = extractStatus(result);
            outcome = OUTCOME_SUCCESS;
            return result;
        } catch (AppException e) {
            statusCode = e.getErrorCode().getStatus().value();
            outcome = e.getErrorCode().getCode();
            throw e;
        } catch (Throwable t) {
            statusCode = STATUS_UNEXPECTED_ERROR;
            outcome = t.getClass().getSimpleName();
            throw t;
        } finally {
            long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
            recordSafely(joinPoint, statusCode, outcome, latencyMs);
        }
    }

    /**
     * 감사 커맨드를 구성해 비동기 적재를 호출한다. 어떤 예외도 본 요청으로 전파하지 않는다.
     *
     * @param joinPoint  컨트롤러 호출
     * @param statusCode 응답 상태 코드
     * @param outcome    결과 코드(success 또는 에러 식별자)
     * @param latencyMs  처리 지연(ms)
     */
    private void recordSafely(ProceedingJoinPoint joinPoint, int statusCode, String outcome, long latencyMs) {
        try {
            HttpServletRequest request = currentRequest();
            RequestAuditCommand command = new RequestAuditCommand(
                    resolveFeature(joinPoint),
                    joinPoint.getSignature().getName(),
                    request != null ? request.getMethod() : null,
                    request != null ? request.getRequestURI() : null,
                    statusCode,
                    (int) latencyMs,
                    ClientIpResolver.resolve(request),
                    currentGithubId(),
                    currentUsername(),
                    request != null ? request.getHeader("User-Agent") : null,
                    outcome
            );
            requestAuditService.record(command);
        } catch (Exception e) {
            log.error("Failed to submit request audit: {}", e.getMessage());
        }
    }

    /**
     * 컨트롤러 반환값에서 HTTP 상태를 추출한다(ResponseEntity가 아니면 200).
     */
    private int extractStatus(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return STATUS_OK;
    }

    /**
     * 컨트롤러 클래스명(XxxController)에서 슬라이스명을 도출한다(AuthController → auth).
     */
    private String resolveFeature(ProceedingJoinPoint joinPoint) {
        String simpleName = joinPoint.getSignature().getDeclaringType().getSimpleName();
        String trimmed = simpleName.endsWith(CONTROLLER_SUFFIX)
                ? simpleName.substring(0, simpleName.length() - CONTROLLER_SUFFIX.length())
                : simpleName;
        return trimmed.toLowerCase();
    }

    /**
     * 현재 스레드에 바인딩된 HttpServletRequest를 반환한다(서블릿 컨텍스트가 없으면 null).
     */
    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    /**
     * JWT subject에서 현재 사용자의 GitHub ID를 추출한다(미인증이면 null).
     */
    private Long currentGithubId() {
        Jwt jwt = currentJwt();
        return jwt != null ? Long.parseLong(jwt.getSubject()) : null;
    }

    /**
     * JWT username 클레임에서 현재 사용자명을 추출한다(미인증이면 null).
     */
    private String currentUsername() {
        Jwt jwt = currentJwt();
        return jwt != null ? jwt.getClaimAsString("username") : null;
    }

    /**
     * 인증된 요청이면 JWT principal을, 아니면 null을 반환한다.
     */
    private Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
