package com.tokenphage.api.feature.badge.svg.theme.grass.claude;

import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.function.LongUnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * grass-claude 뱃지 시나리오별 시각 검증 리포트를 산출하는 일회성 유틸 테스트.
 * <p>
 * 프로덕션 코드가 호출하지 않으며 DB·Spring 컨텍스트 없이 in-memory 합성 데이터로만 동작한다.
 * 산출물: 워크스페이스 루트 docs/html-report/grass-claude-badge.html.
 * SVG는 raw 인라인이 아니라 data URI img로 임베드한다 — 같은 mode 시나리오 여러 개를
 * 한 HTML에 넣으면 id(grass-clip-light 등)가 중복되기 때문.
 */
@DisplayName("GrassClaudeBadge 시나리오 리포트 산출")
class GrassClaudeBadgeReportTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 7, 15);

    private final GrassClaudeBadgeTheme theme = new GrassClaudeBadgeTheme();

    @Test
    @DisplayName("잔디뱃지리포트_시나리오5종_HTML산출")
    void 잔디뱃지리포트_시나리오5종_HTML산출() throws IOException {
        // given: 합성 시나리오 (활발/희소/신규 streak0/365일 만점/rolling 창 중복 월)
        Random random = new Random(42);
        List<Scenario> scenarios = List.of(
                new Scenario("활발 유저 — 매일 사용, streak 87",
                        data("active-user", 87, TODAY, d -> 1_000L + random.nextInt(99_000))),
                new Scenario("희소 유저 — 주 1회, streak 0(회색 불꽃)",
                        data("sparse-user", 0, TODAY, d -> d % 7 == 0 ? 40_000L : 0L)),
                new Scenario("신규 유저 — 데이터 전무",
                        data("new-user", 0, TODAY, d -> 0L)),
                new Scenario("만점 streak — 365일 연속",
                        data("streak-master", 365, TODAY, d -> 20_000L + (d % 30) * 3_000L)),
                new Scenario("rolling 창 — 1월 중복 라벨 (기준일 2026-01-20)",
                        data("rolling-user", 12, LocalDate.of(2026, 1, 20),
                                d -> d % 3 == 0 ? 55_000L : 8_000L)));

        // when: light/dark 렌더 → data URI 카드 조립
        StringBuilder cards = new StringBuilder();
        for (Scenario scenario : scenarios) {
            cards.append("<section><h2>%s</h2><div class=\"pair\">".formatted(scenario.title()));
            cards.append(card(theme.build(scenario.data(), false), "light"));
            cards.append(card(theme.build(scenario.data(), true), "dark"));
            cards.append("</div></section>");
        }
        String html = """
                <!DOCTYPE html><html lang="ko"><head><meta charset="utf-8">
                <title>grass-claude 뱃지 시나리오 리포트</title>
                <style>
                  body { font-family: Pretendard, sans-serif; background: #f4f4f5; margin: 32px; }
                  h1 { font-size: 20px; } h2 { font-size: 14px; color: #444; }
                  .pair { display: flex; gap: 16px; flex-wrap: wrap; }
                  .card { padding: 12px; border-radius: 12px; }
                  .card.light { background: #fafafa; border: 1px solid #ddd; }
                  .card.dark { background: #0a0f1e; }
                </style></head><body>
                <h1>grass-claude 뱃지 — 시나리오 x light/dark (기준 생성일: %s)</h1>
                %s
                </body></html>
                """.formatted(TODAY, cards);

        // then: 워크스페이스 루트 docs/html-report에 산출
        Path outDir = Path.of("../docs/html-report");
        Files.createDirectories(outDir);
        Path out = outDir.resolve("grass-claude-badge.html");
        Files.writeString(out, html);
        assertThat(Files.exists(out)).isTrue();
        assertThat(Files.size(out)).isGreaterThan(10_000);
    }

    private static String card(String svg, String mode) {
        String encoded = Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return "<div class=\"card %s\"><img width=\"700\" height=\"190\" alt=\"%s\" src=\"data:image/svg+xml;base64,%s\"/></div>"
                .formatted(mode, mode, encoded);
    }

    /** endDay 종료 365일 창을 daysAgo→토큰 함수로 채운 합성 응답을 만든다. */
    private static BadgeResponse data(String username, int streakDays, LocalDate endDay,
                                      LongUnaryOperator tokensByDaysAgo) {
        List<DailyCountResponse> daily1y = new ArrayList<>();
        long yearTokens = 0;
        for (int i = 364; i >= 0; i--) {
            long tokens = tokensByDaysAgo.applyAsLong(i);
            yearTokens += tokens;
            daily1y.add(new DailyCountResponse(endDay.minusDays(i).toString(), tokens));
        }
        return new BadgeResponse(username, 0L, List.of(), List.of(), 0.0,
                yearTokens, streakDays, daily1y);
    }

    private record Scenario(String title, BadgeResponse data) {
    }
}
