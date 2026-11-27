package com.tokenphage.api.feature.badge.svg.theme.odometer;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.BadgeTheme;
import com.tokenphage.api.feature.badge.svg.SvgText;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;

/**
 * 누적 에이전트 가동시간을 주행거리계 모양(300×64)으로 그리는 배지 패밀리의 공통 레이아웃.
 * <p>
 * {@link #build}가 전체 골격을 고정하고, 테마마다 다른 마스코트와 색은 {@link #mascot}·{@link #colors}로 위임한다.
 */
@Slf4j
public abstract class OdometerBadgeTheme implements BadgeTheme {

    private static final String SANS_FONT = "Pretendard,system-ui,-apple-system,'Apple SD Gothic Neo',sans-serif";

    /** 숫자 창 위아래를 어둡게 눌러 바퀴가 원통처럼 보이게 하는 음영. 모드·테마와 무관하다. */
    private static final String SHADE_STOPS =
            "<stop offset=\"0\" stop-color=\"#000\" stop-opacity=\".88\"/>"
                    + "<stop offset=\".34\" stop-color=\"#000\" stop-opacity=\"0\"/>"
                    + "<stop offset=\".66\" stop-color=\"#000\" stop-opacity=\"0\"/>"
                    + "<stop offset=\"1\" stop-color=\"#000\" stop-opacity=\".88\"/>";

    /**
     * 주행거리계 패밀리가 렌더링에 쓰는 데이터를 선언한다.
     *
     * @return 누적 가동시간 하나
     * @Since 2026-11-13
     */
    @Override
    public final Set<BadgeDataNeed> needs() {
        return EnumSet.of(BadgeDataNeed.TOTAL_RUNTIME_MS);
    }

    /**
     * 지원 색상 모드를 선언한다.
     *
     * @return LIGHT·DARK
     * @Since 2026-11-13
     */
    @Override
    public final Set<BadgeMode> supportedModes() {
        return EnumSet.of(BadgeMode.LIGHT, BadgeMode.DARK);
    }

    /**
     * 기본 색상 모드를 반환한다.
     *
     * @return LIGHT
     * @Since 2026-11-13
     */
    @Override
    public final BadgeMode defaultMode() {
        return BadgeMode.LIGHT;
    }

    /**
     * 주행거리계 배지 SVG를 생성한다.
     * <p>
     * 사용자명은 그림에 넣지 않고 aria-label에만 넣는다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @param mode 색상 모드 (DARK 외에는 light로 그린다)
     * @return 완성된 SVG 마크업 문자열
     * @Since 2026-11-13
     */
    @Override
    public final String build(BadgeResponse data, BadgeMode mode) {
        boolean isDark = mode == BadgeMode.DARK;
        log.debug("Building odometer badge: user={}, theme={}, isDark={}", data.username(), name(), isDark);

        StringBuilder sb = new StringBuilder();
        sb.append(("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                + " width=\"300\" height=\"64\" viewBox=\"0 0 300 64\" role=\"img\""
                + " aria-label=\"Agent runtime odometer %s\">").formatted(SvgText.escape(data.username())));

        // 배지 전체를 프로젝트 저장소 링크로 감싼다. 몸통 rect가 전면을 덮어 어디를 눌러도 이동한다.
        sb.append("<a href=\"%s\" xlink:href=\"%s\">".formatted(SvgText.LINK_URL, SvgText.LINK_URL));

        // 한 페이지에 여러 배지가 놓여도 겹치지 않게 테마·모드별 id를 쓴다.
        // SMIL은 begin 값의 '-'를 뺄셈으로 읽으므로 id에서 '-'를 뺀다.
        String id = name().replace("-", "") + (isDark ? BadgeMode.DARK : BadgeMode.LIGHT).getCode();
        OdometerColors colors = colors(isDark);

        appendDefs(sb, id, isDark);
        appendBody(sb, id, isDark, colors);
        sb.append(mascot(data, isDark));

        sb.append(label(29, colors.fg(), "AGENT"));
        sb.append(label(43, colors.tint(), "RUNTIME"));

        sb.append("<rect x=\"121\" y=\"10\" width=\"142\" height=\"44\" rx=\"5\" fill=\"#000\" stroke=\"%s\"/>"
                .formatted(colors.windowStroke()));
        sb.append("<g clip-path=\"url(#%swin)\">".formatted(id));
        sb.append(OdometerWheels.render(data.totalRuntimeMs(), colors.accent()));
        sb.append("<rect x=\"124\" y=\"13\" width=\"136\" height=\"38\" fill=\"url(#%sshade)\"/>".formatted(id));
        appendGlint(sb, id);
        sb.append("</g>");

        sb.append("<text x=\"271\" y=\"41\" font-family=\"%s\" font-size=\"18\" font-weight=\"800\" fill=\"%s\" text-anchor=\"start\">h</text>"
                .formatted(SANS_FONT, colors.fg()));

        sb.append("</a></svg>");
        return sb.toString();
    }

