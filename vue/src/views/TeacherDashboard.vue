<template>
  <div v-loading="loading" class="dashboard">
    <div class="filter-bar">
      <el-select
          v-model="selectedProjectIds"
          multiple
          filterable
          placeholder="选择项目（可选，默认全部）"
          class="project-filter"
          @change="handleProjectFilterChange"
      >
        <el-option v-for="p in availableProjects" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
      <el-button type="primary" size="small" @click="handleRefresh">刷新数据</el-button>
    </div>

    <div class="stats-strip">
      <div class="stat-item">
        <div class="stat-label">负责项目数</div>
        <div class="stat-number">{{ stats.projectCount }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">逾期任务</div>
        <div class="stat-number" :class="{ 'is-alert': stats.overdueTasks > 0 }">{{ stats.overdueTasks }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">总任务数</div>
        <div class="stat-number">{{ stats.totalTasks }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">待审核任务</div>
        <div class="stat-number" :class="{ 'is-alert': stats.pendingReviewTasks > 0 }">{{ stats.pendingReviewTasks }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">项目成员</div>
        <div class="stat-number">{{ stats.memberCount }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">完成率</div>
        <div class="stat-number">{{ stats.completionRate }}%</div>
      </div>
    </div>

    <div class="main-content">
      <div class="left-panel">
        <el-card class="review-tasks-card">
          <template #header>
            <div class="card-header">
              <el-icon><Search /></el-icon>
              <span>待审核任务</span>
              <div class="header-buttons">
                <el-button size="small" type="primary" plain @click="goBatchReview">批量审核</el-button>
                <el-button size="small" @click="refreshData">刷新</el-button>
              </div>
            </div>
          </template>
          <div v-if="reviewTasks.length === 0" class="empty-tip">
            <el-icon class="empty-icon"><Trophy /></el-icon>
            <div>暂无待审核任务</div>
          </div>
          <div v-else class="review-task-list">
            <div
                v-for="task in reviewTasks"
                :key="task.id"
                class="review-task-item"
                @click="goTaskDetail(task.projectId, task.id)"
            >
              <div class="review-task-priority" :class="task.priority?.toLowerCase() || 'low'"></div>
              <div class="review-task-info">
                <div class="review-task-title">{{ task.title }}</div>
                <div class="review-task-meta">
                  <span class="task-project">{{ task.projectName }}</span>
                  <span class="task-assignee"><el-icon><User /></el-icon>{{ task.assigneeName }}</span>
                </div>
              </div>
              <div class="review-task-actions">
                <el-button size="small" type="primary" @click.stop="approveTask(task.id)">通过</el-button>
                <el-button size="small" @click.stop="rejectTask(task.id)">驳回</el-button>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="trend-chart-card">
          <template #header>
            <el-icon><DataLine /></el-icon>
            <span>近7天完成趋势</span>
          </template>
          <div ref="trendChart" class="chart chart-large"></div>
        </el-card>

        <el-card class="status-chart-card">
          <template #header>
            <el-icon><PieChart /></el-icon>
            <span>项目状态分布</span>
          </template>
          <div ref="statusChart" class="chart"></div>
        </el-card>
      </div>

      <div class="right-panel">
        <el-card class="projects-card">
          <template #header>
            <div class="card-header">
              <el-icon><FolderOpened /></el-icon>
              <span>负责项目</span>
              <el-button size="small" @click="goProjectList">查看全部</el-button>
            </div>
          </template>
          <el-table :data="stats.projects.slice(0, 5)" stripe>
            <el-table-column prop="name" label="项目名称" min-width="160" show-overflow-tooltip />
            <el-table-column prop="totalTasks" label="任务" width="60" align="center" />
            <el-table-column prop="pendingReviewTasks" label="待审核" width="70" align="center" />
            <el-table-column prop="overdueTasks" label="逾期" width="60" align="center">
              <template #default="scope">
                <span :class="{ 'text-danger': scope.row.overdueTasks > 0 }">{{ scope.row.overdueTasks }}</span>
              </template>
            </el-table-column>
            <el-table-column label="进度" width="120">
              <template #default="scope">
                <el-progress :percentage="calculateProgress(scope.row)" :stroke-width="8" />
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="overdue-list-card">
          <template #header>
            <el-icon><WarningFilled /></el-icon>
            <span>逾期任务预警</span>
          </template>
          <div v-if="overdueTasksByProject.length === 0" class="empty-tip">
            <el-icon class="empty-icon"><CircleCheckFilled /></el-icon>
            <div>暂无逾期任务</div>
          </div>
          <div v-else class="overdue-group-list">
            <div v-for="group in overdueTasksByProject" :key="group.projectId" class="overdue-group">
              <div class="overdue-group-header">
                <el-icon><FolderOpened /></el-icon>
                <span>{{ group.projectName }}</span>
                <span class="overdue-group-count">{{ group.tasks.length }}项</span>
              </div>
              <div class="overdue-task-list">
                <div
                    v-for="task in group.tasks"
                    :key="task.id"
                    class="overdue-task-item"
                    @click="goTaskDetail(task.projectId, task.id)"
                >
                  <div class="overdue-days">{{ getOverdueDays(task.deadline) }}天</div>
                  <div class="overdue-info">
                    <div class="overdue-title">{{ task.title }}</div>
                    <div class="overdue-meta">{{ task.assigneeName }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-card>

        <el-card class="workload-card">
          <template #header>
            <div class="card-header">
              <el-icon><Histogram /></el-icon>
              <span>成员完成情况 TOP5</span>
              <el-button size="small" @click="goWorkload">查看全部</el-button>
            </div>
          </template>
          <div v-if="topWorkloadMembers.length === 0" class="empty-tip">
            <el-icon class="empty-icon"><DataLine /></el-icon>
            <div>暂无完成情况数据</div>
          </div>
          <div v-else class="workload-list-flat">
            <div
                v-for="(item, index) in topWorkloadMembers"
                :key="item.userId"
                class="workload-item-flat"
            >
              <div class="workload-rank" :class="getRankClass(index)">{{ index + 1 }}</div>
              <div class="workload-info">
                <div class="workload-name">{{ item.username || item.userId }}</div>
                <el-progress :percentage="calculateWorkloadPercent(item)" :stroke-width="6" />
              </div>
              <div class="workload-count">{{ item.completedTasks || 0 }}/{{ item.totalTasks || 0 }}</div>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import * as echarts from 'echarts';
import { getTeacherDashboard } from '@/api/dashboard.js';
import { approveReview, rejectReview } from '@/api/task.js';
import {
  FolderOpened, Search, WarningFilled, CircleCheckFilled,
  Trophy, User, Histogram, DataLine, PieChart
} from '@element-plus/icons-vue';

const router = useRouter();
const loading = ref(false);
const stats = ref({
  projectCount: 0,
  totalTasks: 0,
  pendingReviewTasks: 0,
  overdueTasks: 0,
  memberCount: 0,
  completionRate: 0,
  projects: [],
  trend: [],
  trendByProject: [],
  statusDistribution: []
});

const reviewTasks = ref([]);
const overdueTasks = ref([]);
const overdueTasksByProject = ref([]);
const workload = ref([]);
const workloadByProject = ref([]);

const selectedProjectIds = ref([]);
const availableProjects = ref([]);
const allProjects = ref([]);

const allReviewTasks = ref([]);
const allOverdueTasks = ref([]);
const allWorkloadByProject = ref([]);
const allTrendByProject = ref([]);
const allStatusDistribution = ref([]);
const allStatsProjects = ref([]);

const statusChart = ref(null);
const trendChart = ref(null);
let chartInstances = {};

const calculateProgress = (row) => {
  const total = row.totalTasks || 0;
  const completed = row.completedTasks || 0;
  return total === 0 ? 0 : Math.round((completed * 100) / total);
};

const calculateWorkloadPercent = (item) => {
  const total = item.totalTasks || 0;
  const completed = item.completedTasks || 0;
  return total === 0 ? 0 : Math.round((completed * 100) / total);
};

const getRankClass = (index) => {
  if (index === 0) return 'rank-1';
  if (index === 1) return 'rank-2';
  if (index === 2) return 'rank-3';
  return '';
};

const topWorkloadMembers = computed(() => {
  const memberMap = new Map();
  workloadByProject.value.forEach(group => {
    (group.members || []).forEach(member => {
      const key = member.userId;
      if (!memberMap.has(key)) {
        memberMap.set(key, { userId: member.userId, username: member.username, totalTasks: 0, completedTasks: 0 });
      }
      const m = memberMap.get(key);
      m.totalTasks += member.totalTasks || 0;
      m.completedTasks += member.completedTasks || 0;
    });
  });
  return Array.from(memberMap.values())
    .sort((a, b) => b.totalTasks - a.totalTasks)
    .slice(0, 5);
});

const getOverdueDays = (deadline) => {
  if (!deadline) return 0;
  const d = new Date(deadline);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  d.setHours(0, 0, 0, 0);
  const diff = today.getTime() - d.getTime();
  return Math.floor(diff / (1000 * 60 * 60 * 24));
};

const goProject = (id) => {
  router.push(`/project/${id}`);
};

const goProjectList = () => {
  router.push('/teacher/project?tab=list');
};

const goWorkload = () => {
  router.push('/teacher/data-analysis?tab=member');
};

const goTaskDetail = (projectId, taskId) => {
  router.push(`/project/${projectId}/board?taskId=${taskId}`);
};

const goBatchReview = () => {
  router.push('/teacher/training-tasks?pending=1');
};

const approveTask = (taskId) => {
  ElMessageBox.prompt('请输入该任务的评分（0-100，可留空）', '审核通过', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：90',
    inputValidator: (value) => {
      if (!value || !value.trim()) return true;
      const num = Number(value);
      if (isNaN(num) || num < 0 || num > 100) return '评分需为 0-100 之间的数字';
      return true;
    }
  }).then(({ value }) => {
    const score = value && value.trim() ? Number(value) : null;
    approveReview(taskId, score).then(() => {
      ElMessage.success('审核通过');
      refreshData();
    }).catch(() => {
      ElMessage.error('操作失败');
    });
  }).catch(() => {});
};

const rejectTask = (taskId) => {
  ElMessageBox.prompt('请输入驳回原因', '驳回审核', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '请说明驳回原因，以便任务负责人改进',
    inputValidator: (value) => {
      if (!value || !value.trim()) {
        return '请输入驳回原因';
      }
    }
  }).then(({ value }) => {
    rejectReview(taskId, value.trim()).then(() => {
      ElMessage.success('已驳回');
      refreshData();
    }).catch(() => {
      ElMessage.error('操作失败');
    });
  }).catch(() => {});
};

const initCharts = () => {
  Object.values(chartInstances).forEach(instance => instance && instance.dispose());
  chartInstances = {};
  if (statusChart.value) {
    chartInstances['status'] = echarts.init(statusChart.value);
  }
  if (trendChart.value) {
    chartInstances['trend'] = echarts.init(trendChart.value);
  }
};

const renderCharts = () => {
    renderStatusChart(stats.value.statusDistribution);
    renderTrendChart();
  };

const renderStatusChart = (data) => {
  const chart = chartInstances['status'];
  if (!chart || !data || data.length === 0) return;
  const statusMap = { NOT_STARTED: '未开始', IN_PROGRESS: '进行中', DELAYED: '已延期', COMPLETED: '已完成' };
  const colorMap = { NOT_STARTED: '#909399', IN_PROGRESS: '#409EFF', DELAYED: '#F56C6C', COMPLETED: '#67C23A' };
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['50%', '45%'],
      data: data.map(item => ({
        name: statusMap[item.status] || item.status,
        value: item.count,
        itemStyle: { color: colorMap[item.status] || '#909399' }
      })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } },
      label: { fontSize: 12 }
    }]
  });
};

