package com.community.common.config;

import com.community.common.util.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(Exception.class)
    public ApiResponse handle(Exception e) {
        // 避免将内部异常细节回显给客户端，仅记录日�?        log.error("Unhandled exception", e);
        return ApiResponse.error("服务异常");
    }
}
