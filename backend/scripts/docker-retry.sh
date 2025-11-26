#!/bin/bash
# Docker 镜像拉取重试脚本

set -e

echo "=========================================="
echo "Docker 镜像拉取重试工具"
echo "=========================================="

# 获取项目根目录
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT_ROOT"

# 检测 Docker Compose 命令
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE_CMD="docker compose"
else
    echo "❌ Docker Compose 未安装"
    exit 1
fi

# 镜像列表
IMAGES=(
    "mysql:8.0.44-debian"
    "redis:7.2-alpine"
    "minio/minio:latest"
    "phpmyadmin/phpmyadmin:latest"
    "rediscommander/redis-commander:latest"
)

echo ""
echo "📥 开始拉取镜像..."
echo ""

# 拉取镜像（带重试）
for image in "${IMAGES[@]}"; do
    echo "正在拉取: $image"
    retry_count=0
    max_retries=3
    
    while [ $retry_count -lt $max_retries ]; do
        if docker pull "$image"; then
            echo "✅ $image 拉取成功"
            break
        else
            retry_count=$((retry_count + 1))
            if [ $retry_count -lt $max_retries ]; then
                echo "⚠️  拉取失败，5秒后重试 ($retry_count/$max_retries)..."
                sleep 5
            else
                echo "❌ $image 拉取失败，已重试 $max_retries 次"
                echo "💡 建议："
                echo "   1. 检查网络连接"
                echo "   2. 配置 Docker 镜像加速器（见 docs/DOCKER_TROUBLESHOOTING.md）"
                echo "   3. 使用代理服务器"
            fi
        fi
    done
    echo ""
done

echo "=========================================="
echo "镜像拉取完成"
echo "=========================================="
echo ""
echo "现在可以运行: ./backend/scripts/docker-setup.sh"

