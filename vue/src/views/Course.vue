<template>
  <div>
    <el-card>
      <div class="toolbar">
        <el-input v-model="search.keyword" placeholder="课程名称" clearable style="width: 200px" />
        <el-select v-model="search.term" placeholder="学期" clearable filterable style="width: 200px">
          <el-option v-for="t in termOptions" :key="t" :label="t" :value="t" />
        </el-select>
        <el-select v-model="search.teacherId" placeholder="指导老师" clearable filterable style="width: 180px">
          <el-option v-for="t in teachers" :key="t.id" :label="t.nickname || t.username" :value="t.id" />
        </el-select>
        <el-button type="primary" @click="loadCourses">查询</el-button>
        <el-button @click="resetSearch">重置</el-button>
        <el-button type="success" @click="openDialog()">新增课程</el-button>
      </div>

      <el-table :data="courseList" border v-loading="loading">
        <el-table-column prop="name" label="课程名称" min-width="160" />
        <el-table-column prop="term" label="学期" width="140" />
        <el-table-column label="班级" width="180">
          <template #default="scope">{{ classNameOf(scope.row.classId) }}</template>
        </el-table-column>
        <el-table-column label="指导老师" width="140">
          <template #default="scope">
            <span v-if="teacherNameOf(scope.row.teacherId)">{{ teacherNameOf(scope.row.teacherId) }}</span>
            <span v-else style="color: #909399; display: inline-flex; align-items: center; gap: 4px">
              <el-icon><WarningFilled /></el-icon>
              <span>待分配</span>
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">
              {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDialog(scope.row)">编辑</el-button>
            <el-button link type="danger" @click="removeCourse(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination background layout="total, prev, pager, next" :total="total"
                       :page-size="search.pageSize" :current-page="search.pageNum"
                       @current-change="p => { search.pageNum = p; loadCourses(); }" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑课程' : '新增课程'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="课程名称" required>
          <el-input v-model="form.name" placeholder="如 Web 应用开发实训" />
        </el-form-item>
        <el-form-item label="学期">
          <el-select v-model="form.term" placeholder="请选择学期" clearable filterable style="width: 100%">
            <el-option v-for="t in termOptions" :key="t" :label="t" :value="t" />
          </el-select>
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
        <el-form-item label="班级" required>
          <el-select v-model="form.classId" placeholder="请选择班级" clearable filterable style="width: 100%"
                     :disabled="!form.majorId">
            <el-option v-for="c in classOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="指导老师">
          <el-select v-model="form.teacherId" placeholder="请选择指导老师" clearable filterable style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.nickname || t.username" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveCourse">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { WarningFilled } from "@element-plus/icons-vue";
import {
  listAllSchools, listAllMajors, listAllClasses,
  listCourses, createCourse, updateCourse, deleteCourse
} from "@/api/training.js";
import { listTeachers } from "@/api/user.js";

const loading = ref(false);
const courseList = ref([]);
const total = ref(0);
const search = ref({ pageNum: 1, pageSize: 10, keyword: '', term: '', teacherId: null });

const allSchools = ref([]);
const allMajors = ref([]);
const allClasses = ref([]);
const teachers = ref([]);

// 学期选项：按学年倒序生成，如「2026-2027 第2学期」
const termOptions = computed(() => {
  const now = new Date();
  const startYear = now.getMonth() + 1 >= 8 ? now.getFullYear() : now.getFullYear() - 1;
  const options = [];
  for (let year = startYear; year >= startYear - 3; year--) {
    options.push(`${year}-${year + 1} 第2学期`);
    options.push(`${year}-${year + 1} 第1学期`);
  }
  return options;
});

const dialogVisible = ref(false);
const form = ref({});

const majorOptions = computed(() => allMajors.value.filter(m => !form.value.schoolId || m.schoolId === form.value.schoolId));
const classOptions = computed(() => allClasses.value.filter(c => !form.value.majorId || c.majorId === form.value.majorId));

const classNameOf = (classId) => {
  const c = allClasses.value.find(x => x.id === classId);
  return c ? c.name : '';
};

const teacherNameOf = (teacherId) => {
  const t = teachers.value.find(x => x.id === teacherId);
  return t ? (t.nickname || t.username) : '';
};

const resetSearch = () => {
  search.value.keyword = '';
  search.value.term = '';
  search.value.teacherId = null;
  search.value.pageNum = 1;
  loadCourses();
};

const loadCourses = () => {
  loading.value = true;
  listCourses({ ...search.value })
    .then(res => {
      courseList.value = res.data.records;
      total.value = res.data.total;
    }).finally(() => loading.value = false);
};

const openDialog = (row) => {
  if (row) {
    const clazz = allClasses.value.find(c => c.id === row.classId);
    const major = clazz ? allMajors.value.find(m => m.id === clazz.majorId) : null;
    form.value = {
      ...row,
      schoolId: major ? major.schoolId : null,
      majorId: major ? major.id : null
    };
  } else {
    form.value = { name: '', term: '', schoolId: null, majorId: null, classId: null, teacherId: null, status: 'ENABLED' };
  }
  dialogVisible.value = true;
};

const onSchoolChange = () => {
  form.value.majorId = null;
  form.value.classId = null;
};

const onMajorChange = () => {
  form.value.classId = null;
};

const saveCourse = () => {
  if (!form.value.name) { ElMessage.warning('请输入课程名称'); return; }
  if (!form.value.classId) { ElMessage.warning('请选择班级'); return; }
  const payload = {
    name: form.value.name,
    term: form.value.term,
    teacherId: form.value.teacherId,
    classId: form.value.classId,
    status: form.value.status
  };
  const done = () => { dialogVisible.value = false; loadCourses(); };
  if (form.value.id) {
    updateCourse(form.value.id, payload).then(() => { ElMessage.success('保存成功'); done(); });
  } else {
    createCourse(payload).then(() => { ElMessage.success('新增成功'); done(); });
  }
};

const removeCourse = (row) => {
  ElMessageBox.confirm(`确定删除课程「${row.name}」吗？`, '提示', { type: 'warning' }).then(() => {
    deleteCourse(row.id).then(() => { ElMessage.success('删除成功'); loadCourses(); });
  }).catch(() => {});
};

onMounted(() => {
  Promise.all([listAllSchools(), listAllMajors(), listAllClasses(), listTeachers()]).then(([schoolsRes, majorsRes, classesRes, teachersRes]) => {
    allSchools.value = schoolsRes.data || [];
    allMajors.value = majorsRes.data || [];
    allClasses.value = classesRes.data || [];
    teachers.value = teachersRes.data || [];
    loadCourses();
  });
});
</script>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
