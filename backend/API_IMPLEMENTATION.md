# 后端 API 接口实现总结

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、已实现的接口

### 1.1 用户相关接口 (`/api/v1/users`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/users/register` | 用户注册 | 否 |
| POST | `/api/v1/users/login` | 用户登录 | 否 |
| GET | `/api/v1/users/me` | 获取当前用户信息 | 是 |
| GET | `/api/v1/users/{userId}` | 根据ID获取用户信息 | 是 |

**实现状态**: ✅ 已完成

---

### 1.2 游戏相关接口 (`/api/v1/games`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/v1/games` | 获取游戏列表 | 否 |

**实现状态**: ✅ 已完成

---

### 1.3 账号相关接口 (`/api/v1/accounts`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | `/api/v1/accounts` | 查询账号列表（公开） | 否 |
| GET | `/api/v1/accounts/{id}` | 获取账号详情 | 否 |
| GET | `/api/v1/accounts/my` | 获取我的账号列表（商家） | 是 |
| POST | `/api/v1/accounts` | 创建账号（商家） | 是 |
| PUT | `/api/v1/accounts/{id}` | 更新账号（商家） | 是 |
| DELETE | `/api/v1/accounts/{id}` | 删除账号（商家） | 是 |
| PATCH | `/api/v1/accounts/{id}/status` | 上架/下架账号（商家） | 是 |

**实现状态**: ✅ 已完成

---

### 1.4 订单相关接口 (`/api/v1/orders`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/orders` | 创建订单 | 是 |
| GET | `/api/v1/orders` | 查询订单列表 | 是 |
| GET | `/api/v1/orders/{id}` | 获取订单详情 | 是 |
| POST | `/api/v1/orders/{id}/renew` | 续租 | 是 |
| POST | `/api/v1/orders/{id}/return` | 还号 | 是 |

**实现状态**: ✅ 已完成

---

### 1.5 支付相关接口 (`/api/v1/payments`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/payments` | 创建支付 | 是 |
| GET | `/api/v1/payments/{id}/status` | 查询支付状态 | 是 |

**实现状态**: ✅ 已完成（支付URL和二维码为模拟数据，待对接第三方支付）

---

### 1.6 申诉相关接口 (`/api/v1/appeals`)

| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | `/api/v1/appeals` | 创建申诉 | 是 |
| GET | `/api/v1/appeals` | 查询申诉列表 | 是（管理员） |
| GET | `/api/v1/appeals/{id}` | 获取申诉详情 | 是 |
| POST | `/api/v1/appeals/{id}/resolve` | 处理申诉（管理员） | 是（管理员） |

**实现状态**: ✅ 已完成

---

## 二、模块结构

### 2.1 zhk-user 模块
- **实体类**: User, Account, Game
- **控制器**: UserController, AccountController, GameController
- **服务**: UserService
- **Mapper**: UserMapper, AccountMapper, GameMapper

### 2.2 zhk-order 模块
- **实体类**: LeaseOrder, PaymentRecord, Appeal
- **控制器**: OrderController, PaymentController, AppealController
- **服务**: OrderService, PaymentService, AppealService
- **Mapper**: LeaseOrderMapper, PaymentRecordMapper, AppealMapper

---

## 三、认证机制

### 3.1 JWT Token 验证
- 所有需要认证的接口都在 Controller 中手动验证 JWT token
- Token 从请求头 `Authorization: Bearer <token>` 中获取
- 使用 `JwtUtil.getUserIdFromToken()` 和 `JwtUtil.getRoleFromToken()` 解析用户信息

### 3.2 权限控制
- **租客 (TENANT)**: 可以创建订单、查询自己的订单、续租、还号
- **商家 (OWNER)**: 可以管理自己的账号、查询自己的订单
- **管理员 (OPERATOR)**: 可以处理申诉、管理用户

---

## 四、业务逻辑

### 4.1 订单创建流程
1. 验证账号状态（必须为上架状态）
2. 验证用户权限（不能租赁自己的账号）
3. 根据租期类型计算价格和结束时间
4. 创建订单（状态：paying）
5. 更新账号状态为租赁中（status = 3）

### 4.2 支付流程
1. 验证订单状态（必须为paying）
2. 创建支付记录（状态：pending）
3. 返回支付URL和二维码（模拟数据）
4. TODO: 对接第三方支付接口

