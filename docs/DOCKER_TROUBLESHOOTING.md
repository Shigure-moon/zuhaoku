# Docker 故障排查指南

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、镜像拉取问题

### 1.1 网络超时问题

**问题现象**：
```
Error response from daemon: Get "https://registry-1.docker.io/v2/": context deadline exceeded
```

**解决方案**：

#### 方案一：配置 Docker 镜像加速器（推荐）

1. 创建或编辑 `/etc/docker/daemon.json`：

```bash
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com",
    "https://mirror.baidubce.com"
  ]
}
EOF
```

2. 重启 Docker 服务：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```

3. 验证配置：

```bash
docker info | grep -A 10 "Registry Mirrors"
```

#### 方案二：使用国内镜像源

如果特定版本镜像拉取失败，可以临时使用 `latest` 标签：

```bash
# 编辑 docker-compose.dev.yml，将特定版本改为 latest
# 例如：mysql:8.0.44-debian → mysql:8.0
```

#### 方案三：手动拉取镜像

```bash
# 逐个拉取镜像
docker pull mysql:8.0.44-debian
docker pull redis:7.2-alpine
docker pull minio/minio:latest
docker pull phpmyadmin/phpmyadmin:latest
docker pull rediscommander/redis-commander:latest

# 然后重新运行 docker-compose
docker compose -f docker-compose.dev.yml up -d
```

#### 方案四：使用代理

如果有代理服务器，可以配置 Docker 使用代理：

```bash
sudo mkdir -p /etc/systemd/system/docker.service.d
sudo tee /etc/systemd/system/docker.service.d/http-proxy.conf <<-'EOF'
[Service]
Environment="HTTP_PROXY=http://proxy.example.com:8080"
Environment="HTTPS_PROXY=http://proxy.example.com:8080"
Environment="NO_PROXY=localhost,127.0.0.1"
EOF

sudo systemctl daemon-reload
sudo systemctl restart docker
```

---

## 二、容器启动失败

### 2.1 端口占用问题

**问题现象**：
```
Error: bind: address already in use
```

**解决方案**：

```bash
# 检查端口占用
sudo netstat -tulpn | grep :3306
sudo netstat -tulpn | grep :6379
sudo netstat -tulpn | grep :9000

# 停止占用端口的服务，或修改 docker-compose.dev.yml 中的端口映射
```

### 2.2 权限问题

**问题现象**：
```
Permission denied
```

**解决方案**：

```bash
# 将当前用户添加到 docker 组
sudo usermod -aG docker $USER

# 重新登录或执行
newgrp docker
```

### 2.3 数据卷权限问题

**问题现象**：
```
Cannot create directory: Permission denied
```

**解决方案**：

```bash
# 清理数据卷并重新创建
docker compose -f docker-compose.dev.yml down -v
docker compose -f docker-compose.dev.yml up -d
```

---

## 三、服务连接问题

### 3.1 MySQL 连接失败

**检查步骤**：

```bash
# 1. 检查容器是否运行
docker ps | grep mysql

# 2. 查看 MySQL 日志
docker logs zhk-mysql-dev

# 3. 测试连接
docker exec -it zhk-mysql-dev mysql -uroot -proot123456

# 4. 检查健康状态
docker inspect zhk-mysql-dev | grep -A 10 Health
```

### 3.2 Redis 连接失败

**检查步骤**：

```bash
# 1. 检查容器是否运行
docker ps | grep redis

# 2. 测试连接
docker exec -it zhk-redis-dev redis-cli ping

# 3. 查看日志
docker logs zhk-redis-dev
```

### 3.3 MinIO 连接失败

**检查步骤**：

```bash
# 1. 检查容器是否运行
docker ps | grep minio

# 2. 测试健康检查
curl http://localhost:9000/minio/health/live

# 3. 查看日志
docker logs zhk-minio-dev
```

---

## 四、数据持久化问题

### 4.1 数据丢失

**解决方案**：

```bash
# 查看数据卷
docker volume ls | grep zhk

# 备份数据卷
docker run --rm -v zhk-zuhaoku_mysql_dev_data:/data -v $(pwd):/backup alpine tar czf /backup/mysql_backup.tar.gz /data

# 恢复数据卷
docker run --rm -v zhk-zuhaoku_mysql_dev_data:/data -v $(pwd):/backup alpine tar xzf /backup/mysql_backup.tar.gz -C /
```

---

## 五、性能优化

### 5.1 镜像拉取慢

**优化建议**：

1. 使用镜像加速器（见 1.1 节）
2. 使用国内镜像仓库
3. 使用代理服务器

### 5.2 容器启动慢

**优化建议**：

1. 减少健康检查间隔（开发环境）
2. 使用 `depends_on` 的 `condition: service_healthy`
3. 优化镜像大小（使用 alpine 版本）

---

## 六、常用诊断命令

```bash
# 查看所有容器状态
docker compose -f docker-compose.dev.yml ps

# 查看服务日志
docker compose -f docker-compose.dev.yml logs -f [service_name]

# 查看资源使用情况
docker stats

# 查看网络
docker network ls
docker network inspect zhk-zuhaoku_zhk-dev-network

# 查看数据卷
docker volume ls
docker volume inspect zhk-zuhaoku_mysql_dev_data

# 进入容器
docker exec -it zhk-mysql-dev bash
docker exec -it zhk-redis-dev sh
```

---

## 七、完全重置环境

如果遇到无法解决的问题，可以完全重置：

```bash
# ⚠️ 警告：这会删除所有数据
docker compose -f docker-compose.dev.yml down -v
docker system prune -a --volumes

# 重新启动
./backend/scripts/docker-setup.sh
```

---

## 八、获取帮助

如果以上方案都无法解决问题，请：

1. 收集错误日志：`docker compose -f docker-compose.dev.yml logs > error.log`
2. 检查系统信息：`docker info`
3. 检查 Docker 版本：`docker --version` 和 `docker compose version`
4. 查看系统资源：`free -h` 和 `df -h`

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

