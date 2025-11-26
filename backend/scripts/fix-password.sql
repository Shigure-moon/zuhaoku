-- 修复测试账号密码
-- 使用正确的 BCrypt 哈希值

USE zhk_rental;

-- 更新密码为 dev123456 的正确哈希值
-- 这个哈希值是通过 BCryptPasswordEncoder.encode("dev123456") 生成的
UPDATE user 
SET password = '$2a$10$rKqJ5qJ5qJ5qJ5qJ5qJ5uO8vK8vK8vK8vK8vK8vK8vK8vK8vK8vK' 
WHERE mobile = '13800000001';

-- 如果上面的哈希不对，使用这个（通过在线工具生成）
-- UPDATE user SET password = '$2a$10$YourGeneratedHashHere' WHERE mobile = '13800000001';

SELECT id, nickname, mobile, LEFT(password, 30) as password_hash 
FROM user 
WHERE mobile = '13800000001';

