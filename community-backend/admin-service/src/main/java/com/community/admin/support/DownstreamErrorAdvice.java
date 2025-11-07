package com.community.admin.support;

import com.community.common.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@ControllerAdvice
public class DownstreamErrorAdvice {

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiResponse> handleDownstream(RestClientResponseException ex) {
        var status = ex.getStatusCode();
        String body = ex.getResponseBodyAsString();
        ApiResponse r = ApiResponse.error(status.value(), body != null && !body.isBlank() ? body : status.toString());
        return ResponseEntity.status(status).body(r);
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiResponse> handleRestClient(RestClientException ex) {
        ApiResponse r = ApiResponse.error(HttpStatus.BAD_GATEWAY.value(), "下游服务不可用: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(r);
    }
}
