import request from '@/utils/request.js';

export function listLoginLogs(params) {
    return request.get('/admin/login-logs', { params });
}

export function getLoginLogStats() {
    return request.get('/admin/login-logs/stats');
}
