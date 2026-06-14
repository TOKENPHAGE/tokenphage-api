package com.tokenphage.api.feature.auth;

import com.tokenphage.api.feature.auth.service.JwtIssuer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * CLI 실행을 위한 JWT를 ~/.tokenphage/config.json 에 저장.
 * 운영 서버와 동일한 32-byte 시크릿 사용.
 */
@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "badge.jwt-secret=tokenphage-dev-secret-32bytes-xpadxx"
})
class JwtIssuerForCliTest {

    @Autowired
    private JwtIssuer jwtIssuer;

    @AfterEach
    void cleanUpConfig() throws IOException {
        // 테스트가 생성한 config.json 을 삭제해 홈 디렉터리에 잔존하지 않도록 한다.
        Files.deleteIfExists(Path.of(System.getProperty("user.home"), ".tokenphage", "config.json"));
    }

    @Test
    void writeJwtConfigForCli() throws IOException {
        String token = jwtIssuer.issue(-12345L, "kobenlys");

        Path configDir = Path.of(System.getProperty("user.home"), ".tokenphage");
        Files.createDirectories(configDir);
        String config = """
            {
              "token": "%s",
              "deviceId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
            }
            """.formatted(token);
        Files.writeString(configDir.resolve("config.json"), config);
    }
}
