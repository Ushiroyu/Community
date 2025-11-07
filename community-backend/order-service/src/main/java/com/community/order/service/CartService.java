package com.community.order.service;

import java.util.Map;

public interface CartService {
    void addItem(Long userId, Long productId, Integer quantity);

    void updateItem(Long userId, Long productId, Integer quantity);

    void removeItem(Long userId, Long productId);

    void clear(Long userId);

    Map<Long, Integer> getCart(Long userId);
}
