<template>
  <div v-loading="loading">
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon blue">
          <el-icon><User /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayCount || 0 }}</div>
          <div class="stat-label">今日登录</div>
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
          <el-icon><TrendCharts /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalCount || 0 }}</div>
          <div class="stat-label">累计登录</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">
          <el-icon><Avatar /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalUserCount || 0 }}</div>
          <div class="stat-label">活跃用户</div>
        </div>
      </div>
    </div>

    <el-card style="margin-top: 15px">
      <template #header>
        <span>登录记录</span>
      </template>

      <div class="filter-bar">
        <el-input v-model="filter.username" placeholder="用户名" style="width: 160px" clearable />
        <el-select v-model="filter.status" clearable placeholder="状态" style="width: 120px">
          <el-option label="成功" :value="1" />
          <el-option label="失败" :value="0" />
        </el-select>
        <el-date-picker v-model="filter.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" />
        <el-date-picker v-model="filter.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-button type="primary" size="small" @click="loadLogs">查询</el-button>
        <el-button size="small" @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="logs" border style="margin-top: 15px">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="130" />
        <el-table-column prop="loginType" label="登录方式" width="110" />
        <el-table-column prop="ip" label="IP" width="130" />
        <el-table-column prop="userAgent" label="User-Agent" min-width="220" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
              {{ scope.row.status === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="failReason" label="失败原因" min-width="160" show-overflow-tooltip />
        <el-table-column prop="loginTime" label="登录时间" width="170" />
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { User, CircleCheck, TrendCharts, Avatar } from '@element-plus/icons-vue';
import { listLoginLogs, getLoginLogStats } from '@/api/loginLog.js';

const loading = ref(false);
const logs = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filter = reactive({ username: '', status: null, startDate: '', endDate: '' });
const stats = reactive({
  todayCount: 0,
  successRate: 0,
  totalCount: 0,
  totalUserCount: 0
});

const loadStats = () => {
  getLoginLogStats().then(res => {
    if (res.data) {
      Object.assign(stats, res.data);
    }
  }).catch(() => {});
};

const loadLogs = () => {
  loading.value = true;
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    username: filter.username || null,
    status: filter.status,
    startDate: filter.startDate || null,
    endDate: filter.endDate || null
  };
  listLoginLogs(params).then(res => {
    logs.value = res.data.records || [];
    total.value = res.data.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const resetFilter = () => {
  filter.username = '';
  filter.status = null;
  filter.startDate = '';
  filter.endDate = '';
  pageNum.value = 1;
  loadLogs();
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
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
}
</style>
