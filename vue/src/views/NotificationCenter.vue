<template>
  <div v-loading="loading" class="notification-center">
    <el-card>
      <template #header>
        <div class="header">
          <el-button @click="goBack" size="small" icon="ArrowLeft">返回</el-button>
          <el-icon><Bell /></el-icon>
          <span>{{ isAdmin ? '系统通知' : '消息中心' }}</span>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange" type="border-card">
        <el-tab-pane label="消息通知" name="notifications">
          <div class="header-actions">
            <el-checkbox v-model="selectAll" @change="handleSelectAll" size="small">全选</el-checkbox>
            <el-button type="primary" size="small" @click="markAllRead">全部已读</el-button>
            <el-button type="danger" size="small" :disabled="selectedIds.length === 0" @click="handleBatchDelete">
              {{ selectedIds.length > 1 ? '批量删除' : '删除' }}({{ selectedIds.length }})
            </el-button>
          </div>

          <div class="stats-row">
            <el-statistic :title="isAdmin ? '未读通知' : '未读消息'" :value="unreadCount"><template #prefix><el-icon><CircleCloseFilled /></el-icon></template></el-statistic>
            <template v-if="!isAdmin">
              <el-statistic title="任务动态" :value="typeCounts.TASK || 0"><template #prefix><el-icon><List /></el-icon></template></el-statistic>
              <el-statistic title="审核通知" :value="typeCounts.AUDIT || 0"><template #prefix><el-icon><CircleCheckFilled /></el-icon></template></el-statistic>
              <el-statistic title="项目动态" :value="typeCounts.PROJECT || 0"><template #prefix><el-icon><FolderOpened /></el-icon></template></el-statistic>
            </template>
            <el-statistic :title="isAdmin ? '系统通知' : '系统通知'" :value="typeCounts.SYSTEM || 0"><template #prefix><el-icon><Bell /></el-icon></template></el-statistic>
          </div>

          <div class="filter-bar">
            <el-tag
                v-for="tag in displayTypeTags"
                :key="tag.value"
                :class="['type-tag', { active: selectedType === tag.value }]"
                @click="selectType(tag.value)"
            >
              {{ tag.label }}
            </el-tag>
          </div>

          <div v-if="groupedNotifications.length === 0" class="empty">
            <el-icon class="empty-icon"><Bell /></el-icon>
            <div>{{ isAdmin ? '暂无系统通知' : '暂无消息' }}</div>
          </div>

          <div v-for="group in groupedNotifications" :key="group.date">
            <div class="date-header">{{ group.label }}</div>
            <div v-for="typeGroup in group.types" :key="typeGroup.type">
              <div class="type-header">
                <el-icon class="type-icon"><component :is="typeGroup.icon" /></el-icon>
                <span class="type-name">{{ typeGroup.label }}</span>
                <span class="type-count">{{ typeGroup.items.length }}</span>
              </div>
              <div
                  v-for="item in typeGroup.items"
                  :key="item.id"
                  :class="['notification-card', { unread: !item.isRead }]"
                  @click="goTarget(item)"
              >
                <el-checkbox 
                  class="notification-checkbox" 
                  :model-value="selectedIds.includes(item.id)" 
                  @change="(val) => toggleSelect(item.id, val)"
                  @click.stop
                ></el-checkbox>
                <div class="notification-header">
                  <span class="title">{{ item.title }}</span>
                  <span class="time">{{ formatTime(item.createdAt) }}</span>
                </div>
                <div class="content">{{ item.content }}</div>
                <div class="actions">
                  <el-button v-if="!item.isRead" link type="primary" size="small" @click.stop="markReadItem(item)">
                    标记已读
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <el-pagination
              v-model:current-page="pageNum"
              v-model:page-size="pageSize"
              :total="total"
              layout="total, prev, pager, next"
              style="margin-top: 20px; justify-content: flex-end"
              @current-change="loadNotifications"
          />
        </el-tab-pane>

        <el-tab-pane label="系统公告" name="announcements">
          <div v-if="announcements.length === 0" class="empty">
            <el-icon class="empty-icon"><Document /></el-icon>
            <div>暂无系统公告</div>
          </div>

          <div v-for="item in announcements" :key="item.id" class="announcement-card" @click="viewAnnouncement(item)">
            <div class="announcement-header">
              <div class="announcement-title">
                <el-tag v-if="item.isTop === 1" type="danger" size="small" effect="dark">置顶</el-tag>
                <el-tag :type="announcementTypeTagType(item.type)" size="small">{{ announcementTypeLabel(item.type) }}</el-tag>
                <span>{{ item.title }}</span>
              </div>
              <span class="announcement-time">{{ item.publishTime || item.createdAt }}</span>
            </div>
            <div class="announcement-content">{{ item.content }}</div>
            <div class="announcement-meta">
              <span>发布人：{{ item.publisherName || '系统' }}</span>
            </div>
          </div>

          <el-pagination
              v-model:current-page="announcementPageNum"
              v-model:page-size="announcementPageSize"
              :total="announcementTotal"
              layout="total, prev, pager, next"
              style="margin-top: 20px; justify-content: flex-end"
              @current-change="loadAnnouncements"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="announcementDetailVisible" title="公告详情" width="600px">
      <div class="detail-header">
        <h3>{{ currentAnnouncement.title }}</h3>
        <div class="detail-meta">
          <el-tag :type="announcementTypeTagType(currentAnnouncement.type)" size="small">{{ announcementTypeLabel(currentAnnouncement.type) }}</el-tag>
          <span v-if="currentAnnouncement.isTop === 1" class="top-badge">置顶</span>
          <span>发布人：{{ currentAnnouncement.publisherName || '系统' }}</span>
          <span>发布时间：{{ currentAnnouncement.publishTime || currentAnnouncement.createdAt }}</span>
        </div>
      </div>
      <div class="detail-content">
        {{ currentAnnouncement.content }}
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage, ElMessageBox } from 'element-plus';
import { listNotifications, markRead, markAllRead as apiMarkAllRead, deleteBatch } from '@/api/notification.js';
import { Bell, CircleCloseFilled, List, CircleCheckFilled, FolderOpened, Clock, WarningFilled, ChatDotRound, UserFilled, MagicStick, Warning, Document } from '@element-plus/icons-vue';
import { getPublishedAnnouncements } from '@/api/announcement.js';

