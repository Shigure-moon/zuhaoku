# 支付宝支付调试指南

## 问题：支付时返回测试支付页面

如果支付时返回的是测试支付页面（`/pay/{paymentId}`）而不是真实的支付宝支付页面，请按照以下步骤排查：

## 一、检查应用启动日志

### 1.1 查看支付宝客户端初始化日志

启动应用后，检查日志中是否有：

```
✅ 支付宝客户端初始化成功: appId=2021006112616763, gatewayUrl=https://openapi.alipaydev.com/gateway.do
```

**如果没有看到此日志**，说明支付宝客户端未初始化，可能的原因：

1. **配置未加载**
   - 检查 `application-dev.yml` 中是否有 `zhk.alipay` 配置
   - 检查配置属性名称是否正确（`app-id` 对应 `appId`）

2. **条件注解未生效**
   - `AlipayClientConfig` 使用了 `@ConditionalOnProperty(prefix = "zhk.alipay", name = "app-id")`
   - 确保配置中有 `zhk.alipay.app-id` 属性

### 1.2 检查配置错误日志

如果看到以下错误日志，说明配置有问题：

```
❌ 支付宝 AppID 未配置！请检查配置：zhk.alipay.app-id
❌ 支付宝私钥未配置！请检查配置：zhk.alipay.private-key
❌ 支付宝公钥未配置！请检查配置：zhk.alipay.alipay-public-key
```

## 二、验证配置文件

### 2.1 检查配置文件位置

确保配置文件在正确的位置：
- 开发环境：`backend/zhk-monolith/zhk-user/src/main/resources/application-dev.yml`
- 生产环境：`backend/zhk-monolith/zhk-user/src/main/resources/application-prod.yml`

### 2.2 检查配置格式

```yaml
zhk:
  alipay:
    app-id: 2021006112616763  # ✅ 正确
    # appId: 2021006112616763  # ❌ 错误（Spring Boot 使用 kebab-case）
    
    private-key: |
      -----BEGIN PRIVATE KEY-----
      ...（私钥内容）...
      -----END PRIVATE KEY-----
    
    alipay-public-key: |
      -----BEGIN PUBLIC KEY-----
      ...（公钥内容）...
      -----END PUBLIC KEY-----
```

### 2.3 验证密钥格式

1. **私钥格式**：
   - 必须包含 `-----BEGIN PRIVATE KEY-----` 和 `-----END PRIVATE KEY-----`
   - 必须是 PKCS#8 格式
   - 每行长度不超过 64 个字符

2. **公钥格式**：
   - 必须包含 `-----BEGIN PUBLIC KEY-----` 和 `-----END PUBLIC KEY-----`
   - 必须是从支付宝开放平台下载的支付宝公钥

## 三、测试支付接口

### 3.1 创建支付请求

```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{
    "orderId": 1,
    "paymentType": "alipay"
  }'
```

### 3.2 检查返回结果

**正常情况**（真实支付宝支付）：
```json
{
  "code": 200,
  "message": "支付创建成功",
  "data": {
    "id": 1,
    "paymentUrl": "<form name='punchout_form' method='post' action='https://openapi.alipaydev.com/gateway.do?charset=utf-8&method=alipay.trade.page.pay&sign=...'>...</form>"
  }
}
```

**异常情况**（配置错误）：
```json
{
  "code": 500,
  "message": "支付宝支付服务未配置，请联系管理员"
}
```

## 四、常见错误及解决方案

### 错误1：支付宝客户端未配置

**日志**：
```
支付宝客户端未配置，使用测试支付页面
```

**原因**：
- `AlipayClient` Bean 未创建
- 配置属性 `zhk.alipay.app-id` 不存在

**解决**：
1. 检查 `application-dev.yml` 中是否有 `zhk.alipay.app-id` 配置
2. 确认配置属性名称正确（使用 `app-id` 而不是 `appId`）

### 错误2：支付宝配置未完成

