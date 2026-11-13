<template>
  <div class="project-page">
    <div class="page-header">
      <div class="page-title">
        <el-icon><Collection /></el-icon>
        <span>{{ currentUser.role === 'STUDENT' ? '我的实训项目' : '项目管理' }}</span>
      </div>
      <div class="page-actions">
        <el-button v-if="canCreate" type="primary" @click="openCreateDialog">新建项目</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-change="handleTabChange" type="border-card" class="tab-container">
      <el-tab-pane :label="currentUser.role === 'STUDENT' ? '我的实训项目' : '项目列表'" name="list">
        <div class="search-bar">
          <el-input v-model="search.name" placeholder="搜索项目..." clearable style="width: 280px" />
          <el-select v-model="search.status" placeholder="项目状态" clearable style="width: 140px">
            <el-option label="未开始" value="NOT_STARTED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已延期" value="DELAYED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
          <el-radio-group v-if="currentUser.role !== 'STUDENT'" v-model="search.scope" size="small" @change="handleScopeChange">
            <el-radio-button label="未归档" value="active">未归档</el-radio-button>
            <el-radio-button label="已归档" value="archived">已归档</el-radio-button>
          </el-radio-group>
          <el-button type="primary" size="small" @click="loadData">查询</el-button>
          <el-button size="small" @click="resetSearch">重置</el-button>
          <el-tag v-if="courseFilterId" closable type="primary" size="small" @close="clearCourseFilter">
            课程：{{ courseNameOf(courseFilterId) || '已选课程' }}
          </el-tag>
        </div>

    <div v-if="tableData.length === 0" class="empty-tip">
      <el-icon class="empty-icon"><Collection /></el-icon>
      <div>{{ emptyText }}</div>
      <div class="empty-desc">{{ emptyDesc }}</div>
    </div>

    <div v-else class="project-cards">
      <div
          v-for="project in tableData"
          :key="project.id"
          class="project-card"
      >
        <div class="project-header">
          <div class="project-name">
            {{ project.name }}
            <span v-if="courseNameOf(project.courseId)" class="project-course">{{ courseNameOf(project.courseId) }}</span>
            <span v-if="classNameOf(project.classId)" class="project-class">（{{ classNameOf(project.classId) }}）</span>
          </div>
          <div class="project-tags">
            <el-tag v-if="project.archived" type="info" size="small">已归档</el-tag>
            <el-tag :type="projectStatusType(project.status)" size="small">{{ projectStatusLabel(project.status) }}</el-tag>
          </div>
        </div>

        <div v-if="project.description" class="project-description">{{ project.description }}</div>

        <div class="project-progress">
          <el-progress :percentage="project.progress || 0" :status="project.status === 'COMPLETED' ? 'success' : ''" :stroke-width="8" />
        </div>

        <div class="project-meta">
          <div class="meta-item">
            <el-icon class="meta-icon"><Calendar /></el-icon>
            <span>{{ project.startDate }} ~ {{ project.endDate }}</span>
          </div>
          <div class="meta-item">
            <el-icon class="meta-icon"><User /></el-icon>
            <span>{{ project.ownerName }}</span>
          </div>
          <div v-if="scopeText(project)" class="meta-item">
            <el-icon class="meta-icon"><School /></el-icon>
            <span>{{ scopeText(project) }}</span>
          </div>
        </div>

        <div class="project-stats">
          <span v-if="project.pendingTasks > 0" class="stat-badge pending">{{ project.pendingTasks }} 待办</span>
          <span class="stat-badge completed">{{ project.completedTasks }} 已完成</span>
          <span v-if="project.totalTasks > 0" class="stat-badge total">共 {{ project.totalTasks }} 个任务</span>
        </div>

        <div class="project-actions">
          <el-button @click="goDetail(project.id)" type="primary" size="small">进入项目</el-button>
          <el-button v-if="canCopy(project)" @click="openCopyDialog(project)" size="small">复制</el-button>
          <el-button v-if="canWrite(project)" @click="openEditDialog(project)" size="small">编辑</el-button>
          <el-dropdown v-if="canManage(project)" trigger="click" @command="cmd => handleProjectCommand(cmd, project)">
            <el-button size="small">更多</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="canDelete(project)" command="delete">删除项目</el-dropdown-item>
                <el-dropdown-item v-if="project.archived" command="unarchive">取消归档</el-dropdown-item>
                <el-dropdown-item v-else command="archive">归档项目</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
    </div>

    <div v-if="tableData.length > 0" class="pagination">
          <el-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="pageNum"
              @current-change="handlePageChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane v-if="canViewRecycleBin" label="项目回收站" name="recycle-bin">
        <div class="search-bar">
          <el-input v-model="recycleSearch.name" placeholder="搜索项目名称..." clearable style="width: 280px" />
          <el-select v-model="recycleSearch.status" placeholder="项目状态" clearable style="width: 140px">
            <el-option label="未开始" value="NOT_STARTED" />
            <el-option label="进行中" value="IN_PROGRESS" />
            <el-option label="已延期" value="DELAYED" />
            <el-option label="已完成" value="COMPLETED" />
          </el-select>
            <el-button type="primary" size="small" @click="loadRecycleBinData">查询</el-button>
            <el-button size="small" @click="resetRecycleSearch">重置</el-button>
            <el-button v-if="recycleBinData.length" size="small" type="danger" plain @click="clearRecycleBin">清空回收站</el-button>
          </div>

        <div v-if="recycleBinData.length === 0" class="empty-tip">
          <el-icon class="empty-icon"><Delete /></el-icon>
          <div>回收站暂无项目</div>
          <div class="empty-desc">删除的项目将在这里保留，可随时恢复</div>
        </div>

        <div v-else class="project-cards">
          <div
              v-for="project in recycleBinData"
              :key="project.id"
              class="project-card deleted-card"
          >
            <div class="project-header">
              <div class="project-name">{{ project.name }}</div>
              <el-tag :type="projectStatusType(project.status)" size="small">{{ projectStatusLabel(project.status) }}</el-tag>
            </div>

            <div v-if="project.description" class="project-description">{{ project.description }}</div>

            <div class="project-progress">
              <el-progress :percentage="project.progress || 0" :stroke-width="8" />
            </div>

            <div class="project-meta">
              <div class="meta-item">
                <el-icon class="meta-icon"><Calendar /></el-icon>
                <span>{{ project.startDate }} ~ {{ project.endDate }}</span>
              </div>
              <div class="meta-item">
                <el-icon class="meta-icon"><User /></el-icon>
                <span>指导教师：{{ project.teacherName || project.ownerName }}</span>
              </div>
            </div>

            <div class="project-stats">
              <span class="stat-badge completed">{{ project.completedTasks }} 已完成</span>
              <span class="stat-badge pending">{{ project.pendingTasks }} 待办</span>
              <span class="stat-badge deleted">{{ project.totalTasks }} 总任务</span>
            </div>

            <div class="card-actions">
              <el-button type="success" size="small" @click="restoreProject(project)">
                <el-icon><RefreshLeft /></el-icon> 恢复
              </el-button>
              <el-button type="danger" plain size="small" @click="purgeProject(project)">
                <el-icon><Delete /></el-icon> 彻底删除
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="recycleBinData.length > 0" class="pagination">
          <el-pagination
              v-model:current-page="recyclePageNum"
              v-model:page-size="recyclePageSize"
              :page-sizes="[8, 12, 24]"
              :total="recycleTotal"
              layout="total, sizes, prev, pager, next"
              @size-change="loadRecycleBinData"
              @current-change="handleRecyclePageChange"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑项目' : '新建项目'" width="500px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目简介">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入项目简介" />
        </el-form-item>
        <el-form-item label="实训类型">
          <el-select v-model="form.projectType" placeholder="请选择实训类型" style="width: 100%">
            <el-option v-for="item in projectTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="form.courseName" placeholder="请输入实训课程名称" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select-v2
              v-model="form.tags"
              :options="tagOptions"
              placeholder="请选择或输入标签"
              multiple
              allow-create
              filterable
              clearable
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="form.schoolId" placeholder="请选择学院" clearable filterable style="width: 100%" @change="onSchoolChange">
            <el-option v-for="s in allSchools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属专业">
          <el-select v-model="form.majorId" placeholder="请选择专业" clearable filterable style="width: 100%"
                     :disabled="!form.schoolId" @change="onMajorChange">
            <el-option v-for="m in majorOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属班级">
          <el-select v-model="form.classId" placeholder="请选择班级" clearable filterable style="width: 100%"
                     :disabled="!form.majorId" @change="onClassChange">
            <el-option v-for="c in classOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <div class="form-tip">选择班级后，创建时会自动加入该班全部在籍学生</div>
        </el-form-item>
        <el-form-item label="所属课程">
          <el-select v-model="form.courseId" placeholder="请选择教学班（课程）" clearable filterable style="width: 100%">
            <el-option v-for="c in courseOptions" :key="c.id" :label="courseLabel(c)" :value="c.id" />
          </el-select>
          <div class="form-tip">非必填，毕业设计、竞赛等项目可不选</div>
        </el-form-item>
        <el-form-item label="指导教师">
          <el-select v-model="form.teacherId" placeholder="请选择指导教师" clearable filterable style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.nickname || t.username" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止日期" prop="dateRange">
          <el-date-picker
              v-model="form.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="copyDialogVisible" title="复制项目" width="520px">
      <el-form :model="copyForm" :rules="copyRules" ref="copyFormRef" label-width="90px">
        <el-form-item label="项目名称" prop="name">
          <el-input v-model="copyForm.name" placeholder="请输入新项目名称" />
        </el-form-item>
        <el-form-item label="所属学院">
          <el-select v-model="copyForm.schoolId" placeholder="请选择学院" clearable filterable style="width: 100%" @change="onCopySchoolChange">
            <el-option v-for="s in allSchools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属专业">
          <el-select v-model="copyForm.majorId" placeholder="请选择专业" clearable filterable style="width: 100%"
                     :disabled="!copyForm.schoolId" @change="onCopyMajorChange">
            <el-option v-for="m in copyMajorOptions" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属班级">
          <el-select v-model="copyForm.classId" placeholder="请选择班级" clearable filterable style="width: 100%"
                     :disabled="!copyForm.majorId">
            <el-option v-for="c in copyClassOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指导教师">
          <el-select v-model="copyForm.teacherId" placeholder="请选择指导教师" clearable filterable style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.nickname || t.username" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起止日期" prop="dateRange">
          <el-date-picker
              v-model="copyForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="复制内容">
          <el-checkbox v-model="copyForm.copyMembers">项目成员</el-checkbox>
          <el-checkbox v-model="copyForm.copyTasks">任务与依赖</el-checkbox>
          <el-checkbox v-model="copyForm.copyGroups">学生分组</el-checkbox>
        </el-form-item>
      </el-form>
      <div class="copy-tip">任务会复制结构和负责人，状态统一重置为待办；班级、教师、日期可调整为新学期信息。</div>
      <template #footer>
        <el-button @click="copyDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="copySubmitting" @click="submitCopy">开始复制</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted, computed, watch} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import {Collection, Calendar, User, Delete, RefreshLeft, School} from '@element-plus/icons-vue';
