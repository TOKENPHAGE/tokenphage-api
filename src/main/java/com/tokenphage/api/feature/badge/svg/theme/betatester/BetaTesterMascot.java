package com.tokenphage.api.feature.badge.svg.theme.betatester;

import com.tokenphage.api.feature.badge.svg.BadgeMode;

/**
 * 픽셀 파지 마스코트 (16×14 격자, rect 42개).
 * <p>
 * rect 블록은 badge-basic.svg.txt 프로토타입에서 스크립트로 추출했다
 * (rect 42개·좌우대칭·round-trip diff 0 검증 완료). 손으로 수정하지 않는다.
 * <p>
 * shape-rendering="crispEdges"를 넣지 않는다 — 프로토타입이 지정하지 않았고
 * scale(3.8125)가 비정수라 픽셀 행 높이가 불균등해진다 (의도적 예외).
 */
final class BetaTesterMascot {

    /** 배치 변환 — 프로토타입 translate(11,42) 기준 상단 여백 정리로 12 조정. */
    private static final String TRANSFORM = "translate(11,30) scale(3.8125)";

    /** 픽셀 rect 42개 — 본체 #F5F5F7, 음영(눈) #1E1E1E. */
    private static final String PIXEL_RECTS = """
            <rect x="4" y="0" width="2" height="1" fill="#F5F5F7"/><rect x="10" y="0" width="2" height="1" fill="#F5F5F7"/><rect x="4" y="1" width="2" height="1" fill="#F5F5F7"/>\
            <rect x="10" y="1" width="2" height="1" fill="#F5F5F7"/><rect x="2" y="2" width="12" height="1" fill="#F5F5F7"/><rect x="1" y="3" width="14" height="1" fill="#F5F5F7"/>\
            <rect x="0" y="4" width="16" height="1" fill="#F5F5F7"/><rect x="0" y="5" width="16" height="1" fill="#F5F5F7"/><rect x="0" y="6" width="2" height="1" fill="#F5F5F7"/>\
            <rect x="4" y="6" width="8" height="1" fill="#F5F5F7"/><rect x="14" y="6" width="2" height="1" fill="#F5F5F7"/><rect x="0" y="7" width="3" height="1" fill="#F5F5F7"/>\
            <rect x="5" y="7" width="6" height="1" fill="#F5F5F7"/><rect x="13" y="7" width="3" height="1" fill="#F5F5F7"/><rect x="0" y="8" width="4" height="1" fill="#F5F5F7"/>\
            <rect x="6" y="8" width="4" height="1" fill="#F5F5F7"/><rect x="12" y="8" width="4" height="1" fill="#F5F5F7"/><rect x="0" y="9" width="3" height="1" fill="#F5F5F7"/>\
            <rect x="5" y="9" width="6" height="1" fill="#F5F5F7"/><rect x="13" y="9" width="3" height="1" fill="#F5F5F7"/><rect x="0" y="10" width="2" height="1" fill="#F5F5F7"/>\
            <rect x="4" y="10" width="8" height="1" fill="#F5F5F7"/><rect x="14" y="10" width="2" height="1" fill="#F5F5F7"/><rect x="1" y="11" width="14" height="1" fill="#F5F5F7"/>\
            <rect x="1" y="12" width="2" height="1" fill="#F5F5F7"/><rect x="5" y="12" width="2" height="1" fill="#F5F5F7"/><rect x="9" y="12" width="2" height="1" fill="#F5F5F7"/>\
            <rect x="13" y="12" width="2" height="1" fill="#F5F5F7"/><rect x="1" y="13" width="2" height="1" fill="#F5F5F7"/><rect x="5" y="13" width="2" height="1" fill="#F5F5F7"/>\
            <rect x="9" y="13" width="2" height="1" fill="#F5F5F7"/><rect x="13" y="13" width="2" height="1" fill="#F5F5F7"/><rect x="2" y="6" width="2" height="1" fill="#1E1E1E"/>\
            <rect x="12" y="6" width="2" height="1" fill="#1E1E1E"/><rect x="3" y="7" width="2" height="1" fill="#1E1E1E"/><rect x="11" y="7" width="2" height="1" fill="#1E1E1E"/>\
            <rect x="4" y="8" width="2" height="1" fill="#1E1E1E"/><rect x="10" y="8" width="2" height="1" fill="#1E1E1E"/><rect x="3" y="9" width="2" height="1" fill="#1E1E1E"/>\
            <rect x="11" y="9" width="2" height="1" fill="#1E1E1E"/><rect x="2" y="10" width="2" height="1" fill="#1E1E1E"/><rect x="12" y="10" width="2" height="1" fill="#1E1E1E"/>""";

    private BetaTesterMascot() {
    }

    /**
     * 모드 팔레트의 글로우 필터를 입힌 마스코트 SVG 조각을 반환한다.
     *
     * @param mode 색상 모드 (악센트 3종 밖이면 cyan 팔레트로 접힘)
     * @return 마스코트 SVG 조각
     * @Since 2026-08-23
     */
    static String render(BadgeMode mode) {
        BetaTesterColors c = BetaTesterColors.forMode(mode);
        return "<g style=\"filter:%s\"><g transform=\"%s\">%s</g></g>"
                .formatted(c.mascotGlow(), TRANSFORM, PIXEL_RECTS);
    }
}
