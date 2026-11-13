<template>
  <div v-loading="loading" class="group-page">
    <div class="page-head">
      <div>
        <h2 class="page-title">我的小组</h2>
        <p class="page-subtitle">查看你在各实训项目中的分组与组员</p>
      </div>
      <el-button size="small" @click="loadData">
        <el-icon class="btn-icon"><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <div v-if="!loading && visibleProjects.length === 0" class="empty-panel">
      <el-icon class="empty-icon"><UserFilled /></el-icon>
      <p class="empty-title">暂未加入实训项目</p>
      <p class="empty-sub">请确认你已加入实训项目，教师创建分组并开放自选后即可在这里加入小组</p>
    </div>

    <div v-else class="project-sections">
      <section v-for="project in visibleProjects" :key="project.id" class="project-section">
        <div class="project-head">
          <span class="project-title">{{ project.name }}</span>
          <el-button link type="primary" size="small" @click="goProject(project.id)">进入项目</el-button>
        </div>

        <div v-if="project.myGroup" class="group-card">
          <div class="group-head">
            <div class="group-head-text">
              <div class="group-name">
                {{ project.myGroup.groupName }}
                <el-button link type="primary" size="small" class="rename-btn" @click="renameGroupName(project.myGroup)">
                  <el-icon><EditPen /></el-icon>
                </el-button>
              </div>
            </div>
            <el-tag :type="project.myGroup.myRole === '组长' ? 'warning' : 'info'" size="small" effect="plain">
              {{ project.myGroup.myRole }}
            </el-tag>
          </div>

          <div class="group-meta">
            <span class="group-no">组号：{{ project.myGroup.groupNo || '-' }}</span>
            <span class="member-count">{{ project.myGroup.members.length }} 名组员</span>
            <el-button
                v-if="project.myGroup.myRole !== '组长'"
                link type="danger" size="small"
                @click="leave(project.myGroup)"
            >退出小组</el-button>
          </div>

          <div v-if="project.myGroup.description" class="group-desc">{{ project.myGroup.description }}</div>

          <div class="member-list">
            <div
                v-for="member in project.myGroup.members"
                :key="member.studentId"
                :class="['member-row', { 'is-me': member.studentId === user.id }]"
            >
              <div class="member-avatar">{{ initialOf(member) }}</div>
              <div class="member-main">
                <div class="member-name">
                  {{ member.nickname || member.username }}
                  <span v-if="member.studentId === user.id" class="me-tag">我</span>
                </div>
                <div class="member-sub">{{ member.studentNo || ('@' + member.username) }}</div>
              </div>
              <el-tag v-if="member.studentId === project.myGroup.leaderId" size="small" effect="plain">组长</el-tag>
            </div>
          </div>

          <div class="discussion">
            <div class="discussion-head">
              <span class="discussion-title">小组讨论</span>
              <el-button link type="primary" size="small" @click="toggleDiscussion(project.myGroup)">
                {{ discussionExpanded[project.myGroup.id] ? '收起' : '展开' }}
              </el-button>
            </div>

            <div v-if="discussionExpanded[project.myGroup.id]" class="discussion-body">
              <div v-loading="discussionLoading[project.myGroup.id]" class="discussion-list">
                <div
                    v-for="msg in (discussionMessages[project.myGroup.id] || [])"
                    :key="msg.id"
                    :class="['message-row', { 'is-me': msg.senderId === user.id }]"
                >
                  <div class="msg-avatar">{{ initialOf(msg) }}</div>
                  <div class="msg-main">
                    <div class="msg-meta">
                      <span class="msg-name">{{ msg.senderName || msg.senderUsername }}</span>
                      <el-tag v-if="msg.isLeader" size="small" type="warning" effect="plain">组长</el-tag>
                      <span class="msg-time">{{ formatMessageTime(msg.createdAt) }}</span>
                    </div>
                    <div class="msg-content">{{ msg.content }}</div>
                  </div>
                </div>
                <div v-if="!discussionLoading[project.myGroup.id] && !(discussionMessages[project.myGroup.id] || []).length" class="discussion-empty">
                  还没有发言，来讨论分工和进度吧
                </div>
              </div>

              <div class="discussion-editor">
                <el-input
                    v-model="drafts[project.myGroup.id]"
                    type="textarea"
                    :rows="2"
                    maxlength="500"
                    show-word-limit
                    placeholder="输入内容，@组员可提醒对方"
                    @input="onDraftInput(project.myGroup)"
                    @keydown.ctrl.enter="sendMessage(project.myGroup)"
                />
                <div v-if="mention.groupId === project.myGroup.id && mention.list.length" class="mention-list">
                  <div
                      v-for="m in mention.list"
                      :key="m.studentId"
                      class="mention-item"
                      @click="pickMention(project.myGroup, m)"
                  >
                    {{ m.nickname || m.username }} <span class="mention-account">@{{ m.username }}</span>
                  </div>
                </div>
                <div class="discussion-actions">
                  <span class="editor-tip">Ctrl + Enter 发送</span>
                  <el-button
                      type="primary"
                      size="small"
                      :loading="sending[project.myGroup.id]"
                      :disabled="!(drafts[project.myGroup.id] || '').trim()"
                      @click="sendMessage(project.myGroup)"
                  >发送</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div v-else class="group-grid">
          <div v-for="group in project.groups" :key="group.id" class="group-card">
            <div class="group-head">
              <div class="group-head-text">
                <div class="group-name">{{ group.name || group.groupNo }}</div>
                <div class="group-leader">组长：{{ group.leaderName || '待教师指定' }}</div>
              </div>
              <el-tag :type="groupTagType(group)" size="small" effect="plain">{{ groupTagText(group) }}</el-tag>
            </div>

            <div class="group-meta">
              <span class="member-count">{{ group.memberCount || 0 }} / {{ group.maxMembers || '不限' }} 人</span>
              <el-button
                  type="primary" size="small"
                  :disabled="!canJoin(group)"
                  @click="join(group)"
              >加入小组</el-button>
            </div>
          </div>
          <el-empty v-if="project.groups.length === 0" description="指导教师尚未创建分组" />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import {ref, computed, onMounted} from 'vue';