import request from '@/utils/request.js';
import {listAllSchools, listAllMajors, listAllClasses, listAllCourses} from '@/api/training.js';
import {listTeachers} from '@/api/user.js';

const router = useRouter();
const route = useRoute();
const userStr = localStorage.getItem('code_user');
const currentUser = userStr ? JSON.parse(userStr) : {};
const isAdmin = currentUser.role === 'ADMIN';
const isTeacher = currentUser.role === 'TEACHER';
const canCreate = isTeacher;
const canViewRecycleBin = isAdmin || isTeacher;

// 从教师端「我的课程」进入时，按课程过滤项目列表
const courseFilterId = ref(route.query.courseId ? Number(route.query.courseId) : null);
watch(() => route.query.courseId, (val) => {
  const next = val ? Number(val) : null;
  if (next === courseFilterId.value) return;
  courseFilterId.value = next;
  pageNum.value = 1;
  loadData();
});
const clearCourseFilter = () => {
  courseFilterId.value = null;
  pageNum.value = 1;
  const {courseId, ...rest} = route.query;
  router.replace({path: route.path, query: rest});
  loadData();
};

const emptyText = computed(() => {
  if (isAdmin) return '系统中暂无项目';
  if (isTeacher) return '还没有创建实训项目';
  return '还没有加入任何实训项目';
});

