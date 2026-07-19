package com.tokenphage.api.feature.badge.svg;

/**
 * 테마 렌더링에 공통으로 쓰는 SVG 텍스트 유틸 (이스케이프·링크 URL·토큰 포맷).
 * <p>
 * 아웃바운드 의존이 없는 리프 유틸이라 어느 테마 패키지에서든 참조해도 순환을 만들지 않는다.
 * (디스패처 {@link com.tokenphage.api.feature.badge.svg.SvgBuilder}에서 분리했다.)
 */
public final class SvgText {

    /** 뱃지 클릭 시 이동할 프로젝트 GitHub 저장소 URL. 모든 테마·모드 공통이며 전 환경 동일한 고정값이다. */
    public static final String LINK_URL = "https://github.com/TOKENPHAGE/tokenphage-api";

    private SvgText() {
    }

    /**
     * SVG 텍스트에 삽입할 문자열의 XML 특수문자(&amp;, &lt;, &gt;)를 이스케이프한다.
     * <p>
     * 사용자명 등 외부 입력이 텍스트로 렌더링될 때 마크업 주입(XSS)을 막기 위해 사용한다.
     *
     * @param s 이스케이프할 원본 문자열 (null 불허)
     * @return 이스케이프된 문자열
     * @Since 2026-07-16
     */
    public static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * 토큰 수를 읽기 쉬운 단위 문자열로 변환한다.
     * <p>
     * 1K / 1M / 1B / 1T 단위로 표시하며 소수점 첫째 자리를 유지한다. (예: 1500 → "1.5K")
     *
     * @param tokens 변환할 토큰 수
     * @return 단위 변환된 문자열
     * @Since 2026-07-16
     */
    public static String formatTokens(long tokens) {
        return switch (Long.valueOf(tokens)) {
            case Long l when l >= 1_000_000_000_000L -> String.format("%.1fT", l / 1_000_000_000_000.0);
            case Long l when l >= 1_000_000_000L -> String.format("%.1fB", l / 1_000_000_000.0);
            case Long l when l >= 1_000_000L -> String.format("%.1fM", l / 1_000_000.0);
            case Long l when l >= 1_000L -> String.format("%.1fK", l / 1_000.0);
            default -> String.valueOf(tokens);
        };
    }
}
