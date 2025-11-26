// 订单相关类型定义

// 订单状态
export type OrderStatus =
  | 'PENDING_PAYMENT' // 待支付
  | 'PAID' // 已支付
  | 'ACTIVE' // 使用中
  | 'EXPIRED' // 已过期
  | 'RETURNED' // 已归还
  | 'CANCELLED' // 已取消
  | 'DISPUTED' // 申诉中
  | 'COMPLETED' // 已完成

// 订单信息
export interface Order {
  id: number
  orderNo: string
  tenantId: number
  tenantNickname?: string
  accountId: number
  accountTitle?: string
  accountDescription?: string
  gameName?: string
  ownerId: number
  ownerNickname?: string
  duration: number // 租期（分钟）
  startTime?: string
  endTime?: string
  actualEndTime?: string
  amount?: number // 租金
  deposit?: number // 押金
  totalAmount: number
  status: OrderStatus
  paymentType?: string // 支付方式：wechat, alipay
  paymentStatus?: string // 支付状态：pending, success, failed, refunded
  paymentTime?: string
  transactionId?: string // 第三方交易号
  username?: string // 账号（仅订单详情返回，租客可见）
  password?: string // 密码（仅订单详情返回，租客可见）
  accountLevel?: number // 账号等级
  accountSkins?: string // 账号皮肤（JSON字符串）
  remainingMinutes?: number // 剩余分钟数（租赁中订单）
  createdAt: string
  updatedAt: string
}

// 订单查询参数
export interface OrderQueryParams {
  status?: OrderStatus
  page?: number
  pageSize?: number
  startDate?: string
  endDate?: string
}

// 订单列表响应
export interface OrderListResponse {
  list: Order[]
  total: number
  page: number
  pageSize: number
}

// 创建订单请求
export interface CreateOrderDTO {
  accountId: number
  duration: number // 租期时长
  durationType: 'MINUTE' | 'HOUR' | 'OVERNIGHT' // 租期类型
}

// 续租请求
export interface RenewOrderDTO {
  orderId: number
  duration: number // 续租时长
  durationType?: 'MINUTE' | 'HOUR' | 'OVERNIGHT' // 租期类型，默认为 MINUTE
}

// 还号请求
export interface ReturnOrderDTO {
  orderId: number
}

