package com.community.supplier.controller;

import com.community.common.util.ApiResponse;
import com.community.supplier.entity.Supplier;
import com.community.supplier.service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.community.common.util.JwtUtil;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
public class SupplierController {
    private final SupplierService supplierService;

    @PostMapping("/apply")
    public ApiResponse apply(@RequestParam Long userId, @RequestParam String companyName) {
        Supplier s = new Supplier();
        s.setUserId(userId);
        s.setCompanyName(companyName);
        s.setStatus("PENDING");
        supplierService.save(s);
        return ApiResponse.ok("供应商申请已提交").data("supplierId", s.getId());
    }

    @GetMapping("/pending")
    public ApiResponse pending() {
        return ApiResponse.ok().data("pendingSuppliers",
                supplierService.lambdaQuery().eq(Supplier::getStatus, "PENDING").list());
    }

    @GetMapping("/{id}")
    public ApiResponse get(@PathVariable Long id) {
        var s = supplierService.getById(id);
        return s != null ? ApiResponse.ok().data("supplier", s) : ApiResponse.error("供应商不存在");
    }

    /**
     * 当前登录供应商信息（根据 JWT subject 取 userId）
     */
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public ApiResponse me(@RequestHeader(name = "Authorization", required = false) String authorization) {
        if (authorization == null || !JwtUtil.validateToken(authorization))
            return ApiResponse.error("未授权");
        Long uid = JwtUtil.getUserId(authorization);
        var s = supplierService.lambdaQuery().eq(Supplier::getUserId, uid).one();
        return s != null ? ApiResponse.ok().data("supplier", s) : ApiResponse.error("未找到供应商信息");
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','INTERNAL')")
    public ApiResponse setStatus(@PathVariable Long id, @RequestParam String status) {
        var s = supplierService.getById(id);
        if (s == null) return ApiResponse.error("供应商不存在");
        s.setStatus(status);
        supplierService.updateById(s);
        return ApiResponse.ok("状态已更新为 " + status);
    }
}
