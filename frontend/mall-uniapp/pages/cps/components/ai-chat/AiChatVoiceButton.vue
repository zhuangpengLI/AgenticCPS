<template><button class="voice" :class="{ recording }" :aria-label="recording ? '松开结束录音' : '按住说话'" @touchstart="$emit('start')" @touchend="$emit('stop')" @touchcancel="$emit('stop')">{{ recording ? '●' : '🎙' }}</button></template>
<script setup>
  defineProps({ recording: Boolean });
  defineEmits(['start', 'stop']);
</script>
<style scoped lang="scss">
  .voice { min-width: 68rpx; height: 68rpx; margin: 0; padding: 0; border: 0; border-radius: 18rpx; color: #103ea6; background: #edf3ff; font-size: 28rpx; line-height: 68rpx; }
  .voice.recording { color: #fff; background: #d66b08; }
  .voice::after { border: 0; }
</style>
