import {createRouter, createWebHistory} from 'vue-router'

const Layout = () => import('../views/Layout.vue');

// 个人功能（三角色都有）
const personalRoutes = [
    {path: 'my-tasks', meta: {name: '我的任务'}, component: () => import('../views/MyTasks.vue')},
    {path: 'my-calendar', meta: {name: '任务日历'}, component: () => import('../views/MyCalendar.vue')},
    {path: 'notifications', meta: {name: '消息中心'}, component: () => import('../views/NotificationCenter.vue')},
    {path: 'profile', meta: {name: '个人设置'}, component: () => import('../views/Profile.vue')},
];

// 管理员专属的个人功能（只有消息中心和个人设置）
const adminPersonalRoutes = [
    {path: 'notifications', meta: {name: '消息中心'}, component: () => import('../views/NotificationCenter.vue')},
    {path: 'profile', meta: {name: '个人设置'}, component: () => import('../views/Profile.vue')},
];

// 参训学生页面（仅指导教师）
const trainingMemberRoute =
    {path: 'training-members', meta: {name: '参训学生'}, component: () => import('../views/TrainingMembers.vue')};

// 教师「我的课程」
const myCoursesRoute =
    {path: 'my-courses', meta: {name: '我的课程'}, component: () => import('../views/MyCourses.vue')};

// 项目管理页面（ADMIN/TEACHER 叫"项目管理"，STUDENT 叫"我的实训项目"，组件相同）
const projectRoute =
    {path: 'project', meta: {name: '项目管理'}, component: () => import('../views/Project.vue')};

const memberProjectRoute =
    {path: 'project', meta: {name: '我的实训项目'}, component: () => import('../views/Project.vue')};

// 学生专属功能
const studentOnlyRoutes = [
    {path: 'my-group', meta: {name: '我的小组'}, component: () => import('../views/MyGroup.vue')},
    {path: 'my-scores', meta: {name: '我的成绩'}, component: () => import('../views/MyScores.vue')},
];

// 指导教师专属功能
const managerOnlyRoutes = [
    {path: 'training-tasks', meta: {name: '实训任务总览'}, component: () => import('../views/TrainingTasks.vue')},
];

// ADMIN 专属功能
const adminOnlyRoutes = [
    {path: 'training-overview', meta: {name: '实训总览'}, component: () => import('../views/TrainingOverview.vue')},
    {path: 'project', meta: {name: '项目管理', roles: ['ADMIN']}, component: () => import('../views/Project.vue')},
    {path: 'training', meta: {name: '组织架构', roles: ['ADMIN']}, component: () => import('../views/TrainingManage.vue')},
    {path: 'course', meta: {name: '课程管理', roles: ['ADMIN']}, component: () => import('../views/Course.vue')},
    {path: 'user', meta: {name: '用户管理', roles: ['ADMIN']}, component: () => import('../views/User.vue')},
    {path: 'ai-log', meta: {name: 'AI调用日志', roles: ['ADMIN']}, component: () => import('../views/AiLog.vue')},
    {path: 'operation-log', meta: {name: '操作日志', roles: ['ADMIN']}, component: () => import('../views/OperationLog.vue')},
    {path: 'login-log', meta: {name: '登录日志', roles: ['ADMIN']}, component: () => import('../views/LoginLog.vue')},
    {path: 'system-config', meta: {name: '系统配置', roles: ['ADMIN']}, component: () => import('../views/SystemConfig.vue')},
    {path: 'announcement', meta: {name: '系统公告', roles: ['ADMIN']}, component: () => import('../views/AdminAnnouncement.vue')},
    {path: 'data-export', meta: {name: '数据导出', roles: ['ADMIN', 'TEACHER']}, component: () => import('../views/DataExport.vue')},
];

// ADMIN 和 TEACHER 共有的功能
const sharedAdminRoutes = [
    {path: 'data-analysis', meta: {name: '数据分析'}, component: () => import('../views/DataAnalysis.vue')},
];

