package com.tokenphage.api.feature.badge.svg;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SvgBuilder {

    private static final String DEFAULT_THEME = BadgeCode.GPU.getCode();
    private static final String DARK_MODE = "dark";
    private static final String LIGHT_MODE = "light";

    private final Map<String, BadgeTheme> themes;

    public SvgBuilder(List<BadgeTheme> themeList) {
        this.themes = themeList.stream()
                .collect(Collectors.toMap(BadgeTheme::name, Function.identity()));
    }

    /**
     * theme과 mode에 맞는 BadgeTheme을 선택하여 SVG를 생성한다.
     * 등록되지 않은 theme은 기본 테마("gpu")로, "dark"가 아닌 mode는 "light"로 정규화한다.
     *
     * @param data  배지에 표시할 사용자 데이터 (null 불허)
     * @param theme 뱃지 스킨 종류 (예: "gpu")
     * @param mode  색상 모드 ("dark" 또는 "light")
     * @return 완성된 SVG 마크업 문자열
     * @Since 2026-05-27
     */
    public String build(BadgeResponse data, String theme, String mode) {
        BadgeTheme selected = themes.get(normalizeTheme(theme));
        boolean isDark = DARK_MODE.equals(normalizeMode(mode));
        log.debug("Building SVG badge: user={}, theme={}, mode={}", data.username(), theme, mode);
        return selected.build(data, isDark);
    }

    /**
     * theme이 요구하는 데이터 종류 집합을 반환한다.
     * <p>
     * {@link #normalizeTheme}로 정규화한 뒤 해당 테마의 {@link BadgeTheme#needs()}를 반환하므로,
     * 미등록 theme은 기본 테마("gpu")의 needs로 폴백한다(캐시 키·렌더 정규화 경로와 동일).
     *
     * @param theme 원본 theme 파라미터 (null 허용)
     * @return 해당 테마가 요구하는 데이터 종류 집합
     * @Since 2026-07-15
     */
    public Set<BadgeDataNeed> needsOf(String theme) {
        BadgeTheme selected = themes.get(normalizeTheme(theme));
        return selected.needs();
    }

    /**
     * theme 문자열을 등록된 테마 식별자로 정규화한다.
     * <p>
     * 등록된 테마면 소문자 식별자를, 미등록이면 기본 테마("gpu")를 반환한다.
     * 캐시 키 카디널리티를 유한하게 유지하기 위해 캐시 키 생성 전에 사용한다.
     *
     * @param theme 원본 theme 파라미터 (null 허용)
     * @return 등록된 테마 식별자 또는 기본 테마
     * @Since 2026-06-11
     */
    public String normalizeTheme(String theme) {
        if (theme == null) {
            return DEFAULT_THEME;
        }
        String key = theme.toLowerCase();
        return themes.containsKey(key) ? key : DEFAULT_THEME;
    }

    /**
     * mode 문자열을 "dark" 또는 "light"로 정규화한다.
     * <p>
     * "dark"(대소문자 무관)면 "dark"를, 그 외 모든 값(null 포함)은 "light"를 반환한다.
     *
     * @param mode 원본 mode 파라미터 (null 허용)
     * @return "dark" 또는 "light"
     * @Since 2026-06-11
     */
    public String normalizeMode(String mode) {
        return DARK_MODE.equalsIgnoreCase(mode) ? DARK_MODE : LIGHT_MODE;
    }

    /**
     * mode 문자열이 다크 모드인지 판정한다.
     * <p>
     * BadgeTheme.build()는 boolean isDark를 받으므로, 디스패처를 거치지 않고 테마를 직접
     * 호출하는 쪽(잠금 안내 렌더)이 같은 기준으로 변환할 수 있게 노출한다.
     *
     * @param mode 원본 또는 정규화된 mode 문자열 (null 허용)
     * @return "dark"면 true, 그 외에는 false
     * @Since 2026-08-10
     */
    public boolean isDark(String mode) {
        return DARK_MODE.equalsIgnoreCase(mode);
    }
}
