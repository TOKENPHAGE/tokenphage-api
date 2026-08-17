package com.tokenphage.api.domain.badge;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BadgeCacheInvalidator {

    private final StringRedisTemplate redis;

    /**
     * 특정 사용자의 모든 배지 SVG 캐시를 무효화한다(theme/mode 조합 전부).
     * <p>
     * 실제 캐시 키는 badge:{username}:{theme}:{mode} 형태이므로, theme/mode를 열거하지 않고
     * SCAN(badge:{username}:*)으로 실제 키를 찾아 삭제한다.
     * sync·reset 등 사용자 데이터가 바뀐 직후 배지 즉시 갱신을 보장한다.
     *
     * @param username 캐시를 삭제할 GitHub 사용자명 (null 불허)
     * @Since 2026-05-24
     */
    public void evict(String username) {
        evictByUsername(username);
    }

    /**
     * badge:{username}:* 패턴의 모든 캐시 키를 SCAN으로 찾아 일괄 삭제한다.
     * <p>
     * 운영 블로킹을 피하기 위해 KEYS 대신 SCAN을 사용한다.
     */
    private void evictByUsername(String username) {
        ScanOptions options = ScanOptions.scanOptions().match("badge:" + username + ":*").count(100).build();
        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redis.scan(options)) {
            while (cursor.hasNext()) {
                keys.add(cursor.next());
            }
        }
        if (!keys.isEmpty()) {
            redis.delete(keys);
        }
    }
}
