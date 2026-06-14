package com.tokenphage.api.feature.badge.service;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.SvgBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeRenderService {

    private final BadgeQueryService queryService;
    private final SvgBuilder svgBuilder;
    private final StringRedisTemplate redis;

    @Value("${badge.cache-ttl-minutes}")
    private int cacheTtlMinutes;

    /**
     * 사용자 배지 SVG를 반환한다.
     * <p>
     * theme/mode를 등록된 값으로 정규화한 뒤 캐시 키를 구성한다. 정규화로 캐시 키 카디널리티를
     * 유한하게 제한해, 임의 theme/mode 입력에 의한 Redis 키 무한 증식을 방지한다.
     * 캐시에 값이 있으면 즉시 반환하고, 없으면 DB에서 조회해 SVG를 생성한 뒤 캐시에 저장한다.
     * 캐시 TTL은 설정값 badge.cache-ttl-minutes를 따른다.
     *
     * @param username 배지를 조회할 GitHub 사용자명 (null 불허)
     * @param theme    배지 스킨 종류 (미등록 값은 기본 테마로 정규화)
     * @param mode     색상 모드 ("dark"가 아니면 "light"로 정규화)
     * @return SVG 문자열
     * @throws com.tokenphage.api.exception.AppException 사용자가 없을 경우 (BADGE_001)
     * @Since 2026-05-27
     */
    public String getSvg(String username, String theme, String mode) {
        String normalizedTheme = svgBuilder.normalizeTheme(theme);
        String normalizedMode = svgBuilder.normalizeMode(mode);
        String cacheKey = "badge:" + username + ":" + normalizedTheme + ":" + normalizedMode;
        String cached = redis.opsForValue().get(cacheKey);

        // #0. 배지(SVG)가 캐싱되어 있으면 즉시 리턴
        if (cached != null) {
            log.debug("Badge cache hit: [{}] key={}", username, cacheKey);
            return cached;
        }

        log.info("Badge cache miss, building SVG: username={}, theme={}, mode={}", username, normalizedTheme, normalizedMode);

        // #1. BadgeQueryService.query() : 배지(SVG)에 필요한 사용자 토큰 정보 조회
        BadgeResponse data = queryService.query(username);

        // #2. 사용자 토큰 정보 + 정규화된 theme/mode로 배지(SVG) 생성
        String svg = svgBuilder.build(data, normalizedTheme, normalizedMode);

        // #3. 생성된 배지(SVG)를 Redis에 캐싱
        redis.opsForValue().set(cacheKey, svg, Duration.ofMinutes(cacheTtlMinutes));
        return svg;
    }
}
