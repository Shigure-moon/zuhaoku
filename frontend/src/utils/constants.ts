// 用户角色
export const USER_ROLES = {
  TENANT: 'tenant',
  OWNER: 'owner',
  OPERATOR: 'operator',
} as const

// 订单状态
export const ORDER_STATUS = {
  PAYING: 'paying',
  LEASING: 'leasing',
  CLOSED: 'closed',
  APPEAL: 'appeal',
  CANCELLED: 'cancelled',
} as const

// 账号状态
export const ACCOUNT_STATUS = {
  ON_SALE: 1,
  OFF_SALE: 2,
  LEASING: 3,
} as const

