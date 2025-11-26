import request from './request'
import type { LoginParams, LoginResponse, UserInfo } from '@/types/api'

// 用户登录
export const login = (data: LoginParams) => {
  return request.post<LoginResponse>('/users/login', data)
}

// 用户注册
export const register = (data: { username: string; password: string; email: string }) => {
  return request.post('/users/register', data)
}

// 获取用户信息
export const getUserInfo = () => {
  return request.get<UserInfo>('/users/me')
}

// 更新用户信息
export const updateUserInfo = (data: Partial<UserInfo>) => {
  return request.put('/user/info', data)
}

