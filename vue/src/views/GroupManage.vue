<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>实训分组管理</span>
          <div class="header-actions">
            <el-button v-if="canManageGroup" type="primary" @click="openSetupDialog">分组设置</el-button>
            <el-button v-if="canManageGroup" @click="openGroupDialog()">新增分组</el-button>
          </div>
        </div>
      </template>

      <el-table :data="groups" border v-loading="loading">
        <el-table-column prop="groupNo" label="组号" width="100" />
        <el-table-column prop="name" label="小组名称" min-width="160" />
        <el-table-column prop="description" label="小组说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="组员" width="100" align="center">
          <template #default="scope">
            {{ scope.row.memberCount || 0 }} / {{ scope.row.maxMembers ? scope.row.maxMembers : '不限' }}
          </template>
        </el-table-column>
        <el-table-column :label="'操作'" :width="canManageGroup ? 230 : 100" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openMembers(scope.row)">组员管理</el-button>
            <el-button v-if="canManageGroup" link type="primary" @click="openGroupDialog(scope.row)">编辑</el-button>
            <el-button v-if="canManageGroup" link type="danger" @click="removeGroup(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && groups.length === 0" description="暂无分组，请先新增" />
    </el-card>

    <!-- 未分组学生池 -->
    <el-card class="ungrouped-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>未分组学生（{{ ungroupedStudents.length }}）</span>
          <span v-if="groups.length === 0" class="header-tip">请先创建小组</span>
        </div>
      </template>
      <el-empty v-if="ungroupedStudents.length === 0" description="暂无未分组学生" :image-size="60" />
      <div v-else class="ungrouped-list">
        <div v-for="student in ungroupedStudents" :key="student.userId" class="ungrouped-item">
          <div class="ungrouped-info">
            <span class="student-name">{{ student.nickname || student.username }}</span>
            <span class="student-username">{{ student.username }}</span>
          </div>
          <div class="ungrouped-action">
            <template v-if="groups.length > 0">
              <el-select
                  v-model="selectedGroupMap[student.userId]"
                  placeholder="选择小组"
                  size="small"
                  style="width: 150px"
              >
                <el-option
                    v-for="g in groups"
                    :key="g.id"
                    :label="g.name || g.groupNo || ('分组' + g.id)"
                    :value="g.id"
                />
              </el-select>
              <el-button
                  type="primary"
                  size="small"
                  :disabled="!selectedGroupMap[student.userId]"
                  :loading="joiningStudentId === student.userId"
                  @click="joinToGroup(student)"
              >加入小组</el-button>
            </template>
            <span v-else class="header-tip">请先创建小组</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 分组新增/编辑 -->
    <el-dialog v-model="groupDialogVisible" :title="groupForm.id ? '编辑分组' : '新增分组'" width="480px">
      <el-form :model="groupForm" label-width="90px">
        <el-form-item label="小组名称" required><el-input v-model="groupForm.name" /></el-form-item>
        <el-form-item label="组号"><el-input v-model="groupForm.groupNo" placeholder="如 第1组" /></el-form-item>
        <el-form-item label="小组说明"><el-input v-model="groupForm.description" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="groupForm.maxMembers" :min="0" :max="50" />
          <div class="dialog-tip">0 表示不限制，学生自选加入时按此上限校验</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="groupDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveGroup">确定</el-button>
      </template>
    </el-dialog>

    <!-- 分组设置 -->
    <el-dialog v-model="setupDialogVisible" title="分组设置" width="520px">
      <el-form label-width="100px">
        <el-form-item label="创建组数">
          <el-input-number v-model="setupForm.groupCount" :min="1" :max="50" />
          <span class="setup-unit">个空组</span>
        </el-form-item>
      </el-form>
      <el-alert :closable="false" type="info" show-icon>
        <template #title>
          <template v-if="projectStudentCount > 0">
            项目共 {{ projectStudentCount }} 名学生，预计 {{ effectiveGroupCount }} 组，每组约 {{ effectiveGroupSize }} 人
          </template>
          <template v-else>
            项目暂无学生成员，将创建 {{ effectiveGroupCount }} 个空分组
          </template>
        </template>
      </el-alert>
      <template #footer>
        <el-button @click="setupDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="setupRunning" @click="submitSetup">开始分组</el-button>
      </template>
    </el-dialog>

    <!-- 组员管理 -->
    <el-dialog v-model="memberDialogVisible" :title="`组员管理 - ${currentGroup.name || ''}`" width="560px">
      <div v-if="canManageGroup" class="add-member-bar">
        <el-select v-model="pendingStudentIds" multiple filterable placeholder="选择学生加入小组" style="flex: 1">
          <el-option v-for="u in candidateStudents" :key="u.id" :label="u.nickname || u.username"
                     :value="u.id" />
        </el-select>
        <el-button type="primary" :disabled="pendingStudentIds.length === 0" @click="saveMembers">加入</el-button>
      </div>
      <el-table :data="members" border v-loading="memberLoading" style="margin-top: 12px">
        <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="姓名" min-width="120" />
        <el-table-column label="组长" width="80" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.studentId === currentGroup.leaderId" size="small" type="warning" effect="plain">组长</el-tag>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canManageGroup" label="操作" width="170" fixed="right">
          <template #default="scope">
            <el-button
                v-if="scope.row.studentId !== currentGroup.leaderId"
                link
                type="primary"
                @click="setLeader(scope.row)"
            >设为组长</el-button>
            <el-button link type="danger" @click="removeMember(scope.row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!memberLoading && members.length === 0" description="暂无组员" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listGroups, listGroupMembers, createGroup, updateGroup, deleteGroup, addGroupMembers, removeGroupMember,
  batchCreateGroups
} from "@/api/training.js";
import { searchUsers } from "@/api/user.js";
import { assignGroupLeader, getProjectDetail } from "@/api/project.js";

