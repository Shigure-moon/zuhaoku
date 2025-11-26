# 租号酷项目开发指南

## 文档信息
- **版本**: v1.0
- **最后更新**: 2024
- **项目**: 租号酷（ZHK-RentalCore）游戏账号分时租借平台

---

## 一、项目概述

### 1.1 项目简介
租号酷是一个游戏账号分时租借平台，支持用户出租和租赁游戏账号，提供自动上下号、资金担保、风控管理等核心功能。

### 1.2 技术架构
- **前端**: Vue 3 + TypeScript + Vite
- **后端**: Spring Boot 3.2 + MyBatis-Plus + Redis
- **数据库**: MySQL 8.0（主从架构）
- **缓存**: Redis 7.0（Cluster 模式）
- **存储**: MinIO（对象存储）
- **网关**: Spring Cloud Gateway

### 1.3 文档导航
- [前端开发文档](./前端开发文档.md) - 前端开发详细指南
- [后端开发文档](./后端开发文档.md) - 后端开发详细指南
- [架构设计文档](../zuhaoku.md) - 系统架构设计文档

---

## 二、开发环境准备

### 2.1 必需软件

#### 前端开发
- Node.js >= 18.0.0
- npm >= 9.0.0 或 pnpm >= 8.0.0
- Git >= 2.30.0
- VS Code（推荐）或 WebStorm

#### 后端开发
- JDK >= 17
- Maven >= 3.8.0
- Git >= 2.30.0
- IntelliJ IDEA（推荐）或 Eclipse

#### 数据库与中间件
- MySQL >= 8.0
- Redis >= 7.0
- MinIO（对象存储）

### 2.2 快速开始

```bash
# 1. 克隆项目
git clone <repository-url>
cd zuhaoku

# 2. 启动基础设施（Docker Compose）
docker-compose up -d mysql redis minio

# 3. 初始化数据库
mysql -u root -p < scripts/init.sql

# 4. 启动后端
cd backend
mvn spring-boot:run

# 5. 启动前端
cd frontend
npm install
npm run dev
```

---

## 三、项目结构

```
zuhaoku/
├── frontend/              # 前端项目
│   ├── src/              # 源代码
│   ├── public/           # 静态资源
│   └── package.json      # 依赖配置
├── backend/              # 后端项目
│   ├── zhk-monolith/     # 业务聚合层
│   ├── zhk-gateway/      # API 网关
│   └── pom.xml           # Maven 配置
├── docs/                 # 文档目录
│   ├── 前端开发文档.md
│   ├── 后端开发文档.md
│   └── 开发指南.md
├── diagrams/             # 架构图
│   ├── ZHK_C4_*.puml     # C4 模型图
│   └── generate.sh       # 图片生成脚本
├── scripts/              # 脚本文件
│   └── init.sql          # 数据库初始化脚本
├── docker-compose.yml    # Docker Compose 配置
└── zuhaoku.md           # 架构设计文档
```

---

## 四、开发流程

### 4.1 Git 工作流

#### 分支策略
- **main**: 主分支，生产环境代码
- **develop**: 开发分支，集成最新功能
- **feature/xxx**: 功能分支，从 develop 创建
- **hotfix/xxx**: 热修复分支，从 main 创建

#### 提交流程
```bash
# 1. 创建功能分支
git checkout -b feature/user-login

# 2. 开发并提交
git add .
git commit -m "feat: 添加用户登录功能"

# 3. 推送到远程
git push origin feature/user-login

# 4. 创建 Pull Request
# 在 GitLab/GitHub 上创建 PR，请求合并到 develop
```

### 4.2 代码审查

#### 审查清单
- [ ] 代码符合规范（ESLint/Checkstyle）
- [ ] 功能测试通过
- [ ] 单元测试覆盖率 >= 70%
- [ ] 无明显的性能问题
- [ ] 错误处理完善
- [ ] 日志记录适当
- [ ] 文档更新（如有需要）

### 4.3 测试策略

#### 前端测试
- **单元测试**: Vitest
- **组件测试**: Vue Test Utils
- **E2E 测试**: Playwright（可选）

#### 后端测试
- **单元测试**: JUnit 5
- **集成测试**: Spring Boot Test
- **API 测试**: Postman/Newman

---

## 五、API 接口规范

### 5.1 接口设计原则
- RESTful 风格
- 统一响应格式
- 版本控制（/api/v1/）
- 统一错误码

### 5.2 响应格式

**成功响应**
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

**错误响应**
```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null,
  "timestamp": 1703123456789
}
```

### 5.3 错误码规范

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权（Token 无效） |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### 5.4 API 文档

使用 Swagger/OpenAPI 生成 API 文档：

访问地址：`http://localhost:8080/swagger-ui.html`

---

## 六、数据库设计

### 6.1 核心表结构

