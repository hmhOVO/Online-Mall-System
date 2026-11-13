import request from '@/utils/request.js';

export function createProject(data) {
    return request.post('/project', data);
}

export function updateProject(id, data) {
    return request.put('/project/' + id, data);
}

export function deleteProject(id) {
    return request.delete('/project/' + id);
}

export function getProjectDetail(id) {
    return request.get('/project/' + id);
}

export function listProjects(params) {
    return request.get('/project/list', { params });
}

export function listDeletedProjects(params) {
    return request.get('/project/recycle-bin/list', { params });
}

export function restoreProject(id) {
    return request.put('/project/' + id + '/restore');
}

export function purgeProject(id) {
    return request.delete('/project/' + id + '/purge');
}

export function clearRecycleBin() {
    return request.delete('/project/recycle-bin/clear');
}

export function addProjectMember(projectId, userId) {
    return request.post('/project/' + projectId + '/members', { userId });
}

export function removeProjectMember(projectId, userId) {
    return request.delete('/project/' + projectId + '/members/' + userId);
}

export function updateProjectStatus(id, status) {
    return request.put('/project/' + id + '/status', { status });
}

export function restartProject(id) {
    return request.put('/project/' + id + '/restart');
}

export function completeProject(id) {
    return request.post('/project/' + id + '/complete');
}

export function getUnfinishedTaskCount(id) {
    return request.get('/project/' + id + '/unfinished-task-count');
}

export function transferProject(id, newTeacherId) {
    return request.put('/project/' + id + '/transfer', { newTeacherId });
}

export function assignGroupLeader(id, studentId) {
    return request.put('/project/' + id + '/leader', { studentId });
}

export function getProjectLogs(id) {
    return request.get('/project/' + id + '/logs');
}

export function getProjectScores(id) {
    return request.get('/project/' + id + '/scores');
}

export function getProjectGroupScores(id) {
    return request.get('/project/' + id + '/group-scores');
}

export function exportProjectScores(id) {
    return request.get('/project/' + id + '/scores/export', { responseType: 'blob' });
}