package com.tokenphage.api.feature.badge.service;

import com.tokenphage.api.domain.token.repository.DailyTokenUsageRepository;
import com.tokenphage.api.domain.token.repository.projection.CacheTokenSum;
import com.tokenphage.api.domain.token.repository.projection.DailyUsageRow;
import com.tokenphage.api.domain.token.repository.projection.ModelUsageRow;
import com.tokenphage.api.domain.user.repository.UserRepository;
import com.tokenphage.api.domain.user.repository.entity.User;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.exception.BadgeErrorCode;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BadgeQueryService.query / calcCacheHitRate 검증: 사용자 미존재 예외, 정상 응답 조립,
 * 30일 히트바 0값 채움(전체/일부/없음), 누적 null 처리, 캐시 히트율 경계 계산에 더해
 * needs 기반 조회 게이팅(요구된 쿼리만 실행)과 streak 2일 유예 계산을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeQueryService 단위 테스트")
class BadgeQueryServiceTest {

    private static final String TIMEZONE = "Asia/Seoul";
    private static final Long GITHUB_ID = -12345L;
    private static final String USERNAME = "octocat";
    // gpu/claude 통계 카드 테마의 기본 요구 데이터(4종) — 기존 시나리오는 이 needs로 조회한다.
    private static final Set<BadgeDataNeed> GPU_NEEDS = EnumSet.of(
            BadgeDataNeed.TOTAL_TOKENS, BadgeDataNeed.DAILY_30D,
            BadgeDataNeed.TOP_MODELS, BadgeDataNeed.CACHE_HIT_RATE);

    @Mock
    private UserRepository userRepo;

    @Mock
    private DailyTokenUsageRepository tokenRepo;

    @InjectMocks
    private BadgeQueryService service;

    @BeforeEach
    void setUp() {
        // @Value 주입 필드를 테스트 타임존으로 설정
        ReflectionTestUtils.setField(service, "timezone", TIMEZONE);
    }

    /** 테스트 대상과 동일한 기준 시각으로 오늘 날짜를 계산한다. */
    private LocalDate today() {
        return LocalDate.now(ZoneId.of(TIMEZONE));
    }

    /** githubId/username을 가진 User 엔티티를 생성한다. */
    private User user() {
        return new User(GITHUB_ID, USERNAME);
    }

    /** DailyUsageRow 프로젝션 mock (날짜/총합 getter stub). */
    private DailyUsageRow dailyRow(String date, long total) {
        DailyUsageRow row = mock(DailyUsageRow.class);
        when(row.getDate()).thenReturn(date);
        when(row.getTotal()).thenReturn(total);
        return row;
    }

    /** ModelUsageRow 프로젝션 mock (모델/총합 getter stub). */
    private ModelUsageRow modelRow(String model, long total) {
        ModelUsageRow row = mock(ModelUsageRow.class);
        when(row.getModel()).thenReturn(model);
        when(row.getTotal()).thenReturn(total);
        return row;
    }

    /** CacheTokenSum 프로젝션 mock (read/create/input getter stub). */
    private CacheTokenSum cacheRow(Long read, Long create, Long input) {
        CacheTokenSum row = mock(CacheTokenSum.class);
        // read가 null이면 calcCacheHitRate가 read 확인 후 early-return 하므로 나머지는 lenient
        lenient().when(row.getCacheRead()).thenReturn(read);
        lenient().when(row.getCacheCreate()).thenReturn(create);
        lenient().when(row.getInputTok()).thenReturn(input);
        return row;
    }

    @Nested
    @DisplayName("query - 사용자 조회")
    class Query_UserLookup {

