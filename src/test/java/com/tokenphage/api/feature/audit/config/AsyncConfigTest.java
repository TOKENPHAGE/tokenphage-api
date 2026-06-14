package com.tokenphage.api.feature.audit.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AsyncConfig가 생성하는 감사 적재 전용 Executor의 풀 속성을 검증한다(무 Spring).
 */
class AsyncConfigTest {

    @Test
    @DisplayName("감사Executor생성_설정값주입_풀속성일치")
    void 감사Executor생성_설정값주입_풀속성일치() {
        // given
        AsyncConfig config = new AsyncConfig();

        // when
        Executor executor = config.auditTaskExecutor(2, 4, 500);

        // then
        assertThat(executor).isInstanceOf(ThreadPoolTaskExecutor.class);
        ThreadPoolTaskExecutor pool = (ThreadPoolTaskExecutor) executor;
        assertThat(pool.getCorePoolSize()).isEqualTo(2);
        assertThat(pool.getMaxPoolSize()).isEqualTo(4);
        assertThat(pool.getThreadNamePrefix()).isEqualTo("audit-");
    }
}
