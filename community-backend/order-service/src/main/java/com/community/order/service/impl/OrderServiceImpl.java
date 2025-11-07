package com.community.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.order.entity.Order;
import com.community.order.entity.OrderItem;
import com.community.order.mapper.OrderItemMapper;
import com.community.order.mapper.OrderMapper;
import com.community.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {
    private final OrderItemMapper orderItemMapper;

    @Override
    @Transactional
    public void createOrderWithItems(Order order, List<OrderItem> items) {
        this.save(order);
        Long oid = order.getId();
        for (OrderItem it : items) {
            it.setOrderId(oid);
            orderItemMapper.insert(it);
        }
    }

    @Override
    public List<Order> listUserOrders(Long userId) {
        return this.list(new LambdaQueryWrapper<Order>().eq(Order::getUserId, userId));
    }

    @Override
    public List<Order> listLeaderOrders(Long leaderId) {
        return this.list(new LambdaQueryWrapper<Order>().eq(Order::getLeaderId, leaderId));
    }
}