        @Test
        @DisplayName("배지조회_사용자없음_USER_NOT_FOUND예외")
        void 배지조회_사용자없음_USER_NOT_FOUND예외() {
            // given
            when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

            // when
            // then
            assertThatThrownBy(() -> service.query("ghost", GPU_NEEDS))
                    .isInstanceOf(AppException.class)
                    .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                            .isEqualTo(BadgeErrorCode.USER_NOT_FOUND));
            // 사용자 미존재 시 토큰 집계 조회는 전혀 일어나지 않아야 한다
            verifyNoInteractions(tokenRepo);
        }
    }

    @Nested
    @DisplayName("query - 정상 응답 조립")
    class Query_HappyPath {

        @Test
        @DisplayName("배지조회_정상_username과히트바30개및모델매핑반환")
        void 배지조회_정상_username과히트바30개및모델매핑반환() {
            // given
            // 중첩 stubbing(UnfinishedStubbingException) 방지를 위해 mock row를 먼저 만든 뒤 thenReturn에 전달
            LocalDate today = today();
            DailyUsageRow daily = dailyRow(today.toString(), 500L);
            ModelUsageRow opus = modelRow("opus", 700L);
            ModelUsageRow sonnet = modelRow("sonnet", 300L);
            CacheTokenSum cache = cacheRow(70L, 20L, 10L);
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(1_000L);
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of(daily));
            when(tokenRepo.findTop5Models(GITHUB_ID))
                    .thenReturn(List.of(opus, sonnet));
            when(tokenRepo.sumCacheTokens(GITHUB_ID))
                    .thenReturn(List.of(cache));

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.username()).isEqualTo(USERNAME);
            assertThat(res.totalTokens()).isEqualTo(1_000L);
            assertThat(res.daily30d()).hasSize(30);
            assertThat(res.topModels()).hasSize(2);
            assertThat(res.topModels())
                    .extracting("model")
                    .containsExactly("opus", "sonnet");
            assertThat(res.topModels())
                    .extracting("total")
                    .containsExactly(700L, 300L);
        }

        @Test
        @DisplayName("배지조회_상위모델없음_빈리스트반환")
        void 배지조회_상위모델없음_빈리스트반환() {
            // given
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(0L);
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.topModels()).isEmpty();
        }
    }

    @Nested
    @DisplayName("query - 누적 토큰(totalTokens)")
    class Query_TotalTokens {

        @Test
        @DisplayName("누적토큰_sumTotalTokensNull_0반환")
        void 누적토큰_sumTotalTokensNull_0반환() {
            // given
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(null);
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.totalTokens()).isEqualTo(0L);
        }

        @Test
        @DisplayName("누적토큰_정상값_그대로반환")
        void 누적토큰_정상값_그대로반환() {
            // given
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(987_654L);
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.totalTokens()).isEqualTo(987_654L);
        }
    }

    @Nested
    @DisplayName("query - 히트바 30일 채움 로직")
    class Query_Heatbar {

        @Test
        @DisplayName("히트바_일부날짜만조회_누락날짜0으로채워30개구성")
        void 히트바_일부날짜만조회_누락날짜0으로채워30개구성() {
            // given
            LocalDate today = today();
            LocalDate from = today.minusDays(29);
            String todayStr = today.toString();
            String midStr = today.minusDays(15).toString();
            // 중첩 stubbing 방지를 위해 mock row를 먼저 생성
            DailyUsageRow midRow = dailyRow(midStr, 42L);
            DailyUsageRow todayRow = dailyRow(todayStr, 99L);
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(0L);
            // 30일 중 2일치만 데이터가 존재
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, from, today))
                    .thenReturn(List.of(midRow, todayRow));
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            // 총 30개, from..today 오름차순으로 채워지고 마지막이 오늘
            assertThat(res.daily30d()).hasSize(30);
            assertThat(res.daily30d().get(0).date()).isEqualTo(from.toString());
            assertThat(res.daily30d().get(29).date()).isEqualTo(todayStr);
            // 조회된 2일치는 값 유지
            assertThat(res.daily30d().get(29).total()).isEqualTo(99L);
            assertThat(res.daily30d().get(14).date()).isEqualTo(midStr);
            assertThat(res.daily30d().get(14).total()).isEqualTo(42L);
            // 나머지 누락 날짜는 0으로 채워짐 (조회된 2일 제외 28개가 0)
            long zeroCount = res.daily30d().stream().filter(d -> d.total() == 0L).count();
            assertThat(zeroCount).isEqualTo(28L);
        }

        @Test
        @DisplayName("히트바_조회결과없음_전부0인30개구성")
        void 히트바_조회결과없음_전부0인30개구성() {
            // given
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(0L);
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.daily30d()).hasSize(30);
            assertThat(res.daily30d()).allSatisfy(d -> assertThat(d.total()).isZero());
        }
    }

    @Nested
    @DisplayName("calcCacheHitRate (query 경유) - 캐시 히트율")
    class Query_CacheHitRate {

        /** 캐시 히트율 검증을 위한 공통 stub: 사용자/누적/히트바/모델은 비우고 cacheRows만 주입. */
        private void stubBaseWith(List<CacheTokenSum> cacheRows) {
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(0L);
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(cacheRows);
        }

        @Test
        @DisplayName("캐시히트율_빈리스트_0반환")
        void 캐시히트율_빈리스트_0반환() {
            // given
            stubBaseWith(List.of());

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.cacheHitRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("캐시히트율_cacheReadNull_0반환")
        void 캐시히트율_cacheReadNull_0반환() {
            // given
            stubBaseWith(List.of(cacheRow(null, 20L, 10L)));

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.cacheHitRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("캐시히트율_전체토큰0_0반환")
        void 캐시히트율_전체토큰0_0반환() {
            // given
            // read+create+input == 0 인 경계값 (분모 0)
            stubBaseWith(List.of(cacheRow(0L, 0L, 0L)));

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.cacheHitRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("캐시히트율_정상값_read70create20input10이면0_7반환")
        void 캐시히트율_정상값_read70create20input10이면0_7반환() {
            // given
            // 70 / (70 + 20 + 10) = 0.7
            stubBaseWith(List.of(cacheRow(70L, 20L, 10L)));

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            assertThat(res.cacheHitRate()).isCloseTo(0.7, within(1e-9));
        }
    }

    @Nested
    @DisplayName("query - needs 기반 조회 게이팅")
    class Query_NeedsGating {

        @Test
        @DisplayName("배지조회_grass요구사항_통계쿼리미실행")
        void 배지조회_grass요구사항_통계쿼리미실행() {
            // given
            // 연간 계열(DAILY_1Y/STREAK_DAYS/YEAR_TOKENS)만 요구 → 일별 쿼리는 365일 창으로 1회
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(364), today))
                    .thenReturn(List.of());

            // when
            service.query(USERNAME, EnumSet.of(
                    BadgeDataNeed.DAILY_1Y, BadgeDataNeed.STREAK_DAYS, BadgeDataNeed.YEAR_TOKENS));

            // then
            // 일별 쿼리는 365일 창으로 1회 호출되고, 통계 3쿼리는 전혀 실행되지 않아야 한다
            verify(tokenRepo).findDailyTotalsBetween(GITHUB_ID, today.minusDays(364), today);
            verify(tokenRepo, never()).findTop5Models(GITHUB_ID);
            verify(tokenRepo, never()).sumCacheTokens(GITHUB_ID);
            verify(tokenRepo, never()).sumTotalTokens(GITHUB_ID);
        }

        @Test
        @DisplayName("배지조회_통계요구사항_30일창유지")
        void 배지조회_통계요구사항_30일창유지() {
            // given
            // gpu 기본 4종 → 히트바 30일 창 유지, 연간 필드는 빈값
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(0L);
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME, GPU_NEEDS);

            // then
            verify(tokenRepo).findDailyTotalsBetween(GITHUB_ID, today.minusDays(29), today);
            assertThat(res.daily1y()).isEmpty();
            assertThat(res.streakDays()).isZero();
            assertThat(res.yearTokens()).isZero();
        }

        @Test
        @DisplayName("배지조회_1년데이터_yearDaily365개0채움")
        void 배지조회_1년데이터_yearDaily365개0채움() {
            // given
            LocalDate today = today();
            DailyUsageRow todayRow = dailyRow(today.toString(), 500L);
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(364), today))
                    .thenReturn(List.of(todayRow));

            // when
            BadgeResponse res = service.query(USERNAME, EnumSet.of(BadgeDataNeed.DAILY_1Y));

            // then
            // from=today-364 .. today = 365개, 오름차순으로 0-채움, 조회된 날짜만 값 유지
            assertThat(res.daily1y()).hasSize(365);
            assertThat(res.daily1y().get(0).date()).isEqualTo(today.minusDays(364).toString());
            assertThat(res.daily1y().get(364).date()).isEqualTo(today.toString());
            assertThat(res.daily1y().get(364).total()).isEqualTo(500L);
            long zeroCount = res.daily1y().stream().filter(d -> d.total() == 0L).count();
            assertThat(zeroCount).isEqualTo(364L);
        }

        @Test
        @DisplayName("배지조회_기존30일히트바_동일값유지")
        void 배지조회_기존30일히트바_동일값유지() {
            // given
            // 히트바 + 연간을 함께 요구하면 일별 쿼리는 365일 창으로 1회만 조회되고,
            // 히트바 마지막 30일 값은 30일 창 조회와 동일해야 한다(회귀).
            LocalDate today = today();
            String todayStr = today.toString();
            String midStr = today.minusDays(15).toString();
            DailyUsageRow midRow = dailyRow(midStr, 42L);
            DailyUsageRow todayRow = dailyRow(todayStr, 99L);
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(364), today))
                    .thenReturn(List.of(midRow, todayRow));

            // when
            BadgeResponse res = service.query(USERNAME,
                    EnumSet.of(BadgeDataNeed.DAILY_30D, BadgeDataNeed.DAILY_1Y));

            // then
            verify(tokenRepo).findDailyTotalsBetween(GITHUB_ID, today.minusDays(364), today);
            assertThat(res.daily30d()).hasSize(30);
            assertThat(res.daily30d().get(29).date()).isEqualTo(todayStr);
            assertThat(res.daily30d().get(29).total()).isEqualTo(99L);
            assertThat(res.daily30d().get(14).date()).isEqualTo(midStr);
            assertThat(res.daily30d().get(14).total()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("query - streak 계산 (GitHub 스타일 + 2일 유예)")
    class Query_Streak {

        /** streak 요구(YEAR 계열) 시나리오 공통 stub: 365일 창 일별 rows만 주입. */
        private void stubYearRows(LocalDate today, List<DailyUsageRow> rows) {
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.findDailyTotalsBetween(GITHUB_ID, today.minusDays(364), today))
                    .thenReturn(rows);
        }

        @Test
        @DisplayName("배지조회_오늘사용중_streak연속일수")
        void 배지조회_오늘사용중_streak연속일수() {
            // given
            // 오늘·어제·그제 연속 사용, 3일 전은 미사용 → streak 3
            LocalDate today = today();
            stubYearRows(today, List.of(
                    dailyRow(today.toString(), 10L),
                    dailyRow(today.minusDays(1).toString(), 20L),
                    dailyRow(today.minusDays(2).toString(), 30L)));

            // when
            BadgeResponse res = service.query(USERNAME, EnumSet.of(BadgeDataNeed.STREAK_DAYS));

            // then
            assertThat(res.streakDays()).isEqualTo(3);
        }

        @Test
        @DisplayName("배지조회_오늘미사용어제연속_streak유지")
        void 배지조회_오늘미사용어제연속_streak유지() {
            // given
            // 오늘 미사용, 어제·그제 연속 사용, 3일 전 미사용 → 앵커=어제, streak 2
            LocalDate today = today();
            stubYearRows(today, List.of(
                    dailyRow(today.minusDays(1).toString(), 20L),
                    dailyRow(today.minusDays(2).toString(), 30L)));

            // when
            BadgeResponse res = service.query(USERNAME, EnumSet.of(BadgeDataNeed.STREAK_DAYS));

            // then
            assertThat(res.streakDays()).isEqualTo(2);
        }

        @Test
        @DisplayName("배지조회_오늘어제미사용그제연속_streak유지")
        void 배지조회_오늘어제미사용그제연속_streak유지() {
            // given
            // 2일 유예: 오늘·어제 미사용, 그제·3일전 연속 → 앵커=그제, streak 2
            LocalDate today = today();
            stubYearRows(today, List.of(
                    dailyRow(today.minusDays(2).toString(), 30L),
                    dailyRow(today.minusDays(3).toString(), 40L)));

            // when
            BadgeResponse res = service.query(USERNAME, EnumSet.of(BadgeDataNeed.STREAK_DAYS));

            // then
            assertThat(res.streakDays()).isEqualTo(2);
        }

        @Test
        @DisplayName("배지조회_삼일공백_streak0")
        void 배지조회_삼일공백_streak0() {
            // given
            // 오늘·어제·그제 모두 미사용(3일 공백) → 유예 초과, 그 이전에 사용이 있어도 streak 0
            LocalDate today = today();
            stubYearRows(today, List.of(
                    dailyRow(today.minusDays(3).toString(), 40L),
                    dailyRow(today.minusDays(4).toString(), 50L)));

            // when
            BadgeResponse res = service.query(USERNAME, EnumSet.of(BadgeDataNeed.STREAK_DAYS));

            // then
            assertThat(res.streakDays()).isZero();
        }

        @Test
        @DisplayName("배지조회_0값일자끼임_streak끊김")
        void 배지조회_0값일자끼임_streak끊김() {
            // given
            // 오늘·어제 연속 사용 후 그제는 total=0(캐시 전용 사용일) → 미사용 취급으로 끊김, streak 2
            LocalDate today = today();
            stubYearRows(today, List.of(
                    dailyRow(today.toString(), 10L),
                    dailyRow(today.minusDays(1).toString(), 20L),
                    dailyRow(today.minusDays(2).toString(), 0L),
                    dailyRow(today.minusDays(3).toString(), 30L)));

            // when
            BadgeResponse res = service.query(USERNAME, EnumSet.of(BadgeDataNeed.STREAK_DAYS));

            // then
            assertThat(res.streakDays()).isEqualTo(2);
        }

        @Test
        @DisplayName("배지조회_데이터없음_streak0연간0")
        void 배지조회_데이터없음_streak0연간0() {
            // given
            LocalDate today = today();
            stubYearRows(today, List.of());

            // when
            BadgeResponse res = service.query(USERNAME, EnumSet.of(
                    BadgeDataNeed.DAILY_1Y, BadgeDataNeed.STREAK_DAYS, BadgeDataNeed.YEAR_TOKENS));

            // then
            assertThat(res.streakDays()).isZero();
            assertThat(res.yearTokens()).isZero();
            assertThat(res.daily1y()).hasSize(365);
            assertThat(res.daily1y()).allSatisfy(d -> assertThat(d.total()).isZero());
        }
    }
}
