package com.tokenphage.api.feature.badge.service;

import com.tokenphage.api.domain.badge.BadgeCode;
import com.tokenphage.api.domain.badge.service.BadgeGrantResult;
import com.tokenphage.api.domain.badge.service.BadgeGrantService;
import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.dto.response.BadgeSvgResponse;
import com.tokenphage.api.feature.badge.exception.BadgeErrorCode;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.BadgeMode;
import com.tokenphage.api.feature.badge.svg.SvgBuilder;
import com.tokenphage.api.feature.badge.svg.theme.locked.LockedBadgeTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * BadgeRenderService.getSvg()의 자격 확인, 캐시 키 정규화, 캐시 적중·미적중, 예외 전파를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeRenderService 단위 테스트")
class BadgeRenderServiceTest {

    private static final int CACHE_TTL_MINUTES = 60;
    private static final String USERNAME = "octocat";
    private static final String THEME = "gpu";
    private static final String MODE = "light";
    private static final String EXPECTED_CACHE_KEY = "badge:octocat:gpu:light";
    // gpu 테마가 선언하는 기본 needs (needsOf 스텁 반환값 = query에 그대로 전달됨)
    private static final Set<BadgeDataNeed> GPU_NEEDS = EnumSet.of(
            BadgeDataNeed.TOTAL_TOKENS, BadgeDataNeed.DAILY_30D,
            BadgeDataNeed.TOP_MODELS, BadgeDataNeed.CACHE_HIT_RATE);

    @Mock
    private BadgeQueryService queryService;

    @Mock
    private SvgBuilder svgBuilder;

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private BadgeGrantService badgeGrantService;

    @Mock
    private LockedBadgeTheme lockedBadgeTheme;

    private BadgeRenderService service;

    @BeforeEach
    void setUp() {
        // 생성자 인자 순서 = 필드 선언 순서
        service = new BadgeRenderService(queryService, svgBuilder, redis, badgeGrantService, lockedBadgeTheme);
        // @Value 주입 필드는 리플렉션으로 직접 주입
        ReflectionTestUtils.setField(service, "cacheTtlMinutes", CACHE_TTL_MINUTES);
    }

    /**
     * 배지 데이터를 위한 테스트 전용 픽스처를 생성한다 (메모리 전용, 실 DB 미접촉).
     */
    private BadgeResponse sampleData() {
        return new BadgeResponse(USERNAME, 12_345L, List.of(), List.of(), 0.5, 0L, 0, List.of(), "");
    }

    /** theme이 이미 정규화된 값(gpu)이고 mode가 LIGHT로 확정될 때의 정규화 스텁. */
    private void stubNormalizeIdentity() {
        given(svgBuilder.normalizeTheme(THEME)).willReturn(THEME);
        given(svgBuilder.resolveMode(THEME, MODE)).willReturn(BadgeMode.LIGHT);
    }

    /** 자격이 있는 정상 경로 스텁. 이 스텁이 없으면 Mockito 기본값(null)으로 NPE가 난다. */
    private void stubGranted() {
        given(badgeGrantService.resolveGrant(anyString(), anyString()))
                .willReturn(BadgeGrantResult.allow());
    }

    @Nested
    @DisplayName("getSvg - 자격 게이트")
    class GrantGate {

        @Test
        @DisplayName("배지조회_자격없음_잠금SVG반환하고조회캐시생략")
        void 배지조회_자격없음_잠금SVG반환하고조회캐시생략() {
            // given
            String lockedSvg = "<svg>locked</svg>";
            given(svgBuilder.normalizeTheme(THEME)).willReturn(THEME);
            given(badgeGrantService.resolveGrant(USERNAME, THEME))
                    .willReturn(BadgeGrantResult.deny("Contributor", "PR을 보내주세요"));
            // 잠금 렌더는 locked 테마 자신의 지원 집합으로 모드를 다시 정규화한다
            given(svgBuilder.resolveMode(BadgeCode.LOCKED.getCode(), MODE)).willReturn(BadgeMode.LIGHT);
            given(lockedBadgeTheme.render("Contributor", "PR을 보내주세요", false)).willReturn(lockedSvg);

            // when
            BadgeSvgResponse result = service.getSvg(USERNAME, THEME, MODE);

            // then
            assertThat(result.svg()).isEqualTo(lockedSvg);
            assertThat(result.granted()).isFalse();
            // 자격 거부 경로는 집계 조회·SVG 빌드·캐시 저장을 전부 건너뛴다
            then(queryService).should(never()).query(anyString(), anyString(), any());
            then(svgBuilder).should(never()).build(any(), anyString(), anyString());
            then(valueOps).should(never()).set(anyString(), anyString(), any(Duration.class));
        }

