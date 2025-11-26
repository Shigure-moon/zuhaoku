<template>
  <div class="admin-appeals">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>申诉管理</span>
        </div>
      </template>

      <!-- 搜索筛选 -->
      <div class="filter-section">
        <el-form :model="searchForm" inline>
          <el-form-item label="订单ID">
            <el-input v-model="searchForm.orderId" placeholder="请输入订单ID" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="searchForm.type" placeholder="选择类型" clearable style="width: 200px">
              <el-option label="账号异常" :value="1" />
              <el-option label="押金争议" :value="2" />
              <el-option label="其他" :value="3" />
              <el-option label="玩家恶意使用/销毁资源" :value="4" />
              <el-option label="买家脚本盗号" :value="5" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="选择状态" clearable style="width: 150px">
              <el-option label="待处理" :value="0" />
              <el-option label="已处理" :value="1" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 申诉列表 -->
      <el-table :data="appealList" v-loading="loading" style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="orderId" label="订单ID" width="100" />
        <el-table-column prop="type" label="类型" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.type === 1">账号异常</el-tag>
            <el-tag v-else-if="row.type === 2" type="warning">押金争议</el-tag>
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
        <el-table-column prop="createTime" label="创建时间" />
        <el-table-column prop="resolveTime" label="处理时间" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleView(row)" v-if="!row.verdict">处理</el-button>
            <el-button type="info" size="small" @click="handleView(row)" v-else>查看</el-button>
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

    <!-- 处理申诉对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="订单ID">
          <el-input v-model="form.orderId" disabled />
        </el-form-item>
        <el-form-item label="申诉类型">
          <el-tag v-if="form.type === 1">账号异常</el-tag>
          <el-tag v-else-if="form.type === 2" type="warning">押金争议</el-tag>
          <el-tag v-else-if="form.type === 4" type="danger">玩家恶意使用/销毁资源</el-tag>
          <el-tag v-else-if="form.type === 5" type="danger">买家脚本盗号</el-tag>
          <el-tag v-else type="info">其他</el-tag>
        </el-form-item>
        <el-form-item label="证据">
          <div v-if="form.evidenceUrls && form.evidenceUrls.length > 0" class="evidence-preview">
            <el-image
              v-for="(url, index) in form.evidenceUrls"
              :key="index"
              :src="url"
              style="width: 120px; height: 120px; margin-right: 10px; margin-bottom: 10px"
              :preview-src-list="form.evidenceUrls"
              fit="cover"
            />
          </div>
          <span v-else>无证据</span>
        </el-form-item>
        <el-form-item label="裁决结果" prop="verdict" v-if="!form.verdict">
          <el-radio-group v-model="form.verdict">
            <el-radio :label="1">支持租客</el-radio>
            <el-radio :label="2">支持号主</el-radio>
            <el-radio :label="3">各担一半</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="裁决结果" v-else>
          <el-tag v-if="form.verdict === 1" type="success">支持租客</el-tag>
          <el-tag v-else-if="form.verdict === 2" type="danger">支持号主</el-tag>
          <el-tag v-else type="info">各担一半</el-tag>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :disabled="!form.verdict">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAppealList, getAppealDetail, resolveAppeal, type Appeal } from '@/api/appeal'

const route = useRoute()

// 监听路由参数，如果有 appealId，自动打开详情
watch(() => route.query.appealId, async (appealId) => {
  if (appealId) {
    const id = Number(appealId)
    if (!isNaN(id)) {
      // 先加载列表，确保数据存在
      await loadAppealList()
      // 查找对应的申诉记录
      const appeal = appealList.value.find(a => a.id === id)
      if (appeal) {
        await handleView(appeal)
      } else {
        // 如果列表中找不到，直接通过ID获取详情
        try {
          const response = await getAppealDetail(id)
          if (response.code === 200 && response.data) {
            const appealData = response.data
            Object.assign(form, {
              id: appealData.id,
              orderId: appealData.orderId,
              type: appealData.type,
              evidenceUrls: appealData.evidenceUrls || [],
              verdict: appealData.verdict,
            })
            dialogTitle.value = appealData.verdict ? '查看申诉' : '处理申诉'
            dialogVisible.value = true
          }
        } catch (error) {
          console.error('获取申诉详情失败:', error)
          ElMessage.error('获取申诉详情失败')
        }
      }
    }
  }
}, { immediate: true })