import {useRouter} from 'vue-router';
import {ElMessage, ElMessageBox} from 'element-plus';
import {UserFilled, Refresh, EditPen} from '@element-plus/icons-vue';
import {getPersonalDashboard} from '@/api/dashboard.js';
import {listGroups, listGroupMembers, joinGroup, leaveGroup, renameGroup, listGroupMessages, sendGroupMessage} from '@/api/training.js';

const router = useRouter();
const user = JSON.parse(localStorage.getItem('code_user') || '{}');
const loading = ref(false);
const projects = ref([]);

const visibleProjects = computed(() => projects.value);

const discussionExpanded = ref({});
const discussionLoading = ref({});
const discussionMessages = ref({});
const drafts = ref({});
const sending = ref({});
const mention = ref({ groupId: null, list: [], startIndex: -1 });

const initialOf = (member) => {
  const name = member.nickname || member.senderName || member.username || member.senderUsername || '';
  return name ? name.slice(0, 1).toUpperCase() : '?';
};

const formatMessageTime = (value) => {
  if (!value) return '';
  return String(value).slice(5, 16);
};

const toggleDiscussion = (group) => {
  const expanded = !discussionExpanded.value[group.id];
  discussionExpanded.value = { ...discussionExpanded.value, [group.id]: expanded };
  if (expanded && !discussionMessages.value[group.id]) {
    loadMessages(group.id);
  }
};

