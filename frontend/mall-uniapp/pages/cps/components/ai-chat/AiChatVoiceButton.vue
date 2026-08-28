<template>
  <button class="voice" :class="{ recording }" :aria-label="'打开语音输入'" @tap="$emit('open')"
    >🎙</button
  >
</template>
<script setup>
  defineProps({ recording: Boolean });
  defineEmits(['open']);
</script>
<style scoped lang="scss">
  .voice {
    min-width: 68rpx;
    height: 68rpx;
    margin: 0;
    padding: 0;
    border: 0;
    border-radius: 18rpx;
    color: #103ea6;
    background: #edf3ff;
    font-size: 28rpx;
    line-height: 68rpx;
  }
  .voice.recording {
    color: #fff;
    background: #d66b08;
  }
  .voice::after {
    border: 0;
  }
</style>
