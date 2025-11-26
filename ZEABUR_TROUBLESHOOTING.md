# Zeabur 部署故障排查

## 问题：应用无法访问，返回 404

### 可能的原因

1. **Spring Boot 应用未启动**
   - 检查 Zeabur 控制台的 "Runtime Logs"（运行时日志）
   - 查看是否有 Spring Boot 启动日志
   - 如果只有 Caddy 日志，说明应用可能启动失败

2. **数据库连接失败**
   - 检查环境变量 `MYSQL_PASSWORD` 是否正确设置
   - 确认云数据库允许外部连接
   - 检查数据库防火墙设置

3. **端口配置问题**
   - 确保应用监听 `PORT` 环境变量指定的端口（默认 8080）

## 排查步骤

### 1. 查看运行时日志

在 Zeabur 控制台：
1. 进入项目 → 服务 → 点击服务名称
2. 查看 "Runtime Logs" 标签页
3. 查找 Spring Boot 启动日志，应该看到类似：
   ```
   Started ZhkUserApplication in X.XXX seconds
   ```

### 2. 检查环境变量

确保以下环境变量已正确配置：

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

### 3. 测试健康检查端点

部署成功后，访问以下端点：

- 根路径：`https://your-app.zeabur.app/`
- 健康检查：`https://your-app.zeabur.app/health`
- 测试端点：`https://your-app.zeabur.app/api/v1/test/health`

### 4. 常见错误及解决方案

#### 错误：数据库连接失败

**症状**：日志中出现 `Communications link failure` 或 `Access denied`

**解决方案**：
1. 检查数据库密码是否正确
2. 确认数据库允许外部 IP 访问
3. 检查数据库防火墙规则

#### 错误：端口已被占用

**症状**：`Port 8080 is already in use`

**解决方案**：
- Zeabur 会自动设置 `PORT` 环境变量，应用会自动使用该端口
- 如果仍有问题，检查 `application-prod.yml` 中的端口配置

#### 错误：JAR 文件未找到

**症状**：`no main manifest attribute` 或 `Could not find or load main class`

**解决方案**：
1. 检查构建是否成功
2. 确认 `zeabur.yaml` 中的 `outputDirectory` 路径正确
3. 检查 `pom.xml` 中是否配置了 `spring-boot-maven-plugin`

## 验证部署

部署成功后，应该能够访问：

1. **根路径**：
   ```bash
   curl https://your-app.zeabur.app/
   ```
   应该返回 JSON 响应，包含服务状态信息

2. **健康检查**：
   ```bash
   curl https://your-app.zeabur.app/health
   ```

3. **API 端点**：
   ```bash
   curl https://your-app.zeabur.app/api/v1/games
   ```

## 联系支持

如果问题仍然存在：
1. 查看完整的运行时日志
2. 检查构建日志是否有错误
3. 确认所有环境变量都已正确配置
4. 参考 [Zeabur 官方文档](https://zeabur.com/docs)