const emptyDesc = computed(() => {
  if (isAdmin) return '指导教师创建项目后将在此处展示';
  if (isTeacher) return '点击右上角「新建项目」创建第一个实训项目';
  return '指导教师将你加入项目后将在此处展示';
});

const activeTab = ref('list');
const loading = ref(false);
const tableData = ref([]);
const total = ref(0);
const pageNum = ref(1);
const pageSize = ref(12);
const search = reactive({name: '', status: '', dateRange: [], scope: 'active'});

const recycleBinData = ref([]);
const recycleTotal = ref(0);
const recyclePageNum = ref(1);
const recyclePageSize = ref(12);
const recycleSearch = reactive({name: '', status: ''});

const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const formRef = ref(null);
const form = reactive({
  name: '',
  description: '',
  projectType: 'COURSE_TRAINING',
  courseName: '',
  teacherId: null,
  tags: [],
  schoolId: null,
  majorId: null,
  classId: null,
  courseId: null,
  dateRange: []
});
const rules = {
  name: [{required: true, message: '请输入项目名称', trigger: 'blur'}],
  dateRange: [{required: true, message: '请选择起止日期', trigger: 'change'}]
};
const tagOptions = [
  {label: 'Web', value: 'Web'},
  {label: 'App', value: 'App'},
  {label: '小程序', value: '小程序'}
];
const projectStatusLabel = (status) => {
  const map = {NOT_STARTED: '未开始', IN_PROGRESS: '进行中', DELAYED: '已延期', COMPLETED: '已完成'};
  return map[status] || status;
};

