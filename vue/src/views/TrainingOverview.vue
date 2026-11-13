<template>
  <div class="training-overview">
    <div class="greeting-bar">
      <div class="greeting">
        <el-icon class="greeting-icon"><Grid /></el-icon>
        <span class="greeting-text">实训总览</span>
      </div>
      <el-button type="primary" size="small" @click="loadData">刷新数据</el-button>
    </div>

    <div class="stats-row">
      <el-card class="stat-card stat-card-primary">
        <el-icon class="stat-icon"><FolderOpened /></el-icon>
        <div class="stat-content">
          <div class="stat-title">项目总数</div>
          <div class="stat-value">{{ stats.projectCount }}</div>
        </div>
      </el-card>
      <el-card class="stat-card stat-card-success">
        <el-icon class="stat-icon"><CircleCheckFilled /></el-icon>
        <div class="stat-content">
          <div class="stat-title">进行中项目</div>
          <div class="stat-value">{{ stats.activeProjectCount }}</div>
        </div>
      </el-card>
      <el-card class="stat-card stat-card-warning">
        <el-icon class="stat-icon"><List /></el-icon>
        <div class="stat-content">
          <div class="stat-title">任务总数</div>
          <div class="stat-value">{{ stats.taskCount }}</div>
        </div>
      </el-card>
      <el-card class="stat-card stat-card-info">
        <el-icon class="stat-icon"><CircleCheck /></el-icon>
        <div class="stat-content">
          <div class="stat-title">完成率</div>
          <div class="stat-value">{{ stats.taskCompletionRate.toFixed(1) }}%</div>
        </div>
      </el-card>
      <el-card class="stat-card stat-card-danger">
        <el-icon class="stat-icon"><Clock /></el-icon>
        <div class="stat-content">
          <div class="stat-title">逾期任务</div>
          <div class="stat-value" :class="{ 'text-danger': stats.overdueTaskCount > 0 }">{{ stats.overdueTaskCount }}</div>
        </div>
      </el-card>
    </div>

    <div class="main-content">
      <div class="left-panel">
        <el-card class="project-card">
          <template #header>
            <div class="card-header">
              <el-icon><FolderOpened /></el-icon>
              <span>项目总览</span>
              <el-button size="small" type="primary" @click="goToProjectList">查看全部</el-button>
            </div>
          </template>
          <div class="project-list">
            <template v-if="stats.projects.length">
              <div 
                v-for="project in stats.projects.slice(0, 6)" 
                :key="project.id" 
                class="project-item"
                @click="goToProject(project.id)"
              >
                <div class="project-status" :class="project.status.toLowerCase().replace('_', '-')"></div>
                <div class="project-info">
                  <div class="project-name">{{ project.name }}</div>
                  <div class="project-meta">
                    <span>指导教师: {{ project.teacherName || project.ownerName || '未分配' }}</span>
                    <span>{{ project.completedTasks || 0 }}/{{ project.totalTasks || 0 }} 任务</span>
                  </div>
                </div>
                <div class="project-progress">
                  <el-progress 
                    :percentage="project.totalTasks ? (project.completedTasks / project.totalTasks * 100) : 0" 
                    :stroke-width="8"
                    :text-inside="true"
                  />
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无项目" :image-size="72" />
          </div>
        </el-card>

        <el-card class="member-card">
          <template #header>
            <div class="card-header">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-icon><UserFilled /></el-icon>
                <span>成员负载</span>
              </div>
              <el-button size="small" type="primary" @click="goToMemberLoad">查看全部</el-button>
            </div>
          </template>
          <div class="member-list">
            <template v-if="stats.memberWorkload.length">
              <div v-for="member in stats.memberWorkload.slice(0, 8)" :key="member.userId" class="member-item">
                <div class="member-info">
                  <div class="member-name">{{ member.username }}</div>
                  <div class="member-tasks">
                    <span class="todo">{{ member.todoTasks || 0 }}待处理</span>
                    <span class="progress">{{ member.inProgressTasks || 0 }}进行中</span>
                    <span class="review">{{ member.reviewTasks || 0 }}审核中</span>
                  </div>
                </div>
                <div class="member-progress">
                  <el-progress 
                    :percentage="member.completionRate" 
                    :stroke-width="6"
                  />
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无成员数据" :image-size="72" />
          </div>
        </el-card>
      </div>

      <div class="right-panel">
        <el-card class="status-card">
          <template #header>
            <div class="card-header">
              <el-icon><PieChart /></el-icon>
              <span>任务状态分布</span>
            </div>
          </template>
          <div class="status-chart" :class="{ 'is-empty': !stats.taskStatusDistribution.length }">
            <template v-if="stats.taskStatusDistribution.length">
              <div class="status-pie">
                <div class="pie-chart" ref="statusPieChart"></div>
              </div>
              <div class="status-legend">
                <div v-for="item in stats.taskStatusDistribution" :key="item.status" class="legend-item">
                  <span class="legend-color" :class="getTaskStatusClass(item.status)"></span>
                  <span class="legend-label">{{ getStatusLabel(item.status) }}</span>
                  <span class="legend-count">{{ item.cnt }}</span>
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无任务数据" :image-size="76" />
          </div>
        </el-card>

        <el-card class="upcoming-card">
          <template #header>
            <div class="card-header">
              <el-icon><Calendar /></el-icon>
              <span>即将过期</span>
            </div>
          </template>
          <div class="task-list">
            <template v-if="stats.upcomingTasks.length">
              <div v-for="task in stats.upcomingTasks" :key="task.id" class="task-item" @click="goToTask(task.projectId, task.id)">
                <el-tag :type="getPriorityType(task.priority)" size="small" effect="light">{{ getPriorityLabel(task.priority) }}</el-tag>
                <div class="task-info">
                  <div class="task-title">{{ task.title }}</div>
                  <div class="task-meta">{{ task.projectName }} · {{ task.assigneeName || '未分配' }}</div>
                </div>
                <div class="task-deadline">
                  {{ formatDate(task.deadline) }}
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无即将过期任务" :image-size="68" />
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { Grid, FolderOpened, CircleCheckFilled, List, CircleCheck, Clock, UserFilled, PieChart, Calendar } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getAdminBusinessOverview } from '../api/dashboard'

