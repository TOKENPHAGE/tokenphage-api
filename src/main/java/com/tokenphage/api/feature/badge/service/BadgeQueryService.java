package com.tokenphage.api.feature.badge.service;

import com.tokenphage.api.domain.badge.repository.BadgeSnapshotRepository;
import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.token.repository.projection.CacheTokenSum;
import com.tokenphage.api.domain.user.repository.UserRepository;
import com.tokenphage.api.domain.user.repository.entity.User;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import com.tokenphage.api.feature.badge.exception.BadgeErrorCode;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class BadgeQueryService {

    private final UserRepository userRepo;
    private final DailyTokenUsageRepository tokenRepo;
    private final BadgeSnapshotRepository snapshotRepo;

    @Value("${badge.timezone}")
    private String timezone;

    /**
     * username으로, 테마가 선언한 {@code needs}에 해당하는 데이터만 조회해 {@link BadgeResponse}로 반환한다.
     * <p>
     * 요구되지 않은 데이터는 조회 쿼리를 실행하지 않고 빈값(0 / 빈 리스트 / 0.0)으로 채운다.
     * 일별 쿼리는 연간 계열(DAILY_1Y/STREAK_DAYS/YEAR_TOKENS) 또는 DAILY_30D 요구 시에만
     * 최대 범위(연간 요구 시 365일, 아니면 30일)로 1회 조회한 뒤 히트바·연간일별·연간총량·streak을 파생한다.
     *
     * @param username  배지를 조회할 GitHub 사용자명 (null 불허)
     * @param badgeCode 정규화된 배지 코드 (null 불허) — 스냅샷 복합키의 절반
     * @param needs     테마가 요구하는 데이터 종류 집합 (null 불허)
     * @return 배지 렌더링에 필요한 집계 데이터 (미요구 필드는 빈값)
     * @throws AppException 사용자가 존재하지 않을 경우 (BADGE_001)
     * @Since 2026-07-15
     */
    BadgeResponse query(String username, String badgeCode, Set<BadgeDataNeed> needs) {
        log.debug("Querying badge data: username={}, badgeCode={}, needs={}", username, badgeCode, needs);

        // #1. 사용자 조회
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(BadgeErrorCode.USER_NOT_FOUND));
        Long githubId = user.getGithubId();
        LocalDate today = LocalDate.now(ZoneId.of(timezone));

        // #2. 요구된 need들이 필요로 하는 일별 조회 창의 최댓값 (0이면 일별 조회 불필요)
        int dailyWindow = needs.stream()
                .mapToInt(BadgeDataNeed::getDailyWindowDays)
                .max()
                .orElse(0);

        // #3. 일별 쿼리는 최대 창으로 1회만 조회한다 (여러 need가 이 결과를 공유)
        Map<String, Long> tokensByDate = Map.of();
        if (dailyWindow > 0) {
            LocalDate from = today.minusDays(dailyWindow - 1L);
            tokensByDate = tokenRepo.findDailyTotalsBetween(githubId, from, today).stream()
                    .collect(Collectors.toMap(row -> row.getDate(), row -> row.getTotal()));
        }

        // #4. 30일 히트바: 요구 시 오늘까지 창(=DAILY_30D 창)만큼 0-채움, 아니면 빈 리스트
        List<DailyCountResponse> heat30bar = needs.contains(BadgeDataNeed.DAILY_30D)
                ? fillDaily(tokensByDate, today, BadgeDataNeed.DAILY_30D.getDailyWindowDays())
                : List.of();

        // #5. 1년 일별: 요구 시 오늘까지 창(=DAILY_1Y 창)만큼 0-채움, 아니면 빈 리스트
        List<DailyCountResponse> daily1y = needs.contains(BadgeDataNeed.DAILY_1Y)
                ? fillDaily(tokensByDate, today, BadgeDataNeed.DAILY_1Y.getDailyWindowDays())
                : List.of();

        // #6. 1년 총 토큰: 요구 시 조회된 일별 합산, 아니면 0
        long yearTokens = needs.contains(BadgeDataNeed.YEAR_TOKENS)
                ? tokensByDate.values().stream().mapToLong(Long::longValue).sum()
                : 0L;

        // #7. 연속 사용일(streak): 요구 시 2일 유예 규칙으로 계산, 아니면 0
        int streakDays = needs.contains(BadgeDataNeed.STREAK_DAYS)
                ? calcStreakDays(tokensByDate, today)
                : 0;

        // #8. 누적 토큰: 요구 시에만 조회, 아니면 0
        long totalTokens = 0L;
        if (needs.contains(BadgeDataNeed.TOTAL_TOKENS)) {
            Long total = tokenRepo.sumTotalTokens(githubId);
            totalTokens = total == null ? 0L : total;
        }

        // #9. 모델별 상위 5개: 요구 시에만 조회, 아니면 빈 리스트
        List<ModelCountResponse> topModels = List.of();
        if (needs.contains(BadgeDataNeed.TOP_MODELS)) {
            topModels = tokenRepo.findTop5Models(githubId).stream()
                    .map(row -> new ModelCountResponse(row.getModel(), row.getTotal()))
                    .toList();
        }

        // #10. 캐시 히트율: 요구 시에만 조회, 아니면 0.0
        double cacheHitRate = 0.0;
        if (needs.contains(BadgeDataNeed.CACHE_HIT_RATE)) {
            List<CacheTokenSum> cacheRows = tokenRepo.sumCacheTokens(githubId);
            cacheHitRate = calcCacheHitRate(cacheRows.isEmpty() ? null : cacheRows.getFirst());
        }

        // #11. 고정 스냅샷: 요구 시 복합 PK로 1회 조회. 행이 없으면 빈 문자열 (적재 누락 신호)
        String snapshot = "";
        if (needs.contains(BadgeDataNeed.BADGE_SNAPSHOT)) {
            String payload = snapshotRepo.findPayload(githubId, badgeCode);
            if (payload == null) {
                log.warn("Badge snapshot missing: username={}, badgeCode={}", username, badgeCode);
            } else {
                snapshot = payload;
            }
        }

        // #12. 조회된 데이터를 BadgeResponse로 조립하여 반환
        log.debug("Badge data ready: username={}, totalTokens={}, heatbarDays={}, yearDays={}, streakDays={}",
                username, totalTokens, heat30bar.size(), daily1y.size(), streakDays);
        return new BadgeResponse(
                user.getUsername(),
                totalTokens,
                heat30bar,
                topModels,
                cacheHitRate,
                yearTokens,
                streakDays,
                daily1y,
                snapshot
        );
    }

    /**
     * 조회된 일별 합계 맵을 오늘 기준 최근 {@code days}일 배열로 0-채움해 오름차순으로 반환한다.
     * <p>
     * 마지막 원소가 오늘, 첫 원소가 today-(days-1)이며, 데이터가 없는 날짜는 0으로 채운다.
     *
     * @param tokensByDate 날짜(yyyy-MM-dd)→총합 맵
     * @param today        기준 오늘 날짜
     * @param days         채울 일수 (예: 30, 365)
     * @return 오름차순으로 0-채움된 일별 사용량 목록
     */
    private List<DailyCountResponse> fillDaily(Map<String, Long> tokensByDate, LocalDate today, int days) {
        List<DailyCountResponse> result = new ArrayList<>(days);
        for (int i = days - 1; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            result.add(new DailyCountResponse(date, tokensByDate.getOrDefault(date, 0L)));
        }
        return result;
    }

    /**
     * 연속 사용일(streak)을 GitHub 스타일 + 2일 유예 규칙으로 계산한다.
     * <p>
     * 오늘→어제→그제 순으로 첫 사용일(total&gt;0)을 앵커로 삼고, 그 날부터 역방향으로 연속 사용일 수를 센다.
     * 셋 다 미사용이면 0이다. total=0 엔트리(캐시 전용 사용일 등)는 미사용으로 취급해 연속을 끊는다.
     * (클라이언트 로컬 날짜와 서버 타임존 시차로 서쪽 타임존 사용자가 매일 아침 streak이 0으로 떨어지는 문제를 흡수)
     *
     * @param tokensByDate 날짜(yyyy-MM-dd)→총합 맵 (조회된 창 범위)
     * @param today        기준 오늘 날짜
     * @return 연속 사용일 수 (사용 이력 없으면 0)
     */
    private int calcStreakDays(Map<String, Long> tokensByDate, LocalDate today) {
        // 2일 유예: 오늘→어제→그제 순으로 첫 사용일(total>0)을 앵커로 찾는다.
        LocalDate anchor = null;
        for (int i = 0; i <= 2; i++) {
            LocalDate candidate = today.minusDays(i);
            if (tokensByDate.getOrDefault(candidate.toString(), 0L) > 0L) {
                anchor = candidate;
                break;
            }
        }
        if (anchor == null) {
            return 0;
        }
        // 앵커부터 역방향으로 연속 사용일 수를 센다. total=0 엔트리에서 끊긴다.
        int streak = 0;
        LocalDate cursor = anchor;
        while (tokensByDate.getOrDefault(cursor.toString(), 0L) > 0L) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    /**
     * 캐시 토큰 집계 결과로 캐시 히트율(Hit-Rate)을 계산한다.
     * <p>
     * 히트율 = cacheRead / (cacheRead + cacheCreate + inputTok)
     * 데이터가 없거나 전체 토큰이 0이면 0.0을 반환한다.
     *
     * @param row 캐시 토큰 집계 프로젝션 (null 허용)
     * @return 0.0 ~ 1.0 범위의 캐시 히트율
     * @Since 2026-05-25
     */
    private double calcCacheHitRate(CacheTokenSum row) {
        if (row == null || row.getCacheRead() == null) {
            return 0.0;
        }
        long cacheRead = row.getCacheRead();
        long cacheCreate = row.getCacheCreate();
        long inputTok = row.getInputTok();
        long total = cacheRead + cacheCreate + inputTok;
        return total == 0 ? 0.0 : (double) cacheRead / total;
    }
}
