# API 接口设计文档

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、API 设计原则

### 1.1 RESTful 规范
- 使用标准 HTTP 方法（GET, POST, PUT, DELETE, PATCH）
- 资源使用名词，操作使用动词
- URL 层次清晰，语义明确

### 1.2 版本控制
- API 版本通过 URL 路径控制：`/api/v1/`, `/api/v2/`
- 向后兼容，新版本不破坏旧版本

### 1.3 统一响应格式
- 所有接口返回统一格式
- 包含 code, message, data, timestamp

---

## 二、响应格式

### 2.1 成功响应

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    // 业务数据
  },
  "timestamp": 1703123456789
}
```

### 2.2 错误响应

```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null,
  "timestamp": 1703123456789
}
```

### 2.3 错误码规范

| 错误码 | 说明 | HTTP 状态码 |
|--------|------|-------------|
| 200 | 成功 | 200 |
| 400 | 请求参数错误 | 400 |
| 401 | 未授权（Token 无效） | 401 |
| 403 | 无权限 | 403 |
| 404 | 资源不存在 | 404 |
| 500 | 服务器内部错误 | 500 |

---

## 三、认证授权

### 3.1 JWT Token
- **请求头**: `Authorization: Bearer <token>`
- **Token 格式**: JWT
- **过期时间**: 24 小时

### 3.2 权限控制
- 使用 Spring Security `@PreAuthorize` 注解
- 角色：TENANT（租客）、OWNER（商家）、OPERATOR（运营）

---

## 四、核心接口

### 4.1 用户相关接口

#### 用户注册
```
POST /api/v1/users/register
Content-Type: application/json

Request:
{
  "mobile": "13800138000",
  "password": "password123",
  "nickname": "用户昵称",
  "verifyCode": "123456"
}

Response:
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "userId": 1,
    "nickname": "用户昵称"
  }
}
```

#### 用户登录
```
POST /api/v1/users/login
Content-Type: application/json

Request:
{
  "mobile": "13800138000",
  "password": "password123"
}

Response:
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "userInfo": {
      "userId": 1,
      "nickname": "用户昵称",
      "role": "TENANT"
    }
  }
}
```

#### 获取当前用户信息
```
GET /api/v1/users/me
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "userId": 1,
    "nickname": "用户昵称",
    "mobile": "138****8000",
    "role": "TENANT",
    "zhimaScore": 650
  }
}
```

### 4.2 账号相关接口

#### 查询账号列表
```
GET /api/v1/accounts?gameId=1&status=1&page=1&size=10
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      {
        "accountId": 1,
        "gameName": "英雄联盟",
        "level": 30,
        "skins": ["皮肤1", "皮肤2"],
        "deposit": 100.00,
        "price30min": 5.00,
        "price1h": 8.00,
        "priceOvernight": 20.00
      }
    ],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

#### 发布账号
```
POST /api/v1/accounts
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "gameId": 1,
  "username": "account123",
  "password": "password123",
  "level": 30,
  "skins": ["皮肤1", "皮肤2"],
  "deposit": 100.00,
  "price30min": 5.00,
  "price1h": 8.00,
  "priceOvernight": 20.00
}

Response:
{
  "code": 200,
  "message": "发布成功",
  "data": {
    "accountId": 1
  }
}
```

### 4.3 订单相关接口

#### 创建订单
```
POST /api/v1/orders
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "accountId": 1,
  "duration": 30,
  "durationType": "MINUTE"
}

Response:
{
  "code": 200,
  "message": "订单创建成功",
  "data": {
    "orderId": 1,
    "amount": 5.00,
    "deposit": 100.00,
    "paymentUrl": "https://pay.zuhaoku.com/order/1"
  }
}
```

#### 查询订单列表
```
GET /api/v1/orders?status=leasing&page=1&size=10
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "list": [
      {
        "orderId": 1,
        "accountId": 1,
        "gameName": "英雄联盟",
        "startTime": "2024-01-01 10:00:00",
        "endTime": "2024-01-01 10:30:00",
        "amount": 5.00,
        "deposit": 100.00,
        "status": "leasing"
      }
    ],
    "total": 10,
    "page": 1,
    "size": 10
  }
}
```

#### 续租
```
POST /api/v1/orders/{orderId}/renew
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "duration": 30,
  "durationType": "MINUTE"
}

Response:
{
  "code": 200,
  "message": "续租成功",
  "data": {
    "orderId": 1,
    "newEndTime": "2024-01-01 11:00:00"
  }
}
```

#### 还号
```
POST /api/v1/orders/{orderId}/return
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "还号成功",
  "data": null
}
```

### 4.4 支付相关接口

#### 创建支付
```
POST /api/v1/payments
Authorization: Bearer <token>
Content-Type: application/json

Request:
{
  "orderId": 1,
  "paymentType": "alipay"
}

Response:
{
  "code": 200,
  "message": "支付创建成功",
  "data": {
    "paymentId": 1,
    "paymentUrl": "https://openapi.alipay.com/...",
    "qrCode": "data:image/png;base64,..."
  }
}
```

#### 查询支付状态
```
GET /api/v1/payments/{paymentId}/status
Authorization: Bearer <token>

Response:
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "paymentId": 1,
    "status": "success",
    "paidAt": "2024-01-01 10:05:00"
  }
}
```

---

## 五、接口限流

### 5.1 限流规则
- **登录接口**: 5次/分钟
- **下单接口**: 10次/分钟
- **支付接口**: 20次/分钟
- **其他接口**: 100次/分钟

### 5.2 限流响应
```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后重试",
  "data": null,
  "timestamp": 1703123456789
}
```

---

## 六、API 文档

### 6.1 Swagger/OpenAPI
- 访问地址: `http://localhost:8080/swagger-ui.html`
- API 文档自动生成
- 支持在线测试

### 6.2 Postman Collection
- 提供 Postman Collection 文件
- 包含所有接口示例
- 支持环境变量配置

---

## 七、参考资源

- [RESTful API 设计指南](https://restfulapi.net/)
- [OpenAPI 规范](https://swagger.io/specification/)

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

