import request from '@/utils/request.js';

export function getPublicConfig() {
    return request.get('/config/public');
}

export function listAdminConfigs() {
    return request.get('/admin/config');
}

export function updateAdminConfig(data) {
    return request.put('/admin/config', data);
}
