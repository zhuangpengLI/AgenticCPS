<template>
  <view class="composer-shell">
    <AiChatAttachmentPreview :attachments="attachments" @remove="$emit('remove-attachment', $event)" />
    <view class="composer">
      <button class="icon-btn" @tap="$emit('choose-image')">＋</button>
      <textarea v-model="inputValue" class="composer-input" :placeholder="recording ? '松开结束录音' : '问问返利 AI，搜商品或查返利…'" :maxlength="1000" auto-height @confirm="$emit('send')" />
      <AiChatVoiceButton :recording="recording" @start="$emit('record-start')" @stop="$emit('record-stop')" />
      <button class="send-btn" :disabled="disabled || (!inputValue.trim() && !attachments.length)" @tap="$emit('send')">发送</button>
    </view>
    <view v-if="voiceStatus" class="voice-status">{{ voiceStatus }}</view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';
  import AiChatAttachmentPreview from './AiChatAttachmentPreview.vue';
  import AiChatVoiceButton from './AiChatVoiceButton.vue';
  const props = defineProps({ modelValue: { type: String, default: '' }, attachments: { type: Array, default: () => [] }, disabled: Boolean, recording: Boolean, voiceStatus: { type: String, default: '' } });
  const emits = defineEmits(['update:modelValue', 'send', 'choose-image', 'remove-attachment', 'record-start', 'record-stop']);
  const inputValue = computed({ get: () => props.modelValue, set: (value) => emits('update:modelValue', value) });
</script>

<style scoped lang="scss">
  .composer-shell { position: fixed; right: 0; bottom: 0; left: 0; z-index: 10; padding: 10rpx 22rpx calc(14rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #e9edf4; background: rgba(255,255,255,.96); box-shadow: 0 -8rpx 24rpx rgba(35,58,99,.06); }
  .composer { display: flex; align-items: flex-end; gap: 10rpx; }
  .icon-btn, .send-btn { min-width: 68rpx; height: 68rpx; margin: 0; padding: 0; border: 0; border-radius: 18rpx; line-height: 68rpx; }
  .icon-btn { color: #50617a; background: #f0f3f8; font-size: 38rpx; }
  .send-btn { min-width: 110rpx; color: #fff; background: #103ea6; font-size: 24rpx; }
  .send-btn[disabled] { opacity: .45; }
  .composer-input { min-height: 68rpx; max-height: 160rpx; padding: 17rpx 18rpx; box-sizing: border-box; border: 1rpx solid #e4eaf3; border-radius: 18rpx; color: #1b2a43; background: #f9fbfd; flex: 1; font-size: 25rpx; line-height: 1.35; }
  .voice-status { margin: 8rpx 6rpx 0; color: #f28a28; font-size: 21rpx; }
</style>
