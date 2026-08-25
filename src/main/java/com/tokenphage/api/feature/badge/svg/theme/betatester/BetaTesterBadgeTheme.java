package com.tokenphage.api.feature.badge.svg.theme.betatester;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.BadgeTheme;
import com.tokenphage.api.feature.badge.svg.SvgText;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * macOS 터미널 창 모양의 베타 테스터 배지 (310×134, 자격 부여 전용, 단독 테마).
 * <p>
 * 표시 값 전부를 고정 스냅샷(BADGE_SNAPSHOT)에서 읽어 렌더 시점 집계가 없다.
 * 좌표·색은 badge-basic.svg.txt 실측값. 애니메이션은 reduced-motion 지원과
 * 타이핑 효과(clip-path + steps) 재현을 위해 SMIL 대신 인라인 CSS를 쓴다.
 */
@Slf4j
@Component
public class BetaTesterBadgeTheme implements BadgeTheme {

    /** 본문 좌측 원점 x — 커서 위치 계산의 기준. */
    private static final int BODY_X = 95;

    /** 유저명(16px 모노) 글리프 폭 — 프로토타입 7자 커서 x=176 역산 (95 + 8×10.125). */
    private static final double NAME_CHAR_WIDTH = 10.125;

    /** 유저명 표시 상한 — 19자일 때 커서 x=297.5로 우측 경계(298) 안. */
    private static final int MAX_NAME_CHARS = 19;

    /** 커서 rect 상단 y — 프로토타입 54에서 상단 여백 정리로 12 올렸다. */
    private static final int CURSOR_Y = 42;

    /** 모노스페이스 폰트 스택 — 터미널 정체성 유지 (프로토타입 그대로, 기존 Pretendard 스택 미사용). */
    private static final String FONT = "'SF Mono',SFMono-Regular,Menlo,Consolas,'Liberation Mono',monospace";

    /** 기간 텍스트 시작 x — 배너("BETA TESTER" 10px 모노 11자) 우측 끝 161 + 여백 8. */
    private static final int PERIOD_X = 169;

    /** 통계 3칸 x — 라벨 7px 폭(BADGE SERVED 12자 = 57.6)이 다음 열을 침범하지 않는 간격. */
    private static final int[] STAT_XS = {95, 152, 224};

    /** 통계 라벨 3종 (STAT_XS와 같은 순서). */
    private static final String[] STAT_LABELS = {"SYNCS RUN", "BADGE SERVED", "TOKENS ADDED"};

    /** macOS 신호등 색 (좌→우, 프로토타입 실측). */
    private static final String TRAFFIC_RED = "#FF5F57";
    private static final String TRAFFIC_YELLOW = "#FEBC2E";
    private static final String TRAFFIC_GREEN = "#28C840";

    /**
     * 테마 식별자를 반환한다.
     *
     * @return "beta-tester"
     * @Since 2026-08-23
     */
    @Override
    public String name() {
        return BadgeCode.BETA_TESTER.getCode();
    }

    /**
     * 고정 스냅샷 하나만 요구한다 — 라이브 집계 데이터를 쓰지 않는다.
     *
     * @return BADGE_SNAPSHOT 단일 집합
     * @Since 2026-08-23
     */
    @Override
    public Set<BadgeDataNeed> needs() {
        return EnumSet.of(BadgeDataNeed.BADGE_SNAPSHOT);
    }

    /**
     * 지원 색상 모드 — 악센트 3종. light/dark는 지원하지 않는다 (배경이 항상 다크다).
     *
     * @return CYAN·GREEN·PURPLE
     * @Since 2026-08-23
     */
    @Override
    public Set<BadgeMode> supportedModes() {
        return EnumSet.of(BadgeMode.CYAN, BadgeMode.GREEN, BadgeMode.PURPLE);
    }

    /**
     * 기본 모드 — cyan.
     *
     * @return CYAN
     * @Since 2026-08-23
     */
    @Override
    public BadgeMode defaultMode() {
        return BadgeMode.CYAN;
    }

