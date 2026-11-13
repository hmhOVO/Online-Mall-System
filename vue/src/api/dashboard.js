import request from '@/utils/request.js';

export function getPersonalDashboard() {
    return request.get('/dashboard/personal');
}

export function getTeacherDashboard() {
    return request.get('/dashboard/teacher');
}

export function getProjectStatistics(projectId) {
    return request.get(`/dashboard/project/${projectId}`);
}

export function getAiStatistics() {
    return request.get('/dashboard/ai/statistics');
}

export function getAdminDashboard() {
    return request.get('/dashboard/admin');
}

export function getMemberLoad(projectId) {
    const params = projectId ? { projectId } : {};
    return request.get('/dashboard/member-load', { params });
}

export function getAdminBusinessOverview() {
    return request.get('/dashboard/admin/business');
}
