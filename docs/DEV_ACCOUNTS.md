# 开发者测试账号

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、测试账号列表

### 1.1 账号信息

| 角色 | 手机号 | 密码 | 昵称 | 芝麻信用分 |
|------|--------|------|------|-----------|
| 租客 | 13800000001 | dev123456 | 测试租客 | 650 |
| 商家 | 13800000002 | dev123456 | 测试商家 | 700 |
| 运营 | 13800000003 | dev123456 | 测试运营 | - |

### 1.2 账号说明

- **密码**: 所有测试账号密码统一为 `dev123456`
- **用途**: 仅用于开发环境测试
- **安全**: 请勿在生产环境使用这些账号

---

## 二、创建测试账号

### 2.1 使用脚本创建（推荐）

```bash
cd /home/shigure/zuhaoku
./backend/scripts/create-dev-account.sh
```

### 2.2 手动执行 SQL

```bash
# 进入 MySQL 容器
docker exec -it zhk-mysql-dev mysql -uroot -proot123456 zhk_rental

# 执行 SQL 脚本
source /docker-entrypoint-initdb.d/dev-accounts.sql

# 或直接执行
docker exec -i zhk-mysql-dev mysql -uroot -proot123456 zhk_rental < backend/scripts/dev-accounts.sql
```

### 2.3 验证账号

```bash
# 查询测试账号
docker exec -it zhk-mysql-dev mysql -uroot -proot123456 zhk_rental -e "
SELECT id, nickname, mobile, role, status, zhima_score 
FROM user 
WHERE mobile IN ('13800000001', '13800000002', '13800000003')
ORDER BY role;
"
```

---

## 三、使用测试账号

### 3.1 前端登录

1. 访问前端应用：`http://localhost:3000`
2. 点击"登录"
3. 输入手机号和密码：
   - **租客**: `13800000001` / `dev123456`
   - **商家**: `13800000002` / `dev123456`
   - **运营**: `13800000003` / `dev123456`

### 3.2 API 测试

使用 Postman 或 curl 测试 API：

```bash
# 登录获取 Token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "13800000001",
    "password": "dev123456"
  }'

# 使用 Token 访问受保护接口
curl -X GET http://localhost:8080/api/v1/user/info \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 四、重置测试账号

如果需要重置测试账号：

```bash
# 删除并重新创建
./backend/scripts/create-dev-account.sh
```

---

## 五、注意事项

1. **仅开发环境使用**: 这些账号仅用于开发测试，请勿在生产环境使用
2. **密码安全**: 测试账号密码为弱密码，不适合生产环境
3. **数据清理**: 定期清理测试数据，避免影响开发
4. **账号管理**: 如需更多测试账号，可修改 `dev-accounts.sql` 脚本

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

