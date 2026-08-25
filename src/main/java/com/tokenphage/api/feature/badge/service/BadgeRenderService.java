package com.tokenphage.api.feature.badge.service;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.domain.badge.service.BadgeGrantResult;
import com.tokenphage.api.domain.badge.service.BadgeGrantService;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.BadgeSvgResponse;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.SvgBuilder;
import com.tokenphage.api.feature.badge.svg.theme.locked.LockedBadgeTheme;
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
    private final BadgeGrantService badgeGrantService;
    private final LockedBadgeTheme lockedBadgeTheme;

    @Value("${badge.cache-ttl-minutes}")
    private int cacheTtlMinutes;

    /**
     * 사용자 배지 SVG를 반환한다.
     * <p>
     * theme/mode를 등록된 값으로 정규화한 뒤 캐시 키를 구성한다. 정규화로 캐시 키 카디널리티를
     * 유한하게 제한하여, 임의 theme/mode 입력에 의한 Redis 키 무한 증식을 방지한다.
     * 캐시에 값이 있으면 즉시 반환하고, 없으면 DB에서 조회해 SVG를 생성한 뒤 캐시에 저장한다.
     * 캐시 TTL은 설정값 badge.cache-ttl-minutes를 따른다.
     *
     * <p>
     * 자격이 필요한 배지인데 배지 주인에게 자격이 없으면 잠금 안내 SVG를 대신 반환한다.
     * 이때 잠금 SVG는 캐시에 저장하지 않으므로 자격을 부여하면 다음 요청에 즉시 반영된다.
     *
     * @param username 배지를 조회할 GitHub 사용자명 (null 불허)
     * @param theme    배지 스킨 종류 (미등록 값은 기본 테마로 정규화)
     * @param mode     색상 모드 (테마가 지원하지 않는 값은 그 테마의 기본 모드로 정규화)
     * @return SVG 문자열과 자격 판정 결과
     * @throws com.tokenphage.api.exception.AppException 사용자가 없을 경우 (BADGE_001)
     * @Since 2026-05-27
     */
    public BadgeSvgResponse getSvg(String username, String theme, String mode) {

        String normalizedTheme = svgBuilder.normalizeTheme(theme);

        // #0. 자격 확인은 캐시 조회보다 먼저. 캐시-자격 불일치를 막고 회수가 즉시 반영된다.
        //     잠금 SVG는 캐시하지 않으므로 부여도 즉시 반영된다.
        BadgeGrantResult grant = badgeGrantService.resolveGrant(username, normalizedTheme);
        if (!grant.granted()) {
            log.info("Badge locked, rendering lock notice: username={}, theme={}", username, normalizedTheme);
            // 잠금 테마 자신의 지원 집합으로 다시 정규화한다 — 원 테마의 악센트 모드를 잠금 테마는 모른다.
            BadgeMode lockedMode = svgBuilder.resolveMode(BadgeCode.LOCKED.getCode(), mode);
            String lockedSvg = lockedBadgeTheme.render(
                    grant.title(), grant.message(), lockedMode == BadgeMode.DARK);
            return new BadgeSvgResponse(lockedSvg, false);
        }

        // 캐시 키와 렌더가 같은 정규화 결과를 쓰도록 테마 기준으로 모드를 확정한다.
        BadgeMode badgeMode = svgBuilder.resolveMode(normalizedTheme, mode);
        String cacheKey = "badge:" + username + ":" + normalizedTheme + ":" + badgeMode.getCode();
        String cached = redis.opsForValue().get(cacheKey);

        // #1. 배지(SVG)가 캐싱되어 있으면 즉시 리턴
        if (cached != null) {
            log.debug("Badge cache hit: [{}] key={}", username, cacheKey);
            return new BadgeSvgResponse(cached, true);
        }

        log.info("Badge cache miss, building SVG: username={}, theme={}, mode={}", username, normalizedTheme, badgeMode.getCode());

        // #2. BadgeQueryService.query() : 정규화된 테마가 선언한 needs에 해당하는 사용자 토큰 정보만 조회
        BadgeResponse data = queryService.query(username, normalizedTheme, svgBuilder.needsOf(normalizedTheme));

        // #3. 사용자 토큰 정보 + 정규화된 theme/mode로 배지(SVG) 생성
        String svg = svgBuilder.build(data, normalizedTheme, badgeMode.getCode());

        // #4. 생성된 배지(SVG)를 Redis에 캐싱
        redis.opsForValue().set(cacheKey, svg, Duration.ofMinutes(cacheTtlMinutes));
        return new BadgeSvgResponse(svg, true);
    }
}
