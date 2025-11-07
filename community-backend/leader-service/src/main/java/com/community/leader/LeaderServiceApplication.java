package com.community.leader;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = {"com.community"})
@MapperScan("com.community.leader.mapper")
public class LeaderServiceApplication {
    @Bean
    public RestClient orderClient() {
        String base = System.getenv().getOrDefault("ORDER_BASE_URL", "http://localhost:8083");
        return RestClient.builder().baseUrl(base).build();
    }

    public static void main(String[] args) {
        SpringApplication.run(LeaderServiceApplication.class, args);
    }
}
