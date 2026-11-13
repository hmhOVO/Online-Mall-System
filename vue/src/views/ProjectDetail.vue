<template>
  <div v-loading="loading" class="project-detail">
    <!-- 页面头部 -->
    <div class="project-header">
      <div class="header-left">
        <el-button @click="goBack" text>
          <el-icon><ArrowLeft /></el-icon>返回
        </el-button>
        <div class="project-title">
          <span class="title-text">{{ project.name }}</span>
          <el-tag :type="statusType(project.status)" size="small">{{ statusLabel(project.status) }}</el-tag>
          <el-tag v-if="project.status === 'DELAYED'" type="danger" size="small" style="margin-left: 8px">已逾期</el-tag>
        </div>
      </div>
      <div class="header-actions">
        <el-button type="primary" size="small" @click="goBoard">
          <el-icon><Grid /></el-icon>进入看板
        </el-button>
        <el-dropdown v-if="canManage && project.status !== 'COMPLETED'" trigger="click">
          <el-button size="small">
            更多操作<el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="statusDialogVisible = true">
                <el-icon><RefreshRight /></el-icon>变更状态
              </el-dropdown-item>
              <el-dropdown-item @click="changeTeacherDialogVisible = true">
                <el-icon><Switch /></el-icon>变更指导教师
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>

    <!-- Tab 标签页 -->
    <el-tabs v-model="activeTab" class="project-tabs" type="border-card">
      <!-- 概览 -->
      <el-tab-pane label="概览" name="overview">
        <div class="tab-content">
          <el-row :gutter="20">
            <el-col :span="16">
              <el-card class="info-card" shadow="never">
                <template #header>
                  <div class="card-header">
                    <span>项目信息</span>
                    <el-button v-if="canManage && project.status !== 'COMPLETED'" link type="primary" @click="openEditDialog">
                      <el-icon><Edit /></el-icon>编辑
                    </el-button>
                  </div>
                </template>
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="项目简介" :span="2">{{ project.description || '无' }}</el-descriptions-item>
                  <el-descriptions-item label="标签">
                    <el-tag v-for="tag in tagList" :key="tag" size="small" style="margin-right: 5px">{{ tag }}</el-tag>
                    <span v-if="!tagList.length">无</span>
                  </el-descriptions-item>
                  <el-descriptions-item label="起止日期">{{ project.startDate }} 至 {{ project.endDate }}</el-descriptions-item>
                  <el-descriptions-item label="实训类型">{{ projectTypeLabel(project.projectType) || '未设置' }}</el-descriptions-item>
                  <el-descriptions-item label="课程名称">{{ project.courseName || '未设置' }}</el-descriptions-item>
                  <el-descriptions-item label="创建人">{{ project.creatorName }}</el-descriptions-item>
                  <el-descriptions-item label="指导教师">{{ project.teacherName || project.ownerName }}</el-descriptions-item>
                  <el-descriptions-item label="创建时间">{{ project.createdAt }}</el-descriptions-item>
                </el-descriptions>
              </el-card>

              <el-row :gutter="20" style="margin-top: 20px">
                <el-col :span="8">
                  <el-card class="stat-card" shadow="never">
                    <div class="stat-title">项目学生</div>
                    <div class="stat-value">{{ (project.members || []).length }} 人</div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card class="stat-card" shadow="never">
                    <div class="stat-title">项目周期</div>
                    <div class="stat-value">{{ projectCycleDays }} 天</div>
                  </el-card>
                </el-col>
                <el-col :span="8">
                  <el-card class="stat-card" shadow="never">
                    <div class="stat-title">剩余天数</div>
                    <div class="stat-value" :class="{ 'text-danger': remainingDays < 0 }">{{ remainingDaysText }}</div>
                  </el-card>
                </el-col>
              </el-row>
            </el-col>

            <el-col :span="8">
              <el-card class="log-card" shadow="never">
                <template #header>
                  <div class="card-header">
                    <span>操作日志</span>
                    <el-button link @click="loadLogs">刷新</el-button>
                  </div>
                </template>
                <div class="log-timeline-container">
                  <el-timeline>
                    <el-timeline-item v-for="log in logs" :key="log.id" :timestamp="log.createdAt">
                      <div style="font-weight: bold">{{ log.operationType }}</div>
                      <div
                        class="log-content"
                        :class="{ expanded: expandedLogs[log.id] }"
                      >{{ formatLogContent(log) }}</div>
                      <el-button
                        v-if="isLongLog(formatLogContent(log))"
                        link
                        type="primary"
                        size="small"
                        style="padding: 0; margin-top: 2px"
                        @click="toggleLog(log.id)"
                      >{{ expandedLogs[log.id] ? '收起' : '展开' }}</el-button>
                    </el-timeline-item>
                    <el-timeline-item v-if="!logs.length">暂无日志</el-timeline-item>
                  </el-timeline>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 任务 -->
      <el-tab-pane label="任务" name="tasks">
        <div class="tab-content">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-card class="entry-card" shadow="never" @click="goBoard">
                <div class="entry-icon">
                  <el-icon><Grid /></el-icon>
                </div>
                <div class="entry-title">任务看板</div>
                <div class="entry-desc">以看板形式查看和管理项目任务</div>
              </el-card>
            </el-col>
            <el-col :span="12">
              <el-card class="entry-card" shadow="never" @click="goGantt">
                <div class="entry-icon">
                  <el-icon><Calendar /></el-icon>
                </div>
                <div class="entry-title">任务甘特图</div>
                <div class="entry-desc">按时间轴查看任务起止日期与进度安排</div>
              </el-card>
            </el-col>
          </el-row>
          <el-row :gutter="20" v-if="canManage || isAdmin" style="margin-top: 20px">
            <el-col :span="12">
              <el-card class="entry-card" shadow="never" @click="goScores">
                <div class="entry-icon">
                  <el-icon><CircleCheck /></el-icon>
                </div>
                <div class="entry-title">成绩汇总</div>
                <div class="entry-desc">查看学生任务评分与平均成绩</div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

      <!-- 项目文件 -->
      <el-tab-pane label="项目文件" name="files">
        <div class="tab-content">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span class="member-title">
                  公共资料
                  <el-tag size="small" type="info" effect="plain">共 {{ projectFiles.length }} 个</el-tag>
                </span>
                <div>
                  <el-button v-if="canManage || isAdmin" type="primary" size="small" @click="triggerProjectFileUpload">
                    <el-icon><UploadFilled /></el-icon>上传文件
                  </el-button>
                  <input
                      ref="projectFileInputRef"
                      type="file"
                      style="display: none"
                      multiple
                      @change="handleProjectFileUpload"
                  />
                </div>
              </div>
            </template>
            <div v-if="projectFiles.length === 0" class="project-file-empty">
              暂无公共文件，教师上传后学生可在此查看和下载
            </div>
            <el-table v-else :data="projectFiles" border>
              <el-table-column label="文件名" min-width="260">
                <template #default="scope">
                  <el-button link type="primary" @click="handlePreviewProjectFile(scope.row)">
                    {{ scope.row.originalName || scope.row.filename }}
                  </el-button>
                </template>
              </el-table-column>
              <el-table-column label="大小" width="110">
                <template #default="scope">{{ formatFileSize(scope.row.fileSize) }}</template>
              </el-table-column>
              <el-table-column prop="uploaderName" label="上传人" width="120" />
              <el-table-column prop="createdAt" label="上传时间" width="180" />
              <el-table-column label="操作" width="160">
                <template #default="scope">
                  <el-button link type="primary" @click="handlePreviewProjectFile(scope.row)">预览</el-button>
                  <el-button link type="primary" @click="handleDownloadProjectFile(scope.row)">下载</el-button>
                  <el-button v-if="canManage || isAdmin" link type="danger" @click="handleDeleteProjectFile(scope.row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 学生 -->
      <el-tab-pane v-if="canManage || isAdmin" label="学生" name="members">
        <div class="tab-content">
          <el-card shadow="never">
            <template #header>
              <div style="display: flex; justify-content: space-between; align-items: center;">
                <span class="member-title">
                  学生列表
                  <el-tag size="small" type="info" effect="plain">共 {{ (project.members || []).length }} 人</el-tag>
                  <span v-if="memberSearchKeyword" class="member-filter-tip">
                    筛选出 {{ filteredMembers.length }} 人
                  </span>
                </span>
                <div style="display: flex; gap: 10px;">
                  <el-input
                      v-model="memberSearchKeyword"
                      placeholder="搜索姓名/邮箱..."
                      style="width: 220px"
                      clearable
                      :prefix-icon="Search"
                      @input="handleMemberSearch"
                  />
                  <el-button v-if="canManage && project.status !== 'COMPLETED'" type="primary" size="small" @click="memberDialogVisible = true">
                    <el-icon><Plus /></el-icon>添加学生
                  </el-button>
                  <el-button v-if="selectedMembers.length > 0" type="danger" size="small" plain @click="batchRemoveMembers">
                    批量移除 ({{ selectedMembers.length }})
                  </el-button>
                </div>
              </div>
            </template>
            <el-table :data="filteredMembers" border :row-key="(row) => row.userId" @selection-change="handleSelectionChange">
              <el-table-column type="selection" width="50" v-if="canManage && project.status !== 'COMPLETED'" />
              <el-table-column prop="username" label="学号" />
              <el-table-column prop="nickname" label="姓名" />
              <el-table-column prop="joinedAt" label="加入时间" />
              <el-table-column v-if="canManage && project.status !== 'COMPLETED'" label="操作" width="120">
                <template #default="scope">
                  <el-button link type="danger" @click="removeMember(scope.row)">移除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 分组 -->
      <el-tab-pane v-if="canManage || isAdmin" label="分组" name="groups">
        <div class="tab-content">
          <GroupManage :project-id="Number(projectId)" />
        </div>
      </el-tab-pane>

      <!-- AI助手 -->
      <el-tab-pane v-if="canManage" label="AI助手" name="ai">
        <div class="tab-content">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card class="ai-card" shadow="never" @click="openSplitDialog">
                <div class="ai-icon">
                  <el-icon><Scissor /></el-icon>
                </div>
                <div class="ai-title">AI 智能拆解</div>
                <div class="ai-desc">输入需求描述，AI 自动拆解为可执行的任务清单</div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card class="ai-card" shadow="never" @click="openRiskAnalyze">
                <div class="ai-icon">
                  <el-icon><Warning /></el-icon>
                </div>
                <div class="ai-title">AI 风险分析</div>
                <div class="ai-desc">基于项目信息分析潜在风险并提供整改建议</div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-tab-pane>

    </el-tabs>

    <el-dialog v-model="statusDialogVisible" title="变更项目状态" width="400px">
      <el-form label-width="80px">
        <el-form-item label="当前状态">
          <el-tag>{{ statusLabel(project.status) }}</el-tag>
        </el-form-item>
        <el-form-item label="新状态">
          <el-select v-model="newStatus" placeholder="请选择新状态" style="width: 100%">
            <el-option v-for="item in allowedStatusList" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <div style="color: #909399; font-size: 12px; margin: 10px 0">
        状态流转规则：未开始 → 进行中 → 已完成；进行中 ↔ 已延期
      </div>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmStatusChange">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="memberDialogVisible" title="添加学生" width="400px">
      <el-form label-width="90px">
        <el-form-item label="学号">
          <el-input v-model="addMemberKeyword" placeholder="输入学号或姓名搜索" @keyup.enter="searchUser" />
        </el-form-item>
        <el-form-item label="搜索结果" v-if="searchUserResult.length">
          <el-radio-group v-model="selectedUserId">
            <el-radio v-for="u in searchUserResult" :key="u.id" :value="u.id">
              {{ u.studentNo || u.username }}（{{ u.nickname || u.username }}）
            </el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddMember">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="changeTeacherDialogVisible" title="变更指导教师" width="420px">
      <el-form label-width="100px">
        <el-form-item label="新指导教师">
          <el-select v-model="selectedNewTeacherId" placeholder="请选择新指导教师" style="width: 100%" filterable>
            <el-option
                v-for="t in teachers.filter(item => item.id !== (project.teacherId || project.ownerId))"
                :key="t.id"
                :label="t.nickname || t.username"
                :value="t.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div style="color: #909399; font-size: 12px; margin: 4px 0">
        项目归属始终属于指导教师，仅在教师之间转移。
      </div>
      <template #footer>
        <el-button @click="changeTeacherDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmChangeTeacher">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editDialogVisible" title="编辑项目信息" width="550px">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="项目名称">
          <el-input v-model="editForm.name" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="项目简介">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="请输入项目简介" />
        </el-form-item>
        <el-form-item label="实训类型">
          <el-select v-model="editForm.projectType" placeholder="请选择实训类型" style="width: 100%">
            <el-option v-for="item in projectTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="课程名称">
          <el-input v-model="editForm.courseName" placeholder="请输入实训课程名称" />
        </el-form-item>
        <el-form-item label="指导教师">
          <el-select v-model="editForm.teacherId" placeholder="请选择指导教师" clearable filterable style="width: 100%">
            <el-option v-for="t in teachers" :key="t.id" :label="t.nickname || t.username" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editForm.tags" placeholder="多个标签用英文逗号分隔" />
        </el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker
              v-model="editForm.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="confirmEdit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="splitDialogVisible" title="AI 智能拆解任务" width="780px">
      <div v-if="splitStep === 1">
        <el-form :model="splitForm" label-width="90px">
          <el-form-item label="需求描述">
            <el-input v-model="splitForm.requirement" type="textarea" :rows="4" placeholder="请描述需要 AI 拆解的功能/需求" />
          </el-form-item>
          <el-form-item label="学生人数">
            <el-input-number v-model="splitForm.studentCount" :min="1" :max="50" style="width: 100%" />
          </el-form-item>
          <el-form-item label="截止日期">
            <el-date-picker v-model="splitForm.deadline" type="date" value-format="YYYY-MM-DD" placeholder="选择截止日期" style="width: 100%" />
          </el-form-item>
        </el-form>
        <div v-if="splitLoading" style="color: #909399; font-size: 13px">
          <el-icon class="is-loading"><Loading /></el-icon>
          AI 正在生成任务，请稍候…
        </div>
      </div>
      <div v-else>
        <div style="margin-bottom: 12px; font-weight: bold; display: flex; justify-content: space-between; align-items: center">
          <span>任务预览（{{ previewTasks.length }} 个），可编辑后导入</span>
          <el-button size="small" type="primary" plain @click="addPreviewTask">添加任务</el-button>
        </div>
        <el-table :data="previewTasks" border size="small">
          <el-table-column type="index" width="45" />
          <el-table-column label="任务标题" min-width="160">
            <template #default="scope">
              <el-input v-model="scope.row.title" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="优先级" width="90">
            <template #default="scope">
              <el-select v-model="scope.row.priority" size="small">
                <el-option label="高" value="HIGH" />
                <el-option label="中" value="MEDIUM" />
                <el-option label="低" value="LOW" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="难度" width="90">
            <template #default="scope">
              <el-select v-model="scope.row.difficulty" size="small">
                <el-option label="简单" value="EASY" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="困难" value="HARD" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="100">
            <template #default="scope">
              <el-select v-model="scope.row.taskType" size="small">
                <el-option label="认知" value="COGNITIVE" />
                <el-option label="实操" value="SKILL" />
                <el-option label="综合" value="COMPREHENSIVE" />
                <el-option label="成果" value="DELIVERABLE" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="计划学时" width="90">
            <template #default="scope">
              <el-input-number v-model="scope.row.plannedHours" :min="0" :precision="1" :step="0.5" size="small" style="width: 80px" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60">
            <template #default="scope">
              <el-button link type="danger" size="small" @click="removePreviewTask(scope.$index)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="handleSplitCancel">取消</el-button>
        <el-button v-if="splitStep === 3" @click="splitStep = 1">上一步</el-button>
        <el-button v-if="splitStep === 3" type="primary" :loading="splitLoading" @click="confirmImport">确认导入</el-button>
        <el-button v-if="splitStep === 1" type="primary" :loading="splitLoading" @click="confirmSplit">开始生成</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="riskDialogVisible" title="AI 风险分析报告" width="700px">
      <div v-loading="riskLoading" class="risk-content">
        <div v-if="parsedRiskReport" class="risk-report-card">
          <div class="risk-level-row">
            <span class="risk-label">风险等级</span>
            <el-tag :type="riskLevelType" size="large" effect="dark">{{ riskLevelText }}</el-tag>
          </div>
          <div class="risk-section">
            <div class="risk-section-title">
              <el-icon><Warning /></el-icon>
              <span>风险项</span>
            </div>
            <ul class="risk-list">
              <li v-for="(item, index) in parsedRiskReport.riskItems" :key="'risk-' + index">
                <span class="risk-index">{{ index + 1 }}</span>
                <span>{{ item }}</span>
              </li>
            </ul>
          </div>
          <div class="risk-section">
            <div class="risk-section-title">
              <el-icon><CircleCheck /></el-icon>
              <span>整改建议</span>
            </div>
            <ul class="risk-list suggestion-list">
              <li v-for="(item, index) in parsedRiskReport.suggestions" :key="'sug-' + index">
                <span class="risk-index sug-index">{{ index + 1 }}</span>
                <span>{{ item }}</span>
              </li>
            </ul>
          </div>
        </div>
        <pre v-else>{{ riskReport }}</pre>
      </div>
      <template #footer>
        <el-button @click="riskDialogVisible = false">关闭</el-button>
        <el-button type="primary" :disabled="!riskReport" @click="exportRisk">导出</el-button>
      </template>
    </el-dialog>

    <el-dialog
        v-model="previewDialogVisible"
        :title="previewTitle"
        width="70%"
        top="5vh"
        destroy-on-close
        @closed="releasePreview"
    >
      <div v-loading="previewLoading" class="file-preview-body">
        <img v-if="previewKind === 'image' && previewUrl" :src="previewUrl" class="file-preview-image" alt="预览" />
        <iframe v-else-if="previewUrl" :src="previewUrl" class="file-preview-frame"></iframe>
        <div v-else class="project-file-empty">暂无可预览内容</div>
      </div>
    </el-dialog>

  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, onUnmounted} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import request from '@/utils/request.js';
