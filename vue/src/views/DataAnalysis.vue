<template>
  <div class="data-analysis">
    <header class="page-head">
      <div class="head-left">
        <h1>数据分析</h1>
        <p>多维度统计分析项目进度与成员完成情况</p>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="analysis-tabs">
      <el-tab-pane label="项目统计" name="project">
        <div v-loading="projectLoading" class="tab-content">
          <div class="tab-toolbar">
            <el-select
              v-model="selectedProjectId"
              placeholder="请选择项目"
              style="width: 260px"
              :loading="loadingProjects"
              filterable
              clearable
              @change="onProjectChange"
            >
              <el-option
                v-for="item in projectOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
            <el-button type="primary" @click="exportReport">导出报表</el-button>
          </div>

          <el-row :gutter="20">
            <el-col :span="canViewPrivacy ? 8 : 12">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <el-icon><PieChart /></el-icon>
                    <span>任务状态占比</span>
                  </div>
                </template>
                <div ref="statusChart" class="chart chart-status"></div>
              </el-card>
            </el-col>
            <el-col v-if="canViewPrivacy" :span="16">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <el-icon><Histogram /></el-icon>
                    <span>成员完成情况分布</span>
                  </div>
                </template>
                <div ref="workloadChart" class="chart chart-workload"></div>
              </el-card>
            </el-col>
            <el-col v-else :span="12">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <el-icon><TrendCharts /></el-icon>
                    <span>项目进度趋势</span>
                  </div>
                </template>
                <div ref="progressChart" class="chart chart-progress"></div>
              </el-card>
            </el-col>
          </el-row>

          <el-row :gutter="20" style="margin-top: 20px">
            <el-col :span="canViewPrivacy ? 12 : 12">
              <el-card v-if="canViewPrivacy">
                <template #header>
                  <div class="card-header">
                    <el-icon><TrendCharts /></el-icon>
                    <span>项目进度趋势</span>
                  </div>
                </template>
                <div ref="progressChart" class="chart chart-progress"></div>
              </el-card>
            </el-col>
            <el-col :span="canViewPrivacy ? 12 : 12">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <el-icon><DataLine /></el-icon>
                    <span>燃尽图</span>
                  </div>
                </template>
                <div ref="burndownChart" class="chart chart-burndown"></div>
              </el-card>
            </el-col>
          </el-row>

          <el-row v-if="canViewPrivacy" :gutter="20" style="margin-top: 20px">
            <el-col :span="24">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <el-icon><Clock /></el-icon>
                    <span>学时统计（预估 vs 实际）</span>
                  </div>
                </template>
                <div ref="hoursChart" class="chart chart-hours"></div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <el-tab-pane label="成员负载" name="member">
        <div v-loading="memberLoading" class="tab-content">
          <div class="tab-toolbar">
            <el-select
              v-model="memberProjectId"
              placeholder="全部项目"
              style="width: 260px"
              :loading="loadingProjects"
              filterable
              clearable
              @change="onMemberProjectChange"
            >
              <el-option
                v-for="item in projectOptions"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </div>
          <div class="stats-strip">
            <div class="stat-item">
              <div class="stat-label">项目成员</div>
              <div class="stat-number">{{ summary.memberCount }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">总任务数</div>
              <div class="stat-number">{{ summary.totalTasks }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">进行中任务</div>
              <div class="stat-number">{{ summary.inProgressTasks }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">逾期任务</div>
              <div class="stat-number" :class="{ 'is-alert': summary.overdueTasks > 0 }">{{ summary.overdueTasks }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">平均完成率</div>
              <div class="stat-number">{{ summary.avgCompletionRate }}<span class="stat-unit">%</span></div>
            </div>
          </div>

          <el-row :gutter="20" class="chart-row">
            <el-col :span="12">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <el-icon><Histogram /></el-icon>
                    <span>成员任务数分布</span>
                  </div>
                </template>
                <div ref="tasksChart" class="chart"></div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <el-icon><Clock /></el-icon>
                    <span>学时对比（预估 vs 实际）</span>
                  </div>
                </template>
                <div ref="memberHoursChart" class="chart"></div>
              </el-card>
            </el-col>
          </el-row>

          <el-card class="table-card" shadow="never">
            <template #header>
              <div class="table-header">
                <span>成员负载明细</span>
                <div class="table-filters">
                  <el-select
                    v-if="memberProjectId"
                    v-model="groupFilter"
                    size="small"
                    style="width: 160px"
                    @change="applyFilters"
                  >
                    <el-option label="全部分组" value="all" />
                    <el-option
                      v-for="g in groupOptions"
                      :key="g.id"
                      :label="g.name || g.groupNo"
                      :value="String(g.id)"
                    />
                    <el-option label="未分组" value="ungrouped" />
                  </el-select>
                  <el-input
                    v-model="searchKeyword"
                    placeholder="搜索成员姓名"
                    clearable
                    style="width: 220px"
                    @input="applyFilters"
                  />
                </div>
              </div>
            </template>
            <el-table :data="pagedList" style="width: 100%">
              <el-table-column prop="nickname" label="成员" min-width="140" show-overflow-tooltip>
                <template #default="scope">
                  <div class="member-cell">
                    <el-avatar :size="32" class="member-avatar">{{ avatarText(scope.row.nickname || scope.row.username) }}</el-avatar>
                    <div class="member-info">
                      <div class="member-name">{{ scope.row.nickname || scope.row.username }}</div>
                      <div class="member-username">@{{ scope.row.username }}</div>
                    </div>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="totalTasks" label="总任务" width="100" sortable />
              <el-table-column prop="todoTasks" label="待办" width="90" sortable />
              <el-table-column prop="inProgressTasks" label="进行中" width="100" sortable />
              <el-table-column prop="reviewTasks" label="审核中" width="100" sortable />
              <el-table-column prop="doneTasks" label="已完成" width="100" sortable />
              <el-table-column prop="overdueTasks" label="逾期" width="90" sortable>
                <template #default="scope">
                  <span :class="{ 'text-danger': scope.row.overdueTasks > 0 }">{{ scope.row.overdueTasks }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="plannedHours" label="计划学时" width="110" sortable>
                <template #default="scope">{{ scope.row.plannedHours }} 学时</template>
              </el-table-column>
              <el-table-column prop="actualHours" label="实际学时" width="110" sortable>
                <template #default="scope">{{ scope.row.actualHours }} 学时</template>
              </el-table-column>
              <el-table-column prop="completionRate" label="完成率" width="110" sortable>
                <template #default="scope">
                  <el-progress :percentage="scope.row.completionRate" :stroke-width="6" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" fixed="right">
                <template #default="scope">
                  <el-button link type="primary" @click="viewMemberTasks(scope.row)">查看任务</el-button>
                </template>
              </el-table-column>
            </el-table>
            <el-pagination
              v-if="displayList.length > 0"
              v-model:current-page="currentPage"
              v-model:page-size="pageSize"
              :page-sizes="[10, 20, 50, 100]"
              :total="displayList.length"
              layout="total, sizes, prev, pager, next"
              class="table-pagination"
            />
            <el-empty v-if="!memberLoading && displayList.length === 0" description="暂无成员数据" />
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import * as echarts from 'echarts';
import {
  PieChart, Histogram, TrendCharts, DataLine, Clock
} from '@element-plus/icons-vue';
import { getProjectStatistics, getMemberLoad } from '@/api/dashboard.js';
import { listProjects } from '@/api/project.js';
import { listGroups, listGroupMembers } from '@/api/training.js';
import request from '@/utils/request.js';
import { ElMessage } from 'element-plus';

const router = useRouter();
const route = useRoute();
const activeTab = ref(route.query.tab === 'member' ? 'member' : 'project');

// ===== 项目统计 =====
const projectLoading = ref(false);
const selectedProjectId = ref(null);
const projectOptions = ref([]);
const loadingProjects = ref(false);

const userStr = localStorage.getItem('code_user');
const currentUser = userStr ? JSON.parse(userStr) : {};
const canViewPrivacy = computed(() => {
  const role = currentUser.role;
  return role === 'ADMIN' || role === 'TEACHER';
});

const stats = ref({
  totalTasks: 0,
  statusDistribution: [],
  memberWorkload: [],
  progressTrend: [],
  burndown: [],
  hoursStats: []
});

const statusChart = ref(null);
const workloadChart = ref(null);
const progressChart = ref(null);
const burndownChart = ref(null);
const hoursChart = ref(null);
let projectChartInstances = {};

const statusNameMap = {
  TODO: '待办',
  IN_PROGRESS: '进行中',
  REVIEW: '审核中',
  DONE: '已完成',
  BLOCKED: '已阻塞'
};

const initProjectCharts = () => {
  Object.values(projectChartInstances).forEach(instance => instance && instance.dispose());
  projectChartInstances = {};
  const refs = [
    { ref: statusChart, key: 'status' },
    { ref: workloadChart, key: 'workload' },
    { ref: progressChart, key: 'progress' },
    { ref: burndownChart, key: 'burndown' },
    { ref: hoursChart, key: 'hours' }
  ];
  refs.forEach(item => {
    if (item.ref.value) {
      projectChartInstances[item.key] = echarts.init(item.ref.value);
    }
  });
};

const renderProjectCharts = () => {
  renderStatusChart(stats.value.statusDistribution);
  renderProgressChart(stats.value.progressTrend);
  renderBurndownChart(stats.value.burndown);
  if (canViewPrivacy.value) {
    renderWorkloadChart(stats.value.memberWorkload);
    renderHoursChart(stats.value.hoursStats);
  }
};

const renderStatusChart = (data) => {
  const chart = projectChartInstances['status'];
  if (!chart) return;
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '60%',
      data: data.map(item => ({ name: statusNameMap[item.status] || item.status, value: item.cnt })),
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  });
};

const renderWorkloadChart = (data) => {
  const chart = projectChartInstances['workload'];
  if (!chart) return;
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(item => item.username || item.userId || '未知') },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ data: data.map(item => item.cnt), type: 'bar', itemStyle: { color: '#67C23A' } }]
  });
};

