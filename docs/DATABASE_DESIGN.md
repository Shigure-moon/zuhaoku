# 数据库设计文档

## 文档信息
- **版本**: v1.0
- **最后更新**: 2025/11/18
- **维护者**: shigure

---

## 一、数据库概述

### 1.1 数据库选型
- **主数据库**: MySQL 8.0+
- **字符集**: utf8mb4
- **排序规则**: utf8mb4_unicode_ci
- **存储引擎**: InnoDB

### 1.2 设计原则
- 遵循第三范式（3NF）
- 预留扩展字段
- 合理使用索引
- 敏感数据加密存储

---

## 二、核心表结构

### 2.1 用户表 (user)

```sql
CREATE TABLE user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nickname VARCHAR(50) NOT NULL COMMENT '昵称',
  mobile VARCHAR(11) UNIQUE NOT NULL COMMENT '手机号',
  idcard_hash VARCHAR(64) COMMENT '身份证 SHA-256 哈希',
  zhima_score INT COMMENT '芝麻信用分',
  avatar_url VARCHAR(255) COMMENT '头像URL',
  status TINYINT DEFAULT 1 COMMENT '1-正常 2-冻结',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_mobile (mobile),
  INDEX idx_zhima_score (zhima_score),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

**字段说明**:
- `idcard_hash`: 身份证号 SHA-256 哈希值，用于去重和风控，不存储明文
- `zhima_score`: 芝麻信用分，用于免押评估
- `status`: 用户状态，1-正常，2-冻结

### 2.2 游戏表 (game)

```sql
CREATE TABLE game (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL COMMENT '游戏名称',
  publisher VARCHAR(50) COMMENT '发行商',
  login_type ENUM('qr', 'pwd', 'token') NOT NULL COMMENT '登录方式：qr-二维码 pwd-密码 token-令牌',
  icon_url VARCHAR(255) COMMENT '游戏图标URL',
  status TINYINT DEFAULT 1 COMMENT '1-启用 0-禁用',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='游戏表';
```

### 2.3 账号表 (account)

```sql
CREATE TABLE account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  game_id INT NOT NULL COMMENT '游戏ID',
  owner_uid BIGINT NOT NULL COMMENT '号主用户ID',
  username_enc TEXT NOT NULL COMMENT '账号名 AES-256-GCM 加密',
  pwd_enc TEXT NOT NULL COMMENT '密码 AES-256-GCM 加密',
  iv VARCHAR(32) NOT NULL COMMENT '初始化向量',
  lvl INT COMMENT '账号等级',
  skins JSON COMMENT '皮肤信息（JSON格式）',
  deposit DECIMAL(10,2) NOT NULL COMMENT '固定押金',
  price_30min DECIMAL(10,2) NOT NULL COMMENT '30分钟价格',
  price_1h DECIMAL(10,2) NOT NULL COMMENT '1小时价格',
  price_overnight DECIMAL(10,2) NOT NULL COMMENT '包夜价格',
  status TINYINT DEFAULT 1 COMMENT '1-上架 2-下架 3-租赁中',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (game_id) REFERENCES game(id),
  FOREIGN KEY (owner_uid) REFERENCES user(id),
  INDEX idx_game_status (game_id, status),
  INDEX idx_owner (owner_uid),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表';
```

**安全设计**:
- `username_enc`, `pwd_enc`: 使用 AES-256-GCM 加密，每账号独立 IV
- `iv`: 初始化向量，用于解密

### 2.4 租赁订单表 (lease_order)

```sql
CREATE TABLE lease_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_id BIGINT NOT NULL COMMENT '账号ID',
  tenant_uid BIGINT NOT NULL COMMENT '租客用户ID',
  start_time TIMESTAMP NOT NULL COMMENT '租期开始时间',
  end_time TIMESTAMP NOT NULL COMMENT '租期结束时间',
  actual_end_time TIMESTAMP NULL COMMENT '实际结束时间',
  amount DECIMAL(10,2) NOT NULL COMMENT '租金',
  deposit DECIMAL(10,2) NOT NULL COMMENT '押金',
  status ENUM('paying', 'leasing', 'closed', 'appeal', 'cancelled') DEFAULT 'paying' COMMENT '订单状态',
  evidence_hash VARCHAR(64) COMMENT '还号证据哈希（SHA-256）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (account_id) REFERENCES account(id),
  FOREIGN KEY (tenant_uid) REFERENCES user(id),
  INDEX idx_tenant_status (tenant_uid, status),
  INDEX idx_account_time (account_id, start_time, end_time),
  INDEX idx_status_time (status, end_time) COMMENT '用于定时任务查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租赁订单表';
```

**状态说明**:
- `paying`: 待支付
- `leasing`: 租赁中
- `closed`: 已完成
- `appeal`: 申诉中
- `cancelled`: 已取消

### 2.5 支付记录表 (payment_record)

```sql
CREATE TABLE payment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL COMMENT '订单ID',
  payment_type ENUM('wechat', 'alipay') NOT NULL COMMENT '支付方式',
  amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  transaction_id VARCHAR(64) UNIQUE COMMENT '第三方交易号',
  status ENUM('pending', 'success', 'failed', 'refunded') DEFAULT 'pending' COMMENT '支付状态',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL COMMENT '支付完成时间',
  FOREIGN KEY (order_id) REFERENCES lease_order(id),
  INDEX idx_transaction_id (transaction_id),
  INDEX idx_status_time (status, created_at),
  INDEX idx_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付记录表';
```

### 2.6 申诉表 (appeal)

```sql
CREATE TABLE appeal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL COMMENT '订单ID',
  type TINYINT NOT NULL COMMENT '申诉类型：1-账号异常 2-押金争议 3-其他',
  evidence_urls JSON COMMENT '证据URL列表（JSON数组）',
  verdict TINYINT COMMENT '裁决结果：1-支持租客 2-支持号主 3-各担一半',
  operator_uid BIGINT COMMENT '处理人用户ID',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolve_time TIMESTAMP NULL COMMENT '处理完成时间',
  FOREIGN KEY (order_id) REFERENCES lease_order(id),
  INDEX idx_order_id (order_id),
  INDEX idx_status (verdict)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='申诉表';
```

---

## 三、索引设计

### 3.1 主键索引
所有表使用 `id` 作为主键，自增 BIGINT 类型。

### 3.2 唯一索引
- `user.mobile`: 手机号唯一索引
- `payment_record.transaction_id`: 第三方交易号唯一索引

### 3.3 普通索引
- `user.zhima_score`: 用于信用分查询
- `account.game_id, status`: 用于查询可用账号
- `lease_order.tenant_uid, status`: 用于查询用户订单
- `lease_order.status, end_time`: 用于定时任务查询到期订单

### 3.4 复合索引
- `idx_game_status`: (game_id, status) - 查询某游戏的可用账号
- `idx_tenant_status`: (tenant_uid, status) - 查询用户的特定状态订单
- `idx_account_time`: (account_id, start_time, end_time) - 查询账号的租赁时间

---

## 四、数据安全

### 4.1 敏感数据加密
- **账号密码**: AES-256-GCM 加密，每账号独立 IV
- **身份证号**: 仅存储 SHA-256 哈希值

### 4.2 数据脱敏
- 身份证号不存储明文，仅存储哈希值用于去重和风控
- 日志中不输出敏感信息

### 4.3 数据备份
- **主从复制**: MySQL 主从架构，实时同步
- **全量备份**: 每日凌晨执行全量备份
- **增量备份**: 每小时执行增量备份
- **备份加密**: 备份文件使用 AES-256 加密

### 4.4 数据归档
- 超过 1 年的订单数据归档到冷存储
- 归档数据保留 5 年

---

## 五、数据库配置

### 5.1 连接池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 5.2 主从配置

**主库配置** (写操作):
```yaml
spring:
  datasource:
    master:
      url: jdbc:mysql://mysql-master:3306/zhk_rental
      username: root
      password: ${DB_PASSWORD}
```

**从库配置** (读操作):
```yaml
spring:
  datasource:
    slave:
      url: jdbc:mysql://mysql-slave:3306/zhk_rental
      username: root
      password: ${DB_PASSWORD}
```

### 5.3 分库分表策略

当日订单量 > 10万时，考虑分库分表：
- **分片键**: user_id
- **分片策略**: 按 user_id 取模
- **分片数量**: 初始 4 个库，每个库 4 张表（共 16 张表）

---

## 六、数据迁移

### 6.1 版本管理
使用 Flyway 或 Liquibase 进行数据库版本管理。

### 6.2 迁移脚本示例

```sql
-- V1__Create_user_table.sql
CREATE TABLE user (
  -- 表结构
);

-- V2__Add_zhima_score_to_user.sql
ALTER TABLE user ADD COLUMN zhima_score INT COMMENT '芝麻信用分';
```

---

## 七、性能优化

### 7.1 查询优化
- 避免 SELECT *
- 使用覆盖索引
- 合理使用 JOIN
- 避免子查询嵌套过深

### 7.2 慢查询监控
- 开启慢查询日志
- 设置 `long_query_time = 1`（超过1秒记录）
- 定期分析慢查询日志

### 7.3 表分区
对于大表（如 `lease_order`），考虑按时间分区：
```sql
ALTER TABLE lease_order 
PARTITION BY RANGE (YEAR(created_at)) (
  PARTITION p2024 VALUES LESS THAN (2025),
  PARTITION p2025 VALUES LESS THAN (2026),
  PARTITION p_future VALUES LESS THAN MAXVALUE
);
```

---

## 八、参考资源

- [MySQL 8.0 官方文档](https://dev.mysql.com/doc/refman/8.0/en/)
- [数据库设计最佳实践](https://www.postgresql.org/docs/current/ddl.html)

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

