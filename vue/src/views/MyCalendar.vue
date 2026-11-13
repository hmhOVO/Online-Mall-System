<template>
  <div class="my-calendar-page">
    <div class="page-header">
      <h2 class="page-title">任务日历</h2>
      <p class="page-subtitle">{{ pageSubtitle }}</p>
    </div>

    <div class="calendar-layout">
      <el-card class="calendar-card" shadow="never" v-loading="loading">
        <el-calendar v-model="selectedDate">
          <template #date-cell="{ data }">
            <div class="calendar-cell" @click="selectDate(data.day)">
              <span class="day-number">{{ data.day.split('-')[2] }}</span>
              <div class="task-dots">
                <span
                  v-for="(task, index) in getTasksByDate(data.day)"
                  :key="index"
                  class="task-dot"
                  :class="dotClass(task)"
                ></span>
              </div>
              <span v-if="getTasksByDate(data.day).length > 3" class="more-tasks">+{{ getTasksByDate(data.day).length - 3 }}</span>
            </div>
          </template>
        </el-calendar>
      </el-card>

      <el-card class="task-list-card" shadow="never">
        <template #header>
          <div class="card-header">
            <span>{{ selectedDateLabel }} 的任务</span>
            <el-tag type="info" size="small">{{ selectedTasks.length }} 个</el-tag>
          </div>
        </template>

        <div v-if="selectedTasks.length > 0" class="task-list">
          <div
            v-for="task in selectedTasks"
            :key="task.id"
            class="task-item"
            @click="goToTask(task)"
          >
            <div class="task-item-header">
              <el-tag :type="statusType(task.status)" size="small">{{ statusLabel(task.status) }}</el-tag>
              <el-tag :type="priorityType(task.priority)" size="small" effect="plain">{{ priorityLabel(task.priority) }}</el-tag>
            </div>
            <div class="task-title">{{ task.title }}</div>
            <div class="task-meta">
              <span class="project-name">{{ task.projectName || '-' }}</span>
              <span v-if="task.overdue" class="overdue-text">已逾期</span>
            </div>
          </div>
        </div>

        <el-empty v-else description="当天没有相关任务" />
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request.js'

const router = useRouter()
const loading = ref(false)
const taskList = ref([])
const selectedDate = ref(new Date())

const userStr = localStorage.getItem('code_user')
const currentUser = userStr ? JSON.parse(userStr) : {}
const pageSubtitle = computed(() => {
  if (currentUser.role === 'TEACHER') return '按日期查看您负责的任务安排，跟踪进度与审核节点'
  return '按日期查看与您相关的任务安排'
})

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

const formatDate = (date) => {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const getTasksByDate = (dateStr) => {
  return taskList.value.filter(task => {
    const start = task.startDate
    const end = task.endDate
    return start === dateStr || end === dateStr || (start && end && dateStr >= start && dateStr <= end)
  }).slice(0, 4)
}

const dotClass = (task) => {
  if (task.status === 'DONE') return 'dot-done'
  if (task.overdue) return 'dot-overdue'
  if (task.status === 'REVIEW') return 'dot-review'
  if (task.status === 'IN_PROGRESS') return 'dot-progress'
  return 'dot-todo'
}

const selectedTasks = computed(() => {
  const dateStr = formatDate(selectedDate.value)
  return taskList.value.filter(task => {
    const start = task.startDate
    const end = task.endDate
    return start === dateStr || end === dateStr || (start && end && dateStr >= start && dateStr <= end)
  })
})

const selectedDateLabel = computed(() => {
  return formatDate(selectedDate.value)
})

const selectDate = (day) => {
  selectedDate.value = new Date(day)
}

const goToTask = (task) => {
  router.push(`/project/${task.projectId}/board`)
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/task/my-calendar')
    taskList.value = res.data || []
  } catch (error) {
    ElMessage.error('加载日程失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.my-calendar-page {
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

.calendar-layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.calendar-card {
  min-height: 600px;
}

.task-list-card {
  min-height: 600px;
  max-height: 600px;
  display: flex;
  flex-direction: column;
}

.task-list-card :deep(.el-card__body) {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.calendar-cell {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: flex-start;
  padding: 4px;
  cursor: pointer;
}

.day-number {
  font-size: 14px;
  margin-bottom: 4px;
}

.task-dots {
  display: flex;
  gap: 3px;
  flex-wrap: wrap;
  justify-content: center;
  max-width: 40px;
}

.task-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.dot-todo { background-color: #909399; }
.dot-progress { background-color: #409eff; }
.dot-review { background-color: #e6a23c; }
.dot-done { background-color: #67c23a; }
.dot-overdue { background-color: #f56c6c; }

.more-tasks {
  font-size: 10px;
  color: #909399;
  margin-top: 2px;
}

.task-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.task-item {
  padding: 12px;
  border-radius: 8px;
  background-color: #f5f7fa;
  cursor: pointer;
  transition: all 0.2s;
}

.task-item:hover {
  background-color: #ebeef5;
  transform: translateX(4px);
}

.task-item-header {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.task-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 8px;
  line-height: 1.4;
}

.task-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #606266;
}

.project-name {
  color: #409eff;
}

.overdue-text {
  color: #f56c6c;
  font-weight: 500;
}

@media (max-width: 1200px) {
  .calendar-layout {
    grid-template-columns: 1fr;
  }
}
</style>
