import request from '@/utils/request.js';

export function exportUsers(params) {
    return request.get('/admin/export/users', { params, responseType: 'blob' });
}

export function exportProjects(params) {
    return request.get('/admin/export/projects', { params, responseType: 'blob' });
}

export function exportTasks(params) {
    return request.get('/admin/export/tasks', {
        params,
        responseType: 'blob'
    });
}

export function exportScores(params) {
    return request.get('/admin/export/scores', {
        params,
        responseType: 'blob'
    });
}