// ===== 组织架构（项目圈定范围）=====
const allSchools = ref([]);
const allMajors = ref([]);
const allClasses = ref([]);
const allCourses = ref([]);
const teachers = ref([]);
const projectTypeOptions = [
  {label: '课程实训', value: 'COURSE_TRAINING'},
  {label: '课程设计', value: 'COURSE_DESIGN'}
];
const loadTeachers = () => {
  listTeachers().then(res => { teachers.value = res.data || []; });
};
const majorOptions = computed(() => allMajors.value.filter(m => !form.schoolId || m.schoolId === form.schoolId));
const classOptions = computed(() => allClasses.value.filter(c => !form.majorId || c.majorId === form.majorId));
// 所属课程：按已选班级/指导教师过滤，未指定教师的课程也保留可选；已选中的课程始终保留
const courseOptions = computed(() => allCourses.value.filter(c =>
  c.id === form.courseId ||
  ((!form.classId || c.classId === form.classId) &&
   (!form.teacherId || c.teacherId == null || c.teacherId === form.teacherId))
));

// ===== 复制项目 =====
const copyDialogVisible = ref(false);
const copySourceId = ref(null);
const copyFormRef = ref(null);
const copySubmitting = ref(false);
const copyForm = reactive({
  name: '',
  schoolId: null,
  majorId: null,
  classId: null,
  teacherId: null,
  dateRange: [],
  copyMembers: true,
  copyTasks: true,
  copyGroups: true
});
const copyRules = {
  name: [{required: true, message: '请输入项目名称', trigger: 'blur'}],
  dateRange: [{required: true, message: '请选择起止日期', trigger: 'change'}]
};
const copyMajorOptions = computed(() => allMajors.value.filter(m => !copyForm.schoolId || m.schoolId === copyForm.schoolId));
const copyClassOptions = computed(() => allClasses.value.filter(c => !copyForm.majorId || c.majorId === copyForm.majorId));

const loadOrgOptions = () => {
  listAllSchools().then(res => { allSchools.value = res.data || []; });
  listAllMajors().then(res => { allMajors.value = res.data || []; });
  listAllClasses().then(res => { allClasses.value = res.data || []; });
  listAllCourses().then(res => { allCourses.value = res.data || []; });
};

const onSchoolChange = () => {
  form.majorId = null;
  form.classId = null;
  form.courseId = null;
};

const onMajorChange = () => {
  form.classId = null;
  form.courseId = null;
};

const onClassChange = () => {
  form.courseId = null;
};

const courseLabel = (course) => {
  return course.term ? `${course.name}（${course.term}）` : course.name;
};

