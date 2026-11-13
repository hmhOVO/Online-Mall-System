<template>
  <div :class="['comment-item', { 'nested': depth >= 2 }]">
    <div class="comment-header">
      <el-avatar :size="28" class="comment-avatar">{{ avatarText(comment.nickname || comment.username) }}</el-avatar>
      <span class="comment-user">{{ comment.nickname || comment.username }}</span>
      <span class="comment-time">{{ comment.createdAt }}</span>
      <el-tag v-if="comment.isResolved" type="success" size="small" style="margin-left: 8px">已解决</el-tag>
    </div>
    <div class="comment-content" v-html="renderedContent"></div>
    <div class="comment-actions">
      <el-button link type="primary" size="small" @click="$emit('reply', comment)">回复</el-button>
      <el-button v-if="canResolve" link :type="comment.isResolved ? 'info' : 'success'" size="small" @click="$emit('toggle-resolve', comment, !comment.isResolved)">
        {{ comment.isResolved ? '标记未解决' : '标记已解决' }}
      </el-button>
    </div>
    <div v-if="comment.replies && comment.replies.length" class="comment-children">
      <TaskCommentItem
        v-for="reply in comment.replies"
        :key="reply.id"
        :comment="reply"
        :current-user="currentUser"
        :can-resolve="canResolve"
        :depth="depth + 1"
        @reply="$emit('reply', $event)"
        @toggle-resolve="(c, r) => $emit('toggle-resolve', c, r)"
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  comment: { type: Object, required: true },
  currentUser: { type: Object, default: () => ({}) },
  canResolve: { type: Boolean, default: false },
  depth: { type: Number, default: 1 }
});

defineEmits(['reply', 'toggle-resolve']);

const avatarText = (name) => (name ? name.charAt(0).toUpperCase() : '?');

const escapeHtml = (text) => {
  if (!text) return '';
  const div = document.createElement('div');
  div.textContent = text;
  return div.innerHTML;
};

const renderedContent = computed(() => {
  return escapeHtml(props.comment.content)
    .replace(/@([^\s@]+)/g, '<span class="mention">@$1</span>');
});
</script>

<style scoped>
.comment-item {
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.comment-avatar {
  background: #67c23a;
  color: #fff;
  font-size: 12px;
}
.comment-user {
  font-weight: bold;
  font-size: 14px;
}
.comment-time {
  font-size: 12px;
  color: #909399;
}
.comment-content {
  color: #333;
  line-height: 1.6;
  margin-bottom: 6px;
  padding-left: 36px;
}
.comment-content :deep(.mention) {
  color: #409eff;
  font-weight: 500;
  cursor: default;
}
.comment-actions {
  padding-left: 36px;
}
.comment-children {
  margin-left: 36px;
  margin-top: 8px;
}
.comment-item.nested {
  padding-left: 0;
}
.comment-item.nested .comment-children {
  margin-left: 0;
}
</style>
