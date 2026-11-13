import request from '@/utils/request.js';

// ===== 学院 =====
export function listAllSchools() {
    return request.get('/school/list');
}

export function listSchools(params) {
    return request.get('/admin/school/list', { params });
}

export function createSchool(data) {
    return request.post('/admin/school', data);
}

export function updateSchool(id, data) {
    return request.put('/admin/school/' + id, data);
}

export function deleteSchool(id) {
    return request.delete('/admin/school/' + id);
}

// ===== 专业 =====
export function listAllMajors(schoolId) {
    return request.get('/major/list', { params: { schoolId } });
}

export function listMajors(params) {
    return request.get('/admin/major/list', { params });
}

export function createMajor(data) {
    return request.post('/admin/major', data);
}

export function updateMajor(id, data) {
    return request.put('/admin/major/' + id, data);
}

export function deleteMajor(id) {
    return request.delete('/admin/major/' + id);
}

// ===== 班级 =====
export function listAllClasses(majorId) {
    return request.get('/class/list', { params: { majorId } });
}

export function listClasses(params) {
    return request.get('/admin/class/list', { params });
}

export function createClass(data) {
    return request.post('/admin/class', data);
}

export function updateClass(id, data) {
    return request.put('/admin/class/' + id, data);
}

export function deleteClass(id) {
    return request.delete('/admin/class/' + id);
}

// ===== 教学班（课程） =====
export function listAllCourses(teacherId) {
    return request.get('/course/list', { params: { teacherId } });
}

export function listCourses(params) {
    return request.get('/admin/course/list', { params });
}

export function createCourse(data) {
    return request.post('/admin/course', data);
}

export function updateCourse(id, data) {
    return request.put('/admin/course/' + id, data);
}

export function deleteCourse(id) {
    return request.delete('/admin/course/' + id);
}

// 教师端「我的课程」：本人授课课程 + 项目数/学生数
export function getMyCourses() {
    return request.get('/course/my');
}

// ===== 实训分组 =====
export function listGroups(projectId) {
    return request.get('/project/' + projectId + '/groups');
}

export function listGroupMembers(groupId) {
    return request.get('/group/' + groupId + '/members');
}

export function createGroup(data) {
    return request.post('/group', data);
}

export function updateGroup(id, data) {
    return request.put('/group/' + id, data);
}

export function deleteGroup(id) {
    return request.delete('/group/' + id);
}

export function addGroupMembers(groupId, studentIds) {
    return request.post('/group/' + groupId + '/member', { studentIds });
}

export function removeGroupMember(groupId, studentId) {
    return request.delete('/group/' + groupId + '/member/' + studentId);
}

// ===== 批量创建分组 =====
export function batchCreateGroups(projectId, data) {
    return request.post('/project/' + projectId + '/groups/batch', data);
}

export function joinGroup(groupId) {
    return request.post('/group/' + groupId + '/join');
}

export function leaveGroup(groupId) {
    return request.delete('/group/' + groupId + '/leave');
}

export function renameGroup(groupId, name) {
    return request.put('/group/' + groupId + '/name', { name });
}

// ===== 小组讨论 =====
export function listGroupMessages(groupId) {
    return request.get('/group/' + groupId + '/messages');
}

export function sendGroupMessage(groupId, content) {
    return request.post('/group/' + groupId + '/messages', { content });
}