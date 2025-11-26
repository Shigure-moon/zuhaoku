import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { setupRouterGuards } from './guards'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home',
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/common/Login.vue'),
    meta: {
      title: '登录',
      requiresAuth: false,
    },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/common/Register.vue'),
    meta: {
      title: '注册',
      requiresAuth: false,
    },
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/common/Home.vue'),
    meta: {
      title: '首页',
      requiresAuth: false,
    },
  },
  {
    path: '/tenant',
    component: () => import('@/components/layout/BasicLayout.vue'),
    meta: {
      requiresAuth: true,
      role: 'tenant',
    },
    children: [
      {
        path: 'account-list',
        name: 'TenantAccountList',
        component: () => import('@/views/tenant/AccountList.vue'),
        meta: {
          title: '账号列表',
        },
      },
      {
        path: 'orders',
        name: 'TenantOrders',
        component: () => import('@/views/tenant/Orders.vue'),
        meta: {
          title: '我的订单',
        },
      },
      {
        path: 'orders/:id',
        name: 'TenantOrderDetail',
        component: () => import('@/views/tenant/OrderDetail.vue'),
        meta: {
          title: '订单详情',
        },
      },
    ],
  },
  {
    path: '/owner',
    component: () => import('@/components/layout/BasicLayout.vue'),
    meta: {
      requiresAuth: true,
      role: 'owner',
    },
    children: [
      {
        path: 'accounts',
        name: 'OwnerAccounts',
        component: () => import('@/views/owner/Accounts.vue'),
        meta: {
          title: '我的账号',
        },
      },
      {
        path: 'orders',
        name: 'OwnerOrders',
        component: () => import('@/views/owner/Orders.vue'),
        meta: {
          title: '订单管理',
        },
      },
      {
        path: 'orders/:id',
        name: 'OwnerOrderDetail',
        component: () => import('@/views/owner/OrderDetail.vue'),
        meta: {
          title: '订单详情',
        },
      },
    ],
  },
  {
    path: '/admin',
    component: () => import('@/components/layout/BasicLayout.vue'),
    meta: {
      requiresAuth: true,
      role: 'operator',
    },
    children: [
      {
        path: 'dashboard',
        name: 'AdminDashboard',
        component: () => import('@/views/admin/Dashboard.vue'),
        meta: {
          title: '工作台',
        },
      },
      {
        path: 'appeals',
        name: 'AdminAppeals',
        component: () => import('@/views/admin/Appeals.vue'),
        meta: {
          title: '申诉管理',
        },
      },
      {
        path: 'users',
        name: 'AdminUsers',
        component: () => import('@/views/admin/Users.vue'),
        meta: {
          title: '用户管理',
        },
      },
      {
        path: 'audit-logs',
        name: 'AdminAuditLogs',
        component: () => import('@/views/admin/AuditLogs.vue'),
        meta: {
          title: '日志审计',
        },
      },
      {
        path: 'risk',
        name: 'AdminRisk',
        component: () => import('@/views/admin/Risk.vue'),
        meta: {
          title: '风控管理',
        },
      },
    ],
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/common/Profile.vue'),
    meta: {
      title: '个人中心',
      requiresAuth: true,
    },
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: {
      title: '页面不存在',
    },
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 设置路由守卫
setupRouterGuards(router)

export default router

