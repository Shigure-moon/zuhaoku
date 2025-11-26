# 租号酷（ZHK-RentalCore）游戏账号分时租借平台 - 架构设计文档

## 文档说明

本文档采用 **C4 模型**（Context, Container, Component, Code）作为架构描述框架，从系统上下文到组件实现逐层细化，确保架构设计的清晰性和可维护性。文档结构遵循软件架构设计最佳实践，参考了领域驱动设计（DDD）、微服务架构模式、分布式系统设计等前沿理论。

### 文档版本
- **版本**: v1.0
- **最后更新**: 2024
- **架构模型**: C4 Model (Simon Brown)
- **设计原则**: 单体先行、渐进式演进、安全优先

### 设计目标

下面给出一个可直接落地的"游戏账号分时租借平台"起名思路 + 端到端实现架构草案，方便后续开发团队快速进入编码与评审阶段。整套方案兼顾：  
1. **低成本起步**（单体 Spring-Boot + MySQL + Redis，快速验证商业模式）  
2. **核心安全**（自动上下号、异地登录拦截、资金担保、沙盒隔离）  
3. **可水平扩展**（预留微服务切分点，日订单 1w → 100w 可平滑演进）
4. **高可用性**（99.9% 可用性目标，支持灰度发布、容灾切换）

--------------------------------------------------
一、品牌与系统命名
1. 中文品牌：租号酷（Zū Hào Kù）  
   口号：想玩就租，酷爽开局！  
2. 英文/域名：  
   - 主域名：zuhaoku.com（cn/com.cn 同步注册）  
   - 系统内部代号：ZHK-RentalCore  
3. 子模块命名规范  
   - zhk-user（用户）  
   - zhk-asset（账号资产）  
   - zhk-order（订单/计时）  
   - zhk-risk（风控）  
   - zhk-wallet（钱包/资金担保）

--------------------------------------------------
二、业务愿景与 MVP 范围
愿景：成为“游戏账号界的 Airbnb”，让 90% 的闲置高价值账号 30 分钟内被租走。  
MVP 必须交付：  
- 支持 3 款热门游戏（如 LOL、王者荣耀、原神）  
- 支持 30 min/1 h/包夜 三种分时粒度  
- 支持微信/支付宝免押（对接芝麻信用 600+）  
- 支持自动上号（Windows 启动器）+ 自动回收  
- 支持平台资金担保（租客先付→平台托管→确认还号后 24h 打款给出租方）

--------------------------------------------------
三、总体架构设计（基于 C4 模型）

### 3.1 系统上下文图（C4 Level 1: System Context）

系统上下文图展示了租号酷平台与外部用户、第三方系统的交互关系。详见 `../diagrams/ZHK_C4_System_Context.puml`。

**主要参与者**：
- **租客（Tenant）**：租赁游戏账号的终端用户
- **商家/号主（Owner）**：出租游戏账号的提供方
- **运营人员（Operator）**：平台运营和管理人员

**外部系统**：
- **微信支付/支付宝**：支付处理和资金托管
- **芝麻信用**：信用评估和免押服务
- **Windows 启动器**：客户端自动上号工具

### 3.2 容器图（C4 Level 2: Container）

容器图描述了系统内部的主要技术容器及其职责划分。详见 `../diagrams/ZHK_C4_Container.puml`。

**架构分层**（单体先行，渐进式演进）：

