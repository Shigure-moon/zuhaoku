# 租号酷前端应用

## 技术栈

- **Vue 3.4+** - 渐进式 JavaScript 框架
- **TypeScript 5.4+** - 类型安全的 JavaScript
- **Vite 5.2+** - 下一代前端构建工具
- **Element Plus 2.6+** - Vue 3 组件库
- **Pinia 2.1+** - 状态管理
- **Vue Router 4.3+** - 路由管理
- **Axios 1.6+** - HTTP 客户端

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0 或 pnpm >= 8.0.0

### 安装依赖

```bash
npm install
# 或
pnpm install
```

### 开发

```bash
npm run dev
# 或
pnpm dev
```

应用将在 `http://localhost:3000` 启动

### 构建

```bash
npm run build
# 或
pnpm build
```

### 预览生产构建

```bash
npm run preview
# 或
pnpm preview
```

## 项目结构

```
frontend/
├── public/              # 静态资源
├── src/
│   ├── api/            # API 接口
│   ├── assets/         # 资源文件
│   ├── components/     # 组件
│   ├── composables/    # 组合式函数
│   ├── router/         # 路由配置
│   ├── stores/         # Pinia 状态管理
│   ├── types/          # TypeScript 类型
│   ├── utils/          # 工具函数
│   ├── views/          # 页面组件
│   ├── App.vue         # 根组件
│   └── main.ts         # 入口文件
├── .env.development    # 开发环境配置
├── .env.production     # 生产环境配置
├── vite.config.ts      # Vite 配置
└── package.json        # 依赖管理
```

## 环境变量

创建 `.env.development` 和 `.env.production` 文件，配置 API 地址等。

## 开发规范

- 使用 TypeScript 进行类型检查
- 遵循 Vue 3 Composition API 最佳实践
- 使用 ESLint 和 Prettier 保持代码风格一致

## 更多信息

详细开发文档请参考：[前端开发文档](../docs/FRONTEND_DEVELOPMENT.md)

---

**维护者**: shigure  
**最后更新**: 2025/11/18

