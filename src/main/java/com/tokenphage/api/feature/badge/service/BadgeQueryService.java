package com.tokenphage.api.feature.badge.service;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.token.repository.projection.CacheTokenSum;
import com.tokenphage.api.domain.user.repository.UserRepository;
import com.tokenphage.api.domain.user.repository.entity.User;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.DailyCountResponse;
import com.tokenphage.api.feature.badge.dto.response.ModelCountResponse;
import com.tokenphage.api.feature.badge.exception.BadgeErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
class BadgeQueryService {

    private final UserRepository userRepo;
    private final DailyTokenUsageRepository tokenRepo;

    @Value("${badge.timezone}")
    private String timezone;

    /**
     * username으로 배지에 필요한 모든 데이터를 조회해 {@link BadgeResponse}로 반환한다.
     *
     * @param username 배지를 조회할 GitHub 사용자명 (null 불허)
     * @return 배지 렌더링에 필요한 집계 데이터
     * @throws AppException 사용자가 존재하지 않을 경우 (BADGE_001)
     * @Since 2026-05-25
     */
    BadgeResponse query(String username) {
        log.debug("Querying badge data: username={}", username);

        // #1. 사용자 조회 및 조회 기간 설정 (오늘 기준 최근 30일)
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AppException(BadgeErrorCode.USER_NOT_FOUND));
        LocalDate today = LocalDate.now(ZoneId.of(timezone));
        LocalDate from = today.minusDays(29);

        // #2. 누적 토큰 합계 조회
        Long total = tokenRepo.sumTotalTokens(user.getGithubId());

        // #3. 최근 30일 일별 토큰 사용량 조회 후, 0값 포함 완전한 30일 배열로 구성
        Map<String, Long> byDate = tokenRepo.findLast30Days(user.getGithubId(), from, today).stream()
                .collect(Collectors.toMap(row -> row.getDate(), row -> row.getTotal()));
        List<DailyCountResponse> heat30bar = new ArrayList<>(30);
        for (int i = 29; i >= 0; i--) {
            String date = today.minusDays(i).toString();
            heat30bar.add(new DailyCountResponse(date, byDate.getOrDefault(date, 0L)));
        }

        // #4. 모델별 토큰 사용량 상위 5개 조회
        List<ModelCountResponse> topModels = tokenRepo.findTop5Models(user.getGithubId()).stream()
                .map(row -> new ModelCountResponse(row.getModel(), row.getTotal()))
                .toList();

        // #5. 캐시 토큰 합계 조회 후 캐시 히트율 계산
        List<CacheTokenSum> cacheRows = tokenRepo.sumCacheTokens(user.getGithubId());
        double cacheHitRate = calcCacheHitRate(cacheRows.isEmpty() ? null : cacheRows.getFirst());

        // #6. 조회된 데이터를 BadgeResponse로 조립하여 반환
        log.debug("Badge data ready: username={}, totalTokens={}, heatbarDays={}, models={}", username, total, heat30bar.size(), topModels.size());
        return new BadgeResponse(
                user.getUsername(),
                total == null ? 0L : total,
                heat30bar,
                topModels,
                cacheHitRate
        );
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
        long cacheRead   = row.getCacheRead();
        long cacheCreate = row.getCacheCreate();
        long inputTok    = row.getInputTok();
        long total = cacheRead + cacheCreate + inputTok;
        return total == 0 ? 0.0 : (double) cacheRead / total;
    }
}
