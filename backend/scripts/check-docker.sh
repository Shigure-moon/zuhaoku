#!/bin/bash
# 检查 Docker 服务状态脚本

set -e

echo "=========================================="
echo "检查 Docker 服务状态"
echo "=========================================="

# 获取项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

# 检查 Docker 是否运行
if ! docker info &> /dev/null; then
    echo "❌ Docker 未运行，请先启动 Docker"
    exit 1
fi

echo "✅ Docker 正在运行"
echo ""

# 检测 Docker Compose 命令（支持 V1 和 V2）
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
else
    echo "❌ Docker Compose 未安装"
    exit 1
fi

# 检查服务状态
echo "📊 服务状态："
$DOCKER_COMPOSE_CMD -f docker-compose.dev.yml ps

echo ""
echo "🔍 详细检查："

# 检查 MySQL
if docker ps | grep -q zhk-mysql-dev; then
    echo "✅ MySQL 容器运行中"
    if docker exec zhk-mysql-dev mysqladmin ping -h localhost -uroot -proot123456 &> /dev/null; then
        echo "   ✅ MySQL 服务正常"
    else
        echo "   ❌ MySQL 服务异常"
    fi
else
    echo "❌ MySQL 容器未运行"
fi

# 检查 Redis
if docker ps | grep -q zhk-redis-dev; then
    echo "✅ Redis 容器运行中"
    if docker exec zhk-redis-dev redis-cli ping | grep -q PONG; then
        echo "   ✅ Redis 服务正常"
    else
        echo "   ❌ Redis 服务异常"
    fi
else
    echo "❌ Redis 容器未运行"
fi

# 检查 MinIO
if docker ps | grep -q zhk-minio-dev; then
    echo "✅ MinIO 容器运行中"
    if curl -s http://localhost:9000/minio/health/live &> /dev/null; then
        echo "   ✅ MinIO 服务正常"
    else
        echo "   ❌ MinIO 服务异常"
    fi
else
    echo "❌ MinIO 容器未运行"
fi

echo ""
echo "📌 连接信息："
echo "  MySQL:   localhost:3307 (root/root123456)"
echo "  Redis:   localhost:6380"
echo "  MinIO:   http://localhost:9002 (minioadmin/minioadmin123)"
echo "  Console: http://localhost:9003"