const searchForm = ref({
  orderId: undefined,
  type: undefined,
  status: undefined,
})

const appealList = ref<Appeal[]>([])
const loading = ref(false)
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const dialogVisible = ref(false)
const dialogTitle = ref('处理申诉')
const form = reactive({
  id: undefined as number | undefined,
  orderId: undefined as number | undefined,
  type: undefined as number | undefined,
  evidenceUrls: [] as string[],
  verdict: undefined as number | undefined,
})

const loadAppealList = async () => {
  loading.value = true
  try {
    const status = searchForm.value.status === 0 ? 'pending' : searchForm.value.status === 1 ? 'resolved' : undefined
    console.log('加载申诉列表，筛选条件:', { status, page: currentPage.value, pageSize: pageSize.value })
    const response = await getAppealList({
      status,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    console.log('申诉列表响应:', response)
    if (response.code === 200 && response.data) {
      appealList.value = response.data.list || []
      total.value = response.data.total || 0
      // 调试：打印申诉列表，检查 verdict 字段
      console.log('申诉列表数据:', appealList.value.map(a => ({ 
        id: a.id, 
        orderId: a.orderId,
        verdict: a.verdict, 
        verdictName: a.verdictName,
        resolveTime: a.resolveTime 
      })))
    } else {
      console.error('加载申诉列表失败，响应:', response)
      ElMessage.error('加载申诉列表失败: ' + (response.message || '未知错误'))
    }
  } catch (error) {
    console.error('加载申诉列表失败:', error)
    ElMessage.error('加载申诉列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadAppealList()
}

const handleReset = () => {
  searchForm.value = {
    orderId: undefined,
    type: undefined,
    status: undefined,
  }
  handleSearch()
}

const handleView = async (row: Appeal) => {
  try {
    const response = await getAppealDetail(row.id)
    if (response.code === 200 && response.data) {
      const appeal = response.data
      Object.assign(form, {
        id: appeal.id,
        orderId: appeal.orderId,
        type: appeal.type,
        evidenceUrls: appeal.evidenceUrls || [],
        verdict: appeal.verdict,
      })
      dialogTitle.value = appeal.verdict ? '查看申诉' : '处理申诉'
      dialogVisible.value = true
    }
  } catch (error) {
    console.error('获取申诉详情失败:', error)
    ElMessage.error('获取申诉详情失败')
  }
}

const handleSubmit = async () => {
  if (!form.verdict) {
    ElMessage.warning('请选择裁决结果')
    return
  }

  if (!form.id) {
    ElMessage.error('申诉ID不存在')
    return
  }

  console.log('开始处理申诉: appealId=', form.id, 'verdict=', form.verdict)
  try {
    console.log('发送处理申诉请求: POST /api/v1/appeals/' + form.id + '/resolve', { verdict: form.verdict })
    const response = await resolveAppeal(form.id, { verdict: form.verdict })
    console.log('处理申诉响应:', response)
    if (response.code === 200 && response.data) {
      console.log('处理申诉成功，返回数据:', response.data)
      ElMessage.success('处理成功')
      dialogVisible.value = false
      // 清除筛选条件，显示所有申诉（包括刚处理的）
      searchForm.value.status = undefined
      // 重置到第一页
      currentPage.value = 1
      // 刷新列表
      await loadAppealList()
      // 再次确认：检查列表中该申诉的状态
      const updatedAppeal = appealList.value.find(a => a.id === form.id)
      console.log('刷新后的申诉状态:', updatedAppeal)
      if (updatedAppeal && updatedAppeal.verdict) {
        ElMessage.success('申诉已成功处理')
      } else {
        ElMessage.warning('申诉处理成功，但列表未更新，请手动刷新')
      }
    } else {
      console.error('处理申诉失败，响应:', response)
      ElMessage.error('处理申诉失败: ' + (response.message || '未知错误'))
    }
  } catch (error: any) {
    console.error('处理申诉失败:', error)
    ElMessage.error('处理申诉失败: ' + (error.message || '网络错误'))
  }
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  loadAppealList()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  loadAppealList()
}

onMounted(() => {
  loadAppealList()
})
</script>

<style scoped lang="scss">
.admin-appeals {
  .filter-section {
    margin-bottom: 20px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .evidence-preview {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
  }
}
</style>