    /**
     * 스냅샷 데이터로 터미널 배지 SVG를 생성한다.
     * <p>
     * 스냅샷이 비어 있으면 통계 3칸은 "-", 순번·기간은 미표시로 그린다(잠금 배지로 바꾸지 않는다).
     *
     * @param data 배지 데이터 (snapshot 필드 사용, null 불허)
     * @param mode 색상 모드 (지원 집합 밖이면 cyan으로 접힘)
     * @return 완성된 SVG 마크업 문자열
     * @Since 2026-08-23
     */
    @Override
    public String build(BadgeResponse data, BadgeMode mode) {
        BadgeMode resolved = supportedModes().contains(mode) ? mode : defaultMode();
        BetaTesterColors c = BetaTesterColors.forMode(resolved);
        BetaTesterSnapshot snap = BetaTesterSnapshot.parse(data.snapshot());
        String modeKey = resolved.getCode();
        String displayName = truncateName(data.username());
        log.debug("Building beta tester badge: user={}, mode={}", data.username(), modeKey);

        StringBuilder sb = new StringBuilder();
        appendRoot(sb, data.username());
        appendStyle(sb, modeKey);
        appendDefs(sb, modeKey, c);
        sb.append("<a href=\"%s\" xlink:href=\"%s\">".formatted(SvgText.LINK_URL, SvgText.LINK_URL));
        sb.append("<g clip-path=\"url(#tp-bt-clip-%s)\">".formatted(modeKey));
        appendChrome(sb, c);
        sb.append(BetaTesterMascot.render(resolved));
        sb.append(BetaTesterProviderMark.render(snap.isClaudeUse(), snap.isGptUse(), c));
        appendSidebarRank(sb, snap, c);
        appendBanner(sb, snap, c);
        appendUsername(sb, displayName, modeKey, c);
        appendStats(sb, snap, c);
        appendPrompt(sb, modeKey, c);
        sb.append(("<rect class=\"tp-scan-%s\" x=\"0\" y=\"18\" width=\"310\" height=\"12\" "
                + "fill=\"url(#tp-bt-scanline-%s)\"/>").formatted(modeKey, modeKey));
        sb.append("</g>");
        sb.append("<rect x=\"0.5\" y=\"0.5\" width=\"309\" height=\"121\" rx=\"7\" fill=\"none\" stroke=\"%s\"/>"
                .formatted(c.border()));
        sb.append("</a></svg>");
        return sb.toString();
    }

    /**
     * 루트 svg. 캔버스는 310×122, 표시 크기는 410×161.
     * 프로토타입 148에서 상·하 여백을 각 11~12로 맞추며 26을 줄였고 본문은 12 올렸다.
     */
    private void appendRoot(StringBuilder sb, String username) {
        sb.append("""
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
                     width="410" height="161" viewBox="0 0 310 122" role="img" aria-label="Tokenphage beta tester %s">
                """.formatted(SvgText.escape(username)));
    }

    /**
     * 커서 깜빡임·스캔라인·타이핑 keyframes. 클래스·keyframes 이름에 모드 접미사를 붙여
     * 한 페이지 다중 임베드 시 충돌을 막고, prefers-reduced-motion에서 전부 정지한다.
     */
    private void appendStyle(StringBuilder sb, String modeKey) {
        sb.append("""
                <style>
                .tp-blink-%1$s{animation:tp-blink-%1$s 1.1s steps(1) infinite}
                @keyframes tp-blink-%1$s{0%%,49%%{opacity:1}50%%,100%%{opacity:0}}
                .tp-scan-%1$s{animation:tp-scan-%1$s 4.4s cubic-bezier(.4,0,.6,1) infinite}
                @keyframes tp-scan-%1$s{0%%{transform:translateY(0);opacity:0}30%%{opacity:.5}100%%{transform:translateY(92px);opacity:0}}
                .tp-type-%1$s{animation:tp-type-%1$s 6s steps(34,end) infinite}
                @keyframes tp-type-%1$s{0%%,55%%{clip-path:inset(-2px -2px -2px -2px)}60%%,64%%{clip-path:inset(-2px 100%% -2px -2px)}92%%,100%%{clip-path:inset(-2px -2px -2px -2px)}}
                @media (prefers-reduced-motion:reduce){.tp-blink-%1$s,.tp-scan-%1$s,.tp-type-%1$s{animation:none}}
                </style>
                """.formatted(modeKey));
    }

    private void appendDefs(StringBuilder sb, String modeKey, BetaTesterColors c) {
        sb.append("""
                <defs>
                <clipPath id="tp-bt-clip-%1$s"><rect x="0.5" y="0.5" width="309" height="121" rx="7"/></clipPath>
                <linearGradient id="tp-bt-scanline-%1$s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="%2$s" stop-opacity="0"/><stop offset="1" stop-color="%2$s" stop-opacity=".14"/></linearGradient>
                </defs>
                """.formatted(modeKey, c.accent()));
    }

