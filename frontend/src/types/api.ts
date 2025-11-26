// API 响应基础类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 登录参数
export interface LoginParams {
  mobile: string  // 后端使用 mobile 字段
  password: string
}

// 登录响应（后端返回的 data 字段内容）
export interface LoginResponse {
  token: string
  expiresIn?: number
  userInfo?: UserInfo
}

// 用户信息
export interface UserInfo {
  userId?: number  // 后端返回 userId
  id?: number      // 兼容字段
  nickname?: string
  mobile?: string
  phone?: string
  email?: string
  role?: 'TENANT' | 'OWNER' | 'OPERATOR' | 'tenant' | 'owner' | 'operator'
  avatarUrl?: string
  avatar?: string
  zhimaScore?: number
  creditScore?: number
  createdAt?: string
}

