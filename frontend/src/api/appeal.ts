import service from './request'
import type { ApiResponse } from '@/types/api'

export interface Appeal {
  id: number
  orderId: number
  type: number // 1-账号异常, 2-押金争议, 3-其他, 4-玩家恶意使用/销毁资源, 5-买家脚本盗号
  typeName?: string
  evidenceUrls?: string[]
  verdict?: number // 1-支持租客, 2-支持号主, 3-各担一半
  verdictName?: string
  operatorName?: string
  createTime?: string
  resolveTime?: string
}

export interface CreateAppealDTO {
  orderId: number
  type: number
  evidenceUrls?: string[]
}

export interface ResolveAppealDTO {
  verdict: number
}

export interface AppealListResponse {
  list: Appeal[]
  total: number
  page: number
  pageSize: number
}

/**
 * 创建申诉
 */
export function createAppeal(data: CreateAppealDTO): Promise<ApiResponse<Appeal>> {
  return service.post('/appeals', data)
}

/**
 * 获取申诉列表
 */
export function getAppealList(params: {
  status?: string
  page?: number
  pageSize?: number
}): Promise<ApiResponse<AppealListResponse>> {
  return service.get('/appeals', { params })
}

/**
 * 获取申诉详情
 */
export function getAppealDetail(id: number): Promise<ApiResponse<Appeal>> {
  return service.get(`/appeals/${id}`)
}

/**
 * 处理申诉（管理员）
 */
export function resolveAppeal(id: number, data: ResolveAppealDTO): Promise<ApiResponse<Appeal>> {
  return service.post(`/appeals/${id}/resolve`, data)
}