// 各角色首页
const roleHomeRoutes = {
    ADMIN: {path: 'home', meta: {name: '系统概览'}, component: () => import('../views/AdminDashboard.vue')},
    TEACHER: {path: 'home', meta: {name: '首页'}, component: () => import('../views/TeacherDashboard.vue')},
    STUDENT: {path: 'home', meta: {name: '首页'}, component: () => import('../views/StudentWorkspace.vue')},
};

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes: [
        {
            path: '/',
            redirect: () => {
                const userStr = localStorage.getItem('code_user');
                const user = userStr ? JSON.parse(userStr) : null;
                if (!user || !user.role) return '/login';
                if (user.role === 'STUDENT') return '/student/home';
                if (user.role === 'TEACHER') return '/teacher/home';
                if (user.role === 'ADMIN') return '/admin/home';
                // 未知/陈旧角色（如历史 MANAGER/MEMBER）统一回登录页，避免落到 /admin 被守卫踢成 notFound
                return '/login';
            }
        },
        {
            path: '/admin',
            component: Layout,
            children: [
                roleHomeRoutes.ADMIN,
                ...adminOnlyRoutes,
                ...sharedAdminRoutes,
                ...adminPersonalRoutes,
            ],
        },
        {
            path: '/teacher',
            component: Layout,
            children: [
                roleHomeRoutes.TEACHER,
                myCoursesRoute,
                projectRoute,
                trainingMemberRoute,
                ...managerOnlyRoutes,
                ...sharedAdminRoutes,
                ...personalRoutes,
            ],
        },
        {
            path: '/student',
            component: Layout,
            children: [
                roleHomeRoutes.STUDENT,
                memberProjectRoute,
                ...studentOnlyRoutes,
                ...personalRoutes,
            ],
        },
        {
            path: '/project/:id',
            meta: {name: '项目详情'},
            component: () => import('../views/ProjectDetail.vue'),
        },
        {
            path: '/project/:id/board',
            meta: {name: '任务看板'},
            component: () => import('../views/TaskBoard.vue'),
        },
        {
            path: '/project/:id/gantt',
            meta: {name: '甘特图'},
            component: () => import('../views/TaskGantt.vue'),
        },
        {
            path: '/project/:id/scores',
            meta: {name: '成绩汇总'},
            component: () => import('../views/ScoreSummary.vue'),
        },
        {path: '/login', component: () => import('../views/Login.vue')},
        {path: '/notFound', meta: {name: '页面不存在'}, component: () => import('../views/404.vue')},
        {path: '/:pathMatch(.*)', redirect: '/notFound'},
    ],
});

// 平台合法角色白名单（与后端 RoleEnum 保持一致）
const VALID_ROLES = ['ADMIN', 'TEACHER', 'STUDENT'];

router.beforeEach((to, from, next) => {
    let user = null;
    try {
        const userStr = localStorage.getItem('code_user');
        if (userStr) {
            user = JSON.parse(userStr);
        }
    } catch (e) {
        console.error('解析用户信息失败', e);
        localStorage.removeItem('code_user');
    }

    // 角色白名单校验：缓存中若为陈旧角色（如历史 MANAGER/MEMBER），就地清理并回登录页
    // 这样浏览器无需手动清缓存，旧数据会在首次进入时自动失效
    if (user && user.role && !VALID_ROLES.includes(user.role)) {
        localStorage.removeItem('code_user');
        next('/login');
        return;
    }

    if (to.path === '/login') {
        next();
        return;
    }

    if (!user || !user.token) {
        next('/login');
        return;
    }

    if (to.meta.roles && user.role !== 'ADMIN' && !to.meta.roles.includes(user.role)) {
        next('/notFound');
        return;
    }

    if (to.path.startsWith('/admin') && user.role !== 'ADMIN') {
        next('/notFound');
        return;
    }

    if (to.path.startsWith('/teacher') && user.role !== 'TEACHER') {
        next('/notFound');
        return;
    }

    if (to.path.startsWith('/student') && user.role !== 'STUDENT') {
        next('/notFound');
        return;
    }

    next();
});

export default router