<template>
  <div class="risk-management">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 登录记录 -->
      <el-tab-pane label="登录记录" name="login-records">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>登录记录</span>
            </div>
          </template>

          <!-- 搜索表单 -->
          <el-form :model="loginSearchForm" inline class="search-form">
            <el-form-item label="用户ID">
              <el-input
                v-model.number="loginSearchForm.userId"
                placeholder="请输入用户ID"
                clearable
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item label="IP地址">
              <el-input
                v-model="loginSearchForm.ipAddress"
                placeholder="请输入IP地址"
                clearable
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item label="风险等级">
              <el-select
                v-model="loginSearchForm.riskLevel"
                placeholder="请选择风险等级"
                clearable
                style="width: 150px"
              >
                <el-option label="正常" :value="0" />
                <el-option label="低" :value="1" />
                <el-option label="中" :value="2" />
                <el-option label="高" :value="3" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadLoginRecords">搜索</el-button>
              <el-button @click="resetLoginSearch">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 登录记录列表 -->
          <el-table :data="loginRecordList" v-loading="loginLoading" style="width: 100%" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="ipAddress" label="IP地址" width="150" />
            <el-table-column prop="country" label="国家" width="100" />
            <el-table-column prop="province" label="省份" width="100" />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="isSuspicious" label="可疑" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.isSuspicious === 1" type="danger" size="small">是</el-tag>
                <el-tag v-else type="success" size="small">否</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="riskLevel" label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.riskLevel === 0" type="success" size="small">正常</el-tag>
                <el-tag v-else-if="row.riskLevel === 1" type="info" size="small">低</el-tag>
                <el-tag v-else-if="row.riskLevel === 2" type="warning" size="small">中</el-tag>
                <el-tag v-else-if="row.riskLevel === 3" type="danger" size="small">高</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="loginTime" label="登录时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.loginTime) }}
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination" v-if="loginTotal > 0">
            <el-pagination
              v-model:current-page="loginCurrentPage"
              v-model:page-size="loginPageSize"
              :total="loginTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleLoginSizeChange"
              @current-change="handleLoginPageChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 异常行为 -->
      <el-tab-pane label="异常行为" name="abnormal-behaviors">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>异常行为</span>
            </div>
          </template>

          <!-- 搜索表单 -->
          <el-form :model="behaviorSearchForm" inline class="search-form">
            <el-form-item label="用户ID">
              <el-input
                v-model.number="behaviorSearchForm.userId"
                placeholder="请输入用户ID"
                clearable
                style="width: 200px"
              />
            </el-form-item>
            <el-form-item label="行为类型">
              <el-select
                v-model="behaviorSearchForm.behaviorType"
                placeholder="请选择行为类型"
                clearable
                style="width: 200px"
              >
                <el-option label="频繁取消订单" value="FREQUENT_CANCEL" />
                <el-option label="支付失败" value="PAYMENT_FAILURE" />
                <el-option label="账号滥用" value="ACCOUNT_ABUSE" />
                <el-option label="多地登录" value="MULTI_LOCATION_LOGIN" />
              </el-select>
            </el-form-item>
            <el-form-item label="处理状态">
              <el-select
                v-model="behaviorSearchForm.status"
                placeholder="请选择状态"
                clearable
                style="width: 150px"
              >
                <el-option label="待处理" :value="0" />
                <el-option label="已处理" :value="1" />
                <el-option label="已忽略" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadAbnormalBehaviors">搜索</el-button>
              <el-button @click="resetBehaviorSearch">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 异常行为列表 -->
          <el-table :data="abnormalBehaviorList" v-loading="behaviorLoading" style="width: 100%" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="userId" label="用户ID" width="100" />
            <el-table-column prop="behaviorType" label="行为类型" width="150" />
            <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
            <el-table-column prop="riskScore" label="风险评分" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.riskScore >= 80" type="danger" size="small">{{ row.riskScore }}</el-tag>
                <el-tag v-else-if="row.riskScore >= 60" type="warning" size="small">{{ row.riskScore }}</el-tag>
                <el-tag v-else type="info" size="small">{{ row.riskScore }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 0" type="warning" size="small">待处理</el-tag>
                <el-tag v-else-if="row.status === 1" type="success" size="small">已处理</el-tag>
                <el-tag v-else type="info" size="small">已忽略</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 0"
                  type="primary"
                  size="small"
                  @click="handleBehavior(row, 1)"
                >
                  处理
                </el-button>
                <el-button
                  v-if="row.status === 0"
                  type="info"
                  size="small"
                  @click="handleBehavior(row, 2)"
                >
                  忽略
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination" v-if="behaviorTotal > 0">
            <el-pagination
              v-model:current-page="behaviorCurrentPage"
              v-model:page-size="behaviorPageSize"
              :total="behaviorTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleBehaviorSizeChange"
              @current-change="handleBehaviorPageChange"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 黑名单 -->
      <el-tab-pane label="黑名单" name="blacklist">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>黑名单管理</span>
              <el-button type="primary" @click="showAddBlacklistDialog">添加黑名单</el-button>
            </div>
          </template>

          <!-- 搜索表单 -->
          <el-form :model="blacklistSearchForm" inline class="search-form">
            <el-form-item label="类型">
              <el-select
                v-model="blacklistSearchForm.type"
                placeholder="请选择类型"
                clearable
                style="width: 150px"
              >
                <el-option label="IP" value="IP" />
                <el-option label="设备" value="DEVICE" />
                <el-option label="手机号" value="PHONE" />
                <el-option label="用户" value="USER" />
              </el-select>
            </el-form-item>
            <el-form-item label="状态">
              <el-select
                v-model="blacklistSearchForm.status"
                placeholder="请选择状态"
                clearable
                style="width: 150px"
              >
                <el-option label="生效" :value="1" />
                <el-option label="失效" :value="0" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadBlacklist">搜索</el-button>
              <el-button @click="resetBlacklistSearch">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 黑名单列表 -->
          <el-table :data="blacklistList" v-loading="blacklistLoading" style="width: 100%" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="value" label="值" min-width="200" />
            <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
            <el-table-column prop="riskLevel" label="风险等级" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.riskLevel === 1" type="info" size="small">低</el-tag>
                <el-tag v-else-if="row.riskLevel === 2" type="warning" size="small">中</el-tag>
                <el-tag v-else-if="row.riskLevel === 3" type="danger" size="small">高</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag v-if="row.status === 1" type="danger" size="small">生效</el-tag>
                <el-tag v-else type="info" size="small">失效</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="expiresAt" label="过期时间" width="180">
              <template #default="{ row }">
                {{ row.expiresAt ? formatDate(row.expiresAt) : '永久' }}
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180">
              <template #default="{ row }">
                {{ formatDate(row.createdAt) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 1"
                  type="danger"
                  size="small"
                  @click="removeBlacklist(row.id)"
                >
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination" v-if="blacklistTotal > 0">
            <el-pagination
              v-model:current-page="blacklistCurrentPage"
              v-model:page-size="blacklistPageSize"
              :total="blacklistTotal"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleBlacklistSizeChange"
              @current-change="handleBlacklistPageChange"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加黑名单对话框 -->
    <el-dialog v-model="addBlacklistDialogVisible" title="添加黑名单" width="500px">
      <el-form :model="blacklistForm" :rules="blacklistRules" ref="blacklistFormRef" label-width="100px">
        <el-form-item label="类型" prop="type">
          <el-select v-model="blacklistForm.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="IP" value="IP" />
            <el-option label="设备" value="DEVICE" />
            <el-option label="手机号" value="PHONE" />
            <el-option label="用户" value="USER" />
          </el-select>
        </el-form-item>
        <el-form-item label="值" prop="value">
          <el-input v-model="blacklistForm.value" placeholder="请输入黑名单值" />
        </el-form-item>
        <el-form-item label="原因" prop="reason">
          <el-input
            v-model="blacklistForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入加入黑名单的原因"
          />
        </el-form-item>
        <el-form-item label="风险等级" prop="riskLevel">
          <el-select v-model="blacklistForm.riskLevel" placeholder="请选择风险等级" style="width: 100%">
            <el-option label="低" :value="1" />
            <el-option label="中" :value="2" />
            <el-option label="高" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="blacklistForm.expiresAt"
            type="datetime"
            placeholder="选择过期时间（留空表示永久）"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addBlacklistDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAddBlacklist">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getLoginRecords,
  getAbnormalBehaviors,
  getBlacklist,
  addToBlacklist,
  removeFromBlacklist,
  handleAbnormalBehavior,
  type LoginRecord,
  type AbnormalBehavior,
  type Blacklist,
} from '@/api/risk'
import { formatDate } from '@/utils/format'

const activeTab = ref('login-records')

// 登录记录
const loginLoading = ref(false)
const loginRecordList = ref<LoginRecord[]>([])
const loginTotal = ref(0)
const loginCurrentPage = ref(1)
const loginPageSize = ref(20)
const loginSearchForm = ref({
  userId: undefined as number | undefined,
  ipAddress: undefined as string | undefined,
  riskLevel: undefined as number | undefined,
})

// 异常行为
const behaviorLoading = ref(false)
const abnormalBehaviorList = ref<AbnormalBehavior[]>([])
const behaviorTotal = ref(0)
const behaviorCurrentPage = ref(1)
const behaviorPageSize = ref(20)
const behaviorSearchForm = ref({
  userId: undefined as number | undefined,
  behaviorType: undefined as string | undefined,
  status: undefined as number | undefined,
})

// 黑名单
const blacklistLoading = ref(false)
const blacklistList = ref<Blacklist[]>([])
const blacklistTotal = ref(0)
const blacklistCurrentPage = ref(1)
const blacklistPageSize = ref(20)
const blacklistSearchForm = ref({
  type: undefined as string | undefined,
  status: undefined as number | undefined,
})

// 添加黑名单对话框
const addBlacklistDialogVisible = ref(false)
const blacklistFormRef = ref<FormInstance>()
const blacklistForm = ref({
  type: '',
  value: '',
  reason: '',
  riskLevel: 2,
  expiresAt: undefined as string | undefined,
})
const blacklistRules: FormRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  value: [{ required: true, message: '请输入值', trigger: 'blur' }],
  riskLevel: [{ required: true, message: '请选择风险等级', trigger: 'change' }],
}

// 加载登录记录
const loadLoginRecords = async () => {
  loginLoading.value = true
  try {
    const res = await getLoginRecords({
      ...loginSearchForm.value,
      page: loginCurrentPage.value,
      pageSize: loginPageSize.value,
    })
    if (res && res.code === 200 && res.data) {
      loginRecordList.value = res.data.records || []
      loginTotal.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('加载登录记录失败:', error)
    ElMessage.error('加载登录记录失败: ' + (error?.message || '未知错误'))
  } finally {
    loginLoading.value = false
  }
}

// 加载异常行为
const loadAbnormalBehaviors = async () => {
  behaviorLoading.value = true
  try {
    const res = await getAbnormalBehaviors({
      ...behaviorSearchForm.value,
      page: behaviorCurrentPage.value,
      pageSize: behaviorPageSize.value,
    })
    if (res && res.code === 200 && res.data) {
      abnormalBehaviorList.value = res.data.records || []
      behaviorTotal.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('加载异常行为失败:', error)
    ElMessage.error('加载异常行为失败: ' + (error?.message || '未知错误'))
  } finally {
    behaviorLoading.value = false
  }
}

// 加载黑名单
const loadBlacklist = async () => {
  blacklistLoading.value = true
  try {
    const res = await getBlacklist({
      ...blacklistSearchForm.value,
      page: blacklistCurrentPage.value,
      pageSize: blacklistPageSize.value,
    })
    if (res && res.code === 200 && res.data) {
      blacklistList.value = res.data.records || []
      blacklistTotal.value = res.data.total || 0
    }
  } catch (error: any) {
    console.error('加载黑名单失败:', error)
    ElMessage.error('加载黑名单失败: ' + (error?.message || '未知错误'))
  } finally {
    blacklistLoading.value = false
  }
}

// 处理异常行为
const handleBehavior = async (behavior: AbnormalBehavior, action: number) => {
  try {
    await handleAbnormalBehavior(behavior.id, action)
    ElMessage.success(action === 1 ? '处理成功' : '已忽略')
    loadAbnormalBehaviors()
  } catch (error: any) {
    console.error('处理异常行为失败:', error)
    ElMessage.error('处理失败: ' + (error?.message || '未知错误'))
  }
}

// 显示添加黑名单对话框
const showAddBlacklistDialog = () => {
  blacklistForm.value = {
    type: '',
    value: '',
    reason: '',
    riskLevel: 2,
    expiresAt: undefined,
  }
  addBlacklistDialogVisible.value = true
}

// 添加黑名单
const handleAddBlacklist = async () => {
  if (!blacklistFormRef.value) return
  await blacklistFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        await addToBlacklist(blacklistForm.value)
        ElMessage.success('添加成功')
        addBlacklistDialogVisible.value = false
        loadBlacklist()
      } catch (error: any) {
        console.error('添加黑名单失败:', error)
        ElMessage.error('添加失败: ' + (error?.message || '未知错误'))
      }
    }
  })
}