const renderProgressChart = (data) => {
  const chart = projectChartInstances['progress'];
  if (!chart) return;
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(item => item.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ data: data.map(item => item.count), type: 'line', smooth: true, itemStyle: { color: '#409EFF' } }]
  });
};

const renderBurndownChart = (data) => {
  const chart = projectChartInstances['burndown'];
  if (!chart) return;
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(item => item.date) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ data: data.map(item => item.remaining), type: 'line', itemStyle: { color: '#E6A23C' }, areaStyle: { color: 'rgba(230,162,60,0.2)' } }]
  });
};

const renderHoursChart = (data) => {
  const chart = projectChartInstances['hours'];
  if (!chart) return;
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['计划学时', '实际学时'] },
    xAxis: { type: 'category', data: data.map(item => item.username || item.userId || '未知') },
    yAxis: { type: 'value' },
    series: [
      { name: '计划学时', type: 'bar', data: data.map(item => item.planned), itemStyle: { color: '#409EFF' } },
      { name: '实际学时', type: 'bar', data: data.map(item => item.actual), itemStyle: { color: '#67C23A' } }
    ]
  });
};

const loadProjectStats = () => {
  const id = selectedProjectId.value;
  if (!id) return;
  projectLoading.value = true;
  getProjectStatistics(id).then(res => {
    stats.value = res.data;
    projectLoading.value = false;
    nextTick(() => {
      initProjectCharts();
      renderProjectCharts();
    });
  }).catch(() => {
    projectLoading.value = false;
  });
};

