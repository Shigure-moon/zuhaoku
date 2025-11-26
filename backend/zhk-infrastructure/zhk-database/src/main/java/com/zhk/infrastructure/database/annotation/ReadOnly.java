package com.zhk.infrastructure.database.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 只读数据源注解
 * 标记使用从库（读库）进行查询
 *
 * @author shigure
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ReadOnly {
    /**
     * 是否强制使用从库
     * 如果为true，即使从库不可用也不会回退到主库
     *
     * @return 是否强制
     */
    boolean force() default false;
}


