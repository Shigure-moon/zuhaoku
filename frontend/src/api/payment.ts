import request from './request'
import type { CreatePaymentDTO, Payment } from '@/types/payment'

// 创建支付
export const createPayment = (data: CreatePaymentDTO) => {
  return request.post<Payment>('/payments', data)
}

// 查询支付状态
export const getPaymentStatus = (id: number) => {
  return request.get<Payment>(`/payments/${id}/status`)
}