const loadProjects = () => {
  loadingProjects.value = true;
  listProjects({ pageNum: 1, pageSize: 1000 })
    .then(res => {
      projectOptions.value = res.data.records || [];
      if (projectOptions.value.length > 0) {
        selectedProjectId.value = projectOptions.value[0].id;
        loadProjectStats();
      }
    })
    .catch(() => {
      ElMessage.error('加载项目列表失败');
    })
    .finally(() => {
      loadingProjects.value = false;
    });
};

const onProjectChange = (val) => {
  if (!val) return;
  loadProjectStats();
};

const exportReport = () => {
  const id = selectedProjectId.value;
  if (!id) {
    ElMessage.warning('请先选择项目');
    return;
  }
  const userStr = localStorage.getItem('code_user');
  const user = userStr ? JSON.parse(userStr) : {};
  const token = user.token || '';
  const url = `${request.defaults.baseURL}/admin/export/tasks?projectId=${id}`;
  const xhr = new XMLHttpRequest();
  xhr.open('GET', url, true);
  xhr.setRequestHeader('token', token);
  xhr.responseType = 'blob';
  xhr.onload = function() {
    if (xhr.status === 200) {
      const blob = new Blob([xhr.response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `项目任务_${id}_${new Date().toISOString().slice(0,10)}.xlsx`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    } else {
      ElMessage.error('导出失败，请检查登录状态');
    }
  };
  xhr.send();
};

// ===== 成员负载 =====
const memberLoading = ref(false);
const memberProjectId = ref(null);
const rawList = ref([]);
const displayList = ref([]);
const searchKeyword = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const groupFilter = ref('all');
const groupOptions = ref([]);
const studentGroupMap = ref({});

const pagedList = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value;
  return displayList.value.slice(start, start + pageSize.value);
});

