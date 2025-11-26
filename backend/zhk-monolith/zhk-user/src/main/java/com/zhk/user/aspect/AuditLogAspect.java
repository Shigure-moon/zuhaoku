package com.zhk.user.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhk.common.security.SecurityUtils;
import com.zhk.user.entity.AuditLog;
import com.zhk.user.entity.User;
import com.zhk.user.mapper.UserMapper;
import com.zhk.user.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志审计切面
 * 自动记录关键操作的审计日志
 *
 * @author shigure
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogService auditLogService;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 定义切点：所有Controller的方法
     */
    @Pointcut("execution(* com.zhk..controller..*(..))")
    public void controllerMethods() {
    }

    /**
     * 环绕通知：记录审计日志
     */
    @Around("controllerMethods()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        HttpServletRequest request = getRequest();
        
        if (request == null) {
            return joinPoint.proceed();
        }

        String requestPath = request.getRequestURI();
        String requestMethod = request.getMethod();
        
        // 跳过审计日志查询接口本身，避免循环记录
        if (requestPath.contains("/audit-logs")) {
            return joinPoint.proceed();
        }

        // 只记录关键操作
        if (!shouldLog(requestPath, requestMethod)) {
            return joinPoint.proceed();
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setRequestMethod(requestMethod);
        auditLog.setRequestPath(requestPath);
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setCreatedAt(LocalDateTime.now());

        // 获取用户信息
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId != null) {
            auditLog.setUserId(userId);
            User user = userMapper.selectById(userId);
            if (user != null) {
                auditLog.setUsername(user.getNickname());
                auditLog.setRole(user.getRole());
            }
        }

        // 解析操作类型和资源类型
        parseActionAndResource(requestPath, requestMethod, auditLog);

        // 记录请求参数
        try {
            Object[] args = joinPoint.getArgs();
            Map<String, Object> params = new HashMap<>();
            for (int i = 0; i < args.length; i++) {
                Object arg = args[i];
                // 跳过HttpServletRequest等对象
                if (arg instanceof HttpServletRequest) {
                    continue;
                }
                // 只记录简单对象，避免记录敏感信息
                if (isSimpleType(arg)) {
                    params.put("arg" + i, arg);
                }
            }
            if (!params.isEmpty()) {
                auditLog.setRequestParams(objectMapper.writeValueAsString(params));
            }
        } catch (Exception e) {
            log.warn("记录请求参数失败", e);
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            auditLog.setSuccess(1);
            
            // 尝试获取响应状态码
            if (result instanceof com.zhk.common.web.Result) {
                com.zhk.common.web.Result<?> resultObj = (com.zhk.common.web.Result<?>) result;
                auditLog.setResponseStatus(resultObj.getCode());
                if (resultObj.getCode() != 200) {
                    auditLog.setSuccess(0);
                    auditLog.setErrorMessage(resultObj.getMessage());
                }
            }
        } catch (Exception e) {
            auditLog.setSuccess(0);
            auditLog.setErrorMessage(e.getMessage());
            auditLog.setResponseStatus(500);
            throw e;
        } finally {
            long endTime = System.currentTimeMillis();
            auditLog.setExecutionTime((int) (endTime - startTime));
            
            // 设置操作描述
            if (auditLog.getDescription() == null || auditLog.getDescription().isEmpty()) {
                auditLog.setDescription(buildDescription(auditLog));
            }
            
            // 异步记录日志（避免影响主流程性能）
            try {
                auditLogService.log(auditLog);
            } catch (Exception e) {
                log.error("记录审计日志失败", e);
            }
        }

        return result;
    }

    /**
     * 判断是否应该记录日志
     */
    private boolean shouldLog(String path, String method) {
        // 只记录POST、PUT、DELETE、PATCH等写操作
        if (!"POST".equals(method) && !"PUT".equals(method) 
            && !"DELETE".equals(method) && !"PATCH".equals(method)) {
            return false;
        }
        
        // 记录关键操作路径
        return path.contains("/login") 
            || path.contains("/logout")
            || path.contains("/register")
            || path.contains("/orders")
            || path.contains("/payments")
            || path.contains("/appeals")
            || path.contains("/admin")
            || path.contains("/accounts");
    }

    /**
     * 解析操作类型和资源类型
     */
    private void parseActionAndResource(String path, String method, AuditLog auditLog) {
        // 解析资源类型
        if (path.contains("/users")) {
            auditLog.setResourceType("USER");
        } else if (path.contains("/orders")) {
            auditLog.setResourceType("ORDER");
        } else if (path.contains("/accounts")) {
            auditLog.setResourceType("ACCOUNT");
        } else if (path.contains("/appeals")) {
            auditLog.setResourceType("APPEAL");
        } else if (path.contains("/payments")) {
            auditLog.setResourceType("PAYMENT");
        }

        // 解析操作类型
        if (path.contains("/login")) {
            auditLog.setAction("LOGIN");
        } else if (path.contains("/logout")) {
            auditLog.setAction("LOGOUT");
        } else if (path.contains("/register")) {
            auditLog.setAction("REGISTER");
        } else if (path.contains("/resolve")) {
            auditLog.setAction("APPEAL_RESOLVE");
        } else if (path.contains("/status") && path.contains("/users")) {
            if ("PUT".equals(method) || "PATCH".equals(method)) {
                auditLog.setAction("USER_STATUS_UPDATE");
            }
        } else if (path.contains("/freeze")) {
            auditLog.setAction("USER_FREEZE");
        } else if (path.contains("/unfreeze")) {
            auditLog.setAction("USER_UNFREEZE");
        } else if (path.contains("/accounts")) {
            if ("POST".equals(method)) {
                auditLog.setAction("ACCOUNT_CREATE");
            } else if ("PUT".equals(method) || "PATCH".equals(method)) {
                auditLog.setAction("ACCOUNT_UPDATE");
            } else if ("DELETE".equals(method)) {
                auditLog.setAction("ACCOUNT_DELETE");
            }
        } else if (path.contains("/orders")) {
            if ("POST".equals(method)) {
                auditLog.setAction("ORDER_CREATE");
            } else if ("PUT".equals(method) || "PATCH".equals(method)) {
                auditLog.setAction("ORDER_UPDATE");
            }
        } else if (path.contains("/payments")) {
            auditLog.setAction("PAYMENT");
        }
    }

    /**
     * 构建操作描述
     */
    private String buildDescription(AuditLog auditLog) {
        StringBuilder desc = new StringBuilder();
        if (auditLog.getUsername() != null) {
            desc.append(auditLog.getUsername());
        } else {
            desc.append("系统");
        }
        desc.append("执行了").append(auditLog.getAction());
        if (auditLog.getResourceType() != null) {
            desc.append("操作，资源类型：").append(auditLog.getResourceType());
        }
        if (auditLog.getResourceId() != null) {
            desc.append("，资源ID：").append(auditLog.getResourceId());
        }
        return desc.toString();
    }

    /**
     * 获取HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 判断是否为简单类型
     */
    private boolean isSimpleType(Object obj) {
        if (obj == null) {
            return false;
        }
        Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() 
            || clazz == String.class
            || clazz == Integer.class
            || clazz == Long.class
            || clazz == Double.class
            || clazz == Float.class
            || clazz == Boolean.class
            || Number.class.isAssignableFrom(clazz);
    }
}

