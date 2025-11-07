package com.community.order.controller;

import com.community.common.util.ApiResponse;
import com.community.order.entity.Order;
import com.community.order.entity.ShipmentEvent;
import com.community.order.service.OrderService;
import com.community.order.service.ShipmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/orders/{orderId}")
@RequiredArgsConstructor
public class ShipmentController {
    private final ShipmentService shipmentService;
    private final OrderService orderService;

    @GetMapping("/tracking")
    public ApiResponse tracking(@PathVariable Long orderId) {
        return ApiResponse.ok().data("events", shipmentService.listByOrderId(orderId));
    }

    @PostMapping("/tracking")
    public ApiResponse addTracking(@PathVariable Long orderId, @RequestBody ShipmentEvent body) {
        body.setId(null);
        body.setOrderId(orderId);
        if (body.getEventTime() == null) body.setEventTime(new Date());
        shipmentService.addEvent(body);
        return ApiResponse.ok("已追加物流节点");
    }

    @GetMapping
    public ApiResponse list(@PathVariable Long orderId) {
        var order = orderService.getById(orderId);
        if (order == null) return ApiResponse.error("订单不存在");
        return ApiResponse.ok().data("events", shipmentService.listByOrderId(orderId));
    }

    @PostMapping
    public ApiResponse add(@PathVariable Long orderId, @RequestBody ShipmentEvent req) {
        Order order = orderService.getById(orderId);
        if (order == null) return ApiResponse.error("订单不存在");
        req.setOrderId(orderId);
        shipmentService.addEvent(req);
        return ApiResponse.ok("已记录物流节点");
    }
}
