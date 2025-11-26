# Docker 管理文档

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、Docker Compose 配置

### 1.1 配置文件

- `docker-compose.yml` - 生产环境配置
- `docker-compose.dev.yml` - 开发环境配置（推荐开发使用）
- `.env.example` - 环境变量示例文件

### 1.2 服务列表

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| mysql | mysql:8.0 | 3306 | MySQL 数据库 |
| redis | redis:7.2-alpine | 6379 | Redis 缓存 |
| minio | minio/minio:latest | 9000, 9001 | MinIO 对象存储 |
| phpmyadmin | phpmyadmin/phpmyadmin | 8082 | MySQL 管理工具（可选） |
| redis-commander | rediscommander/redis-commander | 8081 | Redis 管理工具（可选） |

---

## 二、快速开始

### 2.1 环境准备

确保已安装：
- Docker >= 20.10
- Docker Compose >= 2.0

### 2.2 启动服务

#### 方式一：使用脚本（推荐）

```bash
cd /home/shigure/zuhaoku
./backend/scripts/docker-setup.sh
```

#### 方式二：手动启动

```bash
# 1. 复制环境变量文件
cp .env.example .env

# 2. 修改 .env 文件中的配置（如需要）

# 3. 启动服务
docker-compose -f docker-compose.dev.yml up -d

# 4. 查看服务状态
docker-compose -f docker-compose.dev.yml ps

# 5. 查看日志
docker-compose -f docker-compose.dev.yml logs -f
```

### 2.3 服务访问

启动成功后，可通过以下地址访问：

- **MySQL**: `localhost:3306`
  - 用户名: `root`
  - 密码: `root123456`（开发环境）
  - 数据库: `zhk_rental`

- **Redis**: `localhost:6379`
  - 密码: 无（开发环境）

- **MinIO Console**: `http://localhost:9001`
  - 用户名: `minioadmin`
  - 密码: `minioadmin123`（开发环境）

- **phpMyAdmin**: `http://localhost:8082`
  - 自动连接到 MySQL

- **Redis Commander**: `http://localhost:8081`
  - 自动连接到 Redis

---

## 三、常用命令

### 3.1 服务管理

```bash
# 启动服务
docker-compose -f docker-compose.dev.yml up -d

# 停止服务
docker-compose -f docker-compose.dev.yml down

# 重启服务
docker-compose -f docker-compose.dev.yml restart

# 查看服务状态
docker-compose -f docker-compose.dev.yml ps

# 查看日志
docker-compose -f docker-compose.dev.yml logs -f

# 查看特定服务日志
docker-compose -f docker-compose.dev.yml logs -f mysql
docker-compose -f docker-compose.dev.yml logs -f redis
docker-compose -f docker-compose.dev.yml logs -f minio
```

### 3.2 数据管理

```bash
# 进入 MySQL 容器
docker exec -it zhk-mysql-dev mysql -uroot -proot123456

# 进入 Redis 容器
docker exec -it zhk-redis-dev redis-cli

# 备份 MySQL 数据
docker exec zhk-mysql-dev mysqldump -uroot -proot123456 zhk_rental > backup.sql

# 恢复 MySQL 数据
docker exec -i zhk-mysql-dev mysql -uroot -proot123456 zhk_rental < backup.sql

# 查看数据卷
docker volume ls | grep zhk
```

### 3.3 清理操作

```bash
# 停止并删除容器
docker-compose -f docker-compose.dev.yml down

# 停止并删除容器和数据卷（⚠️ 危险：会删除所有数据）
docker-compose -f docker-compose.dev.yml down -v

# 清理未使用的资源
docker system prune -a
```

---

## 四、数据持久化

### 4.1 数据卷

所有数据存储在 Docker 数据卷中：

- `mysql_dev_data` - MySQL 数据
- `redis_dev_data` - Redis 数据
- `minio_dev_data` - MinIO 数据

### 4.2 数据备份

```bash
# 备份 MySQL
docker exec zhk-mysql-dev mysqldump -uroot -proot123456 --all-databases > mysql_backup_$(date +%Y%m%d).sql

# 备份 Redis
docker exec zhk-redis-dev redis-cli SAVE
docker cp zhk-redis-dev:/data/dump.rdb ./redis_backup_$(date +%Y%m%d).rdb

# 备份 MinIO（通过 MinIO Client）
# 需要先安装 mc 客户端
mc mirror local/minio-data ./minio_backup
```

### 4.3 数据恢复

```bash
# 恢复 MySQL
docker exec -i zhk-mysql-dev mysql -uroot -proot123456 < mysql_backup_20241118.sql

# 恢复 Redis
docker cp redis_backup_20241118.rdb zhk-redis-dev:/data/dump.rdb
docker restart zhk-redis-dev
```

---

## 五、健康检查

所有服务都配置了健康检查，可以通过以下命令查看：

```bash
# 查看服务健康状态
docker-compose -f docker-compose.dev.yml ps

# 手动检查
docker inspect --format='{{.State.Health.Status}}' zhk-mysql-dev
docker inspect --format='{{.State.Health.Status}}' zhk-redis-dev
docker inspect --format='{{.State.Health.Status}}' zhk-minio-dev
```

---

## 六、故障排查

### 6.1 服务无法启动

```bash
# 查看服务日志
docker-compose -f docker-compose.dev.yml logs [service_name]

# 检查端口占用
netstat -tulpn | grep :3306
netstat -tulpn | grep :6379
netstat -tulpn | grep :9000
```

### 6.2 数据库连接失败

1. 检查 MySQL 容器是否运行：`docker ps | grep mysql`
2. 检查 MySQL 日志：`docker logs zhk-mysql-dev`
3. 验证连接信息是否正确

### 6.3 Redis 连接失败

1. 检查 Redis 容器是否运行：`docker ps | grep redis`
2. 测试连接：`docker exec -it zhk-redis-dev redis-cli ping`
3. 检查密码配置（如果设置了密码）

---

## 七、生产环境配置

### 7.1 安全建议

1. **修改默认密码**
   - 修改 `.env` 文件中的密码
   - 使用强密码

2. **限制网络访问**
   - 生产环境不要暴露管理工具端口
   - 使用防火墙限制访问

3. **启用 SSL/TLS**
   - MySQL 启用 SSL 连接
   - MinIO 启用 HTTPS

### 7.2 性能优化

```yaml
# MySQL 性能优化
command:
  - --innodb_buffer_pool_size=2G
  - --max_connections=2000
  - --query_cache_size=256M

# Redis 性能优化
command: redis-server --maxmemory 2gb --maxmemory-policy allkeys-lru
```

---

## 八、参考资源

- [Docker 官方文档](https://docs.docker.com/)
- [Docker Compose 文档](https://docs.docker.com/compose/)
- [MySQL Docker 镜像](https://hub.docker.com/_/mysql)
- [Redis Docker 镜像](https://hub.docker.com/_/redis)
- [MinIO Docker 镜像](https://hub.docker.com/r/minio/minio)

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

