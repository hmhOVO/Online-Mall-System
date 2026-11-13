<template>
  <div class="admin-dashboard">
    <header class="page-head">
      <div class="head-left">
        <h1>系统概览</h1>
        <p>实时掌握用户、操作与 AI 使用情况</p>
      </div>
      <div class="head-right">
        <span v-if="lastUpdated" class="updated">更新于 {{ lastUpdated }}</span>
        <el-button type="primary" @click="loadData">刷新数据</el-button>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="dashboard-tabs">
      <el-tab-pane label="系统总览" name="overview">
        <!-- 快捷操作 -->
        <div class="section-title">快捷操作</div>
        <div class="quick-actions-row">
          <div class="action-card" @click="goUserManagement">
            <el-icon class="action-icon"><UserFilled /></el-icon>
            <span class="action-name">用户管理</span>
          </div>
          <div class="action-card" @click="goTrainingOverview">
            <el-icon class="action-icon"><Grid /></el-icon>
            <span class="action-name">实训总览</span>
          </div>
          <div class="action-card" @click="goDataAnalysis">
            <el-icon class="action-icon"><PieChart /></el-icon>
            <span class="action-name">数据分析</span>
          </div>
          <div class="action-card" @click="goSystemConfig">
            <el-icon class="action-icon"><Tools /></el-icon>
            <span class="action-name">系统配置</span>
          </div>
        </div>

        <!-- 核心数据 -->
        <div class="section-title">核心数据</div>
        <div class="stats-strip">
          <div class="stat-item">
            <div class="stat-label">总用户数</div>
            <div class="stat-value">{{ stats.userCount }}</div>
            <div class="stat-sub">
              <span><i class="dot ok"></i>启用 <em>{{ stats.enabledCount }}</em></span>
              <span><i class="dot off"></i>禁用 <em>{{ stats.disabledCount }}</em></span>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-label">在线用户</div>
            <div class="stat-value">{{ systemStats.onlineUserCount }}</div>
            <div class="stat-sub">
              <span>今日登录 <em>{{ systemStats.todayLoginCount }}</em> 次</span>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-label">今日操作</div>
            <div class="stat-value">{{ systemStats.todayOperationLogs }}</div>
            <div class="stat-sub">
              <span>累计 <em>{{ formatNumber(systemStats.totalOperationLogs) }}</em></span>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-label">今日 AI 调用</div>
            <div class="stat-value">{{ systemStats.todayAiCalls }}</div>
            <div class="stat-sub">
              <span>累计 <em>{{ formatNumber(systemStats.totalAiCalls) }}</em></span>
            </div>
          </div>
        </div>

        <!-- 图表区域 -->
        <div class="main-content">
          <div class="left-panel">
            <el-card class="panel-card trend-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <el-icon><TrendCharts /></el-icon>
                  <span>近7天登录趋势</span>
                  <span class="badge">共 {{ loginTrendTotal }} 次</span>
                </div>
              </template>
              <div ref="loginTrendChart" class="chart"></div>
            </el-card>
          </div>

          <div class="right-panel">
            <el-card class="panel-card recent-login-card" shadow="never">
              <template #header>
                <div class="card-header">
                  <el-icon><Clock /></el-icon>
                  <span>最近登录记录</span>
                  <el-button size="small" type="primary" link @click="goLoginLog">查看全部</el-button>
                </div>
              </template>
              <div v-if="recentLoginRecords.length === 0" class="empty-tip">
                <el-icon class="empty-icon"><Document /></el-icon>
                <div>暂无登录记录</div>
              </div>
              <div v-else class="record-list">
                <div
                    v-for="record in recentLoginRecords.slice(0, 5)"
                    :key="record.id"
                    class="login-record-item"
                >
                  <el-avatar :size="32" class="login-avatar">
                    {{ (record.nickname || record.username || 'U').charAt(0).toUpperCase() }}
                  </el-avatar>
                  <div class="login-record-info">
                    <div class="login-record-name">
                      <span class="login-name-text">{{ record.nickname || record.username }}</span>
                      <el-tag :type="record.status === 'SUCCESS' ? 'success' : 'danger'" size="small" class="login-status-tag">
                        {{ record.status === 'SUCCESS' ? '成功' : '失败' }}
                      </el-tag>
                    </div>
                    <div class="login-record-detail">
                      <span class="login-ip">IP: {{ record.ipAddress || '未知' }}</span>
                      <span class="login-time">{{ formatLoginTime(record.loginTime) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="AI统计" name="ai" lazy>
        <AiStatistics />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import {ref, computed, onMounted, onUnmounted, watch, nextTick} from 'vue';
import {useRouter} from 'vue-router';
import * as echarts from 'echarts';
import {getAdminDashboard} from '@/api/dashboard.js';
import AiStatistics from './AiStatistics.vue';
import {
  UserFilled, TrendCharts, Clock, Document, Tools, Grid, PieChart
} from '@element-plus/icons-vue';

const router = useRouter();
const activeTab = ref('overview');
const stats = ref({
  userCount: 0,
  enabledCount: 0,
  disabledCount: 0,
});
const systemStats = ref({
  todayLoginCount: 0,
  onlineUserCount: 0,
  totalOperationLogs: 0,
  todayOperationLogs: 0,
  totalAiCalls: 0,
  todayAiCalls: 0
});
const loginTrend = ref([]);
const recentLoginRecords = ref([]);
const lastUpdated = ref('');

const loginTrendChart = ref(null);
let chartInstances = {};

const loginTrendTotal = computed(() =>
  loginTrend.value.reduce((sum, item) => sum + (Number(item.count) || 0), 0)
);

const pad2 = (num) => String(num).padStart(2, '0');

const formatNumber = (num) => {
  if (num == null) return 0;
  if (num >= 10000) {
    return (num / 10000).toFixed(1) + 'w';
  }
  return num;
};

const formatLoginTime = (time) => {
  if (!time) return '';
  const d = new Date(time);
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  return `${month}-${day} ${hours}:${minutes}`;
};

const formatNow = () => {
  const d = new Date();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}`;
};

const goUserManagement = () => {
  router.push('/admin/user');
};

const goSystemConfig = () => {
  router.push('/admin/system-config');
};

const goTrainingOverview = () => {
  router.push('/admin/training-overview');
};

const goDataAnalysis = () => {
  router.push('/admin/data-analysis');
};

const goLoginLog = () => {
  router.push('/admin/login-log');
};

const renderLoginTrendChart = () => {
  const chart = chartInstances['login'];
  if (!chart || !loginTrend.value.length) return;
  chart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c} 次登录' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: loginTrend.value.map(item => item.date), axisLabel: { fontSize: 11, rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
    series: [{
      data: loginTrend.value.map(item => item.count),
      type: 'line',
      smooth: true,
      itemStyle: { color: '#409EFF' },
      areaStyle: { color: 'rgba(64,158,255,0.15)' },
      lineStyle: { width: 2 },
      symbol: 'circle',
      symbolSize: 6
    }]
  });
};

const loadData = () => {
  getAdminDashboard().then(res => {
    const data = res.data;
    const statusDist = data.userStatusDistribution || [];
    const enabledItem = statusDist.find(item => item.status === 'ENABLED');
    const disabledItem = statusDist.find(item => item.status === 'DISABLED');
    stats.value = {
      userCount: data.userCount || 0,
      enabledCount: enabledItem ? enabledItem.cnt : 0,
      disabledCount: disabledItem ? disabledItem.cnt : 0,
    };
    systemStats.value = {
      todayLoginCount: data.todayLoginCount || 0,
      onlineUserCount: data.onlineUserCount || 0,
      totalOperationLogs: data.totalOperationLogs || 0,
      todayOperationLogs: data.todayOperationLogs || 0,
      totalAiCalls: data.totalAiCalls || 0,
      todayAiCalls: data.todayAiCalls || 0
    };
    loginTrend.value = data.loginTrend || [];
    recentLoginRecords.value = data.recentLoginRecords || [];
    lastUpdated.value = formatNow();

    setTimeout(() => {
      if (loginTrendChart.value) {
        if (chartInstances['login']) {
          chartInstances['login'].dispose();
        }
        chartInstances['login'] = echarts.init(loginTrendChart.value);
        renderLoginTrendChart();
      }
    }, 0);
  }).catch(() => {});
};

watch(activeTab, (newTab) => {
  if (newTab === 'overview') {
    nextTick(() => {
      setTimeout(() => {
        if (chartInstances['login']) {
          chartInstances['login'].resize();
        }
      }, 50);
    });
  }
});

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
.admin-dashboard {
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

.head-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.updated {
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.head-right :deep(.el-button) {
  height: 32px;
  border-radius: 8px;
  font-size: 13px;
}

/* ---------- 区块标题 ---------- */
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--ink-1);
  margin: 0 0 12px;
}

/* ---------- 快捷操作 ---------- */
.quick-actions-row {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 26px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 20px 12px;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.action-card:hover {
  transform: translateY(-2px);
  border-color: rgba(79, 124, 255, 0.35);
  box-shadow: 0 4px 14px rgba(31, 35, 41, 0.07);
}

.action-icon {
  font-size: 22px;
  color: var(--ink-3);
  transition: color 0.18s ease;
}

.action-card:hover .action-icon {
  color: var(--brand-primary);
}

.action-name {
  font-size: 13px;
  color: var(--ink-2);
}

.action-card:hover .action-name {
  color: var(--ink-1);
}

/* ---------- 核心数据 ---------- */
.stats-strip {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 4px;
  margin-bottom: 26px;
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

.stat-value {
  font-size: 26px;
  font-weight: 600;
  line-height: 1.1;
  letter-spacing: -0.5px;
  color: var(--ink-1);
  font-variant-numeric: tabular-nums;
}

.stat-sub {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-top: 9px;
  font-size: 12px;
  color: var(--ink-3);
}

.stat-sub em {
  font-style: normal;
  color: var(--ink-2);
  font-variant-numeric: tabular-nums;
}

.dot {
  display: inline-block;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  margin-right: 5px;
  vertical-align: middle;
}

.dot.ok { background: #34c759; }
.dot.off { background: #f56c6c; }

/* ---------- 图表 / 登录记录 ---------- */
.main-content {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  gap: 16px;
}

.left-panel,
.right-panel {
  display: flex;
  flex-direction: column;
}

.panel-card {
  height: 100%;
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
}

.panel-card :deep(.el-card__header) {
  padding: 15px 18px;
  border-bottom: 1px solid var(--line);
}

.trend-card :deep(.el-card__body) {
  padding: 12px 14px 8px;
}

.recent-login-card :deep(.el-card__body) {
  padding: 6px 0;
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

.card-header .el-button {
  margin-left: auto;
  font-size: 12px;
}

.badge {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  height: 22px;
  padding: 0 8px;
  border-radius: 6px;
  background: #f2f4f7;
  color: var(--ink-2);
  font-size: 12px;
  font-weight: 400;
  font-variant-numeric: tabular-nums;
}

.chart {
  width: 100%;
  height: 260px;
}

.empty-tip {
  text-align: center;
  padding: 40px 0;
  font-size: 13px;
  color: var(--ink-3);
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 10px;
  color: #c8cdd6;
}

.record-list {
  display: block;
}

.login-record-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 18px;
  transition: background-color 0.15s ease;
}

.login-record-item + .login-record-item {
  border-top: 1px solid #f2f4f7;
}

.login-record-item:hover {
  background-color: #f8fafc;
}

.login-avatar {
  flex-shrink: 0;
  border-radius: 8px;
  background: #eef2f9;
  color: #5a6472;
  font-size: 13px;
  font-weight: 600;
}

.login-record-info {
  flex: 1;
  min-width: 0;
}

.login-record-name {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--ink-1);
}

.login-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.login-record-item .login-status-tag {
  flex: none;
  margin-left: auto;
  height: 18px;
  line-height: 18px;
  padding: 0 7px;
  border: none;
  border-radius: 4px;
  font-size: 11px;
}

.login-record-detail {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  margin-top: 3px;
  font-size: 12px;
  color: var(--ink-3);
  font-variant-numeric: tabular-nums;
}

.login-ip {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
}

/* ---------- 标签页 ---------- */
.dashboard-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
}

.dashboard-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: var(--line);
}

.dashboard-tabs :deep(.el-tabs__item) {
  height: 40px;
  padding: 0 2px;
  margin-right: 26px;
  font-size: 14px;
  font-weight: 400;
  color: var(--ink-2);
}

.dashboard-tabs :deep(.el-tabs__item.is-active) {
  color: var(--brand-primary);
  font-weight: 500;
}

.dashboard-tabs :deep(.el-tabs__active-bar) {
  background-color: var(--brand-primary);
}

.dashboard-tabs :deep(.el-tab-pane) {
  padding: 0;
}

@media (max-width: 1400px) {
  .quick-actions-row { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 1200px) {
  .stats-strip { grid-template-columns: repeat(2, 1fr); }
  .stat-item:nth-child(2n) { border-right: none; }
  .stat-item:nth-child(-n+2) { border-bottom: 1px solid var(--line); }
  .main-content { grid-template-columns: 1fr; }
  .quick-actions-row { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .admin-dashboard { padding: 20px 16px 32px; }
  .stats-strip { grid-template-columns: 1fr; }
  .stat-item { border-right: none; border-bottom: 1px solid var(--line); }
  .stat-item:last-child { border-bottom: none; }
  .quick-actions-row { grid-template-columns: repeat(2, 1fr); }
  .page-head { flex-direction: column; align-items: flex-start; }
}
</style>