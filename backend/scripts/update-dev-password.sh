#!/bin/bash
# 更新测试账号密码脚本

set -e

echo "=========================================="
echo "更新测试账号密码"
echo "=========================================="

# 检查 MySQL 容器
if ! docker ps | grep -q zhk-mysql-dev; then
    echo "❌ MySQL 容器未运行"
    exit 1
fi

# 生成新的密码哈希（dev123456）
# BCrypt 哈希值：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
NEW_PASSWORD_HASH='$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'

echo "更新测试账号密码..."
docker exec -i zhk-mysql-dev mysql -uroot -proot123456 zhk_rental << EOF
UPDATE user SET password = '$NEW_PASSWORD_HASH' WHERE mobile IN ('13800000001', '13800000002', '13800000003');
SELECT id, nickname, mobile, LEFT(password, 30) as password_hash FROM user WHERE mobile IN ('13800000001', '13800000002', '13800000003');
EOF

echo ""
echo "✅ 密码更新完成！"
echo ""
echo "测试账号："
echo "  - 手机号: 13800000001"
echo "  - 密码: dev123456"

