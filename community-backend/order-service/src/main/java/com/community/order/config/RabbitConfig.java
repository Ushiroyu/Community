package com.community.order.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {
    public static final String ORDER_EXCHANGE = "orderExchange";

    // 下单创建（扣库存）
    public static final String ORDER_CREATED_QUEUE = "orderCreatedQueue";
    public static final String ORDER_CREATED_ROUTING = "order.created";

    // 订单支付事件（有模块会用到）
    public static final String ORDER_PAID_QUEUE = "orderPaidQueue";
    public static final String ORDER_PAID_ROUTING = "order.paid";

    // 未支付自动取消：延时/超时
    public static final String ORDER_DELAY_QUEUE = "orderDelayQueue";
    public static final String ORDER_TIMEOUT_QUEUE = "orderTimeoutQueue";
    public static final String ORDER_TIMEOUT_ROUTING = "order.timeout";

    // 订单取消（通知回补库存等）
    public static final String ORDER_CANCELED_QUEUE = "orderCanceledQueue";
    public static final String ORDER_CANCELED_ROUTING = "order.canceled";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    // === queues ===
    @Bean
    public Queue orderCreatedQueue() {
        return QueueBuilder.durable(ORDER_CREATED_QUEUE).build();
    }

    @Bean
    public Queue orderPaidQueue() {
        return QueueBuilder.durable(ORDER_PAID_QUEUE).build();
    }

    @Bean
    public Queue orderDelayQueue() {
        return QueueBuilder.durable(ORDER_DELAY_QUEUE)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", ORDER_EXCHANGE,
                        "x-dead-letter-routing-key", ORDER_TIMEOUT_ROUTING
                ))
                .build();
    }

    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE).build();
    }

    @Bean
    public Queue orderCanceledQueue() {
        return QueueBuilder.durable(ORDER_CANCELED_QUEUE).build();
    }

    // === bindings（用 @Qualifier 指定具体 Queue，避免歧义） ===
    @Bean
    public Binding bindOrderCreated(
            @Qualifier("orderExchange") TopicExchange orderExchange,
            @Qualifier("orderCreatedQueue") Queue orderCreatedQueue) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with(ORDER_CREATED_ROUTING);
    }

    @Bean
    public Binding bindOrderPaid(
            @Qualifier("orderExchange") TopicExchange orderExchange,
            @Qualifier("orderPaidQueue") Queue orderPaidQueue) {
        return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with(ORDER_PAID_ROUTING);
    }

    @Bean
    public Binding bindOrderTimeout(
            @Qualifier("orderExchange") TopicExchange orderExchange,
            @Qualifier("orderTimeoutQueue") Queue orderTimeoutQueue) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderExchange).with(ORDER_TIMEOUT_ROUTING);
    }

    @Bean
    public Binding bindOrderCanceled(
            @Qualifier("orderExchange") TopicExchange orderExchange,
            @Qualifier("orderCanceledQueue") Queue orderCanceledQueue) {
        return BindingBuilder.bind(orderCanceledQueue).to(orderExchange).with(ORDER_CANCELED_ROUTING);
    }
}
