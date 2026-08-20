<template>
  <view class="message-row" :class="isUser ? 'is-user' : 'is-ai'">
    <view v-if="isUser" class="user-bubble"><view v-if="message.attachmentUrls && message.attachmentUrls.length" class="attachments"><image v-for="url in message.attachmentUrls" :key="url" :src="url" mode="aspectFill" /></view><text>{{ message.content }}</text></view>
    <view v-else class="assistant-card">
      <view v-if="message.reasoningContent" class="reasoning" @tap="message.reasoningOpen = !message.reasoningOpen">{{ message.reasoningOpen ? '收起思考' : '查看思考过程' }}</view>
      <view v-if="message.reasoningOpen" class="reasoning-content">{{ message.reasoningContent }}</view>
      <view class="content">{{ formatContent(message.content) }}</view>
      <view v-if="message.toolExecutions && message.toolExecutions.length" class="tool-list"><view v-for="execution in message.toolExecutions" :key="execution.executionId" class="tool" :class="execution.status"><text />{{ execution.message || execution.label }}</view></view>
      <AiChatBlockRenderer :blocks="message.blocks" @action="$emit('action', $event)" />
      <view v-if="message.webSearchPages && message.webSearchPages.length" class="sources">已参考 {{ message.webSearchPages.length }} 条公开信息</view>
    </view>
  </view>
</template>
<script setup>
  import { computed } from 'vue';
  import AiChatBlockRenderer from './AiChatBlockRenderer.vue';
  const props = defineProps({ message: { type: Object, required: true } });
  defineEmits(['action']);
  const isUser = computed(() => String(props.message.type || '').toUpperCase() === 'USER');
  const formatContent = (content = '') => String(content || '').replace(/\r\n/g, '\n').replace(/\*\*(.*?)\*\*/g, '$1').replace(/^#{1,4}\s*/gm, '').replace(/^[-*]\s+/gm, '• ');
</script>
<style scoped lang="scss">
  .message-row { display: flex; margin-bottom: 22rpx; }
  .is-user { justify-content: flex-end; }
  .user-bubble { max-width: 82%; padding: 20rpx 24rpx; border-radius: 24rpx 24rpx 6rpx 24rpx; color: #fff; background: linear-gradient(135deg, #103ea6, #2f6ceb); box-shadow: 0 10rpx 24rpx rgba(16,62,166,.18); font-size: 27rpx; line-height: 1.6; white-space: pre-wrap; }
  .assistant-card { max-width: 94%; padding: 22rpx 24rpx; border: 1rpx solid #e8edf5; border-radius: 8rpx 24rpx 24rpx 24rpx; color: #263653; background: #fff; box-shadow: 0 10rpx 26rpx rgba(35,58,99,.06); }
  .content { color: #263653; font-size: 27rpx; line-height: 1.68; white-space: pre-wrap; overflow-wrap: anywhere; }
  .reasoning { margin-bottom: 12rpx; color: #62738e; font-size: 22rpx; }
  .reasoning-content { margin-bottom: 14rpx; padding: 14rpx; border-radius: 12rpx; color: #61718a; background: #f7f9fc; font-size: 22rpx; line-height: 1.5; }
  .tool-list { margin-top: 16rpx; padding-top: 14rpx; border-top: 1rpx solid #eef1f6; }
  .tool { color: #60718c; font-size: 22rpx; line-height: 1.7; }
  .tool text { display: inline-block; width: 12rpx; height: 12rpx; margin-right: 10rpx; border-radius: 50%; background: #2f6ceb; }
  .tool.SUCCEEDED text { background: #11845b; }
  .tool.FAILED text { background: #c6413a; }
  .sources { margin-top: 16rpx; color: #6f7c8f; font-size: 21rpx; }
  .attachments { display: flex; margin-bottom: 12rpx; gap: 10rpx; }
  .attachments image { width: 120rpx; height: 120rpx; border-radius: 12rpx; }
</style>
