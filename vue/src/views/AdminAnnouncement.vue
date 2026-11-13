<template>
  <div v-loading="loading" class="announcement-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>系统公告</span>
          <el-button type="primary" size="small" @click="openCreateDialog">
            <el-icon><Plus /></el-icon> 新建公告
          </el-button>
        </div>
      </template>

      <div class="filter-bar">
        <el-input v-model="filter.keyword" placeholder="搜索标题..." style="width: 200px" clearable />
        <el-select v-model="filter.status" placeholder="状态" clearable style="width: 120px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已下线" value="OFFLINE" />
        </el-select>
        <el-select v-model="filter.type" placeholder="类型" clearable style="width: 120px">
          <el-option label="通知" value="NOTICE" />
          <el-option label="重要" value="IMPORTANT" />
          <el-option label="维护" value="MAINTENANCE" />
        </el-select>
        <el-button type="primary" size="small" @click="loadData">查询</el-button>
        <el-button size="small" @click="resetFilter">重置</el-button>
      </div>

      <el-table :data="tableData" border style="margin-top: 15px">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip>
          <template #default="scope">
            <div style="display: flex; align-items: center; gap: 6px">
              <el-tag v-if="scope.row.isTop === 1" type="danger" size="small" effect="dark">置顶</el-tag>
              <span>{{ scope.row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="90">
          <template #default="scope">
            <el-tag :type="typeTagType(scope.row.type)" size="small">{{ typeLabel(scope.row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" width="120" />
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="viewDetail(scope.row)">查看</el-button>
            <el-button link type="primary" size="small" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-if="scope.row.status === 'DRAFT'" link type="success" size="small" @click="handlePublish(scope.row)">发布</el-button>
            <el-button v-if="scope.row.status === 'PUBLISHED'" link type="warning" size="small" @click="handleOffline(scope.row)">下线</el-button>
            <el-button link type="info" size="small" @click="handleToggleTop(scope.row)">
              {{ scope.row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          style="margin-top: 15px; justify-content: flex-end"
          @current-change="loadData"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑公告' : '新建公告'" width="650px">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="通知" value="NOTICE" />
            <el-option label="重要" value="IMPORTANT" />
            <el-option label="维护" value="MAINTENANCE" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio value="DRAFT">草稿</el-radio>
            <el-radio value="PUBLISHED">直接发布</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
              v-model="form.content"
              type="textarea"
              :rows="8"
              placeholder="请输入公告内容..."
              maxlength="2000"
              show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="公告详情" width="650px">
      <div class="detail-header">
        <h3>{{ currentRow.title }}</h3>
        <div class="detail-meta">
          <el-tag :type="typeTagType(currentRow.type)" size="small">{{ typeLabel(currentRow.type) }}</el-tag>
          <span v-if="currentRow.isTop === 1" class="top-badge">置顶</span>
          <span>发布人：{{ currentRow.publisherName }}</span>
          <span>发布时间：{{ currentRow.publishTime || currentRow.createdAt }}</span>
        </div>
      </div>
      <div class="detail-content">
        {{ currentRow.content }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
import {
  getAnnouncementList,
  createAnnouncement,
  updateAnnouncement,
  deleteAnnouncement,
  publishAnnouncement,
  offlineAnnouncement,
  toggleAnnouncementTop
} from '@/api/announcement.js';

const loading = ref(false);
const tableData = ref([]);
const pageNum = ref(1);
const pageSize = ref(10);
const total = ref(0);
const filter = reactive({ keyword: '', status: '', type: '' });

const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const formRef = ref(null);
const form = reactive({
  title: '',
  type: 'NOTICE',
  status: 'DRAFT',
  isTop: 0,
  content: ''
});
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
};

const detailVisible = ref(false);
const currentRow = reactive({});

const typeLabel = (type) => {
  const map = { NOTICE: '通知', IMPORTANT: '重要', MAINTENANCE: '维护' };
  return map[type] || type;
};

const typeTagType = (type) => {
  const map = { NOTICE: 'info', IMPORTANT: 'danger', MAINTENANCE: 'warning' };
  return map[type] || 'info';
};

const statusLabel = (status) => {
  const map = { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' };
  return map[status] || status;
};

const statusTagType = (status) => {
  const map = { DRAFT: 'info', PUBLISHED: 'success', OFFLINE: 'danger' };
  return map[status] || 'info';
};

const loadData = () => {
  loading.value = true;
  const params = {
    pageNum: pageNum.value,
    pageSize: pageSize.value,
    status: filter.status || undefined,
    type: filter.type || undefined,
    keyword: filter.keyword || undefined
  };
  getAnnouncementList(params).then(res => {
    tableData.value = res.data.records;
    total.value = res.data.total;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const resetFilter = () => {
  filter.keyword = '';
  filter.status = '';
  filter.type = '';
  pageNum.value = 1;
  loadData();
};

const openCreateDialog = () => {
  isEdit.value = false;
  editId.value = null;
  form.title = '';
  form.type = 'NOTICE';
  form.status = 'DRAFT';
  form.isTop = 0;
  form.content = '';
  dialogVisible.value = true;
};

const openEditDialog = (row) => {
  isEdit.value = true;
  editId.value = row.id;
  form.title = row.title;
  form.type = row.type;
  form.status = row.status;
  form.isTop = row.isTop;
  form.content = row.content;
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return;
    if (isEdit.value) {
      updateAnnouncement(editId.value, form).then(() => {
        ElMessage.success('更新成功');
        dialogVisible.value = false;
        loadData();
      });
    } else {
      createAnnouncement(form).then(() => {
        ElMessage.success('创建成功');
        dialogVisible.value = false;
        loadData();
      });
    }
  });
};

const handlePublish = (row) => {
  ElMessageBox.confirm(`确定发布公告「${row.title}」吗？`, '发布公告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'success'
  }).then(() => {
    publishAnnouncement(row.id).then(() => {
      ElMessage.success('发布成功');
      loadData();
    });
  }).catch(() => {});
};

const handleOffline = (row) => {
  ElMessageBox.confirm(`确定下线公告「${row.title}」吗？`, '下线公告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    offlineAnnouncement(row.id).then(() => {
      ElMessage.success('已下线');
      loadData();
    });
  }).catch(() => {});
};

const handleToggleTop = (row) => {
  toggleAnnouncementTop(row.id).then(() => {
    ElMessage.success(row.isTop === 1 ? '已取消置顶' : '已置顶');
    loadData();
  });
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除公告「${row.title}」吗？`, '删除公告', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'danger'
  }).then(() => {
    deleteAnnouncement(row.id).then(() => {
      ElMessage.success('删除成功');
      loadData();
    });
  }).catch(() => {});
};

const viewDetail = (row) => {
  Object.assign(currentRow, row);
  detailVisible.value = true;
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.announcement-page {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.filter-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
.detail-header {
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 15px;
  margin-bottom: 15px;
}
.detail-header h3 {
  margin: 0 0 10px 0;
  font-size: 18px;
}
.detail-meta {
  display: flex;
  gap: 15px;
  font-size: 13px;
  color: #909399;
  align-items: center;
}
.top-badge {
  color: #f56c6c;
  font-weight: bold;
}
.detail-content {
  line-height: 1.8;
  font-size: 14px;
  color: #303133;
  white-space: pre-wrap;
}
</style>
