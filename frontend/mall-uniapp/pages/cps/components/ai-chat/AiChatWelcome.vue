<template>
  <view class="welcome">
    <view class="hero-ring">
      <image v-if="avatar" class="hero-avatar" :src="avatar" mode="aspectFill" />
      <text v-else class="hero-letter">CPS</text>
    </view>
    <view class="hero-title">{{ name || '消费助手已就位' }} ✨</view>
    <view class="hero-subtitle">说出你的需求，越具体推荐越精准</view>
    <view class="status-line"><text class="status-dots">•••</text> 正在聆听...</view>
    <scroll-view class="prompt-scroll" scroll-x :show-scrollbar="false">
      <view v-for="prompt in prompts" :key="prompt" class="prompt-chip" @tap="$emit('prompt', prompt)">{{ prompt }}</view>
    </scroll-view>
    <view class="hint">← 左滑查看更多示例</view>
    <view class="trust-card"><text class="trust-icon">✦</text><text>多平台比价、优惠与返利综合参考，帮你省心做决定。</text></view>
  </view>
</template>

<script setup>
  defineProps({
    name: { type: String, default: '' },
    avatar: { type: String, default: '' },
    prompts: { type: Array, default: () => [] },
  });
  defineEmits(['prompt']);
</script>

<style scoped lang="scss">
  .welcome { padding: 66rpx 32rpx 34rpx; text-align: center; }
  .hero-ring { display: flex; width: 178rpx; height: 178rpx; margin: 0 auto 30rpx; align-items: center; justify-content: center; border: 14rpx solid transparent; border-radius: 50%; background: linear-gradient(#fff, #fff) padding-box, linear-gradient(135deg, #2f6ceb 10%, #7596ed 55%, #ef9c52) border-box; box-shadow: 0 20rpx 48rpx rgba(47,108,235,.14); }
  .hero-avatar { width: 122rpx; height: 122rpx; border-radius: 50%; }
  .hero-letter { color: #103ea6; font-size: 37rpx; font-weight: 800; }
  .hero-title { color: #15233f; font-size: 39rpx; font-weight: 800; }
  .hero-subtitle { margin-top: 12rpx; color: #8b96a8; font-size: 25rpx; }
  .status-line { margin-top: 30rpx; color: #103ea6; font-size: 27rpx; font-weight: 700; }
  .status-dots { margin-right: 10rpx; color: #8faee9; letter-spacing: 9rpx; }
  .prompt-scroll { width: 100%; margin-top: 34rpx; white-space: nowrap; text-align: left; }
  .prompt-chip { display: inline-flex; min-height: 68rpx; margin-right: 16rpx; padding: 0 24rpx; align-items: center; border: 1rpx solid #e6ebf4; border-radius: 34rpx; color: #364968; background: #fff; box-shadow: 0 8rpx 18rpx rgba(35,58,99,.05); font-size: 24rpx; }
  .hint { margin-top: 20rpx; color: #a4adbb; font-size: 22rpx; }
  .trust-card { display: flex; margin-top: 30rpx; padding: 22rpx 24rpx; align-items: flex-start; border: 1rpx solid #dfeee7; border-radius: 18rpx; color: #4e6470; background: #effaf4; font-size: 24rpx; line-height: 1.6; text-align: left; }
  .trust-icon { margin-right: 12rpx; color: #f28a28; font-size: 29rpx; }
</style>
