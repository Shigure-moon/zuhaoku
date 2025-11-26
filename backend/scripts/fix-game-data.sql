-- 修复游戏数据字符集脚本
-- 使用 UTF-8 编码执行此脚本

USE zhk_rental;

-- 设置连接字符集
SET NAMES utf8mb4;
SET CHARACTER_SET_CLIENT = utf8mb4;
SET CHARACTER_SET_CONNECTION = utf8mb4;
SET CHARACTER_SET_RESULTS = utf8mb4;

-- 清空表（使用 DELETE 因为外键约束）
DELETE FROM game;

-- 重新插入正确的数据
INSERT INTO game (name, publisher, login_type, status) VALUES
('英雄联盟', 'Riot Games', 'pwd', 1),
('王者荣耀', '腾讯游戏', 'qr', 1),
('原神', 'miHoYo', 'pwd', 1);

-- 验证数据
SELECT id, name, publisher FROM game;