详见 [架构设计文档](../zuhaoku.md#34-数据模型设计)

主要表：
- `user` - 用户表
- `game` - 游戏表
- `account` - 账号表（敏感信息加密）
- `lease_order` - 租赁订单表
- `payment_record` - 支付记录表
- `appeal` - 申诉表

### 6.2 数据库规范

- 表名：小写，下划线分隔，如 `lease_order`
- 字段名：小写，下划线分隔，如 `user_id`
- 主键：统一使用 `id BIGINT AUTO_INCREMENT`
- 时间字段：`created_at`, `updated_at` 使用 TIMESTAMP
- 软删除：使用 `deleted` 字段（0-未删除，1-已删除）

---

## 七、安全规范

### 7.1 认证授权
- JWT Token 认证
- RBAC 权限控制
- Token 过期时间：24 小时
- Token 刷新机制

### 7.2 数据安全
- 敏感数据加密（AES-256-GCM）
- 密码哈希存储（BCrypt）
- SQL 注入防护（MyBatis 参数化查询）
- XSS 防护（前端输入验证）

### 7.3 API 安全
- HTTPS 强制
- 请求签名验证
- 限流熔断（Sentinel）
- IP 白名单（可选）

---

## 八、性能优化

### 8.1 前端优化
- 路由懒加载
- 组件懒加载
- 图片懒加载和压缩
- 代码分割（Code Splitting）
- CDN 加速静态资源

### 8.2 后端优化
- 数据库索引优化
- Redis 缓存热点数据
- 数据库读写分离
- 连接池配置
- 异步处理（@Async）

### 8.3 数据库优化
- 合理使用索引
- 避免全表扫描
- 分页查询优化
- 慢查询监控

---

## 九、部署流程

### 9.1 开发环境
```bash
# 使用 Docker Compose
docker-compose -f docker-compose.dev.yml up -d
```

### 9.2 测试环境
```bash
# 构建镜像
docker build -t zhk-backend:latest ./backend
docker build -t zhk-frontend:latest ./frontend

# 部署
docker-compose -f docker-compose.test.yml up -d
```

### 9.3 生产环境
- 使用 Kubernetes 部署
- 配置 CI/CD 流水线
- 蓝绿部署或滚动更新
- 监控告警配置

---

## 十、常见问题

### 10.1 前端问题

**Q: 跨域问题如何解决？**
A: 开发环境使用 Vite 代理，生产环境通过 Nginx 反向代理。

**Q: 如何调试 API 请求？**
A: 使用浏览器开发者工具的 Network 面板，或使用 Vue DevTools。

### 10.2 后端问题

**Q: 如何查看 SQL 日志？**
A: 配置 MyBatis-Plus 的 `log-impl` 为 `StdOutImpl`。

**Q: Redis 连接失败？**
A: 检查 Redis 服务是否启动，配置是否正确。

### 10.3 数据库问题

**Q: 如何备份数据库？**
A: 使用 `mysqldump` 命令定期备份。

```bash
mysqldump -u root -p zhk_rental > backup.sql
```

---

## 十一、团队协作

### 11.1 沟通渠道
- **日常沟通**: 企业微信/钉钉
- **技术讨论**: GitLab Issues / Discussions
- **文档协作**: 项目 Wiki

### 11.2 会议安排
- **每日站会**: 每天上午 10:00（15 分钟）
- **周会**: 每周一上午（1 小时）
- **技术评审**: 功能开发前（按需）

### 11.3 代码规范
- 遵循团队制定的代码规范
- 使用 Prettier/Checkstyle 自动格式化
- 提交前运行 lint 检查

---

## 十二、参考资源

### 12.1 官方文档
- [Vue 3 文档](https://cn.vuejs.org/)
- [Spring Boot 文档](https://spring.io/projects/spring-boot)
- [MyBatis-Plus 文档](https://baomidou.com/)

### 12.2 学习资源
- [Vue 3 组合式 API](https://cn.vuejs.org/guide/extras/composition-api-faq.html)
- [Spring Boot 最佳实践](https://spring.io/guides)
- [Redis 命令参考](https://redis.io/commands)

### 12.3 工具推荐
- **API 测试**: Postman / Insomnia
- **数据库管理**: DBeaver / DataGrip
- **Redis 管理**: RedisInsight
- **日志查看**: Kibana / Grafana

---

## 附录

### A. 开发环境检查清单

- [ ] Node.js 和 npm 已安装
- [ ] JDK 17+ 已安装
- [ ] Maven 已安装
- [ ] MySQL 已安装并运行
- [ ] Redis 已安装并运行
- [ ] Git 已配置
- [ ] IDE 已安装并配置
- [ ] 项目已克隆到本地
- [ ] 依赖已安装
- [ ] 数据库已初始化
- [ ] 项目可以正常启动

### B. 快速命令参考

```bash
# 前端
npm run dev          # 启动开发服务器
npm run build        # 构建生产版本
npm run lint         # 代码检查

# 后端
mvn clean install    # 编译项目
mvn spring-boot:run  # 启动应用
mvn test            # 运行测试

# Docker
docker-compose up -d        # 启动服务
docker-compose down         # 停止服务
docker-compose logs -f     # 查看日志
```

---

**文档维护**: shigure  
**最后更新**: 2025/11/18

