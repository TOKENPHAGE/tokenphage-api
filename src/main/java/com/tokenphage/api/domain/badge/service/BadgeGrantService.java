package com.tokenphage.api.domain.badge.service;

import com.tokenphage.api.domain.badge.repository.UserBadgeGrantRepository;
import com.tokenphage.api.domain.badge.repository.projection.BadgeGrantRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 배지 사용 가능 여부 판단.
 * <p>
 * 배지 종류를 메모리에 캐싱하지 않는다. 판단 쿼리가 함께 읽으므로 DB 변경이 다음 요청에 반영된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeGrantService {

    private final UserBadgeGrantRepository grantRepo;

    /**
     * 배지 주인이 그 배지를 쓸 수 있는지 확인하고, 거부 시 안내 문구를 함께 반환한다.
     * <p>
     * 판단 기준은 뷰어가 아니라 /badge/{username}의 주인이다.
     * 등록되지 않은 코드는 거부한다. 안내 문구 표시는 feature/badge가 정한다.
     *
     * @param username  배지 주인 GitHub 사용자명 (null 불허)
     * @param badgeCode 정리된 배지 코드 (null 불허)
     * @return 사용 가능 여부와 안내 문구
     * @Since 2026-08-10
     */
    public BadgeGrantResult resolveGrant(String username, String badgeCode) {
        BadgeGrantRow row = grantRepo.findGrant(username, badgeCode);
        if (row == null) {
            log.info("Badge code not found in catalog, denying: badgeCode={}", badgeCode);
            return BadgeGrantResult.deny(null, null);
        }
        if (row.getGranted()) {
            log.info("Badge grant allow: username={}, badgeCode={}", username, badgeCode);
            return BadgeGrantResult.allow();
        }
        log.info("Badge grant denied: username={}, badgeCode={}", username, badgeCode);
        return BadgeGrantResult.deny(row.getDisplayName(), row.getLockedMessage());
    }
}
