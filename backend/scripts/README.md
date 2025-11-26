# 后端脚本说明

## 脚本列表

### docker-setup.sh
Docker 环境设置脚本，用于快速启动所有 Docker 服务。

**使用方法**：
```bash
./docker-setup.sh
```

**功能**：
- 检查 Docker 环境
- 创建 .env 文件（如果不存在）
- 启动所有服务（MySQL, Redis, MinIO）
- 显示服务访问信息

### check-docker.sh
检查 Docker 服务状态脚本。

**使用方法**：
```bash
./check-docker.sh
```

**功能**：
- 检查 Docker 是否运行
- 检查各个服务容器状态
- 测试服务连接
- 显示连接信息

---

## 数据库初始化

数据库初始化脚本 `init.sql` 会在 MySQL 容器首次启动时自动执行。

如需手动执行：

```bash
# 方式一：通过 Docker 执行
docker exec -i zhk-mysql-dev mysql -uroot -proot123456 < init.sql

# 方式二：通过本地 MySQL 客户端
mysql -h localhost -u root -proot123456 < init.sql
```

---

## 常用命令

```bash
# 启动服务
docker-compose -f ../../docker-compose.dev.yml up -d

# 停止服务
docker-compose -f ../../docker-compose.dev.yml down

# 查看日志
docker-compose -f ../../docker-compose.dev.yml logs -f

# 重启服务
docker-compose -f ../../docker-compose.dev.yml restart
```

