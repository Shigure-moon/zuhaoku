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
          <el-descriptions-item label="号主">{{ order.ownerNickname || '-' }}</el-descriptions-item>
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

        <!-- 账号信息（仅租客可见，且订单状态为使用中或已完成） -->
        <el-descriptions
          v-if="order.username && (order.status === 'ACTIVE' || order.status === 'COMPLETED')"
          title="账号信息"
          :column="2"
          border
          style="margin-top: 20px"
        >
          <el-descriptions-item label="账号">
            <el-input v-model="order.username" readonly>
              <template #append>
                <el-button @click="copyToClipboard(order.username!)" icon="DocumentCopy">复制</el-button>
              </template>
            </el-input>
          </el-descriptions-item>
          <el-descriptions-item label="密码">
            <el-input v-model="order.password" type="password" readonly>
              <template #append>
                <el-button @click="copyToClipboard(order.password!)" icon="DocumentCopy">复制</el-button>
              </template>
            </el-input>
          </el-descriptions-item>
          <el-descriptions-item v-if="order.accountLevel" label="账号等级">
            Lv.{{ order.accountLevel }}
          </el-descriptions-item>
          <el-descriptions-item v-if="order.accountSkins" label="皮肤信息">
            {{ formatSkins(order.accountSkins) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 操作按钮 -->
        <div class="actions" style="margin-top: 30px">
          <el-button
            v-if="order.status === 'PENDING_PAYMENT'"
            type="primary"
            @click="handleContinuePayment"
            :loading="paymentLoading"
          >
            继续支付
          </el-button>
          <el-button
            v-if="order.status === 'ACTIVE'"
            type="success"
            @click="handleRenew"
          >
            续租
          </el-button>
          <el-button
            v-if="order.status === 'ACTIVE'"
            type="warning"
            @click="handleReturn"
          >
            还号
          </el-button>
          <el-button
            v-if="order.status === 'PENDING_PAYMENT'"
            type="danger"
            @click="handleCancel"
          >
            取消订单
          </el-button>
          <el-button
            v-if="order.status === 'ACTIVE' || order.status === 'COMPLETED'"
            type="warning"
            @click="showAppealDialog = true"
          >
            创建申诉
          </el-button>
        </div>
      </div>

      <el-empty v-else description="订单不存在" />
    </el-card>

    <!-- 创建申诉对话框 -->
    <el-dialog v-model="showAppealDialog" title="创建申诉" width="600px">
      <el-form :model="appealForm" :rules="appealRules" ref="appealFormRef" label-width="100px">
        <el-form-item label="订单ID">
          <el-input :value="order?.id" disabled />
        </el-form-item>
        <el-form-item label="申诉类型" prop="type">
          <el-select v-model="appealForm.type" placeholder="请选择申诉类型" style="width: 100%">
            <el-option label="账号异常" :value="1" />
            <el-option label="押金争议" :value="2" />
            <el-option label="其他" :value="3" />
            <el-option label="玩家恶意使用/销毁资源" :value="4" />
            <el-option label="买家脚本盗号" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item label="证据上传">
          <el-upload
            v-model:file-list="fileList"
            :action="uploadAction"
            :headers="uploadHeaders"
            :on-success="handleUploadSuccess"
            :on-error="handleUploadError"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
            list-type="picture-card"
            :limit="5"
            accept="image/*,video/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">支持上传图片和视频，最多5个文件，单个文件不超过10MB</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAppealDialog = false">取消</el-button>
        <el-button type="primary" :loading="appealLoading" @click="handleCreateAppeal">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getOrderDetail, renewOrder, returnOrder, cancelOrder } from '@/api/order'
import { createAppeal } from '@/api/appeal'
import { createPayment } from '@/api/payment'
import { useUserStore } from '@/stores/user'
import type { Order } from '@/types/order'
import type { UploadFile, UploadFiles } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const loading = ref(false)
const order = ref<Order | null>(null)

// 支付相关
const paymentLoading = ref(false)

// 申诉相关
const showAppealDialog = ref(false)
const appealFormRef = ref<FormInstance>()
const appealLoading = ref(false)
const appealForm = ref({
  type: undefined as number | undefined,
})
const fileList = ref<UploadFiles>([])
const evidenceUrls = ref<string[]>([])

const appealRules: FormRules = {
  type: [{ required: true, message: '请选择申诉类型', trigger: 'change' }],
}

const uploadAction = computed(() => {
  const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1'
  return `${baseURL}/files/upload`
})

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`,
}))

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

// 复制到剪贴板
const copyToClipboard = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败')
  }
}

// 续租
const handleRenew = () => {
  router.push({
    name: 'TenantOrders',
    query: { renew: order.value?.id },
  })
}

// 还号
const handleReturn = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm('确认还号？还号后订单将完成。', '确认还号', {
      type: 'warning',
    })

    await returnOrder({ orderId: order.value.id })
    ElMessage.success('还号成功')
    await loadOrderDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('还号失败:', error)
      ElMessage.error(error.message || '还号失败')
    }
  }
}

// 继续支付
const handleContinuePayment = async () => {
  if (!order.value) return

  paymentLoading.value = true
  try {
    // 如果订单已有支付方式，使用相同的支付方式；否则默认使用微信支付
    const paymentType = (order.value.paymentType || 'wechat') as 'wechat' | 'alipay'

    const paymentRes = await createPayment({
      orderId: order.value.id,
      paymentType,
    })

    if (paymentRes.data?.paymentUrl) {
      // 如果有支付URL，打开新窗口或显示支付对话框
      ElMessageBox.confirm('订单支付已创建，是否前往支付？', '提示', {
        confirmButtonText: '前往支付',
        cancelButtonText: '稍后支付',
        type: 'info',
      })
        .then(() => {
          // 打开支付URL
          window.open(paymentRes.data.paymentUrl, '_blank')
          // 刷新订单详情
          setTimeout(() => {
            loadOrderDetail()
          }, 2000)
        })
        .catch(() => {
          // 用户选择稍后支付，刷新订单详情
          loadOrderDetail()
        })
    } else if (paymentRes.data?.qrCode) {
      // 如果有二维码，显示二维码对话框
      ElMessageBox.alert(
        `<div style="text-align: center;"><img src="${paymentRes.data.qrCode}" style="max-width: 300px;" /></div>`,
        '请使用手机扫描二维码支付',
        {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '已支付',
          type: 'info',
        }
      ).then(() => {
        // 刷新订单详情
        loadOrderDetail()
      })
    } else {
      ElMessage.warning('支付创建成功，但未获取到支付链接，请稍后刷新页面查看')
      // 刷新订单详情
      loadOrderDetail()
    }
  } catch (error: any) {
    console.error('创建支付失败:', error)
    ElMessage.error(error.message || '创建支付失败')
  } finally {
    paymentLoading.value = false
  }
}

// 取消订单
const handleCancel = async () => {
  if (!order.value) return

  try {
    await ElMessageBox.confirm('确认取消订单？', '确认取消', {
      type: 'warning',
    })

    await cancelOrder(order.value.id)
    ElMessage.success('订单已取消')
    await loadOrderDetail()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error(error.message || '取消订单失败')
    }
  }
}

// 文件上传前验证
const beforeUpload = (file: File) => {
  const isValidType = file.type.startsWith('image/') || file.type.startsWith('video/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('只能上传图片或视频文件')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过10MB')
    return false
  }
  return true
}

// 文件上传成功
const handleUploadSuccess = (response: any, file: UploadFile) => {
  if (response.code === 200 && response.data) {
    evidenceUrls.value.push(response.data)
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error(response.message || '文件上传失败')
  }
}

// 文件上传失败
const handleUploadError = (error: any) => {
  console.error('文件上传失败:', error)
  ElMessage.error('文件上传失败')
}

// 文件移除
const handleFileRemove = (file: UploadFile) => {
  const url = file.response?.data || file.url
  if (url) {
    const index = evidenceUrls.value.indexOf(url)
    if (index > -1) {
      evidenceUrls.value.splice(index, 1)
    }
  }
}

// 创建申诉
const handleCreateAppeal = async () => {
  if (!order.value) return

  if (!appealFormRef.value) return
  await appealFormRef.value.validate()

  if (!appealForm.value.type) {
    ElMessage.warning('请选择申诉类型')
    return
  }

  appealLoading.value = true
  try {
    const response = await createAppeal({
      orderId: order.value.id,
      type: appealForm.value.type,
      evidenceUrls: evidenceUrls.value,
    })

    if (response.code === 200) {
      ElMessage.success('申诉创建成功')
      showAppealDialog.value = false
      // 重置表单
      appealForm.value = { type: undefined }
      fileList.value = []
      evidenceUrls.value = []
      await loadOrderDetail()
    }
  } catch (error: any) {
    console.error('创建申诉失败:', error)
    ElMessage.error(error.message || '创建申诉失败')
  } finally {
    appealLoading.value = false
  }
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

  .upload-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 10px;
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

    .actions {
      display: flex;
      gap: 10px;
    }
  }
}
</style>

