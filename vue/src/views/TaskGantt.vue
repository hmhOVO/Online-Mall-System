<template>
  <div v-loading="loading" class="gantt-container">
    <el-card class="gantt-card">
      <div class="gantt-header">
        <div class="header-left">
          <el-button @click="goBack">返回项目</el-button>
          <span class="project-title">{{ project.name }}</span>
        </div>
        <div class="header-right">
          <el-select v-model="viewScale" @change="changeViewScale">
            <el-option label="日" value="day" />
            <el-option label="周" value="week" />
            <el-option label="月" value="month" />
          </el-select>
          <el-button type="primary" size="small" @click="refreshData">刷新</el-button>
        </div>
      </div>
    </el-card>
    <div ref="ganttContainer" class="gantt-wrapper"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, reactive } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { listProjectTasks } from '@/api/task.js';
import request from '@/utils/request.js';

const route = useRoute();
const router = useRouter();
const projectId = Number(route.params.id);

if (isNaN(projectId)) {
  router.replace('/notFound');
}

const loading = ref(false);
const project = reactive({ name: '' });
const ganttContainer = ref(null);
const viewScale = ref('week');

let ganttInstance = null;

const loadProject = () => {
  request.get('/project/' + projectId).then(res => {
    Object.assign(project, res.data);
  });
};

const loadTasks = () => {
  loading.value = true;
  listProjectTasks(projectId, {}).then(res => {
    try {
      renderGantt(res.data || []);
    } catch (e) {
      console.error('甘特图渲染失败:', e);
      ElMessage.error('甘特图渲染失败');
    }
  }).catch(() => {
    ElMessage.error('加载任务失败');
  }).finally(() => {
    loading.value = false;
  });
};

const flattenTasks = (tasks, list = []) => {
  tasks.forEach(t => {
    const hasChildren = t.children && t.children.length > 0;
    list.push({ ...t, hasChildren });
    if (hasChildren) flattenTasks(t.children, list);
  });
  return list;
};

const safeDestroyGantt = () => {
  if (!ganttInstance) return;
  try {
    if (typeof ganttInstance.clearAll === 'function') {
      ganttInstance.clearAll();
    }
  } catch (e) {
    console.warn('甘特图清理失败，忽略:', e);
  }
  ganttInstance = null;
};

const applyGanttConfig = () => {
  ganttInstance.config.date_format = '%Y-%m-%d';
  ganttInstance.config.scale_unit = viewScale.value;
  ganttInstance.config.date_scale = viewScale.value === 'day' ? '%m/%d' :
      viewScale.value === 'week' ? '%W周' : '%Y年%m月';
  ganttInstance.config.subscales = viewScale.value === 'week' ? [
    { unit: 'day', step: 1, date: '%d' }
  ] : viewScale.value === 'month' ? [
    { unit: 'week', step: 1, date: '第%W周' }
  ] : [];

  ganttInstance.config.grid_width = 420;
  ganttInstance.config.columns = [
    { name: 'title', label: '任务名称', width: '*', tree: true },
    { name: 'assigneeName', label: '负责人', width: 100, template: (task) => formatAssignee(task.assigneeName) },
    { name: 'priority', label: '优先级', width: 70, template: (task) => priorityLabel(task.priority) },
    { name: 'progress', label: '进度', width: 90, template: (task) => progressBar(task.progress) },
    { name: 'start_date', label: '开始', width: 90, template: (task) => formatGanttDate(task.start_date) },
    { name: 'end_date', label: '结束', width: 90, template: (task) => formatGanttDate(task.end_date) },
  ];

  ganttInstance.config.readonly = true;
  ganttInstance.config.show_links = true;
};

const buildGanttData = (tasks) => {
  const allTasks = flattenTasks(tasks);
  const ganttTasks = allTasks.map(t => ({
    id: t.id,
    text: t.title,
    start_date: t.startDate || new Date().toISOString().split('T')[0],
    end_date: t.endDate || new Date(Date.now() + 7 * 24 * 60 * 60 * 1000).toISOString().split('T')[0],
    progress: t.progress || 0,
    parent: t.parentTaskId || 0,
    assigneeName: t.assigneeName || '未分配',
    priority: t.priority,
    status: t.status,
    type: t.hasChildren ? 'project' : 'task'
  }));

  ganttTasks.forEach(t => {
    if (t.type === 'project') {
      t.open = true;
    }
  });

  const links = [];
  allTasks.forEach(t => {
    if (t.dependencyIds && t.dependencyIds.length) {
      t.dependencyIds.forEach(depId => {
        links.push({
          id: `link-${t.id}-${depId}`,
          source: depId,
          target: t.id,
          type: '0'
        });
      });
    }
  });

  return { data: ganttTasks, links };
};

