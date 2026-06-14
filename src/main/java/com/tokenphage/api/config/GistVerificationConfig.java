package com.tokenphage.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class GistVerificationConfig {

    /**
     * Gist 검증용 GitHub API RestClient 빈을 생성한다.
     * <p>
     * apiToken이 설정돼 있으면 Authorization 헤더를 추가해 인증 요청으로 호출한다.
     * 미인증 요청은 응답이 60초간 공개 캐시되어 Gist 수정이 즉시 반영되지 않고,
     * IP당 시간당 60회로 제한된다. 토큰이 비어 있으면 미인증으로 폴백한다.
     *
     * @param baseUrl  GitHub API 베이스 URL
     * @param apiToken GitHub PAT (null이거나 비어 있으면 미인증 폴백)
     * @return 설정된 RestClient 인스턴스
     * @Since 2026-06-08
     */
    @Bean
    public RestClient gistRestClient(
            @Value("${auth.gist.api-base-url}") String baseUrl,
            @Value("${auth.gist.api-token}") String apiToken
    ) {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                // API 버전 고정 — 미설정 시 GitHub 기본 버전 변경으로 응답 구조가 달라질 수 있음
                // TO-DO : 2028년도에 해당 버전 지원 종료 예정, 리펙터링 들어가야함.
                // https://docs.github.com/en/rest/about-the-rest-api/api-versions
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28");

        if (apiToken != null && !apiToken.isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken);
        }
        return builder.build();
    }
}