const loadMessages = (groupId) => {
  discussionLoading.value = { ...discussionLoading.value, [groupId]: true };
  listGroupMessages(groupId).then(res => {
    discussionMessages.value = { ...discussionMessages.value, [groupId]: res.data || [] };
  }).catch(() => {
    ElMessage.error('加载小组讨论失败');
  }).finally(() => {
    discussionLoading.value = { ...discussionLoading.value, [groupId]: false };
  });
};

const onDraftInput = (group) => {
  const text = drafts.value[group.id] || '';
  const atIndex = text.lastIndexOf('@');
  if (atIndex === -1) {
    mention.value = { groupId: null, list: [], startIndex: -1 };
    return;
  }
  const query = text.slice(atIndex + 1);
  if (/\s/.test(query)) {
    mention.value = { groupId: null, list: [], startIndex: -1 };
    return;
  }
  const lower = query.toLowerCase();
  const list = group.members
      .filter(m => m.studentId !== user.id)
      .filter(m => (m.nickname || '').toLowerCase().includes(lower)
          || (m.username || '').toLowerCase().includes(lower))
      .slice(0, 6);
  mention.value = { groupId: group.id, list, startIndex: atIndex };
};

const pickMention = (group, member) => {
  const text = drafts.value[group.id] || '';
  const start = mention.value.startIndex;
  if (start < 0) return;
  drafts.value = { ...drafts.value, [group.id]: text.slice(0, start) + '@' + member.username + ' ' };
  mention.value = { groupId: null, list: [], startIndex: -1 };
};

const sendMessage = (group) => {
  const content = (drafts.value[group.id] || '').trim();
  if (!content) return;
  sending.value = { ...sending.value, [group.id]: true };
  sendGroupMessage(group.id, content).then(() => {
    drafts.value = { ...drafts.value, [group.id]: '' };
    mention.value = { groupId: null, list: [], startIndex: -1 };
    loadMessages(group.id);
  }).catch(() => {
    ElMessage.error('发送失败');
  }).finally(() => {
    sending.value = { ...sending.value, [group.id]: false };
  });
};

const goProject = (projectId) => {
  router.push(`/project/${projectId}`);
};

const isFull = (group) => group.maxMembers > 0 && (group.memberCount || 0) >= group.maxMembers;
const canJoin = (group) => group.joinEnabled && !isFull(group);
const groupTagType = (group) => {
  if (!group.joinEnabled) return 'info';
  if (isFull(group)) return 'warning';
  return 'success';
};
const groupTagText = (group) => {
  if (!group.joinEnabled) return '未开放';
  if (isFull(group)) return '已满';
  return '可加入';
};

const loadData = async () => {
  loading.value = true;
  try {
    const dashboard = await getPersonalDashboard();
    const dashboardProjects = (dashboard.data && dashboard.data.projects) || [];

    const result = [];
    for (const project of dashboardProjects) {
      const res = await listGroups(project.id).catch(() => ({data: []}));
      const groups = res.data || [];
      const joinedGroup = groups.find(group => group.joinedByMe);
      if (joinedGroup) {
        const membersRes = await listGroupMembers(joinedGroup.id).catch(() => ({data: []}));
        result.push({
          id: project.id,
          name: project.name,
          groups,
          myGroup: {
            ...joinedGroup,
            groupName: joinedGroup.name || joinedGroup.groupNo || '未命名小组',
            members: membersRes.data || [],
            myRole: joinedGroup.leaderId === user.id ? '组长' : '组员'
          }
        });
      } else {
        result.push({id: project.id, name: project.name, groups, myGroup: null});
      }
    }
    projects.value = result;
  } catch (error) {
    ElMessage.error('加载小组信息失败');
  } finally {
    loading.value = false;
  }
};

const join = (group) => {
  ElMessageBox.confirm(`确定加入小组「${group.name || group.groupNo}」吗？`, '加入小组', {type: 'info'})
      .then(() => {
        joinGroup(group.id).then(() => {
          ElMessage.success('已加入小组');
          loadData();
        });
      }).catch(() => {});
};

