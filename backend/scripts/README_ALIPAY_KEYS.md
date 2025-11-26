# 支付宝接口密钥生成说明

## 快速开始

运行密钥生成脚本：

```bash
cd backend/scripts
./generate-alipay-keys.sh
```

## 生成的文件

脚本会在 `backend/scripts/alipay-keys/` 目录下生成以下文件：

1. **alipay_app_private_key.pem** - RSA私钥（PKCS#1格式，保留备份）
2. **alipay_app_private_key_pkcs8.pem** - RSA私钥（PKCS#8格式，用于配置）
3. **alipay_app_public_key.pem** - 应用公钥（X.509格式）
4. **alipay_app_csr.pem** - 证书签名请求（可选）
5. **alipay_app_csr.conf** - CSR配置文件

## 密钥格式说明

### 应用私钥（PKCS#8格式）

支付宝要求使用 PKCS#8 格式的私钥，这是 RFC5208 标准定义的格式。

**文件格式：**
```
-----BEGIN PRIVATE KEY-----
...（Base64编码的私钥内容）...
-----END PRIVATE KEY-----
```

### 应用公钥（X.509格式）

公钥使用 X.509 格式，符合 RFC5280 标准。

**文件格式：**
```
-----BEGIN PUBLIC KEY-----
...（Base64编码的公钥内容）...
-----END PUBLIC KEY-----
```

**上传到支付宝时：**
- 需要去除 `-----BEGIN PUBLIC KEY-----` 和 `-----END PUBLIC KEY-----`
- 去除所有换行符
- 只保留Base64编码的字符串

## 配置步骤

### 1. 在支付宝开放平台配置

1. 登录 [支付宝开放平台](https://open.alipay.com)
2. 进入 **应用管理** -> 选择应用 -> **接口加签方式**
3. 选择 **密钥** 加签方式
4. 加签算法选择 **RSA2**
5. 上传 **应用公钥字符串**（脚本会自动提取并显示）

### 2. 在应用代码中配置

将 `alipay_app_private_key_pkcs8.pem` 文件内容配置到应用配置中：

**方式1：配置文件（开发环境）**

```yaml
# application-dev.yml
zhk:
  alipay:
    app-id: YOUR_APP_ID
    private-key: |
      -----BEGIN PRIVATE KEY-----
      ...（私钥内容）...
      -----END PRIVATE KEY-----
    public-key: |
      -----BEGIN PUBLIC KEY-----
      ...（公钥内容）...
      -----END PUBLIC KEY-----
    gateway-url: https://openapi.alipay.com/gateway.do
```

**方式2：环境变量（生产环境推荐）**

```bash
export ALIPAY_APP_ID=YOUR_APP_ID
export ALIPAY_PRIVATE_KEY="-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
export ALIPAY_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\n...\n-----END PUBLIC KEY-----"
```

**方式3：密钥管理服务（生产环境最佳实践）**

- 使用阿里云KMS
- 使用AWS Secrets Manager
- 使用HashiCorp Vault

## 安全注意事项

⚠️ **重要安全提示：**

1. **私钥保密**
   - 私钥文件绝对不能泄露
   - 不要将私钥提交到代码仓库
   - 建议添加到 `.gitignore`

2. **文件权限**
   ```bash
   chmod 600 alipay_app_private_key_pkcs8.pem
   ```

3. **备份**
   - 将私钥备份到安全位置
   - 建议使用加密存储

4. **生产环境**
   - 不要使用配置文件存储私钥
   - 使用环境变量或密钥管理服务
   - 定期轮换密钥

## 验证密钥

可以使用以下命令验证密钥：

```bash
# 验证私钥
openssl rsa -in alipay_app_private_key_pkcs8.pem -check -noout

# 验证公钥
openssl rsa -pubin -in alipay_app_public_key.pem -text -noout

# 验证私钥和公钥是否匹配
openssl rsa -in alipay_app_private_key_pkcs8.pem -pubout | \
  diff - alipay_app_public_key.pem
```

## 参考文档

- [支付宝开放平台 - 密钥与证书格式说明](https://opendocs.alipay.com/common/02kkv7)
- [支付宝开放平台 - 接口加签方式说明](https://opendocs.alipay.com/common/02kkv6)
- [RFC5208 - PKCS#8 Private-Key Information Syntax](https://tools.ietf.org/html/rfc5208)
- [RFC5280 - X.509 Public-Key Infrastructure](https://tools.ietf.org/html/rfc5280)

## 常见问题

### Q: 密钥长度应该选择多少？

A: 支付宝推荐使用 2048 位 RSA 密钥，脚本默认使用 2048 位。

### Q: 可以使用其他工具生成密钥吗？

A: 可以，只要符合以下要求：
- 私钥：PKCS#8 格式，RSA2 算法
- 公钥：X.509 格式
- 密钥长度：2048 位

### Q: 如何更新密钥？

A: 
1. 生成新密钥
2. 在支付宝开放平台更新应用公钥
3. 在应用配置中更新私钥
4. 验证功能正常后，删除旧密钥

### Q: 私钥丢失怎么办？

A: 
- 如果私钥丢失，无法恢复
- 需要在支付宝开放平台重新生成并上传新的应用公钥
- 更新应用配置中的私钥

---

**维护者**: shigure  
**最后更新**: 2025/11/20

