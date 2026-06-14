package com.tokenphage.api.feature.audit.repository.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RequestAuditLog 엔티티의 기본값·세터 동작을 검증한다(실 DB 미접촉, 순수 단위 테스트).
 */
class RequestAuditLogTest {

    @Test
    @DisplayName("감사로그생성_기본생성자_발생시각이초기화됨")
    void 감사로그생성_기본생성자_발생시각이초기화됨() {
        // given / when
        RequestAuditLog log = new RequestAuditLog();

        // then
        assertThat(log.getOccurredAt()).isNotNull();
        assertThat(log.getOccurredAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    @DisplayName("감사로그세터_값주입_동일값반환")
    void 감사로그세터_값주입_동일값반환() {
        // given
        RequestAuditLog log = new RequestAuditLog();

        // when
        log.setFeature("auth");
        log.setAction("verify");
        log.setHttpMethod("POST");
        log.setRequestPath("/auth/verify");
        log.setStatusCode(200);
        log.setLatencyMs(42);
        log.setClientIp("203.0.113.7");
        log.setGithubId(-1001L);
        log.setUsername("octocat");
        log.setUserAgent("tokenphage-cli/1.0");
        log.setOutcome("success");

        // then
        assertThat(log.getFeature()).isEqualTo("auth");
        assertThat(log.getAction()).isEqualTo("verify");
        assertThat(log.getHttpMethod()).isEqualTo("POST");
        assertThat(log.getRequestPath()).isEqualTo("/auth/verify");
        assertThat(log.getStatusCode()).isEqualTo(200);
        assertThat(log.getLatencyMs()).isEqualTo(42);
        assertThat(log.getClientIp()).isEqualTo("203.0.113.7");
        assertThat(log.getGithubId()).isEqualTo(-1001L);
        assertThat(log.getUsername()).isEqualTo("octocat");
        assertThat(log.getUserAgent()).isEqualTo("tokenphage-cli/1.0");
        assertThat(log.getOutcome()).isEqualTo("success");
    }

    @Test
    @DisplayName("감사로그생성_미인증요청_사용자식별필드는null허용")
    void 감사로그생성_미인증요청_사용자식별필드는null허용() {
        // given / when
        RequestAuditLog log = new RequestAuditLog();
        log.setFeature("badge");
        log.setAction("badge");
        log.setHttpMethod("GET");
        log.setRequestPath("/badge/octocat");
        log.setStatusCode(200);
        log.setLatencyMs(5);

        // then
        assertThat(log.getGithubId()).isNull();
        assertThat(log.getUsername()).isNull();
        assertThat(log.getClientIp()).isNull();
    }
}
