# 多阶段构建 Dockerfile
# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-17 AS builder

# 设置工作目录
WORKDIR /app

# 复制所有 pom.xml 文件（利用 Docker 缓存层优化构建）
# 先复制父 pom.xml
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

# 下载依赖（利用缓存，如果失败继续构建）
RUN mvn dependency:go-offline -f backend/pom.xml || true

# 复制所有源代码
COPY backend/ backend/

# 构建应用（跳过测试以加快构建速度）
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

