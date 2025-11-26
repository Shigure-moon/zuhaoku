# 支付宝接入快速开始

## 一、配置步骤

### 1. 获取应用ID和支付宝公钥

1. 登录 [支付宝开放平台](https://open.alipay.com/)
2. 进入 **应用管理** -> 选择应用
3. 获取 **应用ID (APP_ID)**
4. 在 **接口加签方式** 中下载 **支付宝公钥**

### 2. 配置密钥

#### 2.1 生成密钥（如果还没有）

```bash
cd backend/scripts
./generate-alipay-keys.sh
```

#### 2.2 上传应用公钥

1. 复制脚本输出的 **应用公钥字符串**
2. 在支付宝开放平台 **接口加签方式** 中上传
3. 下载 **支付宝公钥**

### 3. 配置应用网关

1. 在支付宝开放平台 **应用信息** -> **应用网关** 中配置：
   ```
   http://your-domain.com/api/v1/payments/alipay/notify
   ```
   
   开发环境可以使用内网穿透工具（如 ngrok）：
   ```
   https://your-ngrok-url.ngrok.io/api/v1/payments/alipay/notify
   ```

### 4. 更新配置文件

编辑 `backend/zhk-monolith/zhk-user/src/main/resources/application-dev.yml`：

```yaml
zhk:
  alipay:
    app-id: YOUR_APP_ID  # 替换为实际的应用ID
    private-key: |
      -----BEGIN PRIVATE KEY-----
      # 复制 backend/scripts/alipay-keys/alipay_app_private_key_pkcs8.pem 文件内容
      MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...
      -----END PRIVATE KEY-----
    alipay-public-key: |
      -----BEGIN PUBLIC KEY-----
      # 从支付宝开放平台下载的支付宝公钥
      MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
      -----END PUBLIC KEY-----
    gateway-url: https://openapi.alipay.com/gateway.do
    # 沙箱环境: https://openapi.alipaydev.com/gateway.do
    encrypt-key: lE4SjgSUUKi5tmrfKB3A3g==
    notify-url: http://localhost:8081/api/v1/payments/alipay/notify
    return-url: http://localhost:3000/tenant/orders
    sign-type: RSA2
    charset: UTF-8
```

---

## 二、测试

### 1. 沙箱环境测试

1. 使用沙箱网关：
   ```yaml
   gateway-url: https://openapi.alipaydev.com/gateway.do
   ```

2. 获取沙箱账号：
   - 登录支付宝开放平台
   - 进入 **开发助手** -> **沙箱环境**
   - 获取沙箱买家账号和密码

3. 测试流程：
   - 创建订单
   - 选择支付宝支付
   - 使用沙箱账号完成支付
   - 验证回调处理

### 2. 验证回调

使用 curl 测试回调接口：

```bash
curl -X POST http://localhost:8081/api/v1/payments/alipay/notify \
  -d "trade_status=TRADE_SUCCESS&out_trade_no=ALIPAY1234567890&trade_no=2023112022001234567890123456"
```

---

## 三、常见问题

### Q1: 签名验证失败

**原因**：
- 支付宝公钥配置错误
- 私钥格式不正确
- 字符编码问题

**解决**：
1. 确认支付宝公钥是从开放平台下载的
2. 确认私钥是PKCS#8格式
3. 确认字符编码为UTF-8

### Q2: 回调未收到

**原因**：
- 应用网关未配置
- 回调URL不可访问
- 防火墙拦截

**解决**：
1. 配置应用网关为公网可访问的URL
2. 使用内网穿透工具（开发环境）
3. 检查防火墙和Nginx配置

### Q3: 支付页面无法打开

**原因**：
- 支付URL格式错误
- 订单信息不正确

**解决**：
1. 检查返回的支付URL是否为HTML表单
2. 前端需要将HTML表单提交到支付宝
3. 或使用iframe嵌入支付页面

---

## 四、生产环境配置

### 1. 使用环境变量

```bash
export ALIPAY_APP_ID=YOUR_APP_ID
export ALIPAY_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
export ALIPAY_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
export ALIPAY_ENCRYPT_KEY="lE4SjgSUUKi5tmrfKB3A3g=="
export ALIPAY_NOTIFY_URL="https://your-domain.com/api/v1/payments/alipay/notify"
```

### 2. 使用密钥管理服务

推荐使用：
- 阿里云KMS
- AWS Secrets Manager
- HashiCorp Vault

---

## 五、参考文档

- [支付宝接入文档](./ALIPAY_INTEGRATION.md) - 详细接入文档
- [密钥生成说明](../backend/scripts/README_ALIPAY_KEYS.md) - 密钥生成和使用
- [支付宝开放平台](https://open.alipay.com/) - 官方文档

---

**维护者**: shigure  
**最后更新**: 2025/11/20