    // 몸통 그라데이션은 dark에서만 쓰므로 light에는 싣지 않는다.
    private void appendDefs(StringBuilder sb, String id, boolean isDark) {
        sb.append("<defs>");
        if (isDark) {
            sb.append("<linearGradient id=\"%sbg\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">".formatted(id));
            sb.append("<stop offset=\"0\" stop-color=\"#2d323c\"/><stop offset=\"1\" stop-color=\"#1c2027\"/></linearGradient>");
        }
        sb.append("<linearGradient id=\"%sshade\" x1=\"0\" y1=\"0\" x2=\"0\" y2=\"1\">%s</linearGradient>"
                .formatted(id, SHADE_STOPS));
        sb.append("<clipPath id=\"%swin\"><rect x=\"124\" y=\"13\" width=\"136\" height=\"38\" rx=\"2\"/></clipPath>"
                .formatted(id));
        sb.append("</defs>");
    }

    private void appendBody(StringBuilder sb, String id, boolean isDark, OdometerColors colors) {
        if (isDark) {
            sb.append("<rect width=\"300\" height=\"64\" rx=\"12\" fill=\"url(#%sbg)\"/>".formatted(id));
            sb.append("<rect x=\".5\" y=\".5\" width=\"299\" height=\"63\" rx=\"11.5\" fill=\"none\" stroke=\"%s\" stroke-opacity=\".45\"/>"
                    .formatted(colors.accent()));
        } else {
            sb.append("<rect x=\".5\" y=\".5\" width=\"299\" height=\"63\" rx=\"11.5\" fill=\"#ffffff\" stroke=\"#e5e7eb\"/>");
        }
    }

    private String label(int y, String fill, String text) {
        return "<text x=\"60\" y=\"%d\" font-family=\"%s\" font-size=\"10\" font-weight=\"800\" fill=\"%s\" text-anchor=\"start\" letter-spacing=\"1.1\">%s</text>"
                .formatted(y, SANS_FONT, fill, text);
    }

    // 바퀴가 다 멈춘 뒤(3.2초) 창을 한 번 훑고, 끝난 지 6초마다 다시 훑는 반사광.
    // opacity 애니메이션은 x 애니메이션의 begin을 따라가야 함께 반복된다.
    private void appendGlint(StringBuilder sb, String id) {
        sb.append("<g transform=\"skewX(-18)\"><rect x=\"140\" y=\"8\" width=\"14\" height=\"48\" fill=\"#fff\" opacity=\"0\">");
        sb.append("<animate id=\"%sglint\" attributeName=\"x\" values=\"140;300\" dur=\"0.8s\" begin=\"3.2s;%sglint.end+6s\"/>"
                .formatted(id, id));
        sb.append("<animate attributeName=\"opacity\" values=\"0;.18;.18;0\" keyTimes=\"0;.1;.9;1\" dur=\"0.8s\" begin=\"%sglint.begin\"/>"
                .formatted(id));
        sb.append("</rect></g>");
    }

    /**
     * 테마별 마스코트 SVG 조각을 반환한다. 몸통 왼쪽(라벨 앞)에 놓인다.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark true면 dark 모드, false면 light 모드
     * @return 마스코트 SVG 조각
     * @Since 2026-11-13
     */
    protected abstract String mascot(BadgeResponse data, boolean isDark);

    /**
     * 테마의 모드별 색을 반환한다.
     *
     * @param isDark true면 dark 모드, false면 light 모드
     * @return 해당 모드의 색
     * @Since 2026-11-13
     */
    protected abstract OdometerColors colors(boolean isDark);
}
