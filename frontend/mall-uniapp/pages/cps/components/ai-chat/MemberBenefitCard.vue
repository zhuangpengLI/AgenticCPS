<template>
  <view class="benefits"><view v-for="benefit in block.benefits || []" :key="benefit.title || benefit.label" class="benefit"><text class="title">{{ benefit.title || benefit.label }}</text><text class="value">{{ benefit.value || benefit.description }}</text></view></view>
</template>

<script setup>
  defineProps({ block: { type: Object, default: () => ({}) } });
</script>

<style scoped lang="scss">
  .benefits { display: flex; margin-top: 16rpx; flex-direction: column; gap: 10rpx; }
  .benefit { display: flex; padding: 14rpx 16rpx; border-radius: 12rpx; background: #f3f7ff; justify-content: space-between; gap: 18rpx; }
  .title { color: #243858; font-size: 23rpx; }
  .value { color: #103ea6; font-size: 23rpx; font-weight: 700; text-align: right; }
</style>
