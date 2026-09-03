package com.tokenphage.api.feature.audit.service;
import com.tokenphage.api.feature.audit.dto.RequestAuditCommand;
import com.tokenphage.api.feature.audit.repository.RequestAuditLogRepository;
import com.tokenphage.api.feature.audit.repository.entity.RequestAuditLog;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;

/**
 * RequestAuditService.record 의 매핑 위임과 best-effort 예외 처리를 검증한다(실 DB 미접촉).
 * <p>
 * Mockito는 @Async 프록시를 거치지 않으므로 record()가 동기로 실행되어 내부 로직을 그대로 검증할 수 있다.
 */
@ExtendWith(MockitoExtension.class)
class RequestAuditServiceTest {

    @Mock
    private RequestAuditLogRepository auditLogRepo;

    @InjectMocks
    private RequestAuditService requestAuditService;

    @Test
    @DisplayName("감사기록_정상커맨드_매핑하여저장위임")
    void 감사기록_정상커맨드_매핑하여저장위임() {
        // given
        RequestAuditCommand command = new RequestAuditCommand(
            "auth", "verify", "POST", "/auth/verify",
            200, 42, "203.0.113.7", -1001L, "octocat", "tokenphage-cli/1.0", "success");

        // when
        requestAuditService.record(command);

        // then
        ArgumentCaptor<RequestAuditLog> captor = ArgumentCaptor.forClass(RequestAuditLog.class);
        then(auditLogRepo).should().save(captor.capture());
        RequestAuditLog saved = captor.getValue();
        assertThat(saved.getFeature()).isEqualTo("auth");
        assertThat(saved.getAction()).isEqualTo("verify");
        assertThat(saved.getHttpMethod()).isEqualTo("POST");
        assertThat(saved.getRequestPath()).isEqualTo("/auth/verify");
        assertThat(saved.getStatusCode()).isEqualTo(200);
        assertThat(saved.getLatencyMs()).isEqualTo(42);
        assertThat(saved.getClientIp()).isEqualTo("203.0.113.7");
        assertThat(saved.getGithubId()).isEqualTo(-1001L);
        assertThat(saved.getUsername()).isEqualTo("octocat");
        assertThat(saved.getUserAgent()).isEqualTo("tokenphage-cli/1.0");
        assertThat(saved.getOutcome()).isEqualTo("success");
    }

    @Test
    @DisplayName("감사기록_저장중예외_예외삼키고전파안함")
    void 감사기록_저장중예외_예외삼키고전파안함() {
        // given — DB 장애로 save가 실패하는 상황
        RequestAuditCommand command = new RequestAuditCommand(
            "sync", "sync", "POST", "/api/sync",
            500, 10, "203.0.113.8", -1002L, "octocat", "tokenphage-cli/1.0", "error");
        willThrow(new RuntimeException("db down")).given(auditLogRepo).save(any());

        // when / then — best-effort: 예외를 전파하지 않는다
        assertThatCode(() -> requestAuditService.record(command)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("감사기록_초과길이필드_컬럼한계로truncate")
    void 감사기록_초과길이필드_컬럼한계로truncate() {
        // given — User-Agent와 request_path가 컬럼 한계(255)를 초과하는 입력
        String longUserAgent = "U".repeat(300);
        String longPath = "/p/" + "x".repeat(300);
        RequestAuditCommand command = new RequestAuditCommand(
            "auth", "verify", "POST", longPath,
            200, 1, "203.0.113.7", -1001L, "octocat", longUserAgent, "success");

        // when
        requestAuditService.record(command);

        // then — 컬럼 한계(255)로 잘려 저장된다(INSERT 실패·유실 방지)
        ArgumentCaptor<RequestAuditLog> captor = ArgumentCaptor.forClass(RequestAuditLog.class);
        then(auditLogRepo).should().save(captor.capture());
        RequestAuditLog saved = captor.getValue();
        assertThat(saved.getUserAgent()).hasSize(255);
        assertThat(saved.getRequestPath()).hasSize(255);
    }

    @Test
    @DisplayName("감사기록_미인증커맨드_식별필드null로저장")
    void 감사기록_미인증커맨드_식별필드null로저장() {
        // given — 미인증 요청(badge 조회): 사용자 식별 필드 null
        RequestAuditCommand command = new RequestAuditCommand(
            "badge", "badge", "GET", "/badge/octocat",
            200, 5, null, null, null, "Mozilla/5.0", "success");

        // when
        requestAuditService.record(command);

        // then
        ArgumentCaptor<RequestAuditLog> captor = ArgumentCaptor.forClass(RequestAuditLog.class);
        then(auditLogRepo).should().save(captor.capture());
        RequestAuditLog saved = captor.getValue();
        assertThat(saved.getGithubId()).isNull();
        assertThat(saved.getUsername()).isNull();
        assertThat(saved.getClientIp()).isNull();
        assertThat(saved.getFeature()).isEqualTo("badge");
    }
}