const chartColors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#C0C4CC', '#722ED1', '#EB2F96', '#13C2C2', '#FA8C16'];

const renderTrendChart = () => {
    const chart = chartInstances['trend'];
    if (!chart) return;
    
    const trendByProject = stats.value.trendByProject || [];
    const trend = stats.value.trend || [];
    const hasProjectData = trendByProject.length > 0;
    
    const dates = trend.map(item => item.date);
    if (dates.length === 0) return;
    
    let series = [];
    let legendData = [];
    
    if (hasProjectData) {
      const topProjects = trendByProject.slice(0, 8);
      topProjects.forEach((project, index) => {
        const color = chartColors[index % chartColors.length];
        legendData.push(project.projectName);
        series.push({
          name: project.projectName,
          data: (project.data || []).map(item => item.count),
          type: 'line',
          smooth: true,
          itemStyle: { color },
          lineStyle: { width: 2 },
          symbol: 'circle',
          symbolSize: 6
        });
      });
    }
    
    legendData.push('合计');
    series.push({
      name: '合计',
      data: trend.map(item => item.count),
      type: 'line',
      smooth: true,
      itemStyle: { color: '#303133' },
      lineStyle: { width: 3, type: 'dashed' },
      areaStyle: { color: 'rgba(64,158,255,0.08)' },
      symbol: 'circle',
      symbolSize: 8
    });
    
    chart.setOption({
      tooltip: { 
        trigger: 'axis',
        formatter: (params) => {
          let result = `<div style="font-weight:bold;margin-bottom:8px">${params[0].axisValue}</div>`;
          params.forEach(p => {
            result += `<div style="display:flex;align-items:center;margin:4px 0">
              <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color};margin-right:8px"></span>
              <span>${p.seriesName}: <strong>${p.value}</strong> 个任务</span>
            </div>`;
          });
          return result;
        }
      },
      legend: { 
        data: legendData, 
        bottom: 0, 
        textStyle: { fontSize: 11 },
        type: 'scroll',
        pageTextStyle: { fontSize: 10 }
      },
      grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
      xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11, rotate: 30 } },
      yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
      series
    });
  };

