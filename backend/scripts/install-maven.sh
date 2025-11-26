#!/bin/bash
# Maven 安装脚本

set -e

echo "=========================================="
echo "安装 Maven"
echo "=========================================="

# 检查是否已安装
if command -v mvn &> /dev/null; then
    echo "✅ Maven 已安装"
    mvn -version
    exit 0
fi

# 检查系统类型
if [ -f /etc/debian_version ]; then
    echo "检测到 Debian/Ubuntu 系统"
    
    # 更新包列表
    echo "更新包列表..."
    sudo apt update
    
    # 安装 Maven
    echo "安装 Maven..."
    sudo apt install maven -y
    
    # 验证安装
    echo ""
    echo "验证安装..."
    mvn -version
    
    echo ""
    echo "✅ Maven 安装成功！"
    
elif [ -f /etc/redhat-release ]; then
    echo "检测到 RedHat/CentOS 系统"
    sudo yum install maven -y
    mvn -version
    
else
    echo "❌ 未识别的系统类型，请手动安装 Maven"
    echo "参考文档: docs/INSTALL_MAVEN.md"
    exit 1
fi

# 配置 Maven 镜像（加速下载）
echo ""
echo "配置 Maven 镜像..."
mkdir -p ~/.m2

if [ ! -f ~/.m2/settings.xml ]; then
    cat > ~/.m2/settings.xml << 'EOF'
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 
          http://maven.apache.org/xsd/settings-1.2.0.xsd">
  <mirrors>
    <mirror>
      <id>aliyun</id>
      <name>Aliyun Maven</name>
      <url>https://maven.aliyun.com/repository/public</url>
      <mirrorOf>central</mirrorOf>
    </mirror>
  </mirrors>
</settings>
EOF
    echo "✅ Maven 镜像配置完成"
else
    echo "⚠️  Maven 配置文件已存在，跳过配置"
fi

echo ""
echo "=========================================="
echo "Maven 安装完成！"
echo "=========================================="
echo ""
echo "现在可以运行: ./backend/scripts/start-backend.sh"

