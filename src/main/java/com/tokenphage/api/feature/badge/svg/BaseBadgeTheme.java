package com.tokenphage.api.feature.badge.svg;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 마스코트를 제외한 공통 배지 레이아웃(배경·유저명·캐시 적중률·누적 토큰·히트바·Top5 모델)을 담당하는 추상 테마.
 * <p>
 * {@link #build}는 템플릿 메서드로 전체 SVG 골격을 고정하고, 테마별로 달라지는 마스코트만 {@link #mascot}로 위임한다.
 * 유저명 아래 보조 캡션은 {@link #subCaption} 훅으로 노출하며 기본은 미표시다.
 */
@Slf4j
public abstract class BaseBadgeTheme implements BadgeTheme {

    /**
     * 공통 레이아웃에 테마별 마스코트를 끼워 완성된 배지 SVG를 생성한다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark true면 다크 모드, false면 라이트 모드
     * @return 완성된 SVG 마크업 문자열
     * @Since 2026-05-31
     */
    @Override
    public final String build(BadgeResponse data, boolean isDark) {

        BadgeColors modeColor = isDark ? BadgeColors.DARK : BadgeColors.LIGHT;
        log.debug("Building badge: user={}, theme={}, isDark={}", data.username(), name(), isDark);

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <svg xmlns="http://www.w3.org/2000/svg" width="540" height="210"
                 viewBox="0 0 540 210" role="img" aria-label="TokenBadge %s">
            """.formatted(escape(data.username())));

        appendBackground(sb, isDark, modeColor);
        sb.append(extraDefs(data, isDark));
        sb.append(mascot(data, isDark));

        sb.append("""
            <text x="102" y="44" font-family="Pretendard,system-ui,sans-serif"
                  font-size="16" fill="%s" font-weight="700">@%s</text>
            """.formatted(modeColor.textPrimary(), escape(data.username())));

        String caption = subCaption(data);
        if (!caption.isBlank()) {
            sb.append("""
                <text x="102" y="62" font-family="Pretendard,system-ui,sans-serif"
                      font-size="10" fill="%s" font-weight="500">%s</text>
                """.formatted(modeColor.textSecondary(), escape(caption)));
        }

        appendCacheHitRate(sb, data.cacheHitRate(), isDark, modeColor);

        sb.append("""
            <line x1="20" y1="76" x2="520" y2="76" stroke="%s" stroke-width="1"/>
            """.formatted(modeColor.divider()));

        appendTokenSection(sb, data, isDark, modeColor);

        sb.append("<g transform=\"translate(30, 164)\">");
        sb.append(buildHeatbar(data.heatbar(), isDark, modeColor));
        sb.append("</g>");

        sb.append("""
            <line x1="280" y1="88" x2="280" y2="196" stroke="%s" stroke-width="1"/>
            """.formatted(modeColor.divider()));

        appendTopModels(sb, data.topModels(), modeColor);

        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * 테마별 마스코트 SVG 조각을 반환한다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark true면 다크 모드, false면 라이트 모드
     * @return 마스코트 SVG 조각
     * @Since 2026-05-31
     */
    protected abstract String mascot(BadgeResponse data, boolean isDark);

    /**
     * 유저명 아래에 표시할 보조 캡션을 반환한다. 기본은 빈 문자열(미표시)이다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @return 캡션 문자열, 미표시면 빈 문자열
     * @Since 2026-05-31
     */
    protected String subCaption(BadgeResponse data) {
        return "";
    }

    /**
     * 배경 직후 주입할 추가 정의(예: &lt;defs&gt; 그라데이션)를 반환한다. 기본은 빈 문자열이다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark 색상 모드
     * @return SVG 정의 조각, 없으면 빈 문자열
     * @Since 2026-05-31
     */
    protected String extraDefs(BadgeResponse data, boolean isDark) {
        return "";
    }

    /**
     * 누적 토큰 숫자의 fill 값을 반환한다. 기본은 기본 텍스트 색이다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark 색상 모드
     * @param modeColor 현재 색상 팔레트
     * @return fill 속성 값 (색상 또는 url(#id))
     * @Since 2026-05-31
     */
    protected String tokenFill(BadgeResponse data, boolean isDark, BadgeColors modeColor) {
        return modeColor.textPrimary();
    }

    private void appendBackground(StringBuilder sb, boolean isDark, BadgeColors modeColor) {
        if (isDark) {
            sb.append("""
                <rect width="540" height="210" fill="%s" rx="12"/>
                """.formatted(modeColor.bg()));
        } else {
            sb.append("""
                <rect width="540" height="210" fill="%s" stroke="#e5e7eb"
                      stroke-width="1" rx="12"/>
                """.formatted(modeColor.bg()));
        }
    }

    private void appendCacheHitRate(StringBuilder sb, double cacheHitRate, boolean isDark, BadgeColors modeColor) {
        String cacheColor = cacheColor(cacheHitRate, isDark);
        sb.append("""
            <text x="515" y="38" font-family="Pretendard,system-ui,sans-serif"
                  font-size="10" fill="%s" font-weight="500" letter-spacing="0.6"
                  text-anchor="end">🔥 CACHE HIT RATE</text>
            <text x="515" y="62" font-family="Pretendard,system-ui,sans-serif"
                  font-size="22" fill="%s" font-weight="700"
                  text-anchor="end">%s</text>
            """.formatted(modeColor.textSecondary(), cacheColor, formatPercent(cacheHitRate)));
    }

    private void appendTokenSection(StringBuilder sb, BadgeResponse data, boolean isDark, BadgeColors modeColor) {
        String total = SvgBuilder.formatTokens(data.totalTokens());
        sb.append("""
            <text x="30" y="100" font-family="Pretendard,system-ui,sans-serif"
                  font-size="11" fill="%s" font-weight="500" letter-spacing="0.6">TOKEN 누적</text>
            <text x="30" y="132" font-family="Pretendard,system-ui,sans-serif"
                  font-size="32" fill="%s" font-weight="700">%s</text>
            <text x="%d" y="132" font-family="Pretendard,system-ui,sans-serif"
                  font-size="14" fill="%s" font-weight="500">tokens</text>
            <text x="30" y="157" font-family="Pretendard,system-ui,sans-serif"
                  font-size="10" fill="%s" font-weight="500" letter-spacing="0.5">최근 30일</text>
            """.formatted(
                modeColor.textSecondary(),
                tokenFill(data, isDark, modeColor), total,
                30 + total.length() * 21 + 1,
                modeColor.textSecondary(),
                modeColor.textSecondary()));
    }

    private void appendTopModels(StringBuilder sb, List<ModelCountResponse> models, BadgeColors modeColor) {
        sb.append("""
            <text x="300" y="100" font-family="Pretendard,system-ui,sans-serif"
                  font-size="11" fill="%s" font-weight="500" letter-spacing="0.6">Top 5 Models</text>
            """.formatted(modeColor.textSecondary()));

        int[] rowYPositions = {122, 140, 158, 176, 194};
        String[] rankFills = {"#5a9ab0", "#f59e0b", "#94a3b8", "#cd7c2f", "#6b7280"};
        for (int i = 0; i < rowYPositions.length; i++) {
            int y = rowYPositions[i];
            sb.append("""
                <circle cx="307" cy="%d" r="7" fill="%s"/>
                <text x="307" y="%d" font-family="Pretendard,system-ui,sans-serif"
                      font-size="9" fill="#ffffff" font-weight="700"
                      text-anchor="middle" dominant-baseline="central">%d</text>
                """.formatted(y - 4, rankFills[i], y - 4, i + 1));
            if (i < models.size()) {
                ModelCountResponse m = models.get(i);
                String nameColor = i == 0 ? modeColor.textPrimary() : modeColor.textSecondary();
                String valWeight = i == 0 ? "700" : "500";
                sb.append("""
                    <text x="321" y="%d" font-family="Pretendard,system-ui,sans-serif"
                          font-size="12" fill="%s" font-weight="500">%s</text>
                    <text x="520" y="%d" font-family="Pretendard,system-ui,sans-serif"
                          font-size="12" fill="%s" font-weight="%s" text-anchor="end">%s</text>
                    """.formatted(y, nameColor, escape(shortModel(m.model())),
                                  y, nameColor, valWeight, SvgBuilder.formatTokens(m.total())));
            } else {
                sb.append("""
                    <text x="321" y="%d" font-family="Pretendard,system-ui,sans-serif"
                          font-size="12" fill="%s" font-weight="500">--</text>
                    <text x="520" y="%d" font-family="Pretendard,system-ui,sans-serif"
                          font-size="12" fill="%s" font-weight="500" text-anchor="end">--</text>
                    """.formatted(y, modeColor.textSecondary(), y, modeColor.textSecondary()));
            }
        }
    }

    private String buildHeatbar(List<DailyCountResponse> heatbar, boolean isDark, BadgeColors modeColor) {
        // 데이터가 30일보다 적어도 항상 막대 30개를 그린다(없는 날은 최소 높이).
        int days = Math.max(heatbar.size(), 30);
        long max = heatbar.stream().mapToLong(DailyCountResponse::total).max().orElse(1);
        int maxBarH = 30;
        StringBuilder sb = new StringBuilder();
        sb.append("<line x1=\"0\" y1=\"%d\" x2=\"239\" y2=\"%d\" stroke=\"%s\" stroke-width=\"0.5\" opacity=\"0.4\"/>"
            .formatted(maxBarH, maxBarH, modeColor.divider()));
        for (int i = 0; i < days; i++) {
            long val = i < heatbar.size() ? heatbar.get(i).total() : 0;
            int bh = val == 0 ? 1 : Math.max(3, (int) Math.round((double) val / max * maxBarH));
            String fill = heatColor(val, max, isDark, modeColor);
            sb.append("<rect x=\"%d\" y=\"%d\" width=\"7\" height=\"%d\" fill=\"%s\" rx=\"2\"/>"
                .formatted(i * 8, maxBarH - bh, bh, fill));
        }
        return sb.toString();
    }

    /**
     * 히트바 한 칸의 색을 비율(val/max)에 따라 반환한다. 테마가 색 팔레트를 바꾸려면 오버라이드한다.
     *
     * @param val  해당 일의 토큰 수
     * @param max  30일 중 최대 토큰 수
     * @param isDark 색상 모드
     * @param modeColor 기본 색상 팔레트
     * @return 막대 fill 색
     * @Since 2026-05-31
     */
    protected String heatColor(long val, long max, boolean isDark, BadgeColors modeColor) {
        if (max == 0 || val == 0) {
            return modeColor.heatLow();
        }
        Double ratio = (double) val / max;
        return switch (ratio) {
            case Double r when r < 0.2 -> modeColor.heatLow();
            case Double r when r < 0.4 -> modeColor.heatMid1();
            case Double r when r < 0.6 -> modeColor.heatMid2();
            case Double r when r < 0.8 -> modeColor.heatMid3();
            default -> modeColor.heatHigh();
        };
    }

    private String shortModel(String model) {
        // claude- 접두사, -latest 접미사, 끝의 -YYYYMMDD 날짜 스냅샷을 제거한다.
        return model.replace("claude-", "").replace("-latest", "").replaceAll("-\\d{8}$", "");
    }

    private String formatPercent(double rate) {
        return String.format("%.1f%%", rate * 100);
    }

    private String cacheColor(double rate, boolean isDark) {
        if (rate >= 0.7) {
            return isDark ? "#4ade80" : "#16a34a";
        }
        if (rate >= 0.4) {
            return isDark ? "#fbbf24" : "#d97706";
        }
        return isDark ? "#94a3b8" : "#6b7280";
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
