import type { Router } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, from, next) => {
    const userStore = useUserStore()

    // 设置页面标题
    if (to.meta.title) {
      document.title = `${to.meta.title} - ${import.meta.env.VITE_APP_TITLE}`
    }

    // 检查是否需要认证
    if (to.meta.requiresAuth) {
      if (!userStore.token) {
        ElMessage.warning('请先登录')
        next({
          path: '/login',
          query: { redirect: to.fullPath },
        })
        return
      }

      // 检查角色权限（支持大小写不敏感）
      if (to.meta.role) {
        const routeRole = to.meta.role.toLowerCase()
        const userRole = userStore.userInfo?.role?.toLowerCase()
        
        // 调试日志
        console.log('路由守卫 - 角色检查:', {
          path: to.path,
          routeRole,
          userRole,
          userInfo: userStore.userInfo
        })
        
        if (!userRole) {
          console.warn('用户角色信息缺失，尝试重新获取用户信息')
          // 如果用户信息缺失，尝试重新获取
          try {
            await userStore.fetchUserInfo()
            const updatedRole = userStore.userInfo?.role?.toLowerCase()
            if (updatedRole !== routeRole) {
              ElMessage.error('无权限访问')
              next('/home')
              return
            }
          } catch (error) {
            console.error('获取用户信息失败:', error)
            ElMessage.error('获取用户信息失败，请重新登录')
            next({
              path: '/login',
              query: { redirect: to.fullPath },
            })
            return
          }
        } else if (userRole !== routeRole) {
          console.warn('角色不匹配:', { userRole, routeRole })
          ElMessage.error('无权限访问')
          next('/home')
          return
        }
      }
    }

    // 已登录用户访问登录/注册页，重定向到首页
    if ((to.path === '/login' || to.path === '/register') && userStore.token) {
      next('/home')
      return
    }

    next()
  })

  router.afterEach(() => {
    // 路由切换后的处理
  })
}