const router = useRouter();
const user = JSON.parse(localStorage.getItem('code_user') || '{}');
const isAdmin = user.role === 'ADMIN';

const loading = ref(false);
const notifications = ref([]);
const pageNum = ref(1);
const pageSize = ref(20);
const total = ref(0);
const unreadCount = ref(0);
const selectedType = ref('');
const selectedIds = ref([]);
const selectAll = ref(false);

const activeTab = ref('notifications');
const announcements = ref([]);
const announcementPageNum = ref(1);
const announcementPageSize = ref(10);
const announcementTotal = ref(0);
const announcementDetailVisible = ref(false);
const currentAnnouncement = reactive({});

const typeTags = [
  { value: '', label: '全部', icon: Bell, category: 'ALL' },
  { value: 'TASK_STATUS', label: '任务动态', icon: List, category: 'TASK' },
  { value: 'TASK_SCORED', label: '任务已评分', icon: CircleCheckFilled, category: 'TASK' },
  { value: 'ASSIGN', label: '任务分配', icon: List, category: 'TASK' },
  { value: 'TASK_CLAIM', label: '任务认领', icon: List, category: 'TASK' },
  { value: 'OVERDUE', label: '逾期提醒', icon: Clock, category: 'TASK' },
  { value: 'TASK_BLOCK', label: '阻塞通知', icon: WarningFilled, category: 'TASK' },
  { value: 'REJECT', label: '审核驳回', icon: CircleCloseFilled, category: 'AUDIT' },
  { value: 'MENTION', label: '@提醒', icon: ChatDotRound, category: 'AUDIT' },
  { value: 'MEMBER', label: '成员变动', icon: UserFilled, category: 'PROJECT' },
  { value: 'GROUP_INVITE', label: '分组通知', icon: UserFilled, category: 'PROJECT' },
  { value: 'LOGIN_WARNING', label: '登录告警', icon: Warning, category: 'SYSTEM' },
  { value: 'AI_RISK', label: 'AI通知', icon: MagicStick, category: 'SYSTEM' },
];

const displayTypeTags = computed(() => {
  if (isAdmin) {
    return typeTags.filter(tag => tag.category === 'SYSTEM' || tag.value === '');
  }
  return typeTags.filter(tag => 
    tag.category !== 'SYSTEM' || tag.value === 'AI_RISK' || tag.value === ''
  );
});

