-- 修复数据库字符集脚本

USE zhk_rental;

-- 修改数据库字符集
ALTER DATABASE zhk_rental CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修改表的字符集
ALTER TABLE user CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE game CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
ALTER TABLE account CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 重新插入游戏数据（如果存在乱码）
DELETE FROM game WHERE id IN (1, 2, 3);
INSERT INTO game (id, name, publisher, login_type, status) VALUES
(1, '英雄联盟', 'Riot Games', 'pwd', 1),
(2, '王者荣耀', '腾讯游戏', 'qr', 1),
(3, '原神', 'miHoYo', 'pwd', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name), publisher=VALUES(publisher);

-- 检查字符集
SHOW CREATE DATABASE zhk_rental;
SELECT id, name, publisher FROM game;