const refreshData = () => {
    loadData();
  };

  const handleRefresh = () => {
    selectedProjectIds.value = [];
    loadData();
  };

  const handleProjectFilterChange = () => {
    applyProjectFilter();
  };

  const applyProjectFilter = () => {
    const selected = selectedProjectIds.value || [];
    if (selected.length === 0) {
      loadData();
      return;
    }
    
    const filteredProjects = allStatsProjects.value.filter(p => selected.includes(p.id));
    stats.value.projects = filteredProjects;
    
    const filteredReview = allReviewTasks.value.filter(t => selected.includes(t.projectId));
    reviewTasks.value = filteredReview;
    
    const filteredOverdue = allOverdueTasks.value.filter(t => selected.includes(t.projectId));
    overdueTasks.value = filteredOverdue;
    const overdueMap = {};
    filteredOverdue.forEach(item => {
      const projectId = item.projectId;
      if (!overdueMap[projectId]) {
        overdueMap[projectId] = { projectId, projectName: item.projectName, tasks: [] };
      }
      overdueMap[projectId].tasks.push(item);
    });
    overdueTasksByProject.value = Object.values(overdueMap);
    
    const filteredWorkload = allWorkloadByProject.value.filter(g => selected.includes(g.projectId));
    workloadByProject.value = filteredWorkload;
    
    const filteredTrendByProject = allTrendByProject.value.filter(t => selected.includes(t.projectId));
    stats.value.trendByProject = filteredTrendByProject;
    
    const statusCountMap = {};
    filteredProjects.forEach(p => {
      statusCountMap[p.status] = (statusCountMap[p.status] || 0) + 1;
    });
    stats.value.statusDistribution = Object.entries(statusCountMap).map(([status, count]) => ({ status, count }));
    
    const totalTasks = filteredProjects.reduce((sum, p) => sum + (p.totalTasks || 0), 0);
    const completedTasks = filteredProjects.reduce((sum, p) => sum + (p.completedTasks || 0), 0);
    const projectCount = filteredProjects.length;
    const overdueTasksCount = filteredOverdue.length;
    const pendingReviewTasksCount = filteredReview.length;
    
    const memberSet = new Set();
    filteredWorkload.forEach(group => {
      group.members.forEach(m => memberSet.add(m.userId));
    });
    const memberCount = memberSet.size;
    
    const completionRate = totalTasks === 0 ? 0 : (completedTasks / totalTasks) * 100;
    
    const dates = stats.value.trend.map(item => item.date);
    const newTrend = dates.map(date => {
      let count = 0;
      filteredTrendByProject.forEach(project => {
        const dataItem = (project.data || []).find(d => d.date === date);
        count += dataItem ? dataItem.count : 0;
      });
      return { date, count };
    });
    stats.value.trend = newTrend;
    
    stats.value.totalTasks = totalTasks;
    stats.value.projectCount = projectCount;
    stats.value.overdueTasks = overdueTasksCount;
    stats.value.pendingReviewTasks = pendingReviewTasksCount;
    stats.value.memberCount = memberCount;
    stats.value.completionRate = completionRate;
    
    setTimeout(() => {
      renderCharts();
    }, 0);
  };

  const loadData = () => {
    loading.value = true;
    getTeacherDashboard().then(res => {
      const data = res.data || {};
      stats.value = {
        projectCount: data.projectCount || 0,
        totalTasks: data.totalTasks || 0,
        pendingReviewTasks: data.pendingReviewTasks || 0,
        overdueTasks: data.overdueTasks || 0,
        memberCount: data.memberCount || 0,
        completionRate: data.completionRate || 0,
        projects: data.projects || [],
        trend: data.trend || [],
        trendByProject: data.trendByProject || [],
        statusDistribution: data.statusDistribution || []
      };
      
      allProjects.value = (data.projects || []).map(p => ({ id: p.id, name: p.name }));
      availableProjects.value = [...allProjects.value];
      
      reviewTasks.value = data.reviewTasks || [];
      allReviewTasks.value = data.reviewTasks || [];
      
      overdueTasks.value = data.overdueTasksList || [];
      allOverdueTasks.value = data.overdueTasksList || [];
      
      const rawOverdue = data.overdueTasksList || [];
      const overdueProjectMap = {};
      rawOverdue.forEach(item => {
        const projectId = item.projectId;
        if (!overdueProjectMap[projectId]) {
          overdueProjectMap[projectId] = {
            projectId: projectId,
            projectName: item.projectName,
            tasks: []
          };
        }
        overdueProjectMap[projectId].tasks.push(item);
      });
      overdueTasksByProject.value = Object.values(overdueProjectMap);
      
      workload.value = data.workload || [];
      
      const rawWorkload = data.workloadByProject || [];
      const projectMap = {};
      rawWorkload.forEach(item => {
        const projectId = item.projectId;
        if (!projectMap[projectId]) {
          projectMap[projectId] = {
            projectId: projectId,
            projectName: item.projectName,
            members: []
          };
        }
        projectMap[projectId].members.push({
          userId: item.userId,
          username: item.username,
          totalTasks: item.totalTasks,
          completedTasks: item.completedTasks
        });
      });
      workloadByProject.value = Object.values(projectMap);
      allWorkloadByProject.value = Object.values(JSON.parse(JSON.stringify(projectMap)));
      
      allTrendByProject.value = data.trendByProject || [];
      allStatusDistribution.value = data.statusDistribution || [];
      allStatsProjects.value = data.projects || [];
      
      loading.value = false;
      setTimeout(() => {
        initCharts();
        renderCharts();
      }, 0);
    }).catch(() => {
      loading.value = false;
    });
  };

