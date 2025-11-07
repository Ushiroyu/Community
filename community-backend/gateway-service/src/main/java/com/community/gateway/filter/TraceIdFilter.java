package com.community.gateway.filter;

import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class TraceIdFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst("X-Trace-Id");
        final String traceId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString().replace("-", "")
                : incoming;

        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.headers(h -> h.set("X-Trace-Id", traceId)))
                .build();

        ServerHttpResponse resp = mutated.getResponse();
        resp.getHeaders().set("X-Trace-Id", traceId);
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
