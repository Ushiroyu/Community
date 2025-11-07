package com.community.order.controller;

import com.community.common.util.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.*;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final StringRedisTemplate stringRedisTemplate;
    private final RestClient productClient;

    @Autowired
    public CartController(StringRedisTemplate stringRedisTemplate,
                          @Qualifier("productClient") RestClient productClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.productClient = productClient;
    }

    private String cartKey(Long userId) {
        return "cart:" + userId;
    }

    /**
     * Add to cart: use query params userId, productId, quantity
     */
    @PostMapping
    public ApiResponse add(@RequestParam Long userId,
                           @RequestParam Long productId,
                           @RequestParam Integer quantity) {
        if (quantity == null || quantity <= 0) return ApiResponse.error("数量必须 > 0");

        // Validate product exists
        Map<?, ?> prodRes = productClient.get().uri("/products/{id}", productId)
                .retrieve().body(Map.class);
        Map<?, ?> product = null;
        if (prodRes != null) {
            Object p = prodRes.get("product");
            if (p == null) p = prodRes.get("item");
            if (p instanceof Map<?, ?> m) product = m;
        }
        if (product == null) {
            return ApiResponse.error("商品不存在");
        }

        stringRedisTemplate.opsForHash().increment(cartKey(userId), String.valueOf(productId), quantity);
        return ApiResponse.ok("已加入购物车");
    }

    /**
     * List cart
     */
    @GetMapping
    public ApiResponse list(@RequestParam Long userId) {
        Map<Object, Object> all = stringRedisTemplate.opsForHash().entries(cartKey(userId));
        List<Map<String, Object>> items = new ArrayList<>();
        for (var e : all.entrySet()) {
            Long productId = Long.valueOf(String.valueOf(e.getKey()));
            Integer qty = Integer.valueOf(String.valueOf(e.getValue()));

            Map<?, ?> prodRes = productClient.get().uri("/products/{id}", productId)
                    .retrieve().body(Map.class);
            Map<?, ?> product = null;
            if (prodRes != null) {
                Object p = prodRes.get("product");
                if (p == null) p = prodRes.get("item");
                if (p instanceof Map<?, ?> m) product = m;
            }

            Map<String, Object> item = new HashMap<>();
            item.put("productId", productId);
            item.put("quantity", qty);
            item.put("product", product);
            if (product != null) {
                Object name = product.get("name");
                Object price = product.get("price");
                item.put("productName", name);
                item.put("price", price);
            }
            items.add(item);
        }
        return ApiResponse.ok().data("items", items);
    }

    /**
     * Remove one product from cart
     */
    @DeleteMapping("/{productId}")
    public ApiResponse remove(@RequestParam Long userId, @PathVariable Long productId) {
        stringRedisTemplate.opsForHash().delete(cartKey(userId), String.valueOf(productId));
        return ApiResponse.ok("已删除");
    }

    /**
     * Clear cart
     */
    @DeleteMapping
    public ApiResponse clear(@RequestParam Long userId) {
        stringRedisTemplate.delete(cartKey(userId));
        return ApiResponse.ok("已清空");
    }
}
