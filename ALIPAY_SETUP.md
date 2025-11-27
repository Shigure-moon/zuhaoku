# 支付宝电脑网站支付接入配置指南

## 应用信息

- **应用名称**: Woo租号酷
- **AppID**: 2021006112616763
- **应用类型**: 网页应用
- **状态**: 开发中

## 一、准备工作

### 1.1 获取密钥信息

1. 登录 [支付宝开放平台](https://open.alipay.com/)
2. 进入 **应用管理** -> 选择应用 "Woo租号酷"
3. 在 **接口加签方式** 中：
   - 查看或上传 **应用公钥**
   - 下载 **支付宝公钥**

### 1.2 生成应用密钥对（如果还没有）

如果还没有生成密钥对，可以使用项目提供的脚本：

```bash
cd backend/scripts
./generate-alipay-keys.sh
```

生成的密钥文件：
- `alipay_app_private_key_pkcs8.pem` - 应用私钥（用于签名）
- `alipay_app_public_key.pem` - 应用公钥（上传到支付宝开放平台）

### 1.3 配置应用网关

在支付宝开放平台中配置 **应用网关**：

1. 进入 **应用信息** -> **应用网关**
2. 设置网关地址（生产环境）：
   ```
   https://zuhaoku.zeabur.app/api/v1/payments/alipay/notify
   ```
   
   注意：根据实际部署域名修改

## 二、配置环境变量

### 2.1 Zeabur 环境变量配置

在 Zeabur 控制台中，为服务添加以下环境变量：

```bash
# 支付宝基础配置
ALIPAY_APP_ID=2021006112616763
ALIPAY_GATEWAY_URL=https://openapi.alipay.com/gateway.do
ALIPAY_SIGN_TYPE=RSA2
ALIPAY_CHARSET=UTF-8

# 支付宝密钥（从支付宝开放平台获取）
ALIPAY_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----
MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...
（完整的私钥内容，包含换行符）
-----END PRIVATE KEY-----

ALIPAY_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
（完整的支付宝公钥内容，包含换行符）
-----END PUBLIC KEY-----

# 接口内容加密密钥（如果启用了加密）
ALIPAY_ENCRYPT_KEY=你的加密密钥

# 回调地址（根据实际部署域名修改）
ALIPAY_NOTIFY_URL=https://zuhaoku.zeabur.app/api/v1/payments/alipay/notify
ALIPAY_RETURN_URL=https://your-frontend-domain.com/tenant/orders
```

### 2.2 环境变量格式说明

**重要**：在 Zeabur 中配置多行环境变量时：

1. **私钥和公钥**：需要保留换行符，可以使用 `\n` 表示换行：
   ```
   ALIPAY_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...\n-----END PRIVATE KEY-----
   ```

2. 或者使用环境变量文件（如果 Zeabur 支持）

### 2.3 本地开发环境配置

编辑 `backend/zhk-monolith/zhk-user/src/main/resources/application-dev.yml`：

```yaml
zhk:
  alipay:
    app-id: 2021006112616763
    private-key: |
      -----BEGIN PRIVATE KEY-----
      MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQC...
      （复制 backend/scripts/alipay-keys/alipay_app_private_key_pkcs8.pem 的内容）
      -----END PRIVATE KEY-----
    alipay-public-key: |
      -----BEGIN PUBLIC KEY-----
      MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA...
      （从支付宝开放平台下载的支付宝公钥）
      -----END PUBLIC KEY-----
    gateway-url: https://openapi.alipay.com/gateway.do
    # 沙箱环境: https://openapi.alipaydev.com/gateway.do
    encrypt-key: 你的加密密钥（如果启用了加密）
    notify-url: http://localhost:8081/api/v1/payments/alipay/notify
    return-url: http://localhost:3000/tenant/orders
    sign-type: RSA2
    charset: UTF-8
```

## 三、测试配置

### 3.1 验证配置

1. 启动应用后，检查日志中是否有：
   ```
   支付宝客户端初始化成功: appId=2021006112616763, gatewayUrl=https://openapi.alipay.com/gateway.do
   ```

2. 如果没有看到此日志，检查：
   - 环境变量是否正确配置
   - 私钥和公钥格式是否正确
   - AppID 是否正确

### 3.2 沙箱环境测试

在正式上线前，建议使用沙箱环境测试：

1. 修改网关地址为沙箱环境：
   ```bash
   ALIPAY_GATEWAY_URL=https://openapi.alipaydev.com/gateway.do
   ```

2. 使用沙箱账号进行测试支付

3. 测试完成后，改回生产环境网关

## 四、支付流程

### 4.1 创建支付

用户下单后，调用支付接口：
```
POST /api/v1/payments
```

返回的 `paymentUrl` 是支付宝支付页面的 HTML 表单，前端需要：
1. 将表单提交到支付宝
2. 或者使用 iframe 嵌入表单

### 4.2 支付回调

- **异步通知**: `POST /api/v1/payments/alipay/notify`
  - 支付宝服务器主动调用
  - 必须返回 "success" 或 "fail"
  - 需要验证签名

- **同步跳转**: `GET /api/v1/payments/alipay/return`
  - 用户支付完成后跳转
  - 重定向到前端订单页面

## 五、常见问题

### 5.1 签名验证失败

**原因**：
- 私钥或公钥格式不正确
- 密钥与 AppID 不匹配
- 字符编码问题

**解决**：
1. 检查密钥格式（必须包含 BEGIN/END 标记）
2. 确认使用的是 PKCS#8 格式的私钥
3. 确认支付宝公钥是从开放平台下载的

### 5.2 回调接收不到

**原因**：
- 应用网关未配置
- 网关地址不可访问
- 防火墙阻止

**解决**：
1. 在支付宝开放平台配置应用网关
2. 确保网关地址是公网可访问的 HTTPS 地址
3. 检查服务器防火墙设置

### 5.3 支付页面无法打开

**原因**：
- 网关地址配置错误
- AppID 不正确
- 接口权限未开通

**解决**：
1. 检查 `ALIPAY_GATEWAY_URL` 是否正确
2. 确认 AppID 与开放平台一致
3. 确认已开通"电脑网站支付"产品

## 六、安全注意事项

1. **私钥保护**
   - 不要将私钥提交到代码仓库
   - 使用环境变量或密钥管理服务
   - 定期轮换密钥

2. **签名验证**
   - 所有回调必须验证签名
   - 验证失败必须拒绝请求

3. **幂等性处理**
   - 支付回调可能重复发送
   - 需要保证处理逻辑的幂等性

4. **HTTPS**
   - 生产环境必须使用 HTTPS
   - 确保回调地址使用 HTTPS

## 七、参考文档

- [支付宝开放平台](https://open.alipay.com/)
- [电脑网站支付文档](https://opendocs.alipay.com/open/270/105898)
- [Java SDK 文档](https://opendocs.alipay.com/apis/api_1/alipay.trade.page.pay)
- [密钥与证书格式说明](https://opendocs.alipay.com/common/02kkv7)

---

**维护者**: shigure  
**最后更新**: 2025/11/26

