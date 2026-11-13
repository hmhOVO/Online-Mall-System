<template>
  <div>
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div>
            <el-button v-if="isAdmin && showStudentActions" type="primary" plain @click="openImportDialog('STUDENT')">批量导入学生</el-button>
            <el-button v-if="isAdmin && showTeacherActions" type="primary" plain @click="openImportDialog('TEACHER')">批量导入教师</el-button>
            <el-button v-if="isAdmin" type="primary" @click="openAddDialog">{{ addButtonLabel }}</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeRoleTab" @tab-change="handleRoleTabChange" class="role-tabs">
        <el-tab-pane label="学生" name="STUDENT" />
        <el-tab-pane label="教师" name="TEACHER" />
        <el-tab-pane v-if="isAdmin" label="管理员" name="ADMIN" />
      </el-tabs>

      <div class="search-bar">
        <el-input v-model="search.username" placeholder="用户名" clearable style="width: 180px" />
        <el-select v-model="search.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="isAdmin" class="batch-actions">
        <span class="selected-tip">已选择 <span class="selected-count">{{ selectedUsers.length }}</span> 项</span>
        <el-button
            type="success"
            plain
            :disabled="selectedUsers.length === 0"
            @click="batchToggleStatus('ENABLED')"
        >
          <el-icon><CircleCheck /></el-icon>
          <span>批量启用</span>
        </el-button>
        <el-button
            plain
            :disabled="selectedUsers.length === 0"
            @click="batchToggleStatus('DISABLED')"
        >
          <el-icon><Warning /></el-icon>
          <span>批量禁用</span>
        </el-button>
        <el-button
            plain
            :disabled="selectedUsers.length === 0"
            @click="batchResetPassword"
        >
          <el-icon><Key /></el-icon>
          <span>批量重置密码</span>
        </el-button>
        <el-button
            plain
            :disabled="selectedUsers.length === 0"
            @click="batchUnlock"
        >
          <el-icon><Unlock /></el-icon>
          <span>批量解锁</span>
        </el-button>
        <el-button
            type="danger"
            :disabled="selectedUsers.length === 0"
            @click="batchDelete"
        >
          <el-icon><Delete /></el-icon>
          <span>批量删除</span>
        </el-button>
      </div>

      <el-table
          :data="tableData"
          border
          v-loading="loading"
          @selection-change="handleSelectionChange"
          ref="tableRef"
      >
        <el-table-column v-if="isAdmin" type="selection" width="55" align="center" />
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="姓名" min-width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="roleType(scope.row.role)">{{ roleLabel(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="账号状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'" effect="plain">
              {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lockStatus" label="锁定状态" width="90">
          <template #default="scope">
            <el-tag v-if="lockStatusMap[scope.row.id]?.locked" type="warning" effect="plain">
              已锁定
            </el-tag>
            <el-tag v-else type="success" effect="plain">
              正常
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="160" />
        <el-table-column label="操作" :width="isAdmin ? '320' : '150'" fixed="right">
          <template #default="scope">
            <el-button
                v-if="isAdmin && scope.row.role !== 'ADMIN'"
                link class="link-neutral" @click="toggleStatus(scope.row)">
              {{ scope.row.status === 'ENABLED' ? '禁用' : '启用' }}
            </el-button>
            <el-button v-if="isAdmin && lockStatusMap[scope.row.id]?.locked" link type="info" @click="unlockUser(scope.row)">解锁</el-button>
            <el-button v-if="isAdmin" link type="primary" @click="resetPassword(scope.row)">重置密码</el-button>
            <el-button v-if="isAdmin" link type="danger" @click="deleteUser(scope.row)">删除</el-button>
            <span v-else-if="scope.row.role === 'ADMIN'" class="no-permission">无权限操作</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            :current-page="pageNum"
            @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="addDialogVisible" :title="addDialogTitle" width="520px">
      <el-form :model="addForm" :rules="addRules" ref="addFormRef" label-width="80px">
        <el-row :gutter="16">
          <!-- 角色由当前 Tab 决定；学生/教师的学号、工号即登录账号 -->
          <el-col v-if="addForm.role === 'STUDENT'" :span="12">
            <el-form-item label="学号" prop="studentNo">
              <el-input v-model="addForm.studentNo" placeholder="请输入学号（即登录账号）" />
            </el-form-item>
          </el-col>
          <el-col v-else-if="addForm.role === 'TEACHER'" :span="12">
            <el-form-item label="工号" prop="teacherNo">
              <el-input v-model="addForm.teacherNo" placeholder="请输入工号（即登录账号）" />
            </el-form-item>
          </el-col>
          <el-col v-else :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="addForm.username" placeholder="请输入登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名">
              <el-input v-model="addForm.nickname" placeholder="选填，默认同登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="密码" prop="password">
              <el-input v-model="addForm.password" show-password placeholder="请输入初始密码" />
            </el-form-item>
          </el-col>
          <el-col v-if="addForm.role !== 'ADMIN'" :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="addForm.email" placeholder="选填" />
            </el-form-item>
          </el-col>

          <!-- 学生：学院/专业由所选班级自动带出 -->
          <el-col v-if="addForm.role === 'STUDENT'" :span="24">
            <el-form-item label="班级">
              <el-select v-model="addForm.classId" placeholder="选填，按 学院 / 专业 / 班级 选择" clearable filterable style="width: 100%" @change="onStudentClassChange">
                <el-option v-for="c in allClassesAll" :key="c.id" :label="classOptionLabel(c)" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="addLoading" @click="saveAdd">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importDialogVisible" :title="importType === 'TEACHER' ? '批量导入教师' : '批量导入学生'" width="520px" destroy-on-close>
      <el-form label-width="80px">
        <template v-if="importType === 'STUDENT'">
          <el-form-item label="学院">
            <el-select v-model="importForm.schoolId" placeholder="请选择学院" clearable filterable style="width: 100%" @change="onImportSchoolChange">
              <el-option v-for="s in allSchools" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="专业">
            <el-select v-model="importForm.majorId" placeholder="请选择专业" clearable filterable style="width: 100%" :disabled="!importForm.schoolId" @change="onImportMajorChange">
              <el-option v-for="m in allMajors" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="班级">
            <el-select v-model="importForm.classId" placeholder="请选择班级" clearable filterable style="width: 100%" :disabled="!importForm.majorId">
              <el-option v-for="c in allClasses" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </template>
        <el-form-item label="Excel 文件">
          <div class="import-file">
            <input ref="importFileRef" type="file" accept=".xlsx,.xls" style="display: none" @change="onImportFileChange" />
            <el-button @click="importFileRef?.click()">选择文件</el-button>
            <span class="import-filename">{{ importFileName || '未选择文件' }}</span>
          </div>
        </el-form-item>
        <el-alert v-if="importType === 'STUDENT'" type="info" :closable="false" show-icon>
          <div>Excel 第一列为学号，第二列为姓名（第一行为表头）。</div>
          <div>导入后用户名 = 学号，初始密码为 123456。</div>
        </el-alert>
        <el-alert v-else type="info" :closable="false" show-icon>
          <div>Excel 第一列为工号，第二列为姓名（第一行为表头）。</div>
          <div>导入后用户名 = 工号，初始密码为 123456。</div>
        </el-alert>
      </el-form>
      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="importLoading" @click="saveImport">开始导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, onMounted, computed} from "vue";
import {ElMessage, ElMessageBox} from "element-plus";
import Request from "@/utils/request.js";
import {
  CircleCheck, Warning, Key, Delete, Unlock
} from '@element-plus/icons-vue';
import { listAllSchools, listAllMajors, listAllClasses } from "@/api/training.js";
import { importStudents, importTeachers } from "@/api/user.js";

const user = ref(JSON.parse(localStorage.getItem('code_user') || '{}'));
const isAdmin = computed(() => user.value.role === 'ADMIN');

const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(10);
const search = ref({username: '', role: 'STUDENT', status: ''});
const activeRoleTab = ref('STUDENT');
const tabRoleMap = { STUDENT: 'STUDENT', TEACHER: 'TEACHER', ADMIN: 'ADMIN' };
const showStudentActions = computed(() => activeRoleTab.value === 'STUDENT');
const showTeacherActions = computed(() => activeRoleTab.value === 'TEACHER');
const addButtonLabel = computed(() => ({
  STUDENT: '新增学生',
  TEACHER: '新增教师',
  ADMIN: '新增管理员'
}[activeRoleTab.value] || '新增用户'));
const addDialogTitle = computed(() => addButtonLabel.value);
const tableRef = ref(null);
const selectedUsers = ref([]);
const lockStatusMap = ref({});
const allSchools = ref([]);
const allMajors = ref([]);
const allClasses = ref([]);
// 新增表单使用：全量专业/班级（班级下拉直接展示 学院/专业/班级）
const allMajorsAll = ref([]);
const allClassesAll = ref([]);

const importDialogVisible = ref(false);
const importType = ref('STUDENT');
const importLoading = ref(false);
const importFileRef = ref(null);
const importFileName = ref('');
const importFile = ref(null);
const importForm = ref({
  schoolId: null,
  majorId: null,
  classId: null
});

const addDialogVisible = ref(false);
const addFormRef = ref();
const addLoading = ref(false);
const addForm = ref({
  username: '',
  password: '',
  nickname: '',
  role: 'STUDENT',
  studentNo: '',
  teacherNo: '',
  schoolId: null,
  majorId: null,
  classId: null,
  grade: '',
  email: ''
});

const addRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {min: 3, message: '用户名至少3位', trigger: 'blur'}
  ],
  password: [
    {required: true, message: '请输入密码', trigger: 'blur'},
    {min: 6, message: '密码至少6位', trigger: 'blur'}
  ],
  studentNo: [{
    validator: (rule, value, callback) => {
      addForm.value.role === 'STUDENT' && !value ? callback(new Error('请输入学号')) : callback();
    },
    trigger: 'blur'
  }],
  teacherNo: [{
    validator: (rule, value, callback) => {
      addForm.value.role === 'TEACHER' && !value ? callback(new Error('请输入工号')) : callback();
    },
    trigger: 'blur'
  }]
};

const handleSelectionChange = (selection) => {
  selectedUsers.value = selection;
};

const batchToggleStatus = (status) => {
  const action = status === 'ENABLED' ? '启用' : '禁用';
  ElMessageBox.confirm(
    `确定${action}选中的 ${selectedUsers.value.length} 个用户吗？`,
    '确认操作',
    {type: 'warning'}
  ).then(() => {
    const ids = selectedUsers.value.map(u => u.id);
    Request.put(`/admin/users/batch/status?status=${status}`, ids).then(() => {
      ElMessage.success(`批量${action}成功`);
      loadData();
    });
  }).catch(() => {});
};

const batchResetPassword = () => {
  ElMessageBox.prompt(
    `即将重置 ${selectedUsers.value.length} 个用户的密码，请输入您的管理员密码确认`,
    '批量重置密码确认',
    {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPlaceholder: '请输入您的登录密码',
      type: 'warning',
      beforeClose: (action, instance, done) => {
        if (action === 'confirm') {
          if (!instance.inputValue) {
            instance.inputErrorMessage = '请输入管理员密码';
            return;
          }
          instance.confirmButtonLoading = true;
          const ids = selectedUsers.value.map(u => u.id);
          Request.put(`/admin/users/batch/reset-password`, {ids, confirmPassword: instance.inputValue}).then(() => {
            done();
            ElMessage.success('批量重置密码成功，新密码为 123456');
          }).catch((err) => {
            instance.inputErrorMessage = err?.msg || '重置失败';
            instance.confirmButtonLoading = false;
          });
        } else {
          done();
        }
      }
    }
  ).catch(() => {});
};

const batchDelete = () => {
  ElMessageBox.confirm(
    `确定删除选中的 ${selectedUsers.value.length} 个用户吗？在岗项目负责人将无法删除。`,
    '确认删除',
    {type: 'warning'}
  ).then(() => {
    const ids = selectedUsers.value.map(u => u.id);
    Request.delete(`/admin/users/batch`, {data: ids}).then(() => {
      ElMessage.success('批量删除成功');
      loadData();
    });
  }).catch(() => {});
};

onMounted(() => {
  loadData();
  loadSchoolOptions();
});

const loadData = () => {
  loading.value = true;
  Request.get('/admin/users', {
    params: {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      username: search.value.username,
      role: search.value.role,
      status: search.value.status
    }
  }).then(res => {
    tableData.value = res.data.records;
    total.value = res.data.total;
    if (isAdmin.value) {
      loadAllLockStatus(res.data.records);
    }
  }).finally(() => {
    loading.value = false;
  });
};

const loadAllLockStatus = (users) => {
  if (!users || users.length === 0) return;
  const map = {};
  const promises = users.map(user => {
    return Request.get(`/admin/user/${user.id}/lock-status`).then(res => {
      map[user.id] = res.data;
    }).catch(() => {
      map[user.id] = { locked: false, failCount: 0 };
    });
  });
  Promise.all(promises).then(() => {
    lockStatusMap.value = map;
  });
};

const unlockUser = (row) => {
  ElMessageBox.confirm(
    `确定解锁用户「${row.username}」吗？解锁后用户可重新登录。`,
    '解锁确认',
    {type: 'warning'}
  ).then(() => {
    Request.put(`/admin/user/${row.id}/unlock`).then(() => {
      ElMessage.success('解锁成功');
      loadData();
    });
  }).catch(() => {});
};

const batchUnlock = () => {
  if (selectedUsers.value.length === 0) {
    ElMessage.warning('请先选择要解锁的用户');
    return;
  }
  ElMessageBox.confirm(
    `确定解锁选中的 ${selectedUsers.value.length} 个用户吗？`,
    '批量解锁确认',
    {type: 'warning'}
  ).then(() => {
    const ids = selectedUsers.value.map(u => u.id);
    Request.put('/admin/users/batch/unlock', ids).then(() => {
      ElMessage.success('批量解锁成功');
      loadData();
    });
  }).catch(() => {});
};

const handleRoleTabChange = (name) => {
  search.value.role = tabRoleMap[name] || '';
  pageNum.value = 1;
  loadData();
};

const resetSearch = () => {
  search.value = {username: '', role: tabRoleMap[activeRoleTab.value] || '', status: ''};
  pageNum.value = 1;
  loadData();
};

const handlePageChange = (page) => {
  pageNum.value = page;
  loadData();
};

const roleLabel = (role) => {
  const map = {ADMIN: '系统管理员', TEACHER: '指导教师', STUDENT: '学生'};
  return map[role] || role;
};

const roleType = (role) => {
  if (role === 'ADMIN') return 'danger';
  if (role === 'TEACHER') return 'warning';
  return 'info';
};

const toggleStatus = (row) => {
  const newStatus = row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED';
  const action = row.status === 'ENABLED' ? '禁用' : '启用';
  ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？`, '提示', {type: 'warning'}).then(() => {
    Request.put(`/admin/user/${row.id}/status`, {status: newStatus}).then(() => {
      ElMessage.success(`${action}成功`);
      loadData();
    });
  }).catch(() => {});
};

const resetPassword = (row) => {
  ElMessageBox.prompt(`请输入您的管理员密码以确认重置`, '重置密码确认', {
    confirmButtonText: '确认重置',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPlaceholder: '请输入您的登录密码',
    type: 'warning',
    beforeClose: (action, instance, done) => {
      if (action === 'confirm') {
        if (!instance.inputValue) {
          instance.inputErrorMessage = '请输入管理员密码';
          return;
        }
        instance.confirmButtonLoading = true;
        Request.put(`/admin/user/${row.id}/reset-password`, {confirmPassword: instance.inputValue}).then(() => {
          done();
          ElMessage.success(`用户「${row.username}」密码已重置为 123456`);
        }).catch((err) => {
          instance.inputErrorMessage = err?.msg || '重置失败';
          instance.confirmButtonLoading = false;
        });
      } else {
        done();
      }
    }
  }).catch(() => {});
};

const deleteUser = (row) => {
  ElMessageBox.confirm(`确定删除用户「${row.username}」吗？在岗项目负责人将无法删除。`, '提示', {type: 'warning'}).then(() => {
    Request.delete(`/admin/user/${row.id}`).then(() => {
      ElMessage.success('删除成功');
      loadData();
    });
  }).catch(() => {});
};

const openAddDialog = () => {
  const role = tabRoleMap[activeRoleTab.value] || 'STUDENT';
  addForm.value = {username: '', password: '', nickname: '', role, studentNo: '', teacherNo: '', schoolId: null, majorId: null, classId: null, grade: '', email: ''};
  addDialogVisible.value = true;
};

const saveAdd = () => {
  // 学生/教师的学号、工号即登录账号
  if (addForm.value.role === 'STUDENT') {
    addForm.value.username = addForm.value.studentNo
  } else if (addForm.value.role === 'TEACHER') {
    addForm.value.username = addForm.value.teacherNo
  }
  addFormRef.value.validate(valid => {
    if (!valid) return;
    addLoading.value = true;
    Request.post('/admin/user', addForm.value).then(() => {
      ElMessage.success('新增成功');
      addDialogVisible.value = false;
      loadData();
    }).finally(() => {
      addLoading.value = false;
    });
  });
};

const loadSchoolOptions = () => {
  listAllSchools().then(res => { allSchools.value = res.data || []; });
  listAllMajors().then(res => { allMajorsAll.value = res.data || []; });
  listAllClasses().then(res => { allClassesAll.value = res.data || []; });
};

// 班级下拉展示「学院 / 专业 / 班级」，选择后带出学院、专业、年级
const classOptionLabel = (cls) => {
  const major = allMajorsAll.value.find(m => m.id === cls.majorId);
  const school = major ? allSchools.value.find(s => s.id === major.schoolId) : null;
  return [school?.name, major?.name, cls.name].filter(Boolean).join(' / ');
};

const onStudentClassChange = () => {
  const cls = allClassesAll.value.find(c => c.id === addForm.value.classId);
  const major = cls ? allMajorsAll.value.find(m => m.id === cls.majorId) : null;
  addForm.value.schoolId = major?.schoolId ?? null;
  addForm.value.majorId = cls?.majorId ?? null;
  addForm.value.grade = cls?.grade || '';
};

const openImportDialog = (type = 'STUDENT') => {
  importType.value = type;
  importForm.value = { schoolId: null, majorId: null, classId: null };
  importFileName.value = '';
  importFile.value = null;
  allMajors.value = [];
  allClasses.value = [];
  importDialogVisible.value = true;
};

const onImportSchoolChange = () => {
  importForm.value.majorId = null;
  importForm.value.classId = null;
  allMajors.value = [];
  allClasses.value = [];
  if (importForm.value.schoolId != null) {
    listAllMajors(importForm.value.schoolId).then(res => { allMajors.value = res.data || []; });
  }
};

const onImportMajorChange = () => {
  importForm.value.classId = null;
  allClasses.value = [];
  if (importForm.value.majorId != null) {
    listAllClasses(importForm.value.majorId).then(res => { allClasses.value = res.data || []; });
  }
};

const onImportFileChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    importFileName.value = file.name;
    importFile.value = file;
  } else {
    importFileName.value = '';
    importFile.value = null;
  }
};

const saveImport = () => {
  if (importType.value === 'STUDENT' && importForm.value.classId == null) {
    ElMessage.warning('请选择目标班级');
    return;
  }
  if (!importFile.value) {
    ElMessage.warning('请选择 Excel 文件');
    return;
  }
  importLoading.value = true;
  const request = importType.value === 'TEACHER'
      ? importTeachers(importFile.value)
      : importStudents(importForm.value.classId, importFile.value);
  request.then(res => {
    const data = res.data || {};
    const msg = `导入完成：成功 ${data.imported} 人，跳过 ${data.skipped} 人`;
    if (data.errors && data.errors.length) {
      ElMessageBox.alert(data.errors.slice(0, 10).join('<br/>'), msg, {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '知道了'
      }).catch(() => {});
    } else {
      ElMessage.success(msg);
    }
    importDialogVisible.value = false;
    loadData();
  }).finally(() => {
    importLoading.value = false;
  });
};
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: bold;
}

.role-tabs {
  margin-bottom: 16px;
}

.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.batch-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background-color: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.selected-tip {
  font-size: 13px;
  color: #606266;
  margin-right: 10px;
}

.selected-count {
  color: #409eff;
  font-weight: 600;
}

.batch-actions .el-button {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.no-permission {
  color: #909399;
  font-size: 12px;
}

.link-neutral {
  color: #303133;
}

.link-neutral:hover,
.link-neutral:focus {
  color: #606266;
}

.import-file {
  display: flex;
  align-items: center;
  gap: 10px;
}

.import-filename {
  color: #909399;
  font-size: 13px;
}
</style>