watch(pageSize, () => {
  currentPage.value = 1;
});

const summary = reactive({
  memberCount: 0,
  totalTasks: 0,
  inProgressTasks: 0,
  overdueTasks: 0,
  avgCompletionRate: 0
});

const tasksChart = ref(null);
const memberHoursChart = ref(null);
let memberChartInstances = {};

const avatarText = (name) => (name ? name.charAt(0).toUpperCase() : '?');

const toNumber = (val) => {
  if (val == null) return 0;
  if (typeof val === 'number') return val;
  return Number(val) || 0;
};

const loadMemberLoad = () => {
  memberLoading.value = true;
  getMemberLoad(memberProjectId.value || null).then(res => {
    const list = (res.data || []).map(item => ({
      userId: item.userId,
      username: item.username || '',
      nickname: item.nickname || '',
      totalTasks: toNumber(item.totalTasks),
      todoTasks: toNumber(item.todoTasks),
      inProgressTasks: toNumber(item.inProgressTasks),
      reviewTasks: toNumber(item.reviewTasks),
      doneTasks: toNumber(item.doneTasks),
      overdueTasks: toNumber(item.overdueTasks),
      plannedHours: toNumber(item.plannedHours),
      actualHours: toNumber(item.actualHours),
      completionRate: toNumber(item.completionRate)
    }));
    rawList.value = list;
    applyFilters();
    calcSummary(list);
    memberLoading.value = false;
    nextTick(() => {
      initMemberCharts();
      renderMemberCharts(list);
    });
  }).catch(() => {
    memberLoading.value = false;
  });
};

const onMemberProjectChange = () => {
  groupFilter.value = 'all';
  loadGroupData(memberProjectId.value);
  loadMemberLoad();
};

const loadGroupData = async (projectId) => {
  if (!projectId) {
    groupOptions.value = [];
    studentGroupMap.value = {};
    return;
  }
  try {
    const res = await listGroups(projectId);
    const groups = res.data || [];
    groupOptions.value = groups;
    const map = {};
    await Promise.all(groups.map(async g => {
      try {
        const memberRes = await listGroupMembers(g.id);
        (memberRes.data || []).forEach(m => { map[m.studentId] = g.id; });
      } catch (error) {
        // ignore
      }
    }));
    studentGroupMap.value = map;
  } catch (error) {
    groupOptions.value = [];
    studentGroupMap.value = {};
  }
};

