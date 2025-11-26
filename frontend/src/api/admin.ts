import request from './request'

// 统计数据
export interface AdminStats {
  totalUsers: number
  totalAccounts: number
  totalOrders: number
  pendingAppeals: number
}

// 用户信息（管理员视图）
export interface AdminUser {
  userId: number
  nickname: string
  mobile: string
  role: string
  zhimaScore?: number
  status: number
  createdAt: string
  updatedAt: string
}

// 用户列表响应
export interface UserListResponse {
  list: AdminUser[]
  total: number
  page: number
  pageSize: number
}

// 获取统计数据
export const getAdminStats = () => {
  return request.get<AdminStats>('/admin/stats')
}

// 获取用户列表（管理员）
export const getAdminUserList = (params?: {
  mobile?: string
  role?: string
  status?: number
  page?: number
  pageSize?: number
}) => {
  return request.get<UserListResponse>('/admin/users', { params })
}

// 更新用户状态（冻结/解冻）
export const updateUserStatus = (userId: number, status: number) => {
  return request.put<string>(`/admin/users/${userId}/status`, null, {
    params: { status }
  })
}

// 获取最近订单（管理员）
export const getRecentOrders = (limit: number = 10) => {
  return request.get<any[]>('/admin/orders/recent', {
    params: { limit }
  })
}

// 获取最近申诉（管理员）
export const getRecentAppeals = (limit: number = 10) => {
  return request.get<any[]>('/admin/appeals/recent', {
    params: { limit }
  })
}

// 日志审计相关类型
export interface AuditLog {
  id: number
  userId?: number
  username?: string
  role?: string
  action: string
  resourceType?: string
  resourceId?: number
  description?: string
  requestMethod?: string
  requestPath?: string
  requestParams?: string
  responseStatus?: number
  ipAddress?: string
  userAgent?: string
  success: number
  errorMessage?: string
  executionTime?: number
  createdAt: string
}

export interface AuditLogQueryParams {
  userId?: number
  username?: string
  role?: string
  action?: string
  resourceType?: string
  resourceId?: number
  success?: number
  startTime?: string
  endTime?: string
  page?: number
  pageSize?: number
}

export interface AuditLogListResponse {
  records: AuditLog[]
  total: number
  current: number
  size: number
}

// 查询日志列表
export const getAuditLogs = (params?: AuditLogQueryParams) => {
  return request.get<AuditLogListResponse>('/admin/audit-logs', { params })
}

