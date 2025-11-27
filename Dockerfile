# 多阶段构建 Dockerfile
# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 配置 Maven 超时和重试设置
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <localRepository>/root/.m2/repository</localRepository>
  <interactiveMode>false</interactiveMode>
</settings>
EOF

# 复制所有 pom.xml 文件（利用 Docker 缓存层优化构建）
COPY backend/pom.xml backend/
COPY backend/zhk-common/pom.xml backend/zhk-common/
COPY backend/zhk-infrastructure/pom.xml backend/zhk-infrastructure/
COPY backend/zhk-monolith/pom.xml backend/zhk-monolith/

# 复制子模块的 pom.xml（利用缓存优化）
COPY backend/zhk-common/zhk-common-core/pom.xml backend/zhk-common/zhk-common-core/
COPY backend/zhk-common/zhk-common-security/pom.xml backend/zhk-common/zhk-common-security/
COPY backend/zhk-common/zhk-common-web/pom.xml backend/zhk-common/zhk-common-web/
COPY backend/zhk-infrastructure/zhk-database/pom.xml backend/zhk-infrastructure/zhk-database/
COPY backend/zhk-infrastructure/zhk-redis/pom.xml backend/zhk-infrastructure/zhk-redis/
COPY backend/zhk-infrastructure/zhk-minio/pom.xml backend/zhk-infrastructure/zhk-minio/
COPY backend/zhk-monolith/zhk-user/pom.xml backend/zhk-monolith/zhk-user/
COPY backend/zhk-monolith/zhk-order/pom.xml backend/zhk-monolith/zhk-order/
COPY backend/zhk-monolith/zhk-risk/pom.xml backend/zhk-monolith/zhk-risk/

# 复制所有源代码
COPY backend/ backend/

# 直接构建应用（跳过依赖预下载步骤，Maven 会在构建时自动下载）
# 使用批处理模式，设置超时和重试
# 确保构建可执行的 Spring Boot JAR
RUN mvn clean package -DskipTests -f backend/pom.xml \
    -B \
    -Dmaven.wagon.http.retryHandler.count=5 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=120 \
    -Dmaven.wagon.http.readTimeout=300000 \
    -pl zhk-monolith/zhk-user \
    -am

# 验证 JAR 文件是否正确构建
RUN ls -lh /app/backend/zhk-monolith/zhk-user/target/*.jar && \
    jar tf /app/backend/zhk-monolith/zhk-user/target/zhk-user-1.0.0-SNAPSHOT.jar | grep -E "(META-INF/MANIFEST.MF|com/zhk/user/ZhkUserApplication)" || \
    (echo "JAR 文件验证失败" && exit 1)

# 阶段2: 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 安装必要的工具（简化安装，减少步骤）
RUN apk add --no-cache --update \
    tzdata \
    curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && rm -rf /var/cache/apk/* \
    && apk del tzdata 2>/dev/null || true

# 创建应用用户（非 root 用户运行）
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 JAR 文件（使用精确路径）
COPY --from=builder --chown=spring:spring /app/backend/zhk-monolith/zhk-user/target/zhk-user-1.0.0-SNAPSHOT.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# 启动应用
ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod}", \
    "-jar", \
    "app.jar"]

