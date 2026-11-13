import request from '@/utils/request.js';

export function getProfile() {
    return request.get('/user/profile');
}

export function updateProfile(data) {
    return request.put('/user/profile', data);
}

export function updatePassword(data) {
    return request.put('/user/password', data);
}

export function searchUsers(params) {
    return request.get('/user/search', { params });
}

export function listTeachers() {
    return request.get('/user/teachers');
}

export function listAdminUsers(params) {
    return request.get('/admin/users', { params });
}

export function addUser(data) {
    return request.post('/admin/user', data);
}

export function importStudents(classId, file) {
    const formData = new FormData();
    formData.append('classId', classId);
    formData.append('file', file);
    return request.post('/admin/user/import', formData);
}

export function importTeachers(file) {
    const formData = new FormData();
    formData.append('file', file);
    return request.post('/admin/user/import-teachers', formData);
}

export function updateUserStatus(id, status) {
    return request.put('/admin/user/' + id + '/status', { status });
}

export function resetPassword(id) {
    return request.put('/admin/user/' + id + '/reset-password');
}

export function deleteUser(id) {
    return request.delete('/admin/user/' + id);
}