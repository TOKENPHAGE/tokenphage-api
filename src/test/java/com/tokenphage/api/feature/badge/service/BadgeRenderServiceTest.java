package com.tokenphage.api.feature.badge.service;

import com.tokenphage.api.exception.AppException;
import com.tokenphage.api.feature.badge.dto.response.BadgeResponse;
import com.tokenphage.api.feature.badge.exception.BadgeErrorCode;
import com.tokenphage.api.feature.badge.svg.BadgeDataNeed;
import com.tokenphage.api.feature.badge.svg.SvgBuilder;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BadgeRenderService.getSvg()의 캐시 키 정규화, Redis 캐시 히트/미스 분기, 예외 전파를 검증한다.
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

    private BadgeRenderService service;

    @BeforeEach
    void setUp() {
        // 생성자 인자 순서 = 필드 선언 순서 (queryService, svgBuilder, redis)
        service = new BadgeRenderService(queryService, svgBuilder, redis);
        // @Value 주입 필드는 리플렉션으로 직접 주입
        ReflectionTestUtils.setField(service, "cacheTtlMinutes", CACHE_TTL_MINUTES);
    }

    /**
     * 배지 데이터를 위한 테스트 전용 픽스처를 생성한다 (메모리 전용, 실 DB 미접촉).
     */
    private BadgeResponse sampleData() {
        return new BadgeResponse(USERNAME, 12_345L, List.of(), List.of(), 0.5, 0L, 0, List.of());
    }

    /** theme/mode가 이미 정규화된 값(gpu/light)일 때의 정규화 스텁. */
    private void stubNormalizeIdentity() {
        when(svgBuilder.normalizeTheme(THEME)).thenReturn(THEME);
        when(svgBuilder.normalizeMode(MODE)).thenReturn(MODE);
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
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(EXPECTED_CACHE_KEY)).thenReturn(cachedSvg);

            // when
            String result = service.getSvg(USERNAME, THEME, MODE);

            // then
            assertThat(result).isEqualTo(cachedSvg);
            verify(queryService, never()).query(anyString(), any());
            verify(svgBuilder, never()).build(any(), anyString(), anyString());
            // 캐시 히트 시 set은 호출되지 않아야 한다
            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
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
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(EXPECTED_CACHE_KEY)).thenReturn(null);
            when(svgBuilder.needsOf(THEME)).thenReturn(GPU_NEEDS);
            when(queryService.query(USERNAME, GPU_NEEDS)).thenReturn(data);
            when(svgBuilder.build(data, THEME, MODE)).thenReturn(builtSvg);

            // when
            String result = service.getSvg(USERNAME, THEME, MODE);

            // then
            assertThat(result).isEqualTo(builtSvg);
            verify(queryService).query(USERNAME, GPU_NEEDS);
            verify(svgBuilder).build(data, THEME, MODE);
        }

        @Test
        @DisplayName("캐시 미스 시 생성된 SVG를 올바른 키와 TTL로 캐시에 저장한다")
        void 배지조회_캐시미스_올바른키와TTL로저장() {
            // given
            BadgeResponse data = sampleData();
            String builtSvg = "<svg>built</svg>";
            stubNormalizeIdentity();
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(EXPECTED_CACHE_KEY)).thenReturn(null);
            when(svgBuilder.needsOf(THEME)).thenReturn(GPU_NEEDS);
            when(queryService.query(USERNAME, GPU_NEEDS)).thenReturn(data);
            when(svgBuilder.build(data, THEME, MODE)).thenReturn(builtSvg);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);

            // when
            service.getSvg(USERNAME, THEME, MODE);

            // then
            verify(valueOps).set(keyCaptor.capture(), eq(builtSvg), ttlCaptor.capture());
            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_CACHE_KEY);
            assertThat(ttlCaptor.getValue()).isEqualTo(Duration.ofMinutes(CACHE_TTL_MINUTES));
        }

        @Test
        @DisplayName("미정규화 theme/mode → 정규화된 단일 키로 조회·저장하고 정규화값으로 빌드한다")
        void 배지조회_미정규화입력_정규화된키로수렴() {
            // given: 임의 theme/mode가 svgBuilder 정규화로 gpu/light로 수렴
            BadgeResponse data = sampleData();
            String builtSvg = "<svg>built</svg>";
            when(svgBuilder.normalizeTheme("ZZZ")).thenReturn("gpu");
            when(svgBuilder.normalizeMode("GARBAGE")).thenReturn("light");
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(EXPECTED_CACHE_KEY)).thenReturn(null);
            // needsOf는 정규화된 theme("gpu")로 호출된다
            when(svgBuilder.needsOf("gpu")).thenReturn(GPU_NEEDS);
            when(queryService.query(USERNAME, GPU_NEEDS)).thenReturn(data);
            when(svgBuilder.build(data, "gpu", "light")).thenReturn(builtSvg);

            ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

            // when
            String result = service.getSvg(USERNAME, "ZZZ", "GARBAGE");

            // then: raw 입력과 무관하게 정규화된 단일 키로 수렴 (캐시 증식 방지)
            assertThat(result).isEqualTo(builtSvg);
            verify(valueOps).set(keyCaptor.capture(), eq(builtSvg), any(Duration.class));
            assertThat(keyCaptor.getValue()).isEqualTo(EXPECTED_CACHE_KEY);
            verify(svgBuilder).build(data, "gpu", "light");
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
            when(redis.opsForValue()).thenReturn(valueOps);
            when(valueOps.get(EXPECTED_CACHE_KEY)).thenReturn(null);
            when(svgBuilder.needsOf(THEME)).thenReturn(GPU_NEEDS);
            when(queryService.query(USERNAME, GPU_NEEDS))
                    .thenThrow(new AppException(BadgeErrorCode.USER_NOT_FOUND));

            // when & then
            assertThatThrownBy(() -> service.getSvg(USERNAME, THEME, MODE))
                    .isInstanceOf(AppException.class)
                    .hasMessage(BadgeErrorCode.USER_NOT_FOUND.getMessage());

            // 예외 발생 시 SVG 생성과 캐시 저장은 일어나지 않아야 한다
            verify(svgBuilder, never()).build(any(), anyString(), anyString());
            verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
        }
    }
}
