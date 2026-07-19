package com.tokenphage.api.feature.badge.svg.theme.card.claude;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * README 전시용 Claude 마스코트 SVG를 레벨별로 추출하는 일회성 유틸 테스트.
 * <p>
 * 프로덕션 코드가 호출하지 않으며, DB·Spring 컨텍스트를 띄우지 않는 순수 단위 테스트다.
 * {@link CardClaudeMascot#render(int, long, boolean)}(package-private) 결과를 독립 SVG로 감싸
 * {@code assets/badge/mascot/lv{n}-light.svg} 로 저장한다. (README는 light만 사용)
 */
class CardClaudeMascotExportTest {

    // 레벨별 최근 1일 토큰 (pop 팝업 "+N Tk" 표시용)
    private static final long[] SAMPLE_RECENT = {0, 1_200L, 45_000L, 900_000L, 12_000_000L, 80_000_000L};

    @Test
    @DisplayName("Claude 마스코트 레벨 1~5를 light SVG로 추출해 assets에 저장한다")
    void exportClaudeMascotsPerLevel() throws Exception {
        Path dir = Path.of("assets/badge/mascot");
        Files.createDirectories(dir);

        // Claude 테마: 레벨 1~5 마스코트 (translate(20,14) scale(3) → viewBox 100x84)
        for (int level = 1; level <= 5; level++) {
            String mascot = CardClaudeMascot.render(level, SAMPLE_RECENT[level], false);
            Files.writeString(dir.resolve("lv" + level + "-light.svg"), wrap(mascot, "0 0 100 84"));
        }
    }

    /**
     * 마스코트 조각을 light 카드 배경이 있는 독립 SVG 문서로 감싼다.
     *
     * @param mascot  마스코트 SVG 조각
     * @param viewBox "minX minY width height" 형식의 viewBox 문자열
     */
    private static String wrap(String mascot, String viewBox) {
        String[] vb = viewBox.split(" ");
        double x = Double.parseDouble(vb[0]);
        double y = Double.parseDouble(vb[1]);
        double w = Double.parseDouble(vb[2]);
        double h = Double.parseDouble(vb[3]);
        String rect = "<rect x=\"%s\" y=\"%s\" width=\"%s\" height=\"%s\" rx=\"14\" fill=\"#ffffff\" stroke=\"#e5e7eb\"/>"
                .formatted(x + 0.5, y + 0.5, w - 1, h - 1);

        return """
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="%s">
                  %s
                  %s
                </svg>
                """.formatted(viewBox, rect, mascot);
    }
}
