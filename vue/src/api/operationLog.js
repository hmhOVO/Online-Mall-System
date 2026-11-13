import request from '@/utils/request.js';

export function listOperationLogs(params) {
    return request.get('/admin/operation-logs', { params });
}

export function getOperationLogStatistics() {
    return request.get('/admin/operation-logs/statistics');
}
