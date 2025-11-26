#!/bin/bash
# 启动后端服务脚本

set -e

echo "=========================================="
echo "启动租号酷后端服务"
echo "=========================================="

# 获取项目根目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
BACKEND_DIR="$SCRIPT_DIR/.."

cd "$BACKEND_DIR"

# 检查 Docker 服务是否运行
echo "检查 Docker 服务状态..."
NEED_START_SERVICES=""

# 检查 MySQL
if docker ps | grep -q zhk-mysql-dev; then
    echo "✅ MySQL 容器已运行"
else
    echo "⚠️  MySQL 容器未运行"
    NEED_START_SERVICES="$NEED_START_SERVICES mysql"
fi

# 检查 Redis
if docker ps | grep -q zhk-redis-dev; then
    echo "✅ Redis 容器已运行"
else
    echo "⚠️  Redis 容器未运行"
    NEED_START_SERVICES="$NEED_START_SERVICES redis"
fi

# 检查 MinIO
if docker ps | grep -q zhk-minio-dev; then
    echo "✅ MinIO 容器已运行"
else
    echo "⚠️  MinIO 容器未运行"
    NEED_START_SERVICES="$NEED_START_SERVICES minio"
fi

# 启动未运行的服务
if [ -n "$NEED_START_SERVICES" ]; then
    echo ""
    echo "正在启动 Docker 服务: $NEED_START_SERVICES..."
    cd "$PROJECT_ROOT"
    docker compose -f docker-compose.dev.yml up -d $NEED_START_SERVICES
    echo "⏳ 等待服务启动（5秒）..."
    sleep 5
    
    # 验证服务是否成功启动
    echo ""
    echo "验证服务状态..."
    for service in $NEED_START_SERVICES; do
        case $service in
            mysql)
                if docker ps | grep -q zhk-mysql-dev; then
                    echo "✅ MySQL 启动成功"
                else
                    echo "❌ MySQL 启动失败"
                fi
                ;;
            redis)
                if docker ps | grep -q zhk-redis-dev; then
                    echo "✅ Redis 启动成功"
                else
                    echo "❌ Redis 启动失败"
                fi
                ;;
            minio)
                if docker ps | grep -q zhk-minio-dev; then
                    echo "✅ MinIO 启动成功 (API: http://localhost:9002, Console: http://localhost:9003)"
                else
                    echo "❌ MinIO 启动失败"
                fi
                ;;
        esac
    done
    
    cd "$BACKEND_DIR"
    echo ""
else
    echo ""
fi

# 检查 Java 环境
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 JDK 17+"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 版本过低，需要 JDK 17+，当前版本: $JAVA_VERSION"
    exit 1
fi

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven 3.8+"
    exit 1
fi

echo "✅ 环境检查通过"
echo ""

# 编译项目
echo "📦 编译项目..."
mvn clean install -DskipTests

echo ""
echo "🚀 启动用户服务..."
echo ""

# 启动用户服务
cd zhk-monolith/zhk-user
mvn spring-boot:run -Dspring-boot.run.profiles=dev -Dspring-boot.run.arguments=--server.port=8081

