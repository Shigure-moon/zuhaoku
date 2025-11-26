// 支付相关类型定义

// 支付方式
export type PaymentType = 'wechat' | 'alipay'

// 支付状态
export type PaymentStatus = 'pending' | 'success' | 'failed' | 'refunded'

// 支付信息
export interface Payment {
  id: number
  orderId: number
  paymentType: PaymentType
  amount: number
  status: PaymentStatus
  transactionId?: string
  paymentUrl?: string
  qrCode?: string
  createdAt: string
  paidAt?: string
}

// 创建支付请求
export interface CreatePaymentDTO {
  orderId: number
  paymentType: PaymentType
}

