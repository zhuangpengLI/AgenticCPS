<template>
  <view v-if="actions.length" class="actions">
    <button v-for="action in actions" :key="`${action.type}-${action.label}`" class="action" @tap="$emit('action', action)">{{ action.label }}</button>
  </view>
</template>

<script setup>
  defineProps({ actions: { type: Array, default: () => [] } });
  defineEmits(['action']);
</script>

<style scoped lang="scss">
  .actions { display: flex; margin-top: 18rpx; flex-wrap: wrap; gap: 12rpx; }
  .action { margin: 0; padding: 0 20rpx; border: 1rpx solid #d6e2fb; border-radius: 26rpx; color: #103ea6; background: #f3f7ff; font-size: 21rpx; line-height: 52rpx; }
  .action::after { border: 0; }
</style>
