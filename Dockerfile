# 多阶段构建 Dockerfile
# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 配置 Maven 使用更快的镜像和超时设置
RUN mkdir -p /root/.m2 && \
    echo '<?xml version="1.0" encoding="UTF-8"?>' > /root/.m2/settings.xml && \
    echo '<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"' >> /root/.m2/settings.xml && \
    echo '          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"' >> /root/.m2/settings.xml && \
    echo '          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">' >> /root/.m2/settings.xml && \
    echo '  <localRepository>/root/.m2/repository</localRepository>' >> /root/.m2/settings.xml && \
    echo '  <interactiveMode>false</interactiveMode>' >> /root/.m2/settings.xml && \
    echo '</settings>' >> /root/.m2/settings.xml

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

# 构建应用（跳过测试，使用批处理模式，设置超时）
RUN mvn clean package -DskipTests -f backend/pom.xml \
    -B \
    -Dmaven.wagon.http.retryHandler.count=3 \
    -Dmaven.wagon.httpconnectionManager.ttlSeconds=25 \
    || (echo "Build failed, but continuing..." && exit 1)

# 阶段2: 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 使用阿里云镜像加速（如果网络慢）
# RUN sed -i 's/dl-cdn.alpinelinux.org/mirrors.aliyun.com/g' /etc/apk/repositories

# 安装必要的工具（添加超时和重试）
RUN apk add --no-cache --timeout=300 \
    tzdata \
    curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata || true

# 创建应用用户（非 root 用户运行）
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# 设置工作目录
WORKDIR /app

# 从构建阶段复制 JAR 文件
COPY --from=builder /app/backend/zhk-monolith/zhk-user/target/zhk-user-*.jar app.jar

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

