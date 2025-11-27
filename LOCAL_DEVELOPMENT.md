# 本地开发指南

## 一、环境准备

### 1.1 启动数据库

```bash
# 使用 Docker Compose 启动数据库
cd /home/shigure/zuhaoku
docker-compose -f docker-compose.dev.yml up -d

# 或使用云数据库（已配置）
# MYSQL_HOST=mysql2.sqlpub.com
# MYSQL_PORT=3307
# MYSQL_DATABASE=zuhaoku
# MYSQL_USER=shigure2
# MYSQL_PASSWORD=NVxtg0a9HYU5i62K
```

### 1.2 启动应用

```bash
cd backend/zhk-monolith/zhk-user
mvn spring-boot:run
```

应用将在 `http://localhost:8081` 启动

## 二、支付宝支付配置（本地开发）

### 2.1 使用沙箱环境（推荐）

本地开发时，建议使用支付宝沙箱环境进行测试：

1. **修改网关地址**：编辑 `application-dev.yml`，将网关改为沙箱环境：
   ```yaml
   gateway-url: https://openapi.alipaydev.com/gateway.do
   ```

2. **获取沙箱账号**：
   - 登录 [支付宝开放平台](https://open.alipay.com/)
   - 进入 **开发助手** -> **沙箱环境**
   - 获取沙箱买家账号和密码

3. **测试支付**：
   - 使用沙箱买家账号登录支付宝
   - 完成支付测试

### 2.2 配置回调地址（本地开发）

#### 方案1：使用内网穿透（推荐）

使用内网穿透工具（如 ngrok、natapp）将本地服务暴露到公网：

1. **安装 ngrok**：
   ```bash
   # 下载 ngrok
   # https://ngrok.com/download
   
   # 启动内网穿透
   ngrok http 8081
   ```

2. **获取公网地址**：
   ```
   Forwarding: https://xxxx-xxxx-xxxx.ngrok.io -> http://localhost:8081
   ```

3. **更新配置**：
   ```yaml
   notify-url: https://xxxx-xxxx-xxxx.ngrok.io/api/v1/payments/alipay/notify
   return-url: http://localhost:3000/tenant/orders
   ```

4. **在支付宝开放平台配置应用网关**：
   ```
   https://xxxx-xxxx-xxxx.ngrok.io/api/v1/payments/alipay/notify
   ```

#### 方案2：使用测试支付页面（临时方案）

如果暂时无法配置回调，可以使用测试支付页面：

1. 支付接口会返回测试支付页面 URL：`/pay/{paymentId}`
2. 在测试页面手动模拟支付成功
3. 调用支付成功接口更新订单状态

### 2.3 验证配置

启动应用后，检查日志：

```bash
# 应该看到：
支付宝客户端初始化成功: appId=2021006112616763, gatewayUrl=https://openapi.alipay.com/gateway.do
```

如果没有看到，检查：
- 配置文件中的 AppID 是否正确
- 私钥和公钥格式是否正确

## 三、测试支付流程

### 3.1 创建测试订单

```bash
# 1. 登录获取 Token
curl -X POST http://localhost:8081/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{
    "mobile": "13800000001",
    "password": "dev123456"
  }'

# 2. 创建订单（需要先有账号和游戏数据）
# 3. 创建支付
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "orderId": 1,
    "paymentType": "alipay"
  }'
```

### 3.2 测试支付回调

如果配置了内网穿透，支付完成后：
1. 支付宝会发送异步通知到回调地址
2. 检查应用日志，应该看到：
   ```
   收到支付宝异步通知: {...}
   支付成功: orderId=1, paymentId=1
   ```

### 3.3 手动测试回调（开发调试）

如果无法接收真实回调，可以手动测试：

```bash
# 模拟支付宝回调
curl -X POST http://localhost:8081/api/v1/payments/alipay/notify \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "trade_status=TRADE_SUCCESS&out_trade_no=ALIPAY1234567890&trade_no=2021112622001234567890123456&total_amount=100.00"
```

## 四、常见问题

### 4.1 支付宝客户端未初始化

**问题**：日志中没有看到 "支付宝客户端初始化成功"

**解决**：
1. 检查 `application-dev.yml` 中的配置
2. 确认 AppID、私钥、公钥都已正确配置
3. 检查密钥格式（必须包含 BEGIN/END 标记）

### 4.2 支付页面无法打开

**问题**：调用支付接口后，返回的支付 URL 无法打开

**解决**：
1. 检查网关地址是否正确（生产环境 vs 沙箱环境）
2. 确认 AppID 与网关环境匹配
3. 检查网络连接

### 4.3 回调接收不到

**问题**：支付完成后，没有收到回调

**解决**：
1. 确认应用网关已配置
2. 确认回调地址是公网可访问的 HTTPS 地址
3. 检查防火墙设置
4. 查看支付宝开放平台的回调日志

### 4.4 签名验证失败

**问题**：回调时签名验证失败

**解决**：
1. 检查支付宝公钥是否正确
2. 确认使用的是从开放平台下载的支付宝公钥
3. 检查字符编码设置

## 五、开发工具

### 5.1 测试支付页面

访问：`http://localhost:8081/pay/{paymentId}`

可以手动模拟支付成功/失败

### 5.2 API 测试

使用 Postman 或 curl 测试 API：

```bash
# 健康检查
curl http://localhost:8081/health

# 获取游戏列表
curl http://localhost:8081/api/v1/games

# 获取账号列表
curl http://localhost:8081/api/v1/accounts
```

## 六、数据库连接

### 6.1 使用本地数据库

```yaml
# application-dev.yml
datasource:
  url: jdbc:mysql://localhost:3307/zhk_rental?...
  username: root
  password: root123456
```

### 6.2 使用云数据库

```yaml
# application-dev.yml
datasource:
  url: jdbc:mysql://mysql2.sqlpub.com:3307/zuhaoku?...
  username: shigure2
  password: NVxtg0a9HYU5i62K
```

## 七、下一步

1. ✅ 配置完成，可以开始开发
2. 📝 测试支付流程
3. 🔧 根据实际需求调整配置
4. 🚀 准备部署到生产环境

---

**提示**：本地开发时，建议使用沙箱环境，避免影响生产环境数据。

