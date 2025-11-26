import request from './request'

// 登录记录
export interface LoginRecord {
  id: number
  userId: number
  ipAddress: string
  country?: string
  province?: string
  city?: string
  latitude?: number
  longitude?: number
  deviceFingerprint?: string
  userAgent?: string
  loginTime: string
  isSuspicious: number
  riskLevel: number
}

// 异常行为
export interface AbnormalBehavior {
  id: number
  userId?: number
  behaviorType: string
  description?: string
  riskScore: number
  resourceType?: string
  resourceId?: number
  ipAddress?: string
  deviceFingerprint?: string
  status: number
  handledBy?: number
  handledAt?: string
  createdAt: string
}

// 黑名单
export interface Blacklist {
  id: number
  type: string
  value: string
  reason?: string
  riskLevel: number
  status: number
  createdBy?: number
  createdAt: string
  expiresAt?: string
}

// 登录记录列表响应
export interface LoginRecordListResponse {
  records: LoginRecord[]
  total: number
  current: number
  size: number
}

// 异常行为列表响应
export interface AbnormalBehaviorListResponse {
  records: AbnormalBehavior[]
  total: number
  current: number
  size: number
}

// 黑名单列表响应
export interface BlacklistListResponse {
  records: Blacklist[]
  total: number
  current: number
  size: number
}

// 用户风控统计
export interface UserRiskStats {
  loginCount: number
  suspiciousLoginCount: number
  abnormalBehaviorCount: number
}

// 获取登录记录列表
export const getLoginRecords = (params?: {
  userId?: number
  ipAddress?: string
  riskLevel?: number
  page?: number
  pageSize?: number
}) => {
  return request.get<LoginRecordListResponse>('/admin/risk/login-records', { params })
}

// 获取异常行为列表
export const getAbnormalBehaviors = (params?: {
  userId?: number
  behaviorType?: string
  status?: number
  page?: number
  pageSize?: number
}) => {
  return request.get<AbnormalBehaviorListResponse>('/admin/risk/abnormal-behaviors', { params })
}

// 获取黑名单列表
export const getBlacklist = (params?: {
  type?: string
  status?: number
  page?: number
  pageSize?: number
}) => {
  return request.get<BlacklistListResponse>('/admin/risk/blacklist', { params })
}

// 添加黑名单
export const addToBlacklist = (data: {
  type: string
  value: string
  reason?: string
  riskLevel?: number
  expiresAt?: string
}) => {
  return request.post<void>('/admin/risk/blacklist', data)
}

// 移除黑名单
export const removeFromBlacklist = (id: number) => {
  return request.delete<void>(`/admin/risk/blacklist/${id}`)
}

// 处理异常行为
export const handleAbnormalBehavior = (id: number, action: number) => {
  return request.post<void>(`/admin/risk/abnormal-behaviors/${id}/handle`, { action })
}

// 获取用户风控统计
export const getUserRiskStats = (userId: number) => {
  return request.get<UserRiskStats>(`/admin/risk/stats/${userId}`)
}

