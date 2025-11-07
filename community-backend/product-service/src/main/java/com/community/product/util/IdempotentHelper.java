package com.community.product.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class IdempotentHelper {
    private final StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "mq:dup:";

    /**
     * Returns true if this msgId is first seen (i.e., proceed), false if duplicated.
     */
    public boolean firstProcess(String msgId, Duration ttl) {
        if (msgId == null || msgId.isBlank()) return true; // no id to dedup, allow
        Boolean ok = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + msgId, "1", ttl);
        return ok != null && ok;
    }
}

