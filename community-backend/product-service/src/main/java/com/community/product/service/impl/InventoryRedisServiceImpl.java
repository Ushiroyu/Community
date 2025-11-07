package com.community.product.service.impl;

import com.community.product.entity.Product;
import com.community.product.mapper.ProductMapper;
import com.community.product.service.InventoryRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class InventoryRedisServiceImpl implements InventoryRedisService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ProductMapper productMapper;

    private static final String KEY_PREFIX = "stock:prod:";

    private static final DefaultRedisScript<Long> RESERVE_LUA;

    static {
        RESERVE_LUA = new DefaultRedisScript<>();
        RESERVE_LUA.setResultType(Long.class);
        RESERVE_LUA.setScriptText(
                "local stock = tonumber(redis.call('GET', KEYS[1]) or '0')\n" +
                        "local qty = tonumber(ARGV[1])\n" +
                        "if stock >= qty then \n" +
                        "  redis.call('DECRBY', KEYS[1], qty) \n" +
                        "  return 1 \n" +
                        "else \n" +
                        "  return 0 \n" +
                        "end\n"
        );
    }

    private String key(Long productId) {
        return KEY_PREFIX + productId;
    }

    @Override
    public boolean reserveStock(Long productId, int quantity) {
        if (productId == null || quantity <= 0) return false;
        Long ok = stringRedisTemplate.execute(RESERVE_LUA, Collections.singletonList(key(productId)), String.valueOf(quantity));
        return ok != null && ok == 1L;
    }

    @Override
    public void releaseStock(Long productId, int quantity) {
        if (productId == null || quantity <= 0) return;
        stringRedisTemplate.opsForValue().increment(key(productId), quantity);
    }

    @Override
    public void syncFromDb(Long productId) {
        if (productId == null) return;
        Product p = productMapper.selectById(productId);
        int stock = (p == null || p.getStock() == null) ? 0 : p.getStock();
        stringRedisTemplate.opsForValue().set(key(productId), String.valueOf(stock));
    }
}

