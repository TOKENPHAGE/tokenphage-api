package com.tokenphage.api.feature.badge.svg.theme.betatester;

import com.tokenphage.api.feature.badge.svg.SvgLogo;

/**
 * 사용 모델 로고(Claude·OpenAI) 배치 유틸.
 * <p>
 * 판정은 스냅샷 적재 시점에 끝나 있다 — 여기서는 boolean 두 개로 배치만 결정한다.
 * Claude 로고 path는 프로토타입(badge-basic.svg.txt) 추출값이다. 손으로 수정하지 않는다.
 * OpenAI 로고 path는 {@link SvgLogo#OPENAI_PATH}를 쓴다.
 */
final class BetaTesterProviderMark {

    /** 로고 2개일 때 좌측(Claude) x (프로토타입 실측). */
    private static final String PAIR_LEFT_X = "31.5";

    /** 로고 2개일 때 우측(OpenAI) x (프로토타입 실측). */
    private static final String PAIR_RIGHT_X = "44.5";

    /** 로고 1개일 때 x — 순번 텍스트와 같은 중심 42 유지 (로고 폭 8px = 24 × 0.3333). */
    private static final String SINGLE_X = "38";

    /** 로고 상단 y — 프로토타입 100 기준 상단 여백 정리로 12 조정. */
    private static final int MARK_Y = 88;

    /** 로고 축소 배율 (프로토타입 실측). */
    private static final String MARK_SCALE = "0.3333";

    /** Claude 로고 fill — 모드와 무관한 브랜드색. */
    private static final String CLAUDE_FILL = "#D97757";

    /** Claude 로고 path d (프로토타입 추출). */
    private static final String CLAUDE_MARK = """
            M4.709 15.955l4.72-2.647.08-.23-.08-.128H9.2l-.79-.048-2.698-.073-2.339-.097-2.266-.122-.571-.121L0 \
            11.784l.055-.352.48-.321.686.06 1.52.103 2.278.158 1.652.097 2.449.255h.389l.055-.157-.134-.098-.103\
            -.097-2.358-1.596-2.552-1.688-1.336-.972-.724-.491-.364-.462-.158-1.008.656-.722.881.06.225.061.893.\
            686 1.908 1.476 2.491 1.833.365.304.145-.103.019-.073-.164-.274-1.355-2.446-1.446-2.49-.644-1.032-.1\
            7-.619a2.97 2.97 0 01-.104-.729L6.283.134 6.696 0l.996.134.42.364.62 1.414 1.002 2.229 1.555 3.03.45\
            6.898.243.832.091.255h.158V9.01l.128-1.706.237-2.095.23-2.695.08-.76.376-.91.747-.492.584.28.48.685-\
            .067.444-.286 1.851-.559 2.903-.364 1.942h.212l.243-.242.985-1.306 1.652-2.064.73-.82.85-.904.547-.4\
            31h1.033l.76 1.129-.34 1.166-1.064 1.347-.881 1.142-1.264 1.7-.79 1.36.073.11.188-.02 2.856-.606 1.5\
            43-.28 1.841-.315.833.388.091.395-.328.807-1.969.486-2.309.462-3.439.813-.042.03.049.061 1.549.146.6\
            62.036h1.622l3.02.225.79.522.474.638-.079.485-1.215.62-1.64-.389-3.829-.91-1.312-.329h-.182v.11l1.09\
            3 1.068 2.006 1.81 2.509 2.33.127.578-.322.455-.34-.049-2.205-1.657-.851-.747-1.926-1.62h-.128v.17l.\
            444.649 2.345 3.521.122 1.08-.17.353-.608.213-.668-.122-1.374-1.925-1.415-2.167-1.143-1.943-.14.08-.\
            674 7.254-.316.37-.729.28-.607-.461-.322-.747.322-1.476.389-1.924.315-1.53.286-1.9.17-.632-.012-.042\
            -.14.018-1.434 1.967-2.18 2.945-1.726 1.845-.414.164-.717-.37.067-.662.401-.589 2.388-3.036 1.44-1.8\
            82.93-1.086-.006-.158h-.055L4.132 18.56l-1.13.146-.487-.456.061-.746.231-.243 1.908-1.312z""";

    private BetaTesterProviderMark() {
    }

    /**
     * 사용 모델 조합에 맞는 로고 SVG 조각을 반환한다.
     *
     * @param isClaudeUse 베타 기간에 claude 계열 모델을 사용했는지
     * @param isGptUse    베타 기간에 gpt 계열 모델을 사용했는지
     * @param colors      현재 팔레트 (OpenAI 로고 fill에 사용)
     * @return 로고 SVG 조각, 둘 다 false면 빈 문자열
     * @Since 2026-08-23
     */
    static String render(boolean isClaudeUse, boolean isGptUse, BetaTesterColors colors) {
        if (isClaudeUse && isGptUse) {
            return mark(PAIR_LEFT_X, CLAUDE_MARK, CLAUDE_FILL)
                    + mark(PAIR_RIGHT_X, SvgLogo.OPENAI_PATH, colors.textPrimary());
        }
        if (isClaudeUse) {
            return mark(SINGLE_X, CLAUDE_MARK, CLAUDE_FILL);
        }
        if (isGptUse) {
            return mark(SINGLE_X, SvgLogo.OPENAI_PATH, colors.textPrimary());
        }
        return "";
    }

    private static String mark(String x, String path, String fill) {
        return "<g transform=\"translate(%s,%d) scale(%s)\"><path d=\"%s\" fill=\"%s\"/></g>"
                .formatted(x, MARK_Y, MARK_SCALE, path, fill);
    }
}
