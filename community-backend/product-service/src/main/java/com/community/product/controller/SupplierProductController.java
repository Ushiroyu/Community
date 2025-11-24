package com.community.product.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.community.common.util.ApiResponse;
import com.community.product.entity.Product;
import com.community.product.entity.Supplier;
import com.community.product.mapper.SupplierMapper;
import com.community.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/suppliers/{supplierId}/products")
@RequiredArgsConstructor
public class SupplierProductController {
    private final ProductService productService;
    private final SupplierMapper supplierMapper;

    // 分页查询“我发布的商品”
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @GetMapping
    public ApiResponse listMine(@PathVariable Long supplierId,
                                @RequestParam(defaultValue = "1") long page,
                                @RequestParam(defaultValue = "10") long size) {
        try {
            Long realSupplierId = resolveSupplierId(supplierId);
            if (realSupplierId == null) {
                return ApiResponse.ok("未找到供应商信息").data("records", java.util.Collections.emptyList()).data("total", 0);
            }
            Page<Product> p = new Page<>(page, size);
            var wrapper = new LambdaQueryWrapper<Product>().eq(Product::getSupplierId, realSupplierId);
            var res = productService.page(p, wrapper);
            return ApiResponse.ok().data("records", res.getRecords()).data("total", res.getTotal());
        } catch (Exception e) {
            return ApiResponse.error("查询失败：" + e.getMessage());
        }
    }

    // 供应商发布商品（默认待审核，上架状态为true/false由前端控制；推荐默认true）
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @PostMapping
    public ApiResponse create(@PathVariable Long supplierId, @RequestBody Product product) {
        try {
            Long realSupplierId = resolveSupplierId(supplierId);
            if (realSupplierId == null) return ApiResponse.error("未找到供应商信息");
            product.setId(null);
            product.setSupplierId(realSupplierId);
            product.setApproved(false);
            if (product.getStatus() == null) product.setStatus(true);
            productService.save(product);
            return ApiResponse.ok("商品已提交，等待审核").data("productId", product.getId());
        } catch (Exception e) {
            return ApiResponse.error("创建失败：" + e.getMessage());
        }
    }

    // 供应商修改自己的商品（禁止改 supplierId）
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @PutMapping("/{productId}")
    public ApiResponse update(@PathVariable Long supplierId, @PathVariable Long productId, @RequestBody Product req) {
        try {
            var p = productService.getById(productId);
            if (p == null) return ApiResponse.error("商品不存在");
            Long realSupplierId = resolveSupplierId(supplierId);
            if (realSupplierId == null || !realSupplierId.equals(p.getSupplierId())) return ApiResponse.error("无权操作他人商品");
            // 允许改名、分类、价格、库存、上下架；审核状态仍由管理员控制
            if (req.getName() != null) p.setName(req.getName());
            if (req.getCategoryId() != null) p.setCategoryId(req.getCategoryId());
            if (req.getPrice() != null) p.setPrice(req.getPrice());
            if (req.getStock() != null) p.setStock(req.getStock());
            if (req.getStatus() != null) p.setStatus(req.getStatus());
            productService.updateById(p);
            return ApiResponse.ok("商品已更新");
        } catch (Exception e) {
            return ApiResponse.error("更新失败：" + e.getMessage());
        }
    }

    // 供应商上下架
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @PutMapping("/{productId}/status")
    public ApiResponse toggleStatus(@PathVariable Long supplierId, @PathVariable Long productId, @RequestParam Boolean status) {
        try {
            var p = productService.getById(productId);
            if (p == null) return ApiResponse.error("商品不存在");
            Long realSupplierId = resolveSupplierId(supplierId);
            if (realSupplierId == null || !realSupplierId.equals(p.getSupplierId())) return ApiResponse.error("无权操作他人商品");
            p.setStatus(status);
            productService.updateById(p);
            return ApiResponse.ok("状态已更新为 " + status);
        } catch (Exception e) {
            return ApiResponse.error("更新失败：" + e.getMessage());
        }
    }

    // 删除商品
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @DeleteMapping("/{productId}")
    public ApiResponse delete(@PathVariable Long supplierId, @PathVariable Long productId) {
        try {
            var p = productService.getById(productId);
            if (p == null) return ApiResponse.error("商品不存在");
            Long realSupplierId = resolveSupplierId(supplierId);
            if (realSupplierId == null || !realSupplierId.equals(p.getSupplierId())) return ApiResponse.error("无权操作他人商品");
            productService.removeById(productId);
            return ApiResponse.ok("商品已删除");
        } catch (Exception e) {
            return ApiResponse.error("删除失败：" + e.getMessage());
        }
    }

    // 新增：当前登录供应商的商品列表
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @GetMapping("/me/list")
    public ApiResponse listMe(@RequestParam(defaultValue = "1") long page,
                              @RequestParam(defaultValue = "10") long size) {
        return listMine(resolveSupplierId(null), page, size);
    }

    // 新增：无需路径变量，按当前登录用户查询
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    @GetMapping("/all-by-user")
    public ApiResponse listByCurrentUser(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size) {
        try {
            Long realSupplierId = resolveSupplierId(null);
            if (realSupplierId == null) {
                return ApiResponse.ok("未找到供应商信息").data("records", java.util.Collections.emptyList()).data("total", 0);
            }
            Page<Product> p = new Page<>(page, size);
            var wrapper = new LambdaQueryWrapper<Product>().eq(Product::getSupplierId, realSupplierId);
            var res = productService.page(p, wrapper);
            return ApiResponse.ok().data("records", res.getRecords()).data("total", res.getTotal());
        } catch (Exception e) {
            return ApiResponse.error("查询失败：" + e.getMessage());
        }
    }

    private Long resolveSupplierId(Long pathSupplierId) {
        // 1) 直接按路径参数查供应商
        if (pathSupplierId != null && pathSupplierId > 0) {
            Supplier s = supplierMapper.selectById(pathSupplierId);
            if (s != null) return s.getId();
        }
        // 2) 根据当前登录用户ID查供应商
        Long currentUserId = currentUserId();
        if (currentUserId != null) {
            Supplier s = supplierMapper.selectOne(new LambdaQueryWrapper<Supplier>()
                    .eq(Supplier::getUserId, currentUserId)
                    .last("limit 1"));
            if (s != null) return s.getId();
        }
        return null;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        try {
            return Long.valueOf(auth.getName());
        } catch (Exception e) {
            return null;
        }
    }
}
