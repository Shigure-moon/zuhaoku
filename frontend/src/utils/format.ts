import dayjs from 'dayjs'

// 格式化日期
export const formatDate = (date: string | Date | null | undefined, format = 'YYYY-MM-DD HH:mm:ss'): string => {
  if (!date) return '-'
  try {
    const formatted = dayjs(date).format(format)
    // 检查格式化结果是否有效（dayjs 在无效日期时会返回 'Invalid Date'）
    if (formatted === 'Invalid Date' || !formatted) {
      return '-'
    }
    return formatted
  } catch (error) {
    console.error('日期格式化失败:', error, date)
    return '-'
  }
}

// 格式化金额
export const formatMoney = (amount: number): string => {
  return `¥${amount.toFixed(2)}`
}

// 格式化时长
export const formatDuration = (seconds: number): string => {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const secs = seconds % 60
  
  if (hours > 0) {
    return `${hours}小时${minutes}分钟`
  } else if (minutes > 0) {
    return `${minutes}分钟${secs}秒`
  } else {
    return `${secs}秒`
  }
}

