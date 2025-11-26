package com.zhk.user.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 临时工具：生成密码哈希
 * 运行后会在控制台输出哈希值，然后可以更新数据库
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "dev123456";
        String hash = encoder.encode(password);
        
        System.out.println("==========================================");
        System.out.println("密码哈希生成工具");
        System.out.println("==========================================");
        System.out.println("密码: " + password);
        System.out.println("BCrypt 哈希: " + hash);
        System.out.println("");
        System.out.println("SQL 更新语句:");
        System.out.println("UPDATE user SET password = '" + hash + "' WHERE mobile IN ('13800000001', '13800000002', '13800000003');");
        System.out.println("==========================================");
    }
}

