package com.community.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class RequiresPermissionAspect {

    private final RestClient userClient = RestClient.create("http://user-service");

    @Around("@annotation(req)")
    public Object check(ProceedingJoinPoint pjp, RequiresPermissions req) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return pjp.proceed();
        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return pjp.proceed();
        }
        String[] need = req.value();
        boolean any = req.any();
        if (need.length == 0) return pjp.proceed();
        try {
            if (any) {
                for (String code : need) {
                    Boolean ok = userClient.get().uri("/internal/rbac/has-perm?userId={u}&perm={p}", userId, code).retrieve().body(Boolean.class);
                    if (Boolean.TRUE.equals(ok)) return pjp.proceed();
                }
                throw new org.springframework.security.access.AccessDeniedException("权限不足");
            } else {
                for (String code : need) {
                    Boolean ok = userClient.get().uri("/internal/rbac/has-perm?userId={u}&perm={p}", userId, code).retrieve().body(Boolean.class);
                    if (!Boolean.TRUE.equals(ok))
                        throw new org.springframework.security.access.AccessDeniedException("权限不足");
                }
                return pjp.proceed();
            }
        } catch (Exception e) {
            throw new org.springframework.security.access.AccessDeniedException("权限校验失败", e);
        }
    }
}