const calcSummary = (list) => {
  summary.memberCount = list.length;
  summary.totalTasks = list.reduce((sum, item) => sum + item.totalTasks, 0);
  summary.inProgressTasks = list.reduce((sum, item) => sum + item.inProgressTasks, 0);
  summary.overdueTasks = list.reduce((sum, item) => sum + item.overdueTasks, 0);
  const rate = list.length === 0 ? 0 : list.reduce((sum, item) => sum + item.completionRate, 0) / list.length;
  summary.avgCompletionRate = Number(rate.toFixed(2));
};

const applyFilters = () => {
  let list = rawList.value;
  const kw = searchKeyword.value.trim().toLowerCase();
  if (kw) {
    list = list.filter(item => (item.nickname || item.username || '').toLowerCase().includes(kw));
  }
  if (memberProjectId.value && groupFilter.value !== 'all') {
    if (groupFilter.value === 'ungrouped') {
      list = list.filter(item => !studentGroupMap.value[item.userId]);
    } else {
      list = list.filter(item => studentGroupMap.value[item.userId] === Number(groupFilter.value));
    }
  }
  displayList.value = list;
  currentPage.value = 1;
};

const initMemberCharts = () => {
  Object.values(memberChartInstances).forEach(instance => instance && instance.dispose());
  memberChartInstances = {};
  if (tasksChart.value) {
    memberChartInstances['tasks'] = echarts.init(tasksChart.value);
  }
  if (memberHoursChart.value) {
    memberChartInstances['hours'] = echarts.init(memberHoursChart.value);
  }
};

const renderMemberCharts = (list) => {
  renderTasksChart(list);
  renderMemberHoursChart(list);
};

const renderTasksChart = (list) => {
  const chart = memberChartInstances['tasks'];
  if (!chart) return;
  const names = list.map(item => item.nickname || item.username || '未知');
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['待办', '进行中', '审核中', '已完成'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1 },
    color: ['#909399', '#409EFF', '#E6A23C', '#67C23A'],
    series: [
      { name: '待办', type: 'bar', stack: 'total', data: list.map(i => i.todoTasks) },
      { name: '进行中', type: 'bar', stack: 'total', data: list.map(i => i.inProgressTasks) },
      { name: '审核中', type: 'bar', stack: 'total', data: list.map(i => i.reviewTasks) },
      { name: '已完成', type: 'bar', stack: 'total', data: list.map(i => i.doneTasks) }
    ]
  }, true);
};

const renderMemberHoursChart = (list) => {
  const chart = memberChartInstances['hours'];
  if (!chart) return;
  const names = list.map(item => item.nickname || item.username || '未知');
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { data: ['计划学时', '实际学时'], bottom: 0 },
    grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
    xAxis: { type: 'category', data: names, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [
      { name: '计划学时', type: 'bar', data: list.map(i => i.plannedHours), itemStyle: { color: '#409EFF' } },
      { name: '实际学时', type: 'bar', data: list.map(i => i.actualHours), itemStyle: { color: '#67C23A' } }
    ]
  }, true);
};

const viewMemberTasks = (row) => {
  const query = { assigneeId: row.userId };
  if (memberProjectId.value) {
    query.projectId = memberProjectId.value;
  }
  router.push({ path: '/teacher/training-tasks', query });
};

// ===== 公共 =====
const handleResize = () => {
  Object.values(projectChartInstances).forEach(instance => instance && instance.resize());
  Object.values(memberChartInstances).forEach(instance => instance && instance.resize());
};

watch(activeTab, (tab) => {
  if (tab === 'project') {
    nextTick(() => {
      initProjectCharts();
      renderProjectCharts();
    });
  } else if (tab === 'member') {
    if (rawList.value.length === 0) {
      loadMemberLoad();
    } else {
      nextTick(() => {
        initMemberCharts();
        renderMemberCharts(displayList.value);
      });
    }
  }
});

onMounted(() => {
  loadProjects();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  Object.values(projectChartInstances).forEach(instance => instance && instance.dispose());
  Object.values(memberChartInstances).forEach(instance => instance && instance.dispose());
});
</script>

<style scoped>
.data-analysis {
  --line: #e9ecf2;
  --line-strong: #dfe3ea;
  --ink-1: #1f2329;
  --ink-2: #4e5969;
  --ink-3: #8a919f;
  padding: 28px 32px 40px;
  background-color: #f5f7fa;
  min-height: calc(100vh - 60px);
}

