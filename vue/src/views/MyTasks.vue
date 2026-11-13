<template>
  <div class="my-tasks-page">
    <div class="page-header">
      <h2 class="page-title">我的任务</h2>
      <p class="page-subtitle">{{ pageSubtitle }}</p>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-tabs v-model="activeTab" class="relation-tabs" @tab-change="handleTabChange">
          <el-tab-pane :label="isStudent ? '我的任务' : '我负责的'" name="responsible" />
          <el-tab-pane v-if="!isStudent" label="我创建的" name="created" />
          <el-tab-pane v-if="canReview" label="待我审核" name="review" />
        </el-tabs>
        <div class="filter-right">
          <el-radio-group v-model="filterStatus" size="default" @change="handleStatusChange">
            <el-radio-button label="全部" value="">全部</el-radio-button>
            <el-radio-button label="待办" value="TODO">待办</el-radio-button>
            <el-radio-button label="进行中" value="IN_PROGRESS">进行中</el-radio-button>
            <el-radio-button label="审核中" value="REVIEW">审核中</el-radio-button>
            <el-radio-button label="已完成" value="DONE">已完成</el-radio-button>
          </el-radio-group>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索任务标题"
            clearable
            style="width: 260px"
            @keyup.enter="loadData"
            @clear="loadData"
          >
            <template #append>
              <el-button size="small" @click="loadData">
                <el-icon><Search /></el-icon>
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </el-card>

    <el-card class="tasks-card" shadow="never" v-loading="loading">
      <el-table :data="taskList" style="width: 100%" @row-click="handleRowClick">
        <el-table-column label="任务标题" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="task-title-cell">
              <el-tag v-if="row.overdue" type="danger" size="small" effect="plain" class="overdue-tag">逾期</el-tag>
              <span :class="{ 'done-task': row.status === 'DONE' }">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click.stop="goToProject(row.projectId)">{{ row.projectName || '-' }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="90">
          <template #default="{ row }">
            <el-tag :type="priorityType(row.priority)" size="small">{{ priorityLabel(row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="进度" width="140">
          <template #default="{ row }">
            <el-progress :percentage="row.progress || 0" :status="row.status === 'DONE' ? 'success' : ''" :stroke-width="6" />
          </template>
        </el-table-column>
        <el-table-column label="负责人" width="110" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.assigneeName || '未分配' }}
          </template>
        </el-table-column>
        <el-table-column label="截止日期" width="110">
          <template #default="{ row }">
            <span :class="{ 'overdue-text': row.overdue }">{{ row.endDate || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click.stop="goToTask(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && taskList.length === 0" description="暂无相关任务" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import request from '@/utils/request.js'

const userStr = localStorage.getItem('code_user');
const currentUser = userStr ? JSON.parse(userStr) : {};
const isStudent = computed(() => currentUser.role === 'STUDENT');
const canReview = computed(() => currentUser.role === 'TEACHER');
const pageSubtitle = computed(() => {
  if (currentUser.role === 'TEACHER') return '管理您负责的项目任务，跟踪进度与审核';
  return '查看分配给你的实训任务，按状态跟进完成情况';
});

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const taskList = ref([])
const filterStatus = ref('')
const searchKeyword = ref('')
const activeTab = ref('responsible')

const statusMap = {
  TODO: { label: '待办', type: 'info' },
  IN_PROGRESS: { label: '进行中', type: 'primary' },
  REVIEW: { label: '审核中', type: 'warning' },
  DONE: { label: '已完成', type: 'success' },
  BLOCKED: { label: '已阻塞', type: 'danger' }
}

const priorityMap = {
  HIGH: { label: '高', type: 'danger' },
  MEDIUM: { label: '中', type: 'warning' },
  LOW: { label: '低', type: 'success' }
}

const statusLabel = (status) => statusMap[status]?.label || status
const statusType = (status) => statusMap[status]?.type || 'info'
const priorityLabel = (priority) => priorityMap[priority]?.label || priority
const priorityType = (priority) => priorityMap[priority]?.type || 'info'

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      relationType: activeTab.value
    }
    if (filterStatus.value) {
      params.statuses = [filterStatus.value]
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    const res = await request.get('/task/my-list', { params })
    taskList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载任务失败')
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  loadData()
}

const handleTabChange = () => {
  filterStatus.value = ''
  loadData()
}

const handleRowClick = (row) => {
  goToTask(row)
}

const goToProject = (projectId) => {
  router.push(`/project/${projectId}`)
}

const goToTask = (row) => {
  router.push(`/project/${row.projectId}/board?taskId=${row.id}`)
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-tasks-page {
  padding: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 14px;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
}

.relation-tabs {
  flex-shrink: 0;
}

.relation-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tasks-card {
  min-height: 400px;
}

.task-title-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.overdue-tag {
  flex-shrink: 0;
}

.done-task {
  text-decoration: line-through;
  color: #909399;
}

.overdue-text {
  color: #f56c6c;
  font-weight: 500;
}

:deep(.el-table__row) {
  cursor: pointer;
}
</style>
