<template>
  <div v-loading="loading" class="ai-statistics">
    <div class="stats-row">
      <el-card class="stat-card" shadow="never">
        <div class="stat-main">
          <div class="stat-title">总调用次数</div>
          <div class="stat-value">{{ stats.totalCalls }}</div>
        </div>
        <div class="stat-icon"><el-icon><ChatDotRound /></el-icon></div>
      </el-card>

      <el-card class="stat-card" shadow="never">
        <div class="stat-main">
          <div class="stat-title">成功率</div>
          <div class="stat-value">{{ stats.successRate }}<span class="unit">%</span></div>
        </div>
        <div class="stat-icon ok"><el-icon><CircleCheck /></el-icon></div>
      </el-card>

      <el-card class="stat-card" shadow="never">
        <div class="stat-main">
          <div class="stat-title">异常率</div>
          <div class="stat-value">{{ stats.failureRate }}<span class="unit">%</span></div>
        </div>
        <div class="stat-icon danger"><el-icon><Warning /></el-icon></div>
      </el-card>

      <el-card class="stat-card" shadow="never">
        <div class="stat-main">
          <div class="stat-title">总Token消耗</div>
          <div class="stat-value">{{ stats.totalTokens }}</div>
        </div>
        <div class="stat-icon"><el-icon><Coin /></el-icon></div>
      </el-card>
    </div>

    <div class="charts-row">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="card-header">
            <el-icon><TrendCharts /></el-icon>
            <span>Token消耗趋势</span>
          </div>
        </template>
        <div ref="tokenChart" class="chart"></div>
      </el-card>

      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="card-header">
            <el-icon><PieChart /></el-icon>
            <span>按场景分布</span>
          </div>
        </template>
        <div ref="sceneChart" class="chart"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import * as echarts from 'echarts';
import { getAiStatistics } from '@/api/dashboard.js';
import { ChatDotRound, CircleCheck, Warning, Coin, TrendCharts, PieChart } from '@element-plus/icons-vue';

const loading = ref(false);
const stats = ref({
  totalCalls: 0,
  successRate: 0,
  failureRate: 0,
  totalTokens: 0,
  tokenTrend: [],
  sceneDistribution: []
});

const tokenChart = ref(null);
const sceneChart = ref(null);
let tokenInstance = null;
let sceneInstance = null;

const initCharts = () => {
  if (tokenInstance) {
    tokenInstance.dispose();
    tokenInstance = null;
  }
  if (sceneInstance) {
    sceneInstance.dispose();
    sceneInstance = null;
  }
  if (tokenChart.value) tokenInstance = echarts.init(tokenChart.value);
  if (sceneChart.value) sceneInstance = echarts.init(sceneChart.value);
};

const renderTokenChart = (trend) => {
  if (!tokenInstance) return;
  tokenInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: trend.map(item => item.date), axisLabel: { fontSize: 11, rotate: 30 } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
    series: [{
      data: trend.map(item => item.tokens),
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(64,158,255,0.15)' },
      itemStyle: { color: '#409EFF' },
      lineStyle: { width: 2 },
      symbol: 'circle',
      symbolSize: 6
    }]
  });
};

const renderSceneChart = (distribution) => {
  if (!sceneInstance) return;
  sceneInstance.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle', itemWidth: 8, itemHeight: 8, textStyle: { fontSize: 12 } },
    series: [{
      type: 'pie',
      radius: '60%',
      center: ['50%', '45%'],
      data: distribution.map(item => ({ name: item.scene || '未知', value: item.cnt })),
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      label: { fontSize: 12 },
      emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' } }
    }]
  });
};

const loadData = () => {
  loading.value = true;
  getAiStatistics().then(res => {
    stats.value = res.data;
    loading.value = false;
    setTimeout(() => {
      initCharts();
      renderTokenChart(stats.value.tokenTrend);
      renderSceneChart(stats.value.sceneDistribution);
    }, 0);
  }).catch(() => {
    loading.value = false;
  });
};

const handleResize = () => {
  tokenInstance && tokenInstance.resize();
  sceneInstance && sceneInstance.resize();
};

onMounted(() => {
  loadData();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  tokenInstance && tokenInstance.dispose();
  sceneInstance && sceneInstance.dispose();
});
</script>

<style scoped>
.ai-statistics {
  --line: #e9ecf2;
  --line-strong: #dfe3ea;
  --ink-1: #1f2329;
  --ink-2: #4e5969;
  --ink-3: #8a919f;
}

/* ---------- 指标卡 ---------- */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 26px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--line);
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(31, 35, 41, 0.04);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.stat-card:hover {
  border-color: var(--line-strong);
  box-shadow: 0 4px 14px rgba(31, 35, 41, 0.07);
}

.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
}

.stat-main {
  min-width: 0;
}

.stat-title {
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

.stat-value .unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--ink-3);
  margin-left: 2px;
  letter-spacing: 0;
}

.stat-icon {
  flex: none;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 9px;
  font-size: 19px;
  background: rgba(79, 124, 255, 0.08);
  color: var(--brand-primary);
}

.stat-icon.ok {
  background: rgba(52, 199, 89, 0.1);
  color: #2f9e44;
}

.stat-icon.danger {
  background: rgba(240, 68, 56, 0.1);
  color: #e03131;
}

/* ---------- 图表 ---------- */
.charts-row {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  gap: 16px;
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

.panel-card :deep(.el-card__body) {
  padding: 12px 14px 8px;
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

.chart {
  width: 100%;
  height: 300px;
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .charts-row { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
}
</style>