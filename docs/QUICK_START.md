# 快速启动指南

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、启动顺序

### 1.1 启动 Docker 服务（数据库和中间件）

```bash
cd /home/shigure/zuhaoku
./backend/scripts/docker-setup.sh
```

或手动启动：

```bash
docker compose -f docker-compose.dev.yml up -d
```

**验证**：
```bash
docker ps | grep zhk-
```

应该看到：
- `zhk-mysql-dev` (端口 3307)
- `zhk-redis-dev` (端口 6380)
- `zhk-minio-dev` (端口 9002, 9003)

### 1.2 创建开发者账号（可选）

```bash
./backend/scripts/create-dev-account.sh
```

### 1.3 启动后端服务

```bash
# 方式一：使用脚本（推荐）
./backend/scripts/start-backend.sh

# 方式二：手动启动
cd backend
mvn clean install -DskipTests
cd zhk-monolith/zhk-user
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

**验证**：
- 后端服务应该运行在 `http://localhost:8080`
- 检查日志确认服务启动成功

### 1.4 启动前端服务

```bash
cd frontend
npm install  # 首次运行需要
npm run dev
```

**验证**：
- 前端服务应该运行在 `http://localhost:3000`
- 如果 3000 端口被占用，Vite 会自动使用下一个可用端口（如 3001）

---

## 二、常见问题

### 2.1 后端服务无法启动

**问题**：`connect ECONNREFUSED 127.0.0.1:8080`

**解决**：
1. 检查后端服务是否运行：`lsof -i :8080` 或 `ss -tuln | grep 8080`
2. 检查 Java 版本：`java -version`（需要 JDK 17+）
3. 检查 Maven 是否安装：`mvn -version`
4. 查看后端启动日志，查找错误信息

### 2.2 前端无法连接后端

**问题**：前端请求返回 500 或连接被拒绝

**解决**：
1. 确认后端服务已启动（端口 8080）
2. 检查前端 `vite.config.ts` 中的代理配置
3. 检查后端 CORS 配置
4. 查看浏览器控制台和网络请求详情

### 2.3 数据库连接失败

**问题**：后端启动时数据库连接错误

**解决**：
1. 确认 MySQL 容器运行：`docker ps | grep mysql`
2. 检查数据库配置：`backend/zhk-monolith/zhk-user/src/main/resources/application-dev.yml`
3. 确认端口映射：3307（外部）-> 3306（容器内）
4. 测试连接：`docker exec -it zhk-mysql-dev mysql -uroot -proot123456`

### 2.4 端口冲突

**问题**：端口已被占用

**解决**：
- 前端端口：修改 `frontend/vite.config.ts` 中的 `server.port`
- 后端端口：修改 `application-dev.yml` 中的 `server.port`
- 或停止占用端口的服务

---

## 三、服务访问地址

| 服务 | 地址 | 说明 |
|------|------|------|
| 前端应用 | http://localhost:3000 | Vue 3 前端 |
| 后端 API | http://localhost:8080 | Spring Boot 后端 |
| MySQL | localhost:3307 | 数据库 |
| Redis | localhost:6380 | 缓存 |
| MinIO Console | http://localhost:9003 | 对象存储管理 |
| phpMyAdmin | http://localhost:8084 | 数据库管理 |
| Redis Commander | http://localhost:8083 | Redis 管理 |

---

## 四、开发账号

| 角色 | 手机号 | 密码 |
|------|--------|------|
| 租客 | 13800000001 | dev123456 |
| 商家 | 13800000002 | dev123456 |
| 运营 | 13800000003 | dev123456 |

---

## 五、常用命令

```bash
# 查看 Docker 服务状态
docker compose -f docker-compose.dev.yml ps

# 查看后端日志
tail -f backend/zhk-monolith/zhk-user/logs/application.log

# 重启 Docker 服务
docker compose -f docker-compose.dev.yml restart

# 停止所有服务
docker compose -f docker-compose.dev.yml down
```

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

