package com.community.order.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.community.order.entity.Order;
import com.community.order.entity.OrderItem;

import java.util.List;

public interface OrderService extends IService<Order> {
    void createOrderWithItems(Order order, List<OrderItem> items);

    List<Order> listUserOrders(Long userId);

    List<Order> listLeaderOrders(Long leaderId);
}
