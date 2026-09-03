package com.tokenphage.api.feature.audit.aspect;
import com.tokenphage.api.audit.AuditOutcome;
import com.tokenphage.api.feature.audit.dto.RequestAuditCommand;
import com.tokenphage.api.feature.audit.service.RequestAuditService;

import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.auth.controller.AuthController;
import com.tokenphage.api.feature.auth.exception.AuthErrorCode;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * RequestAuditAspect의 캡처 로직(성공/AppException/미인증/인증)을 검증한다(무 Spring 컨텍스트, 실 DB 미접촉).
 * <p>
 * Mockito는 @Async 프록시를 거치지 않으므로 audit()가 동기로 실행되어 내부 로직을 그대로 검증할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class RequestAuditAspectTest {

    @Mock
    private RequestAuditService requestAuditService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Signature signature;

    private RequestAuditAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new RequestAuditAspect(requestAuditService);

        // joinPoint 시그니처: AuthController.verify
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getName()).thenReturn("verify");
        lenient().when(signature.getDeclaringType()).thenReturn((Class) AuthController.class);

        // 현재 요청 바인딩
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setRequestURI("/auth/verify");
        request.addHeader("User-Agent", "tokenphage-cli/1.0");
        request.addHeader("CF-Connecting-IP", "203.0.113.7");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("캡처_성공응답_상태와기능정보기록")
    void 캡처_성공응답_상태와기능정보기록() throws Throwable {
        // given
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        RequestAuditCommand command = captureRecorded();
        assertThat(command.feature()).isEqualTo("auth");
        assertThat(command.action()).isEqualTo("verify");
        assertThat(command.httpMethod()).isEqualTo("POST");
        assertThat(command.requestPath()).isEqualTo("/auth/verify");
        assertThat(command.statusCode()).isEqualTo(200);
        assertThat(command.clientIp()).isEqualTo("203.0.113.7");
        assertThat(command.userAgent()).isEqualTo("tokenphage-cli/1.0");
        assertThat(command.outcome()).isEqualTo("success");
    }

    @Test
    @DisplayName("캡처_컨트롤러가결과지정_그값을outcome에기록")
    void 캡처_컨트롤러가결과지정_그값을outcome에기록() throws Throwable {
        // given
        // 200으로 응답하지만 도메인상 거부인 경우(자격 없는 배지)를 구분하기 위한 통로다.
        currentRequest().setAttribute(AuditOutcome.ATTRIBUTE_KEY, AuditOutcome.BADGE_GRANT_DENIED);
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        RequestAuditCommand command = captureRecorded();
        assertThat(command.statusCode()).isEqualTo(200);
        assertThat(command.outcome()).isEqualTo("badge_grant_denied");
    }

    @Test
    @DisplayName("캡처_결과값이enum이아님_success로기록")
    void 캡처_결과값이enum이아님_success로기록() throws Throwable {
        // given
        // AuditOutcome 타입만 받는다. 문자열을 심어도 무시하고 기본값으로 떨어져야 한다.
        currentRequest().setAttribute(AuditOutcome.ATTRIBUTE_KEY, "badge_grant_denied");
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        assertThat(captureRecorded().outcome()).isEqualTo("success");
    }

    @Test
    @DisplayName("캡처_결과미지정_success로기록")
    void 캡처_결과미지정_success로기록() throws Throwable {
        // given
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        assertThat(captureRecorded().outcome()).isEqualTo("success");
    }

    @Test
    @DisplayName("캡처_쿼리스트링존재_경로에포함")
    void 캡처_쿼리스트링존재_경로에포함() throws Throwable {
        // given
        // 어떤 theme으로 접근했는지 남기려면 쿼리스트링이 필요하다.
        MockHttpServletRequest request = currentRequest();
        request.setRequestURI("/badge/example-user");
        request.setQueryString("theme=contributor&mode=dark");
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        assertThat(captureRecorded().requestPath())
                .isEqualTo("/badge/example-user?theme=contributor&mode=dark");
    }

    @Test
    @DisplayName("캡처_경로가컬럼길이초과_255자로절단")
    void 캡처_경로가컬럼길이초과_255자로절단() throws Throwable {
        // given
        // request_path는 VARCHAR(255)라 자르지 않으면 비동기 적재가 조용히 실패한다.
        MockHttpServletRequest request = currentRequest();
        request.setRequestURI("/badge/example-user");
        request.setQueryString("theme=" + "x".repeat(400));
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        assertThat(captureRecorded().requestPath()).hasSize(255);
    }

    /**
     * 현재 스레드에 바인딩된 MockHttpServletRequest를 꺼낸다.
     */
    private MockHttpServletRequest currentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return (MockHttpServletRequest) attrs.getRequest();
    }

    @Test
    @DisplayName("캡처_AppException_에러코드상태와코드기록후재전파")
    void 캡처_AppException_에러코드상태와코드기록후재전파() throws Throwable {
        // given — gist 도용 시나리오(401, AUTH_007)
        given(joinPoint.proceed()).willThrow(new AppException(AuthErrorCode.OWNER_MISMATCH));

        // when / then — 예외는 그대로 재전파되어야 한다
        assertThatThrownBy(() -> aspect.audit(joinPoint)).isInstanceOf(AppException.class);

        RequestAuditCommand command = captureRecorded();
        assertThat(command.statusCode()).isEqualTo(401);
        assertThat(command.outcome()).isEqualTo("AUTH_007");
    }

    @Test
    @DisplayName("캡처_미인증요청_식별필드null")
    void 캡처_미인증요청_식별필드null() throws Throwable {
        // given — SecurityContext 미설정
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        RequestAuditCommand command = captureRecorded();
        assertThat(command.githubId()).isNull();
        assertThat(command.username()).isNull();
    }

    @Test
    @DisplayName("캡처_인증요청_principal에서githubId추출")
    void 캡처_인증요청_principal에서githubId추출() throws Throwable {
        // given — JWT principal을 SecurityContext에 주입
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "HS256")
            .subject("-1001")
            .claim("username", "octocat")
            .build();
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(jwt, null));
        given(joinPoint.proceed()).willReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        RequestAuditCommand command = captureRecorded();
        assertThat(command.githubId()).isEqualTo(-1001L);
        assertThat(command.username()).isEqualTo("octocat");
    }

    private RequestAuditCommand captureRecorded() {
        ArgumentCaptor<RequestAuditCommand> captor = ArgumentCaptor.forClass(RequestAuditCommand.class);
        then(requestAuditService).should().record(captor.capture());
        return captor.getValue();
    }
}
