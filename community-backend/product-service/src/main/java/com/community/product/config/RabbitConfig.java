package com.community.product.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String ORDER_EXCHANGE = "orderExchange";
    public static final String ORDER_CANCELED_QUEUE = "orderCanceledQueue";
    public static final String ORDER_CANCELED_ROUTING = "order.canceled";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE, true, false);
    }

    @Bean
    public Queue orderCanceledQueue() {
        return QueueBuilder.durable(ORDER_CANCELED_QUEUE).build();
    }

    @Bean
    public Binding bindOrderCanceled(TopicExchange orderExchange, Queue orderCanceledQueue) {
        return BindingBuilder.bind(orderCanceledQueue).to(orderExchange).with(ORDER_CANCELED_ROUTING);
    }
}