```
┌─────────────────────────────────────────┐
│  表示层（Presentation Layer）            │
│  ┌───────────────────────────────────┐ │
│  │  Web 前端（Vue 3 + TypeScript）    │ │
│  │  - 租客端、商家端、运营端三端统一   │ │
│  │  - PWA 支持，离线缓存               │ │
│  └───────────────────────────────────┘ │
└─────────────────┬───────────────────────┘
                  │ HTTPS/REST API
┌─────────────────┴───────────────────────┐
│  网关层（Gateway Layer）                 │
│  ┌───────────────────────────────────┐ │
│  │  API 网关（Spring Cloud Gateway） │ │
│  │  - 统一鉴权（JWT + OAuth2）       │ │
│  │  - 限流熔断（Sentinel）            │ │
│  │  - 灰度发布（基于用户标签）        │ │
│  │  - API 版本管理（/v1, /v2）       │ │
│  └───────────────────────────────────┘ │
└─────────────────┬───────────────────────┘
                  │ HTTP（内部调用）
┌─────────────────┴───────────────────────┐
│  业务层（Business Layer）                │
│  ┌───────────────────────────────────┐ │
│  │  业务聚合层（Spring Boot Monolith）│ │
│  │  ├─ zhk-user（用户服务）           │ │
│  │  │  - 注册登录、实名认证           │ │
│  │  │  - 芝麻信用对接                 │ │
│  │  ├─ zhk-asset（资产服务）          │ │
│  │  │  - 账号发布、上下架             │ │
│  │  │  - 账号加密存储（AES-256-GCM）  │ │
│  │  ├─ zhk-order（订单服务）          │ │
│  │  │  - 订单创建、状态流转           │ │
│  │  │  - 租期计时（Redis TTL）        │ │
│  │  ├─ zhk-wallet（钱包服务）          │ │
│  │  │  - 资金冻结、解冻、分账         │ │
│  │  │  - 支付对接（微信/支付宝）      │ │
│  │  └─ zhk-risk（风控服务）           │ │
│  │     - 异地登录检测                 │ │
│  │     - 异常行为识别                 │ │
│  └───────────────────────────────────┘ │
│  【演进路径】日订单 > 5k 时拆分为微服务 │
└─────────────────┬───────────────────────┘
                  │ JDBC/MyBatis
┌─────────────────┴───────────────────────┐
│  数据层（Data Layer）                    │
│  ┌───────────────────────────────────┐ │
│  │  MySQL 8（主从架构）               │ │
│  │  - 主库：写操作                    │ │
│  │  - 从库：读操作（读写分离）         │ │
│  │  - 分库分表策略（按 user_id 分片）  │ │
│  ├─ Redis Cluster（缓存 + 分布式锁）   │ │
│  │  - 热点数据缓存（账号信息、订单）   │ │
│  │  - 分布式锁（Redisson）             │ │
│  │  - 租期计时器（TTL + 定时任务）     │ │
│  │  - 会话管理（JWT 黑名单）           │ │
│  └─ MinIO（对象存储）                  │ │
│     - 截图/录屏存储                    │ │
│     - 举证材料归档                     │ │
│     - CDN 加速（可选）                 │ │
└─────────────────────────────────────────┘
```

**技术选型说明**：
- **前端框架**：Vue 3 + TypeScript + Vite，采用 Composition API 提升代码可维护性
- **API 网关**：Spring Cloud Gateway，支持动态路由、限流、熔断
- **业务框架**：Spring Boot 3.x，采用模块化设计，便于后续微服务拆分
- **ORM 框架**：MyBatis-Plus，简化 CRUD，支持多数据源
- **缓存方案**：Redis Cluster，采用 Redisson 实现分布式锁和定时任务
- **消息队列**（M2 阶段引入）：RabbitMQ/Kafka，用于异步任务和事件驱动

### 3.3 组件图（C4 Level 3: Component）

组件图细化业务聚合层内部的组件设计。详见 `../diagrams/ZHK_C4_Component.puml`。

**核心组件划分**（遵循单一职责原则）：

1. **用户服务组件（zhk-user）**
   - UserController：用户注册、登录、实名认证接口
   - UserService：用户业务逻辑，信用评估
   - AuthService：JWT 生成、验证，OAuth2 集成
   - CreditService：芝麻信用对接

2. **资产服务组件（zhk-asset）**
   - AccountController：账号发布、查询、上下架接口
   - AccountService：账号管理逻辑
   - EncryptionService：账号密码加密/解密（AES-256-GCM）
   - GameService：游戏信息管理

3. **订单服务组件（zhk-order）**
   - OrderController：订单创建、查询、取消接口
   - OrderService：订单状态流转（状态机模式）
   - TimerService：租期计时、到期提醒、自动回收
   - LauncherService：启动器通信（WebSocket）

4. **钱包服务组件（zhk-wallet）**
   - WalletController：余额查询、提现接口
   - PaymentService：支付对接（微信/支付宝）
   - EscrowService：资金担保、冻结、解冻、分账
   - SettlementService：结算服务（定时任务）

5. **风控服务组件（zhk-risk）**
   - RiskController：风控规则配置接口
   - LocationService：异地登录检测（IP 地理位置）
   - BehaviorService：异常行为识别（规则引擎）
   - AppealService：申诉处理

### 3.4 数据模型设计

**核心数据表**（遵循第三范式，预留扩展字段）：

