package com.tokenphage.api.feature.badge.svg.theme.locked;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeTheme;
import com.tokenphage.api.feature.badge.svg.SvgText;
import com.tokenphage.api.feature.badge.svg.theme.card.CardColors;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 자격이 없어 쓸 수 없는 배지임을 알리는 안내 배지.
 * <p>
 * CardBadgeTheme을 상속하지 않는다 — build()가 final이고 사용자 데이터 기반 레이아웃이라 맞지 않는다.
 * needs()가 비어 있어 조회 서비스가 쿼리를 실행하지 않는다.
 */
@Component
public class LockedBadgeTheme implements BadgeTheme {

    /** ?theme=locked 로 직접 호출된 경우의 기본 제목. */
    public static final String DEFAULT_TITLE = "LOCKED BADGE";

    /** 어떤 배지 때문인지 알 수 없을 때의 기본 안내. */
    public static final String DEFAULT_MESSAGE = "TokenPhage 저장소에서 배지 획득 조건을 확인해 주세요";

    /**
     * 테마 식별자를 반환한다.
     *
     * @return "locked"
     * @Since 2026-08-10
     */
    @Override
    public String name() {
        return BadgeCode.LOCKED.getCode();
    }

    /**
     * 그리는 데 필요한 데이터가 없음을 알린다.
     *
     * @return 빈 목록
     * @Since 2026-08-10
     */
    @Override
    public Set<BadgeDataNeed> needs() {
        return Set.of();
    }

    /**
     * 기본 안내 문구로 그린다.
     * <p>
     * ?theme=locked 직접 호출 경로. 자격 거부 시에는 {@link #render}가 배지별 문구를 받아 그린다.
     *
     * @param data   쓰지 않는다
     * @param isDark true면 다크 모드
     * @return 기본 문구 잠금 안내 SVG
     * @Since 2026-08-10
     */
    @Override
    public String build(BadgeResponse data, boolean isDark) {
        return render(DEFAULT_TITLE, DEFAULT_MESSAGE, isDark);
    }

    /**
     * 배지별 안내 문구를 담은 잠금 SVG를 만든다.
     * <p>
     * title/message가 null이거나 공백이면 기본 문구로 대체한다.
     *
     * @param title   잠금 안내 제목 (배지 이름, null 허용)
     * @param message 잠금 안내 문구 (null 허용)
     * @param isDark  true면 다크 모드
     * @return 잠금 안내 SVG 마크업
     * @Since 2026-08-10
     */
    public String render(String title, String message, boolean isDark) {
        CardColors modeColor = isDark ? CardColors.DARK : CardColors.LIGHT;
        String safeTitle = SvgText.escape(blankToDefault(title, DEFAULT_TITLE));
        String safeMessage = SvgText.escape(blankToDefault(message, DEFAULT_MESSAGE));

        StringBuilder sb = new StringBuilder();
        sb.append("""
            <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
                 width="540" height="210" viewBox="0 0 540 210" role="img" aria-label="Locked badge %s">
            """.formatted(safeTitle));

        // 배지 전체를 프로젝트 저장소 링크로 감싼다. 획득 조건을 확인하러 갈 수 있어야 한다.
        sb.append("""
            <a href="%s" xlink:href="%s">
            """.formatted(SvgText.LINK_URL, SvgText.LINK_URL));

        sb.append("""
            <rect width="540" height="210" rx="16" fill="%s"/>
            <rect x="0.5" y="0.5" width="539" height="209" rx="15.5" fill="none" stroke="%s"/>
            """.formatted(modeColor.bg(), modeColor.divider()));

        // 자물쇠 아이콘 — 도메인 용어는 grant지만 사용자에게 보이는 표현은 잠금으로 유지한다.
        sb.append("""
            <g transform="translate(246, 52)">
              <path d="M14 20 v-7 a10 10 0 0 1 20 0 v7" fill="none" stroke="%s" stroke-width="5"/>
              <rect x="6" y="20" width="36" height="28" rx="6" fill="%s"/>
              <circle cx="24" cy="33" r="4" fill="%s"/>
            </g>
            """.formatted(modeColor.textSecondary(), modeColor.textSecondary(), modeColor.bg()));

        sb.append("""
            <text x="270" y="136" text-anchor="middle" font-family="Pretendard,system-ui,sans-serif"
                  font-size="20" fill="%s" font-weight="700">%s</text>
            """.formatted(modeColor.textPrimary(), safeTitle));

        sb.append("""
            <text x="270" y="164" text-anchor="middle" font-family="Pretendard,system-ui,sans-serif"
                  font-size="12" fill="%s" font-weight="500">%s</text>
            """.formatted(modeColor.textSecondary(), safeMessage));

        sb.append("</a></svg>");
        return sb.toString();
    }

    /**
     * null이거나 공백이면 기본값으로 대체한다.
     */
    private String blankToDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