const handleResize = () => {
  Object.values(chartInstances).forEach(instance => instance && instance.resize());
};

onMounted(() => {
  loadData();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  Object.values(chartInstances).forEach(instance => instance && instance.dispose());
});
</script>

<style scoped>
.dashboard {
  padding: 28px 32px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.project-filter {
  width: 320px;
}

.stats-strip {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  margin-bottom: 20px;
}

.stat-item {
  padding: 16px 20px;
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

.main-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

.chart-large {
  height: 280px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.empty-tip {
  text-align: center;
  padding: 30px 0;
  color: var(--text-tertiary);
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 10px;
}

.review-tasks-card,
.workload-card,
.projects-card,
.trend-chart-card,
.status-chart-card,
.overdue-list-card {
  border-radius: var(--border-radius-lg);
  box-shadow: var(--shadow-sm);
  border: 1px solid rgba(79, 124, 255, 0.06);
  margin-bottom: 20px;
}

.projects-card,
.trend-chart-card {
  box-shadow: var(--shadow-md);
}

.review-task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-task-item {
  display: flex;
  align-items: center;
  padding: 12px;
  background-color: #fff;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 4px solid #e6a23c;
}

.review-task-item:hover { box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08); }

.review-task-priority {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 10px;
}

.review-task-priority.high { background-color: #f56c6c; }
.review-task-priority.medium { background-color: #e6a23c; }
.review-task-priority.low { background-color: #67c23a; }

.review-task-info {
  flex: 1;
  min-width: 0;
}

.review-task-title {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.review-task-meta {
  display: flex;
  gap: 8px;
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.task-project {
  background-color: #f5f7fa;
  padding: 2px 8px;
  border-radius: 4px;
}

.review-task-actions {
  display: flex;
  gap: 8px;
  margin-left: 10px;
}

.workload-card {
  max-height: 340px;
  overflow-y: auto;
}

.workload-card :deep(.el-card__body) {
  overflow-y: auto;
  max-height: calc(340px - 60px);
}

.workload-rank {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  background-color: #f5f7fa;
  border-radius: 50%;
}

.workload-rank.rank-1 { background-color: #FFD700; color: #fff; }
.workload-rank.rank-2 { background-color: #C0C0C0; color: #fff; }
.workload-rank.rank-3 { background-color: #CD7F32; color: #fff; }

.workload-info {
  flex: 1;
}

.workload-name {
  font-size: 13px;
  color: #303133;
  margin-bottom: 5px;
}

.workload-count {
  font-size: 12px;
  font-weight: bold;
  color: #606266;
  min-width: 60px;
  text-align: right;
}

.chart {
  width: 100%;
  height: 220px;
}

.overdue-group-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.overdue-group {
  background-color: #fafbfc;
  border-radius: 8px;
  padding: 10px;
}

.overdue-group-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}

.overdue-group-header .el-icon {
  font-size: 14px;
  color: #409EFF;
}

.overdue-group-count {
  font-size: 12px;
  font-weight: normal;
  color: #909399;
  margin-left: auto;
  background-color: #fff;
  padding: 2px 8px;
  border-radius: 10px;
}

.overdue-task-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.overdue-task-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background-color: #fff9f9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 4px solid #f56c6c;
}

.overdue-task-item:hover { background-color: #fff5f5; }

.overdue-days {
  font-size: 14px;
  font-weight: bold;
  color: #f56c6c;
  min-width: 40px;
}

.overdue-info {
  flex: 1;
  min-width: 0;
}

.overdue-title {
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.overdue-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.workload-list-flat {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.workload-item-flat {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  background-color: #fafbfc;
  border-radius: 6px;
}

@media (max-width: 1400px) {
  .main-content { grid-template-columns: 1fr 1fr; }
}

@media (max-width: 1200px) {
  .stats-strip { grid-template-columns: repeat(3, 1fr); }
  .stat-item:nth-child(3) { border-right: none; }
  .stat-item:nth-child(-n+3) { border-bottom: 1px solid #ebeef5; }
}

@media (max-width: 768px) {
  .stats-strip { grid-template-columns: repeat(2, 1fr); }
  .stat-item:nth-child(3) { border-right: 1px solid #ebeef5; }
  .stat-item:nth-child(2n) { border-right: none; }
  .stat-item:nth-child(-n+4) { border-bottom: 1px solid #ebeef5; }
  .stat-item:nth-child(n+5) { border-bottom: none; }
  .main-content { grid-template-columns: 1fr; }
}
</style>