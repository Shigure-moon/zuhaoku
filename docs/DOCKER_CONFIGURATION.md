# Docker 配置文档

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、Docker Compose 配置

### 1.1 完整配置

详见 `../docker-compose.yml`

### 1.2 环境变量

创建 `.env` 文件：

```env
# 数据库配置
DB_PASSWORD=your_password
DB_ROOT_PASSWORD=root_password

# Redis 配置
REDIS_PASSWORD=

# MinIO 配置
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=minioadmin

# 应用配置
SPRING_PROFILES_ACTIVE=dev
```

---

## 二、Dockerfile 配置

### 2.1 后端 Dockerfile

详见 `../backend/Dockerfile`

### 2.2 前端 Dockerfile

详见 `../frontend/Dockerfile`

---

## 三、容器网络

### 3.1 网络配置
- 使用 Docker Bridge 网络
- 容器间通过服务名通信

### 3.2 端口映射
- 前端: 80:80
- 后端: 8080:8080
- MySQL: 3306:3306
- Redis: 6379:6379
- MinIO: 9000:9000, 9001:9001

---

## 四、数据卷

### 4.1 持久化存储
- MySQL 数据: `mysql_data`
- Redis 数据: `redis_data`
- MinIO 数据: `minio_data`

### 4.2 备份恢复
- 定期备份数据卷
- 支持数据卷迁移

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

