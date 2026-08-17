package com.tokenphage.api.integration;

import com.redis.testcontainers.RedisContainer;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.io.IOException;

/**
 * 통합테스트 공용 컨테이너. 실 dev DB 대신 일회용 컨테이너에 붙는다.
 * <p>
 * static 초기화로 한 번만 띄워 전 테스트가 공유한다(@Container 는 클래스마다 재기동).
 * 정리는 Ryuk 이 JVM 종료 시 수행한다.
 */
public class ContainerSupport {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    @ServiceConnection
    static final RedisContainer REDIS = new RedisContainer("redis:7-alpine");

    /** GitHub Gist API 스텁. 컨테이너와 같은 이유로 static 싱글턴이다. */
    static final MockWebServer GIST_API = new MockWebServer();

    /** 로컬 application-dev.yml 의 실 PAT 가 테스트 소켓·로그로 흘러가지 않도록 고정하는 더미 값. */
    static final String GIST_API_TOKEN = "test-token-not-a-real-pat";

    static {
        // 컨테이너보다 먼저 띄운다 — 포트 바인딩이 실패하면 수 초짜리 컨테이너 기동 전에 드러난다.
        try {
            GIST_API.start();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start Gist MockWebServer", e);
        }
        // MockWebServer 의 TaskRunner 스레드는 non-daemon 이라 IDE 단독 실행 시 JVM 이 종료되지 않는다.
        // (Maven fork JVM 은 System.exit 하므로 CI 에는 영향 없다)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                GIST_API.shutdown();
            } catch (IOException ignored) {
                // 종료 중 실패는 무시한다 — 프로세스가 곧 끝난다.
            }
        }));
        POSTGRES.start();
        REDIS.start();
    }

    /**
     * Gist API 대상을 MockWebServer 로 돌려 외부 GitHub 호출을 막는다.
     * <p>
     * RestClient 빈이 baseUrl 을 1회 고정하므로 컨텍스트 refresh 전에 값이 정해져야 한다.
     * 베이스 클래스에 둬야 하위 클래스가 같은 컨텍스트 캐시를 공유한다.
     */
    @DynamicPropertySource
    static void gistApiProperties(DynamicPropertyRegistry registry) {
        // 트레일링 슬래시 없이 조립한다 — RestClient 의 "/gists/{id}" 와 합쳐질 때 모호함을 없앤다.
        registry.add("auth.gist.api-base-url",
                () -> "http://" + GIST_API.getHostName() + ":" + GIST_API.getPort());
        registry.add("auth.gist.api-token", () -> GIST_API_TOKEN);
    }
}
