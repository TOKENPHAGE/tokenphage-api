package com.tokenphage.api.feature.badge.svg.theme;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 누적 토큰을 레벨별 대표 도서의 토큰 수에 빗대 "재치 있는 비교 문구"로 만든다.
 * <p>
 * 활동 레벨이 대표 책을 결정하고, 그 책 토큰 수 대비 배수를 붙여 {@code "Bible 609권 갉아먹는 중"}처럼 표현한다.
 * 책 토큰 수는 book-token-analysis.md(GPT-4 cl100k)의 값을 계산 편의를 위해 대략값(반올림)으로 둔 것이다.
 */
final class TokenBookScale {

    private TokenBookScale() {
    }

    /**
     * 레벨별 대표 책. (도서명, 대략 토큰 수)
     */
    private record Book(String name, long tokens) {
    }

    // 인덱스 1~5 사용 (0번은 자리표시). 토큰 수는 대략값(book-token-analysis.md 기반 반올림).
    private static final Book[] BY_LEVEL = {
            null,
            new Book("The Little Prince", 20_000L),
            new Book("Harry Potter", 110_000L),
            new Book("Crime and Punishment", 290_000L),
            new Book("Bible", 1_150_000L),
            new Book("Harry Potter series", 1_530_000L)
    };

    // "{N}권" 뒤에 붙는 trailing 문구. 노출 시 5종 중 무작위로 고른다. 순서 변경 시 테스트 인덱스도 확인.
    private static final String[] VERBS = {
            "갉아먹는 중", "냠냠", "꿀꺽", "와구와구", "뇸뇸"
    };

    // 배수가 이 값 미만이면 소수 1자리, 이상이면 정수로 표기한다.
    private static final long DECIMAL_THRESHOLD = 10;

    /**
     * 활동 레벨의 대표 책 대비 누적 토큰 배수를 비교 문구로 만든다. trailing 문구는 {@link #VERBS} 5종 중 무작위로 고른다.
     * <p>
     * 배수가 10 미만이면 소수 1자리(예: 2.5권), 10 이상이면 정수(예: 609권)로 표기한다.
     *
     * @param level       활동 레벨 (1~5)
     * @param totalTokens 누적 토큰 수
     * @return "{도서명} {배수}권 {랜덤 문구}" 형태의 캡션 (예: "Bible 609권 냠냠")
     * @Since 2026-06-01
     */
    static String describe(int level, long totalTokens) {
        return describe(level, totalTokens, ThreadLocalRandom.current().nextInt(VERBS.length));
    }

    /**
     * 활동 레벨의 대표 책 대비 누적 토큰 배수를 비교 문구로 만든다. trailing 문구를 인덱스로 고정한다(결정적, 테스트용).
     *
     * @param level       활동 레벨 (1~5)
     * @param totalTokens 누적 토큰 수
     * @param verbIndex   trailing 문구 인덱스 (0 이상 {@link #VERBS} 길이 미만)
     * @return "{도서명} {배수}권 {문구}" 형태의 캡션
     * @Since 2026-06-01
     */
    static String describe(int level, long totalTokens, int verbIndex) {
        Book book = BY_LEVEL[level];
        double ratio = (double) totalTokens / book.tokens();
        String multiple = ratio < DECIMAL_THRESHOLD
                ? String.format(Locale.US, "%.1f", ratio)
                : String.valueOf(Math.round(ratio));
        return book.name() + " " + multiple + "권 " + VERBS[verbIndex];
    }
}
