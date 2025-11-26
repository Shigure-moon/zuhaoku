package com.zhk.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 用户服务启动类
 *
 * @author shigure
 */
@SpringBootApplication(scanBasePackages = "com.zhk")
@MapperScan("com.zhk.**.mapper")
@EnableScheduling // 启用定时任务
@org.springframework.scheduling.annotation.EnableAsync // 启用异步支持
public class ZhkUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZhkUserApplication.class, args);
    }
}

