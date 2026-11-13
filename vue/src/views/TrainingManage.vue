<template>
  <div>
    <el-card>
      <el-tabs v-model="activeTab">
        <!-- 学院 -->
        <el-tab-pane label="学院管理" name="school">
          <div class="toolbar">
            <el-input v-model="schoolSearch.keyword" placeholder="学院名称" clearable style="width: 200px" />
            <el-button type="primary" @click="loadSchools">查询</el-button>
            <el-button @click="resetSchoolSearch">重置</el-button>
            <el-button type="success" @click="openSchoolDialog()">新增学院</el-button>
          </div>
          <el-table :data="schoolList" border v-loading="loading">
            <el-table-column prop="name" label="学院名称" min-width="160" />
            <el-table-column prop="code" label="编码" width="100" />
            <el-table-column prop="dean" label="院长" width="100" />
            <el-table-column prop="contactPhone" label="联系电话" width="130" />
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">
                  {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openSchoolDialog(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="removeSchool(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination background layout="total, prev, pager, next" :total="schoolTotal"
                           :page-size="schoolSearch.pageSize" :current-page="schoolSearch.pageNum"
                           @current-change="p => { schoolSearch.pageNum = p; loadSchools(); }" />
          </div>
        </el-tab-pane>

        <!-- 专业 -->
        <el-tab-pane label="专业管理" name="major">
          <div class="toolbar">
            <el-select v-model="majorSearch.schoolId" placeholder="所属学院" clearable filterable style="width: 200px" @change="loadMajors">
              <el-option v-for="s in allSchools" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
            <el-input v-model="majorSearch.keyword" placeholder="专业名称" clearable style="width: 200px" />
            <el-button type="primary" @click="loadMajors">查询</el-button>
            <el-button @click="resetMajorSearch">重置</el-button>
            <el-button type="success" @click="openMajorDialog()">新增专业</el-button>
          </div>
          <el-table :data="majorList" border v-loading="loading">
            <el-table-column prop="name" label="专业名称" min-width="150" />
            <el-table-column prop="code" label="编码" width="100" />
            <el-table-column prop="schoolName" label="所属学院" width="150" />
            <el-table-column prop="degreeType" label="培养层次" width="100" />
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">
                  {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openMajorDialog(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="removeMajor(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination background layout="total, prev, pager, next" :total="majorTotal"
                           :page-size="majorSearch.pageSize" :current-page="majorSearch.pageNum"
                           @current-change="p => { majorSearch.pageNum = p; loadMajors(); }" />
          </div>
        </el-tab-pane>

        <!-- 班级 -->
        <el-tab-pane label="班级管理" name="clazz">
          <div class="toolbar">
            <el-select v-model="classSearch.majorId" placeholder="所属专业" clearable filterable style="width: 200px" @change="loadClasses">
              <el-option v-for="m in allMajors" :key="m.id" :label="m.name" :value="m.id" />
            </el-select>
            <el-input v-model="classSearch.keyword" placeholder="班级" clearable style="width: 200px" />
            <el-button type="primary" @click="loadClasses">查询</el-button>
            <el-button @click="resetClassSearch">重置</el-button>
            <el-button type="success" @click="openClassDialog()">新增班级</el-button>
          </div>
          <el-table :data="classList" border v-loading="loading">
            <el-table-column prop="name" label="班级" min-width="150" />
            <el-table-column prop="majorName" label="所属专业" width="150" />
            <el-table-column prop="grade" label="年级" width="90" />
            <el-table-column prop="classSize" label="班级人数" width="90" />
            <el-table-column prop="sortOrder" label="排序" width="70" />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="scope">
                <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'info'">
                  {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="scope">
                <el-button link type="primary" @click="openClassDialog(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="removeClass(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="pagination">
            <el-pagination background layout="total, prev, pager, next" :total="classTotal"
                           :page-size="classSearch.pageSize" :current-page="classSearch.pageNum"
                           @current-change="p => { classSearch.pageNum = p; loadClasses(); }" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 学院弹窗 -->
    <el-dialog v-model="schoolDialogVisible" :title="schoolForm.id ? '编辑学院' : '新增学院'" width="480px">
      <el-form :model="schoolForm" label-width="90px">
        <el-form-item label="学院名称" required><el-input v-model="schoolForm.name" /></el-form-item>
        <el-form-item label="学院编码"><el-input v-model="schoolForm.code" /></el-form-item>
        <el-form-item label="院长"><el-input v-model="schoolForm.dean" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="schoolForm.contactPhone" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="schoolForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="schoolForm.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="schoolForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schoolDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveSchool">确定</el-button>
      </template>
    </el-dialog>

    <!-- 专业弹窗 -->
    <el-dialog v-model="majorDialogVisible" :title="majorForm.id ? '编辑专业' : '新增专业'" width="480px">
      <el-form :model="majorForm" label-width="90px">
        <el-form-item label="所属学院" required>
          <el-select v-model="majorForm.schoolId" filterable style="width: 100%">
            <el-option v-for="s in allSchools" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业名称" required><el-input v-model="majorForm.name" /></el-form-item>
        <el-form-item label="专业编码"><el-input v-model="majorForm.code" /></el-form-item>
        <el-form-item label="培养层次"><el-input v-model="majorForm.degreeType" placeholder="如 本科 / 专科" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="majorForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="majorForm.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="简介"><el-input v-model="majorForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="majorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveMajor">确定</el-button>
      </template>
    </el-dialog>

    <!-- 班级弹窗 -->
    <el-dialog v-model="classDialogVisible" :title="classForm.id ? '编辑班级' : '新增班级'" width="480px">
      <el-form :model="classForm" label-width="90px">
        <el-form-item label="所属专业" required>
          <el-select v-model="classForm.majorId" filterable style="width: 100%">
            <el-option v-for="m in allMajors" :key="m.id" :label="m.name" :value="m.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="班级" required><el-input v-model="classForm.name" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="classForm.grade" placeholder="如 2023" /></el-form-item>
        <el-form-item label="班级人数"><el-input-number v-model="classForm.classSize" :min="0" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="classForm.sortOrder" :min="0" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="classForm.status" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="classDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveClass">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  listAllSchools, listSchools, createSchool, updateSchool, deleteSchool,
  listAllMajors, listMajors, createMajor, updateMajor, deleteMajor,
  listAllClasses, listClasses, createClass, updateClass, deleteClass
} from "@/api/training.js";

const activeTab = ref('school');
const loading = ref(false);

// ===== 学院 =====
const schoolList = ref([]);
const schoolTotal = ref(0);
const schoolSearch = ref({ pageNum: 1, pageSize: 10, keyword: '' });
const allSchools = ref([]);
const schoolDialogVisible = ref(false);
const schoolForm = ref({});

// ===== 专业 =====
const majorList = ref([]);
const majorTotal = ref(0);
const majorSearch = ref({ pageNum: 1, pageSize: 10, keyword: '', schoolId: null });
const allMajors = ref([]);
const majorDialogVisible = ref(false);
const majorForm = ref({});

// ===== 班级 =====
const classList = ref([]);
const classTotal = ref(0);
const classSearch = ref({ pageNum: 1, pageSize: 10, keyword: '', majorId: null });
const classDialogVisible = ref(false);
const classForm = ref({});

const loadSchools = () => {
  loading.value = true;
  listSchools({ pageNum: schoolSearch.value.pageNum, pageSize: schoolSearch.value.pageSize, keyword: schoolSearch.value.keyword })
    .then(res => {
      schoolList.value = res.data.records;
      schoolTotal.value = res.data.total;
    }).finally(() => loading.value = false);
};

const loadSchoolOptions = () => {
  listAllSchools().then(res => { allSchools.value = res.data; });
};

const loadMajorOptions = () => {
  listAllMajors().then(res => { allMajors.value = res.data; });
};

const loadMajors = () => {
  loading.value = true;
  listMajors({ pageNum: majorSearch.value.pageNum, pageSize: majorSearch.value.pageSize, keyword: majorSearch.value.keyword, schoolId: majorSearch.value.schoolId })
    .then(res => {
      majorList.value = res.data.records.map(r => ({ ...r, schoolName: schoolNameOf(r.schoolId) }));
      majorTotal.value = res.data.total;
    }).finally(() => loading.value = false);
};

const loadClasses = () => {
  loading.value = true;
  listClasses({ pageNum: classSearch.value.pageNum, pageSize: classSearch.value.pageSize, keyword: classSearch.value.keyword, majorId: classSearch.value.majorId })
    .then(res => {
      classList.value = res.data.records.map(r => ({ ...r, majorName: majorNameOf(r.majorId) }));
      classTotal.value = res.data.total;
    }).finally(() => loading.value = false);
};

const resetSchoolSearch = () => {
  schoolSearch.value.keyword = '';
  schoolSearch.value.pageNum = 1;
  loadSchools();
};

const resetMajorSearch = () => {
  majorSearch.value.schoolId = null;
  majorSearch.value.keyword = '';
  majorSearch.value.pageNum = 1;
  loadMajors();
};

const resetClassSearch = () => {
  classSearch.value.majorId = null;
  classSearch.value.keyword = '';
  classSearch.value.pageNum = 1;
  loadClasses();
};

const schoolNameOf = (id) => {
  const s = allSchools.value.find(x => x.id === id);
  return s ? s.name : '';
};
const majorNameOf = (id) => {
  const m = allMajors.value.find(x => x.id === id);
  return m ? m.name : '';
};

// 学院 CRUD
const openSchoolDialog = (row) => {
  schoolForm.value = row ? { ...row } : { name: '', code: '', dean: '', contactPhone: '', description: '', sortOrder: 0, status: 'ENABLED' };
  schoolDialogVisible.value = true;
};
const saveSchool = () => {
  if (!schoolForm.value.name) { ElMessage.warning('请输入学院名称'); return; }
  const done = () => { schoolDialogVisible.value = false; loadSchools(); loadSchoolOptions(); };
  if (schoolForm.value.id) {
    updateSchool(schoolForm.value.id, schoolForm.value).then(() => { ElMessage.success('保存成功'); done(); });
  } else {
    createSchool(schoolForm.value).then(() => { ElMessage.success('新增成功'); done(); });
  }
};
const removeSchool = (row) => {
  ElMessageBox.confirm(`确定删除学院「${row.name}」吗？`, '提示', { type: 'warning' }).then(() => {
    deleteSchool(row.id).then(() => { ElMessage.success('删除成功'); loadSchools(); loadSchoolOptions(); });
  }).catch(() => {});
};

// 专业 CRUD
const openMajorDialog = (row) => {
  majorForm.value = row ? { ...row } : { schoolId: null, name: '', code: '', degreeType: '', description: '', sortOrder: 0, status: 'ENABLED' };
  majorDialogVisible.value = true;
};
const saveMajor = () => {
  if (!majorForm.value.name) { ElMessage.warning('请输入专业名称'); return; }
  if (!majorForm.value.schoolId) { ElMessage.warning('请选择所属学院'); return; }
  const done = () => { majorDialogVisible.value = false; loadMajors(); loadMajorOptions(); };
  if (majorForm.value.id) {
    updateMajor(majorForm.value.id, majorForm.value).then(() => { ElMessage.success('保存成功'); done(); });
  } else {
    createMajor(majorForm.value).then(() => { ElMessage.success('新增成功'); done(); });
  }
};
const removeMajor = (row) => {
  ElMessageBox.confirm(`确定删除专业「${row.name}」吗？`, '提示', { type: 'warning' }).then(() => {
    deleteMajor(row.id).then(() => { ElMessage.success('删除成功'); loadMajors(); loadMajorOptions(); });
  }).catch(() => {});
};

// 班级 CRUD
const openClassDialog = (row) => {
  classForm.value = row ? { ...row } : { majorId: null, name: '', grade: '', classSize: 0, sortOrder: 0, status: 'ENABLED' };
  classDialogVisible.value = true;
};
const saveClass = () => {
  if (!classForm.value.name) { ElMessage.warning('请输入班级'); return; }
  if (!classForm.value.majorId) { ElMessage.warning('请选择所属专业'); return; }
  const done = () => { classDialogVisible.value = false; loadClasses(); };
  if (classForm.value.id) {
    updateClass(classForm.value.id, classForm.value).then(() => { ElMessage.success('保存成功'); done(); });
  } else {
    createClass(classForm.value).then(() => { ElMessage.success('新增成功'); done(); });
  }
};
const removeClass = (row) => {
  ElMessageBox.confirm(`确定删除班级「${row.name}」吗？`, '提示', { type: 'warning' }).then(() => {
    deleteClass(row.id).then(() => { ElMessage.success('删除成功'); loadClasses(); });
  }).catch(() => {});
};

onMounted(() => {
  Promise.all([listAllSchools(), listAllMajors()]).then(([schoolsRes, majorsRes]) => {
    allSchools.value = schoolsRes.data;
    allMajors.value = majorsRes.data;
    loadSchools();
    loadMajors();
    loadClasses();
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