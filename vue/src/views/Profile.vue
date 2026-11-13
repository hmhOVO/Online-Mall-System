<template>
  <div class="profile-page">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人中心</span>
        </div>
      </template>

      <div class="profile-content">
        <div class="profile-left">
          <el-form :model="form" label-width="80px">
            <el-form-item label="头像">
              <div class="avatar-wrapper">
                <el-avatar :size="80" :src="form.avatar || defaultAvatar" />
                <el-upload
                    class="avatar-uploader"
                    action="#"
                    :auto-upload="false"
                    :show-file-list="false"
                    :on-change="handleAvatarChange"
                    :disabled="avatarUploading"
                    accept="image/png,image/jpeg,image/webp,image/gif"
                >
                  <el-button type="primary" link :loading="avatarUploading">
                    {{ avatarUploading ? '上传中...' : '更换头像' }}
                  </el-button>
                </el-upload>
              </div>
            </el-form-item>
            <el-form-item label="用户名">
              <el-input v-model="form.username" disabled />
            </el-form-item>
            <el-form-item label="姓名">
              <el-input v-model="form.nickname" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" />
            </el-form-item>
            <el-form-item label="手机">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="form.intro" type="textarea" :rows="3" placeholder="请输入个人简介" />
            </el-form-item>
            <el-form-item label="角色">
              <el-tag>{{ roleLabel }}</el-tag>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveProfile">保存修改</el-button>
              <el-button @click="passwordDialogVisible = true">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>

        <div class="profile-right">
          <el-card v-if="isAdmin" class="stats-card">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span><el-icon><Lock /></el-icon> 账号安全</span>
              </div>
            </template>

            <div class="security-list">
              <div class="security-item">
                <div class="security-icon"><el-icon size="28"><Key /></el-icon></div>
                <div class="security-info">
                  <div class="security-title">登录密码</div>
                  <div class="security-desc">建议定期更换密码，确保账号安全</div>
                </div>
                <el-button type="primary" size="small" @click="passwordDialogVisible = true">修改</el-button>
              </div>
              <div class="security-item">
                <div class="security-icon"><el-icon size="28"><User /></el-icon></div>
                <div class="security-info">
                  <div class="security-title">账号角色</div>
                  <div class="security-desc">当前角色：{{ roleLabel }}</div>
                </div>
                <el-tag type="warning">{{ roleLabel }}</el-tag>
              </div>
            </div>
          </el-card>

          <el-card v-if="isAdmin" class="stats-card" style="margin-top: 20px">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span><el-icon><Clock /></el-icon> 最近登录记录</span>
              </div>
            </template>

            <div v-loading="loginLogsLoading" class="login-logs">
              <div v-if="recentLoginLogs.length === 0" class="empty-tip">
                暂无登录记录
              </div>
              <div v-else>
                <div v-for="(log, index) in recentLoginLogs" :key="index" class="login-log-item">
                  <div class="log-left">
                    <el-icon :class="log.status === 1 ? 'success' : 'fail'">
                      <CircleCheckFilled v-if="log.status === 1" />
                      <CircleCloseFilled v-else />
                    </el-icon>
                    <div class="log-info">
                      <div class="log-time">{{ formatTime(log.loginTime) }}</div>
                      <div class="log-detail">
                        <span>{{ log.ip }}</span>
                        <span v-if="log.browser"> · {{ log.browser }}</span>
                      </div>
                    </div>
                  </div>
                  <el-tag :type="log.status === 1 ? 'success' : 'danger'" size="small">
                    {{ log.status === 1 ? '成功' : '失败' }}
                  </el-tag>
                </div>
              </div>
            </div>
          </el-card>

          <el-card v-if="!isAdmin" class="stats-card">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span><el-icon><PieChart /></el-icon> 我的任务统计</span>
                <el-button type="primary" size="small" @click="exportPersonalReport">导出任务报表</el-button>
              </div>
            </template>

            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-value">{{ taskStats.completedThisMonth }}</div>
                <div class="stat-label">本月完成</div>
              </div>
              <div class="stat-item">
                <div class="stat-value overdue">{{ taskStats.overdueThisMonth }}</div>
                <div class="stat-label">本月逾期</div>
              </div>
              <div class="stat-item">
                <div class="stat-value in-progress">{{ taskStats.inProgress }}</div>
                <div class="stat-label">进行中</div>
              </div>
              <div class="stat-item">
                <div class="stat-value rate">{{ taskStats.completionRate }}%</div>
                <div class="stat-label">完成率</div>
              </div>
            </div>

            <div class="trend-chart">
              <div class="chart-title">近7天任务趋势</div>
              <div class="chart-bars">
                <div v-for="(item, index) in taskStats.trend" :key="index" class="bar-item">
                  <div class="bar-container">
                    <div class="bar-fill" :style="{ height: item.height + '%' }"></div>
                  </div>
                  <div class="bar-label">{{ item.label }}</div>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="旧密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" show-password placeholder="请确认新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from "vue";