const props = defineProps({
  projectId: { type: Number, required: true }
});

const user = JSON.parse(localStorage.getItem('code_user') || '{}');
const canManageGroup = ['ADMIN', 'TEACHER'].includes(user.role);
const loading = ref(false);
const groups = ref([]);

const setupDialogVisible = ref(false);
const setupRunning = ref(false);
const setupForm = ref({ groupCount: 4 });
const projectStudentCount = ref(0);

const effectiveGroupCount = computed(() => Math.max(1, setupForm.value.groupCount || 1));

const effectiveGroupSize = computed(() => {
  const total = projectStudentCount.value;
  return total > 0 ? Math.ceil(total / effectiveGroupCount.value) : 0;
});

const projectMembers = ref([]);
const groupMemberIdMap = ref({});
const selectedGroupMap = ref({});
const joiningStudentId = ref(null);

const ungroupedStudents = computed(() => {
  const assigned = new Set();
  Object.values(groupMemberIdMap.value).forEach(ids => ids.forEach(id => assigned.add(id)));
  return projectMembers.value.filter(m => !assigned.has(m.userId));
});

const groupDialogVisible = ref(false);
const groupForm = ref({});

const memberDialogVisible = ref(false);
const memberLoading = ref(false);
const members = ref([]);
const currentGroup = ref({});
const pendingStudentIds = ref([]);
const candidateStudents = ref([]);

const loadGroups = async () => {
  loading.value = true;
  try {
    const res = await listGroups(props.projectId);
    groups.value = res.data || [];
    const memberLists = await Promise.all(groups.value.map(g =>
        listGroupMembers(g.id)
            .then(r => ({ id: g.id, ids: (r.data || []).map(m => m.studentId) }))
            .catch(() => ({ id: g.id, ids: [] }))
    ));
    const map = {};
    memberLists.forEach(item => { map[item.id] = item.ids; });
    groupMemberIdMap.value = map;
    const detailRes = await getProjectDetail(props.projectId);
    projectMembers.value = (detailRes.data && detailRes.data.members) || [];
    projectStudentCount.value = projectMembers.value.length;
  } catch (e) {
    groups.value = [];
    groupMemberIdMap.value = {};
  } finally {
    loading.value = false;
  }
};

const joinToGroup = (student) => {
  const groupId = selectedGroupMap.value[student.userId];
  if (!groupId) { ElMessage.warning('请选择目标小组'); return; }
  joiningStudentId.value = student.userId;
  addGroupMembers(groupId, [student.userId]).then(() => {
    ElMessage.success(`已将「${student.nickname || student.username}」加入小组`);
    delete selectedGroupMap.value[student.userId];
    loadGroups();
  }).finally(() => {
    joiningStudentId.value = null;
  });
};

