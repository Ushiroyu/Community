package com.community.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

@SpringBootApplication(scanBasePackages = {"com.community"})
public class AdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }

    @Bean
    RestClient leaderClient() {
        String base = System.getenv().getOrDefault("LEADER_BASE_URL", "http://localhost:8084");
        return RestClient.builder().baseUrl(base).build();
    }

    @Bean
    RestClient userClient() {
        String base = System.getenv().getOrDefault("USER_BASE_URL", "http://localhost:8081");
        return RestClient.builder().baseUrl(base).build();
    }

    @Bean
    RestClient supplierClient() {
        String base = System.getenv().getOrDefault("SUPPLIER_BASE_URL", "http://localhost:8085");
        return RestClient.builder().baseUrl(base).build();
    }

    /**
     * 新增：产品服务
     */
    @Bean
    RestClient productClient() {
        String base = System.getenv().getOrDefault("PRODUCT_BASE_URL", "http://localhost:8082");
        return RestClient.builder().baseUrl(base).build();
    }

    /**
     * 新增：订单服务（统计等聚合调用）
     */
    @Bean
    RestClient orderClient() {
        String base = System.getenv().getOrDefault("ORDER_BASE_URL", "http://localhost:8083");
        return RestClient.builder().baseUrl(base).build();
    }
}
