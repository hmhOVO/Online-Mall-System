<template>
  <div class="course-page" v-loading="loading">
    <div class="page-header">
      <div class="page-title">
        <el-icon><Reading /></el-icon>
        <span>我的课程</span>
        <span class="page-sub">共 {{ courses.length }} 门</span>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="loadCourses">刷新</el-button>
      </div>
    </div>

    <el-empty v-if="!loading && courses.length === 0" description="暂无授课课程，请联系管理员在「课程管理」中分配" />

    <div v-else class="course-grid">
      <div v-for="course in courses" :key="course.id" class="course-card" @click="openCourse(course)">
        <div class="course-top">
          <span class="course-name">{{ course.name }}</span>
          <el-tag size="small" :type="course.status === 'ENABLED' ? 'success' : 'info'">
            {{ course.status === 'ENABLED' ? '进行中' : '已停用' }}
          </el-tag>
        </div>
        <div class="course-meta">
          <span>{{ course.term || '未设置学期' }}</span>
          <span class="dot">·</span>
          <span>{{ course.className || '未绑定班级' }}</span>
        </div>
        <div class="course-stats">
          <div class="stat">
            <span class="num">{{ course.projectCount }}</span>
            <span class="label">实训项目</span>
          </div>
          <div class="stat">
            <span class="num">{{ course.studentCount }}</span>
            <span class="label">班级学生</span>
          </div>
        </div>
        <div class="course-foot">查看项目</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {Reading, Refresh} from '@element-plus/icons-vue';
import {getMyCourses} from '@/api/training.js';

const router = useRouter();
const loading = ref(false);
const courses = ref([]);

const loadCourses = () => {
  loading.value = true;
  getMyCourses().then(res => {
    courses.value = res.data || [];
  }).finally(() => loading.value = false);
};

const openCourse = (course) => {
  router.push({path: '/teacher/project', query: {courseId: course.id}});
};

onMounted(loadCourses);
</script>

<style scoped>
.course-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
}

.page-sub {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.course-card {
  padding: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.course-card:hover {
  border-color: #409eff;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.12);
}

.course-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.course-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-meta {
  margin-top: 8px;
  font-size: 13px;
  color: #909399;
}

.dot {
  margin: 0 6px;
}

.course-stats {
  display: flex;
  gap: 32px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
}

.stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.num {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.label {
  font-size: 12px;
  color: #909399;
}

.course-foot {
  margin-top: 12px;
  font-size: 13px;
  color: #409eff;
}
</style>