import { streamSplitTask, confirmSplitTask, streamRiskAnalyze } from '@/api/ai.js';
import GroupManage from './GroupManage.vue';
import { updateProjectStatus, completeProject, getUnfinishedTaskCount, transferProject } from '@/api/project.js';
import { listTeachers } from '@/api/user.js';
import { listGroups } from '@/api/training.js';
import { listProjectFiles, uploadProjectFile, downloadFile, previewFile, deleteFile } from '@/api/file.js';
import {
  UploadFilled, Loading, Warning, CircleCheck,
  ArrowLeft, ArrowDown, Grid, RefreshRight, Plus, Switch, Search,
  Scissor, Picture, Edit, Calendar
} from '@element-plus/icons-vue';
import { useProjectPermission } from '@/composables/useProjectPermission.js';

const route = useRoute();
const activeTab = ref('overview');
const router = useRouter();
const projectId = route.params.id;

if (!/^\d+$/.test(projectId)) {
  router.replace('/notFound');
}

const userStr = localStorage.getItem('code_user');
const currentUser = userStr ? JSON.parse(userStr) : {};
const basePath = computed(() => {
  if (currentUser.role === 'ADMIN') return '/admin';
  if (currentUser.role === 'TEACHER') return '/teacher';
  return '/student';
});