const initGantt = (tasks) => {
  if (ganttInstance) return;

  ganttInstance = window.gantt;
  try {
    applyGanttConfig();

    if (typeof ganttInstance.addTaskLayer === 'function') {
      ganttInstance.addTaskLayer((task) => {
        if (task.status === 'DONE') {
          return null;
        }
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        const endDate = new Date(task.end_date);
        if (endDate < today) {
          const el = document.createElement('div');
          el.className = 'gantt-overdue-marker';
          return el;
        }
        return null;
      });
    }

    ganttInstance.templates.task_class = (start, end, task) => {
      if (task.status !== 'DONE' && end < new Date()) {
        return 'gantt-task-overdue';
      }
      return '';
    };

    ganttInstance.init(ganttContainer.value);
    ganttInstance.parse(buildGanttData(tasks));
  } catch (e) {
    console.error('甘特图初始化失败:', e);
    ganttInstance = null;
    ElMessage.error('甘特图初始化失败');
  }
};

const renderGantt = (tasks) => {
  if (!window.gantt) {
    Promise.all([
      import('dhtmlx-gantt'),
      import('dhtmlx-gantt/codebase/dhtmlxgantt.css')
    ]).then(([module]) => {
      window.gantt = module.default || module;
      initGantt(tasks);
    }).catch(err => {
      console.error('甘特图加载失败:', err);
      ElMessage.error('甘特图组件加载失败');
    });
  } else {
    if (!ganttInstance) {
      initGantt(tasks);
    } else {
      ganttInstance.clearAll();
      ganttInstance.parse(buildGanttData(tasks));
    }
  }
};

const formatAssignee = (value) => {
  if (value == null || value === '') return '未分配';
  if (typeof value === 'string') return value;
  if (typeof value === 'object') {
    return value.nickname || value.name || value.username || '未分配';
  }
  return String(value);
};

const formatGanttDate = (value) => {
  if (!value) return '-';
  const date = new Date(value);
  if (isNaN(date.getTime())) return String(value);
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const day = date.getDate().toString().padStart(2, '0');
  return `${month}-${day}`;
};

const priorityLabel = (value) => {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' };
  const colorMap = { HIGH: '#f56c6c', MEDIUM: '#e6a23c', LOW: '#67c23a' };
  return `<span style="color: ${colorMap[value] || '#909399'}; font-weight: 500;">${map[value] || value}</span>`;
};

const progressBar = (value) => {
  const pct = Math.round((Number(value) || 0) * 100);
  return `<div style="width: 100%; height: 14px; background: #ebeef5; border-radius: 7px; overflow: hidden; font-size: 11px; line-height: 14px; color: #606266; text-align: center;">
    <div style="width: ${pct}%; height: 100%; background: #409eff; float: left;"></div>
    <span style="position: relative; z-index: 1;">${pct}%</span>
  </div>`;
};

const changeViewScale = () => {
  if (ganttInstance) {
    applyGanttConfig();
    ganttInstance.render();
  }
  loadTasks();
};

const refreshData = () => {
  loadTasks();
};

const goBack = () => {
  router.push('/project/' + projectId);
};

onMounted(() => {
  loadProject();
  loadTasks();
});

onBeforeUnmount(() => {
  safeDestroyGantt();
});
</script>

<style scoped>
.gantt-container {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}
.gantt-card {
  margin-bottom: 15px;
  flex-shrink: 0;
}
.gantt-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.project-title {
  font-size: 18px;
  font-weight: bold;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.gantt-wrapper {
  flex: 1;
  min-height: 400px;
  background-color: #fff;
  border-radius: 4px;
  overflow: hidden;
}
:deep(.gantt-overdue-marker) {
  background: repeating-linear-gradient(
    -45deg,
    #f56c6c,
    #f56c6c 5px,
    #fff5f5 5px,
    #fff5f5 10px
  );
  opacity: 0.6;
}
:deep(.gantt_task_row.task_row) {
  height: 40px;
}
:deep(.gantt_row, .gantt_cell) {
  line-height: 40px;
}
:deep(.gantt_task_line) {
  background-color: #409eff;
}
:deep(.gantt_task_line.gantt-task-overdue) {
  background: repeating-linear-gradient(
    -45deg,
    #f56c6c,
    #f56c6c 5px,
    #fff5f5 5px,
    #fff5f5 10px
  );
  border-color: #f56c6c;
}
</style>