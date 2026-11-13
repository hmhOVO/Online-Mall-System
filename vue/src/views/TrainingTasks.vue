<template>
  <div v-loading="loading" class="training-tasks">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">实训任务总览</h2>
        <p class="page-subtitle">跨项目查看实训所有任务，支持多维度筛选与汇总分析</p>
      </div>
      <div class="header-right">
        <el-select
          v-model="filter.projectIds"
          multiple
          filterable
          collapse-tags
          placeholder="选择项目（默认全部）"
          style="width: 280px; margin-right: 12px"
          :loading="loadingProjects"
          @change="loadTasks"
        >
          <el-option v-for="p in projectOptions" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button value="board">看板</el-radio-button>
          <el-radio-button value="list">列表</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- 统计摘要 -->
    <div class="stats-strip">
      <div class="stat-item">
        <div class="stat-label">总任务</div>
        <div class="stat-number">{{ summary.total }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">进行中</div>
        <div class="stat-number">{{ summary.inProgress }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">已完成</div>
        <div class="stat-number">{{ summary.done }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">已逾期</div>
        <div class="stat-number" :class="{ 'is-alert': summary.overdue > 0 }">{{ summary.overdue }}</div>
      </div>
      <div class="stat-item">
        <div class="stat-label">未认领</div>
        <div class="stat-number">{{ summary.unclaimed }}</div>
      </div>
    </div>

    <el-card class="filter-card" shadow="never">
      <div class="filter-row">
        <el-select v-model="filter.statuses" multiple collapse-tags placeholder="状态" style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filter.priorities" multiple collapse-tags placeholder="优先级" style="width: 180px">
          <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filter.assigneeId" clearable placeholder="负责人" style="width: 160px">
          <el-option v-for="item in assigneeOptions" :key="item.userId" :label="item.nickname || item.username" :value="item.userId" />
        </el-select>
        <el-input
          v-model="filter.keyword"
          placeholder="搜索任务标题或描述"
          style="width: 220px"
          prefix-icon="Search"
          clearable
          @keyup.enter="loadTasks"
        />
        <el-checkbox v-model="filter.onlyOverdue">仅显示逾期</el-checkbox>
        <el-button size="small" :type="filter.onlyPending ? 'primary' : 'default'" @click="togglePending">
          待审核 {{ pendingCount }}
        </el-button>
        <el-button size="small" @click="resetFilter">重置</el-button>
        <el-button size="small" type="primary" @click="loadTasks">查询</el-button>
      </div>
    </el-card>

    <!-- 项目分布图表 -->
    <el-card class="chart-card" shadow="never">
      <template #header>
        <div class="card-header">
          <span>项目分布</span>
        </div>
      </template>
      <div ref="projectChartRef" class="project-chart"></div>
      <el-empty v-if="projectChartData.length === 0" description="暂无项目分布数据" />
    </el-card>

    <!-- 看板视图 -->
    <div v-if="viewMode === 'board'" class="kanban">
      <div v-for="col in columns" :key="col.status" class="kanban-col">
        <div class="col-header" :class="'col-' + col.status.toLowerCase()">
          <span>{{ col.label }}</span>
          <span class="col-count">{{ boardLists[col.status].length }}</span>
        </div>
        <div class="col-body">
          <div
            v-for="task in boardLists[col.status]"
            :key="task.id"
            class="task-card"
            :class="{ overdue: task.overdue, unclaimed: !task.assigneeId, blocked: task.status === 'BLOCKED' }"
            @click="goTaskBoard(task)"
          >
            <div class="card-project" @click.stop="filterByProject(task.projectId)">{{ task.projectName || '-' }}</div>
            <div class="card-title">{{ task.title }}</div>
            <div class="card-meta">
              <el-tooltip :content="task.assigneeName || '待认领'" placement="top">
                <el-avatar :size="24" class="card-avatar" :class="{ unclaimed: !task.assigneeId }">{{ avatarText(task.assigneeName) }}</el-avatar>
              </el-tooltip>
              <el-tag :type="priorityType(task.priority)" size="small">{{ priorityLabel(task.priority) }}</el-tag>
            </div>
            <el-progress :percentage="task.progress || 0" :stroke-width="6" :show-text="false" />
            <div v-if="task.overdue" class="overdue-tag">已逾期</div>
            <div v-if="task.status === 'BLOCKED'" class="block-reason">
              <el-icon><Warning /></el-icon>
              <span>已阻塞</span>
            </div>
          </div>
          <el-empty v-if="boardLists[col.status].length === 0" description=" " :image-size="60" />
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <el-card v-else class="list-card" shadow="never">
      <div v-if="selectedTasks.length" class="batch-bar">
        <span class="batch-count">已选 {{ selectedTasks.length }} 条审核中任务，涉及 {{ selectedProjectCount }} 个项目</span>
        <el-button size="small" type="primary" @click="openReviewDialog(selectedTasks, 'approve')">批量通过</el-button>
        <el-button size="small" type="danger" plain @click="openReviewDialog(selectedTasks, 'reject')">批量驳回</el-button>
        <el-button size="small" link @click="clearSelection">取消选择</el-button>
      </div>
      <el-table ref="tableRef" :data="filteredTasks" stripe style="width: 100%" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="46" :selectable="row => row.status === 'REVIEW'" />
        <el-table-column label="任务标题" min-width="200">
          <template #default="scope">
            <el-link type="primary" underline="never" @click="goTaskBoard(scope.row)">
              <span :style="{ color: scope.row.overdue ? '#f56c6c' : '' }">{{ scope.row.title }}</span>
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="所属项目" min-width="150">
          <template #default="scope">
            <el-link type="primary" @click.stop="goProject(scope.row.projectId)">{{ scope.row.projectName || '-' }}</el-link>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="100">
          <template #default="scope">
            <el-tag :type="priorityType(scope.row.priority)" size="small">{{ priorityLabel(scope.row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" width="120" />
        <el-table-column label="进度" width="140">
          <template #default="scope">
            <el-progress :percentage="scope.row.progress || 0" :stroke-width="6" />
          </template>
        </el-table-column>
        <el-table-column prop="endDate" label="截止日期" width="120" />
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="scope">
            <el-button link type="primary" size="small" @click="openDetail(scope.row)">详情</el-button>
            <template v-if="scope.row.status === 'REVIEW'">
              <el-button link type="success" size="small" @click="openReviewDialog([scope.row], 'approve')">通过</el-button>
              <el-button link type="danger" size="small" @click="openReviewDialog([scope.row], 'reject')">驳回</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && filteredTasks.length === 0" description="暂无相关任务" />
    </el-card>

    <!-- 任务详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="任务详情" size="600px" destroy-on-close>
      <div v-if="currentTask.id" class="drawer-body">
        <div class="drawer-actions">
          <el-button type="primary" size="small" @click="goTaskBoard(currentTask)">进入项目看板处理</el-button>
          <template v-if="currentTask.status === 'REVIEW'">
            <el-button type="success" size="small" @click="openReviewDialog([currentTask], 'approve')">审核通过</el-button>
            <el-button type="danger" size="small" plain @click="openReviewDialog([currentTask], 'reject')">驳回</el-button>
          </template>
        </div>

        <el-alert
          v-if="currentTask.status === 'BLOCKED'"
          title="任务已阻塞"
          description="任务处于阻塞状态，请先解除阻塞后再继续处理"
          type="error"
          :closable="false"
          style="margin-bottom: 15px"
        />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">{{ currentTask.title }}</el-descriptions-item>
          <el-descriptions-item label="所属项目" :span="2">
            <el-link type="primary" @click="goProject(currentTask.projectId)">{{ currentTask.projectName || '-' }}</el-link>
          </el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentTask.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentTask.status)">{{ statusLabel(currentTask.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="priorityType(currentTask.priority)">{{ priorityLabel(currentTask.priority) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ currentTask.assigneeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ currentTask.creatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ currentTask.reviewerName || '未指定' }}</el-descriptions-item>
          <el-descriptions-item label="起止日期" :span="2">{{ currentTask.startDate || '-' }} 至 {{ currentTask.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划学时">{{ currentTask.plannedHours || 0 }} 学时</el-descriptions-item>
          <el-descriptions-item label="实际学时">{{ currentTask.actualHours || 0 }} 学时</el-descriptions-item>
          <el-descriptions-item label="完成时间" :span="2">{{ currentTask.completedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentTask.dependencies && currentTask.dependencies.length" class="section">
          <div class="section-title">前置依赖</div>
          <el-table :data="currentTask.dependencies" size="small" border>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="负责人" width="120" />
          </el-table>
        </div>

        <div v-if="currentTask.children && currentTask.children.length" class="section">
          <div class="section-title">子任务</div>
          <el-table :data="currentTask.children" size="small" border>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="assigneeName" label="负责人" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>
    </el-drawer>

    <!-- 审核弹窗 -->
    <el-dialog
      v-model="reviewDialogVisible"
      :title="reviewMode === 'approve' ? '审核通过' : '审核驳回'"
      width="460px"
      destroy-on-close
    >
      <div class="review-tip">共 {{ reviewTargets.length }} 条任务，按项目分组如下：</div>
      <div class="review-targets-list">
        <div v-for="group in groupedReviewTargets" :key="group.projectName" class="project-group">
          <div class="project-group-title">
            <el-icon><FolderOpened /></el-icon>
            <span>{{ group.projectName }}</span>
          </div>
          <div v-for="task in group.tasks" :key="task.id" class="task-item-row">
            <span class="task-title">{{ task.title }}</span>
            <span v-if="task.assigneeName" class="task-assignee">· {{ task.assigneeName }}</span>
          </div>
        </div>
      </div>
      <el-form v-if="reviewMode === 'approve'" label-width="70px">
        <el-form-item v-if="reviewTargets.length === 1" label="评分">
          <el-input-number v-model="reviewForm.score" :min="0" :max="100" :precision="1" placeholder="可不填" />
        </el-form-item>
        <el-form-item v-else label="说明">
          <span class="form-tip">批量通过不自动评分，需要打分请单独审核</span>
        </el-form-item>
        <div v-if="reviewTargets.length === 1" class="form-tip form-tip-indent">留空则只通过审核，不记录分数</div>
      </el-form>
      <el-form v-else label-width="70px">
        <el-form-item label="驳回原因" required>
          <el-input
            v-model="reviewForm.comment"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请填写驳回原因，将通知任务负责人"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialogVisible = false">取消</el-button>
        <el-button :type="reviewMode === 'approve' ? 'success' : 'danger'" :loading="reviewing" @click="submitReview">
          {{ reviewMode === 'approve' ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  Warning, FolderOpened
} from '@element-plus/icons-vue';
import { listTrainingTasks, getTaskDetail, approveReview, rejectReview } from '@/api/task.js';
import { listProjects } from '@/api/project.js';

const router = useRouter();
const route = useRoute();

const loading = ref(false);
const loadingProjects = ref(false);
const projectOptions = ref([]);
const taskList = ref([]);
const viewMode = ref('board');

const filter = reactive({
  projectIds: [],
  statuses: [],
  priorities: [],
  assigneeId: null,
  keyword: '',
  onlyOverdue: false,
  onlyPending: false
});

const statusOptions = [
  { label: '待办', value: 'TODO' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '审核中', value: 'REVIEW' },
  { label: '已完成', value: 'DONE' }
];
const priorityOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' }
];
const columns = [
  { status: 'TODO', label: '待办' },
  { status: 'IN_PROGRESS', label: '进行中' },
  { status: 'REVIEW', label: '审核中' },
  { status: 'DONE', label: '已完成' }
];

const boardLists = reactive({ TODO: [], IN_PROGRESS: [], REVIEW: [], DONE: [] });

const drawerVisible = ref(false);
const currentTask = reactive({});

const tableRef = ref(null);
const selectedTasks = ref([]);
const reviewDialogVisible = ref(false);
const reviewMode = ref('approve');
const reviewTargets = ref([]);
const reviewing = ref(false);
const reviewForm = reactive({ score: null, comment: '' });

const selectedProjectCount = computed(() => {
  const ids = new Set(selectedTasks.value.map(t => t.projectId ?? 'none'));
  return ids.size;
});

const groupedReviewTargets = computed(() => {
  const map = new Map();
  reviewTargets.value.forEach(task => {
    const projectName = task.projectName || '未分配项目';
    if (!map.has(projectName)) {
      map.set(projectName, []);
    }
    map.get(projectName).push(task);
  });
  return Array.from(map.entries()).map(([projectName, tasks]) => ({ projectName, tasks }));
});

const projectChartRef = ref(null);
let projectChartInstance = null;

const summary = computed(() => {
  const list = filteredTasks.value;
  return {
    total: list.length,
    inProgress: list.filter(t => t.status === 'IN_PROGRESS' || t.status === 'BLOCKED').length,
    done: list.filter(t => t.status === 'DONE').length,
    overdue: list.filter(t => t.overdue).length,
    unclaimed: list.filter(t => !t.assigneeId).length
  };
});

const pendingCount = computed(() => taskList.value.filter(t => t.status === 'REVIEW').length);

const reviewTitles = computed(() => {
  const titles = reviewTargets.value.slice(0, 3).map(t => t.title).join('、');
  return reviewTargets.value.length > 3 ? `${titles} 等 ${reviewTargets.value.length} 条` : titles;
});

const assigneeOptions = computed(() => {
  const map = new Map();
  taskList.value.forEach(t => {
    if (t.assigneeId && !map.has(t.assigneeId)) {
      map.set(t.assigneeId, { userId: t.assigneeId, nickname: t.assigneeName });
    }
  });
  return Array.from(map.values());
});

const filteredTasks = computed(() => {
  return taskList.value.filter(task => {
    if (filter.statuses.length && !filter.statuses.includes(task.status === 'BLOCKED' ? 'IN_PROGRESS' : task.status)) return false;
    if (filter.priorities.length && !filter.priorities.includes(task.priority)) return false;
    if (filter.assigneeId != null) {
      if (filter.assigneeId === -1) {
        if (task.assigneeId) return false;
      } else if (task.assigneeId !== filter.assigneeId) {
        return false;
      }
    }
    if (filter.onlyOverdue && !task.overdue) return false;
    if (filter.onlyPending && task.status !== 'REVIEW') return false;
    if (filter.keyword) {
      const kw = filter.keyword.toLowerCase();
      const title = (task.title || '').toLowerCase();
      const desc = (task.description || '').toLowerCase();
      if (!title.includes(kw) && !desc.includes(kw)) return false;
    }
    return true;
  });
});

const projectChartData = computed(() => {
  const map = new Map();
  filteredTasks.value.forEach(t => {
    const name = t.projectName || '未分配项目';
    map.set(name, (map.get(name) || 0) + 1);
  });
  return Array.from(map.entries()).map(([name, value]) => ({ name, value }));
});

const statusLabel = (status) => {
  const map = { TODO: '待办', IN_PROGRESS: '进行中', BLOCKED: '已阻塞', REVIEW: '审核中', DONE: '已完成' };
  return map[status] || status;
};
const statusType = (status) => {
  const map = { TODO: 'info', IN_PROGRESS: 'primary', BLOCKED: 'danger', REVIEW: 'warning', DONE: 'success' };
  return map[status] || 'info';
};
const priorityLabel = (priority) => {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' };
  return map[priority] || priority;
};
const priorityType = (priority) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' };
  return map[priority] || 'info';
};
const avatarText = (name) => (name ? name.charAt(0).toUpperCase() : '?');

const loadProjects = () => {
  loadingProjects.value = true;
  listProjects({ pageNum: 1, pageSize: 1000 }).then(res => {
    projectOptions.value = res.data.records || [];
    const qProjectId = route.query.projectId;
    if (qProjectId != null && qProjectId !== '') {
      const parsed = Number(qProjectId);
      if (!isNaN(parsed) && projectOptions.value.some(p => p.id === parsed)) {
        filter.projectIds = [parsed];
        loadTasks();
      }
    }
  }).catch(() => {
    ElMessage.error('加载项目列表失败');
  }).finally(() => {
    loadingProjects.value = false;
  });
};

const loadTasks = () => {
  loading.value = true;
  const params = {
    projectIds: filter.projectIds.length ? filter.projectIds : undefined,
    statuses: filter.statuses.length ? filter.statuses : undefined,
    priorities: filter.priorities.length ? filter.priorities : undefined,
    assigneeId: filter.assigneeId,
    keyword: filter.keyword.trim() || undefined
  };
  listTrainingTasks(params).then(res => {
    taskList.value = res.data || [];
    selectedTasks.value = [];
    if (tableRef.value) tableRef.value.clearSelection();
    buildBoardLists();
    loading.value = false;
    nextTick(() => {
      renderProjectChart();
    });
  }).catch(() => {
    loading.value = false;
  });
};

const buildBoardLists = () => {
  columns.forEach(col => (boardLists[col.status] = []));
  filteredTasks.value.forEach(task => {
    const displayStatus = task.status === 'BLOCKED' ? 'IN_PROGRESS' : task.status;
    boardLists[displayStatus].push(task);
  });
};

const renderProjectChart = () => {
  if (!projectChartRef.value) return;
  if (!projectChartInstance) {
    projectChartInstance = echarts.init(projectChartRef.value);
  }
  const data = projectChartData.value;
  projectChartInstance.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left', type: 'scroll' },
    color: ['#409EFF', '#67C23A', '#E6A23C', '#F56C6C', '#909399', '#9254DE', '#13C2C2'],
    series: [
      {
        name: '项目分布',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
        label: { show: true, formatter: '{b}\n{c} ({d}%)' },
        emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' } },
        data
      }
    ]
  }, true);
};

const resetFilter = () => {
  filter.projectIds = [];
  filter.statuses = [];
  filter.priorities = [];
  filter.assigneeId = null;
  filter.keyword = '';
  filter.onlyOverdue = false;
  filter.onlyPending = false;
  loadTasks();
};

const togglePending = () => {
  filter.onlyPending = !filter.onlyPending;
};

const handleSelectionChange = (rows) => {
  selectedTasks.value = rows;
};

const clearSelection = () => {
  if (tableRef.value) tableRef.value.clearSelection();
  selectedTasks.value = [];
};

const openReviewDialog = (tasks, mode) => {
  if (!tasks || !tasks.length) return;
  reviewMode.value = mode;
  reviewTargets.value = tasks.slice();
  reviewForm.score = null;
  reviewForm.comment = '';
  reviewDialogVisible.value = true;
};

const submitReview = () => {
  const tasks = reviewTargets.value;
  if (!tasks.length) return;
  if (reviewMode.value === 'reject' && !reviewForm.comment.trim()) {
    ElMessage.warning('请填写驳回原因');
    return;
  }
  const score = reviewMode.value === 'approve' && tasks.length === 1 ? reviewForm.score : null;
  reviewing.value = true;
  const requests = tasks.map(task => reviewMode.value === 'approve'
    ? approveReview(task.id, score)
    : rejectReview(task.id, reviewForm.comment.trim()));
  Promise.allSettled(requests).then(results => {
    reviewing.value = false;
    const succeeded = results.filter(item => item.status === 'fulfilled').length;
    const failed = results.length - succeeded;
    if (succeeded) ElMessage.success(`已${reviewMode.value === 'approve' ? '通过' : '驳回'} ${succeeded} 条任务`);
    if (failed) ElMessage.warning(`${failed} 条任务处理失败`);
    reviewDialogVisible.value = false;
    drawerVisible.value = false;
    selectedTasks.value = [];
    loadTasks();
  });
};

const openDetail = (row) => {
  getTaskDetail(row.id).then(res => {
    Object.assign(currentTask, res.data);
    drawerVisible.value = true;
  });
};

const goProject = (projectId) => {
  if (!projectId) return;
  router.push(`/project/${projectId}`);
};

const goTaskBoard = (task) => {
  if (!task.projectId) return;
  window.open(`/project/${task.projectId}/board?taskId=${task.id}`, '_blank');
};

const filterByProject = (projectId) => {
  if (!projectId) return;
  filter.projectIds = [projectId];
  loadTasks();
};

watch([() => filter.statuses, () => filter.priorities, () => filter.assigneeId, () => filter.onlyOverdue, () => filter.onlyPending, () => filter.keyword], () => {
  buildBoardLists();
  nextTick(() => renderProjectChart());
}, { deep: true });

const handleResize = () => {
  projectChartInstance && projectChartInstance.resize();
};

onMounted(() => {
  loadProjects();
  const qAssigneeId = route.query.assigneeId;
  if (qAssigneeId != null && qAssigneeId !== '') {
    const parsed = Number(qAssigneeId);
    if (!isNaN(parsed)) {
      filter.assigneeId = parsed;
    }
  }
  if (route.query.pending === '1') {
    filter.onlyPending = true;
    viewMode.value = 'list';
  }
  loadTasks();
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
  if (projectChartInstance) {
    projectChartInstance.dispose();
    projectChartInstance = null;
  }
});
</script>

<style scoped>
.training-tasks {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.header-left {
  flex: 1;
}
.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}
.page-subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 14px;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.stats-strip {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 16px;
}
.stat-item {
  padding: 16px 20px;
  border-right: 1px solid #ebeef5;
}
.stat-item:last-child {
  border-right: none;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 6px;
}
.stat-number {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}
.stat-number.is-alert {
  color: #cf1322;
}
.filter-card {
  margin-bottom: 16px;
}
.filter-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.chart-card {
  margin-bottom: 16px;
}
.card-header {
  font-weight: 600;
}
.project-chart {
  width: 100%;
  height: 300px;
}
.kanban {
  flex: 1;
  display: flex;
  gap: 15px;
  overflow-x: auto;
  min-height: 500px;
}
.kanban-col {
  flex: 1;
  min-width: 280px;
  background: #f5f7fa;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
}
.col-header {
  padding: 10px 12px;
  font-weight: bold;
  border-radius: 6px 6px 0 0;
  display: flex;
  justify-content: space-between;
}
.col-todo { background: #e6f7ff; color: #1890ff; }
.col-in_progress { background: #fff7e6; color: #fa8c16; }
.col-review { background: #f6ffed; color: #52c41a; }
.col-done { background: #f0f0f0; color: #666; }
.col-count {
  background: rgba(255,255,255,0.7);
  padding: 0 8px;
  border-radius: 10px;
  font-size: 12px;
}
.col-body {
  flex: 1;
  padding: 10px;
  min-height: 100px;
  overflow-y: auto;
}
.task-card {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  transition: transform 0.1s;
}
.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(0,0,0,0.12);
}
.task-card.overdue {
  border: 1px solid #f56c6c;
}
.task-card.unclaimed {
  border: 1px dashed #409eff;
  background: #f0f9ff;
}
.task-card.blocked {
  border: 1px solid #f56c6c;
  background: #fff5f5;
}
.card-project {
  font-size: 12px;
  color: #409eff;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
}
.card-project:hover {
  text-decoration: underline;
}
.card-title {
  font-weight: 500;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-avatar {
  background: #409eff;
  color: #fff;
  font-size: 12px;
}
.card-avatar.unclaimed {
  background: #909399;
}
.overdue-tag {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
}
.block-reason {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
  display: flex;
  align-items: center;
  gap: 4px;
}
.list-card {
  flex: 1;
  overflow: auto;
}
.empty {
  padding: 40px 0;
}
.drawer-body {
  padding-bottom: 20px;
}
.drawer-actions {
  margin-bottom: 15px;
}
.section {
  margin-top: 20px;
}
.section-title {
  font-weight: bold;
  margin-bottom: 10px;
  font-size: 16px;
}
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 4px 12px;
}
.batch-count {
  font-size: 13px;
  color: #606266;
}
.review-tip {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  margin-bottom: 14px;
}
.review-targets-list {
  max-height: 240px;
  overflow-y: auto;
  padding: 10px 12px;
  margin-bottom: 16px;
  background: #f5f7fa;
  border-radius: 6px;
}
.project-group + .project-group {
  margin-top: 12px;
}
.project-group-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}
.task-item-row {
  display: flex;
  align-items: center;
  gap: 6px;
  padding-left: 20px;
  font-size: 13px;
  color: #606266;
  line-height: 1.9;
}
.task-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.task-assignee {
  color: #909399;
  flex-shrink: 0;
}
.form-tip {
  font-size: 12px;
  color: #909399;
}
.form-tip-indent {
  padding-left: 70px;
  margin-top: -8px;
}
</style>
