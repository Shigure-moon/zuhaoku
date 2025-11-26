<template>
  <div class="account-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>可租账号</span>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="filter-section">
        <el-form :model="searchForm" inline>
          <el-form-item label="游戏">
            <el-select v-model="searchForm.gameId" placeholder="选择游戏" clearable style="width: 200px">
              <el-option
                v-for="game in gameList"
                :key="game.id"
                :label="game.name"
                :value="game.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索账号标题"
              style="width: 300px"
              clearable
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 账号列表 -->
      <el-row :gutter="20" v-loading="loading">
        <el-col
          v-for="account in accountList"
          :key="account.id"
          :xs="24"
          :sm="12"
          :md="8"
          :lg="6"
          style="margin-bottom: 20px"
        >
          <el-card class="account-card" shadow="hover">
            <div class="account-image">
              <el-image
                :src="account.images?.[0] || '/placeholder.png'"
                fit="cover"
                style="width: 100%; height: 150px"
              >
                <template #error>
                  <div class="image-slot">
                    <el-icon><Picture /></el-icon>
                  </div>
                </template>
              </el-image>
            </div>
            <div class="account-info">
              <h3 class="account-title">{{ account.title }}</h3>
              <p class="account-game">{{ account.gameName }}</p>
              <div class="account-price">
                <span class="price-label">时租：</span>
                <span class="price-value">¥{{ account.pricePerHour }}/小时</span>
              </div>
              <div class="account-actions">
                <el-button type="primary" size="small" @click="handleRent(account)">
                  立即租用
                </el-button>
                <el-button size="small" @click="handleViewDetail(account)">查看详情</el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && accountList.length === 0" description="暂无可租账号" />

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[12, 24, 48]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 创建订单对话框 -->
    <el-dialog
      v-model="dialogVisible"
      title="创建订单"
      width="500px"
      @close="handleDialogClose"
    >
      <el-form :model="orderForm" :rules="orderRules" ref="orderFormRef" label-width="100px">
        <el-form-item label="账号信息">
          <div v-if="currentAccount">
            <p><strong>{{ currentAccount.title }}</strong></p>
            <p style="color: #909399; font-size: 12px">{{ currentAccount.gameName }}</p>
          </div>
        </el-form-item>
        <el-form-item label="租期类型" prop="durationType">
          <el-radio-group v-model="orderForm.durationType" @change="handleDurationTypeChange">
            <el-radio label="MINUTE">按分钟</el-radio>
            <el-radio label="HOUR">按小时</el-radio>
            <el-radio label="OVERNIGHT">包夜</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item
          v-if="orderForm.durationType !== 'OVERNIGHT'"
          :label="orderForm.durationType === 'MINUTE' ? '租用时长（分钟）' : '租用时长（小时）'"
          prop="duration"
        >
          <el-input-number
            v-model="orderForm.duration"
            :min="1"
            :max="orderForm.durationType === 'MINUTE' ? 1440 : 24"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-radio-group v-model="orderForm.paymentMethod">
            <el-radio label="WECHAT">微信支付</el-radio>
            <el-radio label="ALIPAY">支付宝</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="费用明细" v-if="currentAccount">
          <div class="price-detail">
            <div class="price-item">
              <span>租金：</span>
              <span class="price-value">¥{{ calculateRent() }}</span>
            </div>
            <div class="price-item">
              <span>押金：</span>
              <span class="price-value">¥{{ currentAccount.deposit || 0 }}</span>
            </div>
            <div class="price-total">
              <span>总计：</span>
              <span class="price-value">¥{{ calculateTotal() }}</span>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="orderLoading" @click="handleCreateOrder">
          确认下单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import { getGameList, getAccountList } from '@/api/account'
import { createOrder } from '@/api/order'
import { createPayment } from '@/api/payment'
import type { Game, Account, AccountQueryParams } from '@/types/account'
import type { CreateOrderDTO } from '@/types/order'

// 搜索表单
const searchForm = ref<AccountQueryParams>({
  gameId: undefined,
  keyword: '',
})