```sql
-- 用户表
user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  nickname VARCHAR(50) NOT NULL,
  mobile VARCHAR(11) UNIQUE NOT NULL,
  idcard_hash VARCHAR(64) COMMENT '身份证 SHA-256 哈希',
  zhima_score INT COMMENT '芝麻信用分',
  avatar_url VARCHAR(255),
  status TINYINT DEFAULT 1 COMMENT '1-正常 2-冻结',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_mobile (mobile),
  INDEX idx_zhima_score (zhima_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 游戏表
game (
  id INT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL,
  publisher VARCHAR(50),
  login_type ENUM('qr', 'pwd', 'token') NOT NULL COMMENT '登录方式',
  icon_url VARCHAR(255),
  status TINYINT DEFAULT 1,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 账号表（敏感信息加密存储）
account (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  game_id INT NOT NULL,
  owner_uid BIGINT NOT NULL,
  username_enc TEXT NOT NULL COMMENT 'AES-256-GCM 加密',
  pwd_enc TEXT NOT NULL COMMENT 'AES-256-GCM 加密',
  iv VARCHAR(32) NOT NULL COMMENT '初始化向量',
  lvl INT COMMENT '账号等级',
  skins JSON COMMENT '皮肤信息（JSON）',
  deposit DECIMAL(10,2) NOT NULL COMMENT '固定押金',
  price_30min DECIMAL(10,2) NOT NULL COMMENT '30分钟价格',
  price_1h DECIMAL(10,2) NOT NULL,
  price_overnight DECIMAL(10,2) NOT NULL,
  status TINYINT DEFAULT 1 COMMENT '1-上架 2-下架 3-租赁中',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (game_id) REFERENCES game(id),
  FOREIGN KEY (owner_uid) REFERENCES user(id),
  INDEX idx_game_status (game_id, status),
  INDEX idx_owner (owner_uid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 租赁订单表
lease_order (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  tenant_uid BIGINT NOT NULL,
  start_time TIMESTAMP NOT NULL,
  end_time TIMESTAMP NOT NULL,
  actual_end_time TIMESTAMP NULL COMMENT '实际结束时间',
  amount DECIMAL(10,2) NOT NULL COMMENT '租金',
  deposit DECIMAL(10,2) NOT NULL COMMENT '押金',
  status ENUM('paying', 'leasing', 'closed', 'appeal', 'cancelled') DEFAULT 'paying',
  evidence_hash VARCHAR(64) COMMENT '还号证据哈希（SHA-256）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (account_id) REFERENCES account(id),
  FOREIGN KEY (tenant_uid) REFERENCES user(id),
  INDEX idx_tenant_status (tenant_uid, status),
  INDEX idx_account_time (account_id, start_time, end_time),
  INDEX idx_status_time (status, end_time) COMMENT '用于定时任务查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 申诉表
appeal (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  type TINYINT NOT NULL COMMENT '1-账号异常 2-押金争议 3-其他',
  evidence_urls JSON COMMENT '证据 URL 列表',
  verdict TINYINT COMMENT '1-支持租客 2-支持号主 3-各担一半',
  operator_uid BIGINT COMMENT '处理人',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolve_time TIMESTAMP NULL,
  FOREIGN KEY (order_id) REFERENCES lease_order(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 支付记录表
payment_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  payment_type ENUM('wechat', 'alipay') NOT NULL,
  amount DECIMAL(10,2) NOT NULL,
  transaction_id VARCHAR(64) UNIQUE COMMENT '第三方交易号',
  status ENUM('pending', 'success', 'failed', 'refunded') DEFAULT 'pending',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  paid_at TIMESTAMP NULL,
  FOREIGN KEY (order_id) REFERENCES lease_order(id),
  INDEX idx_transaction_id (transaction_id),
  INDEX idx_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**数据安全设计**：
- 敏感字段加密：账号密码采用 AES-256-GCM，每账号独立 IV
- 身份证脱敏：仅存储 SHA-256 哈希，用于去重和风控
- 数据备份：MySQL 主从复制 + 每日全量备份 + 增量备份
- 数据归档：超过 1 年的订单数据归档到冷存储

### 3.5 安全架构设计

安全架构采用**纵深防御**策略，从网络、应用、数据多个层面保障系统安全。详见 `../diagrams/ZHK_C4_Security_Architecture.puml`。

**安全分层设计**：

1. **传输安全层**
   - HTTPS/TLS 1.3 全站加密
   - API 接口签名验证（防止重放攻击）
   - WebSocket 连接加密（启动器通信）

2. **应用安全层**
   - **身份认证**：JWT + OAuth2，支持微信/支付宝快捷登录
   - **权限控制**：RBAC（基于角色的访问控制），租客/商家/运营角色隔离
   - **密码加密**：AES-256-GCM，密钥存储在 KMS（阿里云 KMS/自建 Vault）
   - **API 限流**：Sentinel 实现接口级限流，防止暴力破解
   - **输入验证**：参数校验、SQL 注入防护、XSS 防护

3. **数据安全层**
   - **敏感数据加密**：账号密码 AES-256-GCM，每账号独立 IV
   - **数据脱敏**：身份证仅存储 SHA-256 哈希
   - **密钥管理**：密钥存储在 KMS，定期轮换
   - **数据备份加密**：备份文件 AES-256 加密

4. **风控安全层**
   - **异地登录检测**：
     - 登录 IP 与常用地距离 > 200km → 触发人脸识别
     - 5 分钟内多地登录 → 自动冻结账号并创建申诉
     - 基于 IP 地理位置库（GeoIP2）
   - **异常行为识别**：
     - 频繁下单取消（规则引擎）
     - 账号异常使用（启动器上报）
     - 黑名单机制（设备指纹、IP、手机号）
   - **资金安全**：
     - 浮动押金计算：`max(固定押金, 账号市场估价 × 30%)`
     - 支付宝预授权：冻结花呗额度，平台不动用现金
     - 资金分账：订单完成后 24h 自动分账给号主（T+1 结算）

5. **沙盒隔离层**（P1 项目）
   - **Windows Hyper-V Container**：每次租号生成一次性快照
   - **写屏蔽磁盘**：快照销毁前禁止写入
   - **内存加密**：运行时内存数据加密
   - **网络隔离**：沙盒内网络访问受限

6. **举证链设计**
   - 还号时启动器自动截图 + 录屏 15s
   - 上传 MinIO，生成 SHA-256 哈希
   - 哈希写入订单表，供后续仲裁使用
   - 支持区块链存证（M3 阶段）

### 3.6 非功能性需求（NFR）

**性能指标**：
- API 响应时间：P99 < 500ms，P95 < 200ms
- 并发处理能力：单节点支持 500 并发下单
- 数据库查询：复杂查询 < 100ms
- 缓存命中率：> 80%

**可用性指标**：
- 系统可用性：99.9%（年停机时间 < 8.76 小时）
- 故障恢复时间：RTO < 30 分钟，RPO < 5 分钟
- 数据库主从切换：< 1 分钟

**可扩展性**：
- 水平扩展：业务层无状态，支持横向扩容
- 数据库扩展：支持分库分表（按 user_id 分片）
- 缓存扩展：Redis Cluster 支持动态扩容

**可维护性**：
- 代码覆盖率：单元测试覆盖率 > 70%
- 文档完整性：API 文档（Swagger）、架构文档（C4 模型）
- 日志规范：结构化日志（JSON），支持链路追踪（TraceID）

### 3.7 部署架构设计

部署架构采用**渐进式演进**策略，从单机到集群平滑过渡。详见 `../diagrams/ZHK_C4_Deployment.puml`。

**阶段一：MVP 阶段（日订单 < 1k）**
- **部署方式**：Docker Compose 单机部署
- **容器配置**：
  - `zhk-app`：Spring Boot 应用（2 CPU, 4GB RAM）
  - `mysql`：MySQL 8 主库（2 CPU, 4GB RAM）
  - `redis`：Redis 单节点（1 CPU, 2GB RAM）
  - `minio`：MinIO 对象存储（1 CPU, 2GB RAM）
  - `nginx`：反向代理（1 CPU, 512MB RAM）

**阶段二：成长阶段（日订单 1k - 5k）**
- **部署方式**：Docker Swarm / K8s 单集群
- **架构升级**：
  - MySQL 主从复制（1 主 1 从）
  - Redis Sentinel（1 主 2 从）
  - 业务层多实例（2-3 个 Pod）
  - Nginx 负载均衡

**阶段三：成熟阶段（日订单 > 5k）**
- **部署方式**：Kubernetes 多集群
- **架构升级**：
  - MySQL 读写分离（1 主 N 从，按业务分库）
  - Redis Cluster（3 主 3 从）
  - 业务层微服务拆分（按模块拆分）
  - 消息队列（RabbitMQ/Kafka）引入
  - 服务网格（Istio）用于服务治理

**灰度发布策略**：
- **用户灰度**：新功能先对 5% 白名单用户开放
- **地域灰度**：新功能先在单个地域（如华东）上线
- **功能开关**：Feature Flag 机制，支持快速回滚
- **监控告警**：实时监控错误率、响应时间，异常自动回滚

**监控与可观测性**：
- **指标监控**：Prometheus + Grafana
  - 业务指标：订单量、支付成功率、用户活跃度
  - 技术指标：CPU、内存、QPS、错误率
- **日志聚合**：ELK Stack（Elasticsearch + Logstash + Kibana）
- **链路追踪**：SkyWalking / Jaeger（分布式追踪）
- **告警机制**：AlertManager + 钉钉/企业微信通知

### 3.8 关键业务流程时序图

核心业务流程的时序设计，展示系统各组件间的交互。详见 `../diagrams/ZHK_C4_Sequence_Message_Flow.puml`。

**场景一：30 分钟短租完整流程**

```
1. 【选号下单】
   租客 → Web前端：浏览账号列表
   Web前端 → API网关：GET /api/v1/accounts?game_id=1
   API网关 → 业务层：查询可用账号
   业务层 → MySQL：查询账号表（status=1）
   业务层 → Redis：缓存热点账号信息
   业务层 → Web前端：返回账号列表
   
