package com.tokenphage.api.feature.audit.aspect;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 리버스 프록시(Cloudflare 등) 뒤에서 원 클라이언트 IP를 추출하는 유틸.
 * <p>
 * CF-Connecting-IP → X-Forwarded-For(첫 항목) → getRemoteAddr() 순으로 해석한다.
 * 프록시 헤더는 신뢰된 프록시 뒤에서만 신뢰 가능하다(직접 노출 시 위조 가능).
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * 요청에서 원 클라이언트 IP를 해석한다.
     *
     * @param request 현재 HTTP 요청 (null이면 null 반환)
     * @return 해석된 IP 문자열, 없으면 null
     * @Since 2026-06-09
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String cfIp = request.getHeader("CF-Connecting-IP");
        if (cfIp != null && !cfIp.isBlank()) {
            return cfIp.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