const courseNameOf = (courseId) => {
  if (!courseId) return '';
  const course = allCourses.value.find(c => c.id === courseId);
  return course ? course.name : '';
};

const scopeText = (project) => {
  const parts = [];
  const school = allSchools.value.find(s => s.id === project.schoolId);
  const major = allMajors.value.find(m => m.id === project.majorId);
  const clazz = allClasses.value.find(c => c.id === project.classId);
  if (school) parts.push(school.name);
  if (major) parts.push(major.name);
  if (clazz) parts.push(clazz.name);
  return parts.join(' / ');
};

const classNameOf = (classId) => {
  if (!classId) return '';
  const clazz = allClasses.value.find(c => c.id === classId);
  return clazz ? clazz.name : '';
};

const projectStatusType = (status) => {
  const map = {NOT_STARTED: 'info', IN_PROGRESS: 'primary', DELAYED: 'danger', COMPLETED: 'success'};
  return map[status] || 'info';
};

const canWrite = (project) => {
  if (isAdmin) return false;
  return project.status !== 'COMPLETED' && !project.archived && project.teacherId === currentUser.id;
};

const canManage = (project) => {
  if (isAdmin) return false;
  return project.teacherId === currentUser.id;
};

// 已完成的项目往往是复制的来源，因此不限制项目状态
const canCopy = (project) => {
  if (isAdmin) return false;
  return project.teacherId === currentUser.id;
};

// 仅未开始的项目允许删除，进行中/已完成只能归档
const canDelete = (project) => {
  if (isAdmin) return false;
  return !project.archived && project.status === 'NOT_STARTED' && project.teacherId === currentUser.id;
};

const handleProjectCommand = (command, project) => {
  if (command === 'delete') {
    handleDelete(project);
  } else if (command === 'archive') {
    handleArchive(project, true);
  } else if (command === 'unarchive') {
    handleArchive(project, false);
  }
};

const handleArchive = (project, archived) => {
  const actionText = archived ? '归档' : '取消归档';
  ElMessageBox.confirm(
      archived
          ? `确定归档项目「${project.name}」吗？归档后默认不在项目列表显示，需切换到「已归档」查看并恢复。`
          : `确定取消归档项目「${project.name}」吗？取消后项目将回到项目列表。`,
      actionText + '项目',
      {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    request.put(`/project/${project.id}/archive`, null, {params: {archived}}).then(() => {
      ElMessage.success(`项目已${actionText}`);
      loadData();
    });
  }).catch(() => {});
};

const handleDelete = (project) => {
  ElMessageBox.confirm(
      `确定删除项目「${project.name}」吗？删除后项目将进入回收站，可以随时恢复。`,
      '删除项目',
      {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    request.delete('/project/' + project.id).then(() => {
      ElMessage.success('项目已删除');
      loadData();
    });
  }).catch(() => {});
};