const leave = (group) => {
  ElMessageBox.confirm(`确定退出小组「${group.groupName}」吗？退出后可重新选择其他小组。`, '退出小组', {type: 'warning'})
      .then(() => {
        leaveGroup(group.id).then(() => {
          ElMessage.success('已退出小组');
          loadData();
        });
      }).catch(() => {});
};

const renameGroupName = (group) => {
  ElMessageBox.prompt('请输入新的小组名称', '修改小组名称', {
    inputValue: group.name || '',
    inputValidator: value => (value && value.trim() ? true : '小组名称不能为空'),
    confirmButtonText: '保存',
    cancelButtonText: '取消'
  }).then(({value}) => {
    renameGroup(group.id, value.trim()).then(() => {
      ElMessage.success('小组名称已更新');
      loadData();
    });
  }).catch(() => {});
};

onMounted(loadData);
</script>

<style scoped>
.group-page {
  padding: 28px 32px;
  background: #f5f7fa;
  min-height: calc(100vh - 60px);
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #1f2d3d;
}

.page-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #909399;
}

.btn-icon {
  margin-right: 4px;
}

.empty-panel {
  padding: 64px 20px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  text-align: center;
}

.empty-icon {
  font-size: 40px;
  color: #c0c4cc;
}

.empty-title {
  margin: 12px 0 4px;
  font-size: 15px;
  color: #606266;
}

.empty-sub {
  margin: 0;
  font-size: 13px;
  color: #909399;
}

.project-sections {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.project-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.project-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.group-leader {
  margin-top: 4px;
  font-size: 12px;
  color: #909399;
}

.group-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 20px;
}

.group-card {
  padding: 18px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
}

.group-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.project-name {
  font-size: 12px;
  color: #909399;
}

.group-name {
  margin-top: 4px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 4px;
}

.rename-btn {
  padding: 0;
}

.group-meta {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f2f5;
  font-size: 12px;
  color: #909399;
}

.group-meta .el-button {
  margin-left: auto;
}

.group-desc {
  margin-top: 12px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 12px;
}

.member-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 6px;
}

.member-row.is-me {
  background: #f5f8ff;
}

.member-avatar {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f0f2f5;
  color: #606266;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.member-main {
  flex: 1;
  min-width: 0;
}

.member-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #303133;
}

.me-tag {
  padding: 0 5px;
  border-radius: 3px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 11px;
}

.member-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.discussion {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #f0f2f5;
}

.discussion-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.discussion-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.discussion-body {
  margin-top: 10px;
}

.discussion-list {
  max-height: 240px;
  overflow-y: auto;
  padding: 8px;
  background: #fafbfc;
  border-radius: 8px;
  min-height: 64px;
}

.message-row {
  display: flex;
  gap: 10px;
  padding: 6px 0;
}

.msg-avatar {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #eef2f7;
  color: #606266;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}

.msg-main {
  flex: 1;
  min-width: 0;
  text-align: left;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
}

.msg-name {
  color: #303133;
  font-weight: 500;
}

.msg-time {
  color: #c0c4cc;
}

.msg-content {
  margin-top: 2px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
}

.discussion-empty {
  padding: 20px 0;
  text-align: center;
  font-size: 12px;
  color: #909399;
}

.discussion-editor {
  margin-top: 8px;
}

.mention-list {
  margin-top: 4px;
  max-height: 140px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.mention-item {
  padding: 6px 10px;
  font-size: 13px;
  color: #303133;
  cursor: pointer;
}

.mention-item:hover {
  background: #f5f7fa;
}

.mention-account {
  margin-left: 6px;
  font-size: 12px;
  color: #909399;
}

.discussion-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 8px;
}

.editor-tip {
  font-size: 12px;
  color: #909399;
  margin-right: auto;
}

@media (max-width: 768px) {
  .group-page {
    padding: 20px 16px;
  }

  .group-grid {
    grid-template-columns: 1fr;
  }
}
</style>
