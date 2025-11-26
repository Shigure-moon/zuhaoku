<template>
  <div class="orders">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的订单</span>
          <el-select v-model="statusFilter" placeholder="筛选状态" style="width: 150px" @change="handleSearch">
            <el-option label="全部" value="" />
            <el-option label="待支付" value="PENDING_PAYMENT" />
            <el-option label="使用中" value="ACTIVE" />
            <el-option label="已归还" value="RETURNED" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </div>
      </template>

      <el-table :data="orderList" v-loading="loading" style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="180" />
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
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt ? formatDate(row.createdAt) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleViewDetail(row)">
              查看详情
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              link
              type="success"
              size="small"
              @click="handleRenew(row)"
            >
              续租
            </el-button>
            <el-button
              v-if="row.status === 'ACTIVE'"
              link
              type="warning"
              size="small"
              @click="handleReturn(row)"
            >
              还号
            </el-button>
            <el-button
              v-if="row.status === 'PENDING_PAYMENT'"
              link
              type="danger"
              size="small"
              @click="handleCancel(row)"
            >
              取消
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyOrders, renewOrder, returnOrder, cancelOrder } from '@/api/order'
import type { Order, OrderQueryParams } from '@/types/order'
import { formatDate } from '@/utils/format'

const router = useRouter()

// 筛选
const statusFilter = ref<Order['status'] | ''>('')

// 数据
const orderList = ref<Order[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 加载订单列表
const loadOrderList = async () => {
  loading.value = true
  try {
    const params: OrderQueryParams = {
      status: statusFilter.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    }

    const res = await getMyOrders(params)
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
    name: 'TenantOrderDetail',
    params: { id: order.id },
  })
}

// 续租
const handleRenew = async (order: Order) => {
  try {
    const { value: duration } = await ElMessageBox.prompt(
      '请输入续租时长（分钟）',
      '续租',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^[1-9]\d*$/,
        inputErrorMessage: '请输入有效的分钟数',
      }
    )

    await renewOrder({ 
      orderId: order.id, 
      duration: parseInt(duration),
      durationType: 'MINUTE' // 续租默认按分钟计算
    })
    ElMessage.success('续租成功')
    loadOrderList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('续租失败:', error)
      ElMessage.error('续租失败')
    }
  }
}

// 还号
const handleReturn = async (order: Order) => {
  try {
    await ElMessageBox.confirm('确认还号？还号后订单将进入结算流程。', '还号确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await returnOrder({ orderId: order.id })
    ElMessage.success('还号成功')
    loadOrderList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('还号失败:', error)
      ElMessage.error('还号失败')
    }
  }
}

// 取消订单
const handleCancel = async (order: Order) => {
  try {
    await ElMessageBox.confirm('确认取消订单？', '取消订单', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await cancelOrder(order.id)
    ElMessage.success('订单已取消')
    loadOrderList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消订单失败:', error)
      ElMessage.error('取消订单失败')
    }
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
}
</style>