const loadCandidates = () => {
  if (candidateStudents.value.length > 0) return;
  searchUsers({ pageNum: 1, pageSize: 200 }).then(res => {
    candidateStudents.value = ((res.data && res.data.records) || []).filter(u => u.role === 'STUDENT');
  });
};

const loadProjectStudentCount = () => {
  getProjectDetail(props.projectId).then(res => {
    projectStudentCount.value = ((res.data && res.data.members) || []).length;
  }).catch(() => {});
};

const openGroupDialog = (row) => {
  groupForm.value = row
      ? { ...row }
      : { projectId: props.projectId, name: '', groupNo: '', description: '', maxMembers: 6, joinEnabled: true };
  groupDialogVisible.value = true;
};

const saveGroup = () => {
  if (!groupForm.value.name) { ElMessage.warning('请输入小组名称'); return; }
  const payload = { ...groupForm.value, joinEnabled: true };
  const done = () => { groupDialogVisible.value = false; loadGroups(); };
  if (groupForm.value.id) {
    updateGroup(groupForm.value.id, payload).then(() => { ElMessage.success('保存成功'); done(); });
  } else {
    createGroup(payload).then(() => { ElMessage.success('新增成功'); done(); });
  }
};

const removeGroup = (row) => {
  ElMessageBox.confirm(`确定删除分组「${row.name}」及其成员吗？`, '提示', { type: 'warning' }).then(() => {
    deleteGroup(row.id).then(() => { ElMessage.success('删除成功'); loadGroups(); });
  }).catch(() => {});
};

const openMembers = (row) => {
  currentGroup.value = row;
  pendingStudentIds.value = [];
  memberDialogVisible.value = true;
  loadCandidates();
  loadMembers();
};

const loadMembers = () => {
  memberLoading.value = true;
  listGroupMembers(currentGroup.value.id).then(res => {
    members.value = res.data;
  }).finally(() => memberLoading.value = false);
};

const saveMembers = () => {
  addGroupMembers(currentGroup.value.id, pendingStudentIds.value).then(() => {
    ElMessage.success('添加成功');
    pendingStudentIds.value = [];
    loadMembers();
    loadGroups();
  });
};

const setLeader = (row) => {
  const name = row.nickname || row.username;
  ElMessageBox.confirm(`确定将「${name}」设为本组组长吗？`, '提示', { type: 'warning' }).then(() => {
    assignGroupLeader(props.projectId, row.studentId).then(() => {
      ElMessage.success('组长已变更');
      currentGroup.value.leaderId = row.studentId;
      loadGroups();
    });
  }).catch(() => {});
};

const removeMember = (row) => {
  ElMessageBox.confirm(`确定将「${row.nickname || row.username}」移出小组吗？`, '提示', { type: 'warning' }).then(() => {
    removeGroupMember(currentGroup.value.id, row.studentId).then(() => {
      ElMessage.success('移除成功');
      loadMembers();
      loadGroups();
    });
  }).catch(() => {});
};

// ===== 分组设置 =====
const openSetupDialog = () => {
  setupForm.value = { groupCount: 4 };
  loadProjectStudentCount();
  setupDialogVisible.value = true;
};

const submitSetup = () => {
  setupRunning.value = true;
  batchCreateGroups(props.projectId, {
    count: effectiveGroupCount.value,
    maxMembers: effectiveGroupSize.value,
    joinEnabled: true
  }).then(res => {
    ElMessage.success(`已创建 ${(res.data && res.data.groupCount) || 0} 个分组`);
    setupDialogVisible.value = false;
    loadGroups();
  }).finally(() => {
    setupRunning.value = false;
  });
};

onMounted(() => {
  loadGroups();
});
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.dialog-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.setup-unit {
  margin-left: 8px;
  font-size: 13px;
  color: #606266;
}

.add-member-bar {
  display: flex;
  gap: 10px;
}

.text-muted {
  color: #c0c4cc;
}

.ungrouped-card {
  margin-top: 16px;
}

.header-tip {
  font-size: 13px;
  color: #909399;
}

.ungrouped-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 12px;
}

.ungrouped-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fafafa;
}

.ungrouped-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.student-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.student-username {
  font-size: 12px;
  color: #909399;
}

.ungrouped-action {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}
</style>