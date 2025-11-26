# 租号酷前端开发文档

## 文档信息
- **版本**: v1.0
- **最后更新**: 2024
- **技术栈**: Vue 3 + TypeScript + Vite
- **目标**: 提供完整的前端开发指南

---

## 一、技术栈与工具

### 1.1 核心技术
- **Vue 3.3+**: 采用 Composition API，提升代码可维护性
- **TypeScript 5.0+**: 类型安全，提升开发体验
- **Vite 5.0+**: 快速构建工具，开发体验优秀
- **Vue Router 4.x**: 路由管理
- **Pinia 2.x**: 状态管理（替代 Vuex）

### 1.2 UI 框架
- **Element Plus**: 主要 UI 组件库
- **Ant Design Vue**: 辅助 UI 组件（可选）

### 1.3 工具库
- **Axios**: HTTP 客户端，封装请求拦截器
- **Day.js**: 日期处理库
- **Lodash-es**: 工具函数库
- **VueUse**: Vue Composition API 工具集

### 1.4 开发工具
- **ESLint**: 代码规范检查
- **Prettier**: 代码格式化
- **Husky**: Git hooks 管理
- **Commitlint**: 提交信息规范

---

## 二、项目结构

```
frontend/
├── public/                 # 静态资源
│   ├── favicon.ico
│   └── index.html
├── src/
│   ├── api/               # API 接口定义
│   │   ├── user.ts        # 用户相关接口
│   │   ├── account.ts     # 账号相关接口
│   │   ├── order.ts       # 订单相关接口
│   │   ├── wallet.ts      # 钱包相关接口
│   │   └── request.ts     # Axios 封装
│   ├── assets/            # 资源文件
│   │   ├── images/       # 图片资源
│   │   ├── styles/       # 样式文件
│   │   └── fonts/        # 字体文件
│   ├── components/        # 公共组件
│   │   ├── common/       # 通用组件
│   │   ├── business/     # 业务组件
│   │   └── layout/       # 布局组件
│   ├── composables/       # 组合式函数
│   │   ├── useAuth.ts    # 认证相关
│   │   ├── useOrder.ts   # 订单相关
│   │   └── usePayment.ts # 支付相关
│   ├── router/           # 路由配置
│   │   ├── index.ts      # 路由入口
│   │   ├── modules/      # 路由模块
│   │   └── guards.ts     # 路由守卫
│   ├── stores/           # Pinia 状态管理
│   │   ├── user.ts       # 用户状态
│   │   ├── order.ts      # 订单状态
│   │   └── app.ts        # 应用状态
│   ├── views/            # 页面组件
│   │   ├── tenant/       # 租客端页面
│   │   ├── owner/        # 商家端页面
│   │   ├── operator/     # 运营端页面
│   │   └── common/       # 公共页面（登录、注册等）
│   ├── utils/            # 工具函数
│   │   ├── format.ts     # 格式化工具
│   │   ├── validate.ts   # 验证工具
│   │   └── constants.ts # 常量定义
│   ├── types/            # TypeScript 类型定义
│   │   ├── api.ts        # API 类型
│   │   ├── user.ts       # 用户类型
│   │   └── order.ts      # 订单类型
│   ├── App.vue           # 根组件
│   └── main.ts           # 入口文件
├── .env.development      # 开发环境配置
├── .env.production       # 生产环境配置
├── .eslintrc.js         # ESLint 配置
├── .prettierrc          # Prettier 配置
├── tsconfig.json        # TypeScript 配置
├── vite.config.ts       # Vite 配置
└── package.json         # 依赖管理
```

---

## 三、环境搭建

### 3.1 前置要求
- **Node.js**: >= 18.0.0
- **npm**: >= 9.0.0 或 **pnpm**: >= 8.0.0（推荐）
- **Git**: >= 2.30.0

### 3.2 安装步骤

```bash
# 1. 克隆项目（如果是从仓库）
git clone <repository-url>
cd zuhaoku/frontend

# 2. 安装依赖
npm install
# 或使用 pnpm（推荐）
pnpm install

# 3. 启动开发服务器
npm run dev
# 或
pnpm dev

# 4. 构建生产版本
npm run build
# 或
pnpm build
```

### 3.3 环境变量配置

创建 `.env.development` 文件：

```env
# API 基础地址
VITE_API_BASE_URL=http://localhost:8080/api/v1

# WebSocket 地址
VITE_WS_URL=ws://localhost:8080/ws

# 应用标题
VITE_APP_TITLE=租号酷

# 是否启用 Mock
VITE_USE_MOCK=false
```

创建 `.env.production` 文件：

```env
VITE_API_BASE_URL=https://api.zuhaoku.com/api/v1
VITE_WS_URL=wss://api.zuhaoku.com/ws
VITE_APP_TITLE=租号酷
VITE_USE_MOCK=false
```

---

## 四、核心功能实现

### 4.1 API 请求封装

**src/api/request.ts**

