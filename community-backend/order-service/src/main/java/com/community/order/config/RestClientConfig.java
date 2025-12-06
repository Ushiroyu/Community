package com.community.order.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean("productClient")
    public RestClient productClient(@Value("${PRODUCT_BASE_URL:http://localhost:8082}") String baseUrl) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(8).toMillis());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(f)
                .requestInterceptor((req, body, ex) -> {
                    var attrs = RequestContextHolder.getRequestAttributes();
                    if (attrs instanceof ServletRequestAttributes sra) {
                        var http = sra.getRequest();
                        var auth = http.getHeader("Authorization");
                        if (auth != null) req.getHeaders().add("Authorization", auth);
                        var xrid = http.getHeader("X-Request-Id");
                        if (xrid != null) req.getHeaders().add("X-Request-Id", xrid);
                    }
                    return ex.execute(req, body);
                })
                .build();
    }

    @Bean("userClient")
    public RestClient userClient(@Value("${USER_BASE_URL:http://localhost:8081}") String baseUrl) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(8).toMillis());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(f)
                .requestInterceptor((req, body, ex) -> {
                    var attrs = RequestContextHolder.getRequestAttributes();
                    if (attrs instanceof ServletRequestAttributes sra) {
                        var http = sra.getRequest();
                        var auth = http.getHeader("Authorization");
                        if (auth != null) req.getHeaders().add("Authorization", auth);
                        var xrid = http.getHeader("X-Request-Id");
                        if (xrid != null) req.getHeaders().add("X-Request-Id", xrid);
                    }
                    return ex.execute(req, body);
                })
                .build();
    }

    @Bean("leaderClient")
    public RestClient leaderClient(@Value("${LEADER_BASE_URL:http://localhost:8084}") String baseUrl) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout((int) Duration.ofSeconds(3).toMillis());
        f.setReadTimeout((int) Duration.ofSeconds(8).toMillis());

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(f)
                .requestInterceptor((req, body, ex) -> {
                    var attrs = RequestContextHolder.getRequestAttributes();
                    if (attrs instanceof ServletRequestAttributes sra) {
                        var http = sra.getRequest();
                        var auth = http.getHeader("Authorization");
                        if (auth != null) req.getHeaders().add("Authorization", auth);
                        var xrid = http.getHeader("X-Request-Id");
                        if (xrid != null) req.getHeaders().add("X-Request-Id", xrid);
                    }
                    return ex.execute(req, body);
                })
                .build();
    }
}
