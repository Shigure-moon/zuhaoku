package com.zhk.infrastructure.database.aspect;

import com.zhk.infrastructure.database.annotation.ReadOnly;
import com.zhk.infrastructure.database.config.DatabaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 只读数据源切面
 * 自动将标记了@ReadOnly的方法路由到从库
 *
 * @author shigure
 */
@Slf4j
@Aspect
@Component
@Order(1) // 确保在事务切面之前执行
public class ReadOnlyAspect {

    /**
     * 切点：所有标记了@ReadOnly的方法
     */
    @Pointcut("@annotation(com.zhk.infrastructure.database.annotation.ReadOnly)")
    public void readOnlyPointcut() {
    }

    /**
     * 切点：所有标记了@ReadOnly的类中的方法
     */
    @Pointcut("@within(com.zhk.infrastructure.database.annotation.ReadOnly)")
    public void readOnlyClassPointcut() {
    }

    /**
     * 环绕通知：切换数据源
     */
    @Around("readOnlyPointcut() || readOnlyClassPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        ReadOnly readOnly = getReadOnlyAnnotation(joinPoint);
        boolean force = readOnly != null && readOnly.force();
        
        String oldDataSource = DatabaseConfig.DataSourceContextHolder.getDataSource();
        try {
            // 切换到从库
            DatabaseConfig.DataSourceContextHolder.setDataSource("slave");
            log.debug("切换到从库执行: {}", joinPoint.getSignature().toShortString());
            
            return joinPoint.proceed();
        } catch (Exception e) {
            // 如果强制使用从库，直接抛出异常
            if (force) {
                throw e;
            }
            // 否则回退到主库重试
            log.warn("从库执行失败，回退到主库: {}", e.getMessage());
            DatabaseConfig.DataSourceContextHolder.setDataSource("master");
            return joinPoint.proceed();
        } finally {
            // 恢复原来的数据源
            if (oldDataSource != null) {
                DatabaseConfig.DataSourceContextHolder.setDataSource(oldDataSource);
            } else {
                DatabaseConfig.DataSourceContextHolder.clearDataSource();
            }
        }
    }

    /**
     * 获取@ReadOnly注解
     */
    private ReadOnly getReadOnlyAnnotation(ProceedingJoinPoint joinPoint) {
        // 先尝试从方法上获取
        try {
            ReadOnly readOnly = joinPoint.getTarget().getClass()
                    .getMethod(joinPoint.getSignature().getName(),
                            ((org.aspectj.lang.reflect.MethodSignature) joinPoint.getSignature()).getParameterTypes())
                    .getAnnotation(ReadOnly.class);
            
            if (readOnly != null) {
                return readOnly;
            }
        } catch (NoSuchMethodException e) {
            // 方法不存在，继续尝试从类上获取
        }
        
        // 如果方法上没有，尝试从类上获取
        return joinPoint.getTarget().getClass().getAnnotation(ReadOnly.class);
    }
}


