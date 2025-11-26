package com.zhk.user.controller;

import com.zhk.common.web.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * 
 * @author shigure
 */
@RestController
public class HealthController {

    /**
     * 根路径健康检查
     */
    @GetMapping("/")
    public Result<Map<String, Object>> root() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "zhk-user");
        health.put("message", "租号酷后端服务运行正常");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0-SNAPSHOT");
        return Result.success(health);
    }

    /**
     * 健康检查端点
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "zhk-user");
        health.put("timestamp", System.currentTimeMillis());
        return Result.success(health);
    }
}