**日志**：
```
支付宝配置未完成，使用测试支付页面
```

**原因**：
- `AlipayProperties` 未正确加载
- AppID 为空

**解决**：
1. 检查配置文件中的 `app-id` 是否有值
2. 确认配置文件被正确加载（检查 `spring.profiles.active`）

### 错误3：调用支付宝接口失败

**日志**：
```
调用支付宝接口失败: ...
支付宝支付创建失败: code=40004, msg=Business Failed, subCode=isv.invalid-app-id, subMsg=无效的AppID参数
```

**原因**：
- AppID 不正确
- 网关地址与 AppID 不匹配（生产环境 AppID 不能使用沙箱网关）

**解决**：
1. 确认 AppID 正确
2. 确认网关地址与 AppID 匹配：
   - 沙箱环境：`https://openapi.alipaydev.com/gateway.do`
   - 生产环境：`https://openapi.alipay.com/gateway.do`

### 错误4：签名验证失败

**日志**：
```
调用支付宝接口失败: code=40002, msg=Invalid Arguments, subCode=isv.invalid-sign, subMsg=签名错误
```

**原因**：
- 私钥不正确
- 私钥格式错误
- 私钥与 AppID 不匹配

**解决**：
1. 检查私钥是否正确（从 `alipay_app_private_key_pkcs8.pem` 复制）
2. 确认私钥格式正确（包含 BEGIN/END 标记）
3. 确认私钥与应用公钥匹配

## 五、调试步骤

### 步骤1：验证配置加载

在 `AlipayClientConfig` 中添加日志：

```java
@PostConstruct
public void validateConfig() {
    log.info("支付宝配置验证: appId={}, gatewayUrl={}, hasPrivateKey={}, hasPublicKey={}", 
            alipayProperties.getAppId(),
            alipayProperties.getGatewayUrl(),
            alipayProperties.getPrivateKey() != null && !alipayProperties.getPrivateKey().isEmpty(),
            alipayProperties.getAlipayPublicKey() != null && !alipayProperties.getAlipayPublicKey().isEmpty());
}
```

### 步骤2：测试支付宝客户端

创建一个测试接口：

```java
@GetMapping("/test/alipay")
public Result<String> testAlipay() {
    if (alipayClient == null) {
        return Result.error("支付宝客户端未初始化");
    }
    if (alipayProperties == null) {
        return Result.error("支付宝配置未加载");
    }
    return Result.success("支付宝配置正常: appId=" + alipayProperties.getAppId());
}
```

### 步骤3：查看详细日志

在 `application-dev.yml` 中启用详细日志：

```yaml
logging:
  level:
    com.zhk.order.service.impl.AlipayPaymentServiceImpl: DEBUG
    com.zhk.order.config.AlipayClientConfig: DEBUG
```

## 六、验证清单

- [ ] 应用启动日志中有 "支付宝客户端初始化成功"
- [ ] 配置文件中 `zhk.alipay.app-id` 有值
- [ ] 配置文件中 `zhk.alipay.private-key` 有值且格式正确
- [ ] 配置文件中 `zhk.alipay.alipay-public-key` 有值且格式正确
- [ ] 网关地址与 AppID 匹配（沙箱/生产）
- [ ] 调用支付接口返回真实的支付宝支付表单 HTML
- [ ] 支付表单 HTML 中包含 `action='https://openapi.alipay...'`

## 七、快速修复

如果确认配置正确但仍然返回测试支付页面，检查：

1. **清除缓存并重新编译**：
   ```bash
   mvn clean compile
   ```

2. **重启应用**：
   ```bash
   # 停止应用
   # 重新启动
   mvn spring-boot:run
   ```

3. **检查是否有多个配置文件**：
   - 确保只加载一个配置文件
   - 检查 `spring.profiles.active` 设置

---

**提示**：修复后，支付接口应该返回真实的支付宝支付表单 HTML，而不是测试支付页面 URL。