const loadData = () => {
  loading.value = true;
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    name: search.name,
    status: search.status,
    startDateBegin: search.dateRange && search.dateRange[0],
    startDateEnd: search.dateRange && search.dateRange[1],
    archived: search.scope === 'archived' ? 1 : 0,
    courseId: courseFilterId.value || undefined
  };
  request.get('/project/list', {params}).then(res => {
    const page = res.data;
    tableData.value = page.records.map(item => {
      const totalTasks = item.totalTasks || item.totalTaskCount || 0;
      const completedTasks = item.completedTasks || item.completedTaskCount || 0;
      const progress = totalTasks === 0 ? 0 : (completedTasks * 100 / totalTasks);
      return {
        ...item,
        totalTasks,
        completedTasks,
        pendingTasks: totalTasks - completedTasks,
        progress: Math.round(progress)
      };
    });
    total.value = page.total;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const resetSearch = () => {
  search.name = '';
  search.status = '';
  search.dateRange = [];
  search.scope = 'active';
  pageNum.value = 1;
  if (courseFilterId.value) {
    clearCourseFilter();
    return;
  }
  loadData();
};

const handleScopeChange = () => {
  pageNum.value = 1;
  loadData();
};

const handlePageChange = (page) => {
  pageNum.value = page;
  loadData();
};

const openCreateDialog = () => {
  isEdit.value = false;
  editId.value = null;
  form.name = '';
  form.description = '';
  form.projectType = 'COURSE_TRAINING';
  form.courseName = '';
  form.teacherId = currentUser.id;
  form.tags = [];
  form.schoolId = null;
  form.majorId = null;
  form.classId = null;
  form.courseId = courseFilterId.value;
  form.dateRange = [];
  dialogVisible.value = true;
};

const openEditDialog = (project) => {
  isEdit.value = true;
  editId.value = project.id;
  form.name = project.name;
  form.description = project.description;
  form.projectType = project.projectType || 'COURSE_TRAINING';
  form.courseName = project.courseName || '';
  form.teacherId = project.teacherId || null;
  form.tags = project.tags ? project.tags.split(',') : [];
  form.schoolId = project.schoolId || null;
  form.majorId = project.majorId || null;
  form.classId = project.classId || null;
  form.courseId = project.courseId || null;
  form.dateRange = [project.startDate, project.endDate];
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value.validate(valid => {
    if (!valid) return;
    const payload = {
      name: form.name,
      description: form.description,
      projectType: form.projectType,
      courseName: form.courseName,
      teacherId: form.teacherId,
      tags: form.tags.join(','),
      schoolId: form.schoolId,
      majorId: form.majorId,
      classId: form.classId,
      courseId: form.courseId,
      startDate: form.dateRange[0],
      endDate: form.dateRange[1]
    };
    const promise = isEdit.value ? request.put('/project/' + editId.value, payload) : request.post('/project', payload);
    promise.then(res => {
      ElMessage.success(isEdit.value ? '编辑成功' : '创建成功');
      dialogVisible.value = false;
      if (!isEdit.value && res && res.data) {
        router.push('/project/' + res.data);
      } else {
        loadData();
      }
    }).catch(() => {
      loadData();
    });
  });
};

const openCopyDialog = (project) => {
  copySourceId.value = project.id;
  copyForm.name = `${project.name || ''}（副本）`;
  copyForm.schoolId = project.schoolId || null;
  copyForm.majorId = project.majorId || null;
  copyForm.classId = project.classId || null;
  copyForm.teacherId = project.teacherId || null;
  copyForm.dateRange = project.startDate && project.endDate ? [project.startDate, project.endDate] : [];
  copyForm.copyMembers = true;
  copyForm.copyTasks = true;
  copyForm.copyGroups = true;
  copyDialogVisible.value = true;
};

const onCopySchoolChange = () => {
  copyForm.majorId = null;
  copyForm.classId = null;
};

const onCopyMajorChange = () => {
  copyForm.classId = null;
};

const submitCopy = () => {
  copyFormRef.value.validate(valid => {
    if (!valid) return;
    copySubmitting.value = true;
    request.post(`/project/${copySourceId.value}/copy`, {
      name: copyForm.name,
      schoolId: copyForm.schoolId,
      majorId: copyForm.majorId,
      classId: copyForm.classId,
      teacherId: copyForm.teacherId,
      startDate: copyForm.dateRange[0],
      endDate: copyForm.dateRange[1],
      copyMembers: copyForm.copyMembers,
      copyTasks: copyForm.copyTasks,
      copyGroups: copyForm.copyGroups
    }).then(res => {
      ElMessage.success('复制成功');
      copyDialogVisible.value = false;
      loadData();
      if (res && res.data) {
        router.push('/project/' + res.data);
      }
    }).catch(() => {
      loadData();
    }).finally(() => {
      copySubmitting.value = false;
    });
  });
};

const goDetail = (id) => {
  router.push('/project/' + id + '/board');
};

const handleTabChange = (tab) => {
  if (tab === 'recycle-bin') {
    loadRecycleBinData();
  } else {
    loadData();
  }
};

const loadRecycleBinData = () => {
  loading.value = true;
  const params = {
    pageNum: recyclePageNum.value,
    pageSize: recyclePageSize.value,
    name: recycleSearch.name,
    status: recycleSearch.status
  };
  request.get('/project/recycle-bin/list', {params}).then(res => {
    const page = res.data;
    recycleBinData.value = (page.records || []).map(item => {
      const totalTasks = item.totalTasks || 0;
      const completedTasks = item.completedTasks || 0;
      const progress = totalTasks === 0 ? 0 : (completedTasks * 100 / totalTasks);
      return {
        ...item,
        totalTasks,
        completedTasks,
        pendingTasks: totalTasks - completedTasks,
        progress: Math.round(progress)
      };
    });
    recycleTotal.value = page.total || 0;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const resetRecycleSearch = () => {
  recycleSearch.name = '';
  recycleSearch.status = '';
  recyclePageNum.value = 1;
  loadRecycleBinData();
};

const handleRecyclePageChange = (page) => {
  recyclePageNum.value = page;
  loadRecycleBinData();
};

const restoreProject = (project) => {
  ElMessageBox.confirm(
      `确定恢复项目「${project.name}」吗？恢复后项目将重新出现在项目列表中。`,
      '恢复项目',
      {confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'}
  ).then(() => {
    request.put(`/project/${project.id}/restore`).then(() => {
      ElMessage.success('项目已恢复');
      loadRecycleBinData();
      loadData();
    });
  }).catch(() => {});
};

const purgeProject = (project) => {
  ElMessageBox.confirm(
      `确定彻底删除项目「${project.name}」吗？项目及其所有任务、成员、分组数据将被永久删除，无法恢复。`,
      '彻底删除项目',
      {confirmButtonText: '彻底删除', cancelButtonText: '取消', type: 'error'}
  ).then(() => {
    request.delete(`/project/${project.id}/purge`).then(() => {
      ElMessage.success('项目已彻底删除');
      loadRecycleBinData();
    });
  }).catch(() => {});
};

const clearRecycleBin = () => {
  ElMessageBox.confirm(
      '确定清空回收站吗？回收站内的项目及其任务、成员、分组数据将被永久删除，无法恢复。',
      '清空回收站',
      {confirmButtonText: '全部彻底删除', cancelButtonText: '取消', type: 'error'}
  ).then(() => {
    request.delete('/project/recycle-bin/clear').then(res => {
      ElMessage.success(`已彻底删除 ${res.data || 0} 个项目`);
      recyclePageNum.value = 1;
      loadRecycleBinData();
    });
  }).catch(() => {});
};

onMounted(() => {
  loadOrgOptions();
  loadTeachers();
  if (route.query.tab === 'recycle-bin' && canViewRecycleBin) {
    activeTab.value = 'recycle-bin';
    loadRecycleBinData();
  } else {
    loadData();
  }
});
</script>

<style scoped>
.project-page {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 20px;
  font-weight: bold;
}

.search-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.empty-tip {
  text-align: center;
  padding: 80px 0;
  color: #909399;
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.project-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
}

.project-card {
  background-color: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.2s;
}

.project-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  transform: translateY(-2px);
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.project-tags {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}

.project-name {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.project-class {
  font-size: 13px;
  font-weight: normal;
  color: #909399;
}

.project-course {
  margin-left: 6px;
  font-size: 13px;
  font-weight: normal;
  color: #909399;
}

.project-description {
  font-size: 14px;
  color: #606266;
  margin-bottom: 16px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-progress {
  margin-bottom: 16px;
}

.project-meta {
  display: flex;
  gap: 20px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #909399;
  flex-wrap: wrap;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.meta-icon {
  font-size: 14px;
}

.project-stats {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.stat-badge {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 12px;
}

.stat-badge.pending {
  background-color: #f0f9ff;
  color: #409EFF;
}

.stat-badge.completed {
  background-color: #f6ffed;
  color: #67c23a;
}

.stat-badge.total {
  background-color: #f5f7fa;
  color: #909399;
}

.project-actions {
  display: flex;
  gap: 8px;
}

.pagination {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
}

.tab-container {
  margin-top: 10px;
}

.deleted-card {
  border-left: 4px solid #f56c6c;
}

.empty-desc {
  font-size: 13px;
  margin-top: 8px;
}

.copy-tip {
  font-size: 12px;
  color: #909399;
  line-height: 1.6;
}

.stat-badge.deleted {
  background-color: #fef0f0;
  color: #f56c6c;
}

.card-actions {
  display: flex;
  gap: 10px;
}

.form-tip {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
}

@media (max-width: 768px) {
  .project-cards {
    grid-template-columns: 1fr;
  }
}
</style>