import {ElMessage} from "element-plus";
import {PieChart, Lock, Key, User, Clock, CircleCheckFilled, CircleCloseFilled} from '@element-plus/icons-vue';
import Request from "@/utils/request.js";
import {getPersonalDashboard} from "@/api/dashboard.js";

const userStr = localStorage.getItem('code_user');
const currentUser = userStr ? JSON.parse(userStr) : {};
const isAdmin = currentUser.role === 'ADMIN';

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';
const form = ref({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  intro: '',
  avatar: '',
  role: ''
});

const passwordDialogVisible = ref(false);
const passwordFormRef = ref();
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
});

const taskStats = ref({
  completedThisMonth: 0,
  overdueThisMonth: 0,
  inProgress: 0,
  completionRate: 0,
  trend: []
});

const loginLogsLoading = ref(false);
const recentLoginLogs = ref([]);

const validateConfirm = (rule, value, callback) => {
  if (value !== passwordForm.value.newPassword) {
    callback(new Error('两次密码输入不一致'));
  } else {
    callback();
  }
};

const passwordRules = {
  oldPassword: [{required: true, message: '请输入旧密码', trigger: 'blur'}],
  newPassword: [
    {required: true, message: '请输入新密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'}
  ],
  confirmPassword: [
    {required: true, message: '请确认新密码', trigger: 'blur'},
    {validator: validateConfirm, trigger: 'blur'}
  ]
};

const roleLabel = computed(() => {
  const map = {ADMIN: '系统管理员', TEACHER: '指导教师', STUDENT: '学生'};
  return map[form.value.role] || form.value.role;
});

onMounted(() => {
  loadProfile();
  if (!isAdmin) {
    loadTaskStats();
  }
});

const loadProfile = () => {
  Request.get('/user/profile').then(res => {
    form.value = res.data;
    let localUser = JSON.parse(localStorage.getItem('code_user') || '{}');
    localUser.nickname = res.data.nickname;
    localUser.avatar = res.data.avatar;
    localStorage.setItem('code_user', JSON.stringify(localUser));
    if (isAdmin) {
      loadRecentLoginLogs();
    }
  });
};

const loadTaskStats = () => {
  getPersonalDashboard().then(res => {
    const data = res.data;
    const completed = data.completedTaskCount || 0;
    const total = data.totalTasks || 0;
    const trend = data.trend || [];

    const maxCount = Math.max(...trend.map(t => t.count || 0), 1);

    taskStats.value = {
      completedThisMonth: completed,
      overdueThisMonth: data.overdueCount || 0,
      inProgress: (data.todoCount || 0) + (data.inProgressCount || 0),
      completionRate: total === 0 ? 0 : Math.round((completed / total) * 100),
      trend: trend.map(t => ({
        label: t.date ? t.date.slice(5) : '',
        height: Math.round((t.count || 0) / maxCount * 100)
      }))
    };
  }).catch(() => {});
};

const loadRecentLoginLogs = () => {
  loginLogsLoading.value = true;
  Request.get('/admin/login-logs', {
    params: {
      pageNum: 1,
      pageSize: 5,
      username: form.value.username
    }
  }).then(res => {
    recentLoginLogs.value = res.data?.records || res.data?.list || [];
  }).catch(() => {}).finally(() => {
    loginLogsLoading.value = false;
  });
};

const formatTime = (time) => {
  if (!time) return '';
  const date = new Date(time);
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  const h = String(date.getHours()).padStart(2, '0');
  const mi = String(date.getMinutes()).padStart(2, '0');
  return `${y}-${m}-${d} ${h}:${mi}`;
};

