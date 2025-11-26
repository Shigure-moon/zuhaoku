<template>
  <div class="order-detail">
    <el-card v-loading="loading">
      <template #header>
        <div class="card-header">
          <el-button @click="$router.back()" icon="ArrowLeft">返回</el-button>
          <span class="title">订单详情</span>
        </div>
      </template>

      <div v-if="order" class="detail-content">
        <!-- 订单基本信息 -->
        <el-descriptions title="订单信息" :column="2" border>
          <el-descriptions-item label="订单号">{{ order.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusType(order.status)">{{ getStatusText(order.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="游戏名称">{{ order.gameName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账号标题">{{ order.accountTitle || '-' }}</el-descriptions-item>
          <el-descriptions-item label="账号描述" :span="2">
            {{ order.accountDescription || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="租客">{{ order.tenantNickname || '-' }}</el-descriptions-item>
          <el-descriptions-item label="租期">
            {{ formatDuration(order.duration) }}
          </el-descriptions-item>
          <el-descriptions-item label="开始时间">
            {{ order.startTime ? formatDate(order.startTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="结束时间">
            {{ order.endTime ? formatDate(order.endTime) : '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="order.actualEndTime" label="实际结束时间">
            {{ formatDate(order.actualEndTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="order.remainingMinutes !== undefined && order.status === 'ACTIVE'" label="剩余时间">
            <span class="remaining-time">{{ formatRemainingTime(order.remainingMinutes) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 金额信息 -->
        <el-descriptions title="金额信息" :column="2" border style="margin-top: 20px">
          <el-descriptions-item label="租金">¥{{ order.amount?.toFixed(2) || '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="押金">¥{{ order.deposit?.toFixed(2) || '0.00' }}</el-descriptions-item>
          <el-descriptions-item label="总金额">
            <span class="total-amount">¥{{ order.totalAmount?.toFixed(2) || '0.00' }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 支付信息 -->
        <el-descriptions v-if="order.paymentType" title="支付信息" :column="2" border style="margin-top: 20px">
          <el-descriptions-item label="支付方式">
            {{ order.paymentType === 'wechat' ? '微信支付' : order.paymentType === 'alipay' ? '支付宝' : order.paymentType }}
          </el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="getPaymentStatusType(order.paymentStatus)">
              {{ getPaymentStatusText(order.paymentStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="order.paymentTime" label="支付时间">
            {{ formatDate(order.paymentTime) }}
          </el-descriptions-item>
          <el-descriptions-item v-if="order.transactionId" label="交易号">
            {{ order.transactionId }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 账号信息（商家可见） -->
        <el-descriptions
          v-if="order.accountLevel"
          title="账号信息"
          :column="2"
          border
          style="margin-top: 20px"
        >
          <el-descriptions-item label="账号等级">
            Lv.{{ order.accountLevel }}
          </el-descriptions-item>
          <el-descriptions-item v-if="order.accountSkins" label="皮肤信息">
            {{ formatSkins(order.accountSkins) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <el-empty v-else description="订单不存在" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getOrderDetail } from '@/api/order'
import type { Order } from '@/types/order'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const order = ref<Order | null>(null)

// 加载订单详情
const loadOrderDetail = async () => {
  const orderId = Number(route.params.id)
  if (!orderId) {
    ElMessage.error('订单ID无效')
    router.back()
    return
  }

  loading.value = true
  try {
    const res = await getOrderDetail(orderId)
    order.value = res.data
  } catch (error: any) {
    console.error('加载订单详情失败:', error)
    ElMessage.error(error.message || '加载订单详情失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 格式化时长
const formatDuration = (minutes: number) => {
  if (minutes < 60) {
    return `${minutes} 分钟`
  } else if (minutes < 1440) {
    return `${Math.floor(minutes / 60)} 小时`
  } else {
    return `${Math.floor(minutes / 1440)} 天`
  }
}

// 格式化剩余时间
const formatRemainingTime = (minutes: number) => {
  if (minutes <= 0) {
    return '已到期'
  }
  if (minutes < 60) {
    return `剩余 ${minutes} 分钟`
  } else if (minutes < 1440) {
    const hours = Math.floor(minutes / 60)
    const mins = minutes % 60
    return `剩余 ${hours} 小时 ${mins} 分钟`
  } else {
    const days = Math.floor(minutes / 1440)
    const hours = Math.floor((minutes % 1440) / 60)
    return `剩余 ${days} 天 ${hours} 小时`
  }
}

// 格式化皮肤信息
const formatSkins = (skins: string) => {
  try {
    const skinsArray = JSON.parse(skins)
    if (Array.isArray(skinsArray)) {
      return skinsArray.join('、')
    }
    return skins
  } catch {
    return skins
  }
}

// 获取状态类型
const getStatusType = (status: Order['status']) => {
  const typeMap: Record<Order['status'], string> = {
    PENDING_PAYMENT: 'warning',
    PAID: 'info',
    ACTIVE: 'success',
    EXPIRED: 'info',
    RETURNED: '',
    CANCELLED: 'info',
    DISPUTED: 'danger',
    COMPLETED: 'success',
  }
  return typeMap[status] || ''
}

// 获取状态文本
const getStatusText = (status: Order['status']) => {
  const textMap: Record<Order['status'], string> = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    ACTIVE: '使用中',
    EXPIRED: '已过期',
    RETURNED: '已归还',
    CANCELLED: '已取消',
    DISPUTED: '申诉中',
    COMPLETED: '已完成',
  }
  return textMap[status] || status
}

// 获取支付状态类型
const getPaymentStatusType = (status?: string) => {
  const typeMap: Record<string, string> = {
    pending: 'warning',
    success: 'success',
    failed: 'danger',
    refunded: 'info',
  }
  return typeMap[status || ''] || ''
}

// 获取支付状态文本
const getPaymentStatusText = (status?: string) => {
  const textMap: Record<string, string> = {
    pending: '待支付',
    success: '支付成功',
    failed: '支付失败',
    refunded: '已退款',
  }
  return textMap[status || ''] || status || '-'
}

onMounted(() => {
  loadOrderDetail()
})
</script>

<style scoped lang="scss">
.order-detail {
  padding: 20px;

  .card-header {
    display: flex;
    align-items: center;
    gap: 10px;

    .title {
      font-size: 18px;
      font-weight: 500;
    }
  }

  .detail-content {
    .remaining-time {
      color: #f56c6c;
      font-weight: 500;
    }

    .total-amount {
      color: #f56c6c;
      font-size: 18px;
      font-weight: 500;
    }
  }
}
</style>

