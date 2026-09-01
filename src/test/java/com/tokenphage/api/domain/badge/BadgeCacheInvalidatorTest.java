package com.tokenphage.api.domain.badge;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * BadgeCacheInvalidator 의 evict 동작(Redis 키 삭제, SCAN 순회)을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BadgeCacheInvalidator 단위 테스트")
class BadgeCacheInvalidatorTest {

    @Mock
    private StringRedisTemplate redis;

    @InjectMocks
    private BadgeCacheInvalidator invalidator;

    @Nested
    @DisplayName("evict(username)")
    class Evict {

        @Test
        @DisplayName("evict_스캔결과존재_실제캐시키(theme포함)일괄삭제")
        void evict_스캔결과존재_실제캐시키일괄삭제() {
            // given — 실제 캐시 키는 badge:{user}:{theme}:{mode} 4-세그먼트 형태다
            String username = "octocat";
            String key1 = "badge:octocat:gpu:light";
            String key2 = "badge:octocat:gpu:dark";

            @SuppressWarnings("unchecked")
            Cursor<String> cursor = (Cursor<String>) mock(Cursor.class);
            given(cursor.hasNext()).willReturn(true, true, false);
            given(cursor.next()).willReturn(key1, key2);
            given(redis.scan(any(ScanOptions.class))).willReturn(cursor);

            // when
            invalidator.evict(username);

            // then — SCAN(badge:octocat:*)으로 찾은 실제 키를 일괄 삭제한다
            then(redis).should().delete(List.of(key1, key2));
            then(cursor).should().close();
        }

        @Test
        @DisplayName("evict_스캔결과없음_삭제미호출(경계)")
        void evict_스캔결과없음_삭제미호출() {
            // given
            String username = "octocat";

            @SuppressWarnings("unchecked")
            Cursor<String> cursor = (Cursor<String>) mock(Cursor.class);
            given(cursor.hasNext()).willReturn(false);
            given(redis.scan(any(ScanOptions.class))).willReturn(cursor);

            // when
            invalidator.evict(username);

            // then
            then(redis).should(never()).delete(anyCollection());
            then(cursor).should(times(1)).close();
        }
    }
}
