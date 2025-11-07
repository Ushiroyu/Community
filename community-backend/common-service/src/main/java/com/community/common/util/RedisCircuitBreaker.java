package com.community.common.util;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

/**
 * 轻量熔断器：
 * - 统计窗口：recentWindowSeconds 内失败次数超过 failThreshold -> 打开 openSeconds
 * - 通过 Redis 共享状态：cb:open:{name}, cb:fail:{name}:{bucket}
 */
public class RedisCircuitBreaker {

    private final String name;
    private final StringRedisTemplate redis;
    private final int recentWindowSeconds;
    private final int failThreshold;
    private final int openSeconds;

    public RedisCircuitBreaker(String name, StringRedisTemplate redis, int recentWindowSeconds, int failThreshold, int openSeconds) {
        this.name = name;
        this.redis = redis;
        this.recentWindowSeconds = recentWindowSeconds;
        this.failThreshold = failThreshold;
        this.openSeconds = openSeconds;
    }

    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        String openKey = "cb:open:" + name;
        if (Boolean.TRUE.equals(redis.hasKey(openKey))) {
            return fallback.get();
        }
        try {
            T t = action.get();
            return t;
        } catch (Exception e) {
            // record failure
            String bucket = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            String failKey = "cb:fail:" + name + ":" + bucket;
            Long c = redis.opsForValue().increment(failKey);
            redis.expire(failKey, Duration.ofSeconds(recentWindowSeconds + 30));
            // count all buckets within window
            int sum = 0;
            for (int i = 0; i <= recentWindowSeconds / 60 + 1; i++) {
                String b = LocalDateTime.now().minusMinutes(i).format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
                String k = "cb:fail:" + name + ":" + b;
                String v = redis.opsForValue().get(k);
                if (v != null) try {
                    sum += Integer.parseInt(v);
                } catch (Exception ignored) {
                }
            }
            if (sum >= failThreshold) {
                redis.opsForValue().set(openKey, "1", Duration.ofSeconds(openSeconds));
            }
            throw e;
        }
    }
}