### 4.3 还号流程
1. 验证订单状态（必须为leasing）
2. 更新订单状态为closed
3. 更新账号状态为上架（status = 1）

### 4.4 申诉流程
1. 验证订单权限（租客或号主可以申诉）
2. 创建申诉记录
3. 更新订单状态为appeal
4. 管理员处理申诉后，更新订单状态为closed

---

## 五、已完善功能

### 5.1 账号加密 ✅
- ✅ 已实现 AES-256-GCM 加密服务
- ✅ 账号密码使用 AES-256-GCM 加密存储
- ✅ 每个账号使用独立密钥（基于账号ID派生）
- ✅ 实现位置：
  - `zhk-user/src/main/java/com/zhk/user/service/EncryptionService.java`
  - `zhk-order/src/main/java/com/zhk/order/service/EncryptionService.java`

### 5.2 定时任务 ✅
- ✅ 已实现订单到期自动关闭（每分钟检查一次）
- ✅ 已实现账号自动回收
- ✅ 已实现即将到期订单提醒（提前5分钟）
- ✅ 已实现超时未支付订单自动取消（30分钟未支付）
- ✅ 实现位置：`zhk-order/src/main/java/com/zhk/order/service/TimerService.java`

### 5.3 分布式锁 ✅
- ✅ 已实现基于 Redis 的分布式锁
- ✅ 防止重复下单（同一账号同一用户）
- ✅ 实现位置：`zhk-order/src/main/java/com/zhk/order/util/DistributedLock.java`

### 5.4 统一响应格式 ✅
- ✅ Result 类已包含 timestamp 字段
- ✅ 所有接口响应自动包含时间戳

### 5.5 账号标题和描述 ✅
- ✅ 数据库已添加 title 和 description 字段
- ✅ 创建和更新账号时支持设置标题和描述

---

## 六、待完善功能

### 6.1 支付对接
- ⚠️ 当前返回模拟支付URL，需要对接微信/支付宝

### 6.2 API限流
- ⚠️ 需要实现接口级限流（登录5次/分钟，下单10次/分钟等）

### 6.3 Swagger文档
- ⚠️ 需要配置 Swagger/OpenAPI 自动生成 API 文档

### 6.4 验证码功能
- ⚠️ 注册时需要验证码验证

---

## 六、测试账号

| 角色 | 手机号 | 密码 | 说明 |
|------|--------|------|------|
| 租客 | 13800000001 | dev123456 | 用于测试租赁功能 |
| 商家 | 13800000002 | dev123456 | 用于测试账号管理 |
| 管理员 | 13800000003 | dev123456 | 用于测试申诉处理 |

---

## 七、API 文档

### 7.1 接口测试
可以使用 Postman 或 curl 测试接口：

```bash
# 登录获取 token
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13800000002","password":"dev123456"}'

# 使用 token 访问接口
curl -X GET http://localhost:8080/api/v1/accounts/my \
  -H "Authorization: Bearer <token>"
```

### 7.2 Swagger 文档
- 待配置 Swagger/OpenAPI 自动生成 API 文档

---

---

## 八、新增功能说明

### 8.1 加密服务使用

账号创建时自动使用 AES-256-GCM 加密：
```java
// 账号创建时自动加密
account.setUsernameEnc(encryptionService.encrypt(dto.getUsername(), account.getId()));
account.setPwdEnc(encryptionService.encrypt(dto.getPassword(), account.getId()));
```

订单详情查看时自动解密（仅租客可见）：
```java
// 订单详情中自动解密账号密码
vo.setUsername(encryptionService.decrypt(account.getUsernameEnc(), account.getId()));
vo.setPassword(encryptionService.decrypt(account.getPwdEnc(), account.getId()));
```

### 8.2 定时任务说明

定时任务已启用，包括：
- **订单到期检查**：每分钟检查一次，自动关闭到期订单并回收账号
- **即将到期提醒**：提前5分钟提醒用户续租
- **超时订单清理**：每5分钟清理30分钟未支付的订单

### 8.3 分布式锁说明

创建订单时自动使用分布式锁：
```java
// 防止同一用户对同一账号重复下单
String lockKey = "order:create:" + accountId + ":" + userId;
distributedLock.executeWithLock(lockKey, 3, 10, () -> {
    // 创建订单逻辑
});
```

---

**文档维护**: shigure  
**最后更新**: 2025/11/19

