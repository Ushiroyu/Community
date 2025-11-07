package com.community.order.config;

import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventBusConfig {
    public static final String EVENT_EX = "event.bus.ex";

    @Bean
    public Exchange eventBusExchange() {
        return ExchangeBuilder.topicExchange(EVENT_EX).durable(true).build();
    }
}
