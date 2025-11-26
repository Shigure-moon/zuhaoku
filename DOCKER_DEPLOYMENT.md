# Docker 部署指南

## 本地 Docker 构建和运行

### 1. 构建 Docker 镜像

```bash
# 在项目根目录执行
docker build -t zhk-user:latest .
```

### 2. 运行 Docker 容器

```bash
# 使用环境变量运行
docker run -d \
  --name zhk-user \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e MYSQL_HOST=mysql2.sqlpub.com \
  -e MYSQL_PORT=3307 \
  -e MYSQL_DATABASE=zuhaoku \
  -e MYSQL_USER=shigure2 \
  -e MYSQL_PASSWORD=NVxtg0a9HYU5i62K \
  -e JWT_SECRET=993056993056993056993056993056993056 \
  -e ENCRYPTION_KEY=993056993056993056993056993056993056 \
  -e ENCRYPTION_MASTER_KEY=993056993056993056993056993056993056 \
  zhk-user:latest
```

### 3. 使用 Docker Compose（推荐）

创建 `docker-compose.prod.yml`：

```yaml
version: '3.8'

services:
  zhk-user:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: zhk-user
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      MYSQL_HOST: mysql2.sqlpub.com
      MYSQL_PORT: 3307
      MYSQL_DATABASE: zuhaoku
      MYSQL_USER: shigure2
      MYSQL_PASSWORD: NVxtg0a9HYU5i62K
      JWT_SECRET: 993056993056993056993056993056993056
      ENCRYPTION_KEY: 993056993056993056993056993056993056
      ENCRYPTION_MASTER_KEY: 993056993056993056993056993056993056
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
```

运行：

```bash
docker-compose -f docker-compose.prod.yml up -d
```

## Zeabur Docker 部署

### 1. 配置

Zeabur 会自动检测 `Dockerfile` 并使用它进行构建。

确保 `zeabur.yaml` 配置正确：

```yaml
build:
  type: dockerfile
  dockerfile: Dockerfile

port: 8080
```

### 2. 环境变量

在 Zeabur 控制台配置以下环境变量：

```bash
SPRING_PROFILES_ACTIVE=prod
MYSQL_HOST=mysql2.sqlpub.com
MYSQL_PORT=3307
MYSQL_DATABASE=zuhaoku
MYSQL_USER=shigure2
MYSQL_PASSWORD=NVxtg0a9HYU5i62K
JWT_SECRET=993056993056993056993056993056993056
ENCRYPTION_KEY=993056993056993056993056993056993056
ENCRYPTION_MASTER_KEY=993056993056993056993056993056993056
```

### 3. 部署

1. 推送代码到 GitHub
2. Zeabur 会自动检测 Dockerfile 并开始构建
3. 等待构建完成
4. 访问应用地址

## Docker 镜像优化

### 多阶段构建

Dockerfile 使用多阶段构建：
- **阶段1**: 使用 Maven 镜像构建应用
- **阶段2**: 使用轻量级 JRE 镜像运行应用

这样可以显著减小最终镜像大小。

### 镜像大小优化

- 使用 Alpine Linux 基础镜像
- 多阶段构建，只复制必要的文件
- 使用非 root 用户运行应用
- 清理构建缓存

## 健康检查

Dockerfile 包含健康检查配置：

```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1
```

## 故障排查

### 查看容器日志

```bash
docker logs zhk-user
docker logs -f zhk-user  # 实时查看
```

### 进入容器调试

```bash
docker exec -it zhk-user sh
```

### 检查容器状态

```bash
docker ps -a
docker inspect zhk-user
```

### 常见问题

1. **端口冲突**
   - 确保 8080 端口未被占用
   - 或修改端口映射：`-p 8081:8080`

2. **数据库连接失败**
   - 检查环境变量是否正确
   - 确认数据库允许外部连接

3. **内存不足**
   - 增加 Docker 内存限制
   - 或优化 JVM 参数

## 生产环境建议

1. **使用环境变量文件**
   ```bash
   docker run --env-file .env.prod zhk-user:latest
   ```

2. **配置资源限制**
   ```yaml
   deploy:
     resources:
       limits:
         cpus: '1'
         memory: 1G
       reservations:
         cpus: '0.5'
         memory: 512M
   ```

3. **配置日志驱动**
   ```yaml
   logging:
     driver: "json-file"
     options:
       max-size: "10m"
       max-file: "3"
   ```

4. **使用 Docker Secrets**（敏感信息）
   - 不要在环境变量中硬编码密码
   - 使用 Docker Secrets 或密钥管理服务

