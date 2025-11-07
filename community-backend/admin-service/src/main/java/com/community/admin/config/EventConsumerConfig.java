package com.community.admin.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class EventConsumerConfig {

    public static final String EVENT_EX = "event.bus.ex";
    public static final String ADMIN_Q = "event.admin.q";
    public static final String ADMIN_DLX_EX = "event.dlx.ex";
    public static final String ADMIN_DLQ = "event.admin.dlq.q";

    @Bean
    public TopicExchange eventBusEx() {
        return new TopicExchange(EVENT_EX, true, false);
    }

    @Bean
    public TopicExchange dlx() {
        return new TopicExchange(ADMIN_DLX_EX, true, false);
    }

    @Bean
    public Queue adminQueue() {
        return QueueBuilder.durable(ADMIN_Q)
                .withArguments(Map.of(
                        "x-dead-letter-exchange", ADMIN_DLX_EX,
                        "x-dead-letter-routing-key", "admin.fail"
                ))
                .build();
    }

    @Bean
    public Queue adminDlq() {
        return QueueBuilder.durable(ADMIN_DLQ).build();
    }

    /**
     * 绑定到事件总线，监听 order.*
     */
    @Bean
    public Binding adminBinding(Queue adminQueue, TopicExchange eventBusEx) {
        // 新版本这里的 .with(...) 已返回 Binding，删除 .noargs()
        return BindingBuilder.bind(adminQueue).to(eventBusEx).with("order.*");
    }

    /**
     * DLQ 绑定
     */
    @Bean
    public Binding adminDlqBinding(Queue adminDlq, TopicExchange dlx) {
        return BindingBuilder.bind(adminDlq).to(dlx).with("admin.fail");
    }
}