// 移除黑名单
const removeBlacklist = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认移除该黑名单？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await removeFromBlacklist(id)
    ElMessage.success('移除成功')
    loadBlacklist()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('移除黑名单失败:', error)
      ElMessage.error('移除失败: ' + (error?.message || '未知错误'))
    }
  }
}

// 重置搜索
const resetLoginSearch = () => {
  loginSearchForm.value = {
    userId: undefined,
    ipAddress: undefined,
    riskLevel: undefined,
  }
  loginCurrentPage.value = 1
  loadLoginRecords()
}

const resetBehaviorSearch = () => {
  behaviorSearchForm.value = {
    userId: undefined,
    behaviorType: undefined,
    status: undefined,
  }
  behaviorCurrentPage.value = 1
  loadAbnormalBehaviors()
}

const resetBlacklistSearch = () => {
  blacklistSearchForm.value = {
    type: undefined,
    status: undefined,
  }
  blacklistCurrentPage.value = 1
  loadBlacklist()
}

// 分页
const handleLoginPageChange = (page: number) => {
  loginCurrentPage.value = page
  loadLoginRecords()
}

const handleLoginSizeChange = (size: number) => {
  loginPageSize.value = size
  loginCurrentPage.value = 1
  loadLoginRecords()
}

const handleBehaviorPageChange = (page: number) => {
  behaviorCurrentPage.value = page
  loadAbnormalBehaviors()
}

const handleBehaviorSizeChange = (size: number) => {
  behaviorPageSize.value = size
  behaviorCurrentPage.value = 1
  loadAbnormalBehaviors()
}

const handleBlacklistPageChange = (page: number) => {
  blacklistCurrentPage.value = page
  loadBlacklist()
}

const handleBlacklistSizeChange = (size: number) => {
  blacklistPageSize.value = size
  blacklistCurrentPage.value = 1
  loadBlacklist()
}

// 监听标签页切换
const handleTabChange = (tabName: string) => {
  if (tabName === 'login-records') {
    loadLoginRecords()
  } else if (tabName === 'abnormal-behaviors') {
    loadAbnormalBehaviors()
  } else if (tabName === 'blacklist') {
    loadBlacklist()
  }
}

onMounted(() => {
  loadLoginRecords()
})
</script>

<style scoped lang="scss">
.risk-management {
  .search-form {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: center;
  }
}
</style>