    private void appendChrome(StringBuilder sb, BetaTesterColors c) {
        sb.append("""
                <rect width="310" height="122" fill="%s"/>
                <rect width="310" height="18" fill="%s"/>
                <line x1="0" y1="18.5" x2="310" y2="18.5" stroke="%s"/>
                <circle cx="11" cy="9" r="3" fill="%s"/><circle cx="22" cy="9" r="3" fill="%s"/><circle cx="33" cy="9" r="3" fill="%s"/>
                <text x="43" y="11.8" font-family="%s" font-size="8.5" fill="%s" letter-spacing=".3">tokenphage — beta</text>
                <line x1="83.5" y1="18" x2="83.5" y2="122" stroke="%s"/>
                """.formatted(c.bg(), c.titleBar(), c.divider(),
                TRAFFIC_RED, TRAFFIC_YELLOW, TRAFFIC_GREEN,
                FONT, c.textSecondary(), c.divider()));
    }

    private void appendSidebarRank(StringBuilder sb, BetaTesterSnapshot snap, BetaTesterColors c) {
        if (snap.signupRank() <= 0) {
            return;
        }
        sb.append("""
                <text x="41.5" y="107" font-family="%s" font-size="9" font-weight="700" fill="%s" text-anchor="middle" letter-spacing=".3">#%d</text>
                """.formatted(FONT, c.accent(), snap.signupRank()));
    }

    private void appendBanner(StringBuilder sb, BetaTesterSnapshot snap, BetaTesterColors c) {
        sb.append("""
                <text x="95" y="37" font-family="%s" font-size="10" font-weight="700" fill="%s">BETA <tspan fill="%s">TESTER</tspan></text>
                """.formatted(FONT, c.textPrimary(), c.accent()));
        if (!snap.period().isBlank()) {
            // 배너와 같은 베이스라인에 이어 붙인다 (우측 끝 정렬에서 변경).
            sb.append("""
                    <text x="%d" y="37" font-family="%s" font-size="7.5" fill="%s" letter-spacing=".4">%s</text>
                    """.formatted(PERIOD_X, FONT, c.textMuted(), SvgText.escape(snap.period())));
        }
    }

    private void appendUsername(StringBuilder sb, String displayName, String modeKey, BetaTesterColors c) {
        sb.append("""
                <text x="95" y="55" font-family="%s" font-size="16" font-weight="700" fill="%s"><tspan fill="%s">@</tspan>%s</text>
                """.formatted(FONT, c.textPrimary(), c.accent(), SvgText.escape(displayName)));
        // 커서 x = 95 + (@ 포함 글리프 수) × 글리프 폭 — 7자에서 프로토타입 실측 176과 일치
        double cursorX = BODY_X + (displayName.length() + 1) * NAME_CHAR_WIDTH;
        sb.append("""
                <rect class="tp-blink-%s" x="%s" y="%d" width="5" height="14" fill="%s"/>
                <line x1="95" y1="63.5" x2="298" y2="63.5" stroke="%s"/>
                """.formatted(modeKey, trimNumber(cursorX), CURSOR_Y, c.accent(), c.divider()));
    }

    private void appendStats(StringBuilder sb, BetaTesterSnapshot snap, BetaTesterColors c) {
        sb.append("""
                <text x="95" y="72" font-family="%s" font-size="6.5" fill="%s" letter-spacing=".9">CONTRIBUTED DURING BETA</text>
                """.formatted(FONT, c.textMuted()));
        // 스냅샷 문자열을 그대로 출력한다 — 표기가 적재 시점에 고정돼 formatTokens를 쓰지 않는다.
        // DB에서 온 자유 문자열이라 전부 escape를 통과시킨다.
        String[] values = {snap.syncsRun(), snap.badgeServed(), snap.tokensAdded()};
        for (int i = 0; i < STAT_XS.length; i++) {
            sb.append("""
                    <text x="%d" y="84" font-family="%s" font-size="11" font-weight="700" fill="%s">%s</text>
                    <text x="%d" y="93" font-family="%s" font-size="7" fill="%s" letter-spacing=".6">%s</text>
                    """.formatted(STAT_XS[i], FONT, c.accent(), SvgText.escape(values[i]),
                    STAT_XS[i], FONT, c.textSecondary(), STAT_LABELS[i]));
        }
    }

    private void appendPrompt(StringBuilder sb, String modeKey, BetaTesterColors c) {
        sb.append("""
                <text x="95" y="109" font-family="%s" font-size="8" fill="%s">$</text>
                <text class="tp-type-%s" x="103" y="109" font-family="%s" font-size="8" fill="%s">echo "Thanks for shaping the beta."</text>
                """.formatted(FONT, c.accent(), modeKey, FONT, c.textBody()));
    }

    private String truncateName(String username) {
        if (username.length() <= MAX_NAME_CHARS) {
            return username;
        }
        return username.substring(0, MAX_NAME_CHARS - 1) + "…";
    }

    /**
     * 좌표 수치를 SVG 속성 문자열로 만든다 — 정수면 소수점 없이(176), 아니면 그대로(297.5).
     */
    private static String trimNumber(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }
}
