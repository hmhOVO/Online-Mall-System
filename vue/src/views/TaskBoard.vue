<template>
  <div v-loading="loading" class="task-board">
    <el-card class="board-card">
      <div class="board-header">
        <div class="header-left">
          <el-button size="small" @click="goBack">返回项目</el-button>
          <span class="project-title">{{ project.name }}</span>
          <el-tag type="info">{{ viewModeLabel }}</el-tag>
        </div>
        <div class="header-right">
          <el-badge :value="overdueCount" :hidden="overdueCount === 0" class="notice-badge">
            <el-icon :size="22" class="bell-icon" @click="noticeVisible = true"><Bell /></el-icon>
          </el-badge>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="board">状态看板</el-radio-button>
            <el-radio-button value="group">分组看板</el-radio-button>
            <el-radio-button v-if="canManageProject" value="task">按任务</el-radio-button>
            <el-radio-button value="list">列表</el-radio-button>
          </el-radio-group>
          <el-button v-if="isProjectMember && !isProjectCompleted" type="primary" size="small" @click="openCreateDialog">新建任务</el-button>
        </div>
      </div>

      <div class="filter-bar">
        <el-input
          v-model="filter.keyword"
          placeholder="搜索任务标题或描述"
          style="width: 220px"
          prefix-icon="Search"
          clearable
          @keyup.enter="loadTasks"
        />
        <el-select v-model="filter.statuses" multiple collapse-tags placeholder="状态" style="width: 180px">
          <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filter.priorities" multiple collapse-tags placeholder="优先级" style="width: 180px">
          <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="filter.assigneeId" clearable placeholder="负责人" style="width: 160px">
          <el-option v-for="item in memberOptions" :key="item.userId" :label="item.nickname || item.username" :value="item.userId" />
        </el-select>
        <el-checkbox v-model="filter.onlyOverdue">仅显示逾期</el-checkbox>
        <el-button size="small" @click="resetFilter">重置</el-button>
        <el-button size="small" type="primary" @click="loadTasks">查询</el-button>
      </div>
    </el-card>

    <!-- 看板视图 -->
    <div v-if="viewMode === 'board'" class="kanban">
      <div v-for="col in columns" :key="col.status" class="kanban-col">
        <div class="col-header" :class="'col-' + col.status.toLowerCase()">
          <span>{{ col.label }}</span>
          <span class="col-count">{{ boardLists[col.status].length }}</span>
        </div>
        <draggable
          :list="boardLists[col.status]"
          group="tasks"
          item-key="id"
          class="col-body"
          :data-status="col.status"
          ghost-class="ghost-card"
          :move="onMove"
          @start="dragStart"
          @add="dragAdd(col.status, $event)"
        >
          <template #item="{ element }">
            <div class="task-card" :class="{ overdue: element.overdue, unclaimed: !element.assigneeId, blocked: element.status === 'BLOCKED' }" @click="openDetail(element)">
              <div class="card-header-row">
                <div class="card-title">{{ element.title }}</div>
              </div>
              <div class="card-meta">
                <el-tooltip :content="element.assigneeName || '待认领'" placement="top">
                  <el-avatar :size="24" class="card-avatar" :class="{ unclaimed: !element.assigneeId }">{{ avatarText(element.assigneeName) }}</el-avatar>
                </el-tooltip>
                <el-tag :type="priorityType(element.priority)" size="small">{{ priorityLabel(element.priority) }}</el-tag>
              </div>
              <div class="card-tags">
                <el-tag v-if="element.taskType" size="small" type="info" effect="plain">{{ taskTypeLabel(element.taskType) }}</el-tag>
                <el-tag v-if="element.difficulty" size="small" :type="difficultyType(element.difficulty)" effect="plain">{{ difficultyLabel(element.difficulty) }}</el-tag>
                <el-tag v-if="!element.assigneeId && element.groupId" size="small" type="warning" effect="plain">{{ groupNameById(element.groupId) }}任务池</el-tag>
              </div>
              <el-progress :percentage="element.progress || 0" :stroke-width="6" :show-text="false" />
              <div v-if="!isProjectCompleted" class="claim-bar">
                <el-button v-if="canClaim(element)" type="primary" size="small" @click.stop="handleClaim(element)">认领</el-button>
                <el-button v-if="canAssignGroupTask(element)" type="warning" size="small" @click.stop="openEditDialog(element)">指派</el-button>
                <el-button v-if="canUnblock(element)" type="success" size="small" @click.stop="handleUnblock(element)">解除阻塞</el-button>
              </div>
              <div v-if="element.overdue" class="overdue-tag">已逾期</div>
              <div v-if="element.status === 'BLOCKED'" class="block-reason">
                <el-icon><Warning /></el-icon>
                <span>已阻塞</span>
              </div>
            </div>
          </template>
        </draggable>
      </div>
    </div>

    <!-- 分组看板视图 -->
    <div v-else-if="viewMode === 'group'" class="kanban">
      <div v-for="col in groupColumns" :key="col.key" class="kanban-col">
        <div class="col-header col-group">
          <span>{{ col.label }}</span>
          <span class="col-count">{{ col.tasks.length }}</span>
        </div>
        <div class="col-body">
          <div
            v-for="element in col.tasks"
            :key="element.id"
            class="task-card"
            :class="{ overdue: element.overdue, unclaimed: !element.assigneeId, blocked: element.status === 'BLOCKED' }"
            @click="openDetail(element)"
          >
            <div class="card-header-row">
              <div class="card-title">{{ element.title }}</div>
            </div>
            <div class="card-meta">
              <el-tooltip :content="element.assigneeName || '待认领'" placement="top">
                <el-avatar :size="24" class="card-avatar" :class="{ unclaimed: !element.assigneeId }">{{ avatarText(element.assigneeName) }}</el-avatar>
              </el-tooltip>
              <el-tag :type="priorityType(element.priority)" size="small">{{ priorityLabel(element.priority) }}</el-tag>
            </div>
            <div class="card-tags">
              <el-tag v-if="element.taskType" size="small" type="info" effect="plain">{{ taskTypeLabel(element.taskType) }}</el-tag>
              <el-tag v-if="element.difficulty" size="small" :type="difficultyType(element.difficulty)" effect="plain">{{ difficultyLabel(element.difficulty) }}</el-tag>
              <el-tag v-if="!element.assigneeId && element.groupId" size="small" type="warning" effect="plain">{{ groupNameById(element.groupId) }}任务池</el-tag>
              <el-tag :type="statusType(element.status)" size="small">{{ statusLabel(element.status) }}</el-tag>
            </div>
            <el-progress :percentage="element.progress || 0" :stroke-width="6" :show-text="false" />
            <div v-if="element.overdue" class="overdue-tag">已逾期</div>
          </div>
          <div v-if="col.tasks.length === 0" class="empty-col">暂无任务</div>
        </div>
      </div>
    </div>

    <!-- 按任务归并视图 -->
    <div v-else-if="viewMode === 'task'" class="task-summary">
      <div v-if="taskSummary.length === 0" class="empty-col">暂无按组下发的任务</div>
      <el-card v-for="item in taskSummary" :key="item.key" class="summary-card" shadow="never">
        <template #header>
          <div class="summary-header">
            <span class="summary-title">{{ item.title }}</span>
            <div class="summary-tags">
              <el-tag type="info" size="small">共 {{ item.instances.length }} 组</el-tag>
              <el-tag type="success" size="small">已完成 {{ item.doneCount }} / {{ item.instances.length }}</el-tag>
            </div>
          </div>
        </template>
        <el-table :data="item.instances" size="small" border>
          <el-table-column label="小组" width="150">
            <template #default="scope">{{ scope.row.groupId ? groupNameById(scope.row.groupId) : '未分组' }}</template>
          </el-table-column>
          <el-table-column label="负责人" width="130">
            <template #default="scope">{{ scope.row.assigneeName || '待组长指派' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="进度" width="150">
            <template #default="scope"><el-progress :percentage="scope.row.progress || 0" :stroke-width="6" /></template>
          </el-table-column>
          <el-table-column prop="endDate" label="截止日期" width="120" />
          <el-table-column label="评分" width="90" align="center">
            <template #default="scope">{{ scope.row.score != null ? scope.row.score : '—' }}</template>
          </el-table-column>
          <el-table-column label="操作" width="90">
            <template #default="scope">
              <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 列表视图 -->
    <el-card v-else class="list-card">
      <el-table :data="filteredTasksTree" row-key="id" default-expand-all :tree-props="{ children: 'children' }">
        <el-table-column label="任务标题" min-width="220">
          <template #default="scope">
            <span :style="{ color: scope.row.overdue ? '#f56c6c' : '' }">{{ scope.row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="scope">
            <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="优先级" width="100">
          <template #default="scope">
            <el-tag :type="priorityType(scope.row.priority)" size="small">{{ priorityLabel(scope.row.priority) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assigneeName" label="负责人" width="120" />
        <el-table-column label="进度" width="140">
          <template #default="scope">
            <el-progress :percentage="scope.row.progress || 0" :stroke-width="6" />
          </template>
        </el-table-column>
        <el-table-column prop="endDate" label="截止日期" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openDetail(scope.row)">详情</el-button>
            <el-button v-if="canClaim(scope.row)" link type="success" @click="handleClaim(scope.row)">认领</el-button>
            <el-button v-if="canBlock(scope.row) && !isProjectCompleted" link type="danger" @click="openBlockDialog(scope.row)">阻塞</el-button>
            <el-button v-if="canUnblock(scope.row) && !isProjectCompleted" link type="success" @click="handleUnblock(scope.row)">解除</el-button>
            <el-button v-if="canEdit(scope.row) && !isProjectCompleted" link type="primary" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-if="canDelete(scope.row) && !isProjectCompleted" link type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑任务' : '新建任务'" width="620px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务标题" />
        </el-form-item>
        <el-form-item label="任务描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入任务描述" />
        </el-form-item>
        <el-row :gutter="15">
          <el-col :span="12">
            <el-form-item label="优先级">
              <el-select v-model="form.priority" style="width: 100%">
                <el-option v-for="item in priorityOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计划学时">
              <el-input-number v-model="form.plannedHours" :min="0" :precision="1" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="15">
          <el-col :span="12">
            <el-form-item label="任务类型">
              <el-select v-model="form.taskType" clearable placeholder="请选择" style="width: 100%">
                <el-option v-for="item in taskTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" clearable placeholder="请选择" style="width: 100%">
                <el-option v-for="item in difficultyOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="canManageProject && (isEdit || form.scope === 'STUDENT')" label="评分">
          <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" style="width: 200px" />
        </el-form-item>
        <el-form-item label="起止日期">
          <el-date-picker
            v-model="form.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item v-if="canManageProject && !isEdit" label="下发范围">
          <el-radio-group v-model="form.scope">
            <el-radio-button value="ALL_GROUPS">全部小组</el-radio-button>
            <el-radio-button value="GROUPS">指定小组</el-radio-button>
            <el-radio-button value="STUDENT">指定学生</el-radio-button>
          </el-radio-group>
          <div class="form-tip">按组下发时，每个小组各生成一份任务，由组长指派给组员</div>
        </el-form-item>
        <el-form-item v-if="canManageProject && !isEdit && form.scope === 'GROUPS'" label="选择小组">
          <el-select v-model="form.groupIds" multiple filterable placeholder="请选择小组" style="width: 100%">
            <el-option v-for="g in groups" :key="g.id" :label="g.name || g.groupNo" :value="g.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="15">
          <el-col :span="12">
            <el-form-item v-if="isEdit || !canManageProject || form.scope === 'STUDENT'" label="负责人">
              <el-select v-model="form.assigneeId" clearable :placeholder="canManageProject ? '暂不分配（放入项目任务池）' : '暂不分配（放入本组任务池）'" style="width: 100%">
                <el-option v-for="item in createAssigneeOptions" :key="item.userId" :label="item.nickname || item.username" :value="item.userId" />
              </el-select>
              <div v-if="!canManageProject" class="form-tip">留空表示放入本组任务池，由组员自行认领</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="isEdit || !canManageProject || form.scope === 'STUDENT'" label="审核人">
              <el-select v-model="form.reviewerId" clearable placeholder="请选择审核人" style="width: 100%">
                <el-option v-for="item in reviewerOptions.filter(m => m.userId !== form.assigneeId)" :key="item.userId" :label="item.nickname || item.username" :value="item.userId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="!canManageProject || isEdit || form.scope === 'STUDENT'" label="父任务">
          <el-select v-model="form.parentTaskId" clearable placeholder="请选择父任务（仅支持三级层级）" style="width: 100%" :disabled="isEdit">
            <el-option
              v-for="item in parentTaskOptions"
              :key="item.id"
              :label="levelPrefix(item.taskLevel) + item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!canManageProject || isEdit || form.scope === 'STUDENT'" label="前置依赖">
          <el-select
            v-model="form.dependencyIds"
            multiple
            collapse-tags
            placeholder="请选择前置依赖任务"
            style="width: 100%"
            @change="checkCycle"
          >
            <el-option
              v-for="item in dependencyOptions"
              :key="item.id"
              :label="levelPrefix(item.taskLevel) + item.title"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>

    <!-- 任务详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="任务详情" size="600px" destroy-on-close>
      <div v-if="currentTask.id" class="drawer-body">
        <div v-if="!isProjectCompleted" class="drawer-actions">
          <el-button v-if="canClaim(currentTask)" type="primary" size="small" @click="claimCurrentTask">认领任务</el-button>
          <el-button v-if="canBlock(currentTask)" type="danger" size="small" plain @click="openBlockDialog(currentTask)">标记阻塞</el-button>
          <el-button v-if="canUnblock(currentTask)" type="success" size="small" @click="handleUnblock(currentTask)">解除阻塞</el-button>
          <el-button v-if="canSubmitReview(currentTask)" type="primary" size="small" @click="submitTaskReview">提交审核</el-button>
          <el-button v-if="canCompleteDirectly(currentTask)" type="success" size="small" @click="handleCompleteDirectly">直接完成</el-button>
          <el-button v-if="canFinishSelf(currentTask)" type="success" size="small" @click="finishTaskSelf">完成任务</el-button>
          <el-button v-if="canApprove(currentTask)" type="success" size="small" @click="approveTaskReview">审核通过</el-button>
          <el-button v-if="canApprove(currentTask)" type="danger" size="small" plain @click="openRejectDialog">审核驳回</el-button>
          <el-button v-if="canScore(currentTask)" type="warning" size="small" plain @click="scoreTaskReview">评分</el-button>
          <el-button v-if="canWithdrawReview(currentTask)" size="small" @click="withdrawTaskReview">撤回审核</el-button>
          <el-button v-if="canEdit(currentTask)" size="small" @click="openEditDialog(currentTask)">编辑</el-button>
          <el-button v-if="canDelete(currentTask)" type="danger" size="small" plain @click="handleDelete(currentTask)">删除</el-button>
        </div>

        <el-alert
          v-if="currentTask.status === 'BLOCKED'"
          title="任务已阻塞"
          description="任务处于阻塞状态，请先解除阻塞后再继续处理"
          type="error"
          :closable="false"
          style="margin-bottom: 15px"
        />

        <el-descriptions :column="2" border>
          <el-descriptions-item label="标题" :span="2">{{ currentTask.title }}</el-descriptions-item>
          <el-descriptions-item label="描述" :span="2">{{ currentTask.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentTask.status)">{{ statusLabel(currentTask.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="优先级">
            <el-tag :type="priorityType(currentTask.priority)">{{ priorityLabel(currentTask.priority) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="负责人">{{ currentTask.assigneeName || '未分配' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ currentTask.creatorName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核人">{{ currentTask.reviewerName || '未指定' }}</el-descriptions-item>
          <el-descriptions-item label="审核意见">{{ currentTask.reviewComment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="起止日期" :span="2">{{ currentTask.startDate || '-' }} 至 {{ currentTask.endDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划学时">{{ currentTask.plannedHours || 0 }} 学时</el-descriptions-item>
          <el-descriptions-item label="任务类型">{{ taskTypeLabel(currentTask.taskType) }}</el-descriptions-item>
          <el-descriptions-item label="难度">{{ difficultyLabel(currentTask.difficulty) }}</el-descriptions-item>
          <el-descriptions-item label="评分">{{ currentTask.score != null ? currentTask.score : '-' }}</el-descriptions-item>
          <el-descriptions-item label="实际学时">
            <div v-if="!isEditingActualHours" class="actual-hours-display">
              {{ currentTask.actualHours || 0 }} 学时
              <el-button v-if="canEditActualHours" size="small" @click="startEditActualHours">编辑</el-button>
            </div>
            <div v-else class="actual-hours-edit">
              <el-input-number v-model="editingActualHours" :min="0" :precision="1" :step="0.5" style="width: 120px" />
              <span> 学时</span>
              <el-button size="small" @click="saveActualHours">保存</el-button>
              <el-button size="small" @click="cancelEditActualHours">取消</el-button>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="完成时间" :span="2">{{ currentTask.completedAt || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div v-if="currentTask.dependencies && currentTask.dependencies.length" class="section">
          <div class="section-title">前置依赖</div>
          <el-table :data="currentTask.dependencies" size="small" border>
            <el-table-column prop="title" label="标题">
              <template #default="scope">
                <span :class="{ 'text-danger': scope.row.status !== 'DONE' }">{{ scope.row.title }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="scope">
                <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="assigneeName" label="负责人" width="120">
              <template #default="scope">
                <span>{{ scope.row.assigneeName || '未认领' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div v-if="!isProjectCompleted" class="section">
          <div class="section-title">进度</div>
          <el-slider v-model="currentTask.progress" :max="100" show-input :disabled="!canEditProgress(currentTask)" @change="updateProgress" />
        </div>

        <div v-if="currentTask.children && currentTask.children.length" class="section">
          <div class="section-title">子任务</div>
          <el-table :data="currentTask.children" size="small" border>
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="assigneeName" label="负责人" width="100" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="statusType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="section">
          <div class="section-title">评论</div>
          <div v-if="!isProjectCompleted" class="comment-input">
            <div class="comment-input-wrapper">
              <el-input
                ref="commentInputRef"
                v-model="newComment"
                type="textarea"
                :rows="2"
                placeholder="发表你的评论，支持 @用户名 提醒成员，或回复某条评论"
                @input="onCommentInput"
              />
              <div v-if="mentionVisible && mentionList.length" class="mention-dropdown">
                <div
                  v-for="m in mentionList"
                  :key="m.userId"
                  class="mention-option"
                  @click="selectMention(m)"
                >
                  <el-avatar :size="24">{{ avatarText(m.nickname || m.username) }}</el-avatar>
                  <span class="mention-name">{{ m.nickname || m.username }}</span>
                  <span class="mention-username">@{{ m.username }}</span>
                </div>
              </div>
            </div>
            <el-button type="primary" size="small" style="margin-top: 8px" @click="submitComment">发送</el-button>
          </div>
          <div v-if="currentTask.comments && currentTask.comments.length" class="comment-list">
            <TaskCommentItem
              v-for="comment in currentTask.comments"
              :key="comment.id"
              :comment="comment"
              :current-user="currentUser"
              :can-resolve="canManageProject"
              @reply="setReplyTarget"
              @toggle-resolve="toggleResolve"
            />
          </div>
          <div v-else class="empty-comment">暂无评论</div>
        </div>

        <div class="section">
          <div class="section-title">
            附件
            <el-button v-if="canEdit(currentTask) || canUploadAttachment(currentTask)" type="primary" size="small" @click="triggerUpload">
              <el-icon><UploadFilled /></el-icon>上传
            </el-button>
            <input
              ref="attachmentInputRef"
              type="file"
              style="display: none"
              @change="handleAttachmentUpload"
              multiple
            />
          </div>
          <div v-if="taskAttachments.length" class="attachment-list">
            <div v-for="attach in taskAttachments" :key="attach.id" class="attachment-item">
              <el-icon :size="24" class="attachment-icon">
                <component :is="getAttachmentIcon(attach.fileName)" />
              </el-icon>
              <div class="attachment-info">
                <a href="javascript:;" class="attachment-name" @click="handleDownloadAttachment(attach)">{{ attach.fileName }}</a>
                <span class="attachment-size">{{ formatFileSize(attach.fileSize) }}</span>
              </div>
              <el-button
                v-if="canEdit(currentTask) || canUploadAttachment(currentTask)"
                type="danger"
                size="small"
                @click="handleDeleteAttachment(attach)"
              >删除</el-button>
            </div>
          </div>
          <div v-else class="empty-comment">暂无附件</div>
        </div>
      </div>
    </el-drawer>

    <!-- 驳回弹窗 -->
    <el-dialog v-model="rejectVisible" title="审核驳回" width="500px">
      <el-form label-width="80px">
        <el-form-item label="驳回意见">
          <el-input v-model="rejectComment" type="textarea" :rows="3" placeholder="请输入驳回意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" @click="rejectTaskReview">确定驳回</el-button>
      </template>
    </el-dialog>

    <!-- 标记阻塞弹窗 -->
    <el-dialog v-model="blockDialogVisible" title="标记任务阻塞" width="500px">
      <p>将任务标记为阻塞状态后，需要解除阻塞才能继续处理。</p>
      <p>建议在评论中记录具体的阻塞原因和需要的协助。</p>
      <template #footer>
        <el-button @click="blockDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmBlock">确定标记</el-button>
      </template>
    </el-dialog>

    <!-- 逾期提醒弹窗 -->
    <el-dialog v-model="noticeVisible" title="逾期/即将到期提醒" width="500px">
      <el-table :data="overdueList" size="small" border>
        <el-table-column prop="title" label="任务标题" />
        <el-table-column prop="endDate" label="截止日期" width="120" />
        <el-table-column label="操作" width="80">
          <template #default="scope">
            <el-button link type="primary" @click="openDetailFromNotice(scope.row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 选择审核人弹窗 -->
    <el-dialog v-model="reviewerDialogVisible" title="选择审核人" width="450px">
      <el-form label-width="80px">
        <el-form-item label="审核人" required>
          <el-select v-model="selectedReviewerId" placeholder="请选择审核人" style="width: 100%" @change="onReviewerChange">
            <el-option 
              v-for="item in availableReviewers" 
              :key="item.userId" 
              :label="item.nickname || item.username" 
              :value="item.userId" 
              :disabled="item.userId === pendingTaskAssigneeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="实际学时" required>
          <el-input-number v-model="pendingActualHours" :min="0" :precision="1" :step="0.5" style="width: 100%" />
          <span style="margin-left: 8px">学时</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelReviewerSelect">取消</el-button>
        <el-button type="primary" :disabled="!selectedReviewerId" @click="confirmReviewerSelect">确定提交</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus';
import { Bell, UploadFilled, Loading, Warning, Document, Box, Picture, VideoCamera, Headset, Files } from '@element-plus/icons-vue';
import draggable from 'vuedraggable';
import {
  createTask,
  updateTask,
  deleteTask,
  getTaskDetail,
  claimTask,
  listProjectTasks,
  updateTaskStatus,
  updateTaskProgress,
  updateTaskActualHours,
  submitReview,
  approveReview,
  rejectReview,
  scoreTask,
  withdrawReview,
  completeDirectly,
  checkDependencyCycle,
  addComment,
  resolveComment,
  listOverdueTasks,
  listTaskAttachments,
  uploadTaskAttachment,
  deleteTaskAttachment
} from '@/api/task.js';
import { downloadFile } from '@/api/file.js';
import { listGroups, listGroupMembers } from '@/api/training.js';
import request from '@/utils/request.js';
import TaskCommentItem from '@/components/TaskCommentItem.vue';

const route = useRoute();
const router = useRouter();
const projectId = Number(route.params.id);

if (isNaN(projectId)) {
  router.replace('/notFound');
}

const userStr = localStorage.getItem('code_user');
const currentUser = userStr ? JSON.parse(userStr) : {};
const isAdmin = currentUser.role === 'ADMIN';

const loading = ref(false);
const project = reactive({ name: '' });
const members = ref([]);
const tasksTree = ref([]);
const boardLists = reactive({ TODO: [], IN_PROGRESS: [], REVIEW: [], DONE: [] });
const viewMode = ref('board');
const groups = ref([]);
const groupMembersMap = reactive({}); // groupId -> [studentId, ...]

const filter = reactive({ statuses: [], priorities: [], assigneeId: null, onlyOverdue: false, keyword: '' });

const statusOptions = [
  { label: '待办', value: 'TODO' },
  { label: '进行中', value: 'IN_PROGRESS' },
  { label: '审核中', value: 'REVIEW' },
  { label: '已完成', value: 'DONE' }
];
const priorityOptions = [
  { label: '高', value: 'HIGH' },
  { label: '中', value: 'MEDIUM' },
  { label: '低', value: 'LOW' }
];
const taskTypeOptions = [
  { label: '认知', value: 'COGNITIVE' },
  { label: '实操', value: 'SKILL' },
  { label: '综合', value: 'COMPREHENSIVE' },
  { label: '成果', value: 'DELIVERABLE' }
];
const difficultyOptions = [
  { label: '简单', value: 'EASY' },
  { label: '中等', value: 'MEDIUM' },
  { label: '困难', value: 'HARD' }
];
const columns = [
  { status: 'TODO', label: '待办' },
  { status: 'IN_PROGRESS', label: '进行中' },
  { status: 'REVIEW', label: '审核中' },
  { status: 'DONE', label: '已完成' }
];

const dialogVisible = ref(false);
const isEdit = ref(false);
const editId = ref(null);
const formRef = ref(null);
const form = reactive({
  title: '',
  description: '',
  priority: 'MEDIUM',
  plannedHours: 0,
  taskType: '',
  difficulty: '',
  score: null,
  dateRange: [],
  assigneeId: null,
  reviewerId: null,
  parentTaskId: null,
  dependencyIds: [],
  scope: 'ALL_GROUPS',
  groupIds: []
});
const rules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }]
};

const drawerVisible = ref(false);
const currentTask = reactive({});
const isEditingActualHours = ref(false);
const editingActualHours = ref(0);
const newComment = ref('');
const replyTarget = ref(null);
const commentInputRef = ref(null);
const mentionVisible = ref(false);
const mentionList = ref([]);
const mentionStartIndex = ref(-1);

const rejectVisible = ref(false);
const rejectComment = ref('');

const blockDialogVisible = ref(false);
const currentBlockTask = ref(null);

const noticeVisible = ref(false);
const overdueList = ref([]);

const notifiedIds = new Set();

const attachmentInputRef = ref(null);
const taskAttachments = ref([]);

  const reviewerDialogVisible = ref(false);
  const selectedReviewerId = ref(null);
  const pendingTaskId = ref(null);
  const pendingTaskAssigneeId = ref(null);
  const pendingActualHours = ref(0);

const memberOptions = computed(() => members.value);
const myLedGroupIds = computed(() => groups.value.filter(g => g.leaderId === currentUser.id).map(g => g.id));
const createAssigneeOptions = computed(() => {
  if (canManageProject.value) return members.value;
  const ids = new Set();
  myLedGroupIds.value.forEach(gid => (groupMembersMap[gid] || []).forEach(id => ids.add(id)));
  return members.value.filter(m => ids.has(m.userId));
});
const groupNameById = (id) => {
  const group = groups.value.find(g => g.id === id);
  return group ? (group.name || '小组') : '小组';
};
const reviewerOptions = computed(() => {
  const list = [];
  const seen = new Set();
  const addOption = (item) => {
    if (item && item.userId != null && !seen.has(item.userId)) {
      seen.add(item.userId);
      list.push(item);
    }
  };
  // 指导教师审核小组组长本人的任务
  if (project.teacherId) {
    addOption(members.value.find(m => m.userId === project.teacherId)
      || { userId: project.teacherId, nickname: project.teacherName || '指导教师', username: null });
  }
  // 小组组长审核本组成员任务
  groups.value.forEach(g => {
    if (g.leaderId) {
      addOption(members.value.find(m => m.userId === g.leaderId)
        || { userId: g.leaderId, nickname: '小组组长', username: null });
    }
  });
  return list;
});
const isProjectMember = computed(() => isAdmin || project.ownerId === currentUser.id || members.value.some(m => m.userId === currentUser.id));
const canManageProject = computed(() => project.ownerId === currentUser.id || project.teacherId === currentUser.id);
const isGroupLeaderOf = (studentId) => {
  if (!studentId) return false;
  return groups.value.some(g => g.leaderId === currentUser.id && (groupMembersMap[g.id] || []).includes(studentId));
};
const findGroupLeaderOf = (studentId) => {
  if (!studentId) return null;
  const group = groups.value.find(g => (groupMembersMap[g.id] || []).includes(studentId));
  return group && group.leaderId ? group.leaderId : null;
};
const hasTeacher = computed(() => project.teacherId != null && project.teacherId !== currentUser.id);
  const availableReviewers = computed(() => {
    return reviewerOptions.value.filter(m => m.userId !== pendingTaskAssigneeId.value);
  });
const isProjectCompleted = computed(() => project.status === 'COMPLETED');
const canEditActualHours = computed(() => {
  if (isProjectCompleted.value) return false;
  if (['REVIEW', 'DONE'].includes(currentTask.status)) return false;
  return currentTask.assigneeId === currentUser.id;
});

const parentTaskOptions = computed(() => {
  const list = [];
  const walk = (arr) => {
    arr.forEach(t => {
      if (t.taskLevel < 3 && t.id !== editId.value) {
        list.push(t);
      }
      if (t.children) walk(t.children);
    });
  };
  walk(tasksTree.value);
  return list;
});

const dependencyOptions = computed(() => {
  const list = [];
  const walk = (arr) => {
    arr.forEach(t => {
      if (t.id !== editId.value) list.push(t);
      if (t.children) walk(t.children);
    });
  };
  walk(tasksTree.value);
  return list;
});

const overdueCount = computed(() => overdueList.value.length);

const filteredTasksTree = computed(() => {
  const matchesFilter = (task) => {
    if (filter.statuses.length && !filter.statuses.includes(task.status)) return false;
    if (filter.priorities.length && !filter.priorities.includes(task.priority)) return false;
    if (filter.assigneeId != null && task.assigneeId !== filter.assigneeId) return false;
    if (filter.onlyOverdue && !task.overdue) return false;
    if (filter.keyword && !task.title.includes(filter.keyword) && !task.description?.includes(filter.keyword)) return false;
    return true;
  };
  const filterTree = (arr) => {
    const result = [];
    arr.forEach(t => {
      const hasMatchingChild = t.children && filterTree(t.children).length > 0;
      if (matchesFilter(t) || hasMatchingChild) {
        const node = { ...t };
        if (t.children) {
          node.children = filterTree(t.children);
        }
        result.push(node);
      }
    });
    return result;
  };
  return filterTree(tasksTree.value);
});

const statusLabel = (status) => {
  const map = { TODO: '待办', IN_PROGRESS: '进行中', BLOCKED: '已阻塞', REVIEW: '审核中', DONE: '已完成' };
  return map[status] || status;
};
const statusType = (status) => {
  const map = { TODO: 'info', IN_PROGRESS: 'primary', BLOCKED: 'danger', REVIEW: 'warning', DONE: 'success' };
  return map[status] || 'info';
};
const priorityLabel = (priority) => {
  const map = { HIGH: '高', MEDIUM: '中', LOW: '低' };
  return map[priority] || priority;
};
const priorityType = (priority) => {
  const map = { HIGH: 'danger', MEDIUM: 'warning', LOW: 'success' };
  return map[priority] || 'info';
};
const taskTypeLabel = (type) => {
  const map = { COGNITIVE: '认知', SKILL: '实操', COMPREHENSIVE: '综合', DELIVERABLE: '成果' };
  return type ? (map[type] || type) : '-';
};
const difficultyLabel = (difficulty) => {
  const map = { EASY: '简单', MEDIUM: '中等', HARD: '困难' };
  return difficulty ? (map[difficulty] || difficulty) : '-';
};
const difficultyType = (difficulty) => {
  const map = { EASY: 'success', MEDIUM: 'warning', HARD: 'danger' };
  return map[difficulty] || 'info';
};
const avatarText = (name) => (name ? name.charAt(0).toUpperCase() : '?');
const levelPrefix = (level) => '　'.repeat((level || 1) - 1);

const loadProject = () => {
  request.get('/project/' + projectId).then(res => {
    Object.assign(project, res.data);
    members.value = res.data.members || [];
  });
};

const loadTasks = () => {
  loading.value = true;
  const params = {
    statuses: filter.statuses,
    priorities: filter.priorities,
    assigneeId: filter.assigneeId,
    onlyOverdue: filter.onlyOverdue,
    keyword: filter.keyword.trim() || undefined
  };
  listProjectTasks(projectId, params).then(res => {
    tasksTree.value = res.data || [];
    buildBoardLists();
    loading.value = false;
  }).catch(() => {
    loading.value = false;
  });
};

const flatten = (arr, list = []) => {
  arr.forEach(t => {
    list.push(t);
    if (t.children && t.children.length) flatten(t.children, list);
  });
  return list;
};

const buildBoardLists = () => {
  columns.forEach(col => (boardLists[col.status] = []));
  const all = flatten(tasksTree.value);
  all.forEach(t => {
    const displayStatus = t.status === 'BLOCKED' ? 'IN_PROGRESS' : t.status;
    if (filter.statuses.length && !filter.statuses.includes(displayStatus)) return;
    if (filter.priorities.length && !filter.priorities.includes(t.priority)) return;
    if (filter.assigneeId != null && t.assigneeId !== filter.assigneeId) return;
    if (filter.onlyOverdue && !t.overdue) return;
    boardLists[displayStatus].push(t);
  });
};

const loadGroups = () => {
  listGroups(projectId).then(res => {
    groups.value = res.data || [];
    groups.value.forEach(g => {
      listGroupMembers(g.id).then(mr => {
        groupMembersMap[g.id] = (mr.data || []).map(m => m.studentId);
      }).catch(() => {
        groupMembersMap[g.id] = [];
      });
    });
  }).catch(() => {
    groups.value = [];
  });
};

const groupColumns = computed(() => {
  const all = flatten(tasksTree.value);
  const studentGroupMap = {};
  groups.value.forEach(g => {
    (groupMembersMap[g.id] || []).forEach(sid => { studentGroupMap[sid] = g.id; });
  });
  const buckets = {};
  const cols = groups.value.map(g => {
    const col = { key: 'g' + g.id, label: g.name || g.groupNo || ('分组' + g.id), tasks: [] };
    buckets[g.id] = col;
    return col;
  });
  const unassigned = { key: 'unassigned', label: '未认领/未分组', tasks: [] };
  all.forEach(t => {
    const displayStatus = t.status === 'BLOCKED' ? 'IN_PROGRESS' : t.status;
    if (filter.statuses.length && !filter.statuses.includes(displayStatus)) return;
    if (filter.priorities.length && !filter.priorities.includes(t.priority)) return;
    if (filter.assigneeId != null && t.assigneeId !== filter.assigneeId) return;
    if (filter.onlyOverdue && !t.overdue) return;
    if (!t.assigneeId) {
      unassigned.tasks.push(t);
      return;
    }
    const gid = studentGroupMap[t.assigneeId];
    if (gid && buckets[gid]) {
      buckets[gid].tasks.push(t);
    } else {
      unassigned.tasks.push(t);
    }
  });
  cols.push(unassigned);
  return cols;
});

const viewModeLabel = computed(() => ({
  board: '看板视图',
  group: '分组看板',
  task: '任务归并',
  list: '列表视图'
}[viewMode.value] || '看板视图'));

const taskSummary = computed(() => {
  const all = flatten(tasksTree.value);
  const map = new Map();
  all.forEach(t => {
    const displayStatus = t.status === 'BLOCKED' ? 'IN_PROGRESS' : t.status;
    if (filter.statuses.length && !filter.statuses.includes(displayStatus)) return;
    if (filter.priorities.length && !filter.priorities.includes(t.priority)) return;
    if (filter.assigneeId != null && t.assigneeId !== filter.assigneeId) return;
    if (filter.onlyOverdue && !t.overdue) return;
    const key = (t.title || '').trim();
    if (!key) return;
    if (!map.has(key)) map.set(key, { key, title: key, instances: [] });
    map.get(key).instances.push(t);
  });
  return Array.from(map.values())
      .filter(item => item.instances.some(t => t.groupId))
      .map(item => ({ ...item, doneCount: item.instances.filter(t => t.status === 'DONE').length }))
      .sort((a, b) => b.instances.length - a.instances.length);
});

const resetFilter = () => {
  filter.statuses = [];
  filter.priorities = [];
  filter.assigneeId = null;
  filter.onlyOverdue = false;
  filter.keyword = '';
  loadTasks();
};

const goBack = () => router.push('/project/' + projectId);

// 拖拽逻辑
let draggingInfo = null;
const onMove = (evt) => {
  const task = evt.draggedContext.element;
  const targetStatus = evt.to.dataset.status;
  if (!targetStatus || targetStatus === task.status) return false;
  if (isProjectCompleted.value) return false;
  // 审核中、已完成、已阻塞的任务不允许拖动
  if (['REVIEW', 'DONE', 'BLOCKED'].includes(task.status)) return false;

  const isAssignee = task.assigneeId === currentUser.id;
  const isManager = isGroupLeaderOf(task.assigneeId) || canManageProject.value;
  if (!isAssignee && !isManager) return false;

  // 状态流转矩阵
  if (task.status === 'TODO') {
    return targetStatus === 'IN_PROGRESS' && isAssignee;
  }
  if (task.status !== 'IN_PROGRESS') return false;
  if (targetStatus === 'REVIEW') {
    return isAssignee && !!task.reviewerId;
  }
  if (targetStatus === 'DONE') {
    return isAssignee && !task.reviewerId;
  }
  if (targetStatus === 'BLOCKED') {
    return isAssignee || isManager;
  }
  return false;
};
const dragStart = (evt) => {
  const status = evt.from.dataset.status;
  draggingInfo = { task: boardLists[status][evt.oldIndex], fromStatus: status, fromIndex: evt.oldIndex };
};
const dragAdd = (status, evt) => {
  const task = boardLists[status][evt.newIndex];
  handleDrop(task, status).catch(() => {
    buildBoardLists();
  });
};
const handleDrop = async (task, targetStatus) => {
  if (targetStatus === task.status) return;
  if (task.status === 'REVIEW') {
    if (targetStatus === 'DONE') {
      ElMessage.warning('请等待审核人审核');
      throw new Error('非法操作');
    }
    if (targetStatus === 'IN_PROGRESS') {
      ElMessage.warning('审核中的任务需由审核人处理');
      throw new Error('非法操作');
    }
    if (targetStatus === 'TODO') {
      ElMessage.warning('审核中的任务无法直接退回待办');
      throw new Error('非法操作');
    }
  }
  if (targetStatus === 'DONE') {
    if (task.reviewerId) {
      ElMessage.warning('该任务已配置审核人，请提交审核由审核人确认');
      throw new Error('非法操作');
    }
    if (task.assigneeId !== currentUser.id) {
      ElMessage.warning('仅任务负责人可完成任务');
      throw new Error('非法操作');
    }
    if (!hasTeacher.value) {
      if (!task.actualHours || task.actualHours <= 0) {
        ElMessage.warning('请先在任务详情中填写实际学时');
        throw new Error('缺少学时');
      }
      await completeDirectly(task.id);
      ElMessage.success('任务已直接完成');
    } else {
      await updateTaskStatus(task.id, 'DONE');
      ElMessage.success('任务已完成');
    }
    loadTasks();
    loadOverview();
    return;
  }
  if (targetStatus === 'REVIEW') {
    if (task.status === 'TODO') {
      ElMessage.warning('待办任务需先开始执行才能提交审核');
      throw new Error('非法操作');
    }
    if (!task.reviewerId) {
      ElMessage.warning('该任务未配置审核人，可直接完成任务');
      throw new Error('非法操作');
    }
    await openReviewerDialog(task);
  } else {
    await updateTaskStatus(task.id, targetStatus);
    ElMessage.success('状态更新成功');
  }
  loadTasks();
};

const updateTaskInTree = (updated) => {
  const walk = (arr) => {
    for (const t of arr) {
      if (t.id === updated.id) {
        Object.assign(t, updated);
        return true;
      }
      if (t.children && walk(t.children)) return true;
    }
    return false;
  };
  walk(tasksTree.value);
};

const removeTaskFromTree = (arr, id) => {
  for (let i = 0; i < arr.length; i++) {
    if (arr[i].id === id) {
      arr.splice(i, 1);
      return true;
    }
    if (arr[i].children && removeTaskFromTree(arr[i].children, id)) return true;
  }
  return false;
};

// 新建/编辑
const resetForm = () => {
  form.title = '';
  form.description = '';
  form.priority = 'MEDIUM';
  form.plannedHours = 0;
  form.taskType = '';
  form.difficulty = '';
  form.score = null;
  form.dateRange = [];
  form.assigneeId = null;
  form.reviewerId = null;
  form.parentTaskId = null;
  form.dependencyIds = [];
  form.scope = 'ALL_GROUPS';
  form.groupIds = [];
};
const openCreateDialog = () => {
  isEdit.value = false;
  editId.value = null;
  resetForm();
  // 留空时由后端按「组员→小组组长，组长→指导教师」规则自动指定审核人
  form.reviewerId = null;
  dialogVisible.value = true;
};
const openEditDialog = (row) => {
  isEdit.value = true;
  editId.value = row.id;
  form.title = row.title;
  form.description = row.description;
  form.priority = row.priority;
  form.plannedHours = row.plannedHours || 0;
  form.taskType = row.taskType || '';
  form.difficulty = row.difficulty || '';
  form.score = row.score != null ? row.score : null;
  form.dateRange = [row.startDate, row.endDate];
  form.assigneeId = row.assigneeId;
  form.reviewerId = row.reviewerId;
  form.parentTaskId = row.parentTaskId;
  form.dependencyIds = row.dependencyIds || [];
  dialogVisible.value = true;
  drawerVisible.value = false;
};
const submitForm = () => {
  formRef.value.validate(valid => {
    if (!valid) return;
    if (form.assigneeId && form.reviewerId && form.assigneeId === form.reviewerId) {
      ElMessage.warning('审核人不能与任务负责人相同');
      return;
    }
    const payload = {
      projectId,
      title: form.title,
      description: form.description,
      priority: form.priority,
      plannedHours: form.plannedHours,
      taskType: form.taskType,
      difficulty: form.difficulty,
      score: form.score,
      startDate: form.dateRange && form.dateRange[0],
      endDate: form.dateRange && form.dateRange[1],
      assigneeId: form.assigneeId,
      reviewerId: form.reviewerId,
      parentTaskId: form.parentTaskId,
      dependencyIds: form.dependencyIds
    };
    if (isEdit.value) {
      updateTask(editId.value, payload).then(() => {
        ElMessage.success('编辑成功');
        dialogVisible.value = false;
        loadTasks();
      });
      return;
    }
    if (canManageProject.value && form.scope !== 'STUDENT') {
      const targetGroupIds = form.scope === 'ALL_GROUPS'
          ? groups.value.map(g => g.id)
          : form.groupIds;
      if (!targetGroupIds.length) {
        ElMessage.warning(form.scope === 'ALL_GROUPS' ? '项目暂无小组，请先创建分组' : '请选择要下发的小组');
        return;
      }
      const groupPayload = {
        ...payload,
        assigneeId: null,
        reviewerId: null,
        parentTaskId: null,
        dependencyIds: [],
        score: null
      };
      Promise.allSettled(targetGroupIds.map(groupId =>
          createTask({ ...groupPayload, groupId })
      )).then(results => {
        const ok = results.filter(r => r.status === 'fulfilled').length;
        const failed = results.length - ok;
        if (ok > 0) {
          ElMessage.success(failed > 0
              ? `已下发到 ${ok} 个小组，${failed} 个失败（可能是该组暂无成员）`
              : `已下发到 ${ok} 个小组`);
        } else {
          ElMessage.error('下发失败，请确认小组已创建且有成员');
        }
        dialogVisible.value = false;
        loadTasks();
      });
      return;
    }
    createTask(payload).then(() => {
      ElMessage.success('创建成功');
      dialogVisible.value = false;
      loadTasks();
    });
  });
};
const checkCycle = () => {
  const id = isEdit.value ? editId.value : 0;
  if (!form.dependencyIds || !form.dependencyIds.length) return;
  checkDependencyCycle(id, form.dependencyIds).then(res => {
    if (res.data) {
      ElMessage.warning('当前依赖关系存在闭环，请重新选择');
    }
  });
};

// 详情抽屉
const openDetail = (row) => {
  getTaskDetail(row.id).then(res => {
    Object.assign(currentTask, res.data);
    drawerVisible.value = true;
    loadTaskAttachments(row.id);
  });
};

const loadTaskAttachments = (taskId) => {
  listTaskAttachments(taskId).then(res => {
    taskAttachments.value = res.data || [];
  });
};

const triggerUpload = () => {
  attachmentInputRef.value?.click();
};

const handleAttachmentUpload = (event) => {
  const files = Array.from(event.target.files);
  if (!files.length) return;
  
  const uploadPromises = files.map(file => {
    return uploadTaskAttachment(currentTask.id, file);
  });
  
  Promise.all(uploadPromises).then(() => {
    ElMessage.success('附件上传成功');
    loadTaskAttachments(currentTask.id);
    event.target.value = '';
  }).catch(() => {
    ElMessage.error('部分附件上传失败');
  });
};

const handleDeleteAttachment = (attach) => {
  ElMessageBox.confirm('确定删除附件「' + attach.fileName + '」吗？', '提示', { type: 'warning' }).then(() => {
    deleteTaskAttachment(currentTask.id, attach.id).then(() => {
      ElMessage.success('删除成功');
      loadTaskAttachments(currentTask.id);
    });
  });
};

const handleDownloadAttachment = (attach) => {
  if (!attach.fileId) {
    ElMessage.warning('文件信息不完整');
    return;
  }
  downloadFile(attach.fileId).then(res => {
    const blob = new Blob([res]);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', attach.fileName || 'download');
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  }).catch(() => {
    ElMessage.error('文件下载失败');
  });
};

const formatFileSize = (size) => {
  if (!size) return '';
  if (size < 1024) return size + ' B';
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB';
  return (size / (1024 * 1024)).toFixed(1) + ' MB';
};

const getAttachmentIcon = (fileName) => {
  const ext = fileName?.split('.').pop()?.toLowerCase() || '';
  const iconMap = {
    'pdf': Document,
    'doc': Document,
    'docx': Document,
    'xls': Document,
    'xlsx': Document,
    'ppt': Document,
    'pptx': Document,
    'txt': Document,
    'md': Document,
    'zip': Box,
    'rar': Box,
    '7z': Box,
    'jpg': Picture,
    'jpeg': Picture,
    'png': Picture,
    'gif': Picture,
    'svg': Picture,
    'webp': Picture,
    'mp4': VideoCamera,
    'avi': VideoCamera,
    'mov': VideoCamera,
    'mp3': Headset,
    'wav': Headset,
    'json': Files,
    'xml': Files,
    'csv': Files
  };
  return iconMap[ext] || Files;
};
const openDetailFromNotice = (row) => {
  noticeVisible.value = false;
  openDetail(row);
};
const updateProgress = (val) => {
  const originalProgress = currentTask.progress;
  const originalStatus = currentTask.status;
  updateTaskProgress(currentTask.id, val).then(() => {
    ElMessage.success('进度已更新');
    currentTask.progress = val;
    if (val > 0 && currentTask.status === 'TODO') {
      currentTask.status = 'IN_PROGRESS';
    }
    updateTaskInTree(currentTask);
    buildBoardLists();
  }).catch(() => {
    currentTask.progress = originalProgress;
    currentTask.status = originalStatus;
  });
};

const startEditActualHours = () => {
  editingActualHours.value = currentTask.actualHours || 0;
  isEditingActualHours.value = true;
};

const saveActualHours = () => {
  updateTaskActualHours(currentTask.id, editingActualHours.value).then(() => {
    ElMessage.success('实际学时已更新');
    currentTask.actualHours = editingActualHours.value;
    isEditingActualHours.value = false;
    updateTaskInTree(currentTask);
    buildBoardLists();
  }).catch(() => {
    ElMessage.error('更新失败');
  });
};

const cancelEditActualHours = () => {
  isEditingActualHours.value = false;
};

const canEdit = (task) => {
  if (['DONE', 'REVIEW'].includes(task.status)) return false;
  return task.creatorId === currentUser.id || isGroupLeaderOf(task.assigneeId) || canManageProject.value
      || isLeaderOfTaskGroup(task);
};
const isLeaderOfTaskGroup = (task) => !!task.groupId
    && groups.value.some(g => g.id === task.groupId && g.leaderId === currentUser.id);
const canAssignGroupTask = (task) => {
  if (isProjectCompleted.value) return false;
  if (task.assigneeId || ['DONE', 'REVIEW'].includes(task.status)) return false;
  return isLeaderOfTaskGroup(task);
};
const canUploadAttachment = (task) => {
  if (['DONE', 'REVIEW'].includes(task.status)) return false;
  return task.assigneeId === currentUser.id;
};
const canDelete = (task) => {
  return task.creatorId === currentUser.id || isGroupLeaderOf(task.assigneeId) || canManageProject.value;
};
const canSubmitReview = (task) => {
  if (task.status !== 'IN_PROGRESS') return false;
  if (task.assigneeId !== currentUser.id) return false;
  return true;
};
const canApprove = (task) => {
  return task.status === 'REVIEW'
      && task.assigneeId !== currentUser.id
      && (canManageProject.value || task.reviewerId === currentUser.id || isGroupLeaderOf(task.assigneeId));
};
const canScore = (task) => {
  return canManageProject.value && task.status === 'DONE';
};
const canWithdrawReview = (task) => {
  return task.status === 'REVIEW'
      && !isProjectCompleted.value
      && (task.assigneeId === currentUser.id || isGroupLeaderOf(task.assigneeId) || canManageProject.value);
};
const canEditProgress = (task) => {
  if (isProjectCompleted.value) return false;
  if (task.status === 'BLOCKED') return false;
  if (task.status === 'DONE') return false;
  if (task.status === 'REVIEW') return false;
  if (task.assigneeId !== currentUser.id) return false;
  return true;
};
const canClaim = (task) => {
  if (currentUser.role !== 'STUDENT') return false;
  if (task.assigneeId || task.status !== 'TODO' || isProjectCompleted.value) return false;
  if (task.groupId) {
    return groups.value.some(g => g.id === task.groupId
        && (g.leaderId === currentUser.id || (groupMembersMap[g.id] || []).includes(currentUser.id)));
  }
  return true;
};
const canCompleteDirectly = (task) => {
  if (isProjectCompleted.value) return false;
  if (task.status !== 'TODO' && task.status !== 'IN_PROGRESS') return false;
  if (task.assigneeId !== currentUser.id) return false;
  if (task.reviewerId) return false;
  return !hasTeacher.value;
};
const canFinishSelf = (task) => {
  if (isProjectCompleted.value) return false;
  if (task.status !== 'TODO' && task.status !== 'IN_PROGRESS') return false;
  if (task.assigneeId !== currentUser.id) return false;
  if (task.reviewerId) return false;
  return hasTeacher.value;
};
const canBlock = (task) => {
  return task.status === 'IN_PROGRESS'
      && !isProjectCompleted.value
      && (task.assigneeId === currentUser.id || isGroupLeaderOf(task.assigneeId) || canManageProject.value);
};
const canUnblock = (task) => {
  return task.status === 'BLOCKED'
      && !isProjectCompleted.value
      && (task.assigneeId === currentUser.id || isGroupLeaderOf(task.assigneeId) || canManageProject.value);
};

const handleClaim = (task) => {
  claimTask(task.id).then(() => {
    ElMessage.success('认领成功');
    task.assigneeId = currentUser.id;
    task.assigneeName = currentUser.nickname || currentUser.username;
    updateTaskInTree(task);
    buildBoardLists();
  });
};

const claimCurrentTask = () => {
  handleClaim(currentTask);
  drawerVisible.value = false;
};

const submitTaskReview = () => {
  openReviewerDialog(currentTask);
};

const handleCompleteDirectly = async () => {
  try {
    if (!currentTask.actualHours || currentTask.actualHours <= 0) {
      ElMessage.warning('请先填写实际学时');
      return;
    }
    await completeDirectly(currentTask.id);
    ElMessage.success('任务已完成');
    currentTask.status = 'DONE';
    currentTask.progress = 100;
    loadTasks();
    loadOverview();
  } catch (e) {
    // 错误已在拦截器中处理
  }
};

const finishTaskSelf = async () => {
  try {
    await updateTaskStatus(currentTask.id, 'DONE');
    ElMessage.success('任务已完成');
    currentTask.status = 'DONE';
    currentTask.progress = 100;
    loadTasks();
    loadOverview();
  } catch (e) {
    // 错误已在拦截器中处理
  }
};

const openReviewerDialog = async (task) => {
  pendingTaskId.value = task.id;
  pendingTaskAssigneeId.value = task.assigneeId;
  pendingActualHours.value = task.actualHours || 0;
  if (task.reviewerId) {
    selectedReviewerId.value = task.reviewerId;
  } else {
    const recommended = recommendReviewer(task.assigneeId);
    selectedReviewerId.value = recommended;
  }
  reviewerDialogVisible.value = true;
};

const recommendReviewer = (assigneeId) => {
  if (!assigneeId) return null;
  const leaderId = findGroupLeaderOf(assigneeId);
  if (leaderId && leaderId !== assigneeId) return leaderId;
  if (project.teacherId && project.teacherId !== assigneeId) return project.teacherId;
  return null;
};

const onReviewerChange = () => {};

const cancelReviewerSelect = () => {
  reviewerDialogVisible.value = false;
  pendingTaskId.value = null;
  pendingTaskAssigneeId.value = null;
  selectedReviewerId.value = null;
  pendingActualHours.value = 0;
};

const confirmReviewerSelect = async () => {
  if (!selectedReviewerId.value) {
    ElMessage.warning('请选择审核人');
    return;
  }
  if (pendingActualHours.value <= 0) {
    ElMessage.warning('请填写实际学时');
    return;
  }
  try {
    await updateTaskActualHours(pendingTaskId.value, pendingActualHours.value);
    await submitReview(pendingTaskId.value, selectedReviewerId.value);
    ElMessage.success('已提交审核');
    reviewerDialogVisible.value = false;
    if (currentTask.id === pendingTaskId.value) {
      currentTask.status = 'REVIEW';
      currentTask.progress = 100;
      currentTask.reviewerId = selectedReviewerId.value;
      currentTask.actualHours = pendingActualHours.value;
    }
    cancelReviewerSelect();
    loadTasks();
  } catch (error) {
    // request.js 拦截器已统一弹出错误提示，此处不再重复提示
  }
};
const applyApprove = (value) => {
  const score = value && value.trim() ? Number(value) : null;
  approveReview(currentTask.id, score).then(() => {
    ElMessage.success(score != null ? '审核已通过，评分已记录' : '审核已通过');
    currentTask.status = 'DONE';
    currentTask.progress = 100;
    if (score != null) currentTask.score = score;
    updateTaskInTree(currentTask);
    buildBoardLists();
  });
};
const approveTaskReview = () => {
  // 组长只做组内验收，不评分；评分权归指导教师
  if (!canManageProject.value) {
    ElMessageBox.confirm('确认通过该成员的任务？通过后由指导教师统一评分。', '组内验收', {
      confirmButtonText: '通过',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(() => applyApprove(null)).catch(() => {});
    return;
  }
  ElMessageBox.prompt('请输入该任务的评分（0-100，可留空）', '审核通过', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputPlaceholder: '例如：90',
    inputValidator: (value) => {
      if (!value || !value.trim()) return true;
      const num = Number(value);
      if (isNaN(num) || num < 0 || num > 100) return '评分需为 0-100 之间的数字';
      return true;
    }
  }).then(({ value }) => applyApprove(value)).catch(() => {});
};
const scoreTaskReview = () => {
  ElMessageBox.prompt('请输入评分（0-100）', '任务评分', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputValue: currentTask.score != null ? String(currentTask.score) : '',
    inputPlaceholder: '例如：90',
    inputValidator: (value) => {
      if (!value || !value.trim()) return '请输入评分';
      const num = Number(value);
      if (isNaN(num) || num < 0 || num > 100) return '评分需为 0-100 之间的数字';
      return true;
    }
  }).then(({ value }) => {
    const score = Number(value);
    scoreTask(currentTask.id, score).then(() => {
      ElMessage.success('评分已更新');
      currentTask.score = score;
      updateTaskInTree(currentTask);
    });
  }).catch(() => {});
};
const withdrawTaskReview = () => {
  withdrawReview(currentTask.id).then(() => {
    ElMessage.success('已撤回审核');
    currentTask.status = 'IN_PROGRESS';
    currentTask.reviewComment = null;
    updateTaskInTree(currentTask);
    buildBoardLists();
  });
};
const openRejectDialog = () => {
  rejectComment.value = '';
  rejectVisible.value = true;
};
const rejectTaskReview = () => {
  if (!rejectComment.value.trim()) {
    ElMessage.warning('请输入驳回意见');
    return;
  }
  rejectReview(currentTask.id, rejectComment.value).then(() => {
    ElMessage.success('已驳回');
    currentTask.status = 'IN_PROGRESS';
    currentTask.reviewComment = rejectComment.value;
    currentTask.progress = 80;
    rejectVisible.value = false;
    updateTaskInTree(currentTask);
    buildBoardLists();
  });
};

const openBlockDialog = (task) => {
  currentBlockTask.value = task;
  blockDialogVisible.value = true;
};
const confirmBlock = () => {
  const task = currentBlockTask.value;
  updateTaskStatus(task.id, 'BLOCKED').then(() => {
    ElMessage.success('已标记阻塞');
    task.status = 'BLOCKED';
    blockDialogVisible.value = false;
    updateTaskInTree(task);
    buildBoardLists();
    if (task.id === currentTask.id) {
      Object.assign(currentTask, task);
    }
  });
};
const handleUnblock = (task) => {
  ElMessageBox.confirm('确定解除任务「' + task.title + '」的阻塞状态吗？', '提示', { type: 'warning' }).then(() => {
    updateTaskStatus(task.id, 'IN_PROGRESS').then(() => {
      ElMessage.success('已解除阻塞');
      task.status = 'IN_PROGRESS';
      updateTaskInTree(task);
      buildBoardLists();
      if (task.id === currentTask.id) {
        Object.assign(currentTask, task);
      }
    });
  });
};

const handleDelete = (task) => {
  ElMessageBox.confirm('确定删除任务「' + task.title + '」吗？子任务将一并删除。', '提示', { type: 'warning' }).then(() => {
    deleteTask(task.id).then(() => {
      ElMessage.success('删除成功');
      drawerVisible.value = false;
      removeTaskFromTree(tasksTree.value, task.id);
      buildBoardLists();
    });
  });
};

// 评论
const setReplyTarget = (comment) => {
  replyTarget.value = comment;
  if (comment.userId === currentUser.id) {
    newComment.value = '';
  } else {
    newComment.value = '@' + (comment.nickname || comment.username) + ' ';
  }
  mentionVisible.value = false;
};

const getCommentTextarea = () => {
  return commentInputRef.value?.textarea || commentInputRef.value?.$el?.querySelector('textarea');
};

const mentionCandidates = computed(() => {
  const task = currentTask.value || {};
  const base = task.groupId
      ? members.value.filter(m => (groupMembersMap[task.groupId] || []).includes(m.userId))
      : members.value;
  return base.filter(m => m.userId !== currentUser.id);
});

const onCommentInput = () => {
  const textarea = getCommentTextarea();
  if (!textarea) return;
  const cursor = textarea.selectionStart || 0;
  const text = newComment.value;
  const before = text.slice(0, cursor);
  const atIndex = before.lastIndexOf('@');
  if (atIndex === -1) {
    mentionVisible.value = false;
    return;
  }
  const afterAt = before.slice(atIndex + 1);
  if (/\s/.test(afterAt)) {
    mentionVisible.value = false;
    return;
  }
  const query = afterAt.toLowerCase();
  mentionStartIndex.value = atIndex;
  mentionList.value = mentionCandidates.value.filter(m => {
    const name = (m.nickname || m.username || '').toLowerCase();
    const username = (m.username || '').toLowerCase();
    return name.includes(query) || username.includes(query);
  });
  mentionVisible.value = mentionList.value.length > 0;
};

const selectMention = (member) => {
  const textarea = getCommentTextarea();
  if (!textarea) return;
  const cursor = textarea.selectionStart || 0;
  const before = newComment.value.slice(0, mentionStartIndex.value);
  const after = newComment.value.slice(cursor);
  newComment.value = before + '@' + member.username + ' ' + after;
  mentionVisible.value = false;
  nextTick(() => {
    textarea.focus();
    const pos = before.length + member.username.length + 2;
    textarea.setSelectionRange(pos, pos);
  });
};

const submitComment = () => {
  if (!newComment.value.trim()) {
    ElMessage.warning('评论内容不能为空');
    return;
  }
  addComment(currentTask.id, newComment.value, replyTarget.value ? replyTarget.value.id : null).then(() => {
    ElMessage.success('评论成功');
    newComment.value = '';
    replyTarget.value = null;
    openDetail(currentTask);
  });
};
const toggleResolve = (comment, resolved) => {
  resolveComment(comment.id, resolved).then(() => {
    ElMessage.success('标记成功');
    comment.isResolved = resolved;
  });
};

// 逾期提醒轮询
const loadOverdue = () => {
  listOverdueTasks().then(res => {
    overdueList.value = res.data || [];
    const newItems = overdueList.value.filter(item => !notifiedIds.has(item.id));
    newItems.forEach(item => {
      notifiedIds.add(item.id);
      ElNotification({
        title: '任务逾期提醒',
        message: item.title + ' 已逾期，请尽快处理',
        type: 'warning',
        duration: 5000
      });
    });
  });
};

watch([() => filter.statuses, () => filter.priorities, () => filter.assigneeId, () => filter.onlyOverdue], () => {
  buildBoardLists();
}, { deep: true });

let overdueTimer = null;

onMounted(() => {
  loadProject();
  loadTasks();
  loadGroups();
  loadOverdue();
  const taskId = Number(route.query.taskId);
  if (taskId) {
    openDetail({ id: taskId });
  }
  overdueTimer = setInterval(loadOverdue, 60000);
});

onBeforeUnmount(() => {
  if (overdueTimer) {
    clearInterval(overdueTimer);
    overdueTimer = null;
  }
});
</script>

<style scoped>
.task-board {
  height: 100%;
  display: flex;
  flex-direction: column;
}
.board-card {
  margin-bottom: 15px;
}
.board-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.project-title {
  font-size: 18px;
  font-weight: bold;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 15px;
}
.notice-badge {
  cursor: pointer;
}
.bell-icon {
  color: #606266;
}
.filter-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.kanban {
  flex: 1;
  display: flex;
  gap: 15px;
  overflow-x: auto;
  min-height: 500px;
}
.kanban-col {
  flex: 1;
  min-width: 260px;
  background: #f5f7fa;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
}
.col-header {
  padding: 10px 12px;
  font-weight: bold;
  border-radius: 6px 6px 0 0;
  display: flex;
  justify-content: space-between;
}
.col-todo { background: #e6f7ff; color: #1890ff; }
.col-in_progress { background: #fff7e6; color: #fa8c16; }
.col-review { background: #f6ffed; color: #52c41a; }
.col-done { background: #f0f0f0; color: #666; }
.col-count {
  background: rgba(255,255,255,0.7);
  padding: 0 8px;
  border-radius: 10px;
  font-size: 12px;
}
.col-body {
  flex: 1;
  padding: 10px;
  min-height: 100px;
}
.task-card {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 10px;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  transition: transform 0.1s;
}
.task-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 3px 8px rgba(0,0,0,0.12);
}
.task-card.overdue {
  border: 1px solid #f56c6c;
}
.task-card.unclaimed {
  border: 1px dashed #409eff;
  background: #f0f9ff;
}
.task-card.blocked {
  border: 1px solid #f56c6c;
  background: #fff5f5;
}
.card-title {
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.card-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 8px;
}
.card-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.card-avatar {
  background: #409eff;
  color: #fff;
  font-size: 12px;
}
.card-avatar.unclaimed {
  background: #909399;
}
.card-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}
.form-tip {
  width: 100%;
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.4;
  color: #909399;
}
.empty-col {
  color: #c0c4cc;
  text-align: center;
  padding: 20px 0;
  font-size: 13px;
}
.col-group {
  background: #eef3fe;
  color: #3370ff;
}
.claim-bar {
  margin-top: 8px;
  text-align: right;
}
.overdue-tag {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
}
.block-reason {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
  display: flex;
  align-items: flex-start;
  gap: 4px;
}
.block-reason span {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.text-danger {
  color: #f56c6c;
}
.ghost-card {
  opacity: 0.5;
  background: #c6e2ff;
}
.drawer-body {
  padding-bottom: 20px;
}
.drawer-actions {
  margin-bottom: 15px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
.section {
  margin-top: 20px;
}
.section-title {
  font-weight: bold;
  margin-bottom: 10px;
  font-size: 16px;
}
.comment-input {
  margin-bottom: 15px;
}
.comment-input-wrapper {
  position: relative;
}
.mention-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  z-index: 100;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  max-height: 200px;
  overflow-y: auto;
  margin-top: 4px;
}
.mention-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background 0.2s;
}
.mention-option:hover {
  background: #f5f7fa;
}
.mention-name {
  font-weight: 500;
  color: #303133;
}
.mention-username {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
}
.comment-list {
  border-top: 1px solid #ebeef5;
  padding-top: 10px;
}
.empty-comment {
  color: #909399;
  text-align: center;
  padding: 20px 0;
}
.actual-hours-display {
  display: flex;
  align-items: center;
  gap: 8px;
}
.actual-hours-edit {
  display: flex;
  align-items: center;
  gap: 8px;
}
.attachment-list {
  margin-top: 10px;
}
.attachment-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  background: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 8px;
}
.attachment-icon {
  color: #409eff;
}
.attachment-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.attachment-name {
  font-weight: 500;
  color: #409eff;
  text-decoration: none;
}
.attachment-name:hover {
  text-decoration: underline;
}
.attachment-size {
  font-size: 12px;
  color: #909399;
}
.task-summary {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}
.summary-card {
  width: 100%;
}
.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.summary-title {
  font-weight: 600;
  font-size: 15px;
}
.summary-tags {
  display: flex;
  gap: 8px;
}
</style>
