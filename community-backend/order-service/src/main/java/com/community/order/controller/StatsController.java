package com.community.order.controller;

import com.community.common.util.ApiResponse;
import com.community.order.dto.StatsDTO;
import com.community.order.mapper.StatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsMapper statsMapper;
    private final com.community.order.service.OrderService orderService;

    @GetMapping("/overview")
    public ApiResponse overview(@RequestParam java.time.Instant from,
                                @RequestParam java.time.Instant to) {
        Date fromDate = Date.from(from);
        Date toDate = Date.from(to);
        var dto = new StatsDTO(
                statsMapper.total(fromDate, toDate),
                statsMapper.paid(fromDate, toDate),
                statsMapper.shipped(fromDate, toDate),
                statsMapper.delivered(fromDate, toDate),
                statsMapper.gmv(fromDate, toDate)
        );
        return ApiResponse.ok().data("overview", dto);
    }

    @GetMapping("/top-products")
    public ApiResponse topProducts(@RequestParam java.time.Instant from,
                                   @RequestParam java.time.Instant to,
                                   @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok().data("items", statsMapper.topProducts(Date.from(from), Date.from(to), limit));
    }

    @GetMapping("/top-leaders")
    public ApiResponse topLeaders(@RequestParam java.time.Instant from,
                                  @RequestParam java.time.Instant to,
                                  @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok().data("items", statsMapper.topLeaders(Date.from(from), Date.from(to), limit));
    }

    /**
     * 团长总览
     */
    @GetMapping("/leader-overview")
    public ApiResponse leaderOverview(@RequestParam Long leaderId,
                                      @RequestParam java.time.Instant from,
                                      @RequestParam java.time.Instant to) {
        Date fromDate = Date.from(from);
        Date toDate = Date.from(to);
        var qwBase = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.community.order.entity.Order>()
                .eq(com.community.order.entity.Order::getLeaderId, leaderId)
                .between(com.community.order.entity.Order::getCreatedAt, fromDate, toDate);
        long total = orderService.count(qwBase);
        long paid = orderService.count(qwBase.clone().eq(com.community.order.entity.Order::getStatus, "PAID"));
        long shipped = orderService.count(qwBase.clone().eq(com.community.order.entity.Order::getStatus, "SHIPPED"));
        long delivered = orderService.count(qwBase.clone().eq(com.community.order.entity.Order::getStatus, "DELIVERED"));
        java.math.BigDecimal gmv = orderService.list(qwBase).stream()
                .map(com.community.order.entity.Order::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var dto = new com.community.order.dto.StatsDTO(total, paid, shipped, delivered, gmv);
        return ApiResponse.ok().data("overview", dto);
    }

    /**
     * 供应商总览
     */
    @GetMapping("/supplier-overview")
    public ApiResponse supplierOverview(@RequestParam Long supplierId,
                                        @RequestParam java.time.Instant from,
                                        @RequestParam java.time.Instant to) {
        Date fromDate = Date.from(from);
        Date toDate = Date.from(to);
        var qwBase = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.community.order.entity.Order>()
                .eq(com.community.order.entity.Order::getSupplierId, supplierId)
                .between(com.community.order.entity.Order::getCreatedAt, fromDate, toDate);
        long total = orderService.count(qwBase);
        long paid = orderService.count(qwBase.clone().eq(com.community.order.entity.Order::getStatus, "PAID"));
        long shipped = orderService.count(qwBase.clone().eq(com.community.order.entity.Order::getStatus, "SHIPPED"));
        long delivered = orderService.count(qwBase.clone().eq(com.community.order.entity.Order::getStatus, "DELIVERED"));
        java.math.BigDecimal gmv = orderService.list(qwBase).stream()
                .map(com.community.order.entity.Order::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        var dto = new com.community.order.dto.StatsDTO(total, paid, shipped, delivered, gmv);
        return ApiResponse.ok().data("overview", dto);
    }
}
