import request from '@/utils/request.js';

export function createTask(data) {
    return request.post('/task', data);
}

export function updateTask(id, data) {
    return request.put('/task/' + id, data);
}

export function deleteTask(id) {
    return request.delete('/task/' + id);
}

export function getTaskDetail(id) {
    return request.get('/task/' + id);
}

export function claimTask(id) {
    return request.post('/task/' + id + '/claim');
}

export function listProjectTasks(projectId, params) {
    return request.get('/project/' + projectId + '/tasks', {params});
}

export function updateTaskStatus(id, status) {
    return request.put('/task/' + id + '/status', {status});
}

export function updateTaskProgress(id, progress) {
    return request.put('/task/' + id + '/progress', {progress});
}

export function updateTaskActualHours(id, actualHours) {
    return request.put('/task/' + id + '/actual-hours', {actualHours});
}

export function submitReview(id, reviewerId) {
    return request.post('/task/' + id + '/review', reviewerId != null ? { reviewerId } : {});
}

export function approveReview(id, score) {
    return request.put('/task/' + id + '/review/approve', score != null ? { score } : {});
}

export function rejectReview(id, comment) {
    return request.put('/task/' + id + '/review/reject', {comment});
}

export function scoreTask(id, score) {
    return request.put('/task/' + id + '/score', {score});
}

export function withdrawReview(id) {
    return request.put('/task/' + id + '/review/withdraw');
}

export function completeDirectly(id) {
    return request.put('/task/' + id + '/complete');
}

export function checkDependencyCycle(id, dependencyIds) {
    return request.post('/task/' + id + '/dependency/check', {dependencyIds});
}

export function addComment(id, content, parentCommentId) {
    return request.post('/task/' + id + '/comment', {content, parentCommentId});
}

export function resolveComment(id, resolved) {
    return request.put('/task/comment/' + id + '/resolve', {resolved});
}

export function listOverdueTasks() {
    return request.get('/task/overdue/list');
}

export function listTrainingTasks(params) {
    return request.get('/task/training-list', {params});
}

export function listTaskAttachments(taskId) {
    return request.get('/task/' + taskId + '/attachments');
}

export function uploadTaskAttachment(taskId, file) {
    const formData = new FormData();
    formData.append('file', file);
    return request.post('/task/' + taskId + '/attachment', formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
    });
}

export function deleteTaskAttachment(taskId, attachmentId) {
    return request.delete('/task/' + taskId + '/attachment/' + attachmentId);
}
