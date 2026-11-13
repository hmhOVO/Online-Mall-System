<template>
  <div class="workspace">
    <div class="page-head">
      <div class="page-head-text">
        <h2 class="page-title">我的实训</h2>
        <p class="page-subtitle">{{ greetingText }}，{{ user.nickname || user.username }}。{{ summaryText }}</p>
      </div>
      <el-button size="small" @click="handleRefresh">
        <el-icon class="btn-icon"><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="stats-strip">
      <div class="stat-item">
        <div class="stat-label">待办任务</div>
        <div class="stat-number">{{ stats.todoCount }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">逾期任务</div>
        <div class="stat-number" :class="{ 'is-alert': stats.overdueCount > 0 }">{{ stats.overdueCount }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">已完成</div>
        <div class="stat-number">{{ stats.completedTaskCount }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">实训项目</div>
        <div class="stat-number">{{ stats.projectCount }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">实际学时</div>
        <div class="stat-number">{{ stats.actualHours.toFixed(1) }}<span class="stat-unit">学时</span></div>
      </div>
      <div class="stat-item">
        <div class="stat-label">学时完成率</div>
        <div class="stat-number">{{ Math.round(stats.hoursRatio) }}<span class="stat-unit">%</span></div>
      </div>
    </div>

    <div class="content-grid">
      <div class="primary-col">
        <el-card class="panel today-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Flag /></el-icon>今日待办</span>
              <el-button link type="primary" size="small" @click="goMyTasks">全部任务</el-button>
            </div>
          </template>
          <div v-if="todayTasks.length === 0" class="empty-tip">
            <el-icon class="empty-icon"><Trophy /></el-icon>
            <div>今天没有待办任务</div>
          </div>
          <div v-else class="task-list">
            <div
                v-for="task in todayTasks"
                :key="task.id"
                :class="['task-item', { overdue: task.isOverdue, blocked: task.status === 'BLOCKED' }]"
                @click="goTaskDetail(task.projectId, task.id)"
            >
              <el-button
                  v-if="task.status === 'TODO'"
                  class="task-action"
                  size="small"
                  @click.stop="startTask(task)"
              >开始</el-button>
              <el-button
                  v-else-if="task.status === 'IN_PROGRESS' && task.reviewerId"
                  class="task-action"
                  size="small"
                  type="primary"
                  plain
                  @click.stop="submitTaskReview(task)"
              >提交审核</el-button>
              <el-button
                  v-else-if="task.status === 'IN_PROGRESS'"
                  class="task-action"
                  size="small"
                  type="success"
                  plain
                  @click.stop="completeTask(task)"
              >完成</el-button>
              <div class="task-info">
                <div class="task-title">{{ task.title }}</div>
                <div class="task-meta">
                  <span class="task-project">{{ task.projectName }}</span>
                  <span v-if="task.isOverdue" class="task-overdue">
                    <el-icon><CircleCloseFilled /></el-icon>已逾期
                  </span>
                  <span v-else-if="task.deadline" class="task-deadline">
                    <el-icon><Clock /></el-icon>{{ formatDeadline(task.deadline) }}
                  </span>
                  <span v-if="task.status === 'REVIEW'" class="task-state">审核中</span>
                  <span v-else-if="task.status === 'BLOCKED'" class="task-state task-state-warning">阻塞</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="panel projects-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Collection /></el-icon>我的实训项目</span>
              <el-button link type="primary" size="small" @click="goProjects">查看全部</el-button>
            </div>
          </template>
          <div v-if="projects.length === 0" class="empty-tip">
            <el-icon class="empty-icon"><Folder /></el-icon>
            <div>还没有加入实训项目</div>
          </div>
          <div v-else class="project-cards">
            <div
                v-for="project in projects"
                :key="project.id"
                class="project-card"
                @click="goProjectDetail(project.id)"
            >
              <div class="project-header">
                <span class="project-name">{{ project.name }}</span>
                <el-tag :type="projectStatusType(project.status)" size="small" effect="plain">
                  {{ projectStatusLabel(project.status) }}
                </el-tag>
              </div>
              <div class="project-progress">
                <div class="project-progress-label">
                  <span>任务进度</span>
                  <span>{{ project.progress }}%</span>
                </div>
                <el-progress
                    :percentage="project.progress"
                    :status="project.status === 'COMPLETED' ? 'success' : ''"
                    :stroke-width="6"
                    :show-text="false"
                />
              </div>
              <div class="project-meta">
                <span><el-icon><Calendar /></el-icon>{{ project.startDate || '-' }} ~ {{ project.endDate || '-' }}</span>
              </div>
              <div class="project-stats">
                <span v-if="project.pendingTasks > 0" class="stat-badge pending">{{ project.pendingTasks }} 待办</span>
                <span class="stat-badge completed">{{ project.completedTasks }} 已完成</span>
              </div>
            </div>
          </div>
        </el-card>

        <div class="charts-row">
          <el-card class="panel chart-card" shadow="never">
            <template #header>
              <span class="card-title"><el-icon><Histogram /></el-icon>任务状态分布</span>
            </template>
            <div v-if="statusDistribution.length > 0" ref="statusChart" class="chart"></div>
            <div v-else class="chart-empty">暂无任务数据</div>
          </el-card>
          <el-card class="panel chart-card" shadow="never">
            <template #header>
              <span class="card-title"><el-icon><DataLine /></el-icon>近 7 天完成趋势</span>
            </template>
            <div v-if="trend.length > 0" ref="trendChart" class="chart"></div>
            <div v-else class="chart-empty">暂无完成记录</div>
          </el-card>
        </div>
      </div>

      <div class="side-col">
        <el-card class="panel score-card" shadow="never">
          <template #header>
            <div class="card-header">
              <span class="card-title"><el-icon><Medal /></el-icon>我的成绩</span>
              <el-button link type="primary" size="small" @click="goMyScores">查看全部</el-button>
            </div>
          </template>
          <div class="score-main">
            <div class="score-avg">
              <span class="score-avg-value">{{ scoreSummary.avgScore }}</span>
              <span class="score-avg-label">平均分</span>
            </div>
            <div class="score-sub">
              <div class="score-sub-item">
                <div class="score-sub-label">已评分</div>
                <div class="score-sub-value">{{ scoreSummary.scoredCount }}</div>
              </div>
              <div class="score-sub-item">
                <div class="score-sub-label">累计得分</div>
                <div class="score-sub-value">{{ scoreSummary.totalScore }}</div>
              </div>
              <div class="score-sub-item">
                <div class="score-sub-label">最高分</div>
                <div class="score-sub-value">{{ scoreSummary.maxScore }}</div>
              </div>
            </div>
          </div>
          <div v-if="scoredTasksPreview.length === 0" class="empty-tip score-empty">暂无评分记录</div>
          <div v-else class="scored-task-list">
            <div
                v-for="t in scoredTasksPreview"
                :key="t.id"
                class="scored-task-item"
                @click="goTaskDetail(t.projectId, t.id)"
            >
              <div class="scored-task-info">
                <div class="scored-task-title">{{ t.title }}</div>
                <div class="scored-task-project">{{ t.projectName }}</div>
              </div>
              <div class="scored-task-score">{{ t.score }}</div>
            </div>
          </div>
        </el-card>

        <el-card class="panel hours-card" shadow="never">
          <template #header>
            <span class="card-title"><el-icon><Timer /></el-icon>学时统计</span>
          </template>
          <div class="hours-row">
            <div class="hours-item">
              <div class="hours-label">计划学时</div>
              <div class="hours-value">{{ stats.plannedHours.toFixed(1) }}</div>
            </div>
            <div class="hours-item">
              <div class="hours-label">实际学时</div>
              <div class="hours-value">{{ stats.actualHours.toFixed(1) }}</div>
            </div>
          </div>
          <div class="hours-progress">
            <div class="progress-label">
              <span>学时完成率</span>
              <span>{{ Math.round(stats.hoursRatio) }}%</span>
            </div>
            <el-progress
                :percentage="Math.min(Math.round(stats.hoursRatio), 100)"
                :stroke-width="8"
                :show-text="false"
            />
          </div>
        </el-card>

        <el-card class="panel recent-card" shadow="never">
          <template #header>
            <span class="card-title"><el-icon><Clock /></el-icon>最近任务</span>
          </template>
          <div v-if="recentTasks.length === 0" class="empty-tip">
            <el-icon class="empty-icon"><Document /></el-icon>
            <div>暂无任务记录</div>
          </div>
          <div v-else class="recent-task-list">
            <div
                v-for="task in recentTasks"
                :key="task.id"
                class="recent-task-item"
                @click="goTaskDetail(task.projectId, task.id)"
            >
              <div class="recent-task-status" :class="task.status.toLowerCase().replace('_', '-')"></div>
              <div class="recent-task-content">
                <div class="recent-task-title">{{ task.title }}</div>
                <div class="recent-task-meta">
                  <span>{{ task.projectName }}</span>
                  <span>{{ task.statusLabel }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted, onUnmounted} from 'vue';
import {useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import * as echarts from 'echarts';
import {getPersonalDashboard} from '@/api/dashboard.js';
import {updateTaskStatus, submitReview} from '@/api/task.js';
import {
  Timer,
  Flag, Trophy, CircleCloseFilled, Clock, Histogram, Folder, Calendar,
  DataLine, Document, Refresh, Collection, Medal
} from '@element-plus/icons-vue';
import {useWebSocket} from '@/composables/useWebSocket.js';

const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('code_user') || '{}'));

const {addListener} = useWebSocket();
let removeWebSocketListener = null;

const handleWebSocketMessage = (data) => {
  if (data.type === 'STATS_UPDATE') {
    loadData();
  }
};

const stats = ref({
  todoCount: 0,
  overdueCount: 0,
  completedTaskCount: 0,
  projectCount: 0,
  plannedHours: 0,
  actualHours: 0,
  hoursRatio: 0
});

const todayTasks = ref([]);
const projects = ref([]);
const recentTasks = ref([]);
const statusDistribution = ref([]);
const trend = ref([]);
const scoreSummary = ref({scoredCount: 0, avgScore: 0, totalScore: 0, maxScore: 0});
const myScoredTasks = ref([]);

const statusChart = ref(null);
const trendChart = ref(null);
let chartInstances = {};

const scoredTasksPreview = computed(() => myScoredTasks.value.slice(0, 5));

const greetingText = computed(() => {
  const hour = new Date().getHours();
  if (hour < 6) return '夜深了';
  if (hour < 12) return '上午好';
  if (hour < 14) return '中午好';
  if (hour < 18) return '下午好';
  return '晚上好';
});

const summaryText = computed(() => {
  if (stats.value.overdueCount > 0) {
    return `当前有 ${stats.value.todoCount} 项待办，其中 ${stats.value.overdueCount} 项已逾期`;
  }
  if (stats.value.todoCount > 0) {
    return `当前有 ${stats.value.todoCount} 项待办`;
  }
  return '当前没有待办任务，可从实训项目中查看后续任务';
});

const projectStatusLabel = (status) => {
  const map = {NOT_STARTED: '未开始', IN_PROGRESS: '进行中', DELAYED: '已延期', COMPLETED: '已完成'};
  return map[status] || status;
};

const projectStatusType = (status) => {
  const map = {NOT_STARTED: 'info', IN_PROGRESS: 'primary', DELAYED: 'danger', COMPLETED: 'success'};
  return map[status] || 'info';
};

const formatDeadline = (deadline) => {
  if (!deadline) return '';
  const d = new Date(deadline);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  d.setHours(0, 0, 0, 0);
  const diff = d.getTime() - today.getTime();
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));
  if (days === 0) return '今日截止';
  if (days === 1) return '明日截止';
  return deadline.split(' ')[0] + ' 截止';
};

const changeTaskStatus = (task, status, successText) => {
  updateTaskStatus(task.id, status).then(() => {
    ElMessage.success(successText);
    loadData();
  }).catch(() => {});
};

const startTask = (task) => {
  changeTaskStatus(task, 'IN_PROGRESS', '任务已开始');
};

const completeTask = (task) => {
  changeTaskStatus(task, 'DONE', '任务已完成');
};

const submitTaskReview = (task) => {
  submitReview(task.id, task.reviewerId).then(() => {
    ElMessage.success('已提交审核，请等待审核人处理');
    loadData();
  }).catch(() => {});
};

const goTaskDetail = (projectId, taskId) => {
  router.push(`/project/${projectId}/board?taskId=${taskId}`);
};

const goProjectDetail = (projectId) => {
  router.push(`/project/${projectId}`);
};

const goProjects = () => {
  router.push('/student/project');
};

const goMyTasks = () => {
  router.push('/student/my-tasks');
};

const goMyScores = () => {
  router.push('/student/my-scores');
};

const handleRefresh = () => {
  loadData();
};

const renderStatusChart = () => {
  const chart = chartInstances.status;
  if (!chart || statusDistribution.value.length === 0) return;
  const statusMap = {TODO: '待处理', IN_PROGRESS: '进行中', REVIEW: '审核中', BLOCKED: '已阻塞', DONE: '已完成'};
  const colorMap = {TODO: '#E6A23C', IN_PROGRESS: '#409EFF', REVIEW: '#909399', BLOCKED: '#F56C6C', DONE: '#67C23A'};
  chart.setOption({
    tooltip: {trigger: 'item', formatter: '{b}: {c} ({d}%)'},
    legend: {bottom: 0, textStyle: {fontSize: 12}},
    series: [{
      type: 'pie',
      radius: '62%',
      center: ['50%', '44%'],
      data: statusDistribution.value.map(item => ({
        name: statusMap[item.status] || item.status,
        value: item.count,
        itemStyle: {color: colorMap[item.status] || '#909399'}
      })),
      label: {fontSize: 12}
    }]
  });
};

const renderTrendChart = () => {
  const chart = chartInstances.trend;
  if (!chart || trend.value.length === 0) return;
  chart.setOption({
    tooltip: {trigger: 'axis', formatter: '{b}: 完成 {c} 个任务'},
    grid: {left: '3%', right: '4%', bottom: '3%', containLabel: true},
    xAxis: {type: 'category', data: trend.value.map(item => item.date.slice(5)), axisLabel: {fontSize: 11}},
    yAxis: {type: 'value', minInterval: 1, axisLabel: {fontSize: 11}},
    series: [{
      data: trend.value.map(item => item.count),
      type: 'line',
      smooth: true,
      itemStyle: {color: '#409EFF'},
      areaStyle: {color: 'rgba(64,158,255,0.12)'},
      lineStyle: {width: 2},
      symbol: 'circle',
      symbolSize: 5
    }]
  });
};

const initCharts = () => {
  Object.values(chartInstances).forEach(instance => instance && instance.dispose());
  chartInstances = {};
  if (statusChart.value) {
    chartInstances.status = echarts.init(statusChart.value);
  }
  if (trendChart.value) {
    chartInstances.trend = echarts.init(trendChart.value);
  }
  renderStatusChart();
  renderTrendChart();
};

const loadData = () => {
  getPersonalDashboard().then(res => {
    const data = res.data || {};
    stats.value = {
      todoCount: data.todoCount || 0,
      overdueCount: data.overdueCount || 0,
      completedTaskCount: data.completedTaskCount || 0,
      projectCount: data.projectCount || 0,
      plannedHours: data.plannedHours || 0,
      actualHours: data.actualHours || 0,
      hoursRatio: data.hoursRatio || 0
    };
    todayTasks.value = data.todayTasks || [];
    projects.value = data.projects || [];
    recentTasks.value = data.recentTasks || [];
    statusDistribution.value = data.statusDistribution || [];
    trend.value = data.trend || [];
    scoreSummary.value = data.scoreSummary || {scoredCount: 0, avgScore: 0, totalScore: 0, maxScore: 0};
    myScoredTasks.value = data.myScoredTasks || [];

    setTimeout(initCharts, 0);
  }).catch(() => {
    ElMessage.error('加载实训数据失败');
  });
};

const handleResize = () => {
  Object.values(chartInstances).forEach(instance => instance && instance.resize());
};

onMounted(() => {
  loadData();
  window.addEventListener('resize', handleResize);
  removeWebSocketListener = addListener(handleWebSocketMessage);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  Object.values(chartInstances).forEach(instance => instance && instance.dispose());
  if (removeWebSocketListener) {
    removeWebSocketListener();
    removeWebSocketListener = null;
  }
});
</script>

<style scoped>
.workspace {
  padding: 28px 32px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1f2d3d;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

.btn-icon {
  margin-right: 4px;
}

.stats-strip {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 20px;
}

.stat-item {
  padding: 16px 18px;
  border-right: 1px solid #ebeef5;
}

.stat-item:last-child {
  border-right: none;
}

.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-number {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.stat-number.is-alert {
  color: #cf1322;
}

.stat-value {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.2;
  color: #1f2d3d;
  font-variant-numeric: tabular-nums;
}

.stat-value.text-danger {
  color: #f56c6c;
}

.stat-unit {
  margin-left: 2px;
  font-size: 12px;
  font-weight: 400;
  color: #909399;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 20px;
  align-items: start;
}

.primary-col,
.side-col {
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-width: 0;
}

.panel {
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.panel :deep(.el-card__header) {
  padding: 14px 18px;
  border-bottom: 1px solid #f0f2f5;
}

.panel :deep(.el-card__body) {
  padding: 16px 18px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.empty-tip {
  padding: 32px 0;
  text-align: center;
  color: #909399;
  font-size: 13px;
}

.empty-icon {
  font-size: 36px;
  margin-bottom: 8px;
  color: #c0c4cc;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid #f0f2f5;
  border-left: 3px solid #dcdfe6;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.task-item:hover {
  background-color: #f8fafc;
}

.task-item.overdue {
  border-left-color: #f56c6c;
  background-color: #fef7f7;
}

.task-item.blocked {
  border-left-color: #e6a23c;
}

.task-action {
  flex-shrink: 0;
}

.task-info {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.task-meta .el-icon {
  font-size: 12px;
  vertical-align: -1px;
}

.task-project {
  padding: 1px 6px;
  background: #f5f7fa;
  border-radius: 3px;
}

.task-overdue {
  color: #f56c6c;
}

.task-deadline {
  color: #909399;
}

.task-state {
  color: #909399;
}

.task-state-warning {
  color: #e6a23c;
}

.project-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.project-card {
  padding: 14px;
  border: 1px solid #f0f2f5;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.project-card:hover {
  border-color: #c6d7f0;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.project-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.project-progress {
  margin-bottom: 10px;
}

.project-progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  color: #909399;
}

.project-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
}

.project-meta .el-icon {
  font-size: 12px;
  vertical-align: -1px;
  margin-right: 4px;
}

.project-stats {
  display: flex;
  gap: 8px;
}

.stat-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
}

.stat-badge.pending {
  background: #ecf5ff;
  color: #409eff;
}

.stat-badge.completed {
  background: #f0f9eb;
  color: #67c23a;
}

.charts-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
}

.chart {
  width: 100%;
  height: 220px;
}

.chart-empty {
  height: 220px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  color: #909399;
}

.score-main {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 14px;
  border-bottom: 1px solid #f0f2f5;
}

.score-avg {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 84px;
  padding: 10px 0;
  background: #f7f8fa;
  border-radius: 8px;
}

.score-avg-value {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.1;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.score-avg-label {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.score-sub {
  flex: 1;
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.score-sub-item {
  flex: 1;
  text-align: center;
}

.score-sub-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.score-sub-value {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.score-empty {
  padding: 20px 0;
}

.scored-task-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 220px;
  overflow-y: auto;
  margin-top: 12px;
}

.scored-task-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.scored-task-item:hover {
  background: #f8fafc;
}

.scored-task-info {
  flex: 1;
  min-width: 0;
}

.scored-task-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.scored-task-project {
  margin-top: 2px;
  font-size: 11px;
  color: #909399;
}

.scored-task-score {
  margin-left: 12px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.hours-row {
  display: flex;
  gap: 24px;
}

.hours-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.hours-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

.hours-progress {
  margin-top: 16px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 12px;
  color: #909399;
}

.recent-task-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
  max-height: 240px;
  overflow-y: auto;
}

.recent-task-item {
  display: flex;
  align-items: center;
  padding: 8px 6px;
  border-radius: 6px;
  cursor: pointer;
}

.recent-task-item:hover {
  background: #f8fafc;
}

.recent-task-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 10px;
  flex-shrink: 0;
}

.recent-task-status.todo { background-color: #e6a23c; }
.recent-task-status.in-progress { background-color: #409eff; }
.recent-task-status.review { background-color: #909399; }
.recent-task-status.done { background-color: #67c23a; }
.recent-task-status.blocked { background-color: #f56c6c; }

.recent-task-content {
  flex: 1;
  min-width: 0;
}

.recent-task-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-task-meta {
  display: flex;
  gap: 8px;
  margin-top: 2px;
  font-size: 11px;
  color: #909399;
}

@media (max-width: 1400px) {
  .stats-strip {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .stat-item:nth-child(3) {
    border-right: none;
  }

  .stat-item:nth-child(-n+3) {
    border-bottom: 1px solid #ebeef5;
  }
}

@media (max-width: 1200px) {
  .content-grid {
    grid-template-columns: minmax(0, 1fr) 300px;
  }
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .workspace {
    padding: 20px 16px;
  }

  .stats-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .stat-item:nth-child(3) {
    border-right: 1px solid #ebeef5;
  }

  .stat-item:nth-child(2n) {
    border-right: none;
  }

  .stat-item:nth-child(-n+4) {
    border-bottom: 1px solid #ebeef5;
  }

  .stat-item:nth-child(n+5) {
    border-bottom: none;
  }

  .project-cards {
    grid-template-columns: 1fr;
  }
}
</style>
