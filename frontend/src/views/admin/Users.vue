<template>
  <div class="admin-users">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="filter-section">
        <el-form :model="searchForm" inline>
          <el-form-item label="手机号">
            <el-input v-model="searchForm.mobile" placeholder="请输入手机号" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="角色">
            <el-select v-model="searchForm.role" placeholder="选择角色" clearable style="width: 150px">
              <el-option label="租客" value="TENANT" />
              <el-option label="商家" value="OWNER" />
              <el-option label="运营" value="OPERATOR" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 150px">
              <el-option label="正常" :value="1" />
              <el-option label="冻结" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 用户列表 -->
      <el-table :data="userList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="mobile" label="手机号" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'TENANT'">租客</el-tag>
            <el-tag v-else-if="row.role === 'OWNER'" type="warning">商家</el-tag>
            <el-tag v-else type="success">运营</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="zhimaScore" label="芝麻信用分" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">正常</el-tag>
            <el-tag v-else type="danger">冻结</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="handleToggleStatus(row)" v-if="row.status === 1">
              冻结
            </el-button>
            <el-button type="success" size="small" @click="handleToggleStatus(row)" v-else>
              解冻
            </el-button>
          </template>
        </el-table-column>
      </el-table>

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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminUserList, updateUserStatus } from '@/api/admin'
import type { AdminUser } from '@/api/admin'

const searchForm = ref({
  mobile: undefined,
  role: undefined,
  status: undefined,
})

const userList = ref<AdminUser[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const loadUserList = async () => {
  loading.value = true
  try {
    const response = await getAdminUserList({
      mobile: searchForm.value.mobile,
      role: searchForm.value.role,
      status: searchForm.value.status,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    if (response.code === 200 && response.data) {
      userList.value = response.data.list || []
      total.value = response.data.total || 0
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
    ElMessage.error('加载用户列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadUserList()
}

const handleReset = () => {
  searchForm.value = {
    mobile: undefined,
    role: undefined,
    status: undefined,
  }
  handleSearch()
}

const handleToggleStatus = async (row: AdminUser) => {
  try {
    const action = row.status === 1 ? '冻结' : '解冻'
    await ElMessageBox.confirm(`确认${action}该用户？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })

    const newStatus = row.status === 1 ? 2 : 1
    const response = await updateUserStatus(row.userId, newStatus)
    if (response.code === 200) {
      ElMessage.success(response.data || `${action}成功`)
      loadUserList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('操作失败:', error)
      ElMessage.error('操作失败')
    }
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadUserList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadUserList()
}

onMounted(() => {
  loadUserList()
})
</script>

<style scoped lang="scss">
.admin-users {
  .filter-section {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>

