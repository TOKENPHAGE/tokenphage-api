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
     * 가입되지 않은 주인은 자격 거부보다 먼저 가려낸다 — 공개 배지는 미가입이어도 granted가 참이라
     * granted만으로는 구분되지 않는다. 미가입을 어떤 응답으로 바꿀지는 feature/badge가 정한다.
     *
     * @param username  배지 주인 GitHub 사용자명 (null 불허)
     * @param badgeCode 정리된 배지 코드 (null 불허)
     * @return 사용 가능 여부·사용자 존재 여부와 안내 문구
     * @Since 2026-08-10
     */
    public BadgeGrantResult resolveGrant(String username, String badgeCode) {
        BadgeGrantRow row = grantRepo.findGrant(username, badgeCode);
        if (row == null) {
            // 조회 결과가 없으면 사용자 존재도 알 수 없다. 미가입으로 단정하지 않고 거부만 한다.
            log.info("Badge code not found in catalog, denying: badgeCode={}", badgeCode);
            return BadgeGrantResult.deny(null, null);
        }
        if (!row.getUserExists()) {
            log.info("Badge owner not registered: username={}, badgeCode={}", username, badgeCode);
            return BadgeGrantResult.userNotFound();
        }
        if (row.getGranted()) {
            log.info("Badge grant allow: username={}, badgeCode={}", username, badgeCode);
            return BadgeGrantResult.allow();
        }
        log.info("Badge grant denied: username={}, badgeCode={}", username, badgeCode);
        return BadgeGrantResult.deny(row.getDisplayName(), row.getLockedMessage());
    }
}
