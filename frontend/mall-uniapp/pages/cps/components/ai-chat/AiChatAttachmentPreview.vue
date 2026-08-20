<template><view v-if="attachments.length" class="attachments"><view v-for="item in attachments" :key="item.url" class="attachment"><image :src="item.url" mode="aspectFill" /><text aria-label="删除图片" @tap="$emit('remove', item)">×</text></view></view></template>
<script setup>
  defineProps({ attachments: { type: Array, default: () => [] } });
  defineEmits(['remove']);
</script>
<style scoped lang="scss">
  .attachments { display: flex; margin-bottom: 10rpx; gap: 12rpx; }
  .attachment { position: relative; }
  image { width: 76rpx; height: 76rpx; border-radius: 12rpx; }
  text { position: absolute; top: -14rpx; right: -12rpx; width: 32rpx; height: 32rpx; border-radius: 50%; color: #fff; background: #46546a; font-size: 24rpx; line-height: 31rpx; text-align: center; }
</style>
