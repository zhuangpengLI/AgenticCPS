<template>
  <view class="ai-card" :style="panelStyle" @tap="openChat">
    <view class="ai-heading"><view class="ai-icon _icon-magic" /><text>{{ content.title }}</text></view>
    <view class="ai-description">{{ content.description }}</view>
    <view class="ai-prompt"><text class="_icon-magic" />{{ content.placeholder }}</view>
    <view class="ai-action">{{ content.buttonText }}<text class="_icon-arrow-right" /></view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  const props = defineProps({ data: { type: Object, default: () => ({}) }, styles: { type: Object, default: () => ({}) } });
  const content = computed(() => ({ title: props.data.title || '返利 AI 助手', description: props.data.description || '问价格、优惠和返利，帮你快速找到合适商品', placeholder: props.data.placeholder || '例如：帮我找一款高返蓝牙耳机', buttonText: props.data.buttonText || '开始对话', roleId: props.data.roleId }));
  const panelStyle = computed(() => ({ borderRadius: `${Number(props.styles.borderRadius || 8)}px` }));
  function openChat() { if (!sheep.$store('user').isLogin) { showAuthModal(); return; } sheep.$router.go('/pages/cps/ai-chat', content.value.roleId ? { roleId: content.value.roleId } : {}); }
</script>

<style lang="scss" scoped>
  .ai-card { padding: 28rpx; color: #fff; background: linear-gradient(135deg, #202833, #394d5e); }
  .ai-heading { display: flex; align-items: center; gap: 14rpx; font-size: 32rpx; font-weight: 600; }
  .ai-icon { display: inline-flex; width: 50rpx; height: 50rpx; align-items: center; justify-content: center; border-radius: 12rpx; background: #f4513b; font-size: 27rpx; }
  .ai-description { margin-top: 12rpx; color: rgba(255,255,255,.72); font-size: 23rpx; }
  .ai-prompt { margin-top: 22rpx; padding: 18rpx; border-radius: 12rpx; color: rgba(255,255,255,.78); background: rgba(255,255,255,.1); font-size: 23rpx; }
  .ai-prompt text { margin-right: 8rpx; }
  .ai-action { display: flex; align-items: center; justify-content: space-between; margin-top: 18rpx; padding: 16rpx 20rpx; border-radius: 10rpx; color: #fff; background: #f4513b; font-size: 24rpx; }
</style>
