package com.tokenphage.api.domain.badge;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 배지 코드
 * <p>
 * URL {@code ?theme=} 값, {@code badge_catalog.code}, {@code BadgeTheme.name()}이 공유하는
 * 문자열의 정의처. 여기서만 정의해 오타를 컴파일 단계에서 잡는다.
 * <p>
 * feature/badge와 domain/badge가 함께 쓰므로 domain에 둔다(feature → domain만 허용).
 */
public enum BadgeCode {

    GPU("gpu"),
    CLAUDE("claude"),
    GRASS_CLAUDE("grass-claude"),

    /** 자격이 없어 사용할 수 없음을 알리는 안내 배지. 사용자가 직접 고르는 테마는 아니다. */
    LOCKED("locked");

    private final String code;

    BadgeCode(String code) {
        this.code = code;
    }

    /**
     * URL 및 DB에 쓰이는 소문자 코드 문자열을 반환한다.
     *
     * @return 소문자 코드 (예: "grass-claude")
     * @Since 2026-08-10
     */
    public String getCode() {
        return code;
    }

    /**
     * 등록된 모든 코드 문자열을 반환한다.
     * <p>
     * 테마 클래스·DB 초기 데이터와의 대조에 쓴다.
     *
     * @return 수정 불가 코드 목록
     * @Since 2026-08-10
     */
    public static Set<String> allCodes() {
        return Arrays.stream(values())
                .map(BadgeCode::getCode)
                .collect(Collectors.toUnmodifiableSet());
    }
}
