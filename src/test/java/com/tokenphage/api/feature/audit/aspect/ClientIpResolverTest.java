package com.tokenphage.api.feature.audit.aspect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClientIpResolver의 헤더 우선순위·폴백 동작을 검증한다(무 Spring 컨텍스트).
 */
class ClientIpResolverTest {

    @Test
    @DisplayName("IP해석_CF헤더존재_CF우선")
    void IP해석_CF헤더존재_CF우선() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("CF-Connecting-IP", "203.0.113.7");
        request.addHeader("X-Forwarded-For", "198.51.100.2");
        request.setRemoteAddr("10.0.0.1");

        // when / then
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("203.0.113.7");
    }

    @Test
    @DisplayName("IP해석_XFF다중_첫항목반환")
    void IP해석_XFF다중_첫항목반환() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "198.51.100.2, 70.41.3.18, 150.172.238.178");
        request.setRemoteAddr("10.0.0.1");

        // when / then
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("198.51.100.2");
    }

    @Test
    @DisplayName("IP해석_헤더없음_remoteAddr폴백")
    void IP해석_헤더없음_remoteAddr폴백() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");

        // when / then
        assertThat(ClientIpResolver.resolve(request)).isEqualTo("10.0.0.1");
    }

    @Test
    @DisplayName("IP해석_요청null_null반환")
    void IP해석_요청null_null반환() {
        // when / then
        assertThat(ClientIpResolver.resolve(null)).isNull();
    }
}
