package com.community.common.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class SimpleMetrics {
    private static final ConcurrentHashMap<String, AtomicLong> counters = new ConcurrentHashMap<>();

    public static void inc(String name) {
        counters.computeIfAbsent(name, k -> new AtomicLong()).incrementAndGet();
    }

    public static long get(String name) {
        return counters.getOrDefault(name, new AtomicLong()).get();
    }

    public static java.util.Map<String, Long> snapshot() {
        java.util.Map<String, Long> m = new java.util.HashMap<>();
        for (var e : counters.entrySet()) m.put(e.getKey(), e.getValue().get());
        return m;
    }
}
