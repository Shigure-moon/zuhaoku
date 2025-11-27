# 支付宝支付配置完成清单

## ✅ 已完成的配置

### 1. 应用信息
- **应用名称**: Woo租号酷
- **AppID**: 2021006112616763
- **应用类型**: 网页应用

### 2. 密钥文件
- ✅ 应用私钥: `backend/scripts/alipay-keys/alipay_app_private_key_pkcs8.pem`
- ✅ 应用公钥: `backend/scripts/alipay-keys/alipay_app_public_key.pem`
- ✅ 支付宝公钥: `backend/scripts/alipay-keys/alipayPublicKey_RSA2.txt` (已格式化)

### 3. 代码实现
- ✅ 支付宝 SDK 已添加 (alipay-sdk-java 4.40.0.ALL)
- ✅ AlipayProperties 配置类
- ✅ AlipayClientConfig 客户端配置
- ✅ AlipayPaymentServiceImpl 支付服务实现
- ✅ AlipayNotifyController 回调控制器

### 4. 配置文件
- ✅ 开发环境配置已更新 (`application-dev.yml`)
- ✅ 生产环境配置已更新 (`application-prod.yml`)

## 📋 待完成的配置

### 1. 支付宝开放平台配置

#### 1.1 配置应用网关

1. 登录 [支付宝开放平台](https://open.alipay.com/)
2. 进入 **应用管理** -> 选择应用 "Woo租号酷" (2021006112616763)
3. 进入 **应用信息** -> **应用网关**
4. 设置网关地址：
   ```
   https://zuhaoku.zeabur.app/api/v1/payments/alipay/notify
   ```
   ⚠️ **注意**: 根据实际部署域名修改

#### 1.2 验证接口加签方式

1. 进入 **接口加签方式**
2. 确认已上传应用公钥
3. 确认已下载支付宝公钥（应与 `alipayPublicKey_RSA2.txt` 一致）

### 2. Zeabur 环境变量配置

在 Zeabur 控制台中添加以下环境变量：

```bash
# 支付宝基础配置
ALIPAY_APP_ID=2021006112616763
ALIPAY_GATEWAY_URL=https://openapi.alipay.com/gateway.do
ALIPAY_SIGN_TYPE=RSA2
ALIPAY_CHARSET=UTF-8

# 支付宝私钥（PKCS#8格式，使用 \n 表示换行）
ALIPAY_PRIVATE_KEY=-----BEGIN PRIVATE KEY-----\nMIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCjLQcH89cIzvtG\neoUKwzqsEoMDz2RoH16wRXGUipfTQPOiTITVBtGGCPWbEtgfrtH548+is6LfUXDS\nVywDsYIQrX60RwqjEKuKrU1392nBpa7TvYycj1KUP/AhSFEmMbvD4DX+hRDrc9ms\n3bkl12hqLQV6VCKv1QqSdAq2sQFXjd4CEWer/4EpYS7vRWnO/DmlJAGpEasSVQrR\nZHZE13ShMcJGOAhqSG4EEOqiVpTV7OKdnXaFu2/Tf0WcDOD76B586LD8FY0G+PgX\nNqd7/e9lpUPSw/6jXLKVgu+oklQnSHkfTL5OV3HuNoLnaD4gHdwqRsziqkfOO0Nl\npDlowTrXAgMBAAECggEAJ7L2QXSNLfrxfkmjoaUy2pdcYaps4rozFs6mXf3EB2UH\ngUZ9kLhqdGIsovBYPYkSU+odY5exugQ93ybPMgzXOIiFSYX5LAgLvB8CuHussqzF\nFoXItnRWInRVzzshZxn1dSfQ6x3jqm0SmfCNsYuxPxe5+/OJRt8dhdkOnDj/OxYI\n3FFBRrjciV46+66nYjeHkLsglOisSfn/wrUWzopPkHoXMnwVbsUcXJdR86UWxn3t\nk3wPgoq/BCBzTFLIMviNG4nDIhCe4t8rUVPKvopnBumVj3XiwBExj7c2C+jEkAj4\nD+DAsCzJRQobZi/JNYWuO7S8majmER/2NJ2xtn13DQKBgQDSxyW97Ikz4p/bNfa+\nMISlD3d2XntQsaY/0T7GkmS6TrgHnfk0KFPwMed3UyiDOMKx6g+uiNJdYBFbnm7D\ny6G9BM3IQGiMeSot7YMz9dHNrswp846p7BfZ+oKPnPMn8GWZqrhKq+xIT+/U/CXl\nlV9Sk7uGO03N+xjg9VBZ3m6+bQKBgQDGL13bka9kSd2em+g52bbJghEgxO+F7zJP\nMyiXmGuEYvApQXHAR1dN2/48YBPyjYoS4RwTKsT4TFehQxTQOsV11Zl5MF9dmtYJ\nKl2ThIxkfB1Sd66cvAW2i3aUvP3zLgHFPza3rfy1VGXWIDEajdQpDhZSR5z1vgEr\nA2Y+YNsD0wKBgQDB945d5wyj+MuPGWwFvfRzSXZwMaZdrf4GAHM6nYGgicyFBfVM\n8Ee+ZM5TWu2PRG6292STA0pDr6KJo5TfDce7gG42+D699srAFTCGYpM+KrurZ31E\nFgYlDImVy6Ngf7Of5CFQZkI8kYNthtUBH6LnBRivAGgrRAWzD01wRme3SQKBgQCM\nRzK0G+Z9ohsQDWLOr9/Fuh7NhxfTQX/mt1tgEi7oQe+kBAK0Csss/S5zJb54zN6S\nhmwM1RUee0r7hjPRQAhMY6iCM08NPm+JhUc/B28oIQHQQY6CukOA9i41EaDe5A20\nxA3C7ZAV+WsrqRWYqhMmDCaYjr/24UqKW5mfRYdAqQKBgB3t36c3Iirno82hLajA\naa3Rdq9DSXmshr2e34VoCoIcCvO0ErR8bBPVab+QirFxiWX7bhY5QrYciLc5/F8W\nmYAlggZjEDIcXvNHeB6f+EUcW9Or/CJiMlOcFh3vBGBwKU6pBH9zPZLKSCp3ctYx\ngpIYVGM32DOtStNVF+IEmXyB\n-----END PRIVATE KEY-----

# 支付宝公钥
ALIPAY_PUBLIC_KEY=-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkAazBMmylpczF95Cnst378nberUZuTFLdx549S/0JFRU7wqfAn91/HIrxJZUJpiKC3xvrkiz4vA4+RSDAGpP5KTyPmfQRuphtn38tquQp08a57l+osx9L0121r5VESwIbDhsmvFsJ9PMr0U7xuVrZk3jDSyTLHaW0kAeh7l8hFumLnK7eFQsiFEOLWnK+0Y02xpuiY9bzCvuR54bFfrtGlYt2yHVG77DWTSj8RjXY0ZziQ19N83yUoKzNjW5FbmO+ZqVDbMLdvAxw8h3+XdMMqOxAAZBl+n7zq/u41USDZZZ3s+KmR6iz7aqQI3ds+4SGh5+PaSaslkApAagPJIXtQIDAQAB\n-----END PUBLIC KEY-----

# 接口内容加密密钥（如果启用了加密）
ALIPAY_ENCRYPT_KEY=lE4SjgSUUKi5tmrfKB3A3g==

# 回调地址（根据实际部署域名修改）
ALIPAY_NOTIFY_URL=https://zuhaoku.zeabur.app/api/v1/payments/alipay/notify
ALIPAY_RETURN_URL=https://your-frontend-domain.com/tenant/orders
```

### 3. 测试支付流程

#### 3.1 本地测试

1. 启动应用：
   ```bash
   cd backend/zhk-monolith/zhk-user
   mvn spring-boot:run
   ```

2. 检查日志，应该看到：
   ```
   支付宝客户端初始化成功: appId=2021006112616763, gatewayUrl=https://openapi.alipay.com/gateway.do
   ```

3. 创建测试订单并调用支付接口

#### 3.2 沙箱环境测试（推荐）

在正式上线前，使用沙箱环境测试：

1. 修改网关地址为沙箱环境：
   ```yaml
   gateway-url: https://openapi.alipaydev.com/gateway.do
   ```

2. 使用沙箱账号进行测试支付

3. 测试完成后，改回生产环境网关

## 🔍 验证配置

### 1. 检查应用启动日志

启动应用后，查看日志中是否有：
```
支付宝客户端初始化成功: appId=2021006112616763, gatewayUrl=https://openapi.alipay.com/gateway.do
```

如果没有看到此日志，检查：
- 环境变量是否正确配置
- 私钥和公钥格式是否正确
- AppID 是否正确

### 2. 测试支付接口

```bash
# 创建支付
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "orderId": 1,
    "paymentType": "alipay"
  }'
```

### 3. 验证回调

支付完成后，检查：
- 支付宝是否发送了异步通知
- 订单状态是否正确更新
- 支付记录是否正确创建

## ⚠️ 重要提示

1. **应用网关配置**: 必须在支付宝开放平台配置应用网关，否则无法接收回调
2. **HTTPS**: 生产环境必须使用 HTTPS
3. **密钥安全**: 不要将私钥提交到代码仓库
4. **测试**: 建议先在沙箱环境测试

## 📚 相关文档

- 详细配置指南: `ALIPAY_SETUP.md`
- 快速开始: `docs/ALIPAY_QUICK_START.md`
- 支付宝开放平台: https://open.alipay.com/
- 电脑网站支付文档: https://opendocs.alipay.com/open/270/105898

---

**状态**: ✅ 代码配置已完成，待完成支付宝开放平台配置和 Zeabur 环境变量配置