2. 【创建订单】
   租客 → Web前端：选择账号，下单（30分钟）
   Web前端 → API网关：POST /api/v1/orders
   API网关 → 业务层：创建订单
   业务层 → MySQL：插入订单记录（status='paying'）
   业务层 → Redis：设置分布式锁（防止重复下单）
   业务层 → Web前端：返回订单号和支付信息
   
3. 【支付处理】
   租客 → 微信/支付宝：发起支付
   微信/支付宝 → 业务层：支付回调（异步）
   业务层 → MySQL：更新订单状态（status='leasing'）
   业务层 → Redis：写入"租期令牌"（key=order_id, TTL=30min）
   业务层 → 钱包服务：冻结押金（资金担保）
   业务层 → Web前端：支付成功通知（WebSocket推送）
   
4. 【自动上号】
   业务层 → Windows启动器：WebSocket消息（账号信息+一次性token）
   Windows启动器 → 业务层：确认接收
   Windows启动器 → 游戏客户端：自动登录（使用加密账号信息）
   游戏客户端 → 游戏服务器：登录成功
   Windows启动器 → 业务层：心跳上报（每5分钟）
   
5. 【租期管理】
   Redis TTL到期前5分钟：
   业务层 → Windows启动器：弹窗提醒续租
   租客选择续租：
     - 业务层 → 微信/支付宝：发起续租支付
     - 业务层 → Redis：延长TTL
   租客未续租：
     - 业务层 → Windows启动器：强制踢号指令
     - Windows启动器 → 游戏客户端：退出登录
     - Windows启动器 → 业务层：修改密码（随机生成）
     - 业务层 → MySQL：更新账号状态（status=1，可租）
     - 业务层 → Redis：删除租期令牌
   