```typescript
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

// 创建 Axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config: AxiosRequestConfig) => {
    const userStore = useUserStore()
    // 添加 Token
    if (userStore.token) {
      config.headers = config.headers || {}
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data
    
    // 业务错误处理
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      
      // Token 过期，跳转登录
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
      
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
```

### 4.2 用户认证

**src/composables/useAuth.ts**

```typescript
import { ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { login, register, getUserInfo } from '@/api/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

export function useAuth() {
  const loading = ref(false)
  const userStore = useUserStore()

  // 登录
  const handleLogin = async (loginForm: LoginForm) => {
    loading.value = true
    try {
      const res = await login(loginForm)
      userStore.setToken(res.data.token)
      userStore.setUserInfo(res.data.userInfo)
      ElMessage.success('登录成功')
      router.push('/')
    } catch (error) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 注册
  const handleRegister = async (registerForm: RegisterForm) => {
    loading.value = true
    try {
      await register(registerForm)
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      loading.value = false
    }
  }

  // 退出登录
  const handleLogout = () => {
    userStore.logout()
    router.push('/login')
    ElMessage.success('已退出登录')
  }

  return {
    loading,
    handleLogin,
    handleRegister,
    handleLogout
  }
}
```

### 4.3 订单管理

**src/composables/useOrder.ts**

```typescript
import { ref, computed } from 'vue'
import { createOrder, getOrderList, getOrderDetail, renewOrder, returnOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

export function useOrder() {
  const orderList = ref<Order[]>([])
  const currentOrder = ref<Order | null>(null)
  const loading = ref(false)

  // 创建订单
  const create = async (orderData: CreateOrderDto) => {
    loading.value = true
    try {
      const res = await createOrder(orderData)
      ElMessage.success('订单创建成功')
      return res.data
    } catch (error) {
      console.error('创建订单失败:', error)
      throw error
    } finally {
      loading.value = false
    }
  }

  // 续租
  const renew = async (orderId: number, duration: number) => {
    try {
      await ElMessageBox.confirm('确认续租？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      const res = await renewOrder(orderId, duration)
      ElMessage.success('续租成功')
      return res.data
    } catch (error) {
      if (error !== 'cancel') {
        console.error('续租失败:', error)
      }
      throw error
    }
  }

  // 还号
  const returnAccount = async (orderId: number) => {
    try {
      await ElMessageBox.confirm('确认还号？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      
      const res = await returnOrder(orderId)
      ElMessage.success('还号成功')
      return res.data
    } catch (error) {
      if (error !== 'cancel') {
        console.error('还号失败:', error)
      }
      throw error
    }
  }

  return {
    orderList,
    currentOrder,
    loading,
    create,
    renew,
    returnAccount
  }
}
```

### 4.4 支付集成

**src/composables/usePayment.ts**

```typescript
import { ref } from 'vue'
import { createPayment, queryPaymentStatus } from '@/api/wallet'
import { ElMessage } from 'element-plus'

export function usePayment() {
  const paying = ref(false)

  // 发起支付
  const pay = async (orderId: number, paymentType: 'wechat' | 'alipay') => {
    paying.value = true
    try {
      const res = await createPayment(orderId, paymentType)
      
      if (paymentType === 'wechat') {
        // 微信支付：打开二维码
        // 实现微信支付二维码显示逻辑
      } else if (paymentType === 'alipay') {
        // 支付宝：跳转到支付页面
        window.location.href = res.data.paymentUrl
      }
      
      return res.data
    } catch (error) {
      ElMessage.error('支付失败')
      throw error
    } finally {
      paying.value = false
    }
  }

  // 查询支付状态
  const checkPaymentStatus = async (orderId: number) => {
    try {
      const res = await queryPaymentStatus(orderId)
      return res.data
    } catch (error) {
      console.error('查询支付状态失败:', error)
      throw error
    }
  }

  return {
    paying,
    pay,
    checkPaymentStatus
  }
}
```

---

## 五、路由配置

### 5.1 路由结构

**src/router/index.ts**

```typescript
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupRouterGuards } from './guards'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/tenant/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/common/Login.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/common/Register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  // 租客端路由
  {
    path: '/tenant',
    component: () => import('@/components/layout/TenantLayout.vue'),
    meta: { requiresAuth: true, role: 'tenant' },
    children: [
      {
        path: 'home',
        name: 'TenantHome',
        component: () => import('@/views/tenant/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'accounts',
        name: 'TenantAccounts',
        component: () => import('@/views/tenant/AccountList.vue'),
        meta: { title: '账号列表' }
      },
      {
        path: 'orders',
        name: 'TenantOrders',
        component: () => import('@/views/tenant/OrderList.vue'),
        meta: { title: '我的订单' }
      }
    ]
  },
  // 商家端路由
  {
    path: '/owner',
    component: () => import('@/components/layout/OwnerLayout.vue'),
    meta: { requiresAuth: true, role: 'owner' },
    children: [
      {
        path: 'dashboard',
        name: 'OwnerDashboard',
        component: () => import('@/views/owner/Dashboard.vue'),
        meta: { title: '工作台' }
      },
      {
        path: 'accounts',
        name: 'OwnerAccounts',
        component: () => import('@/views/owner/AccountList.vue'),
        meta: { title: '我的账号' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 设置路由守卫
setupRouterGuards(router)

export default router
```

