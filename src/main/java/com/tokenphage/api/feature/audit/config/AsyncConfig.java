package com.tokenphage.api.feature.audit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 활성화 및 감사 로그 적재 전용 스레드풀 설정.
 * <p>
 * 감사 적재를 본 요청 스레드풀과 분리해, 적재 부하·지연이 서비스 응답에 영향을 주지 않게 한다.
 * 풀 포화 시 기본 AbortPolicy로 거부되며, 거부 예외는 호출부(Aspect)에서 삼켜 로그로 남긴다.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 요청 감사 로그 적재 전용 스레드풀 Executor 빈을 생성한다.
     *
     * @param corePoolSize  기본 스레드 수
     * @param maxPoolSize   최대 스레드 수
     * @param queueCapacity 대기 큐 용량
     * @return 초기화된 ThreadPoolTaskExecutor
     * @Since 2026-06-09
     */
    @Bean
    public Executor auditTaskExecutor(
            @Value("${audit.executor.core-pool-size}") int corePoolSize,
            @Value("${audit.executor.max-pool-size}") int maxPoolSize,
            @Value("${audit.executor.queue-capacity}") int queueCapacity
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("audit-");
        executor.initialize();
        return executor;
    }
}