const loading = ref(false);
const project = reactive({
  id: null,
  name: '',
  description: '',
  tags: '',
  startDate: '',
  endDate: '',
  status: 'NOT_STARTED',
  creatorId: null,
  ownerId: null,
  creatorName: '',
  ownerName: '',
  members: []
});
const logs = ref([]);

const projectRef = ref(project);
const { canManage, isAdmin } = useProjectPermission(projectRef, null, currentUser);

const statusDialogVisible = ref(false);
const newStatus = ref('');

const memberDialogVisible = ref(false);
const addMemberKeyword = ref('');
const searchUserResult = ref([]);
const selectedUserId = ref(null);
const memberSearchKeyword = ref('');
const selectedMembers = ref([]);

// ===== 项目公共文件 =====
const projectFiles = ref([]);
const projectFileInputRef = ref(null);
const previewDialogVisible = ref(false);
const previewLoading = ref(false);
const previewUrl = ref('');
const previewTitle = ref('');
const previewKind = ref('file');
const filteredMembers = computed(() => {
  if (!memberSearchKeyword.value) return project.members || [];
  const keyword = memberSearchKeyword.value.toLowerCase();
  return (project.members || []).filter(m => 
    (m.username && m.username.toLowerCase().includes(keyword)) ||
    (m.nickname && m.nickname.toLowerCase().includes(keyword)) ||
    (m.email && m.email.toLowerCase().includes(keyword))
  );
});

