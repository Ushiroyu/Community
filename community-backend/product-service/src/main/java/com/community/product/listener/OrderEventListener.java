package com.community.product.listener;

import com.community.product.entity.Product;
import com.community.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.amqp.core.Message;
import com.community.product.util.IdempotentHelper;

import java.time.Duration;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventListener {

    private final ProductService productService;
    private final IdempotentHelper idem;


    /**
     * 订单创建（立即扣库存）
     * 注意：实际生产应结合库存预占或乐观锁处理并发。
     */
    // 已有 onOrderCreated 监听相同队列，此处删除重复监听以避免重复扣减

    /**
     * 订单未支付超时（自动释放库存）
     */
    @RabbitListener(queues = "orderTimeoutQueue")
    public void handleOrderTimeout(Map<String, Object> orderData) {
        try {
            Long productId = Long.valueOf(String.valueOf(orderData.get("productId")));
            Integer quantity = Integer.valueOf(String.valueOf(orderData.get("quantity")));
            var p = productService.getById(productId);
            if (p != null) {
                int base = p.getStock() == null ? 0 : p.getStock();
                p.setStock(base + quantity);
                productService.updateById(p);
            }
        } catch (Exception e) {
            log.error("handle order timeout failed: {}", orderData, e);
        }
    }

    /**
     * 订单支付成功：最终扣减库存
     */
    @RabbitListener(queues = "orderPaidQueue")
    public void handleOrderPaid(java.util.Map<String, Object> orderData, Message message) {
        try {
            String msgId = message.getMessageProperties().getMessageId();
            if (!idem.firstProcess(msgId, Duration.ofHours(24))) {
                return; // duplicated delivery
            }
            Object itemsObj = orderData.get("items");
            if (itemsObj instanceof java.util.List<?> list) {
                for (Object o : list) {
                    if (o instanceof java.util.Map<?, ?> m) {
                        Long productId = Long.valueOf(String.valueOf(m.get("productId")));
                        Integer quantity = Integer.valueOf(String.valueOf(m.get("quantity")));
                        var p = productService.getById(productId);
                        if (p != null) {
                            int newStock = (p.getStock() == null ? 0 : p.getStock()) - quantity;
                            p.setStock(Math.max(newStock, 0));
                            productService.updateById(p);
                        }
                    }
                }
            } else {
                // 兼容旧负载：直接从根读 productId/quantity
                Long productId = Long.valueOf(String.valueOf(orderData.get("productId")));
                Integer quantity = Integer.valueOf(String.valueOf(orderData.get("quantity")));
                var p = productService.getById(productId);
                if (p != null) {
                    int newStock = (p.getStock() == null ? 0 : p.getStock()) - quantity;
                    p.setStock(Math.max(newStock, 0));
                    productService.updateById(p);
                }
            }
        } catch (Exception e) {
            log.error("handle order paid failed: {}", orderData, e);
        }
    }

    /**
     * 扣库存监听（order.created）
     */
    @RabbitListener(queues = "orderCreatedQueue")
    public void onOrderCreated(Map<String, Object> payload) {
        Long productId = Long.valueOf(String.valueOf(payload.get("productId")));
        Integer quantity = Integer.valueOf(String.valueOf(payload.get("quantity")));
        Product p = productService.getById(productId);
        if (p == null) return;
        int newStock = (p.getStock() == null ? 0 : p.getStock()) - quantity;
        if (newStock < 0) newStock = 0; // 简单兜底
        p.setStock(newStock);
        productService.updateById(p);
        log.info("Stock deducted for product {}, -{} -> {}", productId, quantity, newStock);
    }

    /**
     * 新增：订单取消回补库存（order.canceled）
     */
    @RabbitListener(queues = "orderCanceledQueue")
    public void onOrderCanceled(Map<String, Object> payload) {
        // 这里假设你的订单项只有一个商品，如有多项可拓展为查询 order_items 再逐个回补
        // 为简单演示，我们仅打印；你也可把 productId/quantity 一并带入取消消息
        log.info("Order canceled event: {}", payload);
        // 如果你希望回补库存，建议在取消时发送 productId & quantity；
        // 或者 product-service 维护一个 outbox/订单明细快照，此处略。
    }
}