const typeConfig = {
  TASK_STATUS: { label: '任务动态', icon: List, category: 'TASK' },
  TASK_SCORED: { label: '任务已评分', icon: CircleCheckFilled, category: 'TASK' },
  ASSIGN: { label: '任务分配', icon: List, category: 'TASK' },
  TASK_CLAIM: { label: '任务认领', icon: List, category: 'TASK' },
  OVERDUE: { label: '逾期提醒', icon: Clock, category: 'TASK' },
  TASK_BLOCK: { label: '阻塞通知', icon: WarningFilled, category: 'TASK' },
  REJECT: { label: '审核驳回', icon: CircleCloseFilled, category: 'AUDIT' },
  MENTION: { label: '@提醒', icon: ChatDotRound, category: 'AUDIT' },
  MEMBER: { label: '成员变动', icon: UserFilled, category: 'PROJECT' },
  GROUP_INVITE: { label: '分组通知', icon: UserFilled, category: 'PROJECT' },
  LOGIN_WARNING: { label: '登录告警', icon: Warning, category: 'SYSTEM' },
  AI_RISK: { label: 'AI通知', icon: MagicStick, category: 'SYSTEM' },
};

const categoryConfig = {
  TASK: { label: '任务动态', icon: List },
  AUDIT: { label: '审核通知', icon: CircleCheckFilled },
  PROJECT: { label: '项目动态', icon: FolderOpened },
  SYSTEM: { label: '系统通知', icon: Bell },
};

const typeCounts = ref({ TASK: 0, AUDIT: 0, PROJECT: 0, SYSTEM: 0 });

const groupedNotifications = computed(() => {
  let filtered = selectedType.value
      ? notifications.value.filter(n => n.type === selectedType.value)
      : notifications.value;

  // 管理员只显示系统通知
  if (isAdmin) {
    filtered = filtered.filter(n => {
      const config = typeConfig[n.type] || { category: 'SYSTEM' };
      return config.category === 'SYSTEM';
    });
  }

  const dateGroups = {};
  filtered.forEach(item => {
    const dateLabel = getDateLabel(item.createdAt);
    if (!dateGroups[dateLabel]) {
      dateGroups[dateLabel] = { date: dateLabel, label: dateLabel, types: {} };
    }
    const config = typeConfig[item.type] || { label: item.type, icon: Bell, category: 'SYSTEM' };
    const category = config.category;
    if (!dateGroups[dateLabel].types[category]) {
      dateGroups[dateLabel].types[category] = {
        type: category,
        label: categoryConfig[category].label,
        icon: categoryConfig[category].icon,
        items: []
      };
    }
    dateGroups[dateLabel].types[category].items.push(item);
  });

  const result = Object.values(dateGroups).sort((a, b) => {
    const order = ['今天', '昨天', '本周', '更早'];
    return order.indexOf(a.date) - order.indexOf(b.date);
  });

  result.forEach(group => {
    group.types = Object.values(group.types).sort((a, b) => {
      const order = ['TASK', 'AUDIT', 'PROJECT', 'SYSTEM'];
      return order.indexOf(a.type) - order.indexOf(b.type);
    });
  });

  return result;
});

function getDateLabel(dateStr) {
  const date = new Date(dateStr);
  const today = new Date();
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);

  const isToday = date.toDateString() === today.toDateString();
  const isYesterday = date.toDateString() === yesterday.toDateString();

  if (isToday) return '今天';
  if (isYesterday) return '昨天';

  const startOfWeek = new Date(today);
  startOfWeek.setDate(today.getDate() - today.getDay() + 1);
  if (date >= startOfWeek) return '本周';

  return '更早';
}

function formatTime(dateStr) {
  const date = new Date(dateStr);
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
}

const selectType = (type) => {
  selectedType.value = type;
  pageNum.value = 1;
  selectedIds.value = [];
  selectAll.value = false;
  loadNotifications();
};