        @Test
        @DisplayName("배지조회_자격없음_캐시를조회조차하지않는다")
        void 배지조회_자격없음_캐시를조회조차하지않는다() {
            // given
            given(svgBuilder.normalizeTheme(THEME)).willReturn(THEME);
            given(badgeGrantService.resolveGrant(USERNAME, THEME))
                    .willReturn(BadgeGrantResult.deny(null, null));
            given(svgBuilder.resolveMode(BadgeCode.LOCKED.getCode(), MODE)).willReturn(BadgeMode.LIGHT);
            given(lockedBadgeTheme.render(null, null, false)).willReturn("<svg>locked</svg>");

            // when
            service.getSvg(USERNAME, THEME, MODE);

            // then
            // 게이트가 캐시보다 앞에 있어야 캐시 키와 내용 불일치가 생기지 않는다
            then(redis).should(never()).opsForValue();
        }

        @Test
        @DisplayName("배지조회_캐시히트여도_자격판정은매번수행")
        void 배지조회_캐시히트여도_자격판정은매번수행() {
            // given
            stubNormalizeIdentity();
            stubGranted();
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get(EXPECTED_CACHE_KEY)).willReturn("<svg>cached</svg>");

            // when
            service.getSvg(USERNAME, THEME, MODE);

            // then
            // 판정을 캐시 뒤로 미루면 자격 회수가 최대 60분간 반영되지 않는다
            then(badgeGrantService).should().resolveGrant(USERNAME, THEME);
        }
    }

    @Nested
    @DisplayName("getSvg - 캐시 히트")
    class CacheHit {

        @Test
        @DisplayName("캐시에 값이 있으면 그대로 반환하고 조회/빌드를 호출하지 않는다")
        void 배지조회_캐시히트_캐시값반환하고조회생략() {
            // given
            String cachedSvg = "<svg>cached</svg>";
            stubNormalizeIdentity();
            stubGranted();
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get(EXPECTED_CACHE_KEY)).willReturn(cachedSvg);

            // when
            BadgeSvgResponse result = service.getSvg(USERNAME, THEME, MODE);

            // then
            assertThat(result.svg()).isEqualTo(cachedSvg);
            assertThat(result.granted()).isTrue();
            then(queryService).should(never()).query(anyString(), anyString(), any());
            then(svgBuilder).should(never()).build(any(), anyString(), anyString());
            // 캐시 히트 시 set은 호출되지 않아야 한다
            then(valueOps).should(never()).set(anyString(), anyString(), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("getSvg - 캐시 미스")
    class CacheMiss {

        @Test
        @DisplayName("캐시가 없으면 조회 후 SVG를 생성하고 결과를 반환한다")
        void 배지조회_캐시미스_조회후빌드결과반환() {
            // given
            BadgeResponse data = sampleData();
            String builtSvg = "<svg>built</svg>";
            stubNormalizeIdentity();
            stubGranted();
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get(EXPECTED_CACHE_KEY)).willReturn(null);
            given(svgBuilder.needsOf(THEME)).willReturn(GPU_NEEDS);
            given(queryService.query(USERNAME, THEME, GPU_NEEDS)).willReturn(data);
            given(svgBuilder.build(data, THEME, MODE)).willReturn(builtSvg);

            // when
            BadgeSvgResponse result = service.getSvg(USERNAME, THEME, MODE);

            // then
            assertThat(result.svg()).isEqualTo(builtSvg);
            assertThat(result.granted()).isTrue();
            then(queryService).should().query(USERNAME, THEME, GPU_NEEDS);
            then(svgBuilder).should().build(data, THEME, MODE);
        }

        @Test
        @DisplayName("캐시 미스 시 생성된 SVG를 올바른 키와 TTL로 캐시에 저장한다")
        void 배지조회_캐시미스_올바른키와TTL로저장() {
            // given
            BadgeResponse data = sampleData();
            String builtSvg = "<svg>built</svg>";
            stubNormalizeIdentity();
            stubGranted();
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get(EXPECTED_CACHE_KEY)).willReturn(null);
            given(svgBuilder.needsOf(THEME)).willReturn(GPU_NEEDS);
            given(queryService.query(USERNAME, THEME, GPU_NEEDS)).willReturn(data);
            given(svgBuilder.build(data, THEME, MODE)).willReturn(builtSvg);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

            // when
            service.getSvg(USERNAME, THEME, MODE);

            // then
            then(valueOps).should().set(keyCaptor.capture(), eq(builtSvg), ttlCaptor.capture());
            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_CACHE_KEY);
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(CACHE_TTL_MINUTES));
        }

        @Test
        @DisplayName("미정규화 theme/mode → 정규화된 단일 키로 조회·저장하고 정규화값으로 빌드한다")
        void 배지조회_미정규화입력_정규화된키로수렴() {
            // given: 임의 theme/mode가 svgBuilder 정규화로 gpu/light로 수렴
            BadgeResponse data = sampleData();
            String builtSvg = "<svg>built</svg>";
            given(svgBuilder.normalizeTheme("ZZZ")).willReturn("gpu");
            given(svgBuilder.resolveMode("gpu", "GARBAGE")).willReturn(BadgeMode.LIGHT);
            stubGranted();
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get(EXPECTED_CACHE_KEY)).willReturn(null);
            // needsOf는 정규화된 theme("gpu")로 호출된다
            given(svgBuilder.needsOf("gpu")).willReturn(GPU_NEEDS);
            given(queryService.query(USERNAME, THEME, GPU_NEEDS)).willReturn(data);
            given(svgBuilder.build(data, "gpu", "light")).willReturn(builtSvg);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            BadgeSvgResponse result = service.getSvg(USERNAME, "ZZZ", "GARBAGE");

            // then: raw 입력과 무관하게 정규화된 단일 키로 수렴 (캐시 증식 방지)
            assertThat(result.svg()).isEqualTo(builtSvg);
            then(valueOps).should().set(keyCaptor.capture(), eq(builtSvg), any(Duration.class));
            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_CACHE_KEY);
            then(svgBuilder).should().build(data, "gpu", "light");
            // 자격 판정도 정규화된 theme으로 이뤄져야 한다
            then(badgeGrantService).should().resolveGrant(USERNAME, "gpu");
        }
    }

    @Nested
    @DisplayName("getSvg - 실패")
    class Failure {

        @Test
        @DisplayName("조회가 AppException을 던지면 그대로 전파하고 캐시에 저장하지 않는다")
        void 배지조회_조회실패_예외전파하고저장생략() {
            // given
            stubNormalizeIdentity();
            stubGranted();
            given(redis.opsForValue()).willReturn(valueOps);
            given(valueOps.get(EXPECTED_CACHE_KEY)).willReturn(null);
            given(svgBuilder.needsOf(THEME)).willReturn(GPU_NEEDS);
            given(queryService.query(USERNAME, THEME, GPU_NEEDS))
                    .willThrow(new AppException(BadgeErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> service.getSvg(USERNAME, THEME, MODE))
                    .isInstanceOf(AppException.class)
                    .hasMessage(BadgeErrorCode.USER_NOT_FOUND.getMessage());

            // 예외 발생 시 SVG 생성과 캐시 저장은 일어나지 않아야 한다
            then(svgBuilder).should(never()).build(any(), anyString(), anyString());
            then(valueOps).should(never()).set(anyString(), anyString(), any(Duration.class));
        }
    }
}
