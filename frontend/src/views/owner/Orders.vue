<template>
  <div class="orders">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>订单管理</span>
          <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 150px" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="PENDING_PAYMENT" />
            <el-option label="使用中" value="ACTIVE" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="申诉中" value="DISPUTED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </div>
      </template>

      <el-table :data="orderList" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="tenantNickname" label="租客" width="120" />
        <el-table-column prop="accountTitle" label="账号" width="200" />
        <el-table-column prop="gameName" label="游戏" width="120" />
        <el-table-column label="租期" width="150">
          <template #default="{ row }">
            <span>{{ formatDuration(row.duration) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">
            <span class="amount">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="{ row }">
            {{ row.startTime ? formatDate(row.startTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="结束时间" width="180">
          <template #default="{ row }">
            {{ row.endTime ? formatDate(row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? formatDate(row.createdAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="canAppeal(row)"
              link
              type="danger"
              size="small"
              @click="handleCreateAppeal(row)"
            >
              申诉
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && orderList.length === 0" description="暂无订单" />

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 创建申诉对话框 -->
    <el-dialog v-model="showAppealDialog" title="创建申诉" width="600px">
      <el-form :model="appealForm" :rules="appealRules" ref="appealFormRef" label-width="120px">
        <el-form-item label="订单号">
          <el-input :value="currentOrder?.orderNo" disabled />
        </el-form-item>
        <el-form-item label="申诉类型" prop="type">
          <el-select v-model="appealForm.type" placeholder="请选择申诉类型" style="width: 100%">
            <el-option label="玩家恶意使用/销毁资源" :value="4" />
            <el-option label="买家脚本盗号" :value="5" />
            <el-option label="押金争议" :value="2" />
            <el-option label="其他" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="申诉说明">
          <el-input
            v-model="appealForm.description"
            type="textarea"
            :rows="4"
            :placeholder="getAppealPlaceholder()"
            maxlength="500"
            show-word-limit
          />
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
        <el-button type="primary" :loading="appealLoading" @click="handleSubmitAppeal">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getOwnerOrders } from '@/api/order'
import { createAppeal } from '@/api/appeal'
import { useUserStore } from '@/stores/user'
import type { Order, OrderQueryParams } from '@/types/order'
import type { UploadFile, UploadFiles } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { formatDate } from '@/utils/format'

const router = useRouter()
const userStore = useUserStore()

// 筛选
const statusFilter = ref<Order['status'] | ''>('')

// 数据
const orderList = ref<Order[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 申诉相关
const showAppealDialog = ref(false)
const appealFormRef = ref<FormInstance>()
const appealLoading = ref(false)
const currentOrder = ref<Order | null>(null)
const appealForm = ref({
  type: undefined as number | undefined,
  description: '',
})
const fileList = ref<UploadFiles>([])
const evidenceUrls = ref<string[]>([])

const appealRules: FormRules = {
  type: [{ required: true, message: '请选择申诉类型', trigger: 'change' }],
}

const uploadAction = computed(() => {
  const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1'
  return `${baseUrl}/upload`
})

const uploadHeaders = computed(() => {
  const token = userStore.token
  return token ? { Authorization: `Bearer ${token}` } : {}
})

// 加载订单列表
const loadOrderList = async () => {
  loading.value = true
  try {
    const params: OrderQueryParams = {
      status: statusFilter.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    }

    const res = await getOwnerOrders(params)
    if (res && res.data) {
      orderList.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      orderList.value = []
      total.value = 0
    }
  } catch (error: any) {
    console.error('加载订单列表失败:', error)
    ElMessage.error('加载订单列表失败: ' + (error?.message || '未知错误'))
    orderList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadOrderList()
}

// 格式化时长
const formatDuration = (minutes: number | null | undefined) => {
  if (!minutes || minutes <= 0) {
    return '-'
  }
  if (minutes < 60) {
    return `${minutes} 分钟`
  } else if (minutes < 1440) {
    return `${Math.floor(minutes / 60)} 小时`
  } else {
    return `${Math.floor(minutes / 1440)} 天`
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

// 查看详情
const handleViewDetail = (order: Order) => {
  router.push({
    name: 'OwnerOrderDetail',
    params: { id: order.id },
  })
}

// 判断是否可以申诉
const canAppeal = (order: Order) => {
  // 订单状态为使用中、已归还、已完成时可以申诉
  // 且订单状态不是申诉中
  return (
    (order.status === 'ACTIVE' || order.status === 'RETURNED' || order.status === 'COMPLETED') &&
    order.status !== 'DISPUTED'
  )
}

// 获取申诉说明占位符
const getAppealPlaceholder = () => {
  if (appealForm.value.type === 4) {
    return '请详细描述玩家恶意使用或销毁资源的情况，包括时间、具体行为等'
  } else if (appealForm.value.type === 5) {
    return '请详细描述买家使用脚本盗号的情况，包括发现时间、异常行为、账号变化等，并提供相关证据'
  } else {
    return '请详细描述申诉原因和具体情况'
  }
}

// 创建申诉
const handleCreateAppeal = (order: Order) => {
  currentOrder.value = order
  appealForm.value = {
    type: undefined,
    description: '',
  }
  fileList.value = []
  evidenceUrls.value = []
  showAppealDialog.value = true
}

// 文件上传成功
const handleUploadSuccess = (response: any, file: UploadFile) => {
  if (response && response.data && response.data.url) {
    evidenceUrls.value.push(response.data.url)
    ElMessage.success('文件上传成功')
  } else {
    ElMessage.error('文件上传失败：响应格式错误')
  }
}

// 文件上传失败
const handleUploadError = (error: Error) => {
  console.error('文件上传失败:', error)
  ElMessage.error('文件上传失败: ' + error.message)
}

// 文件移除
const handleFileRemove = (file: UploadFile) => {
  if (file.response && file.response.data && file.response.data.url) {
    const index = evidenceUrls.value.indexOf(file.response.data.url)
    if (index > -1) {
      evidenceUrls.value.splice(index, 1)
    }
  }
}

// 上传前验证
const beforeUpload = (file: File) => {
  const isValidType = file.type.startsWith('image/') || file.type.startsWith('video/')
  const isLt10M = file.size / 1024 / 1024 < 10

  if (!isValidType) {
    ElMessage.error('只能上传图片或视频文件!')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

// 提交申诉
const handleSubmitAppeal = async () => {
  if (!appealFormRef.value) return

  try {
    await appealFormRef.value.validate()
  } catch (error) {
    return
  }

  if (!currentOrder.value) {
    ElMessage.error('订单信息不存在')
    return
  }

  if (!appealForm.value.type) {
    ElMessage.warning('请选择申诉类型')
    return
  }

  appealLoading.value = true
  try {
    await createAppeal({
      orderId: currentOrder.value.id,
      type: appealForm.value.type,
      evidenceUrls: evidenceUrls.value,
    })
    ElMessage.success('申诉提交成功，管理员将尽快处理')
    showAppealDialog.value = false
    // 重新加载订单列表
    loadOrderList()
  } catch (error: any) {
    console.error('提交申诉失败:', error)
    ElMessage.error('提交申诉失败: ' + (error?.message || '未知错误'))
  } finally {
    appealLoading.value = false
  }
}

// 分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadOrderList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadOrderList()
}

onMounted(() => {
  try {
    loadOrderList()
  } catch (error) {
    console.error('Orders 组件初始化失败:', error)
    ElMessage.error('页面加载失败，请刷新重试')
  }
})
</script>

<style scoped lang="scss">
.orders {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
    font-size: 16px;
  }

  .amount {
    font-weight: bold;
    color: #f56c6c;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }

  .upload-tip {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
  }
}
</style>