6. 【还号确认】
   租客 → Web前端：确认还号
   Windows启动器 → MinIO：上传截图+录屏（15s）
   Windows启动器 → 业务层：还号确认（证据哈希）
   业务层 → MySQL：更新订单（status='closed', evidence_hash=xxx）
   业务层 → 钱包服务：解冻押金，分账给号主（T+1结算）
   业务层 → Web前端：订单完成通知
```

**场景二：异地登录风控流程**

```
1. 【登录检测】
   租客 → Windows启动器：登录游戏
   Windows启动器 → 业务层：上报登录IP
   业务层 → GeoIP服务：查询IP地理位置
   业务层 → MySQL：查询用户常用登录地
   
2. 【风控判断】
   距离 > 200km：
     - 业务层 → Web前端：触发人脸识别
     - 租客 → 人脸识别服务：完成验证
     - 业务层 → MySQL：更新用户常用登录地
   5分钟内多地登录：
     - 业务层 → MySQL：冻结账号（status=2）
     - 业务层 → MySQL：创建申诉记录
     - 业务层 → 运营系统：推送告警
```

--------------------------------------------------
四、迭代 Roadmap
M1（0-1 个月）：  
  账号发布/浏览/下单/支付/自动上号/自动回收/评价体系  
M2（1-3 个月）：  
  芝麻免押、分时竞价、商家批量导入、运营后台数据大盘  
M3（3-6 个月）：  
  微服务拆分、多云容灾、国际版（英文/Stripe 支付）、区块链举证存证  
M4（6-12 个月）：  
  号主保险（账号封禁赔付）、AI 推荐定价、直播带租、NFT 游戏资产租赁

--------------------------------------------------
五、风险与合规
1. 游戏厂商 ToS：部分厂商禁止账号共享，需准备“封禁险”与合规声明。  
2. 个人信息：身份证 OCR 数据需加密落库，仅用于风控，不对外提供。  
3. 资金二清：若平台代收代付超过 500w/月，需申请《支付业务许可证》或与持牌机构合作（微信/支付宝收单+分账）。

--------------------------------------------------
六、下一步行动清单（供技术负责人直接排期）
- [ ] 0.5d 注册域名/备案/云主机  
- [ ] 1d 搭建 GitLab-CI + SonarQube  
- [ ] 3d 完成数据库 ER 图评审 & MySQL 脚本  
- [ ] 5d 完成 Spring-Boot 基架（统一异常、分页、Swagger、多环境配置）  
- [ ] 7d 完成账号发布/上下架/浏览接口 + 单元测试覆盖率>70%  
- [ ] 10d 对接微信/支付宝支付沙箱 + 担保记账  
- [ ] 14d Windows Launcher Demo（C#/Electron）能自动改密并启动游戏  
- [ ] 21d 压测：单节点 500 并发下单，CPU<60%，P99<500ms  
- [ ] 30d MVP 上线，邀 50 名种子用户内测，收集 100 条反馈→进入 M2

--------------------------------------------------
九、附录

### 9.1 C4 模型架构图文件清单

本文档配套的 C4 模型架构图文件位于 `../diagrams/` 目录：

- `ZHK_C4_System_Context.puml` - 系统上下文图（C4 Level 1）
- `ZHK_C4_Container.puml` - 容器图（C4 Level 2）
- `ZHK_C4_Component.puml` - 组件图（C4 Level 3）
- `ZHK_C4_Deployment.puml` - 部署图
- `ZHK_C4_Security_Architecture.puml` - 安全架构图
- `ZHK_C4_Sequence_Message_Flow.puml` - 关键业务流程时序图

**生成图片**：使用 `../diagrams/generate.sh` 脚本生成 PNG 图片。

### 9.2 技术栈清单

**前端技术栈**：
- Vue 3.3+ (Composition API)
- TypeScript 5.0+
- Vite 5.0+
- Element Plus / Ant Design Vue
- Axios (HTTP 客户端)
- Pinia (状态管理)
- Vue Router (路由)

**后端技术栈**：
- Spring Boot 3.2+
- Spring Cloud Gateway (API 网关)
- MyBatis-Plus 3.5+
- Spring Security + JWT (认证授权)
- Redisson (分布式锁)
- Sentinel (限流熔断)

**数据存储**：
- MySQL 8.0+ (主从架构)
- Redis 7.0+ (Cluster 模式)
- MinIO (对象存储)

**运维工具**：
- Docker & Docker Compose
- Kubernetes (生产环境)
- Nginx (反向代理)
- Prometheus + Grafana (监控)
- ELK Stack (日志聚合)

### 9.3 关键设计决策记录（ADR）

**ADR-001: 采用单体架构起步**
- **决策**：MVP 阶段采用 Spring Boot 单体架构
- **理由**：降低开发复杂度，快速验证商业模式，减少运维成本
- **影响**：后续需要重构为微服务，但通过模块化设计降低重构成本

**ADR-002: 采用 C4 模型作为架构文档框架**
- **决策**：使用 C4 模型描述系统架构
- **理由**：层次清晰，易于理解，适合不同角色人员阅读
- **影响**：需要维护多层次的架构图，但提升了文档质量

**ADR-003: 账号密码采用 AES-256-GCM 加密**
- **决策**：使用 AES-256-GCM 对称加密，每账号独立 IV
- **理由**：GCM 模式提供认证加密，安全性高，性能好
- **影响**：需要妥善管理密钥，考虑引入 KMS

**ADR-004: 租期计时采用 Redis TTL + 定时任务**
- **决策**：使用 Redis TTL 作为主要计时机制，定时任务作为兜底
- **理由**：Redis TTL 精确度高，性能好，定时任务保证可靠性
- **影响**：需要处理 Redis 故障场景，考虑主从切换

**ADR-005: 资金担保采用支付宝预授权**
- **决策**：使用支付宝预授权接口，冻结花呗额度
- **理由**：平台不动用现金，降低资金风险，符合监管要求
- **影响**：需要对接支付宝预授权接口，处理预授权过期场景

---

**文档版本历史**：
- v1.0 (2024) - 初始版本，包含完整的 C4 模型架构设计

**维护者**：架构团队  
**审核者**：技术负责人  
**最后更新**：2025/11/

---

至此，"租号酷（ZHK-RentalCore）"的命名、愿景、架构设计（基于 C4 模型）、迭代路线与落地清单全部就绪。本文档参考了前沿文献和成熟的架构设计实践，可直接用于毕业设计开题和项目开发。祝项目启动顺利，早日上线！

--------------------------------------------------
七、安全架构进阶项目（P1-P6）

以下为安全架构的进阶研究方向，适用于毕业设计的技术创新点。

| 编号 | 子项目名称        | 采用的前沿技术        | 拟解决的核心痛点                   | 实施要点与指标                                                                                                | 预期周期 |
| -- | ------------ | -------------- | -------------------------- | ------------------------------------------------------------------------------------------------------ | ---- |
| P1 | 沙盒式自动登录器     | Windows Hyper-V Container 沙盒隔离      | 防止租客提取账号密码、外挂注入            | 基于 Windows Hyper-V Container 构建"游戏沙盒"，每次租号生成一次性快照；快照销毁前写屏蔽磁盘，内存加密。验收：通过代码审计 + 逆向挑战赛（白帽 48 h 内未提取有效密码）。 | 6 周  |
| P2 | 提示注入检测网关     | LLM 提示注入防御模式   | 平台客服机器人和公告系统被注入恶意指令导致封号    | 在客服 LLM 前加"安全前置模型"，对输入进行令牌化+上下文裁剪；超过风险阈值转人工。验收：对 1 万条真实会话注入测试，误杀率 <3%，漏杀率 <1%。                         | 4 周  |
| P3 | 隐匿状态隐私合规层    | LLM 内部状态逆向攻击防御 | 平台未来若使用"AI 托管申诉文本"可能泄露用户隐私 | ① 不上传完整中间层 ② 采用差分隐私 + 分层量化 ③ 本地 GPU 推理。验收：用论文攻击方法还原率 <10%。                                             | 8 周  |
| P4 | 图风控后门清洗引擎    | 图神经网络多维防御 MAD  | 黑产通过伪造"正常好友链"绕过现有图风控       | 每 24 h 离线重训 GNN，训练前运行 MAD 清洗；在线推理时加噪声边检测。验收：后门攻击成功率 <1%，图模型 AUC 下降 <0.5%。                              | 6 周  |
| P5 | 量子安全密钥协商层    | DAWN-NTRU 双编码  | 长期账号加密密钥被量子计算暴力破解          | 在账号密码本地加密环节，新增"量子保险箱"：对称密钥用 DAWN-NTRU 封装，定期轮换。验收：密钥封装耗时 <0.8 ms/次，公钥尺寸 <800 B。                         | 10 周 |
| P6 | 零知识"未泄露密码"证明 | 自定义 ZK 协议      | 平台需向游戏厂商自证"未向租客泄露明文密码"     | 采用 zk-SNARK 方案，把"对称加密 + 沙盒快照哈希"作为 witness，生成证明；厂商可公开验证。验收：生成证明时间 <3 s，验证时间 <50 ms，证明大小 <2 kB。          | 12 周 |

--------------------------------------------------
八、参考文献与相关文献

### 8.1 架构设计相关文献

1. **Brown, S.** (2018). *Software Architecture for Developers: Volume 2 - Visualise, document and explore your software architecture*. Leanpub.
   - C4 模型的提出者和权威指南，本文档采用 C4 模型作为架构描述框架

2. **Richards, M., & Ford, N.** (2020). *Fundamentals of Software Architecture: An Engineering Approach*. O'Reilly Media.
   - 软件架构基础理论，涵盖架构特征、模式、组件设计等核心概念

3. **Newman, S.** (2021). *Building Microservices: Designing Fine-Grained Systems* (2nd ed.). O'Reilly Media.
   - 微服务架构设计模式，指导从单体到微服务的演进路径

4. **Evans, E.** (2003). *Domain-Driven Design: Tackling Complexity in the Heart of Software*. Addison-Wesley.
   - 领域驱动设计（DDD）理论，指导业务建模和系统设计

### 8.2 分布式系统相关文献

5. **Kleppmann, M.** (2017). *Designing Data-Intensive Applications: The Big Ideas Behind Reliable, Scalable, and Maintainable Systems*. O'Reilly Media.
   - 分布式系统设计原理，涵盖数据存储、复制、分区、事务等核心概念

6. **Tanenbaum, A. S., & Van Steen, M.** (2017). *Distributed Systems: Principles and Paradigms* (3rd ed.). Pearson.
   - 分布式系统理论基础，适合深入理解系统设计原理

### 8.3 安全架构相关文献

7. **Anderson, R.** (2020). *Security Engineering: A Guide to Building Dependable Distributed Systems* (3rd ed.). Wiley.
   - 安全工程实践，涵盖加密、认证、访问控制等安全设计

8. **Howard, M., & LeBlanc, D.** (2003). *Writing Secure Code* (2nd ed.). Microsoft Press.
   - 安全编码实践，指导应用层安全设计

### 8.4 支付与金融系统相关文献

9. **Bauer, M., & Böhme, R.** (2020). "The Economics of Payment Card Interchange Fees and the Limits of Regulation." *International Journal of Central Banking*.
   - 支付系统经济学分析，理解支付流程设计

10. **Nakamoto, S.** (2008). "Bitcoin: A Peer-to-Peer Electronic Cash System." *Bitcoin Whitepaper*.
    - 区块链与数字货币基础，为未来区块链存证提供参考

### 8.5 容器化与云原生相关文献

11. **Burns, B., & Beda, J.** (2019). *Kubernetes: Up and Running* (2nd ed.). O'Reilly Media.
    - Kubernetes 容器编排实践，指导生产环境部署

12. **Hightower, K., et al.** (2017). *Kubernetes: The Hard Way*. GitHub.
    - Kubernetes 深入理解，适合学习容器化架构

### 8.6 中文技术社区资源

13. **阿里云开发者社区** - 《软件架构可视化及C4模型，架构设计不仅仅是UML》
    - https://developer.aliyun.com/article/2569

14. **腾讯云开发者社区** - 《探索软件架构的艺术：C4模型与4+1视图模型的比较与应用》
    - https://cloud.tencent.com/developer/article/2387509

15. **CSDN博客** - 《C4模型详解：面向现代软件系统的可视化架构框架》
    - https://blog.csdn.net/csdn_tom_168/article/details/148869213

### 8.7 相关技术标准

16. **OWASP Top 10** (2021). *OWASP Top 10 - 2021: The Ten Most Critical Web Application Security Risks*.
    - Web 应用安全风险标准，指导安全设计

17. **PCI DSS** (2022). *Payment Card Industry Data Security Standard v4.0*.
    - 支付卡行业数据安全标准，指导支付系统安全设计

### 8.8 毕业设计开题建议

**研究方向建议**：
1. **基于 C4 模型的游戏账号租赁平台架构设计与实现**
   - 研究重点：C4 模型在复杂业务系统中的应用，从单体到微服务的演进路径

2. **基于沙盒隔离的游戏账号安全租赁机制研究**
   - 研究重点：Windows Hyper-V Container 在账号安全中的应用，防止密码泄露

3. **基于分布式锁和 Redis 的租期计时系统设计与实现**
   - 研究重点：分布式系统一致性保证，高并发场景下的计时精度

4. **游戏账号租赁平台资金担保系统设计与实现**
   - 研究重点：支付系统设计，资金冻结/解冻/分账的分布式事务处理

5. **基于零知识证明的游戏账号租赁隐私保护研究**
   - 研究重点：零知识证明在业务场景中的应用，向游戏厂商证明未泄露密码

**开题报告结构建议**：
1. 研究背景与意义
2. 国内外研究现状
3. 研究内容与技术路线
4. 系统需求分析
5. 系统架构设计（采用 C4 模型）
6. 关键技术实现
7. 系统测试与评估
8. 总结与展望
9. 参考文献

--------------------------------------------------


