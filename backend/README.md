# 租号酷后端项目

## 项目简介

租号酷（ZHK-RentalCore）游戏账号分时租借平台后端系统。

## 技术栈

- **框架**: Spring Boot 3.2.0
- **网关**: Spring Cloud Gateway
- **ORM**: MyBatis-Plus 3.5.5
- **缓存**: Redis + Redisson
- **数据库**: MySQL 8.0+
- **对象存储**: MinIO

## 项目结构

```
backend/
├── zhk-common/              # 公共模块
│   ├── zhk-common-core/    # 核心工具类
│   ├── zhk-common-security/# 安全相关
│   └── zhk-common-web/     # Web 相关
├── zhk-infrastructure/     # 基础设施模块
│   ├── zhk-database/       # 数据库配置
│   ├── zhk-redis/          # Redis 配置
│   └── zhk-minio/          # MinIO 配置
├── zhk-monolith/           # 业务聚合层
│   ├── zhk-user/           # 用户服务模块
│   ├── zhk-asset/          # 资产服务模块
│   ├── zhk-order/          # 订单服务模块
│   ├── zhk-wallet/         # 钱包服务模块
│   └── zhk-risk/           # 风控服务模块
└── zhk-gateway/            # API 网关
```

## 快速开始

### 1. 环境要求

- JDK >= 17
- Maven >= 3.8.0
- MySQL >= 8.0
- Redis >= 7.0

### 2. 启动 Docker 服务（推荐）

```bash
# 使用 Docker Compose 启动数据库和中间件
cd ..
./backend/scripts/docker-setup.sh

# 或手动启动
docker-compose -f docker-compose.dev.yml up -d
```

### 2.1 数据库初始化

数据库初始化脚本会在 MySQL 容器首次启动时自动执行。

如需手动初始化：

```bash
# 进入 MySQL 容器
docker exec -it zhk-mysql-dev mysql -uroot -proot123456

# 或使用本地 MySQL 客户端
mysql -h localhost -u root -p < backend/scripts/init.sql
```

### 3. 启动项目

```bash
# 编译项目
mvn clean install

# 启动网关
cd zhk-gateway
mvn spring-boot:run

# 启动业务服务
cd zhk-monolith
mvn spring-boot:run
```

## Docker 管理

使用 Docker 管理数据库和中间件，详见：[Docker 管理文档](../docs/DOCKER_MANAGEMENT.md)

### 快速启动

```bash
# 启动所有服务（MySQL, Redis, MinIO）
cd ..
./backend/scripts/docker-setup.sh
```

## 开发文档

详细开发文档请参考：
- [后端开发文档](../docs/BACKEND_DEVELOPMENT.md)
- [Docker 管理文档](../docs/DOCKER_MANAGEMENT.md)

## 许可证

Copyright © 2025 租号酷

