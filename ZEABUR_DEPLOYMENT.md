# Zeabur 部署指南

## 部署步骤

### 1. 连接 GitHub 仓库

1. 登录 [Zeabur](https://zeabur.com)
2. 创建新项目
3. 选择 "从 GitHub 仓库部署"
4. 选择仓库：`Shigure-moon/zuhaoku`
5. 点击 "导入"

### 2. 配置环境变量

在 Zeabur 控制台中，为服务配置以下环境变量：

#### 必需的环境变量

```bash
# Spring 配置
SPRING_PROFILES_ACTIVE=prod

# 数据库配置（云数据库）
MYSQL_HOST=mysql2.sqlpub.com
MYSQL_PORT=3307
MYSQL_DATABASE=zuhaoku
MYSQL_USER=shigure2
MYSQL_PASSWORD=NVxtg0a9HYU5i62K

# JWT 密钥
JWT_SECRET=993056993056993056993056993056993056

# 加密密钥
ENCRYPTION_KEY=993056993056993056993056993056993056
ENCRYPTION_MASTER_KEY=993056993056993056993056993056993056
```

#### 可选的环境变量

```bash
# Redis 配置（如果使用）
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your-redis-password

# MinIO 配置（如果使用）
MINIO_ENDPOINT=http://your-minio-endpoint:9000
MINIO_ACCESS_KEY=your-access-key
MINIO_SECRET_KEY=your-secret-key
MINIO_BUCKET=zhk-evidence

# 支付宝配置（如果使用）
ALIPAY_APP_ID=2021006112616763
ALIPAY_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----
ALIPAY_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----
ALIPAY_GATEWAY_URL=https://openapi.alipay.com/gateway.do
ALIPAY_ENCRYPT_KEY=your-encrypt-key
ALIPAY_NOTIFY_URL=https://zuhaoku.zeabur.app/api/v1/payments/alipay/notify
ALIPAY_RETURN_URL=https://your-frontend-domain.com/tenant/orders
ALIPAY_SIGN_TYPE=RSA2
ALIPAY_CHARSET=UTF-8
```

**注意**：支付宝密钥配置请参考 `ALIPAY_SETUP.md` 文档

### 3. 构建配置

Zeabur 会自动检测 `zeabur.yaml` 配置文件，使用以下配置：

- **构建类型**: Maven
- **构建命令**: `cd backend && mvn clean package -DskipTests`
- **输出目录**: `backend/zhk-monolith/zhk-user/target`
- **运行命令**: `java -jar backend/zhk-monolith/zhk-user/target/zhk-user-1.0.0-SNAPSHOT.jar`
- **端口**: 8080（Zeabur 会自动设置 PORT 环境变量）

### 4. 部署

1. 配置完环境变量后，点击 "部署"
2. 等待构建完成
3. 部署成功后，Zeabur 会提供一个公网访问地址

### 5. 验证部署

部署成功后，可以通过以下方式验证：

1. 访问健康检查端点（如果配置了 Actuator）：
   ```
   https://your-app.zeabur.app/actuator/health
   ```

2. 访问 API 端点：
   ```
   https://your-app.zeabur.app/api/v1/users/register
   ```

## 注意事项

1. **数据库连接**: 确保云数据库允许来自 Zeabur 的 IP 地址访问
2. **安全密钥**: 生产环境请务必修改 JWT_SECRET 和加密密钥
3. **端口配置**: 应用会自动使用 Zeabur 提供的 PORT 环境变量
4. **日志查看**: 在 Zeabur 控制台可以查看应用日志

## 故障排查

### 构建失败

- 检查 Maven 构建日志
- 确保所有依赖都能正常下载
- 检查 Java 版本（项目使用 JDK 17）

### 启动失败

- 检查环境变量是否配置正确
- 检查数据库连接是否正常
- 查看应用日志定位问题

### 数据库连接失败

- 确认数据库允许外部连接
- 检查防火墙设置
- 验证数据库用户名和密码

## 参考文档

- [Zeabur 官方文档](https://zeabur.com/docs)
- [Spring Boot 部署指南](https://zeabur.com/docs/zh-CN/guides/java/spring-boot)

