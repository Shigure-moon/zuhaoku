-- 开发者测试账号脚本
-- 版本: v1.0
-- 最后更新: 2025/11/18
-- 说明: 用于开发环境测试的账号，密码统一为: dev123456

-- 设置客户端字符集为 utf8mb4（必须在执行 SQL 之前设置）
SET NAMES utf8mb4;

USE zhk_rental;

-- 确保会话使用正确的字符集
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 删除已存在的测试账号（如果存在）
DELETE FROM user WHERE mobile IN ('13800000001', '13800000002', '13800000003');

-- 插入租客账号（密码: dev123456）
-- BCrypt 哈希值: $2a$10$zytEwirytMyTq2bx09aiYuR/vakZ2.2wbnhgku.FUpTLxU1pi0Ud.
INSERT INTO user (nickname, mobile, password, role, status, zhima_score) VALUES
('测试租客', '13800000001', '$2a$10$zytEwirytMyTq2bx09aiYuR/vakZ2.2wbnhgku.FUpTLxU1pi0Ud.', 'TENANT', 1, 650)
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);

-- 插入商家账号（密码: dev123456）
INSERT INTO user (nickname, mobile, password, role, status, zhima_score) VALUES
('测试商家', '13800000002', '$2a$10$zytEwirytMyTq2bx09aiYuR/vakZ2.2wbnhgku.FUpTLxU1pi0Ud.', 'OWNER', 1, 700)
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);

-- 插入运营账号（密码: dev123456）
INSERT INTO user (nickname, mobile, password, role, status, zhima_score) VALUES
('测试运营', '13800000003', '$2a$10$zytEwirytMyTq2bx09aiYuR/vakZ2.2wbnhgku.FUpTLxU1pi0Ud.', 'OPERATOR', 1, NULL)
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);

-- 查询插入的账号
SELECT id, nickname, mobile, role, status, zhima_score, created_at 
FROM user 
WHERE mobile IN ('13800000001', '13800000002', '13800000003')
ORDER BY role;

