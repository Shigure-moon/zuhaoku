<template>
  <div class="audit-logs">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>日志审计</span>
        </div>
      </template>

      <!-- 搜索表单 -->
      <el-form :model="searchForm" inline class="search-form">
        <el-form-item label="用户名">
          <el-input
            v-model="searchForm.username"
            placeholder="请输入用户名"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select
            v-model="searchForm.role"
            placeholder="请选择角色"
            clearable
            style="width: 150px"
          >
            <el-option label="租客" value="TENANT" />
            <el-option label="商家" value="OWNER" />
            <el-option label="管理员" value="OPERATOR" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select
            v-model="searchForm.action"
            placeholder="请选择操作类型"
            clearable
            style="width: 200px"
          >
            <el-option label="登录" value="LOGIN" />
            <el-option label="登出" value="LOGOUT" />
            <el-option label="注册" value="REGISTER" />
            <el-option label="创建订单" value="ORDER_CREATE" />
            <el-option label="支付" value="PAYMENT" />
            <el-option label="申诉处理" value="APPEAL_RESOLVE" />
            <el-option label="用户冻结" value="USER_FREEZE" />
            <el-option label="用户解冻" value="USER_UNFREEZE" />
            <el-option label="账号创建" value="ACCOUNT_CREATE" />
            <el-option label="账号更新" value="ACCOUNT_UPDATE" />
            <el-option label="账号删除" value="ACCOUNT_DELETE" />
          </el-select>
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select
            v-model="searchForm.resourceType"
            placeholder="请选择资源类型"
            clearable
            style="width: 150px"
          >
            <el-option label="用户" value="USER" />
            <el-option label="订单" value="ORDER" />
            <el-option label="账号" value="ACCOUNT" />
            <el-option label="申诉" value="APPEAL" />
            <el-option label="支付" value="PAYMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="操作结果">
          <el-select
            v-model="searchForm.success"
            placeholder="请选择结果"
            clearable
            style="width: 120px"
          >
            <el-option label="成功" :value="1" />
            <el-option label="失败" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 400px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志列表 -->
      <el-table :data="logList" v-loading="loading" style="width: 100%" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.role === 'TENANT'" type="success" size="small">租客</el-tag>
            <el-tag v-else-if="row.role === 'OWNER'" type="warning" size="small">商家</el-tag>
            <el-tag v-else-if="row.role === 'OPERATOR'" type="danger" size="small">管理员</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="action" label="操作类型" width="150" />
        <el-table-column prop="resourceType" label="资源类型" width="100" />
        <el-table-column prop="resourceId" label="资源ID" width="100" />
        <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="requestMethod" label="请求方法" width="100" />
        <el-table-column prop="requestPath" label="请求路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP地址" width="150" />
        <el-table-column prop="success" label="结果" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.success === 1" type="success" size="small">成功</el-tag>
            <el-tag v-else type="danger" size="small">失败</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responseStatus" label="状态码" width="100" />
        <el-table-column prop="executionTime" label="耗时(ms)" width="100" />
        <el-table-column prop="createdAt" label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleViewDetail(row)">查看详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[20, 50, 100, 200]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog v-model="detailDialogVisible" title="日志详情" width="800px">
      <el-descriptions :column="2" border v-if="selectedLog">
        <el-descriptions-item label="日志ID">{{ selectedLog.id }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ selectedLog.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ selectedLog.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ selectedLog.role || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ selectedLog.action }}</el-descriptions-item>
        <el-descriptions-item label="资源类型">{{ selectedLog.resourceType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="资源ID">{{ selectedLog.resourceId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作结果">
          <el-tag v-if="selectedLog.success === 1" type="success">成功</el-tag>
          <el-tag v-else type="danger">失败</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ selectedLog.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求路径">{{ selectedLog.requestPath || '-' }}</el-descriptions-item>
        <el-descriptions-item label="响应状态码">{{ selectedLog.responseStatus || '-' }}</el-descriptions-item>
        <el-descriptions-item label="执行耗时">{{ selectedLog.executionTime ? selectedLog.executionTime + 'ms' : '-' }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ selectedLog.ipAddress || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户代理" :span="2">{{ selectedLog.userAgent || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ selectedLog.description || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre v-if="selectedLog.requestParams" style="max-height: 200px; overflow: auto">{{ formatJson(selectedLog.requestParams) }}</pre>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2" v-if="selectedLog.errorMessage">
          <el-alert type="error" :closable="false">{{ selectedLog.errorMessage }}</el-alert>
        </el-descriptions-item>
        <el-descriptions-item label="操作时间" :span="2">{{ formatDate(selectedLog.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs, type AuditLog, type AuditLogQueryParams } from '@/api/admin'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const logList = ref<AuditLog[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const dateRange = ref<[string, string] | null>(null)

const searchForm = ref<AuditLogQueryParams>({
  username: undefined,
  role: undefined,
  action: undefined,
  resourceType: undefined,
  resourceId: undefined,
  success: undefined,
  startTime: undefined,
  endTime: undefined,
})

const detailDialogVisible = ref(false)
const selectedLog = ref<AuditLog | null>(null)

// 加载日志列表
const loadLogList = async () => {
  loading.value = true
  try {
    const params: AuditLogQueryParams = {
      ...searchForm.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    }

    // 处理时间范围
    if (dateRange.value && dateRange.value.length === 2) {
      params.startTime = dateRange.value[0]
      params.endTime = dateRange.value[1]
    }

    const res = await getAuditLogs(params)
    console.log('日志列表API响应:', res)
    if (res && res.code === 200 && res.data) {
      logList.value = res.data.records || []
      total.value = res.data.total || 0
      console.log('日志列表加载成功:', { count: logList.value.length, total: total.value })
    } else {
      console.warn('日志列表返回数据格式异常:', res)
      logList.value = []
      total.value = 0
      if (res && res.message) {
        ElMessage.warning(res.message)
      }
    }
  } catch (error: any) {
    console.error('加载日志列表失败:', error)
    ElMessage.error('加载日志列表失败: ' + (error?.message || '未知错误'))
    logList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  currentPage.value = 1
  loadLogList()
}

// 重置
const handleReset = () => {
  searchForm.value = {
    username: undefined,
    role: undefined,
    action: undefined,
    resourceType: undefined,
    resourceId: undefined,
    success: undefined,
    startTime: undefined,
    endTime: undefined,
  }
  dateRange.value = null
  currentPage.value = 1
  loadLogList()
}

// 分页
const handlePageChange = (page: number) => {
  currentPage.value = page
  loadLogList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadLogList()
}

// 查看详情
const handleViewDetail = (log: AuditLog) => {
  selectedLog.value = log
  detailDialogVisible.value = true
}

// 格式化JSON
const formatJson = (jsonStr: string) => {
  try {
    const obj = JSON.parse(jsonStr)
    return JSON.stringify(obj, null, 2)
  } catch (e) {
    return jsonStr
  }
}

onMounted(() => {
  loadLogList()
})
</script>

<style scoped lang="scss">
.audit-logs {
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

