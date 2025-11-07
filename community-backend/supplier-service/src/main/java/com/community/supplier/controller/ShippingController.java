package com.community.supplier.controller;

import com.community.common.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/shipping")
@RequiredArgsConstructor
public class ShippingController {

    /**
     * 指向 order-service 的 RestClient
     * RestClientConfig 里定义的 bean 名是 "orderClient"（已带 Authorization/X-Request-Id 透传）
     */
    private final @Qualifier("orderClient") RestClient orderClient;

    /**
     * 供应商查看“待发货”订单
     */
    @GetMapping("/pending/{supplierId}")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public ApiResponse pending(@PathVariable Long supplierId) {
        ApiResponse resp = orderClient.get()
                .uri(u -> u.path("/orders/pending").queryParam("supplierId", supplierId).build())
                .retrieve()
                .body(ApiResponse.class);
        return resp == null ? ApiResponse.ok().data("orders", java.util.List.of()) : resp;
    }

    /**
     * 发货（可选 trackingNo）
     */
    @PostMapping("/{orderId}/ship")
    @PreAuthorize("hasAnyRole('SUPPLIER','ADMIN')")
    public ApiResponse ship(@PathVariable Long orderId,
                            @RequestParam(required = false) String trackingNo) {
        orderClient.post()
                .uri(u -> u.path("/orders/{id}/ship")
                        .queryParam("trackingNo", trackingNo == null ? "" : trackingNo)
                        .build(orderId))
                .retrieve()
                .toBodilessEntity();
        return ApiResponse.ok("订单已标记为发货");
    }
}
