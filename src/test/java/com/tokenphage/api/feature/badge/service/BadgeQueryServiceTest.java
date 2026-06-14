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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * BadgeQueryService.query / calcCacheHitRate 검증: 사용자 미존재 예외, 정상 응답 조립,
 * 30일 히트바 0값 채움(전체/일부/없음), 누적 null 처리, 캐시 히트율 경계 계산을 확인한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeQueryService 단위 테스트")
class BadgeQueryServiceTest {

    private static final String TIMEZONE = "Asia/Seoul";
    private static final Long GITHUB_ID = -12345L;
    private static final String USERNAME = "octocat";

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
            assertThatThrownBy(() -> service.query("ghost"))
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
            when(tokenRepo.findLast30Days(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of(daily));
            when(tokenRepo.findTop5Models(GITHUB_ID))
                    .thenReturn(List.of(opus, sonnet));
            when(tokenRepo.sumCacheTokens(GITHUB_ID))
                    .thenReturn(List.of(cache));

            // when
            BadgeResponse res = service.query(USERNAME);

            // then
            assertThat(res.username()).isEqualTo(USERNAME);
            assertThat(res.totalTokens()).isEqualTo(1_000L);
            assertThat(res.heatbar()).hasSize(30);
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
            when(tokenRepo.findLast30Days(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME);

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
            when(tokenRepo.findLast30Days(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME);

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
            when(tokenRepo.findLast30Days(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME);

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
            when(tokenRepo.findLast30Days(GITHUB_ID, from, today))
                    .thenReturn(List.of(midRow, todayRow));
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME);

            // then
            // 총 30개, from..today 오름차순으로 채워지고 마지막이 오늘
            assertThat(res.heatbar()).hasSize(30);
            assertThat(res.heatbar().get(0).date()).isEqualTo(from.toString());
            assertThat(res.heatbar().get(29).date()).isEqualTo(todayStr);
            // 조회된 2일치는 값 유지
            assertThat(res.heatbar().get(29).total()).isEqualTo(99L);
            assertThat(res.heatbar().get(14).date()).isEqualTo(midStr);
            assertThat(res.heatbar().get(14).total()).isEqualTo(42L);
            // 나머지 누락 날짜는 0으로 채워짐 (조회된 2일 제외 28개가 0)
            long zeroCount = res.heatbar().stream().filter(d -> d.total() == 0L).count();
            assertThat(zeroCount).isEqualTo(28L);
        }

        @Test
        @DisplayName("히트바_조회결과없음_전부0인30개구성")
        void 히트바_조회결과없음_전부0인30개구성() {
            // given
            LocalDate today = today();
            when(userRepo.findByUsername(USERNAME)).thenReturn(Optional.of(user()));
            when(tokenRepo.sumTotalTokens(GITHUB_ID)).thenReturn(0L);
            when(tokenRepo.findLast30Days(GITHUB_ID, today.minusDays(29), today))
                    .thenReturn(List.of());
            when(tokenRepo.findTop5Models(GITHUB_ID)).thenReturn(List.of());
            when(tokenRepo.sumCacheTokens(GITHUB_ID)).thenReturn(List.of());

            // when
            BadgeResponse res = service.query(USERNAME);

            // then
            assertThat(res.heatbar()).hasSize(30);
            assertThat(res.heatbar()).allSatisfy(d -> assertThat(d.total()).isZero());
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
            when(tokenRepo.findLast30Days(GITHUB_ID, today.minusDays(29), today))
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
            BadgeResponse res = service.query(USERNAME);

            // then
            assertThat(res.cacheHitRate()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("캐시히트율_cacheReadNull_0반환")
        void 캐시히트율_cacheReadNull_0반환() {
            // given
            stubBaseWith(List.of(cacheRow(null, 20L, 10L)));

            // when
            BadgeResponse res = service.query(USERNAME);

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
            BadgeResponse res = service.query(USERNAME);

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
            BadgeResponse res = service.query(USERNAME);

            // then
            assertThat(res.cacheHitRate()).isCloseTo(0.7, within(1e-9));
        }
    }
}
