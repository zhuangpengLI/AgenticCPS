<template>
  <scroll-view class="message-scroll" scroll-y :scroll-into-view="anchor" :scroll-with-animation="true">
    <AiChatMessage v-for="(item, index) in messages" :id="`ai-msg-${index}`" :key="item.id || `${item.type}-${index}`" :message="item" @action="$emit('action', $event)" />
    <view v-if="loading" class="message-row is-ai"><view class="assistant-card loading-card"><text class="loading-dot">●</text> 正在整理平台价格和优惠…</view></view>
    <view v-if="error" class="error-row" @tap="$emit('retry')">{{ error }}，点击重试</view>
  </scroll-view>
</template>

<script setup>
  import AiChatMessage from './AiChatMessage.vue';
  defineProps({ messages: { type: Array, default: () => [] }, anchor: { type: String, default: '' }, loading: Boolean, error: { type: String, default: '' } });
  defineEmits(['action', 'retry']);
</script>

<style scoped lang="scss">
  .message-scroll { height: 100%; padding: 22rpx 32rpx 250rpx; box-sizing: border-box; }
  .message-row { display: flex; margin-bottom: 22rpx; }
  .is-user { justify-content: flex-end; }
  .user-bubble { max-width: 82%; padding: 20rpx 24rpx; border-radius: 24rpx 24rpx 6rpx 24rpx; color: #fff; background: linear-gradient(135deg, #103ea6, #2f6ceb); box-shadow: 0 10rpx 24rpx rgba(16,62,166,.18); font-size: 27rpx; line-height: 1.6; white-space: pre-wrap; }
  .assistant-card { max-width: 94%; padding: 22rpx 24rpx; border: 1rpx solid #e8edf5; border-radius: 8rpx 24rpx 24rpx 24rpx; color: #263653; background: #fff; box-shadow: 0 10rpx 26rpx rgba(35,58,99,.06); }
  .assistant-content { color: #263653; font-size: 27rpx; line-height: 1.68; white-space: pre-wrap; overflow-wrap: anywhere; }
  .reasoning { margin-bottom: 12rpx; color: #8090a8; font-size: 22rpx; }
  .reasoning-content { margin-bottom: 14rpx; padding: 14rpx; border-radius: 12rpx; color: #73819a; background: #f7f9fc; font-size: 22rpx; line-height: 1.5; }
  .tool-status-list { margin-top: 16rpx; padding-top: 14rpx; border-top: 1rpx solid #eef1f6; }
  .tool-status { color: #71809a; font-size: 22rpx; line-height: 1.7; }
  .tool-dot { display: inline-block; width: 12rpx; height: 12rpx; margin-right: 10rpx; border-radius: 50%; background: #2f6ceb; }
  .tool-status.SUCCEEDED .tool-dot { background: #16a66f; }
  .tool-status.FAILED .tool-dot { background: #dc5b55; }
  .sources { margin-top: 16rpx; color: #9aa5b5; font-size: 21rpx; }
  .loading-card { color: #6d7e99; font-size: 24rpx; }
  .loading-dot { margin-right: 8rpx; color: #2f6ceb; animation: pulse 1.1s infinite; }
  .error-row { margin: 12rpx 0; color: #c54e48; font-size: 23rpx; text-align: center; }
  .message-attachments { display: flex; margin-bottom: 12rpx; gap: 10rpx; }
  .message-attachments image { width: 120rpx; height: 120rpx; border-radius: 12rpx; }
  @keyframes pulse { 0%, 100% { opacity: .35; } 50% { opacity: 1; } }
</style>
