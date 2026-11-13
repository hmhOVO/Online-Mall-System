<template>
  <div class="layout">
    <aside class="sidebar" :class="{'is-collapsed': menuCollapse, 'is-mobile-open': mobileOpen}">
      <div class="logo" title="高校实训管理系统">
        <span v-if="!menuCollapse">高校实训管理系统</span>
        <span v-else class="logo-short">实训</span>
      </div>
      <nav class="sidebar-nav" aria-label="主导航">
      <el-menu
          :default-active="activePath"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#8CC5FF"
          class="menu"
          :collapse="menuCollapse"
          :collapse-transition="false"
      >
        <template v-if="isStudent">
          <el-menu-item :index="basePath + '/home'">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/project'">
            <el-icon><Collection /></el-icon>
            <span>我的实训项目</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/my-tasks'">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>我的任务</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/my-calendar'">
            <el-icon><Calendar /></el-icon>
            <span>任务日历</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/my-group'">
            <el-icon><UserFilled /></el-icon>
            <span>我的小组</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/my-scores'">
            <el-icon><Medal /></el-icon>
            <span>我的成绩</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/notifications'">
            <el-icon><MessageBox /></el-icon>
            <span>消息中心</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/profile'">
            <el-icon><User /></el-icon>
            <span>个人设置</span>
          </el-menu-item>
        </template>

        <template v-else-if="isTeacher">
          <el-menu-item :index="basePath + '/home'">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/my-courses'">
            <el-icon><Reading /></el-icon>
            <span>我的课程</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/project'">
            <el-icon><Collection /></el-icon>
            <span>项目列表</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/training-tasks'">
            <el-icon><Grid /></el-icon>
            <span>实训任务总览</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/data-analysis'">
            <el-icon><PieChart /></el-icon>
            <span>数据分析</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/training-members'">
            <el-icon><UserFilled /></el-icon>
            <span>参训学生</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/my-tasks'">
            <el-icon><CircleCheckFilled /></el-icon>
            <span>我的任务</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/my-calendar'">
            <el-icon><Calendar /></el-icon>
            <span>任务日历</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/notifications'">
            <el-icon><Bell /></el-icon>
            <span>消息中心</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/profile'">
            <el-icon><User /></el-icon>
            <span>个人设置</span>
          </el-menu-item>
        </template>

        <template v-else-if="isAdmin">
          <el-menu-item :index="basePath + '/home'">
            <el-icon><DataLine /></el-icon>
            <span>系统概览</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/training'">
            <el-icon><OfficeBuilding /></el-icon>
            <span>组织架构</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/course'">
            <el-icon><Reading /></el-icon>
            <span>课程管理</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/user'">
            <el-icon><UserFilled /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/project'">
            <el-icon><Collection /></el-icon>
            <span>项目管理</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/training-overview'">
            <el-icon><Grid /></el-icon>
            <span>实训总览</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/data-analysis'">
            <el-icon><PieChart /></el-icon>
            <span>数据分析</span>
          </el-menu-item>
          <el-sub-menu index="/admin-system">
            <template #title>
              <el-icon><Tools /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item :index="basePath + '/system-config'">
              <el-icon><Setting /></el-icon>
              <span>系统配置</span>
            </el-menu-item>
            <el-menu-item :index="basePath + '/announcement'">
              <el-icon><Bell /></el-icon>
              <span>系统公告</span>
            </el-menu-item>
            <el-menu-item :index="basePath + '/data-export'">
              <el-icon><Download /></el-icon>
              <span>数据导出</span>
            </el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="/admin-log">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>日志管理</span>
            </template>
            <el-menu-item :index="basePath + '/operation-log'">
              <el-icon><Document /></el-icon>
              <span>操作日志</span>
            </el-menu-item>
            <el-menu-item :index="basePath + '/login-log'">
              <el-icon><DocumentChecked /></el-icon>
              <span>登录日志</span>
            </el-menu-item>
            <el-menu-item :index="basePath + '/ai-log'">
              <el-icon><ChatDotRound /></el-icon>
              <span>AI调用日志</span>
            </el-menu-item>
          </el-sub-menu>
          <el-menu-item :index="basePath + '/notifications'">
            <el-icon><Bell /></el-icon>
            <span>系统通知</span>
          </el-menu-item>
          <el-menu-item :index="basePath + '/profile'">
            <el-icon><Avatar /></el-icon>
            <span>个人设置</span>
          </el-menu-item>
        </template>
      </el-menu>
      </nav>
    </aside>

    <div v-if="isMobile && mobileOpen" class="sidebar-mask" @click="closeMobileNav" />

    <div class="main" :class="{'is-collapsed': menuCollapse}">
      <div class="header">
        <div class="header-left">
          <button
              class="icon-btn header-toggle header-toggle--mobile"
              type="button"
              :aria-label="mobileOpen ? '关闭导航菜单' : '打开导航菜单'"
              :aria-expanded="mobileOpen"
              @click="toggleMobileNav"
          >
            <el-icon :size="20"><Menu /></el-icon>
          </button>
          <button
              class="icon-btn header-toggle header-toggle--desktop"
              type="button"
              :aria-label="menuCollapse ? '展开侧边栏' : '收起侧边栏'"
              :aria-expanded="!menuCollapse"
              @click="toggleCollapse"
          >
            <el-icon :size="20"><Expand v-if="menuCollapse" /><Fold v-else /></el-icon>
          </button>
          <div class="breadcrumb">{{ route.meta.name || '首页' }}</div>
        </div>
        <div class="user-info">
          <el-dropdown placement="bottom" @visible-change="onNotificationVisible">
            <div class="notification-bell">
              <el-icon :size="22"><Bell /></el-icon>
              <el-badge v-if="unreadCount > 0" :value="unreadCount > 99 ? '99+' : unreadCount" class="badge" />
            </div>
            <template #dropdown>
              <el-dropdown-menu class="notification-dropdown">
                <div v-if="recentNotifications.length === 0" class="notification-empty">暂无新通知</div>
                <el-dropdown-item v-for="item in recentNotifications" :key="item.id" @click="goNotification(item)">
                  <div :class="['notification-item', { unread: !item.isRead }]">
                    <div class="notification-title">{{ item.title }}</div>
                    <div class="notification-content">{{ item.content }}</div>
                    <div class="notification-time">{{ item.createdAt }}</div>
                  </div>
                </el-dropdown-item>
                <el-dropdown-item divided @click="router.push(basePath + '/notifications')">
                  <div class="view-all">查看全部</div>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-avatar :size="32" :src="user.avatar || defaultAvatar" />
          <el-dropdown @command="handleCommand">
            <span class="nickname">
              {{ user.nickname || user.username }}<el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>
      <div class="content">
        <RouterView />
      </div>
    </div>

  </div>
