// 账号相关类型定义

// 游戏信息
export interface Game {
  id: number
  name: string
  icon?: string
  category?: string
}

// 账号信息
export interface Account {
  id: number
  gameId: number
  gameName?: string
  gameIcon?: string
  ownerId: number
  ownerNickname?: string
  title: string
  description?: string
  pricePerHour: number
  pricePerDay?: number
  pricePerNight?: number
  deposit?: number
  status: 'ONLINE' | 'OFFLINE' | 'RENTED' | 'BANNED'
  level?: number
  rank?: string
  region?: string
  tags?: string[]
  images?: string[]
  createdAt: string
  updatedAt: string
}

// 账号查询参数
export interface AccountQueryParams {
  gameId?: number
  keyword?: string
  minPrice?: number
  maxPrice?: number
  status?: string
  page?: number
  pageSize?: number
  sortBy?: 'price' | 'createdAt' | 'popularity'
  sortOrder?: 'asc' | 'desc'
}

// 账号列表响应
export interface AccountListResponse {
  list: Account[]
  total: number
  page: number
  pageSize: number
}

// 创建账号请求
export interface CreateAccountDTO {
  gameId: number
  title: string
  description?: string
  pricePerHour: number
  pricePerDay?: number
  pricePerNight?: number
  level?: number
  rank?: string
  region?: string
  tags?: string[]
  images?: string[]
  username: string
  password: string
}

// 更新账号请求
export interface UpdateAccountDTO {
  title?: string
  description?: string
  pricePerHour?: number
  pricePerDay?: number
  pricePerNight?: number
  status?: string
  tags?: string[]
  images?: string[]
}

