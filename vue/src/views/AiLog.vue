<template>
  <div v-loading="loading">
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><Cpu /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayCount || 0 }}</div>
          <div class="stat-label">今日调用</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.successRate || 0 }}%</div>
          <div class="stat-label">成功率</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatTokens(stats.todayTokens) }}</div>
          <div class="stat-label">今日Token</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon><Timer /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ formatLatency(stats.todayAvgLatency) }}</div>
          <div class="stat-label">平均耗时</div>
        </div>
      </div>
    </div>

    <el-card style="margin-top: 15px">
      <template #header>
        <span>调用记录</span>
      </template>
      <div class="filter-bar">
        <el-input v-model="filter.userId" placeholder="用户ID" style="width: 120px" clearable />
        <el-input v-model="filter.projectId" placeholder="项目ID" style="width: 120px" clearable />
        <el-select v-model="filter.scene" clearable placeholder="场景" style="width: 140px">
          <el-option label="任务拆解" value="TASK_SPLIT" />
          <el-option label="风险分析" value="RISK_ANALYSIS" />
        </el-select>
        <el-select v-model="filter.status" clearable placeholder="状态" style="width: 120px">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
        </el-select>
        <el-button type="primary" size="small" @click="loadLogs">查询</el-button>
        <el-button size="small" @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="logs" border style="margin-top: 15px">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column prop="projectId" label="项目ID" width="90" />
        <el-table-column prop="scene" label="场景" width="120" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'SUCCESS' ? 'success' : 'danger'" size="small">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalTokens" label="Tokens" width="90" />
        <el-table-column prop="latencyMs" label="耗时(ms)" width="100" />
        <el-table-column prop="createdAt" label="调用时间" width="170" />
        <el-table-column label="操作" width="120" fixed="right">
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

    <el-dialog v-model="detailVisible" title="调用详情" width="700px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="请求">{{ currentRow.request }}</el-descriptions-item>
        <el-descriptions-item label="响应">{{ currentRow.response }}</el-descriptions-item>
        <el-descriptions-item label="错误信息">{{ currentRow.errorMsg || '无' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { Cpu, CircleCheck, Coin, Timer } from '@element-plus/icons-vue';
import { listAiLogs, getAiLogStats } from '@/api/ai.js';

const loading = ref(false);
const logs = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filter = reactive({ userId: '', projectId: '', scene: '', status: '' });
const stats = reactive({
  todayCount: 0,
  successRate: 0,
  todayTokens: 0,
  todayAvgLatency: 0
});

const detailVisible = ref(false);
const currentRow = reactive({});

const loadStats = () => {
  getAiLogStats().then(res => {
    if (res.data) {
      Object.assign(stats, res.data);
    }
  }).catch(() => {});
};

const formatTokens = (val) => {
  if (!val) return '0';
  const num = Number(val);
  if (num >= 10000) return (num / 10000).toFixed(1) + 'w';
  if (num >= 1000) return (num / 1000).toFixed(1) + 'k';
  return num.toString();
};

const formatLatency = (val) => {
  if (!val) return '0ms';
  const num = Number(val);
  if (num >= 1000) return (num / 1000).toFixed(1) + 's';
  return Math.round(num) + 'ms';
};

const loadLogs = () => {
  loading.value = true;
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    userId: filter.userId ? Number(filter.userId) : null,
    projectId: filter.projectId ? Number(filter.projectId) : null,
    scene: filter.scene,
    status: filter.status
  };
  listAiLogs(params).then(res => {
    logs.value = res.data.records || [];
    total.value = res.data.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const resetFilter = () => {
  filter.userId = '';
  filter.projectId = '';
  filter.scene = '';
  filter.status = '';
  pageNum.value = 1;
  loadLogs();
};

const showDetail = (row) => {
  Object.assign(currentRow, row);
  detailVisible.value = true;
};

onMounted(() => {
  loadStats();
  loadLogs();
});
</script>

<style scoped>
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-icon.blue { background: linear-gradient(135deg, #409eff, #66b1ff); }
.stat-icon.green { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.orange { background: linear-gradient(135deg, #e6a23c, #ebb563); }
.stat-icon.purple { background: linear-gradient(135deg, #909399, #a6a9ad); }

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 6px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
