import { computed } from 'vue';

export function useProjectPermission(project, members, currentUser) {
    const isAdmin = computed(() => currentUser.role === 'ADMIN');

    const canView = computed(() => {
        if (isAdmin.value) {
            return true;
        }
        const projectData = project.value !== undefined ? project.value : project;
        if (projectData && projectData.ownerId === currentUser.id) {
            return true;
        }
        if (projectData && projectData.teacherId === currentUser.id) {
            return true;
        }
        const memberList = members?.value || projectData?.members || [];
        return memberList.some(m => m.userId === currentUser.id);
    });

    const canManage = computed(() => {
        if (isAdmin.value) {
            return false;
        }
        const projectData = project.value !== undefined ? project.value : project;
        // 项目级管理权只属于指导教师，组长职责下沉到小组（student_group.leader_id）
        return !!(projectData && projectData.teacherId === currentUser.id);
    });

    return { canView, canManage, isAdmin };
}