</template>

<script setup>
import {ref, computed, watch, onMounted, onUnmounted} from "vue";
import {useRoute, useRouter} from "vue-router";
import {ElMessage, ElMessageBox} from "element-plus";
import {
  HomeFilled, UserFilled, Collection, Avatar, ArrowDown, ChatDotRound,
  DataLine, DataAnalysis, Document, Tools, Setting, TrendCharts, Bell, List, Calendar,
  FolderOpened, User, CircleCheckFilled, MessageBox, Grid, Medal,
  DocumentChecked, Download, PieChart, OfficeBuilding, Menu, Fold, Expand, Reading
} from '@element-plus/icons-vue';
import Request from "@/utils/request.js";
import {getUnreadCount, listNotifications, markRead} from "@/api/notification.js";
import {useWebSocket} from "@/composables/useWebSocket.js";

const route = useRoute();
const router = useRouter();
const user = ref(JSON.parse(localStorage.getItem('code_user') || '{}'));
const activePath = ref(route.path);
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png';

// 侧边栏：桌面端可收起为图标栏（64px），窄屏（<=768px）收起为抽屉
const MOBILE_BREAKPOINT = 768;
const collapsed = ref(localStorage.getItem('code_sidebar_collapsed') === '1');
const mobileOpen = ref(false);
const isMobile = ref(window.innerWidth <= MOBILE_BREAKPOINT);
const menuCollapse = computed(() => !isMobile.value && collapsed.value);

const toggleCollapse = () => {
  collapsed.value = !collapsed.value;
  localStorage.setItem('code_sidebar_collapsed', collapsed.value ? '1' : '0');
};

const toggleMobileNav = () => {
  mobileOpen.value = !mobileOpen.value;
};

const closeMobileNav = () => {
  mobileOpen.value = false;
};