// ===== 变更指导教师 / 编辑项目 =====
const changeTeacherDialogVisible = ref(false);
const selectedNewTeacherId = ref(null);

const editDialogVisible = ref(false);
const editLoading = ref(false);
const editForm = reactive({
  name: '',
  description: '',
  projectType: 'COURSE_TRAINING',
  courseName: '',
  teacherId: null,
  tags: '',
  dateRange: []
});

const teachers = ref([]);
const projectTypeOptions = [
  {label: '课程实训', value: 'COURSE_TRAINING'},
  {label: '课程设计', value: 'COURSE_DESIGN'}
];
const projectTypeLabel = (type) => {
  const item = projectTypeOptions.find(t => t.value === type);
  return item ? item.label : (type || '');
};
const loadTeachers = () => {
  listTeachers().then(res => { teachers.value = res.data || []; });
};

const splitDialogVisible = ref(false);
const splitLoading = ref(false);
const splitStep = ref(1);
const splitForm = reactive({ requirement: '', studentCount: 3, deadline: '' });
const previewTasks = ref([]);
let splitAbortController = null;

const riskDialogVisible = ref(false);
const riskLoading = ref(false);
const riskReport = ref('');
let riskAbortController = null;

const parsedRiskReport = computed(() => {
  if (!riskReport.value || riskReport.value.trim() === '') return null;
  try {
    const json = JSON.parse(riskReport.value);
    if (json.riskLevel && Array.isArray(json.riskItems) && Array.isArray(json.suggestions)) {
      return json;
    }
    return null;
  } catch (e) {
    return null;
  }
});

