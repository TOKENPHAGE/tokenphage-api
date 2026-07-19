package com.tokenphage.api.feature.badge.svg.theme.card.gpu;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 현재 구현된 {@link CardGpuBadgeTheme} 출력을 레벨 1~5 × light/dark로 렌더해
 * {@code docs/html-report/gpu-badge-levels.html} 리포트를 생성하는 일회성 유틸 테스트.
 * <p>
 * 프로덕션 코드가 호출하지 않으며, DB·Spring 컨텍스트 없이 in-memory 합성 데이터로만 동작한다.
 */
class CardGpuBadgeLevelsReportTest {

    // 레벨 1~5를 타도록 한 대표 누적 토큰
    private static final long[] LEVEL_TOKENS = {5_000_000L, 50_000_000L, 300_000_000L, 800_000_000L, 5_000_000_000L};

    @Test
    @DisplayName("GPU 배지를 레벨별 light/dark로 렌더해 docs/html-report에 HTML 리포트를 쓴다")
    void writeGpuBadgeLevelsReport() throws IOException {
        CardGpuBadgeTheme theme = new CardGpuBadgeTheme();

        StringBuilder lightCards = new StringBuilder();
        StringBuilder darkCards = new StringBuilder();
        for (int i = 0; i < LEVEL_TOKENS.length; i++) {
            BadgeResponse data = sampleData(LEVEL_TOKENS[i]);
            lightCards.append(card(i + 1, LEVEL_TOKENS[i], theme.build(data, false)));
            darkCards.append(card(i + 1, LEVEL_TOKENS[i], theme.build(data, true)));
        }

        String html = page(lightCards.toString(), darkCards.toString());

        // 테스트 작업 디렉토리(tokenphage-api) 기준 루트 docs/html-report
        Path outDir = Path.of("../docs/html-report");
        Files.createDirectories(outDir);
        Files.writeString(outDir.resolve("gpu-badge-levels.html"), html);
    }

    // 레벨별 누적 토큰만 바꾼 표시용 데이터 (히트바·모델은 공통 샘플)
    private BadgeResponse sampleData(long totalTokens) {
        LocalDate today = LocalDate.now();
        long[] daily = {
            5000, 12000, 0, 8000, 23000, 45000, 3000, 0, 7000, 15000,
            0, 30000, 12000, 8000, 0, 50000, 22000, 4000, 18000, 9000,
            0, 35000, 11000, 6000, 25000, 0, 14000, 42000, 7000, 19000
        };
        List<DailyCountResponse> daily30d = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            daily30d.add(new DailyCountResponse(today.minusDays(i).toString(), daily[29 - i]));
        }
        List<ModelCountResponse> topModels = List.of(
            new ModelCountResponse("claude-sonnet-4-6", 1_200_000),
            new ModelCountResponse("claude-opus-4-7", 450_000),
            new ModelCountResponse("claude-haiku-4-5-20251001", 300_000),
            new ModelCountResponse("claude-sonnet-4-5", 120_000),
            new ModelCountResponse("gpt-4o", 80_000)
        );
        return new BadgeResponse("leeyoungseok", totalTokens, daily30d, topModels, 0.87, 0L, 0, List.of());
    }

    private String card(int level, long tokens, String svg) {
        return """
                <div class="card">
                  <div class="meta"><span class="lv">Lv.%d</span><span class="tok">%,d tokens</span></div>
                  <div class="badge">%s</div>
                </div>
                """.formatted(level, tokens, svg);
    }

    private String page(String lightCards, String darkCards) {
        return """
                <!DOCTYPE html>
                <html lang="ko"><head><meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>GPU 배지 레벨별 (현재 구현)</title>
                <style>
                  body { margin:0; padding:40px 24px; background:#1b2230; color:#e2e8f0;
                    font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",system-ui,sans-serif; }
                  h1 { font-size:20px; margin:0 0 4px; }
                  p.sub { color:#94a3b8; margin:0 0 28px; font-size:13px; }
                  h2 { font-size:13px; color:#94a3b8; margin:28px 0 14px; font-weight:600;
                    letter-spacing:.06em; text-transform:uppercase; }
                  .grid { display:flex; flex-direction:column; gap:20px; }
                  .card { display:flex; flex-direction:column; gap:8px; }
                  .meta { display:flex; align-items:baseline; gap:12px; }
                  .lv { font-weight:700; font-size:15px; }
                  .tok { font-size:12px; color:#94a3b8; font-family:ui-monospace,monospace; }
                  .badge { border-radius:14px; overflow:hidden; width:fit-content;
                    box-shadow:0 8px 24px rgba(0,0,0,.3); }
                  .badge svg { display:block; }
                </style></head>
                <body>
                  <h1>🦠 GPU 배지 — 레벨별 (현재 구현 출력)</h1>
                  <p class="sub">CardGpuBadgeTheme.build() 실제 출력. 누적 토큰에 따라 Lv.1~5: 팬 가속 · 과열 글로우 · 색 변화 · 열파 · 전기 스파크.</p>
                  <h2>Light</h2>
                  <div class="grid">%s</div>
                  <h2>Dark</h2>
                  <div class="grid">%s</div>
                </body></html>
                """.formatted(lightCards, darkCards);
    }
}