const handleResize = () => {
  const mobile = window.innerWidth <= MOBILE_BREAKPOINT;
  isMobile.value = mobile;
  if (!mobile) {
    mobileOpen.value = false;
  }
};

const isAdmin = computed(() => user.value.role === 'ADMIN');
const isTeacher = computed(() => user.value.role === 'TEACHER');
const isStudent = computed(() => user.value.role === 'STUDENT');
const basePath = computed(() => {
  if (isAdmin.value) return '/admin';
  if (isTeacher.value) return '/teacher';
  return '/student';
});
const unreadCount = ref(0);
const recentNotifications = ref([]);
const {connect: connectWebSocket, addListener, disconnect} = useWebSocket();
let removeWebSocketListener = null;

watch(() => route.path, (newPath) => {
  activePath.value = newPath;
  closeMobileNav();
  loadUnreadCount();
});

const loadUnreadCount = () => {
  getUnreadCount().then(res => {
    unreadCount.value = res.data || 0;
  }).catch(() => {});
};

const loadRecentNotifications = () => {
  listNotifications({ pageNum: 1, pageSize: 5 }).then(res => {
    recentNotifications.value = (res.data.records || []).slice(0, 5);
  }).catch(() => {});
};

const onNotificationVisible = (visible) => {
  if (visible) {
    loadRecentNotifications();
  }
};

const goNotification = (item) => {
  const navigate = () => {
    if (item.targetUrl) {
      router.push(item.targetUrl);
    } else {
      router.push(basePath.value + '/notifications');
    }
  };
  if (!item.isRead) {
    markRead(item.id).then(() => {
      unreadCount.value = Math.max(0, unreadCount.value - 1);
      item.isRead = true;
      navigate();
    }).catch(navigate);
  } else {
    navigate();
  }
};

const handleWebSocketMessage = (data) => {
  if (data.type === 'STATS_UPDATE') {
    return;
  }
  if (data.type === 'LOG_UPDATE') {
    window.dispatchEvent(new CustomEvent('project-log-update', { detail: data }));
    return;
  }
  unreadCount.value = (unreadCount.value || 0) + 1;
  recentNotifications.value.unshift(data);
  if (recentNotifications.value.length > 5) {
    recentNotifications.value.pop();
  }
  if (data.title) {
    ElMessage.info(data.title);
  }
};

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push(basePath.value + '/profile');
  } else if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {type: 'warning'}).then(() => {
      Request.post('/auth/logout').finally(() => {
        localStorage.removeItem('code_user');
        disconnect();
        router.push('/login');
        ElMessage.success('已退出登录');
      });
    });
  }
};

const onUnreadCountChange = (event) => {
  if (event.detail && typeof event.detail.count === 'number') {
    unreadCount.value = Math.max(0, event.detail.count);
  }
};

const handleProfileUpdated = (event) => {
  if (event.detail) {
    user.value = { ...user.value, ...event.detail };
  }
};

onMounted(() => {
  loadUnreadCount();
  loadRecentNotifications();
  connectWebSocket();
  removeWebSocketListener = addListener(handleWebSocketMessage);
  window.addEventListener('notification-unread-count-change', onUnreadCountChange);
  window.addEventListener('profile-updated', handleProfileUpdated);
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  window.removeEventListener('notification-unread-count-change', onUnreadCountChange);
  window.removeEventListener('profile-updated', handleProfileUpdated);
  window.removeEventListener('resize', handleResize);
  if (removeWebSocketListener) {
    removeWebSocketListener();
    removeWebSocketListener = null;
  }
  disconnect();
});
</script>

<style scoped>
/* ============ 固定侧边栏布局（sidebar-fixed） ============ */
.layout {
  min-height: 100vh;
  background-color: #f5f7fa;
}

/* 侧边栏固定定位，宽度统一 256px（收起时 64px） */
.sidebar {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 256px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  background-color: #304156;
  color: #fff;
  overflow: hidden;
  transition: width .15s ease, transform .15s ease;
}

.sidebar.is-collapsed {
  width: 64px;
}

.logo {
  flex-shrink: 0;
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 18px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  border-bottom: 1px solid #1f2d3d;
}

.logo-short {
  font-size: 15px;
  letter-spacing: 1px;
}

