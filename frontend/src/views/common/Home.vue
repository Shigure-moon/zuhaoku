<template>
  <div class="home-container">
    <el-container>
      <el-header class="home-header">
        <div class="header-content">
          <h1 class="title">租号酷</h1>
          <div class="actions">
            <el-button v-if="!userStore.token" @click="$router.push('/login')">登录</el-button>
            <el-button v-if="!userStore.token" type="primary" @click="$router.push('/register')">
              注册
            </el-button>
            <el-dropdown v-else @command="handleCommand">
              <span class="user-info">
                <el-avatar :size="32" :src="userStore.userInfo?.avatarUrl">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <span class="username">{{ userStore.userInfo?.nickname || '用户' }}</span>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="dashboard">进入工作台</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>
      </el-header>
      <el-main class="home-main">
        <!-- 已登录用户提示 -->
        <div class="user-tip-section" v-if="userStore.token">
          <el-alert
            :title="getUserTipTitle()"
            type="info"
            :closable="false"
            show-icon
          >
            <template #default>
              <div class="tip-content">
                <span>{{ getUserTipMessage() }}</span>
                <el-button type="primary" size="small" @click="goToDashboard" style="margin-left: 12px">
                  进入工作台
                </el-button>
              </div>
            </template>
          </el-alert>
        </div>

        <!-- 搜索区域 -->
        <div class="search-section">
          <el-card shadow="never">
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
                  placeholder="搜索账号标题、描述"
                  style="width: 300px"
                  clearable
                />
              </el-form-item>
              <el-form-item label="价格">
                <el-input-number
                  v-model="searchForm.minPrice"
                  :min="0"
                  placeholder="最低价"
                  style="width: 120px"
                />
                <span style="margin: 0 8px">-</span>
                <el-input-number
                  v-model="searchForm.maxPrice"
                  :min="0"
                  placeholder="最高价"
                  style="width: 120px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
                <el-button @click="handleReset">重置</el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </div>

        <!-- 账号列表 -->
        <div class="account-list-section">
          <el-card shadow="never">
            <template #header>
              <div class="card-header">
                <span>可租账号</span>
                <el-select v-model="sortBy" placeholder="排序方式" style="width: 150px" @change="handleSearch">
                  <el-option label="价格从低到高" value="price_asc" />
                  <el-option label="价格从高到低" value="price_desc" />
                  <el-option label="最新发布" value="createdAt_desc" />
                </el-select>
              </div>
            </template>

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
                <el-card
                  class="account-card"
                  shadow="hover"
                  @click="handleViewAccount(account)"
                  style="cursor: pointer"
                >
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
                    <div class="account-tags" v-if="account.tags && account.tags.length > 0">
                      <el-tag
                        v-for="tag in account.tags.slice(0, 3)"
                        :key="tag"
                        size="small"
                        style="margin-right: 4px"
                      >
                        {{ tag }}
                      </el-tag>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <el-empty v-if="!loading && accountList.length === 0" description="暂无可租账号，请稍后再试" />

            <!-- 分页 -->
            <div class="pagination" v-if="total > 0">
              <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[12, 24, 48, 96]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handlePageChange"
              />
            </div>
          </el-card>
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessageBox, ElMessage } from 'element-plus'
import { User, Picture } from '@element-plus/icons-vue'
import { getGameList, getAccountList } from '@/api/account'
import type { Game, Account, AccountQueryParams } from '@/types/account'

const router = useRouter()
const userStore = useUserStore()

// 搜索表单
const searchForm = ref<AccountQueryParams>({
  gameId: undefined,
  keyword: '',
  minPrice: undefined,
  maxPrice: undefined,
})

const sortBy = ref('price_asc')

