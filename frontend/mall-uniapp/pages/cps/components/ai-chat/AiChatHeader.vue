<template>
  <view class="chat-header">
    <view class="avatar-wrap">
      <image v-if="avatar" class="avatar" :src="avatar" mode="aspectFill" />
      <view v-else class="avatar avatar-fallback">CPS</view>
      <view class="online-dot" />
    </view>
    <view class="identity">
      <view class="name">{{ name || 'AgenticCPS 消费助手' }}</view>
      <view class="subtitle">{{ subtitle || '帮你搜好货、比价格、查返利' }}</view>
    </view>
    <view class="header-actions">
      <text class="header-action" @tap="$emit('history')">☰</text>
      <text class="header-action" @tap="$emit('new-chat')">＋</text>
    </view>
  </view>
</template>

<script setup>
  defineProps({
    name: { type: String, default: '' },
    subtitle: { type: String, default: '' },
    avatar: { type: String, default: '' },
  });
  defineEmits(['history', 'new-chat']);
</script>

<style scoped lang="scss">
  .chat-header { display: flex; align-items: center; padding: 24rpx 32rpx; border-bottom: 1rpx solid #edf0f5; background: #fff; }
  .avatar-wrap { position: relative; flex: none; }
  .avatar { width: 76rpx; height: 76rpx; border-radius: 50%; }
  .avatar-fallback { display: flex; align-items: center; justify-content: center; color: #fff; background: linear-gradient(135deg, #103ea6, #2f6ceb); font-size: 22rpx; font-weight: 700; }
  .online-dot { position: absolute; right: -2rpx; bottom: 2rpx; width: 18rpx; height: 18rpx; border: 3rpx solid #fff; border-radius: 50%; background: #19bd79; }
  .identity { min-width: 0; margin-left: 18rpx; flex: 1; }
  .name { overflow: hidden; color: #15233f; font-size: 31rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
  .subtitle { margin-top: 6rpx; overflow: hidden; color: #8994a7; font-size: 22rpx; text-overflow: ellipsis; white-space: nowrap; }
  .header-actions { display: flex; gap: 20rpx; color: #60708b; font-size: 37rpx; }
  .header-action { display: flex; width: 48rpx; height: 48rpx; align-items: center; justify-content: center; }
</style>
