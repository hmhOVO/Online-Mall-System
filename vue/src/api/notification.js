import request from '@/utils/request.js';

export function listNotifications(params) {
    return request.get('/notification/list', { params });
}

export function getUnreadCount() {
    return request.get('/notification/unread-count');
}

export function markRead(id) {
    return request.put(`/notification/${id}/read`);
}

export function markAllRead() {
    return request.put('/notification/read-all');
}

export function deleteBatch(ids) {
    return request.delete('/notification/batch', { data: ids });
}
