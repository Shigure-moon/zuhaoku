import request from './request'
import type {
  Order,
  OrderQueryParams,
  OrderListResponse,
  CreateOrderDTO,
  RenewOrderDTO,
  ReturnOrderDTO,
} from '@/types/order'

// 创建订单（租客端）
export const createOrder = (data: CreateOrderDTO) => {
  return request.post<Order>('/orders', data)
}

// 获取订单列表
export const getOrderList = (params?: OrderQueryParams) => {
  return request.get<OrderListResponse>('/orders', { params })
}

// 获取订单详情
export const getOrderDetail = (id: number) => {
  return request.get<Order>(`/orders/${id}`)
}

// 续租（租客端）
export const renewOrder = (data: RenewOrderDTO) => {
  return request.post<Order>(`/orders/${data.orderId}/renew`, {
    duration: data.duration,
    durationType: data.durationType || 'MINUTE' // 默认按分钟续租
  })
}

// 还号（租客端）
export const returnOrder = (data: ReturnOrderDTO) => {
  return request.post<Order>(`/orders/${data.orderId}/return`)
}

// 取消订单
export const cancelOrder = (id: number) => {
  return request.post<Order>(`/orders/${id}/cancel`)
}

// 获取我的订单列表（租客端）
export const getMyOrders = (params?: OrderQueryParams) => {
  return request.get<OrderListResponse>('/orders/my', { params })
}

// 获取商家订单列表（商家端）
export const getOwnerOrders = (params?: OrderQueryParams) => {
  return request.get<OrderListResponse>('/orders/owner', { params })
}

