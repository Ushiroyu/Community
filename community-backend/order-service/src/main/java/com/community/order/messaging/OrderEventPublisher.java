package com.community.order.messaging;

import com.community.order.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderEventPublisher {

    private final ObjectProvider<RabbitTemplate> rabbitProvider;

    public OrderEventPublisher(ObjectProvider<RabbitTemplate> rabbitProvider) {
        this.rabbitProvider = rabbitProvider;
        this.rabbitProvider.ifAvailable(t ->
                t.setMessageConverter(new org.springframework.amqp.support.converter.Jackson2JsonMessageConverter())
        );
    }

    /**
     * 支付成功事件
     */
    public record OrderPaidEvent(Long orderId, List<Item> items) {
        public record Item(Long productId, Integer quantity) {
        }
    }

    public void publishOrderPaid(OrderPaidEvent event) {
        rabbitProvider.ifAvailable(t -> t.convertAndSend(
                RabbitConfig.ORDER_EXCHANGE,
                RabbitConfig.ORDER_PAID_ROUTING,
                event
        ));
    }
}
