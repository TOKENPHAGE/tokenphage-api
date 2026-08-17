package com.tokenphage.api.domain.badge.service;

/**
 * 배지 사용 가능 여부 확인 결과.
 *
 * @param granted 쓸 수 있으면 true
 * @param title   잠금 안내 제목(배지 이름). 거부일 때만 채워진다
 * @param message 잠금 안내 문구. null이면 받는 쪽이 기본 문구로 채운다
 */
public record BadgeGrantResult(boolean granted, String title, String message) {

    /**
     * 사용 가능 결과를 만든다.
     * <p>
     * granted가 아닌 allow인 이유: record accessor {@code granted()}와 이름이 충돌한다.
     *
     * @return 사용 가능 결과
     * @Since 2026-08-10
     */
    public static BadgeGrantResult allow() {
        return new BadgeGrantResult(true, null, null);
    }

    /**
     * 거부 결과를 안내 문구와 함께 만든다.
     *
     * @param title   배지 이름 (null이면 기본 제목으로 대체)
     * @param message 안내 문구 (null이면 기본 문구로 대체)
     * @return 거부 결과
     * @Since 2026-08-10
     */
    public static BadgeGrantResult deny(String title, String message) {
        return new BadgeGrantResult(false, title, message);
    }
}