/* ---------- 页头 ---------- */
.page-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.head-left h1 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: -0.2px;
  color: var(--ink-1);
}

.head-left p {
  margin: 0;
  font-size: 13px;
  color: var(--ink-3);
}

/* ---------- 标签页 ---------- */
.analysis-tabs {
  margin-top: 0;
}

.analysis-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.analysis-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--line);
}

.analysis-tabs :deep(.el-tabs__item) {
  height: 40px;
  padding: 0 2px;
  margin-right: 26px;
  font-size: 14px;
  font-weight: 400;
  color: var(--ink-2);
}

.analysis-tabs :deep(.el-tabs__item.is-active) {
  color: var(--brand-primary);
  font-weight: 500;
}

.analysis-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--brand-primary);
}

.tab-content {
  padding-top: 0;
}

/* ---------- 工具条 ---------- */
.tab-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.tab-toolbar :deep(.el-button) {
  height: 32px;
  border-radius: 8px;
  font-size: 13px;
}

/* ---------- 卡片统一 ---------- */
.tab-content :deep(.el-card) {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}

.tab-content :deep(.el-card__header) {
  padding: 15px 18px;
  border-bottom: 1px solid var(--line);
}

.tab-content :deep(.el-card__body) {
  padding: 16px 18px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-1);
}

.card-header .el-icon {
  font-size: 16px;
  color: var(--ink-3);
}

/* ---------- 图表 ---------- */
.chart {
  width: 100%;
  height: 280px;
}

/* ---------- 统计卡 ---------- */
.stats-strip {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 4px;
  margin-bottom: 20px;
}

.stat-item {
  padding: 18px 20px;
  border-right: 1px solid var(--line);
}

.stat-item:last-child {
  border-right: none;
}

.stat-label {
  font-size: 13px;
  color: var(--ink-3);
  margin-bottom: 10px;
}

.stat-number {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.5px;
  color: var(--ink-1);
  font-variant-numeric: tabular-nums;
}

.stat-number.is-alert {
  color: #cf1322;
}

.stat-unit {
  font-size: 14px;
  font-weight: 400;
  margin-left: 2px;
  color: var(--ink-3);
}

/* ---------- 表格 ---------- */
.chart-row {
  margin-bottom: 20px;
}

.table-card {
  min-height: 300px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.table-header > span {
  font-size: 14px;
  font-weight: 600;
  color: var(--ink-1);
}

.table-header :deep(.el-input) {
  width: 220px;
}

.table-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 14px;
}

.member-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.member-avatar {
  background: #eef2f9;
  color: #5a6472;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
}

.member-info {
  display: flex;
  flex-direction: column;
}

.member-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-1);
}

.member-username {
  font-size: 12px;
  color: var(--ink-3);
}

.text-danger {
  color: #f56c6c;
  font-weight: 500;
}

/* ---------- 响应式 ---------- */
@media (max-width: 1400px) {
  .stats-strip { grid-template-columns: repeat(3, minmax(0, 1fr)); }
  .stat-item:nth-child(3) { border-right: none; }
  .stat-item:nth-child(-n+3) { border-bottom: 1px solid var(--line); }
}

@media (max-width: 1200px) {
  .stats-strip { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .stat-item:nth-child(3) { border-right: 1px solid var(--line); }
  .stat-item:nth-child(2n) { border-right: none; }
  .stat-item:nth-child(-n+4) { border-bottom: 1px solid var(--line); }
  .stat-item:nth-child(n+5) { border-bottom: none; }
}

@media (max-width: 992px) {
  .chart-row :deep(.el-col) { width: 100%; margin-bottom: 20px; }
}

@media (max-width: 768px) {
  .data-analysis { padding: 20px 16px 32px; }
  .stats-strip { grid-template-columns: 1fr; }
  .stat-item { border-right: none; border-bottom: 1px solid var(--line); }
  .stat-item:last-child { border-bottom: none; }
}
</style>
