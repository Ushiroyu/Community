package com.community.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.util.ApiResponse;
import com.community.product.entity.Product;
import com.community.product.service.ProductService;
import com.community.product.service.InventoryRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final InventoryRedisService inventoryRedisService;

    @GetMapping
    public ApiResponse page(@RequestParam(defaultValue = "1") long page,
                            @RequestParam(defaultValue = "10") long size,
                            @RequestParam(required = false) String keyword,
                            @RequestParam(required = false) Long categoryId,
                            @RequestParam(required = false) Boolean approved) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Product::getName, keyword);
        }
        if (categoryId != null) {
            qw.eq(Product::getCategoryId, categoryId);
        }
        if (approved != null) {
            qw.eq(Product::getApproved, approved);
        }
        Page<Product> res = productService.page(Page.of(page, size), qw);
        return ApiResponse.ok().data("list", res.getRecords()).data("total", res.getTotal());
    }

    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        Product p = productService.getById(id);
        return p == null ? ApiResponse.error("商品不存在") : ApiResponse.ok().data("item", p);
    }

    @PutMapping("/{id}/stock")
    public ApiResponse updateStock(@PathVariable Long id, @RequestParam int delta) {
        if (delta == 0) return ApiResponse.ok("无需变更");
        Product p = productService.getById(id);
        if (p == null) return ApiResponse.error("商品不存在");
        int newStock = (p.getStock() == null ? 0 : p.getStock()) + delta;
        if (newStock < 0) return ApiResponse.error("库存不足");
        p.setStock(newStock);
        productService.updateById(p);
        return ApiResponse.ok("库存已更新");
    }

    @PutMapping("/{id}/approve")
    public ApiResponse approve(@PathVariable Long id, @RequestParam(defaultValue = "true") boolean approved) {
        var p = productService.getById(id);
        if (p == null) return ApiResponse.error("商品不存在");
        p.setApproved(approved);
        productService.updateById(p);
        return ApiResponse.ok(approved ? "商品审核通过" : "商品审核未通过");
    }

    // 使用 Redis Lua 原子预占库存
    @PostMapping("/{id}/reserve")
    public ApiResponse reserve(@PathVariable Long id, @RequestParam int qty) {
        if (qty <= 0) return ApiResponse.error("数量非法");
        boolean ok = inventoryRedisService.reserveStock(id, qty);
        return ok ? ApiResponse.ok("预占成功") : ApiResponse.error("库存不足");
    }

    @PostMapping("/{id}/release")
    public ApiResponse release(@PathVariable Long id, @RequestParam int qty) {
        if (qty <= 0) return ApiResponse.error("数量非法");
        inventoryRedisService.releaseStock(id, qty);
        return ApiResponse.ok("已回补");
    }

    @PostMapping("/{id}/sync-stock")
    public ApiResponse sync(@PathVariable Long id) {
        inventoryRedisService.syncFromDb(id);
        return ApiResponse.ok("已同步");
    }
}