### 5.2 路由守卫

**src/router/guards.ts**

```typescript
import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

export function setupRouterGuards(router: Router) {
  // 前置守卫
  router.beforeEach((to, from, next) => {
    const userStore = useUserStore()
    
    // 设置页面标题
    document.title = (to.meta.title as string) || '租号酷'
    
    // 检查是否需要认证
    if (to.meta.requiresAuth) {
      if (!userStore.token) {
        ElMessage.warning('请先登录')
        next({
          path: '/login',
          query: { redirect: to.fullPath }
        })
        return
      }
      
      // 检查角色权限
      if (to.meta.role && userStore.userInfo?.role !== to.meta.role) {
        ElMessage.error('无权限访问')
        next('/')
        return
      }
    }
    
    next()
  })
}
```

---

## 六、状态管理

### 6.1 用户状态

**src/stores/user.ts**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getUserInfo } from '@/api/user'
import type { UserInfo } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => userInfo.value?.role || '')

  // 设置 Token
  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  // 设置用户信息
  const setUserInfo = (info: UserInfo) => {
    userInfo.value = info
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    try {
      const res = await getUserInfo()
      userInfo.value = res.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    userRole,
    setToken,
    setUserInfo,
    fetchUserInfo,
    logout
  }
})
```

---

## 七、开发规范

### 7.1 代码规范

#### 命名规范
- **组件名**: PascalCase，如 `UserProfile.vue`
- **文件名**: kebab-case，如 `user-profile.vue`
- **变量/函数**: camelCase，如 `getUserInfo`
- **常量**: UPPER_SNAKE_CASE，如 `API_BASE_URL`
- **类型/接口**: PascalCase，如 `UserInfo`

#### 组件规范
```vue
<template>
  <div class="user-profile">
    <!-- 模板内容 -->
  </div>
</template>

<script setup lang="ts">
// 1. 导入依赖
import { ref, computed } from 'vue'
import { useUserStore } from '@/stores/user'

// 2. 定义 Props
interface Props {
  userId: number
}
const props = defineProps<Props>()

// 3. 定义 Emits
const emit = defineEmits<{
  update: [value: string]
}>()

// 4. 组合式函数
const userStore = useUserStore()

// 5. 响应式数据
const loading = ref(false)

// 6. 计算属性
const userName = computed(() => userStore.userInfo?.name)

// 7. 方法
const handleClick = () => {
  emit('update', 'value')
}
</script>

<style scoped lang="scss">
.user-profile {
  // 样式
}
</style>
```

### 7.2 Git 提交规范

使用 Conventional Commits 规范：

```
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 代码重构
perf: 性能优化
test: 测试相关
chore: 构建/工具相关
```

示例：
```bash
git commit -m "feat: 添加订单列表页面"
git commit -m "fix: 修复支付回调处理问题"
```

### 7.3 代码审查清单

- [ ] 代码符合 ESLint 规范
- [ ] 代码已格式化（Prettier）
- [ ] TypeScript 类型定义完整
- [ ] 组件有适当的注释
- [ ] 错误处理完善
- [ ] 性能优化考虑（如防抖、节流）
- [ ] 响应式设计适配

---

## 八、常见问题

### 8.1 跨域问题

开发环境通过 Vite 代理解决：

**vite.config.ts**
```typescript
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, '/api')
      }
    }
  }
})
```

### 8.2 环境变量使用

```typescript
// 在代码中使用环境变量
const apiBaseUrl = import.meta.env.VITE_API_BASE_URL
```

### 8.3 性能优化

1. **路由懒加载**: 使用动态 import
2. **组件懒加载**: 大组件使用 `defineAsyncComponent`
3. **图片优化**: 使用 WebP 格式，添加懒加载
4. **代码分割**: 合理使用 `splitChunks`

---

## 九、部署

### 9.1 构建

```bash
# 构建生产版本
npm run build

# 构建产物在 dist/ 目录
```

### 9.2 Docker 部署

**Dockerfile**
```dockerfile
FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

### 9.3 Nginx 配置

**nginx.conf**
```nginx
server {
    listen 80;
    server_name zuhaoku.com;
    
    root /usr/share/nginx/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://backend:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

---

## 十、参考资源

- [Vue 3 官方文档](https://cn.vuejs.org/)
- [TypeScript 官方文档](https://www.typescriptlang.org/)
- [Vite 官方文档](https://cn.vitejs.dev/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [Pinia 文档](https://pinia.vuejs.org/zh/)

---

**文档维护**: shigure  
**最后更新**: 2025

