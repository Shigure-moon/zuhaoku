import request from './request'
import type {
  Account,
  AccountQueryParams,
  AccountListResponse,
  CreateAccountDTO,
  UpdateAccountDTO,
} from '@/types/account'
import type { Game } from '@/types/account'

// 获取游戏列表
export const getGameList = () => {
  return request.get<Game[]>('/games')
}

// 获取账号列表（租客端 - 可租账号）
export const getAccountList = (params?: AccountQueryParams) => {
  return request.get<AccountListResponse>('/accounts', { params })
}

// 获取账号详情
export const getAccountDetail = (id: number) => {
  return request.get<Account>(`/accounts/${id}`)
}

// 创建账号（商家端）
export const createAccount = (data: CreateAccountDTO) => {
  return request.post<Account>('/accounts', data)
}

// 更新账号（商家端）
export const updateAccount = (id: number, data: UpdateAccountDTO) => {
  return request.put<Account>(`/accounts/${id}`, data)
}

// 删除账号（商家端）
export const deleteAccount = (id: number) => {
  return request.delete(`/accounts/${id}`)
}

// 上架/下架账号（商家端）
export const toggleAccountStatus = (id: number, status: 'ONLINE' | 'OFFLINE') => {
  return request.patch(`/accounts/${id}/status`, { status })
}

// 获取我的账号列表（商家端）
export const getMyAccounts = (params?: AccountQueryParams) => {
  return request.get<AccountListResponse>('/accounts/my', { params })
}

