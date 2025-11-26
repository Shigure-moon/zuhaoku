package com.zhk.user.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 临时测试控制器（仅用于开发环境）
 * 用于生成密码哈希
 */
@RestController
@RequestMapping("/api/v1/test")
public class TestController {

    @GetMapping("/generate-password-hash")
    public String generatePasswordHash(@RequestParam(defaultValue = "dev123456") String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);
        
        return String.format(
            "密码: %s\n" +
            "BCrypt 哈希: %s\n\n" +
            "SQL 更新语句:\n" +
            "UPDATE user SET password = '%s' WHERE mobile IN ('13800000001', '13800000002', '13800000003');",
            password, hash, hash
        );
    }
}