// 数据
const gameList = ref<Game[]>([])
const accountList = ref<Account[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(12)

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

    // 处理排序
    if (sortBy.value === 'price_asc') {
      params.sortBy = 'price'
      params.sortOrder = 'asc'
    } else if (sortBy.value === 'price_desc') {
      params.sortBy = 'price'
      params.sortOrder = 'desc'
    } else if (sortBy.value === 'createdAt_desc') {
      params.sortBy = 'createdAt'
      params.sortOrder = 'desc'
    }

    const res = await getAccountList(params)
    if (res && res.data) {
      accountList.value = res.data.list || []
      total.value = res.data.total || 0
      console.log('账号列表加载成功:', { count: accountList.value.length, total: total.value })
    } else {
      accountList.value = []
      total.value = 0
      console.warn('账号列表返回数据格式异常:', res)
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error)
    ElMessage.error('加载账号列表失败: ' + (error?.message || '未知错误'))
    accountList.value = []
    total.value = 0
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
    minPrice: undefined,
    maxPrice: undefined,
  }
  sortBy.value = 'price_asc'
  handleSearch()
}

// 查看账号详情
const handleViewAccount = (account: Account) => {
  // 如果用户已登录且是租客，跳转到租客账号列表页面（可以下单）
  if (userStore.token && userStore.userInfo?.role?.toUpperCase() === 'TENANT') {
    router.push('/tenant/account-list')
  } else if (userStore.token) {
    // 其他角色也跳转到对应的工作台
    goToDashboard()
  } else {
    // 未登录用户，提示登录
    ElMessageBox.confirm('查看账号详情需要登录，是否前往登录？', '提示', {
      confirmButtonText: '去登录',
      cancelButtonText: '取消',
      type: 'info',
    }).then(() => {
      router.push('/login')
    })
  }
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

// 获取用户提示标题
const getUserTipTitle = () => {
  const role = userStore.userInfo?.role?.toUpperCase()
  if (role === 'TENANT') {
    return '欢迎回来，租客！'
  } else if (role === 'OWNER') {
    return '欢迎回来，商家！'
  } else if (role === 'OPERATOR') {
    return '欢迎回来，管理员！'
  }
  return '欢迎回来！'
}

// 获取用户提示消息
const getUserTipMessage = () => {
  const role = userStore.userInfo?.role?.toUpperCase()
  if (role === 'TENANT') {
    return '您可以浏览和租赁账号，或查看您的订单'
  } else if (role === 'OWNER') {
    return '您可以管理您的账号和订单'
  } else if (role === 'OPERATOR') {
    return '您可以管理申诉、用户和系统数据'
  }
  return '进入您的工作台开始使用'
}

// 跳转到工作台
const goToDashboard = () => {
  const role = userStore.userInfo?.role?.toUpperCase()
  if (role === 'TENANT') {
    router.push('/tenant/account-list')
  } else if (role === 'OWNER') {
    router.push('/owner/accounts')
  } else if (role === 'OPERATOR') {
    router.push('/admin/dashboard')
  }
}

// 用户操作
const handleCommand = (command: string) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    }).then(() => {
      userStore.logout()
      router.push('/login')
    })
  } else if (command === 'dashboard') {
    goToDashboard()
  }
}

onMounted(() => {
  loadGameList()
  loadAccountList()
})
</script>

<style scoped lang="scss">
.home-container {
  min-height: 100vh;
  background: #f5f7fa;

  .home-header {
    background: #fff;
    border-bottom: 1px solid #e4e7ed;
    padding: 0;

    .header-content {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 100%;
      padding: 0 24px;

      .title {
        margin: 0;
        color: #409eff;
        font-size: 24px;
        font-weight: bold;
      }

      .actions {
        display: flex;
        align-items: center;
        gap: 12px;

        .user-info {
          display: flex;
          align-items: center;
          gap: 8px;
          cursor: pointer;
          padding: 4px 12px;
          border-radius: 4px;
          transition: background 0.3s;

          &:hover {
            background: #f5f7fa;
          }

          .username {
            font-size: 14px;
          }
        }
      }
    }
  }

  .home-main {
    max-width: 1400px;
    margin: 0 auto;
    padding: 24px;

    .user-tip-section {
      margin-bottom: 20px;

      .tip-content {
        display: flex;
        align-items: center;
        justify-content: space-between;
      }
    }

    .search-section {
      margin-bottom: 24px;
    }

    .account-list-section {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        font-weight: bold;
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
            margin-bottom: 8px;

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

          .account-tags {
            margin-top: 8px;
          }
        }
      }

      .pagination {
        margin-top: 24px;
        display: flex;
        justify-content: center;
      }
    }
  }
}
</style>
