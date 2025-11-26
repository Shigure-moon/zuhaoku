package com.zhk.infrastructure.database.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据库配置类
 * 支持多数据源和读写分离
 *
 * @author shigure
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    /**
     * 主数据源（写库）
     * 配置属性前缀：spring.datasource.master
     */
    @Bean("masterDataSource")
    @ConfigurationProperties("spring.datasource.master")
    public DataSource masterDataSource() {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        // 设置Druid连接池默认参数
        configureDruidDataSource(dataSource);
        log.info("主数据源（写库）初始化完成");
        return dataSource;
    }

    /**
     * 从数据源（读库）- 可选
     * 配置属性前缀：spring.datasource.slave
     */
    @Bean("slaveDataSource")
    @ConditionalOnProperty(name = "spring.datasource.slave.enabled", havingValue = "true")
    @ConfigurationProperties("spring.datasource.slave")
    public DataSource slaveDataSource() {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        // 设置Druid连接池默认参数
        configureDruidDataSource(dataSource);
        log.info("从数据源（读库）初始化完成");
        return dataSource;
    }

    /**
     * 动态数据源（支持读写分离）
     * 如果启用了从库，则使用动态数据源；否则直接使用主数据源
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.datasource.slave.enabled", havingValue = "true")
    public DataSource routingDataSource(DataSource masterDataSource, DataSource slaveDataSource) {
        DynamicDataSource routingDataSource = new DynamicDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("master", masterDataSource);
        targetDataSources.put("slave", slaveDataSource);
        
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(masterDataSource);
        
        log.info("动态数据源（读写分离）初始化完成");
        return routingDataSource;
    }

    /**
     * 单数据源配置（未启用读写分离时使用）
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.datasource.slave.enabled", havingValue = "false", matchIfMissing = true)
    public DataSource dataSource(DataSource masterDataSource) {
        log.info("使用单数据源配置（主库）");
        return masterDataSource;
    }

    /**
     * 动态数据源路由
     */
    public static class DynamicDataSource extends AbstractRoutingDataSource {
        @Override
        protected Object determineCurrentLookupKey() {
            // 从ThreadLocal中获取数据源标识
            String dataSourceKey = DataSourceContextHolder.getDataSource();
            log.debug("当前数据源: {}", dataSourceKey != null ? dataSourceKey : "master");
            return dataSourceKey != null ? dataSourceKey : "master";
        }
    }

    /**
     * 数据源上下文持有者（ThreadLocal）
     */
    public static class DataSourceContextHolder {
        private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

        /**
         * 设置数据源
         *
         * @param dataSource 数据源标识（master/slave）
         */
        public static void setDataSource(String dataSource) {
            contextHolder.set(dataSource);
        }

        /**
         * 获取数据源
         *
         * @return 数据源标识
         */
        public static String getDataSource() {
            return contextHolder.get();
        }

        /**
         * 清除数据源
         */
        public static void clearDataSource() {
            contextHolder.remove();
        }
    }

    /**
     * 配置Druid数据源的默认参数
     */
    private void configureDruidDataSource(DruidDataSource dataSource) {
        // 连接池配置（如果配置文件中没有指定，使用默认值）
        if (dataSource.getInitialSize() == 0) {
            dataSource.setInitialSize(5);           // 初始连接数
        }
        if (dataSource.getMinIdle() == 0) {
            dataSource.setMinIdle(5);               // 最小空闲连接数
        }
        if (dataSource.getMaxActive() == 8) {
            dataSource.setMaxActive(20);            // 最大活跃连接数
        }
        if (dataSource.getMaxWait() == -1) {
            dataSource.setMaxWait(60000);           // 获取连接等待超时时间（毫秒）
        }
        
        // 连接有效性检测
        dataSource.setTestOnBorrow(false);          // 获取连接时检测
        dataSource.setTestOnReturn(false);          // 归还连接时检测
        dataSource.setTestWhileIdle(true);          // 空闲时检测
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setValidationQueryTimeout(3);
        if (dataSource.getTimeBetweenEvictionRunsMillis() == -1) {
            dataSource.setTimeBetweenEvictionRunsMillis(60000);  // 空闲连接回收时间间隔
        }
        if (dataSource.getMinEvictableIdleTimeMillis() == 0) {
            dataSource.setMinEvictableIdleTimeMillis(300000);    // 连接在池中最小生存时间
        }
        
        // 连接泄漏检测
        dataSource.setRemoveAbandoned(true);        // 是否自动回收超时连接
        dataSource.setRemoveAbandonedTimeout(1800); // 超时时间（秒）
        dataSource.setLogAbandoned(true);           // 是否在自动回收超时连接的时候打印连接的超时错误
        
        // 监控统计
        try {
            // 设置Druid过滤器（如果配置文件中没有指定）
            // 注意：如果配置文件中已通过 spring.datasource.master.druid.filters 配置，则不会覆盖
            dataSource.setFilters("stat,wall,slf4j");
        } catch (SQLException e) {
            log.error("Druid过滤器配置失败", e);
        }
    }
}

