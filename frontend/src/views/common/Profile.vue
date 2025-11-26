<template>
  <div class="profile">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <!-- 个人信息 -->
        <el-tab-pane label="个人信息" name="info">
          <div class="info-section">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="昵称">
                {{ userStore.userInfo?.nickname || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="手机号">
                {{ userStore.userInfo?.mobile || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="角色">
                <el-tag :type="getRoleType(userStore.userInfo?.role)">
                  {{ getRoleText(userStore.userInfo?.role) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="注册时间">
                {{ userStore.userInfo?.createdAt ? formatDate(userStore.userInfo.createdAt) : '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-tab-pane>

        <!-- 商家端：我的账号 -->
        <el-tab-pane v-if="isOwner" label="我的账号" name="accounts">
          <div class="accounts-section">
            <div class="section-header">
              <el-button type="primary" @click="handleGoToAccounts">管理账号</el-button>
            </div>
            <el-table :data="accountList" v-loading="loading" style="width: 100%">
              <el-table-column prop="title" label="标题" width="200" />
              <el-table-column prop="gameName" label="游戏" width="120" />
              <el-table-column prop="pricePerHour" label="时租" width="100">
                <template #default="{ row }">
                  <span class="price">¥{{ row.pricePerHour }}</span>
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
              <el-table-column label="操作" width="150" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleViewAccount(row)">
                    查看
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!loading && accountList.length === 0" description="暂无账号" />
            <div class="pagination" v-if="total > 0">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[5, 10, 20]"
                layout="total, sizes, prev, pager, next"
                @size-change="handleSizeChange"
                @current-change="handlePageChange"
              />
            </div>
          </div>
        </el-tab-pane>

        <!-- 租客端：我的订单 -->
        <el-tab-pane v-if="isTenant" label="我的订单" name="orders">
          <div class="orders-section">
            <div class="section-header">
              <el-button type="primary" @click="handleGoToOrders">查看全部订单</el-button>
            </div>
            <el-table :data="orderList" v-loading="loading" style="width: 100%">
              <el-table-column prop="orderNo" label="订单号" width="180" />
              <el-table-column prop="accountTitle" label="账号" width="200" />
              <el-table-column prop="totalAmount" label="金额" width="100">
                <template #default="{ row }">
                  <span class="amount">¥{{ row.totalAmount }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getOrderStatusType(row.status)">{{ getOrderStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="createdAt" label="创建时间" width="180">
                <template #default="{ row }">
                  {{ row.createdAt ? formatDate(row.createdAt) : '-' }}
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="!loading && orderList.length === 0" description="暂无订单" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getMyAccounts } from '@/api/account'
import { getMyOrders } from '@/api/order'
import type { Account } from '@/types/account'
import type { Order } from '@/types/order'
import { formatDate } from '@/utils/format'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref('info')
const loading = ref(false)

// 账号相关
const accountList = ref<Account[]>([])
const accountTotal = ref(0)
const accountPage = ref(1)
const accountPageSize = ref(5)

// 订单相关
const orderList = ref<Order[]>([])

const isOwner = computed(() => {
  const role = userStore.userInfo?.role?.toUpperCase()
  return role === 'OWNER'
})

const isTenant = computed(() => {
  const role = userStore.userInfo?.role?.toUpperCase()
  return role === 'TENANT'
})

const total = computed(() => {
  if (activeTab.value === 'accounts') {
    return accountTotal.value
  }
  return 0
})

const currentPage = computed({
  get: () => (activeTab.value === 'accounts' ? accountPage.value : 1),
  set: (val) => {
    if (activeTab.value === 'accounts') {
      accountPage.value = val
    }
  },
})

const pageSize = computed({
  get: () => (activeTab.value === 'accounts' ? accountPageSize.value : 10),
  set: (val) => {
    if (activeTab.value === 'accounts') {
      accountPageSize.value = val
    }
  },
})

// 加载账号列表
const loadAccountList = async () => {
  if (!isOwner.value) return
  
  loading.value = true
  try {
    const res = await getMyAccounts({
      page: accountPage.value,
      pageSize: accountPageSize.value,
    })
    if (res && res.data) {
      accountList.value = res.data.list || []
      accountTotal.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error)
    ElMessage.error('加载账号列表失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// 加载订单列表
const loadOrderList = async () => {
  if (!isTenant.value) return
  
  loading.value = true
  try {
    const res = await getMyOrders({
      page: 1,
      pageSize: 5,
    })
    if (res && res.data) {
      orderList.value = res.data.list || []
    }
  } catch (error: any) {
    console.error('加载订单列表失败:', error)
    ElMessage.error('加载订单列表失败: ' + (error?.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

// Tab切换
const handleTabChange = (name: string) => {
  if (name === 'accounts' && accountList.value.length === 0) {
    loadAccountList()
  } else if (name === 'orders' && orderList.value.length === 0) {
    loadOrderList()
  }
}

// 跳转到账号管理
const handleGoToAccounts = () => {
  router.push('/owner/accounts')
}

// 跳转到订单管理
const handleGoToOrders = () => {
  router.push('/tenant/orders')
}

// 查看账号
const handleViewAccount = (account: Account) => {
  router.push('/owner/accounts')
}

// 获取角色类型
const getRoleType = (role?: string) => {
  if (!role) return ''
  const roleUpper = role.toUpperCase()
  if (roleUpper === 'TENANT') return 'success'
  if (roleUpper === 'OWNER') return 'warning'
  if (roleUpper === 'OPERATOR') return 'danger'
  return ''
}

// 获取角色文本
const getRoleText = (role?: string) => {
  if (!role) return '未知'
  const roleUpper = role.toUpperCase()
  if (roleUpper === 'TENANT') return '租客'
  if (roleUpper === 'OWNER') return '商家'
  if (roleUpper === 'OPERATOR') return '管理员'
  return role
}

// 获取状态类型
const getStatusType = (status?: string) => {
  if (status === 'ONLINE') return 'success'
  if (status === 'OFFLINE') return 'info'
  if (status === 'RENTED') return 'warning'
  return ''
}

// 获取状态文本
const getStatusText = (status?: string) => {
  if (status === 'ONLINE') return '在线'
  if (status === 'OFFLINE') return '离线'
  if (status === 'RENTED') return '已租出'
  return status || '-'
}

// 获取订单状态类型
const getOrderStatusType = (status?: string) => {
  const typeMap: Record<string, string> = {
    PENDING_PAYMENT: 'warning',
    PAID: 'info',
    ACTIVE: 'success',
    EXPIRED: 'info',
    RETURNED: '',
    CANCELLED: 'info',
    DISPUTED: 'danger',
    COMPLETED: 'success',
  }
  return typeMap[status || ''] || ''
}

// 获取订单状态文本
const getOrderStatusText = (status?: string) => {
  const textMap: Record<string, string> = {
    PENDING_PAYMENT: '待支付',
    PAID: '已支付',
    ACTIVE: '使用中',
    EXPIRED: '已过期',
    RETURNED: '已归还',
    CANCELLED: '已取消',
    DISPUTED: '申诉中',
    COMPLETED: '已完成',
  }
  return textMap[status || ''] || status || '-'
}

// 分页
const handlePageChange = (page: number) => {
  if (activeTab.value === 'accounts') {
    accountPage.value = page
    loadAccountList()
  }
}

const handleSizeChange = (size: number) => {
  if (activeTab.value === 'accounts') {
    accountPageSize.value = size
    accountPage.value = 1
    loadAccountList()
  }
}

onMounted(() => {
  // 根据角色设置默认Tab
  if (isOwner.value) {
    activeTab.value = 'accounts'
    loadAccountList()
  } else if (isTenant.value) {
    activeTab.value = 'orders'
    loadOrderList()
  }
})
</script>

<style scoped lang="scss">
.profile {
  .card-header {
    font-weight: bold;
    font-size: 16px;
  }

  .info-section {
    padding: 20px 0;
  }

  .accounts-section,
  .orders-section {
    .section-header {
      margin-bottom: 20px;
      display: flex;
      justify-content: flex-end;
    }

    .price,
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
}
</style>

