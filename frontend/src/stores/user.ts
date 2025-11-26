import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { UserInfo } from '@/types/user'
import { login, getUserInfo } from '@/api/user'
import { removeToken, setToken } from '@/utils/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  // 登录
  const userLogin = async (mobile: string, password: string) => {
    try {
      const res = await login({ mobile, password })
      // 后端返回格式：{ code: 200, message: "登录成功", data: { token: "...", ... } }
      if (res.data && res.data.token) {
        token.value = res.data.token
        setToken(res.data.token)
        if (res.data.userInfo) {
          userInfo.value = res.data.userInfo
        } else {
          await fetchUserInfo()
        }
      }
      return res
    } catch (error) {
      throw error
    }
  }

  // 获取用户信息
  const fetchUserInfo = async () => {
    try {
      const res = await getUserInfo()
      if (res && res.data) {
        userInfo.value = res.data
        console.log('获取用户信息成功:', res.data)
      } else {
        console.warn('获取用户信息返回数据格式异常:', res)
      }
      return res.data
    } catch (error) {
      console.error('获取用户信息失败:', error)
      throw error
    }
  }

  // 退出登录
  const logout = () => {
    token.value = ''
    userInfo.value = null
    removeToken()
  }

  // 初始化用户信息（从本地存储恢复）
  const initUserInfo = () => {
    const localToken = localStorage.getItem('token')
    if (localToken) {
      token.value = localToken
      fetchUserInfo()
    }
  }

  return {
    token,
    userInfo,
    userLogin,
    fetchUserInfo,
    logout,
    initUserInfo,
  }
})