const riskLevelType = computed(() => {
  const level = parsedRiskReport.value?.riskLevel;
  if (level === 'HIGH') return 'danger';
  if (level === 'MIDDLE') return 'warning';
  return 'success';
});

const riskLevelText = computed(() => {
  const level = parsedRiskReport.value?.riskLevel;
  if (level === 'HIGH') return '高风险';
  if (level === 'MIDDLE') return '中风险';
  return '低风险';
});

const statusLabel = (status) => {
  const map = {NOT_STARTED: '未开始', IN_PROGRESS: '进行中', DELAYED: '已延期', COMPLETED: '已完成'};
  return map[status] || status;
};

const statusType = (status) => {
  const map = {NOT_STARTED: 'info', IN_PROGRESS: 'primary', DELAYED: 'danger', COMPLETED: 'success'};
  return map[status] || 'info';
};

const tagList = computed(() => project.tags ? project.tags.split(',').filter(t => t) : []);

const projectCycleDays = computed(() => {
  if (!project.startDate || !project.endDate) return 0;
  const start = new Date(project.startDate);
  const end = new Date(project.endDate);
  const diff = Math.ceil((end - start) / (1000 * 60 * 60 * 24));
  return diff >= 0 ? diff : 0;
});

const remainingDays = computed(() => {
  if (!project.endDate) return 0;
  const end = new Date(project.endDate);
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  end.setHours(0, 0, 0, 0);
  return Math.ceil((end - today) / (1000 * 60 * 60 * 24));
});

const remainingDaysText = computed(() => {
  const days = remainingDays.value;
  if (days < 0) return `逾期 ${Math.abs(days)} 天`;
  if (days === 0) return '今天截止';
  return `${days} 天`;
});

const allowedStatusList = computed(() => {
  const current = project.status;
  const all = [
    {label: '未开始', value: 'NOT_STARTED'},
    {label: '进行中', value: 'IN_PROGRESS'},
    {label: '已延期', value: 'DELAYED'},
    {label: '已完成', value: 'COMPLETED'}
  ];
  if (current === 'NOT_STARTED') return all.filter(i => i.value === 'IN_PROGRESS');
  if (current === 'IN_PROGRESS') return all.filter(i => i.value === 'DELAYED' || i.value === 'COMPLETED');
  if (current === 'DELAYED') return all.filter(i => i.value === 'IN_PROGRESS' || i.value === 'COMPLETED');
  return [];
});

