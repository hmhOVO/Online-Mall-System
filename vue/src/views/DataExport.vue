<template>
  <div class="data-export">
    <el-card>
      <template #header>
        <div class="card-header">
          <el-icon><Download /></el-icon>
          <span>数据导出</span>
        </div>
      </template>

      <el-row :gutter="20">
        <el-col :span="8" v-if="canExportUsers">
          <el-card shadow="hover" class="export-card">
            <div class="export-icon">
              <el-icon><UserFilled /></el-icon>
            </div>
            <div class="export-title">用户数据</div>
            <div class="export-desc">导出系统用户的基本信息，支持筛选</div>
            <div class="export-filters">
              <el-select v-model="userFilter.role" clearable placeholder="全部角色" style="width: 100%; margin-bottom: 10px">
                <el-option label="系统管理员" value="ADMIN" />
                <el-option label="指导教师" value="TEACHER" />
                <el-option label="学生" value="STUDENT" />
              </el-select>
              <el-select v-model="userFilter.status" clearable placeholder="全部状态" style="width: 100%; margin-bottom: 10px">
                <el-option label="启用" value="ENABLED" />
                <el-option label="禁用" value="DISABLED" />
              </el-select>
              <el-input v-model="userFilter.keyword" clearable placeholder="搜索用户名/姓名" style="width: 100%" />
            </div>
            <el-button type="primary" :loading="loading.user" @click="handleExport('users')">
              导出用户
            </el-button>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="export-card">
            <div class="export-icon project-icon">
              <el-icon><FolderOpened /></el-icon>
            </div>
            <div class="export-title">项目数据</div>
            <div class="export-desc">导出项目及其指导教师信息，支持筛选</div>
            <div class="export-filters">
              <el-select v-model="projectFilter.status" clearable placeholder="全部状态" style="width: 100%; margin-bottom: 10px">
                <el-option label="未开始" value="NOT_STARTED" />
                <el-option label="进行中" value="IN_PROGRESS" />
                <el-option label="已延期" value="DELAYED" />
                <el-option label="已完成" value="COMPLETED" />
              </el-select>
              <el-input v-model="projectFilter.keyword" clearable placeholder="搜索项目名称" style="width: 100%" />
            </div>
            <el-button type="success" :loading="loading.project" @click="handleExport('projects')">
              导出项目
            </el-button>
          </el-card>
        </el-col>

        <el-col :span="8">
          <el-card shadow="hover" class="export-card">
            <div class="export-icon task-icon">
              <el-icon><List /></el-icon>
            </div>
            <div class="export-title">任务数据</div>
            <div class="export-desc">导出任务明细，支持多条件筛选</div>
            <div class="export-filters">
              <el-select v-model="taskFilter.projectId" clearable placeholder="全部项目" style="width: 100%; margin-bottom: 10px">
                <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
              <el-select v-model="taskFilter.status" clearable placeholder="全部状态" style="width: 100%; margin-bottom: 10px">
                <el-option label="待处理" value="TODO" />
                <el-option label="进行中" value="IN_PROGRESS" />
                <el-option label="审核中" value="REVIEW" />
                <el-option label="已完成" value="DONE" />
                <el-option label="已阻塞" value="BLOCKED" />
              </el-select>
              <el-select v-model="taskFilter.priority" clearable placeholder="全部优先级" style="width: 100%; margin-bottom: 10px">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </div>
            <el-button type="warning" :loading="loading.task" @click="handleExport('tasks')">
              导出任务
            </el-button>
          </el-card>
        </el-col>

        <el-col :span="8" v-if="canExportScores">
          <el-card shadow="hover" class="export-card">
            <div class="export-icon score-icon">
              <el-icon><Medal /></el-icon>
            </div>
            <div class="export-title">实训成绩</div>
            <div class="export-desc">导出学生任务完成与得分表，支持按项目筛选</div>
            <div class="export-filters">
              <el-select v-model="scoreFilter.projectId" clearable placeholder="全部项目" style="width: 100%; margin-bottom: 10px">
                <el-option v-for="p in projects" :key="p.id" :label="p.name" :value="p.id" />
              </el-select>
              <el-input v-model="scoreFilter.keyword" clearable placeholder="搜索项目/学生" style="width: 100%" />
            </div>
            <el-button type="danger" :loading="loading.scores" @click="handleExport('scores')">
              导出成绩
            </el-button>
          </el-card>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, computed, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { exportUsers, exportProjects, exportTasks, exportScores } from '@/api/export.js';
import { listProjects } from '@/api/project.js';
import { Download, UserFilled, FolderOpened, List, Medal } from '@element-plus/icons-vue';

const userStr = localStorage.getItem('code_user');
const currentUser = userStr ? JSON.parse(userStr) : {};
const canExportUsers = computed(() => currentUser.role === 'ADMIN');
const canExportScores = computed(() => currentUser.role === 'ADMIN' || currentUser.role === 'TEACHER');

const loading = reactive({ user: false, project: false, task: false, scores: false });

const userFilter = reactive({ role: '', status: '', keyword: '' });
const projectFilter = reactive({ status: '', keyword: '' });
const taskFilter = reactive({ projectId: null, status: '', priority: '' });
const scoreFilter = reactive({ projectId: null, keyword: '' });

const projects = ref([]);

onMounted(() => {
  listProjects({ pageNum: 1, pageSize: 1000 }).then(res => {
    projects.value = (res.data && res.data.records) || [];
  }).catch(() => {});
});

const handleExport = (type) => {
  loading[type] = true;
  let request;
  let filename;
  if (type === 'users') {
    const params = {};
    if (userFilter.role) params.role = userFilter.role;
    if (userFilter.status) params.status = userFilter.status;
    if (userFilter.keyword) params.keyword = userFilter.keyword;
    request = exportUsers(params);
    filename = '用户列表.xlsx';
  } else if (type === 'projects') {
    const params = {};
    if (projectFilter.status) params.status = projectFilter.status;
    if (projectFilter.keyword) params.keyword = projectFilter.keyword;
    request = exportProjects(params);
    filename = '项目列表.xlsx';
  } else if (type === 'scores') {
    const params = {};
    if (scoreFilter.projectId) params.projectId = scoreFilter.projectId;
    if (scoreFilter.keyword) params.keyword = scoreFilter.keyword;
    request = exportScores(params);
    filename = '学生任务完成与得分表.xlsx';
  } else {
    const params = {};
    if (taskFilter.projectId) params.projectId = taskFilter.projectId;
    if (taskFilter.status) params.status = taskFilter.status;
    if (taskFilter.priority) params.priority = taskFilter.priority;
    request = exportTasks(params);
    filename = '任务列表.xlsx';
  }

  request.then(res => {
    downloadFile(res, filename);
    ElMessage.success('导出成功');
    loading[type] = false;
  }).catch(() => {
    ElMessage.error('导出失败');
    loading[type] = false;
  });
};

const downloadFile = (blob, filename) => {
  const url = window.URL.createObjectURL(new Blob([blob]));
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
};
</script>

<style scoped>
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
}
.export-card {
  text-align: center;
  padding: 10px 0;
}
.export-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 15px;
}
.project-icon {
  color: #67C23A;
}
.task-icon {
  color: #E6A23C;
}
.score-icon {
  color: #F56C6C;
}
.export-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}
.export-desc {
  font-size: 13px;
  color: #909399;
  margin-bottom: 15px;
  min-height: 36px;
}
.export-filters {
  margin-bottom: 15px;
  padding: 0 10px;
}
</style>
