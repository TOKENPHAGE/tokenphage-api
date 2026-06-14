package com.tokenphage.api.domain;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
            when(cursor.hasNext()).thenReturn(true, true, false);
            when(cursor.next()).thenReturn(key1, key2);
            when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);

            // when
            invalidator.evict(username);

            // then — SCAN(badge:octocat:*)으로 찾은 실제 키를 일괄 삭제한다
            verify(redis).delete(List.of(key1, key2));
            verify(cursor).close();
        }

        @Test
        @DisplayName("evict_스캔결과없음_삭제미호출(경계)")
        void evict_스캔결과없음_삭제미호출() {
            // given
            String username = "octocat";

            @SuppressWarnings("unchecked")
            Cursor<String> cursor = (Cursor<String>) mock(Cursor.class);
            when(cursor.hasNext()).thenReturn(false);
            when(redis.scan(any(ScanOptions.class))).thenReturn(cursor);

            // when
            invalidator.evict(username);

            // then
            verify(redis, never()).delete(anyCollection());
            verify(cursor, times(1)).close();
        }
    }
}
