#!/bin/bash
# 本地开发环境启动脚本

set -e

echo "=========================================="
echo "启动租号酷本地开发环境"
echo "=========================================="
echo ""

# 检查 Java 版本
if ! command -v java &> /dev/null; then
    echo "❌ Java 未安装，请先安装 JDK 17"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | sed '/^1\./s///' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo "❌ Java 版本过低，需要 JDK 17 或更高版本"
    exit 1
fi

echo "✅ Java 版本: $(java -version 2>&1 | head -n 1)"
echo ""

# 检查 Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven 未安装，请先安装 Maven"
    exit 1
fi

echo "✅ Maven 版本: $(mvn -version | head -n 1)"
echo ""

# 进入项目目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "📁 项目目录: $PROJECT_ROOT"
echo ""

# 检查数据库连接
echo "🔍 检查数据库连接..."
if [ -z "$MYSQL_HOST" ]; then
    MYSQL_HOST="localhost"
    MYSQL_PORT="3307"
    echo "   使用默认配置: $MYSQL_HOST:$MYSQL_PORT"
else
    echo "   使用环境变量: $MYSQL_HOST:$MYSQL_PORT"
fi
echo ""

# 编译项目
echo "🔨 编译项目..."
cd backend
mvn clean compile -DskipTests
echo ""

# 启动应用
echo "🚀 启动应用..."
echo "   应用将在 http://localhost:8081 启动"
echo "   按 Ctrl+C 停止应用"
echo ""

cd zhk-monolith/zhk-user
mvn spring-boot:run

