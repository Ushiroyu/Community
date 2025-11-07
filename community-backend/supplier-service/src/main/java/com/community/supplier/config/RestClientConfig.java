package com.community.supplier.config;

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

    @Bean("orderClient")
    public RestClient orderClient(@Value("${ORDER_BASE_URL:http://localhost:8083}") String baseUrl) {
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

    /**
     * 若你项目中有引用，可保留此异常类（否则可删除）
     */
    public static class DownstreamServiceException extends RuntimeException {
        private final org.springframework.http.HttpStatus status;

        public DownstreamServiceException(org.springframework.http.HttpStatus status, String msg) {
            super(msg);
            this.status = status;
        }

        public org.springframework.http.HttpStatus getStatus() {
            return status;
        }
    }
}