const stats = reactive({
  projectCount: 0,
  activeProjectCount: 0,
  taskCount: 0,
  completedTaskCount: 0,
  overdueTaskCount: 0,
  taskCompletionRate: 0,
  projects: [],
  taskStatusDistribution: [],
  memberWorkload: [],
  upcomingTasks: []
})

const statusPieChart = ref(null)
let pieChartInstance = null

const loadData = async () => {
  try {
    const res = await getAdminBusinessOverview()
    Object.assign(stats, res.data)
    nextTick(() => {
      renderCharts()
    })
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

const renderCharts = () => {
  if (statusPieChart.value) {
    if (pieChartInstance) {
      pieChartInstance.dispose()
    }
    pieChartInstance = echarts.init(statusPieChart.value)
    
    const data = stats.taskStatusDistribution.map(item => ({
      value: item.cnt,
      name: getStatusLabel(item.status),
      itemStyle: {
        color: getStatusColor(item.status)
      }
    }))
    
    pieChartInstance.setOption({
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }]
    })
  }
}

const getStatusLabel = (status) => {
  const map = { TODO: '待处理', IN_PROGRESS: '进行中', REVIEW: '审核中', DONE: '已完成', BLOCKED: '已阻塞' }
  return map[status] || status
}

const getStatusColor = (status) => {
  const map = { TODO: '#909399', IN_PROGRESS: '#409EFF', REVIEW: '#E6A23C', DONE: '#67C23A', BLOCKED: '#F56C6C' }
  return map[status] || '#909399'
}

const getTaskStatusClass = (status) => {
  return `status-${status.toLowerCase().replace('_', '-')}`
}

const getPriorityType = (priority) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'info' }
  return map[priority] || 'default'
}

const getPriorityLabel = (priority) => {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' }
  return map[priority] || priority
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return `${d.getMonth() + 1}/${d.getDate()}`
}

const goToProjectList = () => {
  window.location.href = '/admin/project'
}

const goToProject = (projectId) => {
  window.location.href = `/project/${projectId}`
}

const goToTask = (projectId, taskId) => {
  window.location.href = `/project/${projectId}/board`
}

const goToMemberLoad = () => {
  window.location.href = '/admin/data-analysis?tab=member'
}

onMounted(() => {
  loadData()
  window.addEventListener('resize', renderCharts)
})

onUnmounted(() => {
  window.removeEventListener('resize', renderCharts)
  if (pieChartInstance) {
    pieChartInstance.dispose()
  }
})
</script>

<style scoped>
.training-overview {
  padding: 20px;
}

/* 页头 */
.greeting-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.greeting {
  display: flex;
  align-items: center;
  gap: 10px;
}

