package com.zhk.infrastructure.database.config;

// Note: Druid monitoring servlets are temporarily disabled due to Spring Boot 3 / Jakarta EE compatibility
// import com.alibaba.druid.support.http.StatViewServlet;
// import com.alibaba.druid.support.http.WebStatFilter;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.boot.web.servlet.FilterRegistrationBean;
// import org.springframework.boot.web.servlet.ServletRegistrationBean;
// import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Druid连接池配置类
 * 提供连接池监控和管理功能
 *
 * @author shigure
 */
@Slf4j
@Configuration
public class DruidConfig {

    /**
     * Druid连接池配置说明
     * 注意：实际的数据源Bean在DatabaseConfig中通过DruidDataSourceBuilder创建
     * 连接池参数通过application.yml中的spring.datasource.master.druid.*或spring.datasource.druid.*配置
     * 
     * 默认连接池参数：
     * - initialSize: 5 (初始连接数)
     * - minIdle: 5 (最小空闲连接数)
     * - maxActive: 20 (最大活跃连接数)
     * - maxWait: 60000 (获取连接等待超时时间，毫秒)
     * - testWhileIdle: true (空闲时检测连接有效性)
     * - validationQuery: SELECT 1
     * - removeAbandoned: true (自动回收超时连接)
     * - removeAbandonedTimeout: 1800 (超时时间，秒)
     */

    /**
     * Druid监控页面配置
     * 访问地址：/druid/index.html
     * 
     * 注意：由于 Spring Boot 3 使用 Jakarta EE (jakarta.servlet)，
     * 而 Druid 1.2.20 的 StatViewServlet 和 WebStatFilter 基于 javax.servlet，
     * 存在兼容性问题，监控功能已暂时禁用。
     * 
     * 如需启用监控，可以：
     * 1. 等待 Druid 发布支持 Jakarta EE 的版本
     * 2. 使用其他监控方案（如 Actuator + Micrometer）
     * 3. 通过配置文件中的 druid.stat-view-servlet.enabled=false 禁用（默认已禁用）
     */
    /*
    @Bean
    @ConditionalOnClass(StatViewServlet.class)
    @ConditionalOnProperty(name = "spring.datasource.druid.stat-view-servlet.enabled", havingValue = "true", matchIfMissing = false)
    public ServletRegistrationBean<StatViewServlet> druidStatViewServlet() {
        ServletRegistrationBean<StatViewServlet> registrationBean = 
                new ServletRegistrationBean<>(new StatViewServlet(), "/druid/*");
        
        // 监控页面登录用户名和密码
        registrationBean.addInitParameter("loginUsername", "admin");
        registrationBean.addInitParameter("loginPassword", "admin123");
        
        // 是否允许重置数据
        registrationBean.addInitParameter("resetEnable", "false");
        
        // IP白名单（没有配置或者为空，则允许所有访问）
        registrationBean.addInitParameter("allow", "");
        
        // IP黑名单（存在共同时，deny优先于allow）
        registrationBean.addInitParameter("deny", "");
        
        log.info("Druid监控页面配置完成: /druid/*");
        return registrationBean;
    }

    @Bean
    @ConditionalOnClass(WebStatFilter.class)
    @ConditionalOnProperty(name = "spring.datasource.druid.web-stat-filter.enabled", havingValue = "true", matchIfMissing = false)
    public FilterRegistrationBean<WebStatFilter> druidWebStatFilter() {
        FilterRegistrationBean<WebStatFilter> filterRegistrationBean = 
                new FilterRegistrationBean<>(new WebStatFilter());
        
        // 添加过滤规则
        filterRegistrationBean.addUrlPatterns("/*");
        
        // 添加不需要忽略的格式信息
        filterRegistrationBean.addInitParameter("exclusions", 
                "*.js,*.gif,*.jpg,*.png,*.css,*.ico,/druid/*");
        
        log.info("Druid Web监控过滤器配置完成");
        return filterRegistrationBean;
    }
    */
}

