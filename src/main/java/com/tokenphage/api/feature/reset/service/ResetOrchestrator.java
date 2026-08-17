package com.tokenphage.api.feature.reset.service;

import com.tokenphage.api.domain.badge.BadgeCacheInvalidator;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.reset.exception.ResetErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** 전체 초기화 오케스트레이터: 쿨다운 게이트 → 삭제 → 배지 캐시 무효화. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResetOrchestrator {

    private static final String COOLDOWN_KEY_PREFIX = "cooldown:reset:";
    private static final Duration COOLDOWN = Duration.ofHours(24);

    private final ResetService resetService;
    private final BadgeCacheInvalidator badgeCacheInvalidator;
    private final StringRedisTemplate redis;

    /**
     * 인증된 사용자 본인의 모든 토큰 사용량을 초기화한다.
     * <p>
     * 식별자는 검증된 JWT(sub/username)에서만 파생한다(IDOR 방지 — 요청 본문 미사용).
     * 24시간 쿨다운을 원자적으로 선점(SET NX EX)한 뒤 삭제를 수행하고, 실패 시 쿨다운을 해제해
     * 서버 오류로 사용자가 24시간 잠기지 않도록 한다.
     *
     * @param jwt 인증된 호출자의 JWT 주체 (null 불허)
     * @throws AppException 쿨다운이 활성화된 경우 (RESET_001, HTTP 429)
     * @Since 2026-06-06
     */
    public void reset(Jwt jwt) {
        Long githubId = Long.parseLong(jwt.getSubject());
        String username = jwt.getClaimAsString("username");
        String cooldownKey = COOLDOWN_KEY_PREFIX + githubId;

        boolean acquired = Boolean.TRUE.equals(
            redis.opsForValue().setIfAbsent(cooldownKey, Instant.now().toString(), COOLDOWN));
        if (!acquired) {
            log.warn("Reset rejected by cooldown: githubId={}", githubId);
            throw new AppException(ResetErrorCode.RESET_COOLDOWN);
        }

        log.info("Reset started: githubId={}, username={}", githubId, username);
        try {
            resetService.resetUsage(githubId);
        } catch (RuntimeException e) {
            // DB 쓰기는 트랜잭션이라 실패 시 전부 롤백된다(삭제된 데이터 없음).
            // 서버 오류로 24시간 잠기지 않도록 쿨다운을 해제한 뒤 그대로 전파한다.
            releaseCooldown(cooldownKey, githubId);
            log.error("Reset DB write failed, cooldown released: githubId={}", githubId, e);
            throw e;
        }

        // DB 삭제는 이미 커밋됨. 배지 캐시 무효화는 best-effort —
        // 실패해도 reset 자체는 성공이고(캐시는 TTL로 자연 만료/재생성), 쿨다운은 유지한다.
        try {
            badgeCacheInvalidator.evict(username);
        } catch (RuntimeException e) {
            log.error("Reset committed but badge cache eviction failed (cache expires via TTL): githubId={}, username={}", githubId, username, e);
        }
        log.info("Reset completed: githubId={}, username={}", githubId, username);
    }

    /** 쿨다운 키를 해제한다. 삭제 자체가 실패해도 원래 예외 전파를 막지 않도록 격리한다. */
    private void releaseCooldown(String cooldownKey, Long githubId) {
        try {
            redis.delete(cooldownKey);
        } catch (RuntimeException e) {
            log.error("Failed to release reset cooldown key={}, githubId={}: {}", cooldownKey, githubId, e.getMessage());
        }
    }
}
