// 仅供参考：当你使用 @Bean 的方式手工绑定时，记得用 @Qualifier 指定具体的 Queue Bean，避免“找到了多个 Queue”
package com.community.product.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfigQualifierSample {

    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange("orderExchange", true, false);
    }

    @Bean(name = "orderPaidQueue")
    public Queue orderPaidQueue() {
        return new Queue("orderPaidQueue", true);
    }

    // ⚠️ 这里通过 @Qualifier 明确要注入的是 orderPaidQueue，避免和其它 Queue 混淆
    @Bean
    public Binding orderPaidBinding(@Qualifier("orderPaidQueue") Queue orderPaidQueue,
                                    DirectExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with("order.paid");
    }
}