const avatarUploading = ref(false);

const handleAvatarChange = (file) => {
  const raw = file.raw;
  if (!raw) return;
  if (!raw.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件');
    return;
  }
  if (raw.size > 2 * 1024 * 1024) {
    ElMessage.warning('头像大小不能超过 2MB');
    return;
  }
  const formData = new FormData();
  formData.append('file', raw);
  avatarUploading.value = true;
  Request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(res => {
    form.value.avatar = res.data;
    const localUser = JSON.parse(localStorage.getItem('code_user') || '{}');
    localUser.avatar = res.data;
    localStorage.setItem('code_user', JSON.stringify(localUser));
    window.dispatchEvent(new CustomEvent('profile-updated', { detail: { avatar: res.data } }));
    ElMessage.success('头像已更新');
  }).finally(() => {
    avatarUploading.value = false;
  });
};

const saveProfile = () => {
  Request.put('/user/profile', form.value).then(() => {
    const localUser = JSON.parse(localStorage.getItem('code_user') || '{}');
    localUser.nickname = form.value.nickname;
    localUser.avatar = form.value.avatar;
    localStorage.setItem('code_user', JSON.stringify(localUser));
    window.dispatchEvent(new CustomEvent('profile-updated', {
      detail: { nickname: form.value.nickname, avatar: form.value.avatar }
    }));
    ElMessage.success('保存成功');
    loadProfile();
  });
};

const savePassword = () => {
  passwordFormRef.value.validate(valid => {
    if (valid) {
      Request.put('/user/password', {
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword
      }).then(() => {
        ElMessage.success('密码修改成功，请重新登录');
        passwordDialogVisible.value = false;
        localStorage.removeItem('code_user');
        window.location.href = '/login';
      });
    }
  });
};

const exportPersonalReport = () => {
  const userStr = localStorage.getItem('code_user');
  const user = userStr ? JSON.parse(userStr) : {};
  const token = user.token || '';
  const url = `${Request.defaults.baseURL}/export/personal`;
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
      link.download = `个人任务报表_${new Date().toISOString().slice(0,10)}.xlsx`;
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
</script>

<style scoped>
.profile-page {
  padding: 20px;
}

.card-header {
  font-weight: bold;
}

.profile-content {
  display: flex;
  gap: 30px;
}

.profile-left {
  flex: 1;
  max-width: 600px;
}

.profile-right {
  flex: 1;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  gap: 15px;
}

.avatar-uploader {
  display: inline-block;
}

.stats-card {
  height: 100%;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.stat-item {
  text-align: center;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value.overdue {
  color: #f56c6c;
}

.stat-value.in-progress {
  color: #409EFF;
}

.stat-value.rate {
  color: #67c23a;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.trend-chart {
  padding-top: 10px;
  border-top: 1px solid #e4e7ed;
}

.chart-title {
  font-size: 14px;
  color: #606266;
  margin-bottom: 15px;
  font-weight: 500;
}

.chart-bars {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  height: 100px;
  padding: 0 10px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar-container {
  width: 24px;
  height: 80px;
  background-color: #f0f0f0;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
}

.bar-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #409EFF;
  border-radius: 4px;
  transition: height 0.3s;
}

.bar-label {
  font-size: 11px;
  color: #909399;
  margin-top: 6px;
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.security-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ecf5ff;
  color: #409eff;
  border-radius: 8px;
}

.security-info {
  flex: 1;
}

.security-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
}

.security-desc {
  font-size: 13px;
  color: #909399;
}

.login-logs {
  min-height: 200px;
}

.login-logs .empty-tip {
  text-align: center;
  color: #909399;
  padding: 40px 0;
  font-size: 14px;
}

.login-log-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.login-log-item:last-child {
  border-bottom: none;
}

.log-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.log-left .el-icon {
  font-size: 20px;
}

.log-left .el-icon.success {
  color: #67c23a;
}

.log-left .el-icon.fail {
  color: #f56c6c;
}

.log-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-time {
  font-size: 14px;
  color: #303133;
}

.log-detail {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 1000px) {
  .profile-content {
    flex-direction: column;
  }
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>