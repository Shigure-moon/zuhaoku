<template>
  <div class="admin-dashboard">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalAccounts }}</div>
            <div class="stat-label">总账号数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ stats.totalOrders }}</div>
            <div class="stat-label">总订单数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <div class="stat-content">
            <div class="stat-value">{{ stats.pendingAppeals }}</div>
            <div class="stat-label">待处理申诉</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近申诉</span>
              <el-button type="text" @click="$router.push('/admin/appeals')">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentAppeals" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="orderId" label="订单ID" width="100" />
            <el-table-column prop="type" label="类型" width="150">
              <template #default="{ row }">
                <el-tag v-if="row.type === 1">账号异常</el-tag>
                <el-tag v-else-if="row.type === 2" type="warning">押金争议</el-tag>
                <el-tag v-else-if="row.type === 4" type="danger">玩家恶意使用/销毁资源</el-tag>
                <el-tag v-else-if="row.type === 5" type="danger">买家脚本盗号</el-tag>
                <el-tag v-else type="info">其他</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="120">
              <template #default="{ row }">
                <el-tag v-if="!row.verdict" type="warning">待处理</el-tag>
                <el-tag v-else-if="row.verdict === 1" type="success">支持租客</el-tag>
                <el-tag v-else-if="row.verdict === 2" type="danger">支持号主</el-tag>
                <el-tag v-else type="info">各担一半</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="resolveTime" label="处理时间" width="180">
              <template #default="{ row }">
                {{ row.resolveTime ? formatDate(row.resolveTime) : '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleViewAppeal(row)">
                  查看
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>最近订单</span>
              <el-button type="text">查看全部</el-button>
            </div>
          </template>
          <el-table :data="recentOrders" v-loading="loading" style="width: 100%">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="accountId" label="账号ID" width="100" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'paying'" type="warning">待支付</el-tag>
                <el-tag v-else-if="row.status === 'leasing'" type="success">租赁中</el-tag>
                <el-tag v-else-if="row.status === 'closed'" type="info">已关闭</el-tag>
                <el-tag v-else-if="row.status === 'appeal'" type="danger">申诉中</el-tag>
                <el-tag v-else>已取消</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminStats, getRecentAppeals, getRecentOrders } from '@/api/admin'
import { formatDate } from '@/utils/format'

const router = useRouter()

// 统计数据
const stats = ref({
  totalUsers: 0,
  totalAccounts: 0,
  totalOrders: 0,
  pendingAppeals: 0,
})

const recentAppeals = ref<any[]>([])
const recentOrders = ref<any[]>([])
const loading = ref(false)

// 加载统计数据
const loadStats = async () => {
  loading.value = true
  try {
    const response = await getAdminStats()
    if (response.code === 200 && response.data) {
      stats.value = response.data
    }
  } catch (error) {
    console.error('加载统计数据失败:', error)
    ElMessage.error('加载统计数据失败')
  } finally {
    loading.value = false
  }
}

// 加载最近申诉
const loadRecentAppeals = async () => {
  try {
    const response = await getRecentAppeals(10)
    if (response.code === 200 && response.data) {
      recentAppeals.value = response.data.map((appeal: any) => ({
        id: appeal.id,
        orderId: appeal.orderId,
        type: appeal.type,
        verdict: appeal.verdict,
        createTime: appeal.createTime || appeal.createdAt,
        resolveTime: appeal.resolveTime,
      }))
    }
  } catch (error) {
    console.error('加载最近申诉失败:', error)
  }
}

// 查看申诉详情
const handleViewAppeal = (appeal: any) => {
  router.push({
    path: '/admin/appeals',
    query: { appealId: appeal.id },
  })
}

// 加载最近订单
const loadRecentOrders = async () => {
  try {
    const response = await getRecentOrders(10)
    if (response.code === 200 && response.data) {
      recentOrders.value = response.data.map((order: any) => ({
        id: order.id,
        accountId: order.accountId,
        status: order.status,
        amount: order.amount,
        createdAt: order.createdAt,
      }))
    }
  } catch (error) {
    console.error('加载最近订单失败:', error)
  }
}

onMounted(() => {
  loadStats()
  loadRecentAppeals()
  loadRecentOrders()
})
</script>

<style scoped lang="scss">
.admin-dashboard {
  .stat-card {
    .stat-content {
      text-align: center;
      padding: 20px 0;

      .stat-value {
        font-size: 32px;
        font-weight: bold;
        color: #409eff;
        margin-bottom: 8px;
      }

      .stat-label {
        font-size: 14px;
        color: #909399;
      }
    }
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}
</style>