const loadData = () => {
  loading.value = true;
  request.get('/project/' + projectId).then(res => {
    Object.assign(project, res.data);
    loadLogs();
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const loadLogs = () => {
  request.get('/project/' + projectId + '/logs').then(res => {
    logs.value = res.data || [];
  }).catch(() => {
    logs.value = [];
  });
};

const loadProjectFiles = () => {
  listProjectFiles(projectId).then(res => {
    projectFiles.value = res.data || [];
  }).catch(() => {
    projectFiles.value = [];
  });
};

const triggerProjectFileUpload = () => {
  projectFileInputRef.value?.click();
};

const handleProjectFileUpload = (event) => {
  const files = Array.from(event.target.files || []);
  if (!files.length) return;
  Promise.all(files.map(file => uploadProjectFile(projectId, file)))
      .then(() => {
        ElMessage.success('文件上传成功');
        loadProjectFiles();
      })
      .finally(() => {
        event.target.value = '';
      });
};

const handleDownloadProjectFile = (file) => {
  downloadFile(file.id).then(res => {
    const url = window.URL.createObjectURL(new Blob([res]));
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', file.originalName || file.filename || 'download');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }).catch(() => {
    ElMessage.error('文件下载失败');
  });
};

const handlePreviewProjectFile = (file) => {
  previewTitle.value = file.originalName || file.filename || '文件预览';
  previewKind.value = (file.mimeType || '').startsWith('image/') ? 'image' : 'file';
  previewUrl.value = '';
  previewLoading.value = true;
  previewDialogVisible.value = true;
  previewFile(file.id).then(res => {
    const type = previewKind.value === 'image' ? file.mimeType : 'application/pdf';
    previewUrl.value = window.URL.createObjectURL(new Blob([res], { type }));
  }).catch(() => {
    previewDialogVisible.value = false;
    ElMessage.error('文件预览失败');
  }).finally(() => {
    previewLoading.value = false;
  });
};

const releasePreview = () => {
  if (previewUrl.value) {
    window.URL.revokeObjectURL(previewUrl.value);
    previewUrl.value = '';
  }
};

const handleDeleteProjectFile = (file) => {
  const name = file.originalName || file.filename;
  ElMessageBox.confirm('确定删除公共文件「' + name + '」吗？', '提示', { type: 'warning' }).then(() => {
    deleteFile(file.id).then(() => {
      ElMessage.success('删除成功');
      loadProjectFiles();
    });
  });
};

const formatFileSize = (size) => {
  if (!size) return '';
  if (size < 1024) return size + ' B';
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB';
  return (size / (1024 * 1024)).toFixed(1) + ' MB';
};

const expandedLogs = ref({});

const isLongLog = (text) => !!text && text.length > 100;

const toggleLog = (id) => {
  expandedLogs.value[id] = !expandedLogs.value[id];
};

const formatLogContent = (log) => {
  const username = log.username || '系统';
  if (!log.params) {
    return `${username} 执行了该操作`;
  }
  try {
    const params = JSON.parse(log.params);
    const type = log.operationType;
    const payload = Array.isArray(params) && params.length > 0 ? params[params.length - 1] : params;

    if (type === '添加成员' && payload?.userId !== undefined) {
      return `${username} 添加成员: ${payload.userId}`;
    }
    if (type === '移除成员' && payload?.userId !== undefined) {
      return `${username} 移除成员: ${payload.userId}`;
    }
    if (type === 'AI风险分析' && payload?.riskLevel) {
      const summary = JSON.stringify({
        riskLevel: payload.riskLevel,
        riskPoints: payload.riskPoints,
        suggestions: payload.suggestions
      });
      return `${username} AI风险分析: ${summary}`;
    }
    if (type === 'AI任务拆解确认' && Array.isArray(params) && params[0]?.tasks) {
      const taskCount = params[0].tasks.length;
      return `${username} 确认生成 ${taskCount} 个子任务`;
    }
    if (type === 'AI任务拆解预览' && Array.isArray(params) && params[0]?.requirement) {
      return `${username} 预览任务拆解：${params[0].requirement}`;
    }
    if (type === '创建任务' && payload?.title) {
      return `${username} 创建任务: ${payload.title}`;
    }
    if (type === '编辑任务' && payload?.title) {
      return `${username} 编辑任务: ${payload.title}`;
    }
    if (type === '删除任务') {
      return `${username} 删除任务 ID ${payload || params[0]}`;
    }
    if (type === '变更项目状态' && payload) {
      return `${username} 变更项目状态为: ${statusLabel(payload)}`;
    }
    if (type === '变更指导教师') {
      return `${username} 变更了指导教师`;
    }
    if (type === '上传图片' && payload?.fileId !== undefined) {
      return `${username} 上传图片/文件`;
    }
    if (type === '删除图片/文件') {
      return `${username} 删除图片/文件 ID ${payload || params[0]}`;
    }

    return `${username}: ${JSON.stringify(params)}`;
  } catch (e) {
    return `${username}: ${log.params}`;
  }
};

let ws = null;

const connectWebSocket = () => {
  const userStr = localStorage.getItem('code_user');
  if (!userStr) return;
  
  let token = '';
  try {
    const user = JSON.parse(userStr);
    token = user.token || '';
  } catch (e) {
    console.error('解析用户信息失败 ', e);
    return;
  }
  
  if (!token) return;
  
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const host = window.location.host;
  ws = new WebSocket(`${protocol}//${host}/ws/notification?token=${token}`);
  
  ws.onopen = () => {
    console.log('WebSocket 连接成功');
  };
  
  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data);
      if (data.type === 'LOG_UPDATE' && data.projectId === projectId) {
        loadLogs();
      }
    } catch (e) {
      console.error('WebSocket 消息解析失败', e);
    }
  };
  
  ws.onerror = () => {
    console.warn('WebSocket 连接错误');
  };
  
  ws.onclose = () => {
    console.log('WebSocket 连接关闭');
  };
};

const goBack = () => {
  router.push(basePath.value + '/project');
};

const goBoard = () => {
  router.push('/project/' + projectId + '/board');
};

const goGantt = () => {
  router.push('/project/' + projectId + '/gantt');
};

const goScores = () => {
  router.push('/project/' + projectId + '/scores');
};

const confirmStatusChange = () => {
  if (!newStatus.value) {
    ElMessage.warning('请选择新状态');
    return;
  }
  const doChange = () => {
    updateProjectStatus(projectId, newStatus.value).then(() => {
      ElMessage.success('状态变更成功');
      statusDialogVisible.value = false;
      newStatus.value = '';
      loadData();
    });
  };

  if (newStatus.value === 'COMPLETED') {
    getUnfinishedTaskCount(projectId).then(res => {
      const count = res.data || 0;
      if (count > 0) {
        ElMessageBox.confirm(
          `该项目还有 ${count} 个未完成任务，是否将所有未完成任务标记为已完成并结项？`,
          '项目结项确认',
          { type: 'warning', confirmButtonText: '全部完成并结项', cancelButtonText: '取消' }
        ).then(() => {
          completeProject(projectId).then(() => {
            ElMessage.success('项目已结项，所有未完成任务已标记为已完成');
            statusDialogVisible.value = false;
            newStatus.value = '';
            loadData();
          });
        });
      } else {
        ElMessageBox.confirm('确定将项目状态变更为「已完成」吗？', '提示', { type: 'warning' }).then(doChange);
      }
    });
  } else {
    ElMessageBox.confirm('确定将项目状态变更为「' + statusLabel(newStatus.value) + '」吗？', '提示', { type: 'warning' }).then(doChange);
  }
};

