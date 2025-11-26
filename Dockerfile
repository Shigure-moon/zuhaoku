# 多阶段构建 Dockerfile
# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制 pom.xml 文件（利用 Docker 缓存层）
COPY backend/pom.xml backend/pom.xml
COPY backend/zhk-common/pom.xml backend/zhk-common/pom.xml
COPY backend/zhk-infrastructure/pom.xml backend/zhk-infrastructure/pom.xml
COPY backend/zhk-monolith/pom.xml backend/zhk-monolith/pom.xml

# 复制所有模块的 pom.xml（提前下载依赖）
COPY backend/zhk-common/zhk-common-core/pom.xml backend/zhk-common/zhk-common-core/pom.xml
COPY backend/zhk-common/zhk-common-security/pom.xml backend/zhk-common/zhk-common-security/pom.xml
COPY backend/zhk-common/zhk-common-web/pom.xml backend/zhk-common/zhk-common-web/pom.xml
COPY backend/zhk-infrastructure/zhk-database/pom.xml backend/zhk-infrastructure/zhk-database/pom.xml
COPY backend/zhk-infrastructure/zhk-redis/pom.xml backend/zhk-infrastructure/zhk-redis/pom.xml
COPY backend/zhk-infrastructure/zhk-minio/pom.xml backend/zhk-infrastructure/zhk-minio/pom.xml
COPY backend/zhk-monolith/zhk-user/pom.xml backend/zhk-monolith/zhk-user/pom.xml
COPY backend/zhk-monolith/zhk-order/pom.xml backend/zhk-monolith/zhk-order/pom.xml
COPY backend/zhk-monolith/zhk-risk/pom.xml backend/zhk-monolith/zhk-risk/pom.xml

# 下载依赖（利用缓存）
RUN mvn dependency:go-offline -f backend/pom.xml || true

# 复制源代码
COPY backend/ backend/

# 构建应用
RUN mvn clean package -DskipTests -f backend/pom.xml

# 阶段2: 运行阶段
FROM eclipse-temurin:17-jre-alpine

# 安装必要的工具
RUN apk add --no-cache \
    tzdata \
    curl \
    && cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone \
    && apk del tzdata

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

