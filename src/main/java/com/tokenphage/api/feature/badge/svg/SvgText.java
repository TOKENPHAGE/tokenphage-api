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
     * 1K / 1M / 1B / 1T 단위로 표시하며 소수점 첫째 자리까지 남기고 나머지는 자른다. (예: 1760 → "1.7K")
     * 반올림하지 않으므로 표시값이 실제 값을 넘지 않아, 레벨 임계값 근처에서도 숫자와 레벨이 어긋나지 않는다.
     *
     * @param tokens 변환할 토큰 수
     * @return 단위 변환된 문자열
     * @Since 2026-07-16
     */
    public static String formatTokens(long tokens) {
        return switch (Long.valueOf(tokens)) {
            case Long l when l >= 1_000_000_000_000L -> truncate(l, 1_000_000_000_000L, "T");
            case Long l when l >= 1_000_000_000L -> truncate(l, 1_000_000_000L, "B");
            case Long l when l >= 1_000_000L -> truncate(l, 1_000_000L, "M");
            case Long l when l >= 1_000L -> truncate(l, 1_000L, "K");
            default -> String.valueOf(tokens);
        };
    }

    // 단위로 나눈 값을 소수 첫째 자리에서 자른다. 정수 연산이라 부동소수 오차가 없다.
    private static String truncate(long tokens, long unit, String suffix) {
        long tenths = tokens / (unit / 10);
        return tenths / 10 + "." + tenths % 10 + suffix;
    }
}
