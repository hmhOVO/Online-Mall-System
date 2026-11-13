<template>
  <div v-loading="loading" class="scores-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的成绩</h2>
        <p class="page-subtitle">查看已完成任务的评分记录与得分情况</p>
      </div>
      <el-button size="small" @click="loadData">
        <el-icon class="btn-icon"><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div class="summary-row">
      <div class="summary-card">
        <div class="summary-label">平均分</div>
        <div class="summary-value">{{ scoreSummary.avgScore }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">已评分任务</div>
        <div class="summary-value">{{ scoreSummary.scoredCount }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">累计得分</div>
        <div class="summary-value">{{ scoreSummary.totalScore }}</div>
      </div>
      <div class="summary-card">
        <div class="summary-label">最高分</div>
        <div class="summary-value">{{ scoreSummary.maxScore }}</div>
      </div>
    </div>

    <el-card class="panel" shadow="never">
      <template #header>
        <div class="card-header">
          <span class="card-title"><el-icon><Medal /></el-icon>评分记录</span>
          <span class="card-extra">最近 {{ scoredTasks.length }} 条</span>
        </div>
      </template>

      <el-table v-if="scoredTasks.length > 0" :data="scoredTasks" style="width: 100%">
        <el-table-column label="任务标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="task-title">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="所属实训项目" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ row.projectName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="任务状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="100" align="right">
          <template #default="{ row }">
            <span class="score-value">{{ row.score }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90" align="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="goTask(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-else-if="!loading" description="暂无评分记录，任务评分后会显示在这里" />
    </el-card>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {ElMessage} from 'element-plus';
import {Medal, Refresh} from '@element-plus/icons-vue';
import {getPersonalDashboard} from '@/api/dashboard.js';

const router = useRouter();
const loading = ref(false);
const scoreSummary = ref({scoredCount: 0, avgScore: 0, totalScore: 0, maxScore: 0});
const scoredTasks = ref([]);

const statusLabel = (status) => {
  const map = {TODO: '待办', IN_PROGRESS: '进行中', REVIEW: '审核中', DONE: '已完成', BLOCKED: '已阻塞'};
  return map[status] || status;
};

const statusType = (status) => {
  const map = {TODO: 'info', IN_PROGRESS: 'primary', REVIEW: 'warning', DONE: 'success', BLOCKED: 'danger'};
  return map[status] || 'info';
};

const loadData = () => {
  loading.value = true;
  getPersonalDashboard().then(res => {
    const data = res.data || {};
    scoreSummary.value = data.scoreSummary || {scoredCount: 0, avgScore: 0, totalScore: 0, maxScore: 0};
    scoredTasks.value = data.myScoredTasks || [];
  }).catch(() => {
    ElMessage.error('加载成绩失败');
  }).finally(() => {
    loading.value = false;
  });
};

const goTask = (row) => {
  router.push(`/project/${row.projectId}/board?taskId=${row.id}`);
};

onMounted(loadData);
</script>

<style scoped>
.scores-page {
  padding: 28px 32px;
  background: #f5f7fa;
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

.summary-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-card {
  padding: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.summary-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}

.summary-value {
  font-size: 24px;
  font-weight: 600;
  color: #1f2d3d;
  font-variant-numeric: tabular-nums;
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
  padding: 8px 18px 18px;
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

.card-extra {
  font-size: 12px;
  color: #909399;
}

.task-title {
  color: #303133;
}

.score-value {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  font-variant-numeric: tabular-nums;
}

@media (max-width: 900px) {
  .scores-page {
    padding: 20px 16px;
  }

  .summary-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
