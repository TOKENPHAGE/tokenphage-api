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

}