const restartProject = () => {
  ElMessageBox.confirm('确定重启该项目吗？', '提示', {type: 'warning'}).then(() => {
    request.put('/project/' + projectId + '/restart').then(() => {
      ElMessage.success('项目已重启');
      loadData();
    });
  });
};

const searchUser = () => {
  if (!addMemberKeyword.value) return;
  request.get('/user/search', {
    params: {pageNum: 1, pageSize: 20, keyword: addMemberKeyword.value, role: 'STUDENT'}
  }).then(res => {
    searchUserResult.value = res.data.records || [];
    if (!searchUserResult.value.length) {
      ElMessage.info('未找到该学生');
    }
  }).catch(() => {
    searchUserResult.value = [];
  });
};

const handleMemberSearch = () => {};

const handleSelectionChange = (val) => {
  selectedMembers.value = val;
};

const batchRemoveMembers = () => {
  if (selectedMembers.value.length === 0) return;
  const names = selectedMembers.value.map(m => m.nickname || m.username).join('、');
  ElMessageBox.confirm('确定批量移除以下学生：' + names + '？', '提示', {type: 'warning'}).then(() => {
    const promises = selectedMembers.value.map(m => 
      request.delete('/project/' + projectId + '/members/' + m.userId)
    );
    Promise.all(promises).then(() => {
      ElMessage.success('批量移除成功');
      selectedMembers.value = [];
      loadData();
    }).catch(() => {
      ElMessage.error('部分学生移除失败');
      loadData();
    });
  });
};

const confirmAddMember = () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请选择用户');
    return;
  }
  request.post('/project/' + projectId + '/members', {
    userId: selectedUserId.value
  }).then(() => {
    ElMessage.success('添加成功');
    memberDialogVisible.value = false;
    addMemberKeyword.value = '';
    searchUserResult.value = [];
    selectedUserId.value = null;
    loadData();
  });
};

const removeMember = (row) => {
  ElMessageBox.confirm('确定移除学生「' + (row.nickname || row.username) + '」吗？', '提示', {type: 'warning'}).then(() => {
    request.delete('/project/' + projectId + '/members/' + row.userId).then(() => {
      ElMessage.success('移除成功');
      loadData();
    });
  });
};

const confirmChangeTeacher = () => {
  if (!selectedNewTeacherId.value) {
    ElMessage.warning('请选择新指导教师');
    return;
  }
  transferProject(projectId, selectedNewTeacherId.value).then(() => {
    ElMessage.success('指导教师变更成功');
    changeTeacherDialogVisible.value = false;
    selectedNewTeacherId.value = null;
    loadData();
  });
};

const openEditDialog = () => {
  editForm.name = project.name;
  editForm.description = project.description || '';
  editForm.projectType = project.projectType || 'COURSE_TRAINING';
  editForm.courseName = project.courseName || '';
  editForm.teacherId = project.teacherId || null;
  editForm.tags = project.tags || '';
  editForm.dateRange = [project.startDate, project.endDate];
  editDialogVisible.value = true;
};

const confirmEdit = () => {
  if (!editForm.name || !editForm.name.trim()) {
    ElMessage.warning('项目名称不能为空');
    return;
  }
  if (!editForm.dateRange || editForm.dateRange.length !== 2) {
    ElMessage.warning('请选择起止日期');
    return;
  }
  editLoading.value = true;
  request.put('/project/' + projectId, {
    name: editForm.name.trim(),
    description: editForm.description,
    projectType: editForm.projectType,
    courseName: editForm.courseName,
    teacherId: editForm.teacherId,
    tags: editForm.tags,
    startDate: editForm.dateRange[0],
    endDate: editForm.dateRange[1]
  }).then(() => {
    ElMessage.success('项目信息更新成功');
    editDialogVisible.value = false;
    loadData();
  }).finally(() => {
    editLoading.value = false;
  });
};

const openSplitDialog = async () => {
  splitStep.value = 1;
  previewTasks.value = [];
  splitForm.requirement = '';
  splitForm.studentCount = 3;
  splitForm.deadline = '';
  splitAbortController = null;
  splitDialogVisible.value = true;
  // 默认按已有分组的平均人数填充
  try {
    const res = await listGroups(projectId);
    const settled = (res.data || []).filter(g => (g.memberCount || 0) > 0);
    if (settled.length > 0) {
      const total = settled.reduce((sum, g) => sum + (g.memberCount || 0), 0);
      const avg = Math.round(total / settled.length);
      splitForm.studentCount = Math.min(50, Math.max(1, avg));
    }
  } catch (e) {
    // 分组读取失败时保持默认值
  }
};

const handleSplitCancel = () => {
  if (splitAbortController) {
    splitAbortController.abort();
    splitAbortController = null;
  }
  splitDialogVisible.value = false;
};

const confirmSplit = () => {
  if (!splitForm.requirement.trim()) {
    ElMessage.warning('请输入需求描述');
    return;
  }
  splitLoading.value = true;
  splitAbortController = new AbortController();

  streamSplitTask(projectId, {
    requirement: splitForm.requirement,
    studentCount: splitForm.studentCount,
    deadline: splitForm.deadline,
    abortSignal: splitAbortController.signal,
    onResult: (tasks) => {
      previewTasks.value = tasks || [];
      splitStep.value = 3;
    },
    onError: (err) => {
      ElMessage.error(err || 'AI 生成失败');
      splitStep.value = 1;
    },
    onDone: () => {
      splitLoading.value = false;
      splitAbortController = null;
    }
  });
};

