<template>
  <div class="accounts">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>我的账号</span>
          <el-button type="primary" @click="handleAdd">添加账号</el-button>
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
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 150px">
              <el-option label="在线" value="ONLINE" />
              <el-option label="离线" value="OFFLINE" />
              <el-option label="已租出" value="RENTED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 账号列表 -->
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
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              v-if="row.status === 'ONLINE'"
              link
              type="warning"
              size="small"
              @click="handleToggleStatus(row, 'OFFLINE')"
            >
              下架
            </el-button>
            <el-button
              v-if="row.status === 'OFFLINE'"
              link
              type="success"
              size="small"
              @click="handleToggleStatus(row, 'ONLINE')"
            >
              上架
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && accountList.length === 0" description="暂无账号" />

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

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="游戏" prop="gameId">
          <el-select v-model="form.gameId" placeholder="选择游戏" style="width: 100%">
            <el-option
              v-for="game in gameList"
              :key="game.id"
              :label="game.name"
              :value="game.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入账号标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            placeholder="请输入账号描述"
          />
        </el-form-item>
        <el-form-item label="时租" prop="pricePerHour">
          <el-input-number v-model="form.pricePerHour" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="游戏账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="游戏密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getGameList, getMyAccounts, createAccount, updateAccount, deleteAccount, toggleAccountStatus } from '@/api/account'
import type { Game, Account, AccountQueryParams, CreateAccountDTO, UpdateAccountDTO } from '@/types/account'
import { formatDate } from '@/utils/format'

// 搜索表单
const searchForm = ref<AccountQueryParams>({
  gameId: undefined,
  status: undefined,
})

// 数据
const gameList = ref<Game[]>([])
const accountList = ref<Account[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('添加账号')
const formRef = ref<FormInstance>()
const form = reactive<CreateAccountDTO & { id?: number }>({
  gameId: 0,
  title: '',
  description: '',
  pricePerHour: 0,
  username: '',
  password: '',
})

const rules: FormRules = {
  gameId: [{ required: true, message: '请选择游戏', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  pricePerHour: [{ required: true, message: '请输入时租', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

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
    }

    const res = await getMyAccounts(params)
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
    status: undefined,
  }
  handleSearch()
}

// 获取状态类型
const getStatusType = (status: Account['status']) => {
  const typeMap: Record<Account['status'], string> = {
    ONLINE: 'success',
    OFFLINE: 'info',
    RENTED: 'warning',
    BANNED: 'danger',
  }
  return typeMap[status] || ''
}

// 获取状态文本
const getStatusText = (status: Account['status']) => {
  const textMap: Record<Account['status'], string> = {
    ONLINE: '在线',
    OFFLINE: '离线',
    RENTED: '已租出',
    BANNED: '已封禁',
  }
  return textMap[status] || status
}

// 添加账号
const handleAdd = () => {
  dialogTitle.value = '添加账号'
  Object.assign(form, {
    gameId: 0,
    title: '',
    description: '',
    pricePerHour: 0,
    username: '',
    password: '',
  })
  dialogVisible.value = true
}

// 编辑账号
const handleEdit = (account: Account) => {
  dialogTitle.value = '编辑账号'
  Object.assign(form, {
    id: account.id,
    gameId: account.gameId,
    title: account.title,
    description: account.description || '',
    pricePerHour: account.pricePerHour,
    username: '',
    password: '',
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      try {
        if (form.id) {
          // 更新
          const updateData: UpdateAccountDTO = {
            title: form.title,
            description: form.description,
            pricePerHour: form.pricePerHour,
          }
          await updateAccount(form.id, updateData)
          ElMessage.success('更新成功')
        } else {
          // 创建
          const createData: CreateAccountDTO = {
            gameId: form.gameId,
            title: form.title,
            description: form.description,
            pricePerHour: form.pricePerHour,
            username: form.username,
            password: form.password,
          }
          await createAccount(createData)
          ElMessage.success('添加成功')
        }
        dialogVisible.value = false
        loadAccountList()
      } catch (error) {
        console.error('操作失败:', error)
        ElMessage.error('操作失败')
      }
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
}

// 切换状态
const handleToggleStatus = async (account: Account, status: 'ONLINE' | 'OFFLINE') => {
  try {
    await toggleAccountStatus(account.id, status)
    ElMessage.success('操作成功')
    loadAccountList()
  } catch (error) {
    console.error('操作失败:', error)
    ElMessage.error('操作失败')
  }
}

// 删除账号
const handleDelete = async (account: Account) => {
  try {
    await ElMessageBox.confirm('确认删除该账号？', '删除确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    await deleteAccount(account.id)
    ElMessage.success('删除成功')
    loadAccountList()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
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

onMounted(() => {
  loadGameList()
  loadAccountList()
})
</script>

<style scoped lang="scss">
.accounts {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-weight: bold;
    font-size: 16px;
  }

  .filter-section {
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #e4e7ed;
  }

  .price {
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
