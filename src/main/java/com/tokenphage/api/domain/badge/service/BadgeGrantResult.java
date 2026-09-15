package com.tokenphage.api.domain.badge.service;

/**
 * 배지 사용 가능 여부 확인 결과.
 *
 * @param granted    쓸 수 있으면 true
 * @param userExists 배지 주인이 가입된 사용자면 true. false면 받는 쪽이 404로 끊는다
 * @param title      잠금 안내 제목(배지 이름). 거부일 때만 채워진다
 * @param message    잠금 안내 문구. null이면 받는 쪽이 기본 문구로 채운다
 */
public record BadgeGrantResult(boolean granted, boolean userExists, String title, String message) {

    /**
     * 사용 가능 결과를 만든다.
     * <p>
     * granted가 아닌 allow인 이유: record accessor {@code granted()}와 이름이 충돌한다.
     *
     * @return 사용 가능 결과
     * @Since 2026-08-10
     */
    public static BadgeGrantResult allow() {
        return new BadgeGrantResult(true, true, null, null);
    }

    /**
     * 거부 결과를 안내 문구와 함께 만든다.
     * <p>
     * 배지 주인은 가입돼 있고 자격만 없는 상태다. 받는 쪽은 잠금 안내를 그린다.
     *
     * @param title   배지 이름 (null이면 기본 제목으로 대체)
     * @param message 안내 문구 (null이면 기본 문구로 대체)
     * @return 거부 결과
     * @Since 2026-08-10
     */
    public static BadgeGrantResult deny(String title, String message) {
        return new BadgeGrantResult(false, true, title, message);
    }

    /**
     * 가입되지 않은 사용자라는 결과를 만든다.
     * <p>
     * 자격 거부와 구분된다. 잠금 안내를 그릴 대상이 아니므로 문구를 싣지 않는다.
     *
     * @return 미가입 결과
     * @Since 2026-09-06
     */
    public static BadgeGrantResult userNotFound() {
        return new BadgeGrantResult(false, false, null, null);
    }
}