/* 侧边栏内容超出时内部滚动 */
.sidebar-nav {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-nav::-webkit-scrollbar {
  width: 6px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background-color: rgba(255, 255, 255, .22);
  border-radius: 3px;
}

.menu {
  border-right: none;
}

/* 导航项交互：150ms 克制反馈（颜色 + 轻微图标位移） */
.menu :deep(.el-menu-item),
.menu :deep(.el-sub-menu__title) {
  transition: background-color .15s ease, color .15s ease;
}

.menu :deep(.el-menu-item .el-icon),
.menu :deep(.el-sub-menu__title .el-icon) {
  transition: transform .15s ease;
}

.menu :deep(.el-menu-item:hover .el-icon),
.menu :deep(.el-sub-menu__title:hover .el-icon) {
  transform: translateX(2px);
}

/* 当前页高亮：左侧实心锚点 + 浅色底 + 高对比文字 */
.menu :deep(.el-menu-item.is-active) {
  background-color: rgba(64, 158, 255, .12);
  box-shadow: inset 3px 0 0 0 #409EFF;
}

.menu :deep(.el-sub-menu.is-active > .el-sub-menu__title) {
  color: #8CC5FF;
  box-shadow: inset 3px 0 0 0 #409EFF;
}

/* 窄屏抽屉遮罩 */
.sidebar-mask {
  position: fixed;
  inset: 0;
  z-index: 900;
  background-color: rgba(0, 0, 0, .45);
}

/* 主内容区：左边距与侧边栏宽度对齐（等同于 ml-64） */
.main {
  margin-left: 256px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: margin-left .15s ease;
}

.main.is-collapsed {
  margin-left: 64px;
}

.header {
  height: 60px;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background-color: transparent;
  color: #606266;
  cursor: pointer;
  transition: background-color .15s ease, color .15s ease;
}

.icon-btn:hover {
  background-color: #f0f2f5;
  color: #409EFF;
}

.icon-btn:focus-visible {
  outline: 2px solid #409EFF;
  outline-offset: 2px;
}

.header-toggle--mobile {
  display: none;
}

.breadcrumb {
  font-size: 16px;
  font-weight: bold;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 15px;
}

.notification-bell {
  position: relative;
  cursor: pointer;
  color: #606266;
  display: flex;
  align-items: center;
}

.notification-bell:hover {
  color: #409EFF;
}

.badge {
  position: absolute;
  top: -8px;
  right: -8px;
}

.notification-dropdown {
  width: 320px;
  max-height: 400px;
  overflow-y: auto;
}

.notification-empty {
  padding: 20px;
  text-align: center;
  color: #909399;
}

.notification-item {
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}

.notification-item.unread {
  background-color: #f0f9ff;
}

.notification-title {
  font-weight: bold;
  color: #303133;
  font-size: 14px;
}

.notification-content {
  color: #606266;
  font-size: 12px;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notification-time {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}

.view-all {
  text-align: center;
  color: #409EFF;
  font-size: 14px;
}

.nickname {
  display: flex;
  align-items: center;
  color: #333;
}

.content {
  flex: 1;
  padding: 20px;
  background-color: #f5f7fa;
  overflow: auto;
}

/* ============ 窄屏：侧边栏收起为抽屉，主内容不再留左边距 ============ */
@media (max-width: 768px) {
  .sidebar {
    width: 256px;
    transform: translateX(-100%);
  }

  .sidebar.is-collapsed {
    width: 256px;
  }

  .sidebar.is-mobile-open {
    transform: translateX(0);
    box-shadow: 0 0 24px rgba(0, 0, 0, .28);
  }

  .main,
  .main.is-collapsed {
    margin-left: 0;
  }

  .header-toggle--mobile {
    display: inline-flex;
  }

  .header-toggle--desktop {
    display: none;
  }

  .header {
    padding: 0 12px;
  }

  .content {
    padding: 12px;
  }
}

/* ============ 尊重 reduced-motion：不依赖动效理解或完成任务 ============ */
@media (prefers-reduced-motion: reduce) {
  .sidebar,
  .main,
  .icon-btn,
  .menu :deep(.el-menu-item),
  .menu :deep(.el-sub-menu__title),
  .menu :deep(.el-menu-item .el-icon),
  .menu :deep(.el-sub-menu__title .el-icon) {
    transition: none;
  }

  .menu :deep(.el-menu-item:hover .el-icon),
  .menu :deep(.el-sub-menu__title:hover .el-icon) {
    transform: none;
  }
}
</style>