// 数据
const gameList = ref<Game[]>([])
const accountList = ref<Account[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

// 订单对话框
const dialogVisible = ref(false)
const orderFormRef = ref<FormInstance>()
const orderLoading = ref(false)
const currentAccount = ref<Account | null>(null)
const orderForm = ref({
  duration: 30,
  durationType: 'MINUTE' as 'MINUTE' | 'HOUR' | 'OVERNIGHT',
  paymentMethod: 'WECHAT' as 'WECHAT' | 'ALIPAY',
})

const orderRules: FormRules = {
  duration: [
    { required: true, message: '请输入租用时长', trigger: 'blur' },
    { type: 'number', min: 1, message: '租用时长必须大于0', trigger: 'blur' },
  ],
  durationType: [{ required: true, message: '请选择租期类型', trigger: 'change' }],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
}

const router = useRouter()

// 加载游戏列表
const loadGameList = async () => {
  try {
    const res = await getGameList()
    gameList.value = res.data || []
  } catch (error) {
    console.error('加载游戏列表失败:', error)
  }
}

// 加载账号列表
const loadAccountList = async () => {
  loading.value = true
  try {
    const params: AccountQueryParams = {
      ...searchForm.value,
      page: currentPage.value,
      pageSize: pageSize.value,
      status: 'ONLINE',
    }

    const res = await getAccountList(params)
    accountList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载账号列表失败:', error)
    ElMessage.error('加载账号列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadAccountList()
}

// 重置
const handleReset = () => {
  searchForm.value = {
    gameId: undefined,
    keyword: '',
  }
  handleSearch()
}

// 租用账号
const handleRent = async (account: Account) => {
  dialogVisible.value = true
  currentAccount.value = account
  // 重置表单
  orderForm.duration = 30
  orderForm.durationType = 'MINUTE'
  orderForm.paymentMethod = 'WECHAT'
}

// 查看详情
const handleViewDetail = (account: Account) => {
  // TODO: 跳转到账号详情页
  ElMessage.info('账号详情功能开发中')
}

// 计算租金
const calculateRent = () => {
  if (!currentAccount.value) return 0

  const account = currentAccount.value
  if (orderForm.value.durationType === 'MINUTE') {
    // 按分钟：30分钟价格为时租的一半，按比例计算
    const pricePerMinute = (account.pricePerHour || 0) / 60
    return (pricePerMinute * orderForm.value.duration).toFixed(2)
  } else if (orderForm.value.durationType === 'HOUR') {
    // 按小时
    return ((account.pricePerHour || 0) * orderForm.value.duration).toFixed(2)
  } else if (orderForm.value.durationType === 'OVERNIGHT') {
    // 包夜：默认8倍时租
    return ((account.pricePerHour || 0) * 8).toFixed(2)
  }
  return 0
}

// 计算总金额
const calculateTotal = () => {
  const rent = parseFloat(calculateRent())
  const deposit = currentAccount.value?.deposit || 0
  return (rent + deposit).toFixed(2)
}

// 租期类型改变
const handleDurationTypeChange = () => {
  if (orderForm.value.durationType === 'OVERNIGHT') {
    orderForm.value.duration = 1
  } else if (orderForm.value.durationType === 'MINUTE') {
    orderForm.value.duration = 30
  } else {
    orderForm.value.duration = 1
  }
}

// 创建订单
const handleCreateOrder = async () => {
  if (!orderFormRef.value || !currentAccount.value) return

  await orderFormRef.value.validate(async (valid) => {
    if (valid) {
      orderLoading.value = true
      try {
        // 创建订单
        const orderData: CreateOrderDTO = {
          accountId: currentAccount.value.id,
          duration: orderForm.value.duration,
          durationType: orderForm.value.durationType,
        }

        const orderRes = await createOrder(orderData)
        const order = orderRes.data

        if (!order) {
          throw new Error('订单创建失败')
        }

        ElMessage.success('订单创建成功，请完成支付')

        // 创建支付记录
        try {
          const paymentRes = await createPayment({
            orderId: order.id,
            paymentType: orderForm.value.paymentMethod.toLowerCase() as 'wechat' | 'alipay',
          })

          const payment = paymentRes.data
          if (payment?.paymentUrl) {
            // 如果有支付URL，跳转到支付页面
            const paymentUrl = payment.paymentUrl
            // 确保使用完整的绝对URL（包含后端服务器地址和端口）
            // 注意：支付页面在后端服务器上（8081端口），不在前端开发服务器（3001端口）
            let fullPaymentUrl: string
            if (paymentUrl.startsWith('http')) {
              fullPaymentUrl = paymentUrl
            } else {
              // 支付页面必须访问后端服务器，不能使用前端代理
              // 从环境变量中获取后端服务器地址，或使用默认值
              const backendUrl = import.meta.env.VITE_BACKEND_URL || 'http://localhost:8081'
              fullPaymentUrl = `${backendUrl}${paymentUrl}`
            }
            
            console.log('支付URL:', fullPaymentUrl, '原始URL:', paymentUrl) // 调试用
            
            ElMessageBox.confirm('订单已创建，是否前往支付？', '提示', {
              confirmButtonText: '前往支付',
              cancelButtonText: '稍后支付',
              type: 'info',
            })
              .then(() => {
                // 跳转到支付页面
                window.location.href = fullPaymentUrl
              })
              .catch(() => {
                // 用户选择稍后支付，跳转到订单列表
                router.push('/tenant/orders')
              })
          } else {
            // 没有支付URL，直接跳转到订单列表
            ElMessage.warning('支付链接获取失败，请稍后在订单列表中完成支付')
            router.push('/tenant/orders')
          }
        } catch (paymentError) {
          console.error('创建支付失败:', paymentError)
          // 即使支付创建失败，订单也已创建，跳转到订单列表
          ElMessage.warning('订单已创建，但支付创建失败，请稍后在订单列表中完成支付')
          router.push('/tenant/orders')
        }

        dialogVisible.value = false
      } catch (error: any) {
        console.error('创建订单失败:', error)
        ElMessage.error(error.message || '创建订单失败，请重试')
      } finally {
        orderLoading.value = false
      }
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  orderFormRef.value?.resetFields()
  currentAccount.value = null
}

// 分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadAccountList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadAccountList()
}

onMounted(() => {
  loadGameList()
  loadAccountList()
})
</script>

<style scoped lang="scss">
.account-list {
  .card-header {
    font-weight: bold;
    font-size: 16px;
  }

  .filter-section {
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #e4e7ed;
  }

  .account-card {
    height: 100%;
    transition: transform 0.3s;

    &:hover {
      transform: translateY(-4px);
    }

    .account-image {
      margin-bottom: 12px;

      .image-slot {
        display: flex;
        justify-content: center;
        align-items: center;
        width: 100%;
        height: 150px;
        background: #f5f7fa;
        color: #909399;
      }
    }

    .account-info {
      .account-title {
        font-size: 16px;
        font-weight: bold;
        margin: 0 0 8px 0;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .account-game {
        font-size: 14px;
        color: #909399;
        margin: 0 0 8px 0;
      }

      .account-price {
        margin-bottom: 12px;

        .price-label {
          font-size: 12px;
          color: #909399;
        }

        .price-value {
          font-size: 18px;
          font-weight: bold;
          color: #f56c6c;
        }
      }

      .account-actions {
        display: flex;
        gap: 8px;
      }
    }
  }

  .pagination {
    margin-top: 24px;
    display: flex;
    justify-content: center;
  }

  .price-detail {
    padding: 12px;
    background: #f5f7fa;
    border-radius: 4px;

    .price-item {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 14px;
    }

    .price-total {
      display: flex;
      justify-content: space-between;
      margin-top: 8px;
      padding-top: 8px;
      border-top: 1px solid #e4e7ed;
      font-size: 16px;
      font-weight: bold;

      .price-value {
        color: #f56c6c;
        font-size: 18px;
      }
    }

    .price-value {
      color: #f56c6c;
      font-weight: bold;
    }
  }
}
</style>
