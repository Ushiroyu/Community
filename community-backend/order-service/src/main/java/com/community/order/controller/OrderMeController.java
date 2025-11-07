package com.community.order.controller;

import com.community.common.util.ApiResponse;
import com.community.common.util.JwtUtil;
import com.community.order.entity.Order;
import com.community.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/orders", produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderMeController {

    @Autowired
    private OrderService orderService;

    /**
     * 获取当前登录用户的订单列表
     */
    @GetMapping("/me")
    public Object myOrders(@RequestHeader(name = "Authorization", required = false) String authorization) {
        if (authorization == null || !JwtUtil.validateToken(authorization)) {
            return ApiResponse.error("未授权");
        }
        Long userId = JwtUtil.getUserId(authorization);
        List<Order> list = orderService.listUserOrders(userId);
        return ApiResponse.ok().data("list", list);
    }

}
