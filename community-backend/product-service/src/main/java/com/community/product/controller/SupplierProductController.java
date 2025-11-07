package com.community.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.util.ApiResponse;
import com.community.common.security.RequiresPermissions;
import com.community.product.entity.Product;
import com.community.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suppliers/{supplierId}/products")
@RequiredArgsConstructor
public class SupplierProductController {
    private final ProductService productService;

    // 分页查询“我发布的商品”
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @RequiresPermissions({"supplier:product:list"})
    @GetMapping
    public ApiResponse listMine(@PathVariable Long supplierId,
                                @RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "10") long size) {
        Page<Product> p = new Page<>(page, size);
        var wrapper = new LambdaQueryWrapper<Product>().eq(Product::getSupplierId, supplierId);
        var res = productService.page(p, wrapper);
        return ApiResponse.ok().data("records", res.getRecords()).data("total", res.getTotal());
    }

    // 供应商发布商品（默认待审核，上架状态为true/false由前端控制；推荐默认true）
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @RequiresPermissions({"supplier:product:add"})
    @PostMapping
    public ApiResponse create(@PathVariable Long supplierId, @RequestBody Product product) {
        product.setId(null);
        product.setSupplierId(supplierId);
        product.setApproved(false);
        if (product.getStatus() == null) product.setStatus(true);
        productService.save(product);
        return ApiResponse.ok("商品已提交，等待审核").data("productId", product.getId());
    }

    // 供应商修改自己的商品（禁止改 supplierId）
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @RequiresPermissions({"supplier:product:edit"})
    @PutMapping("/{productId}")
    public ApiResponse update(@PathVariable Long supplierId, @PathVariable Long productId, @RequestBody Product req) {
        var p = productService.getById(productId);
        if (p == null) return ApiResponse.error("商品不存在");
        if (!supplierId.equals(p.getSupplierId())) return ApiResponse.error("无权操作他人商品");
        // 允许改名、分类、价格、库存、上下架；审核状态仍由管理员控制
        if (req.getName() != null) p.setName(req.getName());
        if (req.getCategoryId() != null) p.setCategoryId(req.getCategoryId());
        if (req.getPrice() != null) p.setPrice(req.getPrice());
        if (req.getStock() != null) p.setStock(req.getStock());
        if (req.getStatus() != null) p.setStatus(req.getStatus());
        productService.updateById(p);
        return ApiResponse.ok("商品已更新");
    }

    // 供应商上下架
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @RequiresPermissions({"supplier:product:edit"})
    @PutMapping("/{productId}/status")
    public ApiResponse toggleStatus(@PathVariable Long supplierId, @PathVariable Long productId, @RequestParam Boolean status) {
        var p = productService.getById(productId);
        if (p == null) return ApiResponse.error("商品不存在");
        if (!supplierId.equals(p.getSupplierId())) return ApiResponse.error("无权操作他人商品");
        p.setStatus(status);
        productService.updateById(p);
        return ApiResponse.ok("状态已更新为 " + status);
    }

    // 删除商品
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @RequiresPermissions({"supplier:product:delete"})
    @DeleteMapping("/{productId}")
    public ApiResponse delete(@PathVariable Long supplierId, @PathVariable Long productId) {
        var p = productService.getById(productId);
        if (p == null) return ApiResponse.error("商品不存在");
        if (!supplierId.equals(p.getSupplierId())) return ApiResponse.error("无权操作他人商品");
        productService.removeById(productId);
        return ApiResponse.ok("商品已删除");
    }
}
