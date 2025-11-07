package com.community.supplier.support;

import com.community.common.util.ApiResponse;
import com.community.supplier.config.RestClientConfig.DownstreamServiceException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 供应商服务侧的全局异常处理器
 * 关键：通过 @Component("supplierGlobalExceptionHandler") 显式指定 Bean 名，避免与 common 模块的同名 Handler 冲突。
 */
@RestControllerAdvice
@Component("supplierGlobalExceptionHandler")
public class GlobalExceptionHandler {

    @ExceptionHandler(DownstreamServiceException.class)
    public ApiResponse handleDownstream(DownstreamServiceException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatus().value());
        if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
        // 你的 common-service 里应提供 ApiResponse.fail(int,String)
        return ApiResponse.fail(status.value(), ex.getMessage());
    }
}
