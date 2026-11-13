<template>
  <div v-loading="loading">
    <el-card class="stats-card">
      <template #header>
        <div class="card-header">
          <el-icon><DataLine /></el-icon>
          <span>操作统计</span>
        </div>
      </template>
      <el-row :gutter="20">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">总操作次数</div>
            <div class="stat-value">{{ stats.totalCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">今日操作</div>
            <div class="stat-value">{{ stats.todayCount }}</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">成功率</div>
            <div class="stat-value success">{{ stats.successRate }}%</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-label">失败次数</div>
            <div class="stat-value danger">{{ stats.failedCount }}</div>
          </div>
        </el-col>
      </el-row>

      <el-row :gutter="20" style="margin-top: 20px">
        <el-col :span="14">
          <el-card shadow="never">
            <div class="chart-title">最近 7 天操作趋势</div>
            <div ref="trendChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card shadow="never">
            <div class="chart-title">操作类型分布</div>
            <div ref="typeChartRef" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <el-card style="margin-top: 16px">
      <template #header>
        <span>操作日志</span>
      </template>

      <div class="filter-bar">
        <el-input v-model="filter.userId" placeholder="用户ID" style="width: 120px" clearable />
        <el-input v-model="filter.operationType" placeholder="操作类型" style="width: 160px" clearable />
        <el-date-picker v-model="filter.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" />
        <el-date-picker v-model="filter.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-button type="primary" size="small" @click="loadLogs">查询</el-button>
        <el-button size="small" @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="logs" border style="margin-top: 15px">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="operationType" label="操作类型" width="140" />
        <el-table-column prop="methodName" label="方法名" min-width="250" show-overflow-tooltip />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="showDetail(scope.row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          style="margin-top: 15px; justify-content: flex-end"
          @current-change="loadLogs"
      />
    </el-card>

    <el-dialog v-model="detailVisible" title="日志详情" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户">{{ currentRow.username }} (ID: {{ currentRow.userId }})</el-descriptions-item>
        <el-descriptions-item label="操作类型">{{ currentRow.operationType }}</el-descriptions-item>
        <el-descriptions-item label="方法名">{{ currentRow.methodName }}</el-descriptions-item>
        <el-descriptions-item label="IP">{{ currentRow.ip }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentRow.durationMs }} ms</el-descriptions-item>
        <el-descriptions-item label="参数">{{ currentRow.params }}</el-descriptions-item>
        <el-descriptions-item label="异常信息">{{ currentRow.errorMsg || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick, onBeforeUnmount } from 'vue';
import { listOperationLogs, getOperationLogStatistics } from '@/api/operationLog.js';
import { DataLine } from '@element-plus/icons-vue';
import * as echarts from 'echarts';

const loading = ref(false);
const logs = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filter = reactive({ userId: '', operationType: '', startDate: '', endDate: '' });

const detailVisible = ref(false);
const currentRow = reactive({});

const stats = ref({ totalCount: 0, todayCount: 0, successRate: 0, failedCount: 0 });
const trendChartRef = ref(null);
const typeChartRef = ref(null);
let trendChart = null;
let typeChart = null;

const loadStatistics = () => {
  getOperationLogStatistics().then(res => {
    const data = res.data || {};
    const totalC = data.totalCount || 0;
    const failedC = data.failedCount || 0;
    stats.value = {
      totalCount: totalC,
      todayCount: data.todayCount || 0,
      successRate: totalC > 0 ? (((totalC - failedC) / totalC) * 100).toFixed(1) : '0.0',
      failedCount: failedC
    };
    nextTick(() => {
      renderTrendChart(data.operationTrend || []);
      renderTypeChart(data.operationTypeDistribution || []);
    });
  }).catch(() => {});
};

const renderTrendChart = (data) => {
  if (!trendChartRef.value) return;
  if (trendChart) trendChart.dispose();
  trendChart = echarts.init(trendChartRef.value);
  trendChart.setOption({
    tooltip: { trigger: 'axis', formatter: '{b}: {c} 次' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.map(item => item.date),
      axisLabel: { fontSize: 11, rotate: 30 }
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
    series: [{
      data: data.map(item => item.cnt),
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#667EEA' },
          { offset: 1, color: '#764BA2' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      barWidth: '40%'
    }]
  });
};

const renderTypeChart = (data) => {
  if (!typeChartRef.value) return;
  if (typeChart) typeChart.dispose();
  typeChart = echarts.init(typeChartRef.value);
  const colors = ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9333EA', '#0EA5E9'];
  const chartData = data.map((item, index) => ({
    name: item.operationType,
    value: item.cnt,
    itemStyle: { color: colors[index % colors.length] }
  }));
  typeChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: {
      type: 'scroll',
      orient: 'vertical',
      right: '5%',
      top: 'middle',
      textStyle: { fontSize: 12 },
      itemHeight: 12,
      itemWidth: 18,
      itemGap: 10,
      pageIconSize: 12,
      pageTextStyle: { fontSize: 11 }
    },
    series: [{
      type: 'pie',
      radius: ['45%', '70%'],
      center: ['30%', '50%'],
      avoidLabelOverlap: true,
      label: { show: false },
      emphasis: {
        label: { show: true, fontSize: 12, fontWeight: 'bold' }
      },
      labelLine: { show: false },
      data: chartData
    }]
  });
};

const handleResize = () => {
  trendChart && trendChart.resize();
  typeChart && typeChart.resize();
};

const loadLogs = () => {
  loading.value = true;
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    userId: filter.userId ? Number(filter.userId) : null,
    operationType: filter.operationType,
    startDate: filter.startDate,
    endDate: filter.endDate
  };
  listOperationLogs(params).then(res => {
    logs.value = res.data.records || [];
    total.value = res.data.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const resetFilter = () => {
  filter.userId = '';
  filter.operationType = '';
  filter.startDate = '';
  filter.endDate = '';
  pageNum.value = 1;
  loadLogs();
};

const showDetail = (row) => {
  Object.assign(currentRow, row);
  detailVisible.value = true;
};

onMounted(() => {
  loadStatistics();
  loadLogs();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  trendChart && trendChart.dispose();
  typeChart && typeChart.dispose();
});
</script>

<style scoped>
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.stat-item {
  text-align: center;
  padding: 10px 0;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}
.stat-value.success { color: #67C23A; }
.stat-value.danger { color: #F56C6C; }
.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.chart-container {
  width: 100%;
  height: 350px;
}
.stats-card :deep(.el-card__body) {
  padding: 20px;
}
</style>
