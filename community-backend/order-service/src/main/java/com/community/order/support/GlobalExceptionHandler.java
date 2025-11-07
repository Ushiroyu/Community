package com.community.order.support;

import com.community.common.util.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.stereotype.Component;

@RestControllerAdvice
@Component("orderGlobalExceptionHandler")
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse handleValid(MethodArgumentNotValidException e) {
        var msg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(msg);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse handleDenied(AccessDeniedException e) {
        return ApiResponse.error(HttpStatus.FORBIDDEN.value(), "无权限");
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ApiResponse handleRest(RestClientResponseException e) {
        // getRawStatusCode() 已过时 -> 使用 getStatusCode().value()
        return ApiResponse.error(e.getStatusCode().value(), e.getResponseBodyAsString());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse handleOther(Exception e) {
        return ApiResponse.error("服务异常：" + e.getMessage());
    }
}