const loadNotifications = () => {
  loading.value = true;
  const params = { pageNum: pageNum.value, pageSize: pageSize.value };
  if (selectedType.value) params.type = selectedType.value;
  listNotifications(params).then(res => {
    notifications.value = res.data.records || [];
    total.value = res.data.total || 0;
    unreadCount.value = res.data.unread || 0;
    typeCounts.value = res.data.typeCounts || { TASK: 0, AUDIT: 0, PROJECT: 0, SYSTEM: 0 };
    selectedIds.value = [];
    selectAll.value = false;
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const toggleSelect = (id, val) => {
  const index = selectedIds.value.indexOf(id);
  if (val) {
    if (index === -1) {
      selectedIds.value.push(id);
    }
  } else {
    if (index > -1) {
      selectedIds.value.splice(index, 1);
    }
  }
  updateSelectAll();
};

const handleSelectAll = () => {
  if (selectAll.value) {
    selectedIds.value = notifications.value.map(n => n.id);
  } else {
    selectedIds.value = [];
  }
};

const updateSelectAll = () => {
  selectAll.value = notifications.value.length > 0 && 
    selectedIds.value.length === notifications.value.length;
};

const handleBatchDelete = () => {
  if (selectedIds.value.length === 0) return;
  ElMessageBox.confirm(
    `确定要删除选中的 ${selectedIds.value.length} 条消息吗？`,
    '批量删除',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    deleteBatch(selectedIds.value).then(() => {
      ElMessage.success(`成功删除 ${selectedIds.value.length} 条消息`);
      selectedIds.value = [];
      selectAll.value = false;
      loadNotifications();
    }).catch(() => {
      ElMessage.error('删除失败');
    });
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
};

const broadcastUnreadCount = () => {
  window.dispatchEvent(new CustomEvent('notification-unread-count-change', {
    detail: { count: unreadCount.value }
  }));
};

const markReadItem = (item) => {
  return markRead(item.id).then(() => {
    item.isRead = true;
    unreadCount.value = Math.max(0, unreadCount.value - 1);
    broadcastUnreadCount();
    ElMessage.success('已标记为已读');
  });
};

const markAllRead = () => {
  apiMarkAllRead().then(() => {
    notifications.value.forEach(item => item.isRead = true);
    unreadCount.value = 0;
    broadcastUnreadCount();
    ElMessage.success('全部已读');
  });
};

const goTarget = (item) => {
  const navigate = () => {
    if (item.targetUrl) {
      router.push(item.targetUrl);
    }
  };
  if (!item.isRead) {
    markReadItem(item).then(navigate).catch(navigate);
  } else {
    navigate();
  }
};

const goBack = () => {
  router.back();
};

const handleTabChange = (tabName) => {
  if (tabName === 'announcements') {
    loadAnnouncements();
  }
};

const loadAnnouncements = () => {
  const params = {
    pageNum: announcementPageNum.value,
    pageSize: announcementPageSize.value
  };
  getPublishedAnnouncements(params).then(res => {
    announcements.value = res.data.records;
    announcementTotal.value = res.data.total;
  }).catch(() => {});
};

const announcementTypeLabel = (type) => {
  const map = { NOTICE: '通知', IMPORTANT: '重要', MAINTENANCE: '维护' };
  return map[type] || type;
};

const announcementTypeTagType = (type) => {
  const map = { NOTICE: 'info', IMPORTANT: 'danger', MAINTENANCE: 'warning' };
  return map[type] || 'info';
};

const viewAnnouncement = (item) => {
  Object.assign(currentAnnouncement, item);
  announcementDetailVisible.value = true;
};

onMounted(() => {
  loadNotifications();
});
</script>

<style scoped>
.notification-center {
  max-width: 1000px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.stats-row {
  display: flex;
  gap: 20px;
  padding: 15px 0;
  border-bottom: 1px solid #ebeef5;
  margin-bottom: 15px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 0;
  margin-bottom: 15px;
}

.type-tag {
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  transition: all 0.2s;
}

.type-tag:hover {
  background-color: #f0f5ff;
}

.type-tag.active {
  background-color: #409EFF;
  color: #fff;
}

.empty {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.date-header {
  font-size: 14px;
  color: #909399;
  margin: 20px 0 10px;
  padding-left: 10px;
  border-left: 3px solid #409EFF;
}

.type-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 15px;
  background-color: #fafafa;
  margin-top: 5px;
  border-radius: 4px;
}

.type-icon {
  font-size: 16px;
}

.type-name {
  font-weight: bold;
  font-size: 14px;
  color: #303133;
}

.type-count {
  background-color: #409EFF;
  color: #fff;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 10px;
}

.notification-card {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background 0.2s;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.notification-checkbox {
  flex-shrink: 0;
  margin-top: 2px;
}

.notification-card:hover {
  background-color: #f5f7fa;
}

.notification-card.unread {
  background-color: #f0f9ff;
  border-left: 4px solid #409EFF;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title {
  font-weight: bold;
  font-size: 15px;
}

.time {
  color: #909399;
  font-size: 13px;
}

.content {
  color: #606266;
  margin-top: 8px;
  line-height: 1.5;
}

.actions {
  margin-top: 8px;
}

.announcement-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}
.announcement-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.1);
}
.announcement-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.announcement-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
.announcement-time {
  color: #909399;
  font-size: 13px;
  flex-shrink: 0;
}
.announcement-content {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.announcement-meta {
  color: #909399;
  font-size: 12px;
  text-align: right;
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
  flex-wrap: wrap;
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
