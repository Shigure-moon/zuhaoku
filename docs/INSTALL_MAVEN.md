# Maven 安装指南

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、Ubuntu/Debian 系统安装

### 方式一：使用 apt 安装（推荐）

```bash
# 更新包列表
sudo apt update

# 安装 Maven
sudo apt install maven -y

# 验证安装
mvn -version
```

### 方式二：手动安装（最新版本）

```bash
# 1. 下载 Maven（选择最新版本）
cd /tmp
wget https://dlcdn.apache.org/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz

# 2. 解压
tar -xzf apache-maven-3.9.9-bin.tar.gz

# 3. 移动到 /opt 目录
sudo mv apache-maven-3.9.9 /opt/maven

# 4. 设置环境变量
echo 'export M2_HOME=/opt/maven' >> ~/.bashrc
echo 'export PATH=$M2_HOME/bin:$PATH' >> ~/.bashrc

# 5. 重新加载配置
source ~/.bashrc

# 6. 验证安装
mvn -version
```

---

## 二、配置 Maven 镜像（加速下载）

创建或编辑 `~/.m2/settings.xml`：

```bash
mkdir -p ~/.m2
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
```

---

## 三、验证安装

```bash
# 检查 Maven 版本
mvn -version

# 应该看到类似输出：
# Apache Maven 3.9.x
# Maven home: /usr/share/maven
# Java version: 17.x.x
```

---

## 四、常见问题

### 4.1 Java 版本问题

如果 Maven 提示 Java 版本过低：

```bash
# 检查 Java 版本
java -version

# 如果版本低于 17，需要安装 JDK 17+
sudo apt install openjdk-17-jdk -y

# 设置 JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

### 4.2 权限问题

如果遇到权限问题：

```bash
# 确保 ~/.m2 目录有写权限
chmod -R 755 ~/.m2
```

---

## 五、快速安装脚本

```bash
# 一键安装 Maven（Ubuntu/Debian）
sudo apt update && sudo apt install maven -y

# 配置镜像
mkdir -p ~/.m2
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

# 验证
mvn -version
```

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

