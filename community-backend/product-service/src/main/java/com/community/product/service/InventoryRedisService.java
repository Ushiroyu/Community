package com.community.product.service;

public interface InventoryRedisService {
    boolean reserveStock(Long productId, int quantity);

    void releaseStock(Long productId, int quantity);

    void syncFromDb(Long productId);
}

