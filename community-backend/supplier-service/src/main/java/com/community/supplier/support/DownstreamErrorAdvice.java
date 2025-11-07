package com.community.supplier.support;

import com.community.common.util.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * 将下游(订单服务) 4xx/5xx 错误统一转换为 ApiResponse 返回给前端
 */
@RestControllerAdvice(basePackages = "com.community.supplier")
@RequiredArgsConstructor
public class DownstreamErrorAdvice {

    private final ObjectMapper mapper; // Boot 默认已注册

    /**
     * 下游返回了4xx/5xx（RestClient 会抛这个异常）
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiResponse> handleDownstream(RestClientResponseException ex) {
        ApiResponse body = tryDecode(ex.getResponseBodyAsString());
        if (body == null) {
            // 如果下游不是 ApiResponse 格式，就自己包一层
            body = ApiResponse.fail(ex.getStatusCode().value(),
                    "订单服务错误: " + ex.getStatusCode().value() + " " + ex.getStatusText());
        }
        // 保留下游的 HTTP 状态码；也可以改成 always-200，看你们前端约定
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /**
     * 无法连接下游（超时、连接错误等）
     */
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse> handleConnectError(ResourceAccessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(HttpStatus.BAD_GATEWAY.value(), "无法连接订单服务"));
    }

    /**
     * 其它 RestClient 相关异常的兜底
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiResponse> handleRestClient(RestClientException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.fail(HttpStatus.BAD_GATEWAY.value(), "调用订单服务失败"));
    }

    private ApiResponse tryDecode(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readValue(json, ApiResponse.class);
        } catch (Exception ignore) {
            return null;
        }
    }
}
