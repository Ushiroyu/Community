package com.community.admin.controller;

import com.community.common.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final RestClient leaderClient;
    private final RestClient userClient;
    private final RestClient supplierClient;
    private final RestClient productClient; // 新增
    private final RestClient orderClient;   // 新增

    @PostMapping("/approve/leader/{leaderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse approveLeader(@PathVariable Long leaderId,
                                     @RequestParam Long userId,
                                     @RequestHeader(value = "Authorization", required = false) String auth) {
        leaderClient.put()
                .uri("/leaders/{id}/status?status=APPROVED", leaderId)
                .header("Authorization", auth)
                .retrieve().toBodilessEntity();

        userClient.put()
                .uri("/users/{id}/role?role=LEADER", userId)
                .header("Authorization", auth)
                .retrieve().toBodilessEntity();

        return ApiResponse.ok("团长申请已审核通过");
    }

    @PostMapping("/approve/supplier/{supplierId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse approveSupplier(@PathVariable Long supplierId,
                                       @RequestParam Long userId,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        supplierClient.put()
                .uri("/suppliers/{id}/status?status=APPROVED", supplierId)
                .header("Authorization", auth)
                .retrieve().toBodilessEntity();

        userClient.put()
                .uri("/users/{id}/role?role=SUPPLIER", userId)
                .header("Authorization", auth)
                .retrieve().toBodilessEntity();

        return ApiResponse.ok("供应商申请已审核通过");
    }

    /**
     * 新增：走 Admin 聚合审核商品，并把 Authorization 传给下游 product-service
     */
    @PutMapping("/products/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse approveProduct(@PathVariable Long id,
                                      @RequestParam(defaultValue = "true") boolean approved,
                                      @RequestHeader(value = "Authorization", required = false) String auth) {

        productClient.put()
                .uri("/products/{id}/approve?approved={a}", id, approved)
                .header("Authorization", auth)
                .retrieve().toBodilessEntity();

        return ApiResponse.ok(approved ? "商品审核通过" : "商品审核未通过");
    }

    /**
     * 管理概览（示例：转发到 order-service 的统计接口）
     */
    @GetMapping("/stats/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse overview(@RequestParam String from,
                                @RequestParam String to,
                                @RequestHeader(value = "Authorization", required = false) String auth) {
        Map<?, ?> body = orderClient.get()
                .uri(uri -> uri.path("/stats/overview").queryParam("from", from).queryParam("to", to).build())
                .header("Authorization", auth)
                .retrieve()
                .body(Map.class);
        Object ov = null;
        if (body != null) {
            ov = body.get("overview");
            if (ov == null && body.get("data") instanceof Map<?, ?> m) {
                ov = m.get("overview");
            }
        }
        return ApiResponse.ok().data("overview", ov);
    }
}
