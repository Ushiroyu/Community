package com.community.order.listener;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.community.order.config.RabbitConfig;
import com.community.order.entity.Order;
import com.community.order.entity.OrderItem;
import com.community.order.service.OrderService;
import com.community.order.mapper.OrderItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class OrderTimeoutListener {

    private final OrderService orderService;
    private final OrderItemMapper orderItemMapper;
    private final @Qualifier("productClient") RestClient productClient;

    @Autowired
    public OrderTimeoutListener(OrderService orderService,
                                OrderItemMapper orderItemMapper,
                                @Qualifier("productClient") RestClient productClient) {
        this.orderService = orderService;
        this.orderItemMapper = orderItemMapper;
        this.productClient = productClient;
    }

    /**
     * 监听超时队列：如订单仍未支付，取消并回补 Redis 预占库存
     */
    @RabbitListener(queues = RabbitConfig.ORDER_TIMEOUT_QUEUE)
    public void onTimeout(Map<String, Object> payload) {
        try {
            Long orderId = Long.valueOf(String.valueOf(payload.get("orderId")));
            Order o = orderService.getById(orderId);
            if (o == null) return;
            if (!"CREATED".equals(o.getStatus())) return; // 已支付/取消则忽略

            // 取消订单
            o.setStatus("CANCELED");
            orderService.updateById(o);

            // 回补预占库存
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
            for (OrderItem it : items) {
                try {
                    productClient.post()
                            .uri("/products/{id}/release?qty={qty}", it.getProductId(), it.getQuantity())
                            .retrieve()
                            .toBodilessEntity();
                } catch (Exception ex) {
                    log.warn("release stock failed productId={} qty={}", it.getProductId(), it.getQuantity());
                    log.warn("release stock exception", ex);
                }
            }

            log.info("Order {} canceled by timeout", orderId);
        } catch (Exception e) {
            log.error("handle timeout error", e);
        }
    }
}
