package com.tokenphage.api.feature.audit.aspect;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

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
    @DisplayName("캡처_AppException_에러코드상태와코드기록후재전파")
    void 캡처_AppException_에러코드상태와코드기록후재전파() throws Throwable {
        // given — gist 도용 시나리오(401, AUTH_007)
        when(joinPoint.proceed()).thenThrow(new AppException(AuthErrorCode.OWNER_MISMATCH));

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
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

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
        when(joinPoint.proceed()).thenReturn(ResponseEntity.ok().build());

        // when
        aspect.audit(joinPoint);

        // then
        RequestAuditCommand command = captureRecorded();
        assertThat(command.githubId()).isEqualTo(-1001L);
        assertThat(command.username()).isEqualTo("octocat");
    }

    private RequestAuditCommand captureRecorded() {
        ArgumentCaptor<RequestAuditCommand> captor = ArgumentCaptor.forClass(RequestAuditCommand.class);
        verify(requestAuditService).record(captor.capture());
        return captor.getValue();
    }
}
