<template>
  <div v-loading="loading" class="training-members">
    <el-card>
      <template #header>
        <div class="header">
          <el-icon><UserFilled /></el-icon>
          <span>参训学生</span>
          <el-input
              v-model="searchKeyword"
              placeholder="搜索学生姓名或邮箱"
              class="search-input"
              clearable
              @keyup.enter="loadMembers"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>

      <div class="project-selector">
        <el-select
            v-model="activeProject"
            placeholder="请选择项目"
            filterable
            style="width: 280px"
            @change="handleProjectChange"
        >
          <el-option
              v-for="project in projects"
              :key="project.id"
              :label="projectLabel(project)"
              :value="String(project.id)"
          />
        </el-select>
        <span v-if="currentProject" class="current-project-hint">
          当前项目：{{ projectLabel(currentProject) }}
        </span>
        <span v-else class="current-project-hint">
          共 {{ projects.length }} 个实训项目
        </span>
      </div>

      <div v-if="projects.length === 0" class="empty">
        <el-icon size="48"><UserFilled /></el-icon>
        <p>暂无实训项目</p>
      </div>

      <template v-else>
        <el-empty v-if="members.length === 0" description="暂无参训学生" />

        <el-table v-else :data="filteredMembers" border stripe style="width: 100%">
          <el-table-column label="学生" min-width="210">
            <template #default="{ row }">
              <div class="student-cell">
                <el-avatar :size="32" :src="row.avatar || undefined">
                  <el-icon><User /></el-icon>
                </el-avatar>
                <div class="student-meta">
                  <div class="student-name">{{ row.nickname || row.username }}</div>
                  <div class="student-username">@{{ row.username }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="班级" min-width="140">
            <template #default>
              <span>{{ projectClassLabel }}</span>
            </template>
          </el-table-column>
          <el-table-column label="所属分组" min-width="120">
            <template #default="{ row }">
              <el-tag v-if="groupNameMap[row.userId]" size="small">{{ groupNameMap[row.userId] }}</el-tag>
              <el-tag v-else size="small" type="info" effect="plain">未分组</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="任务完成" width="110" align="center">
            <template #default="{ row }">
              {{ row.completedCount || 0 }} / {{ row.taskCount || 0 }}
            </template>
          </el-table-column>
          <el-table-column label="完成率" width="160">
            <template #default="{ row }">
              <el-progress :percentage="getCompletionRate(row)" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column label="加入时间" min-width="160">
            <template #default="{ row }">
              <span>{{ formatJoinTime(row.joinedAt) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="showMemberDetail(row)">详情</el-button>
              <el-button link type="primary" @click="viewMemberTasks(row)">任务</el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>
    </el-card>

    <el-dialog v-model="showDetail" title="学生详情" width="400px" destroy-on-close>
      <div v-if="selectedMember" class="member-detail">
        <div class="detail-avatar">
          <img v-if="selectedMember.avatar" :src="selectedMember.avatar" :alt="selectedMember.nickname" />
          <div v-else class="avatar-large">
            <el-icon><User /></el-icon>
          </div>
        </div>
        <div class="detail-info">
          <h3>{{ selectedMember.nickname || selectedMember.username }}</h3>
          <p>@{{ selectedMember.username }}</p>
          <p class="detail-email">{{ selectedMember.email || '未填写邮箱' }}</p>
        </div>
        <div class="detail-divider"></div>
        <div class="detail-stats">
          <div class="detail-stat">
            <div class="detail-stat-value">{{ selectedMember.taskCount || 0 }}</div>
            <div class="detail-stat-label">分配任务</div>
          </div>
          <div class="detail-stat">
            <div class="detail-stat-value">{{ selectedMember.completedCount || 0 }}</div>
            <div class="detail-stat-label">已完成</div>
          </div>
          <div class="detail-stat">
            <div class="detail-stat-value">{{ getCompletionRate(selectedMember) }}%</div>
            <div class="detail-stat-label">完成率</div>
          </div>
        </div>
        <div class="detail-meta">
          <span>加入时间：{{ formatJoinTime(selectedMember.joinedAt) }}</span>
        </div>
      </div>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <el-button type="primary" @click="viewMemberTasks(selectedMember)">查看任务</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showTasksModal" title="学生任务列表" width="900px" destroy-on-close>
      <div v-loading="loading">
        <el-table :data="memberTasks" style="width: 100%">
          <el-table-column label="任务标题" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span :class="{ 'done-task': row.status === 'DONE' }">{{ row.title }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="90">
            <template #default="{ row }">
              <el-tag :type="getPriorityType(row.priority)" size="small">{{ getPriorityLabel(row.priority) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="140">
            <template #default="{ row }">
              <el-progress :percentage="row.progress || 0" :status="row.status === 'DONE' ? 'success' : ''" :stroke-width="6" />
            </template>
          </el-table-column>
          <el-table-column label="截止日期" width="120">
            <template #default="{ row }">
              <span>{{ row.endDate || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && memberTasks.length === 0" description="暂无任务" />
      </div>
      <template #footer>
        <el-button @click="showTasksModal = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { UserFilled, User, Search } from '@element-plus/icons-vue';
import Request from '@/utils/request.js';
import { listAllClasses, listGroups, listGroupMembers } from '@/api/training.js';

const loading = ref(false);
const projects = ref([]);
const members = ref([]);
const classes = ref([]);
const groupData = ref([]);
const activeProject = ref('');
const searchKeyword = ref('');
const showDetail = ref(false);
const selectedMember = ref(null);
const showTasksModal = ref(false);
const memberTasks = ref([]);

const classMap = computed(() => {
  const map = {};
  classes.value.forEach(c => { map[c.id] = c; });
  return map;
});

const classLabel = (cls) => {
  if (!cls) return '';
  const grade = cls.grade ? (String(cls.grade).endsWith('级') ? cls.grade : `${cls.grade}级`) : '';
  return [grade, cls.name].filter(Boolean).join(' ');
};

const projectLabel = (project) => {
  if (!project) return '';
  const cls = classMap.value[project.classId];
  const clsText = classLabel(cls);
  return clsText ? `${project.name}（${clsText}）` : project.name;
};

const currentProject = computed(() => {
  if (activeProject.value === 'all') return null;
  return projects.value.find(p => String(p.id) === activeProject.value) || null;
});

const getStatusLabel = (status) => {
  const map = {
    TODO: '待办',
    IN_PROGRESS: '进行中',
    REVIEW: '审核中',
    DONE: '已完成',
    BLOCKED: '已阻塞'
  };
  return map[status] || status;
};

const getStatusType = (status) => {
  const map = {
    TODO: 'info',
    IN_PROGRESS: 'primary',
    REVIEW: 'warning',
    DONE: 'success',
    BLOCKED: 'danger'
  };
  return map[status] || 'info';
};

const getPriorityLabel = (priority) => {
  const map = {
    HIGH: '高',
    MEDIUM: '中',
    LOW: '低'
  };
  return map[priority] || priority;
};

const getPriorityType = (priority) => {
  const map = {
    HIGH: 'danger',
    MEDIUM: 'warning',
    LOW: 'success'
  };
  return map[priority] || 'info';
};

const formatJoinTime = (time) => {
  if (!time) return '';
  return new Date(time).toLocaleString('zh-CN');
};

const getCompletionRate = (member) => {
  const total = member.taskCount || 0;
  const completed = member.completedCount || 0;
  if (total === 0) return 0;
  return Math.round((completed / total) * 100);
};

const filteredMembers = computed(() => {
  if (!searchKeyword.value) return members.value;
  const keyword = searchKeyword.value.toLowerCase();
  return members.value.filter(member => {
    const nickname = (member.nickname || '').toLowerCase();
    const username = (member.username || '').toLowerCase();
    const email = (member.email || '').toLowerCase();
    return nickname.includes(keyword) || username.includes(keyword) || email.includes(keyword);
  });
});

const groupNameMap = computed(() => {
  const map = {};
  groupData.value.forEach(g => {
    const name = g.name || g.groupNo || ('分组' + g.id);
    (g.memberIds || []).forEach(id => { map[id] = name; });
  });
  return map;
});

const projectClassLabel = computed(() => {
  const project = currentProject.value;
  if (!project) return '—';
  return classLabel(classMap.value[project.classId]) || '—';
});

const loadClasses = async () => {
  try {
    const res = await listAllClasses();
    classes.value = res.data || [];
  } catch (error) {
    classes.value = [];
  }
};

const loadGroupData = async (projectId) => {
  try {
    const res = await listGroups(projectId);
    const groups = res.data || [];
    groupData.value = await Promise.all(groups.map(async g => {
      try {
        const memberRes = await listGroupMembers(g.id);
        return { ...g, memberIds: (memberRes.data || []).map(m => m.studentId) };
      } catch (error) {
        return { ...g, memberIds: [] };
      }
    }));
  } catch (error) {
    groupData.value = [];
  }
};

const loadProjects = async () => {
  loading.value = true;
  try {
    const res = await Request.get('/project/list', {
      params: { pageNum: 1, pageSize: 100 }
    });
    projects.value = res.data.records || [];
    if (projects.value.length > 0) {
      activeProject.value = String(projects.value[0].id);
      await loadMembers();
    }
  } catch (error) {
    console.error('加载项目失败', error);
  } finally {
    loading.value = false;
  }
};

const handleProjectChange = () => {
  searchKeyword.value = '';
  loadMembers();
};

const loadSingleProjectMembers = async (projectId) => {
  const res = await Request.get('/member/project-members', {
    params: { projectId: projectId }
  });
  return res.data || [];
};

const loadMembers = async () => {
  if (!activeProject.value) {
    members.value = [];
    groupData.value = [];
    return;
  }
  loading.value = true;
  try {
    const projectId = parseInt(activeProject.value);
    members.value = await loadSingleProjectMembers(projectId);
    await loadGroupData(projectId);
  } catch (error) {
    console.error('加载成员失败', error);
  } finally {
    loading.value = false;
  }
};

const showMemberDetail = (member) => {
  selectedMember.value = member;
  showDetail.value = true;
};

const viewMemberTasks = async (member) => {
  showDetail.value = false;
  const userId = member.userId;
  loading.value = true;
  try {
    const projectId = parseInt(activeProject.value);
    const res = await Request.get(`/project/${projectId}/tasks`, {
      params: { assigneeId: userId }
    });
    memberTasks.value = res.data || [];
    showTasksModal.value = true;
  } catch (error) {
    console.error('加载成员任务失败', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadClasses();
  loadProjects();
});
</script>

<style scoped>
.training-members {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header span {
  font-size: 18px;
  font-weight: 600;
}

.search-input {
  margin-left: auto;
  width: 260px;
}

.project-selector {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.current-project-hint {
  font-size: 13px;
  color: #909399;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
}

.empty p {
  margin-top: 16px;
  font-size: 14px;
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.student-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.student-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  line-height: 1.4;
}

.student-username {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
}

.member-detail {
  padding: 20px 0;
}

.detail-avatar {
  text-align: center;
  margin-bottom: 20px;
}

.detail-avatar img {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-large {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #e8f4fd;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  margin: 0 auto;
}

.avatar-large :deep(.el-icon) {
  font-size: 36px;
}

.detail-info {
  text-align: center;
}

.detail-info h3 {
  font-size: 20px;
  font-weight: 600;
  margin: 0;
}

.detail-info p {
  font-size: 14px;
  color: #909399;
  margin: 4px 0;
}

.detail-email {
  color: #606266 !important;
}

.detail-projects {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.detail-projects .el-icon {
  font-size: 12px;
}

.detail-divider {
  height: 1px;
  background: #f0f0f0;
  margin: 20px 0;
}

.detail-stats {
  display: flex;
  justify-content: center;
  gap: 32px;
  margin-bottom: 20px;
}

.detail-stat {
  text-align: center;
}

.detail-stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.detail-stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.detail-meta {
  text-align: center;
  font-size: 13px;
  color: #909399;
}

.done-task {
  text-decoration: line-through;
  color: #909399;
}
</style>