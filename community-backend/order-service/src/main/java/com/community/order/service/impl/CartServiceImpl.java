package com.community.order.service.impl;

import com.community.order.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final StringRedisTemplate redis;

    private String key(Long userId) {
        return "cart:%d".formatted(userId);
    }

    @Override
    public void addItem(Long userId, Long productId, Integer quantity) {
        if (quantity == null || quantity <= 0) quantity = 1;
        var ops = redis.opsForHash();
        String k = String.valueOf(productId);
        Integer old = ops.hasKey(key(userId), k) ? Integer.valueOf(ops.get(key(userId), k).toString()) : 0;
        ops.put(key(userId), k, String.valueOf(old + quantity));
    }

    @Override
    public void updateItem(Long userId, Long productId, Integer quantity) {
        var ops = redis.opsForHash();
        if (quantity == null || quantity <= 0) {
            ops.delete(key(userId), String.valueOf(productId));
        } else {
            ops.put(key(userId), String.valueOf(productId), String.valueOf(quantity));
        }
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        redis.opsForHash().delete(key(userId), String.valueOf(productId));
    }

    @Override
    public void clear(Long userId) {
        redis.delete(key(userId));
    }

    @Override
    public Map<Long, Integer> getCart(Long userId) {
        var map = redis.opsForHash().entries(key(userId));
        return map.entrySet().stream().collect(Collectors.toMap(
                e -> Long.valueOf(e.getKey().toString()),
                e -> Integer.valueOf(e.getValue().toString())
        ));
    }
}
