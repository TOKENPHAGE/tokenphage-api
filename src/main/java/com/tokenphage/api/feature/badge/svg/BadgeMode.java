package com.tokenphage.api.feature.badge.svg;

import java.util.Set;

/**
 * 배지 색상 모드 (URL {@code ?mode=} 축).
 * <p>
 * 지원 집합과 기본값은 테마마다 다르다({@link BadgeTheme#supportedModes()}).
 * 코드 문자열은 URL·Redis 캐시 키로 사용되며, 바꾸면 기존 배지 주소가 깨진다.
 */
public enum BadgeMode {

    LIGHT("light"),
    DARK("dark"),
    CYAN("cyan"),
    GREEN("green"),
    PURPLE("purple");

    private final String code;

    BadgeMode(String code) {
        this.code = code;
    }

    /**
     * URL 및 캐시 키에 쓰이는 소문자 코드 문자열을 반환한다.
     *
     * @return 소문자 코드 (예: "cyan")
     * @Since 2026-08-23
     */
    public String getCode() {
        return code;
    }

    /**
     * 원본 mode 문자열을 지원 집합 안의 모드로 정규화한다.
     * <p>
     * 대소문자 무관 매칭. null·공백·미지원 값은 {@code fallback}으로 접는다.
     *
     * @param raw       원본 mode 파라미터 (null 허용)
     * @param supported 테마가 지원하는 모드 집합 (null 불허)
     * @param fallback  미지원 값일 때 반환할 기본 모드 (null 불허)
     * @return 지원 집합 안의 모드 또는 fallback
     * @Since 2026-08-23
     */
    public static BadgeMode from(String raw, Set<BadgeMode> supported, BadgeMode fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        for (BadgeMode mode : supported) {
            if (mode.code.equalsIgnoreCase(raw)) {
                return mode;
            }
        }
        return fallback;
    }
}
