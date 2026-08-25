package com.tokenphage.api.feature.badge.svg.theme.grass;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.BadgeTheme;
import com.tokenphage.api.feature.badge.svg.SvgText;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 잔디(contribution graph) 스타일 뱃지의 공통 레이아웃을 담당하는 추상 테마 (700x190).
 * <p>
 * {@link #build}는 템플릿 메서드로 헤더(username·streak 불꽃·연간 토큰)·하늘 놀이터·
 * 52주 잔디 그리드 골격을 고정하고, 테마별로 달라지는 마스코트는 {@link #mascot},
 * 팔레트는 {@link #colors}, 하늘 장식은 {@link #playground} 훅으로 위임한다.
 * <p>
 * 레이아웃 좌표·픽셀은 docs/svg-template 프로토타입 실측값이다. 단 svg 루트는
 * 프로토타입이 아니라 CardBadgeTheme을 미러링한다 — 프로토타입 루트에는 xmlns:xlink
 * 선언이 없어 그대로 쓰면 프로덕션 image/svg+xml XML 파싱이 깨진다.
 */
@Slf4j
public abstract class GrassBadgeTheme implements BadgeTheme {

    // ==== 하늘 놀이터 장식 (프로토타입 추출 — 손 전사 금지, .dev/grass-svg-extract.py --emit 산출물) ====

    /**
     * light 장식: 해(광선 line 8개)·구름·나무 3그루·풀포기 (하늘 배경/잔디띠 rect 제외).
     * <p>
     * 추출 후 수동 조정: 구름 4개에 느린 좌우 드리프트(animateTransform), 좌측 나무 아래
     * 풀포기(x369)를 g translate로 3px 내림 — rect 픽셀 좌표·색은 무변경.
     */
    private static final String DECOR_LIGHT = """
            <g><line x1="635.0" y1="18.1" x2="635.0" y2="15.7" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round"></line><line x1="642.9" y1="26.0" x2="645.3" y2="26.0" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round">\
            </line><line x1="635.0" y1="33.9" x2="635.0" y2="36.3" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round"></line>\
            <line x1="627.1" y1="26.0" x2="624.7" y2="26.0" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round"></line><line x1="641.3" y1="19.7" x2="643.3" y2="17.7" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round">\
            </line><line x1="641.3" y1="32.3" x2="643.3" y2="34.3" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round"></line>\
            <line x1="628.7" y1="32.3" x2="626.7" y2="34.3" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round"></line><line x1="628.7" y1="19.7" x2="626.7" y2="17.7" stroke="#ffce54" stroke-width="1.6" stroke-linecap="round">\
            </line></g><rect x="631" y="20" width="2" height="2" fill="#ffce54"></rect><rect x="633" y="20" width="2" height="2" fill="#ffce54">\
            </rect><rect x="635" y="20" width="2" height="2" fill="#ffce54"></rect><rect x="637" y="20" width="2" height="2" fill="#ffce54">\
            </rect><rect x="629" y="22" width="2" height="2" fill="#ffce54"></rect><rect x="631" y="22" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="633" y="22" width="2" height="2" fill="#ffe08a"></rect><rect x="635" y="22" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="637" y="22" width="2" height="2" fill="#ffe08a"></rect><rect x="639" y="22" width="2" height="2" fill="#ffce54">\
            </rect><rect x="629" y="24" width="2" height="2" fill="#ffce54"></rect><rect x="631" y="24" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="633" y="24" width="2" height="2" fill="#ffe08a"></rect><rect x="635" y="24" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="637" y="24" width="2" height="2" fill="#ffe08a"></rect><rect x="639" y="24" width="2" height="2" fill="#ffce54">\
            </rect><rect x="629" y="26" width="2" height="2" fill="#ffce54"></rect><rect x="631" y="26" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="633" y="26" width="2" height="2" fill="#ffe08a"></rect><rect x="635" y="26" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="637" y="26" width="2" height="2" fill="#ffe08a"></rect><rect x="639" y="26" width="2" height="2" fill="#ffce54">\
            </rect><rect x="629" y="28" width="2" height="2" fill="#ffce54"></rect><rect x="631" y="28" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="633" y="28" width="2" height="2" fill="#ffe08a"></rect><rect x="635" y="28" width="2" height="2" fill="#ffe08a">\
            </rect><rect x="637" y="28" width="2" height="2" fill="#ffe08a"></rect><rect x="639" y="28" width="2" height="2" fill="#ffce54">\
            </rect><rect x="631" y="30" width="2" height="2" fill="#ffce54"></rect><rect x="633" y="30" width="2" height="2" fill="#ffce54">\
            </rect><rect x="635" y="30" width="2" height="2" fill="#ffce54"></rect><rect x="637" y="30" width="2" height="2" fill="#ffce54">\
            </rect><g><animateTransform attributeName="transform" type="translate" values="0 0;40 0;0 0" dur="24s" repeatCount="indefinite"/>\
            <rect x="397" y="26" width="3" height="3" fill="#ffffff"></rect><rect x="400" y="26" width="3" height="3" fill="#ffffff">\
            </rect><rect x="403" y="26" width="3" height="3" fill="#ffffff"></rect><rect x="406" y="26" width="3" height="3" fill="#ffffff">\
            </rect><rect x="394" y="29" width="3" height="3" fill="#ffffff"></rect><rect x="397" y="29" width="3" height="3" fill="#ffffff">\
            </rect><rect x="400" y="29" width="3" height="3" fill="#ffffff"></rect><rect x="403" y="29" width="3" height="3" fill="#ffffff">\
            </rect><rect x="406" y="29" width="3" height="3" fill="#ffffff"></rect><rect x="409" y="29" width="3" height="3" fill="#ffffff">\
            </rect><rect x="391" y="32" width="3" height="3" fill="#ffffff"></rect><rect x="394" y="32" width="3" height="3" fill="#ffffff">\
            </rect><rect x="397" y="32" width="3" height="3" fill="#ffffff"></rect><rect x="400" y="32" width="3" height="3" fill="#ffffff">\
            </rect><rect x="403" y="32" width="3" height="3" fill="#ffffff"></rect><rect x="406" y="32" width="3" height="3" fill="#ffffff">\
            </rect><rect x="409" y="32" width="3" height="3" fill="#ffffff"></rect><rect x="412" y="32" width="3" height="3" fill="#ffffff">\
            </rect><rect x="394" y="35" width="3" height="3" fill="#ffffff"></rect><rect x="397" y="35" width="3" height="3" fill="#ffffff">\
            </rect><rect x="400" y="35" width="3" height="3" fill="#ffffff"></rect><rect x="403" y="35" width="3" height="3" fill="#ffffff">\
            </rect><rect x="406" y="35" width="3" height="3" fill="#ffffff"></rect><rect x="409" y="35" width="3" height="3" fill="#ffffff">\
            </rect></g><g><animateTransform attributeName="transform" type="translate" values="0 0;32 0;0 0" dur="22s" repeatCount="indefinite"/>\
            <rect x="545" y="30" width="4" height="4" fill="#ffffff"></rect><rect x="549" y="30" width="4" height="4" fill="#ffffff">\
            </rect><rect x="553" y="30" width="4" height="4" fill="#ffffff"></rect><rect x="541" y="34" width="4" height="4" fill="#ffffff">\
            </rect><rect x="545" y="34" width="4" height="4" fill="#ffffff"></rect><rect x="549" y="34" width="4" height="4" fill="#ffffff">\
            </rect><rect x="553" y="34" width="4" height="4" fill="#ffffff"></rect><rect x="557" y="34" width="4" height="4" fill="#ffffff">\
            </rect><rect x="545" y="38" width="4" height="4" fill="#ffffff"></rect><rect x="549" y="38" width="4" height="4" fill="#ffffff">\
            </rect><rect x="553" y="38" width="4" height="4" fill="#ffffff"></rect></g><g><animateTransform attributeName="transform" type="translate" values="0 0;45 0;0 0" dur="28s" repeatCount="indefinite"/>\
            <rect x="477" y="20" width="3" height="3" fill="#ffffff"></rect><rect x="480" y="20" width="3" height="3" fill="#ffffff">\
            </rect><rect x="483" y="20" width="3" height="3" fill="#ffffff"></rect><rect x="474" y="23" width="3" height="3" fill="#ffffff">\
            </rect><rect x="477" y="23" width="3" height="3" fill="#ffffff"></rect><rect x="480" y="23" width="3" height="3" fill="#ffffff">\
            </rect><rect x="483" y="23" width="3" height="3" fill="#ffffff"></rect><rect x="486" y="23" width="3" height="3" fill="#ffffff">\
            </rect><rect x="489" y="23" width="3" height="3" fill="#ffffff"></rect><rect x="471" y="26" width="3" height="3" fill="#ffffff">\
            </rect><rect x="474" y="26" width="3" height="3" fill="#ffffff"></rect><rect x="477" y="26" width="3" height="3" fill="#ffffff">\
            </rect><rect x="480" y="26" width="3" height="3" fill="#ffffff"></rect><rect x="483" y="26" width="3" height="3" fill="#ffffff">\
            </rect><rect x="486" y="26" width="3" height="3" fill="#ffffff"></rect><rect x="489" y="26" width="3" height="3" fill="#ffffff">\
            </rect><rect x="492" y="26" width="3" height="3" fill="#ffffff"></rect><rect x="471" y="29" width="3" height="3" fill="#ffffff">\
            </rect><rect x="474" y="29" width="3" height="3" fill="#ffffff"></rect><rect x="477" y="29" width="3" height="3" fill="#ffffff">\
            </rect><rect x="480" y="29" width="3" height="3" fill="#ffffff"></rect><rect x="483" y="29" width="3" height="3" fill="#ffffff">\
            </rect><rect x="486" y="29" width="3" height="3" fill="#ffffff"></rect><rect x="489" y="29" width="3" height="3" fill="#ffffff">\
            </rect><rect x="492" y="29" width="3" height="3" fill="#ffffff"></rect><rect x="474" y="32" width="3" height="3" fill="#ffffff">\
            </rect><rect x="477" y="32" width="3" height="3" fill="#ffffff"></rect><rect x="480" y="32" width="3" height="3" fill="#ffffff">\
            </rect><rect x="483" y="32" width="3" height="3" fill="#ffffff"></rect><rect x="486" y="32" width="3" height="3" fill="#ffffff">\
            </rect></g><g><animateTransform attributeName="transform" type="translate" values="0 0;-40 0;0 0" dur="26s" repeatCount="indefinite"/>\
            <rect x="644" y="24" width="3" height="3" fill="#ffffff"></rect><rect x="647" y="24" width="3" height="3" fill="#ffffff">\
            </rect><rect x="650" y="24" width="3" height="3" fill="#ffffff"></rect><rect x="641" y="27" width="3" height="3" fill="#ffffff">\
            </rect><rect x="644" y="27" width="3" height="3" fill="#ffffff"></rect><rect x="647" y="27" width="3" height="3" fill="#ffffff">\
            </rect><rect x="650" y="27" width="3" height="3" fill="#ffffff"></rect><rect x="653" y="27" width="3" height="3" fill="#ffffff">\
            </rect><rect x="644" y="30" width="3" height="3" fill="#ffffff"></rect><rect x="647" y="30" width="3" height="3" fill="#ffffff">\
            </rect><rect x="650" y="30" width="3" height="3" fill="#ffffff"></rect></g><rect x="369" y="24" width="4" height="4" fill="#77c043">\
            </rect><rect x="373" y="24" width="4" height="4" fill="#77c043"></rect><rect x="377" y="24" width="4" height="4" fill="#77c043">\
            </rect><rect x="365" y="28" width="4" height="4" fill="#77c043"></rect><rect x="369" y="28" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="373" y="28" width="4" height="4" fill="#5aa02c"></rect><rect x="377" y="28" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="381" y="28" width="4" height="4" fill="#77c043"></rect><rect x="361" y="32" width="4" height="4" fill="#77c043">\
            </rect><rect x="365" y="32" width="4" height="4" fill="#5aa02c"></rect><rect x="369" y="32" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="373" y="32" width="4" height="4" fill="#3f7d1e"></rect><rect x="377" y="32" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="381" y="32" width="4" height="4" fill="#5aa02c"></rect><rect x="385" y="32" width="4" height="4" fill="#77c043">\
            </rect><rect x="361" y="36" width="4" height="4" fill="#77c043"></rect><rect x="365" y="36" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="369" y="36" width="4" height="4" fill="#5aa02c"></rect><rect x="373" y="36" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="377" y="36" width="4" height="4" fill="#5aa02c"></rect><rect x="381" y="36" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="385" y="36" width="4" height="4" fill="#77c043"></rect><rect x="365" y="40" width="4" height="4" fill="#77c043">\
            </rect><rect x="369" y="40" width="4" height="4" fill="#5aa02c"></rect><rect x="373" y="40" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="377" y="40" width="4" height="4" fill="#5aa02c"></rect><rect x="381" y="40" width="4" height="4" fill="#77c043">\
            </rect><rect x="373" y="44" width="4" height="4" fill="#8a5a2b"></rect><rect x="377" y="44" width="4" height="4" fill="#6e4420">\
            </rect><rect x="373" y="48" width="4" height="4" fill="#8a5a2b"></rect><rect x="377" y="48" width="4" height="4" fill="#6e4420">\
            </rect><rect x="373" y="52" width="4" height="4" fill="#8a5a2b"></rect><rect x="377" y="52" width="4" height="4" fill="#6e4420">\
            </rect><rect x="373" y="56" width="4" height="4" fill="#8a5a2b"></rect><rect x="377" y="56" width="4" height="4" fill="#6e4420">\
            </rect><rect x="515.6" y="30" width="4" height="4" fill="#77c043"></rect><rect x="511.6" y="34" width="4" height="4" fill="#77c043">\
            </rect><rect x="515.6" y="34" width="4" height="4" fill="#5aa02c"></rect><rect x="519.6" y="34" width="4" height="4" fill="#77c043">\
            </rect><rect x="527.6" y="34" width="4" height="4" fill="#77c043"></rect><rect x="507.6" y="38" width="4" height="4" fill="#77c043">\
            </rect><rect x="511.6" y="38" width="4" height="4" fill="#5aa02c"></rect><rect x="515.6" y="38" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="519.6" y="38" width="4" height="4" fill="#5aa02c"></rect><rect x="523.6" y="38" width="4" height="4" fill="#77c043">\
            </rect><rect x="527.6" y="38" width="4" height="4" fill="#5aa02c"></rect><rect x="531.6" y="38" width="4" height="4" fill="#77c043">\
            </rect><rect x="511.6" y="42" width="4" height="4" fill="#77c043"></rect><rect x="515.6" y="42" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="519.6" y="42" width="4" height="4" fill="#3f7d1e"></rect><rect x="523.6" y="42" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="527.6" y="42" width="4" height="4" fill="#77c043"></rect><rect x="511.6" y="46" width="4" height="4" fill="#77c043">\
            </rect><rect x="515.6" y="46" width="4" height="4" fill="#5aa02c"></rect><rect x="519.6" y="46" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="523.6" y="46" width="4" height="4" fill="#5aa02c"></rect><rect x="527.6" y="46" width="4" height="4" fill="#77c043">\
            </rect><rect x="515.6" y="50" width="4" height="4" fill="#8a5a2b"></rect><rect x="519.6" y="50" width="4" height="4" fill="#6e4420">\
            </rect><rect x="515.6" y="54" width="4" height="4" fill="#8a5a2b"></rect><rect x="519.6" y="54" width="4" height="4" fill="#6e4420">\
            </rect><rect x="585" y="22" width="4" height="4" fill="#77c043"></rect><rect x="581" y="26" width="4" height="4" fill="#77c043">\
            </rect><rect x="585" y="26" width="4" height="4" fill="#5aa02c"></rect><rect x="589" y="26" width="4" height="4" fill="#77c043">\
            </rect><rect x="577" y="30" width="4" height="4" fill="#77c043"></rect><rect x="581" y="30" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="585" y="30" width="4" height="4" fill="#5aa02c"></rect><rect x="589" y="30" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="593" y="30" width="4" height="4" fill="#77c043"></rect><rect x="573" y="34" width="4" height="4" fill="#77c043">\
            </rect><rect x="577" y="34" width="4" height="4" fill="#5aa02c"></rect><rect x="581" y="34" width="4" height="4" fill="#3f7d1e">\
            </rect><rect x="585" y="34" width="4" height="4" fill="#5aa02c"></rect><rect x="589" y="34" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="593" y="34" width="4" height="4" fill="#5aa02c"></rect><rect x="597" y="34" width="4" height="4" fill="#77c043">\
            </rect><rect x="573" y="38" width="4" height="4" fill="#77c043"></rect><rect x="577" y="38" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="581" y="38" width="4" height="4" fill="#5aa02c"></rect><rect x="585" y="38" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="589" y="38" width="4" height="4" fill="#5aa02c"></rect><rect x="593" y="38" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="597" y="38" width="4" height="4" fill="#77c043"></rect><rect x="577" y="42" width="4" height="4" fill="#77c043">\
            </rect><rect x="581" y="42" width="4" height="4" fill="#5aa02c"></rect><rect x="585" y="42" width="4" height="4" fill="#5aa02c">\
            </rect><rect x="589" y="42" width="4" height="4" fill="#77c043"></rect><rect x="585" y="46" width="4" height="4" fill="#8a5a2b">\
            </rect><rect x="589" y="46" width="4" height="4" fill="#6e4420"></rect><rect x="585" y="50" width="4" height="4" fill="#8a5a2b">\
            </rect><rect x="589" y="50" width="4" height="4" fill="#6e4420"></rect><rect x="585" y="54" width="4" height="4" fill="#8a5a2b">\
            </rect><rect x="589" y="54" width="4" height="4" fill="#6e4420"></rect><rect x="329" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="333" y="52" width="2" height="2" fill="#5aa02c"></rect><rect x="337" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="329" y="54" width="2" height="2" fill="#77c043"></rect><rect x="331" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="333" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="335" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="337" y="54" width="2" height="2" fill="#77c043"></rect><rect x="329" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="331" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="333" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="335" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="337" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><g transform="translate(0,3)"><rect x="369" y="52" width="2" height="2" fill="#77c043"></rect><rect x="373" y="52" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="377" y="52" width="2" height="2" fill="#77c043"></rect><rect x="369" y="54" width="2" height="2" fill="#77c043">\
            </rect><rect x="371" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="373" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="375" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="377" y="54" width="2" height="2" fill="#77c043">\
            </rect><rect x="369" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="371" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="373" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="375" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="377" y="56" width="2" height="2" fill="#5aa02c"></rect></g><rect x="409" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="413" y="52" width="2" height="2" fill="#5aa02c"></rect><rect x="417" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="409" y="54" width="2" height="2" fill="#77c043"></rect><rect x="411" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="413" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="415" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="417" y="54" width="2" height="2" fill="#77c043"></rect><rect x="409" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="411" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="413" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="415" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="417" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="489" y="52" width="2" height="2" fill="#77c043"></rect><rect x="493" y="52" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="497" y="52" width="2" height="2" fill="#77c043"></rect><rect x="489" y="54" width="2" height="2" fill="#77c043">\
            </rect><rect x="491" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="493" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="495" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="497" y="54" width="2" height="2" fill="#77c043">\
            </rect><rect x="489" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="491" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="493" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="495" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="497" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="529" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="533" y="52" width="2" height="2" fill="#5aa02c"></rect><rect x="537" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="529" y="54" width="2" height="2" fill="#77c043"></rect><rect x="531" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="533" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="535" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="537" y="54" width="2" height="2" fill="#77c043"></rect><rect x="529" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="531" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="533" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="535" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="537" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="609" y="52" width="2" height="2" fill="#77c043"></rect><rect x="613" y="52" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="617" y="52" width="2" height="2" fill="#77c043"></rect><rect x="609" y="54" width="2" height="2" fill="#77c043">\
            </rect><rect x="611" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="613" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="615" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="617" y="54" width="2" height="2" fill="#77c043">\
            </rect><rect x="609" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="611" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="613" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="615" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="617" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="649" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="653" y="52" width="2" height="2" fill="#5aa02c"></rect><rect x="657" y="52" width="2" height="2" fill="#77c043">\
            </rect><rect x="649" y="54" width="2" height="2" fill="#77c043"></rect><rect x="651" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="653" y="54" width="2" height="2" fill="#5aa02c"></rect><rect x="655" y="54" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="657" y="54" width="2" height="2" fill="#77c043"></rect><rect x="649" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="651" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="653" y="56" width="2" height="2" fill="#5aa02c">\
            </rect><rect x="655" y="56" width="2" height="2" fill="#5aa02c"></rect><rect x="657" y="56" width="2" height="2" fill="#5aa02c">\
            </rect>""";

    /**
     * dark 장식: 달·별·모닥불·야간 구름·나무 (하늘 배경/잔디띠 rect 제외).
     * <p>
     * 추출 후 수동 조정: 별 9개에 opacity 반짝임(animate, dur/begin 엇갈림), 야간 구름에
     * 느린 좌우 드리프트(animateTransform) — rect 픽셀 좌표·색은 무변경.
     */
    private static final String DECOR_DARK = """
            <rect x="423" y="16" width="2" height="2" fill="#ffe98a"></rect><rect x="425" y="16" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="427" y="16" width="2" height="2" fill="#ffe98a"></rect><rect x="429" y="16" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="419" y="18" width="2" height="2" fill="#ffe98a"></rect><rect x="421" y="18" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="423" y="18" width="2" height="2" fill="#fff4bd"></rect><rect x="425" y="18" width="2" height="2" fill="#fff4bd">\
            </rect><rect x="427" y="18" width="2" height="2" fill="#ffe98a"></rect><rect x="429" y="18" width="2" height="2" fill="#1a2440">\
            </rect><rect x="419" y="20" width="2" height="2" fill="#ffe98a"></rect><rect x="421" y="20" width="2" height="2" fill="#fff4bd">\
            </rect><rect x="423" y="20" width="2" height="2" fill="#fff4bd"></rect><rect x="425" y="20" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="427" y="20" width="2" height="2" fill="#1a2440"></rect><rect x="417" y="22" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="419" y="22" width="2" height="2" fill="#fff4bd"></rect><rect x="421" y="22" width="2" height="2" fill="#fff4bd">\
            </rect><rect x="423" y="22" width="2" height="2" fill="#ffe98a"></rect><rect x="425" y="22" width="2" height="2" fill="#1a2440">\
            </rect><rect x="417" y="24" width="2" height="2" fill="#ffe98a"></rect><rect x="419" y="24" width="2" height="2" fill="#fff4bd">\
            </rect><rect x="421" y="24" width="2" height="2" fill="#fff4bd"></rect><rect x="423" y="24" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="417" y="26" width="2" height="2" fill="#ffe98a"></rect><rect x="419" y="26" width="2" height="2" fill="#fff4bd">\
            </rect><rect x="421" y="26" width="2" height="2" fill="#fff4bd"></rect><rect x="423" y="26" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="425" y="26" width="2" height="2" fill="#1a2440"></rect><rect x="419" y="28" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="421" y="28" width="2" height="2" fill="#fff4bd"></rect><rect x="423" y="28" width="2" height="2" fill="#fff4bd">\
            </rect><rect x="425" y="28" width="2" height="2" fill="#ffe98a"></rect><rect x="427" y="28" width="2" height="2" fill="#1a2440">\
            </rect><rect x="419" y="30" width="2" height="2" fill="#ffe98a"></rect><rect x="421" y="30" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="423" y="30" width="2" height="2" fill="#fff4bd"></rect><rect x="425" y="30" width="2" height="2" fill="#fff4bd">\
            </rect><rect x="427" y="30" width="2" height="2" fill="#ffe98a"></rect><rect x="429" y="30" width="2" height="2" fill="#1a2440">\
            </rect><rect x="423" y="32" width="2" height="2" fill="#ffe98a"></rect><rect x="425" y="32" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="427" y="32" width="2" height="2" fill="#ffe98a"></rect><rect x="429" y="32" width="2" height="2" fill="#ffe98a">\
            </rect><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="3.2s" begin="0s" repeatCount="indefinite"/>\
            <rect x="347" y="18" width="2" height="2" fill="#ffe98a"></rect><rect x="345" y="20" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="347" y="20" width="2" height="2" fill="#ffffff"></rect><rect x="349" y="20" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="347" y="22" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="2.6s" begin="0.7s" repeatCount="indefinite"/>\
            <rect x="393" y="28" width="2" height="2" fill="#ffe98a"></rect><rect x="391" y="30" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="393" y="30" width="2" height="2" fill="#ffffff"></rect><rect x="395" y="30" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="393" y="32" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="3.8s" begin="0.3s" repeatCount="indefinite"/>\
            <rect x="443" y="16" width="2" height="2" fill="#ffe98a"></rect><rect x="441" y="18" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="443" y="18" width="2" height="2" fill="#ffffff"></rect><rect x="445" y="18" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="443" y="20" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="2.9s" begin="1.1s" repeatCount="indefinite"/>\
            <rect x="493" y="32" width="2" height="2" fill="#ffe98a"></rect><rect x="491" y="34" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="493" y="34" width="2" height="2" fill="#ffffff"></rect><rect x="495" y="34" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="493" y="36" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="3.4s" begin="0.5s" repeatCount="indefinite"/>\
            <rect x="543" y="20" width="2" height="2" fill="#ffe98a"></rect><rect x="541" y="22" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="543" y="22" width="2" height="2" fill="#ffffff"></rect><rect x="545" y="22" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="543" y="24" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="2.4s" begin="0.9s" repeatCount="indefinite"/>\
            <rect x="593" y="28" width="2" height="2" fill="#ffe98a"></rect><rect x="591" y="30" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="593" y="30" width="2" height="2" fill="#ffffff"></rect><rect x="595" y="30" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="593" y="32" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="3.6s" begin="0.2s" repeatCount="indefinite"/>\
            <rect x="623" y="17" width="2" height="2" fill="#ffe98a"></rect><rect x="621" y="19" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="623" y="19" width="2" height="2" fill="#ffffff"></rect><rect x="625" y="19" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="623" y="21" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="2.8s" begin="1.4s" repeatCount="indefinite"/>\
            <rect x="418" y="38" width="2" height="2" fill="#ffe98a"></rect><rect x="416" y="40" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="418" y="40" width="2" height="2" fill="#ffffff"></rect><rect x="420" y="40" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="418" y="42" width="2" height="2" fill="#ffe98a"></rect></g><g opacity="0.9"><animate attributeName="opacity" values="1;0.55;1" dur="3.1s" begin="0.6s" repeatCount="indefinite"/>\
            <rect x="523" y="40" width="2" height="2" fill="#ffe98a"></rect><rect x="521" y="42" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="523" y="42" width="2" height="2" fill="#ffffff"></rect><rect x="525" y="42" width="2" height="2" fill="#ffe98a">\
            </rect><rect x="523" y="44" width="2" height="2" fill="#ffe98a"></rect></g><rect x="565" y="28" width="3" height="3" fill="#f0916a">\
            </rect><rect x="562" y="31" width="3" height="3" fill="#f0916a"></rect><rect x="565" y="31" width="3" height="3" fill="#e0623a">\
            </rect><rect x="568" y="31" width="3" height="3" fill="#f0916a"></rect><rect x="559" y="34" width="3" height="3" fill="#f0916a">\
            </rect><rect x="562" y="34" width="3" height="3" fill="#e0623a"></rect><rect x="565" y="34" width="3" height="3" fill="#e0623a">\
            </rect><rect x="568" y="34" width="3" height="3" fill="#e0623a"></rect><rect x="571" y="34" width="3" height="3" fill="#f0916a">\
            </rect><rect x="556" y="37" width="3" height="3" fill="#f0916a"></rect><rect x="559" y="37" width="3" height="3" fill="#e0623a">\
            </rect><rect x="562" y="37" width="3" height="3" fill="#e0623a"></rect><rect x="565" y="37" width="3" height="3" fill="#241a12">\
            </rect><rect x="568" y="37" width="3" height="3" fill="#e0623a"></rect><rect x="571" y="37" width="3" height="3" fill="#e0623a">\
            </rect><rect x="574" y="37" width="3" height="3" fill="#f0916a"></rect><rect x="553" y="40" width="3" height="3" fill="#f0916a">\
            </rect><rect x="556" y="40" width="3" height="3" fill="#e0623a"></rect><rect x="559" y="40" width="3" height="3" fill="#e0623a">\
            </rect><rect x="562" y="40" width="3" height="3" fill="#e0623a"></rect><rect x="565" y="40" width="3" height="3" fill="#241a12">\
            </rect><rect x="568" y="40" width="3" height="3" fill="#e0623a"></rect><rect x="571" y="40" width="3" height="3" fill="#e0623a">\
            </rect><rect x="574" y="40" width="3" height="3" fill="#e0623a"></rect><rect x="577" y="40" width="3" height="3" fill="#f0916a">\
            </rect><rect x="550" y="43" width="3" height="3" fill="#f0916a"></rect><rect x="553" y="43" width="3" height="3" fill="#e0623a">\
            </rect><rect x="556" y="43" width="3" height="3" fill="#e0623a"></rect><rect x="559" y="43" width="3" height="3" fill="#e0623a">\
            </rect><rect x="562" y="43" width="3" height="3" fill="#e0623a"></rect><rect x="565" y="43" width="3" height="3" fill="#241a12">\
            </rect><rect x="568" y="43" width="3" height="3" fill="#e0623a"></rect><rect x="571" y="43" width="3" height="3" fill="#e0623a">\
            </rect><rect x="574" y="43" width="3" height="3" fill="#e0623a"></rect><rect x="577" y="43" width="3" height="3" fill="#e0623a">\
            </rect><rect x="580" y="43" width="3" height="3" fill="#f0916a"></rect><rect x="547" y="46" width="3" height="3" fill="#c0472a">\
            </rect><rect x="550" y="46" width="3" height="3" fill="#e0623a"></rect><rect x="553" y="46" width="3" height="3" fill="#e0623a">\
            </rect><rect x="556" y="46" width="3" height="3" fill="#e0623a"></rect><rect x="559" y="46" width="3" height="3" fill="#e0623a">\
            </rect><rect x="562" y="46" width="3" height="3" fill="#241a12"></rect><rect x="565" y="46" width="3" height="3" fill="#241a12">\
            </rect><rect x="568" y="46" width="3" height="3" fill="#241a12"></rect><rect x="571" y="46" width="3" height="3" fill="#e0623a">\
            </rect><rect x="574" y="46" width="3" height="3" fill="#e0623a"></rect><rect x="577" y="46" width="3" height="3" fill="#e0623a">\
            </rect><rect x="580" y="46" width="3" height="3" fill="#e0623a"></rect><rect x="583" y="46" width="3" height="3" fill="#c0472a">\
            </rect><rect x="547" y="49" width="3" height="3" fill="#c0472a"></rect><rect x="550" y="49" width="3" height="3" fill="#e0623a">\
            </rect><rect x="553" y="49" width="3" height="3" fill="#e0623a"></rect><rect x="556" y="49" width="3" height="3" fill="#e0623a">\
            </rect><rect x="559" y="49" width="3" height="3" fill="#241a12"></rect><rect x="562" y="49" width="3" height="3" fill="#f0e6d2">\
            </rect><rect x="565" y="49" width="3" height="3" fill="#f0e6d2"></rect><rect x="568" y="49" width="3" height="3" fill="#f0e6d2">\
            </rect><rect x="571" y="49" width="3" height="3" fill="#241a12"></rect><rect x="574" y="49" width="3" height="3" fill="#e0623a">\
            </rect><rect x="577" y="49" width="3" height="3" fill="#e0623a"></rect><rect x="580" y="49" width="3" height="3" fill="#e0623a">\
            </rect><rect x="583" y="49" width="3" height="3" fill="#c0472a"></rect><rect x="547" y="52" width="3" height="3" fill="#c0472a">\
            </rect><rect x="550" y="52" width="3" height="3" fill="#e0623a"></rect><rect x="553" y="52" width="3" height="3" fill="#e0623a">\
            </rect><rect x="556" y="52" width="3" height="3" fill="#e0623a"></rect><rect x="559" y="52" width="3" height="3" fill="#241a12">\
            </rect><rect x="562" y="52" width="3" height="3" fill="#f0e6d2"></rect><rect x="565" y="52" width="3" height="3" fill="#f0e6d2">\
            </rect><rect x="568" y="52" width="3" height="3" fill="#f0e6d2"></rect><rect x="571" y="52" width="3" height="3" fill="#241a12">\
            </rect><rect x="574" y="52" width="3" height="3" fill="#e0623a"></rect><rect x="577" y="52" width="3" height="3" fill="#e0623a">\
            </rect><rect x="580" y="52" width="3" height="3" fill="#e0623a"></rect><rect x="583" y="52" width="3" height="3" fill="#c0472a">\
            </rect><rect x="547" y="55" width="3" height="3" fill="#5e3720"></rect><rect x="550" y="55" width="3" height="3" fill="#c0472a">\
            </rect><rect x="553" y="55" width="3" height="3" fill="#c0472a"></rect><rect x="556" y="55" width="3" height="3" fill="#c0472a">\
            </rect><rect x="559" y="55" width="3" height="3" fill="#241a12"></rect><rect x="562" y="55" width="3" height="3" fill="#f0e6d2">\
            </rect><rect x="565" y="55" width="3" height="3" fill="#f0e6d2"></rect><rect x="568" y="55" width="3" height="3" fill="#f0e6d2">\
            </rect><rect x="571" y="55" width="3" height="3" fill="#241a12"></rect><rect x="574" y="55" width="3" height="3" fill="#c0472a">\
            </rect><rect x="577" y="55" width="3" height="3" fill="#c0472a"></rect><rect x="580" y="55" width="3" height="3" fill="#c0472a">\
            </rect><rect x="583" y="55" width="3" height="3" fill="#5e3720"></rect><rect x="575" y="52" width="3" height="3" fill="#a06d38">\
            </rect><rect x="578" y="52" width="3" height="3" fill="#a06d38"></rect><rect x="581" y="52" width="3" height="3" fill="#a06d38">\
            </rect><rect x="584" y="52" width="3" height="3" fill="#a06d38"></rect><rect x="587" y="52" width="3" height="3" fill="#a06d38">\
            </rect><rect x="575" y="55" width="3" height="3" fill="#8a5a2b"></rect><rect x="578" y="55" width="3" height="3" fill="#5e3a1c">\
            </rect><rect x="581" y="55" width="3" height="3" fill="#8a5a2b"></rect><rect x="584" y="55" width="3" height="3" fill="#5e3a1c">\
            </rect><rect x="587" y="55" width="3" height="3" fill="#8a5a2b"></rect><rect x="575" y="58" width="3" height="3" fill="#a06d38">\
            </rect><rect x="578" y="58" width="3" height="3" fill="#a06d38"></rect><rect x="581" y="58" width="3" height="3" fill="#a06d38">\
            </rect><rect x="584" y="58" width="3" height="3" fill="#a06d38"></rect><rect x="587" y="58" width="3" height="3" fill="#a06d38">\
            </rect><g><animateTransform attributeName="transform" type="translate" values="0 0;-32 0;0 0" dur="30s" repeatCount="indefinite"/>\
            <rect x="632" y="50" width="3" height="3" fill="#aab2c6"></rect><rect x="635" y="50" width="3" height="3" fill="#aab2c6">\
            </rect><rect x="638" y="50" width="3" height="3" fill="#aab2c6"></rect><rect x="629" y="53" width="3" height="3" fill="#aab2c6">\
            </rect><rect x="632" y="53" width="3" height="3" fill="#aab2c6"></rect><rect x="635" y="53" width="3" height="3" fill="#aab2c6">\
            </rect><rect x="638" y="53" width="3" height="3" fill="#aab2c6"></rect><rect x="641" y="53" width="3" height="3" fill="#aab2c6">\
            </rect><rect x="629" y="56" width="3" height="3" fill="#5b6478"></rect><rect x="632" y="56" width="3" height="3" fill="#5b6478">\
            </rect><rect x="635" y="56" width="3" height="3" fill="#5b6478"></rect><rect x="638" y="56" width="3" height="3" fill="#5b6478">\
            </rect><rect x="641" y="56" width="3" height="3" fill="#5b6478"></rect></g><g><g><rect x="605" y="44" width="3" height="3" fill="#ffc247">\
            </rect><rect x="602" y="47" width="3" height="3" fill="#ffc247"></rect><rect x="605" y="47" width="3" height="3" fill="#fff3b0">\
            </rect><rect x="608" y="47" width="3" height="3" fill="#ffc247"></rect><rect x="599" y="50" width="3" height="3" fill="#ff7a1a">\
            </rect><rect x="602" y="50" width="3" height="3" fill="#ffc247"></rect><rect x="605" y="50" width="3" height="3" fill="#fff3b0">\
            </rect><rect x="608" y="50" width="3" height="3" fill="#ffc247"></rect><rect x="611" y="50" width="3" height="3" fill="#ff7a1a">\
            </rect><rect x="599" y="53" width="3" height="3" fill="#ff7a1a"></rect><rect x="602" y="53" width="3" height="3" fill="#ff3d00">\
            </rect><rect x="605" y="53" width="3" height="3" fill="#ff7a1a"></rect><rect x="608" y="53" width="3" height="3" fill="#ff3d00">\
            </rect><rect x="611" y="53" width="3" height="3" fill="#ff7a1a"></rect><rect x="599" y="56" width="3" height="3" fill="#8a5a2b">\
            </rect><rect x="602" y="56" width="3" height="3" fill="#8a5a2b"></rect><rect x="605" y="56" width="3" height="3" fill="#8a5a2b">\
            </rect><rect x="608" y="56" width="3" height="3" fill="#8a5a2b"></rect><rect x="611" y="56" width="3" height="3" fill="#8a5a2b">\
            </rect></g><g opacity="0"><rect x="602" y="44" width="3" height="3" fill="#ffc247"></rect><rect x="602" y="47" width="3" height="3" fill="#ffc247">\
            </rect><rect x="605" y="47" width="3" height="3" fill="#fff3b0"></rect><rect x="608" y="47" width="3" height="3" fill="#ffc247">\
            </rect><rect x="599" y="50" width="3" height="3" fill="#ff7a1a"></rect><rect x="602" y="50" width="3" height="3" fill="#ffc247">\
            </rect><rect x="605" y="50" width="3" height="3" fill="#fff3b0"></rect><rect x="608" y="50" width="3" height="3" fill="#ffc247">\
            </rect><rect x="611" y="50" width="3" height="3" fill="#ff7a1a"></rect><rect x="599" y="53" width="3" height="3" fill="#ff7a1a">\
            </rect><rect x="602" y="53" width="3" height="3" fill="#ff3d00"></rect><rect x="605" y="53" width="3" height="3" fill="#ff7a1a">\
            </rect><rect x="608" y="53" width="3" height="3" fill="#ff3d00"></rect><rect x="611" y="53" width="3" height="3" fill="#ff7a1a">\
            </rect><rect x="599" y="56" width="3" height="3" fill="#8a5a2b"></rect><rect x="602" y="56" width="3" height="3" fill="#8a5a2b">\
            </rect><rect x="605" y="56" width="3" height="3" fill="#8a5a2b"></rect><rect x="608" y="56" width="3" height="3" fill="#8a5a2b">\
            </rect><rect x="611" y="56" width="3" height="3" fill="#8a5a2b"></rect></g></g>""";

    /**
     * streak 불꽃 프레임 1 (2x2 픽셀, 양 모드 동일).
     */
    private static final String FLAME_FRAME1 = """
            <rect x="4" y="0" width="2" height="2" fill="#ffc247"></rect><rect x="2" y="2" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="4" y="2" width="2" height="2" fill="#ffc247"></rect><rect x="6" y="2" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="2" y="4" width="2" height="2" fill="#ffc247"></rect><rect x="4" y="4" width="2" height="2" fill="#fff3b0"></rect>\
            <rect x="6" y="4" width="2" height="2" fill="#ffc247"></rect><rect x="0" y="6" width="2" height="2" fill="#ff7a1a"></rect>\
            <rect x="2" y="6" width="2" height="2" fill="#ffc247"></rect><rect x="4" y="6" width="2" height="2" fill="#fff3b0"></rect>\
            <rect x="6" y="6" width="2" height="2" fill="#ffc247"></rect><rect x="8" y="6" width="2" height="2" fill="#ff7a1a"></rect>\
            <rect x="0" y="8" width="2" height="2" fill="#ff7a1a"></rect><rect x="2" y="8" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="4" y="8" width="2" height="2" fill="#fff3b0"></rect><rect x="6" y="8" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="8" y="8" width="2" height="2" fill="#ff7a1a"></rect><rect x="0" y="10" width="2" height="2" fill="#ff7a1a"></rect>\
            <rect x="2" y="10" width="2" height="2" fill="#ff3d00"></rect><rect x="4" y="10" width="2" height="2" fill="#ff7a1a"></rect>\
            <rect x="6" y="10" width="2" height="2" fill="#ff3d00"></rect><rect x="8" y="10" width="2" height="2" fill="#ff7a1a"></rect>\
            <rect x="2" y="12" width="2" height="2" fill="#ff3d00"></rect><rect x="4" y="12" width="2" height="2" fill="#ff3d00"></rect>\
            <rect x="6" y="12" width="2" height="2" fill="#ff3d00"></rect>""";

    /**
     * streak 불꽃 프레임 2 — 프레임 1과 opacity 교차로 깜빡인다.
     */
    private static final String FLAME_FRAME2 = """
            <rect x="4" y="0" width="2" height="2" fill="#ffc247"></rect><rect x="4" y="2" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="6" y="2" width="2" height="2" fill="#ffc247"></rect><rect x="2" y="4" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="4" y="4" width="2" height="2" fill="#fff3b0"></rect><rect x="6" y="4" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="2" y="6" width="2" height="2" fill="#ffc247"></rect><rect x="4" y="6" width="2" height="2" fill="#fff3b0"></rect>\
            <rect x="6" y="6" width="2" height="2" fill="#ffc247"></rect><rect x="8" y="6" width="2" height="2" fill="#ff7a1a"></rect>\
            <rect x="0" y="8" width="2" height="2" fill="#ff7a1a"></rect><rect x="2" y="8" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="4" y="8" width="2" height="2" fill="#fff3b0"></rect><rect x="6" y="8" width="2" height="2" fill="#ffc247"></rect>\
            <rect x="0" y="10" width="2" height="2" fill="#ff7a1a"></rect><rect x="2" y="10" width="2" height="2" fill="#ff3d00"></rect>\
            <rect x="4" y="10" width="2" height="2" fill="#ff7a1a"></rect><rect x="6" y="10" width="2" height="2" fill="#ff3d00"></rect>\
            <rect x="8" y="10" width="2" height="2" fill="#ff7a1a"></rect><rect x="2" y="12" width="2" height="2" fill="#ff3d00"></rect>\
            <rect x="4" y="12" width="2" height="2" fill="#ff3d00"></rect><rect x="6" y="12" width="2" height="2" fill="#ff3d00"></rect>""";

    // ==== 레이아웃 상수 (프로토타입 실측) ====

    /**
     * 헤더 텍스트 x 원점.
     */
    private static final int HEADER_X = 68;

    /**
     * streak 텍스트 색 (양 모드 동일, GitHub 녹색).
     */
    private static final String STREAK_GREEN = "#2ea043";

    /**
     * Montserrat 700 13px 글리프 폭 추정 — 18px 실측(10.92)에서 13/18 비례 축소.
     */
    private static final double NAME_CHAR_WIDTH = 7.9;

    /**
     * username과 streak 그룹 사이 여백(px) — 넓은 '@' 글리프 과소추정 보정 + 시각적 간격.
     */
    private static final double STREAK_GAP = 12;

    /**
     * 히어로 연간 토큰용 3x5 픽셀 숫자 폰트(0~9). 각 문자열 = 5행 x 3열, '1'=칠, '0'=빔.
     */
    private static final String[] PIXEL_DIGITS = {
            "111101101101111", // 0
            "010110010010111", // 1
            "111001111100111", // 2
            "111001111001111", // 3
            "101101111001001", // 4
            "111100111001111", // 5
            "111100111101111", // 6
            "111001010010010", // 7
            "111101111101111", // 8
            "111101111001111"  // 9
    };

    /**
     * username 표시 최대 길이 — 초과 시 말줄임 (streak 그룹이 하늘 영역 x=321을 침범하지 않는 상한).
     */
    private static final int MAX_NAME_CHARS = 15;

    /**
     * 하늘 놀이터 영역.
     */
    private static final int SKY_X = 321;
    private static final int SKY_Y = 8;
    private static final int SKY_W = 348;
    private static final int SKY_H = 58;
    private static final int STRIP_H = 8;

    /**
     * 잔디 그리드 원점·간격.
     */
    private static final int GRID_X = 53;
    private static final int GRID_Y = 88;
    private static final int CELL_STEP = 12;

    /**
     * 불꽃 깜빡임 주기.
     */
    private static final String FLICKER_DUR = "0.9s";

    /**
     * 지원 색상 모드를 선언한다.
     *
     * @return LIGHT·DARK
     * @Since 2026-08-23
     */
    @Override
    public final Set<BadgeMode> supportedModes() {
        return EnumSet.of(BadgeMode.LIGHT, BadgeMode.DARK);
    }

    /**
     * 기본 색상 모드를 반환한다.
     *
     * @return LIGHT
     * @Since 2026-08-23
     */
    @Override
    public final BadgeMode defaultMode() {
        return BadgeMode.LIGHT;
    }

    /**
     * 공통 잔디 레이아웃에 테마별 마스코트·팔레트를 끼워 완성된 뱃지 SVG를 생성한다.
     * <p>
     * 패밀리 내부 훅은 boolean isDark 계약을 유지한다 — 모드 공간이 두 개뿐이라 정확하다.
     *
     * @param data 배지에 표시할 사용자 데이터 (null 불허)
     * @param mode 색상 모드 (DARK 외에는 라이트로 그린다)
     * @return 완성된 SVG 마크업 문자열
     * @Since 2026-07-15
     */
    @Override
    public final String build(BadgeResponse data, BadgeMode mode) {
        boolean isDark = mode == BadgeMode.DARK;
        GrassColors c = colors(isDark);
        String modeKey = mode.getCode();
        log.debug("Building grass badge: user={}, theme={}, isDark={}", data.username(), name(), isDark);

        StringBuilder sb = new StringBuilder();
        sb.append("""
                <svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink"
                     width="700" height="190" viewBox="0 0 700 190" role="img" aria-label="Tokenphage grass %s">
                """.formatted(SvgText.escape(data.username())));

        // 뱃지 전체를 프로젝트 저장소 링크로 감싼다 (CardBadgeTheme과 동일).
        sb.append("""
                <a href="%s" xlink:href="%s">
                """.formatted(SvgText.LINK_URL, SvgText.LINK_URL));

        sb.append("""
                <rect x="0.5" y="0.5" width="699" height="189" rx="16" fill="%s" stroke="%s"/>
                """.formatted(c.bg(), c.border()));

        appendHeader(sb, data, c);
        appendSky(sb, data, isDark, modeKey, c);
        sb.append("""
                <line x1="53" y1="70" x2="677" y2="70" stroke="%s"/>
                """.formatted(c.divider()));
        appendGrid(sb, data.daily1y(), c);
        sb.append(mascot(data, isDark));

        sb.append("</a>");
        sb.append("</svg>");
        return sb.toString();
    }

    /**
     * 이 테마가 요구하는 데이터 집합 — 기본 3종(연간 일별·streak·연간 총합)에
     * {@link #extraNeeds} 훅을 병합한다. final로 고정해 서브클래스가 기본 3종을
     * 실수로 빠뜨려 조용히 빈 잔디가 렌더되는 것을 막는다.
     *
     * @return 요구 데이터 집합
     * @Since 2026-07-15
     */
    @Override
    public final Set<BadgeDataNeed> needs() {
        Set<BadgeDataNeed> merged = EnumSet.of(
                BadgeDataNeed.DAILY_1Y, BadgeDataNeed.STREAK_DAYS, BadgeDataNeed.YEAR_TOKENS);
        merged.addAll(extraNeeds());
        return merged;
    }

    /**
     * 서브클래스가 추가로 요구할 데이터를 선언하는 훅. 기본은 없음.
     * <p>
     * 예: 마스코트를 누적 토큰 기반으로 레벨링하려면 TOTAL_TOKENS를 반환에 포함한다.
     * 선언하지 않은 데이터는 BadgeResponse에서 빈값(0 / List.of() / 0.0)이다.
     *
     * @return 추가 요구 데이터 집합 (null 불허)
     * @Since 2026-07-15
     */
    protected Set<BadgeDataNeed> extraNeeds() {
        return EnumSet.noneOf(BadgeDataNeed.class);
    }

    /**
     * 테마별 마스코트 SVG 조각을 반환한다. 하늘 놀이터(x 321..669, 지면 y=58) 위에 그려진다.
     * <p>
     * 주의: {@link #extraNeeds}로 선언하지 않은 BadgeResponse 필드는 빈값이다 —
     * grass 기본 경로에서 totalTokens는 항상 0이므로 레벨링에 쓰려면 TOTAL_TOKENS를 선언할 것.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark true면 다크 모드
     * @return 마스코트 SVG 조각
     * @Since 2026-07-15
     */
    protected abstract String mascot(BadgeResponse data, boolean isDark);

    /**
     * 모드별 팔레트를 반환한다. 기본은 프로토타입 실측 LIGHT/DARK.
     *
     * @param isDark true면 다크 모드
     * @return grass 팔레트
     * @Since 2026-07-15
     */
    protected GrassColors colors(boolean isDark) {
        return isDark ? GrassColors.DARK : GrassColors.LIGHT;
    }

    /**
     * 하늘 놀이터 장식(해/달·구름·나무 등) SVG 조각을 반환한다. 기본은 프로토타입 추출 상수.
     *
     * @param data   배지에 표시할 사용자 데이터 (null 불허)
     * @param isDark true면 다크 모드
     * @return 장식 SVG 조각
     * @Since 2026-07-15
     */
    protected String playground(BadgeResponse data, boolean isDark) {
        return isDark ? DECOR_DARK : DECOR_LIGHT;
    }

    private void appendHeader(StringBuilder sb, BadgeResponse data, GrassColors c) {
        String displayName = truncateName(data.username());
        // 1행: @username(보조) — 히어로(연간 토큰)에 위계를 양보해 작게 둔다.
        sb.append("""
                <text x="68" y="27" font-family="Montserrat,system-ui,sans-serif"
                      font-weight="700" font-size="13" fill="%s">@%s</text>
                """.formatted(c.textPrimary(), SvgText.escape(displayName)));

        // streak 그룹 x = 헤더 원점 + (@ 포함 글리프 수) x 글리프 폭 추정치(13px 기준), y는 1행에 맞춰 -8
        double streakX = HEADER_X + (displayName.length() + 1) * NAME_CHAR_WIDTH + STREAK_GAP;
        sb.append("<g transform=\"translate(%s,-8)\">".formatted(format2(streakX)));
        appendFlame(sb, data.streakDays(), c);
        String streakColor = data.streakDays() > 0 ? STREAK_GREEN : c.textSecondary();
        sb.append("""
                <text x="16" y="35" font-family="'JetBrains Mono',ui-monospace,monospace"
                      font-weight="700" font-size="11" fill="%s">%d-day</text>
                """.formatted(streakColor, data.streakDays()));
        sb.append("</g>");

        // 2행(히어로): 연간 토큰 수를 픽셀 디자인 숫자로(단색), 자릿수가 많으면 배율을 자동 축소한다.
        String yearStr = String.format(Locale.US, "%,d", data.yearTokens());
        double scale = pixelScale(yearStr);
        double numW = appendPixelNumber(sb, yearStr, HEADER_X, 57 - 5 * scale, scale, c.accent());
        // 단위 라벨 — 픽셀 숫자 오른쪽 (aria는 픽셀 숫자 그룹이 담당하므로 라벨은 화면용)
        sb.append("""
                <text x="%s" y="54" font-family="Montserrat,system-ui,sans-serif" font-weight="600"
                      font-size="10" fill="%s" aria-hidden="true">tokens/year</text>
                """.formatted(format2(HEADER_X + numW + 4), c.textSecondary()));
    }

    /**
     * 픽셀 숫자를 단색 rect로 그린다. 접근성·검증을 위해 그룹에 aria-label(숫자+단위)을 단다.
     *
     * @param sb     출력 버퍼
     * @param num    콤마 포함 숫자 문자열
     * @param startX 좌측 시작 x
     * @param topY   상단 y (숫자 0행)
     * @param scale  픽셀 한 칸 크기(px)
     * @param fill   숫자 색
     * @return 그려진 전체 폭(px)
     */
    private double appendPixelNumber(StringBuilder sb, String num, double startX, double topY,
                                     double scale, String fill) {
        sb.append("<g role=\"img\" aria-label=\"%s tokens/year\" shape-rendering=\"crispEdges\">".formatted(num));
        double x = startX;
        for (int i = 0; i < num.length(); i++) {
            char ch = num.charAt(i);
            if (ch == ',') {
                // 콤마 — 아래로 흐르는 꼬리, 좁은 폭
                sb.append(pixelCell(x + scale, topY + 3 * scale, scale, fill));
                sb.append(pixelCell(x + scale, topY + 4 * scale, scale, fill));
                sb.append(pixelCell(x, topY + 5 * scale, scale, fill));
                x += 2.5 * scale;
                continue;
            }
            String pat = PIXEL_DIGITS[ch - '0'];
            for (int row = 0; row < 5; row++) {
                for (int col = 0; col < 3; col++) {
                    if (pat.charAt(row * 3 + col) == '1') {
                        sb.append(pixelCell(x + col * scale, topY + row * scale, scale, fill));
                    }
                }
            }
            x += 4 * scale;
        }
        sb.append("</g>");
        return x - startX;
    }

    /**
     * 픽셀 숫자 한 칸(정사각 rect).
     */
    private static String pixelCell(double x, double y, double s, String fill) {
        return "<rect x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\" fill=\"%s\"/>"
                .formatted(format2(x), format2(y), format2(s), format2(s), fill);
    }

    /**
     * 픽셀 숫자 배율(px) — 콤마 포함 폭이 하늘 영역(x=321) 안에 들어가게 자릿수로 정한다(2.4~4.2).
     */
    private static double pixelScale(String num) {
        double cols = 0;
        for (int i = 0; i < num.length(); i++) {
            cols += num.charAt(i) == ',' ? 2.5 : 4;
        }
        return Math.max(2.4, Math.min(4.2, 150.0 / cols));
    }

    /**
     * streak>=1이면 주황 불꽃 2프레임 깜빡임, 0이면 회색 불꽃 정적 (결정 5).
     */
    private void appendFlame(StringBuilder sb, int streakDays, GrassColors c) {
        sb.append("<g transform=\"translate(0,21)\">");
        if (streakDays > 0) {
            sb.append("""
                    <g><animate attributeName="opacity" calcMode="discrete" values="1;0" keyTimes="0;0.5" dur="%s" repeatCount="indefinite"/>%s</g>
                    <g opacity="0"><animate attributeName="opacity" calcMode="discrete" values="0;1" keyTimes="0;0.5" dur="%s" repeatCount="indefinite"/>%s</g>
                    """.formatted(FLICKER_DUR, FLAME_FRAME1, FLICKER_DUR, FLAME_FRAME2));
        } else {
            sb.append(FLAME_FRAME1.replaceAll("fill=\"#[0-9a-fA-F]{6}\"", "fill=\"" + c.textSecondary() + "\""));
        }
        sb.append("</g>");
    }

    private void appendSky(StringBuilder sb, BadgeResponse data, boolean isDark, String mode, GrassColors c) {
        // id는 grass- 접두사 + 모드 접미사 — 타 테마·양모드 동시 임베드 시 충돌 방지
        String clipId = "grass-clip-" + mode;
        sb.append("""
                <defs><clipPath id="%s"><rect x="%d" y="%d" width="%d" height="%d" rx="8"/></clipPath></defs>
                <g clip-path="url(#%s)">
                <rect x="%d" y="%d" width="%d" height="%d" fill="%s"/>
                <rect x="%d" y="%d" width="%d" height="%d" fill="%s"/>
                %s
                </g>
                <rect x="%d" y="%d" width="%d" height="%d" rx="8" fill="none" stroke="%s"/>
                """.formatted(
                clipId, SKY_X, SKY_Y, SKY_W, SKY_H,
                clipId,
                SKY_X, SKY_Y, SKY_W, SKY_H, c.skyBg(),
                SKY_X, SKY_Y + SKY_H - STRIP_H, SKY_W, STRIP_H, c.groundStrip(),
                playground(data, isDark),
                SKY_X, SKY_Y, SKY_W, SKY_H, c.divider()));
    }

    private void appendGrid(StringBuilder sb, List<DailyCountResponse> daily1y, GrassColors c) {
        // 요일 라벨은 데이터와 무관하게 고정 (Mon/Wed/Fri = row 1/3/5)
        sb.append("""
                <text x="45" y="107" font-family="Montserrat,system-ui,sans-serif" font-size="10" fill="%s" text-anchor="end">Mon</text>
                <text x="45" y="131" font-family="Montserrat,system-ui,sans-serif" font-size="10" fill="%s" text-anchor="end">Wed</text>
                <text x="45" y="155" font-family="Montserrat,system-ui,sans-serif" font-size="10" fill="%s" text-anchor="end">Fri</text>
                """.formatted(c.textSecondary(), c.textSecondary(), c.textSecondary()));

        if (daily1y.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.parse(daily1y.get(daily1y.size() - 1).date());
        LocalDate gridStart = GrassGrid.gridStart(today);

        for (GrassGrid.MonthLabel label : GrassGrid.monthLabels(today)) {
            sb.append("""
                    <text x="%d" y="80" font-family="Montserrat,system-ui,sans-serif" font-size="10" fill="%s">%s</text>
                    """.formatted(GRID_X + label.column() * CELL_STEP, c.textSecondary(), label.label()));
        }

        long max = daily1y.stream()
                .filter(d -> !LocalDate.parse(d.date()).isBefore(gridStart))
                .mapToLong(DailyCountResponse::total)
                .max().orElse(0);
        for (DailyCountResponse day : daily1y) {
            LocalDate date = LocalDate.parse(day.date());
            if (date.isBefore(gridStart)) {
                continue;
            }
            int x = GRID_X + GrassGrid.columnOf(date, today) * CELL_STEP;
            int y = GRID_Y + GrassGrid.rowOf(date) * CELL_STEP;
            sb.append("<rect x=\"%d\" y=\"%d\" width=\"9\" height=\"9\" rx=\"2.5\" fill=\"%s\"/>"
                    .formatted(x, y, c.gridColor(GrassGrid.levelFor(day.total(), max))));
        }
        sb.append('\n');
    }

    private String truncateName(String username) {
        if (username.length() <= MAX_NAME_CHARS) {
            return username;
        }
        return username.substring(0, MAX_NAME_CHARS - 1) + "…";
    }

    private static String format2(double value) {
        return String.format(Locale.US, "%.2f", value);
    }
}
