#!/bin/bash
# Docker 环境设置脚本

set -e

echo "=========================================="
echo "租号酷 Docker 环境设置"
echo "=========================================="

# 检查 Docker 是否安装
if ! command -v docker &> /dev/null; then
    echo "❌ Docker 未安装，请先安装 Docker"
    exit 1
fi

# 检测 Docker Compose 命令（支持 V1 和 V2）
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
else
    echo "❌ Docker Compose 未安装，请先安装 Docker Compose"
    exit 1
fi

echo "使用 Docker Compose 命令: $DOCKER_COMPOSE_CMD"

# 获取项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

# 检查 .env 文件
if [ ! -f .env ]; then
    echo "📝 创建 .env 文件..."
    if [ -f .env.example ]; then
        cp .env.example .env
        echo "✅ .env 文件已创建（从 .env.example 复制），请根据需要修改配置"
    else
        # 如果 .env.example 不存在，直接创建 .env 文件（使用开发环境默认值）
        cat > .env << 'EOF'
# Docker Compose 环境变量配置
# 开发环境默认配置

# MySQL 配置
MYSQL_ROOT_PASSWORD=root123456
MYSQL_DATABASE=zhk_rental
MYSQL_USER=zhk_user
MYSQL_PASSWORD=zhk_password
MYSQL_PORT=3306

# Redis 配置
REDIS_PASSWORD=
REDIS_PORT=6379

# MinIO 配置
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin123
MINIO_API_PORT=9000
MINIO_CONSOLE_PORT=9001

# 管理工具端口（可选）
REDIS_COMMANDER_PORT=8081
PHPMYADMIN_PORT=8082
EOF
        echo "✅ .env 文件已创建（使用默认配置），请根据需要修改配置"
    fi
fi

# 启动服务
echo ""
echo "🚀 启动 Docker 服务..."
$DOCKER_COMPOSE_CMD -f docker-compose.dev.yml up -d

# 等待服务启动
echo ""
echo "⏳ 等待服务启动..."
sleep 10

# 检查服务状态
echo ""
echo "📊 服务状态："
$DOCKER_COMPOSE_CMD -f docker-compose.dev.yml ps

echo ""
echo "✅ Docker 环境设置完成！"
echo ""
echo "📌 服务访问地址："
echo "  - MySQL:        localhost:3307"
echo "  - Redis:        localhost:6380"
echo "  - MinIO API:    http://localhost:9002"
echo "  - MinIO Console: http://localhost:9003 (用户名: minioadmin, 密码: minioadmin123)"
echo "  - phpMyAdmin:   http://localhost:8084"
echo "  - Redis Commander: http://localhost:8083"
echo ""
echo "📝 数据库连接信息："
echo "  - 数据库名: zhk_rental"
echo "  - 用户名: root"
echo "  - 密码: root123456"
echo ""
echo "💡 常用命令："
echo "  - 查看日志: $DOCKER_COMPOSE_CMD -f docker-compose.dev.yml logs -f"
echo "  - 停止服务: $DOCKER_COMPOSE_CMD -f docker-compose.dev.yml down"
echo "  - 重启服务: $DOCKER_COMPOSE_CMD -f docker-compose.dev.yml restart"

