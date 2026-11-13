<template>
  <div v-loading="loading" class="score-container">
    <el-card class="score-card">
      <div class="score-header">
        <div class="header-left">
          <el-button @click="goBack">返回项目</el-button>
          <span class="project-title">{{ project.name }}</span>
        </div>
        <div class="header-right">
          <el-button type="success" size="small" :loading="exporting" @click="handleExport">导出成绩</el-button>
          <el-button type="primary" size="small" @click="loadScores">刷新</el-button>
        </div>
      </div>

      <el-empty
        v-if="!loading && !hasAnyData"
        description="暂无成绩数据，已完成任务的审核评分将在这里汇总"
      />

      <div v-else class="score-body">
        <div class="stat-row">
          <div class="stat-item">
            <div class="stat-value">{{ rows.length }}</div>
            <div class="stat-label">已评分成员数</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ totalScoredTasks }}</div>
            <div class="stat-label">已评分任务数</div>
          </div>
        </div>

        <el-card v-if="groupRows.length > 0" shadow="never" class="group-card">
          <template #header><span>小组统计</span></template>
          <el-table :data="groupRows" size="small" border>
            <el-table-column label="小组" min-width="140">
              <template #default="scope">{{ scope.row.groupName || scope.row.groupNo || ('分组' + scope.row.groupId) }}</template>
            </el-table-column>
            <el-table-column prop="memberCount" label="人数" width="70" align="center" />
            <el-table-column prop="taskCount" label="任务数" width="80" align="center" />
            <el-table-column label="完成情况" width="120" align="center">
              <template #default="scope">
                {{ scope.row.doneCount }} / {{ scope.row.taskCount }}
                <span v-if="scope.row.taskCount > 0" class="group-progress">（{{ Math.round(scope.row.doneCount / scope.row.taskCount * 100) }}%）</span>
              </template>
            </el-table-column>
            <el-table-column label="平均分" width="90" align="center">
              <template #default="scope">{{ scope.row.averageScore != null ? scope.row.averageScore : '—' }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-table :data="rankedRows" border>
          <el-table-column type="index" label="排名" width="70" align="center" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="姓名" />
          <el-table-column prop="scoredTaskCount" label="已评分任务数" width="120" align="center" />
          <el-table-column label="平均分" width="120" align="center">
            <template #default="scope">
              <span class="score-value" :class="scoreClass(scope.row.averageScore)">
                {{ scope.row.averageScore }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import request from '@/utils/request.js';
import { getProjectScores, getProjectGroupScores, exportProjectScores } from '@/api/project.js';

const route = useRoute();
const router = useRouter();
const projectId = route.params.id;

if (!/^\d+$/.test(projectId)) {
  router.replace('/notFound');
}

const loading = ref(false);
const project = ref({ name: '' });
const rows = ref([]);
const groupRows = ref([]);
const hasAnyData = computed(() => rows.value.length > 0 || groupRows.value.length > 0);

const rankedRows = computed(() => {
  const sorted = [...rows.value].sort(
    (a, b) => Number(b.averageScore) - Number(a.averageScore)
  );
  return sorted.map((r, i) => ({ ...r, rank: i + 1 }));
});

const totalScoredTasks = computed(() =>
  rows.value.reduce((sum, r) => sum + Number(r.scoredTaskCount || 0), 0)
);

const scoreClass = (score) => {
  const v = Number(score);
  if (v >= 90) return 'excellent';
  if (v >= 60) return 'pass';
  return 'fail';
};

const loadProject = () => {
  request.get('/project/' + projectId).then((res) => {
    project.value = res.data || {};
  }).catch(() => {});
};

const loadScores = () => {
  loading.value = true;
  getProjectScores(projectId).then((res) => {
    rows.value = res.data || [];
    loading.value = false;
  }).catch(() => {
    loading.value = false;
    ElMessage.error('加载成绩失败');
  });
  getProjectGroupScores(projectId).then((res) => {
    groupRows.value = res.data || [];
  }).catch(() => {});
};

const goBack = () => {
  router.push('/project/' + projectId);
};

const exporting = ref(false);
const handleExport = () => {
  exporting.value = true;
  exportProjectScores(projectId).then((res) => {
    const url = window.URL.createObjectURL(new Blob([res]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', '学生任务完成与得分表.xlsx');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  }).catch(() => {
    ElMessage.error('导出失败');
  }).finally(() => {
    exporting.value = false;
  });
};

onMounted(() => {
  loadProject();
  loadScores();
});
</script>

<style scoped>
.score-container {
  padding: 20px;
  min-height: 100vh;
  background: #f5f7fa;
}
.score-card {
  max-width: 900px;
  margin: 0 auto;
}
.score-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.project-title {
  font-size: 16px;
  font-weight: 600;
}
.stat-row {
  display: flex;
  gap: 20px;
  margin-bottom: 16px;
}
.stat-item {
  flex: 1;
  text-align: center;
  padding: 16px;
  background: #f7f8fa;
  border-radius: 8px;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #409eff;
}
.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}
.group-card {
  margin-bottom: 16px;
}
.group-progress {
  font-size: 12px;
  color: #909399;
}
.score-value {
  font-weight: 700;
}
.score-value.excellent {
  color: #67c23a;
}
.score-value.pass {
  color: #409eff;
}
.score-value.fail {
  color: #f56c6c;
}
</style>