const removePreviewTask = (index) => {
  previewTasks.value.splice(index, 1);
};

const addPreviewTask = () => {
  previewTasks.value.push({
    title: '',
    priority: 'MEDIUM',
    difficulty: 'MEDIUM',
    taskType: 'SKILL',
    plannedHours: 2
  });
};

const confirmImport = () => {
  if (previewTasks.value.length === 0) {
    ElMessage.warning('没有可导入的任务');
    return;
  }
  const invalid = previewTasks.value.some(t => !t.title || !t.title.trim());
  if (invalid) {
    ElMessage.warning('请填写所有任务标题');
    return;
  }
  splitLoading.value = true;
  confirmSplitTask({
    projectId,
    tasks: previewTasks.value.map(t => ({
      title: t.title,
      description: t.description,
      priority: t.priority,
      plannedHours: t.plannedHours,
      taskType: t.taskType,
      difficulty: t.difficulty,
      taskLevel: t.taskLevel || 1,
      startDate: t.startDate,
      endDate: t.endDate,
      dependencyIds: t.dependencyIds || []
    }))
  }).then(res => {
    ElMessage.success('已导入 ' + (res.data ? res.data.length : 0) + ' 个任务');
    splitDialogVisible.value = false;
    splitStep.value = 1;
    previewTasks.value = [];
    router.push('/project/' + projectId + '/board');
  }).finally(() => {
    splitLoading.value = false;
  });
};

const openRiskAnalyze = () => {
  riskReport.value = '';
  riskDialogVisible.value = true;
  riskLoading.value = true;
  if (riskAbortController) riskAbortController.abort();
  riskAbortController = new AbortController();
  streamRiskAnalyze(projectId, {
    abortSignal: riskAbortController.signal,
    onMessage: (chunk) => {
      riskReport.value += chunk;
    },
    onError: (msg) => {
      riskLoading.value = false;
      ElMessage.error(msg);
    },
    onDone: () => {
      riskLoading.value = false;
    }
  });
};

const exportRisk = () => {
  let content = '';
  if (parsedRiskReport.value) {
    content = `# AI 风险分析报告\n\n## 风险等级\n\n${riskLevelText.value}\n\n## 风险项\n\n`;
    parsedRiskReport.value.riskItems.forEach(item => {
      content += `- ${item}\n`;
    });
    content += '\n## 整改建议\n\n';
    parsedRiskReport.value.suggestions.forEach(item => {
      content += `- ${item}\n`;
    });
  } else {
    content = riskReport.value;
  }
  const blob = new Blob([content], { type: 'text/markdown' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = '风险分析报告.md';
  link.click();
};

onMounted(() => {
  loadData();
  loadTeachers();
  loadProjectFiles();
  connectWebSocket();
});

onUnmounted(() => {
  if (ws) {
    ws.close();
    ws = null;
  }
});
</script>

<style scoped>
.project-detail {
  padding: 20px;
}

.member-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.project-file-empty {
  padding: 24px 0;
  text-align: center;
  color: #909399;
  font-size: 14px;
}

.file-preview-body {
  min-height: 300px;
}

.file-preview-image {
  display: block;
  max-width: 100%;
  max-height: 70vh;
  margin: 0 auto;
}

.file-preview-frame {
  width: 100%;
  height: 70vh;
  border: none;
}

.member-filter-tip {
  font-size: 12px;
  color: #909399;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.project-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.title-text {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.project-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.project-tabs :deep(.el-tabs__content) {
  padding: 20px;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-top: none;
  border-radius: 0 0 4px 4px;
}

.tab-content {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.info-card {
  margin-bottom: 0;
}

.stat-card {
  text-align: center;
  cursor: default;
}

.stat-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 10px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-value.text-danger {
  color: #F56C6C;
}

.log-card {
  height: 100%;
}

.log-timeline-container {
  max-height: 500px;
  overflow-y: auto;
  padding-right: 8px;
}

.log-content {
  font-size: 12px;
  color: #666;
  line-height: 1.5;
  word-break: break-all;
}

.log-content:not(.expanded) {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.entry-card {
  text-align: center;
  padding: 24px 20px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.entry-card:hover {
  border-color: #c0c4cc;
}

.entry-icon,
.ai-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #606266;
  margin-bottom: 10px;
}

.entry-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.entry-desc {
  font-size: 13px;
  color: #909399;
}

.ai-card {
  text-align: center;
  padding: 24px 20px;
  cursor: pointer;
  transition: border-color 0.2s;
}

.ai-card:hover {
  border-color: #c0c4cc;
}

.ai-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.ai-desc {
  font-size: 13px;
  color: #909399;
  line-height: 1.6;
}

.risk-content pre {
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: inherit;
  line-height: 1.8;
}
.risk-report-card {
  padding: 8px 4px;
}
.risk-level-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}
.risk-label {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
}
.risk-section {
  margin-bottom: 20px;
}
.risk-section-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}
.risk-section-title .el-icon {
  color: #f56c6c;
}
.risk-section:nth-of-type(3) .risk-section-title .el-icon {
  color: #67c23a;
}
.risk-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.risk-list li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px 12px;
  margin-bottom: 8px;
  background: #f5f7fa;
  border-radius: 6px;
  line-height: 1.6;
  color: #606266;
}
.suggestion-list li {
  background: #f0f9eb;
}
.risk-index {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fde2e2;
  color: #f56c6c;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
}
.sug-index {
  background: #e1f3d8;
  color: #67c23a;
}
</style>