.greeting-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  font-size: 20px;
  border-radius: 10px;
  background: linear-gradient(135deg, #ecf5ff, #d9ecff);
  color: #409EFF;
}

.greeting-text {
  font-size: 18px;
  font-weight: 600;
  color: #1f2d3d;
  letter-spacing: 0.5px;
}

/* 统计卡 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  border: none;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 21, 41, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 22px rgba(0, 21, 41, 0.12);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 46px;
  height: 46px;
  font-size: 22px;
  border-radius: 12px;
  flex-shrink: 0;
}

.stat-card-primary .stat-icon { color: #409EFF; background: rgba(64, 158, 255, 0.12); }
.stat-card-success .stat-icon { color: #67C23A; background: rgba(103, 194, 58, 0.12); }
.stat-card-warning .stat-icon { color: #E6A23C; background: rgba(230, 162, 60, 0.12); }
.stat-card-info .stat-icon { color: #909399; background: rgba(144, 147, 153, 0.12); }
.stat-card-danger .stat-icon { color: #F56C6C; background: rgba(245, 108, 108, 0.12); }

.stat-content {
  flex: 1;
  min-width: 0;
}

.stat-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: #1f2d3d;
  font-variant-numeric: tabular-nums;
}

.text-danger {
  color: #F56C6C !important;
}

/* 主体布局 */
.main-content {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.left-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.right-panel {
  width: 400px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 卡片通用 */
.main-content :deep(.el-card) {
  border: none;
  border-radius: 14px;
  box-shadow: 0 2px 12px rgba(0, 21, 41, 0.06);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header .el-icon {
  color: #409EFF;
}

.card-header > span,
.card-header > div > span {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.card-header .el-button {
  margin-left: auto;
  border-radius: 8px;
}

/* 项目列表 */
.project-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.project-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  background: #f7f9fc;
  border: 1px solid #eef1f6;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.project-item:hover {
  background: #ecf5ff;
  border-color: #c6e2ff;
  transform: translateX(2px);
}

.project-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.project-status.not-started { background: #909399; }
.project-status.in-progress { background: #409EFF; }
.project-status.completed { background: #67C23A; }
.project-status.delayed { background: #F56C6C; }

.project-info {
  flex: 1;
  min-width: 0;
}

.project-name {
  font-weight: 600;
  color: #1f2d3d;
  margin-bottom: 4px;
}

.project-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 12px;
}

.project-progress {
  width: 140px;
  flex-shrink: 0;
}

/* 成员负载 */
.member-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.member-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.member-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.member-name {
  font-weight: 600;
  color: #1f2d3d;
}

.member-tasks {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.member-tasks .todo { color: #909399; }
.member-tasks .progress { color: #409EFF; }
.member-tasks .review { color: #E6A23C; }

.member-progress {
  width: 100%;
}

/* 任务状态分布 */
.status-chart {
  display: flex;
  align-items: center;
  gap: 20px;
  min-height: 220px;
  padding: 6px 0;
}

.status-chart.is-empty {
  justify-content: center;
}

.status-pie {
  flex: 1;
  display: flex;
  justify-content: center;
}

.pie-chart {
  width: 180px;
  height: 180px;
}

.status-legend {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.legend-color {
  width: 10px;
  height: 10px;
  border-radius: 3px;
  flex-shrink: 0;
}

.legend-color.status-todo { background: #909399; }
.legend-color.status-in-progress { background: #409EFF; }
.legend-color.status-review { background: #E6A23C; }
.legend-color.status-done { background: #67C23A; }
.legend-color.status-blocked { background: #F56C6C; }

.legend-label {
  flex: 1;
  color: #606266;
}

.legend-count {
  font-weight: 600;
  color: #1f2d3d;
}

/* 任务列表 */
.task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  background: #f7f9fc;
  border: 1px solid #eef1f6;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.task-item:hover {
  background: #ecf5ff;
  border-color: #c6e2ff;
}

.task-item .el-tag {
  flex-shrink: 0;
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 13px;
  font-weight: 600;
  color: #1f2d3d;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-deadline {
  font-size: 12px;
  color: #909399;
  flex-shrink: 0;
}

.task-deadline.overdue {
  color: #F56C6C;
  font-weight: 600;
}

/* 空状态 */
.el-empty {
  padding: 18px 0;
}

.el-empty :deep(.el-empty__description) p {
  color: #a8abb2;
  font-size: 13px;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-row {
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  }
}

@media (max-width: 1200px) {
  .main-content {
    flex-direction: column;
  }
  .right-panel {
    width: 100%;
  }
}
</style>