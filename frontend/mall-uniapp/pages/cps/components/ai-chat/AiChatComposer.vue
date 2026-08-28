<template>
  <view class="composer-shell">
    <AiChatAttachmentPreview
      :attachments="attachments"
      @remove="$emit('remove-attachment', $event)"
    />
    <view class="composer">
      <button class="icon-btn" @tap="$emit('choose-image')">＋</button>
      <textarea
        v-model="inputValue"
        class="composer-input"
        :placeholder="recording ? '松开结束录音' : '问问返利 AI，搜商品或查返利…'"
        :maxlength="1000"
        auto-height
        @confirm="$emit('send')"
      />
      <AiChatVoiceButton :recording="recording" @open="openVoiceDialog" />
      <button
        class="send-btn"
        :disabled="disabled || (!inputValue.trim() && !attachments.length)"
        @tap="$emit('send')"
        >发送</button
      >
    </view>
    <view v-if="voiceStatus" class="voice-status">{{ voiceStatus }}</view>
    <view v-if="voiceDialog" class="voice-mask" @tap="closeVoiceDialog">
      <view class="voice-dialog" @tap.stop>
        <view class="voice-dialog-title">语音输入</view>
        <view class="voice-dialog-status">{{
          recording ? '正在录音…' : voiceStatus || '长按下方按钮开始录音'
        }}</view>
        <view class="voice-dialog-hint">长按录音，松开后自动转成中文并发送</view>
        <button
          class="hold-record-btn"
          :class="{ recording }"
          :disabled="disabled || !!voiceStatus"
          @touchstart.stop.prevent="$emit('record-start')"
          @touchend.stop.prevent="$emit('record-stop')"
          @touchcancel.stop.prevent="$emit('record-stop')"
        >
          <text class="hold-record-icon">{{ recording ? '●' : '🎙' }}</text>
          <text>{{ recording ? '松开结束' : '按住说话' }}</text>
        </button>
        <button
          class="voice-cancel-btn"
          :disabled="recording || !!voiceStatus"
          @tap="closeVoiceDialog"
          >取消</button
        >
      </view>
    </view>
  </view>
</template>

<script setup>
  import { computed, onBeforeUnmount, ref, watch } from 'vue';
  import AiChatAttachmentPreview from './AiChatAttachmentPreview.vue';
  import AiChatVoiceButton from './AiChatVoiceButton.vue';
  const props = defineProps({
    modelValue: { type: String, default: '' },
    attachments: { type: Array, default: () => [] },
    disabled: Boolean,
    recording: Boolean,
    voiceStatus: { type: String, default: '' },
  });
  const emits = defineEmits([
    'update:modelValue',
    'send',
    'choose-image',
    'remove-attachment',
    'record-start',
    'record-stop',
    'voice-open',
    'voice-close',
  ]);
  const voiceDialog = ref(false);
  let closeTimer = null;
  const inputValue = computed({
    get: () => props.modelValue,
    set: (value) => emits('update:modelValue', value),
  });
  function openVoiceDialog() {
    if (closeTimer) {
      clearTimeout(closeTimer);
      closeTimer = null;
    }
    voiceDialog.value = true;
    emits('voice-open');
  }
  function closeVoiceDialog() {
    if (closeTimer) {
      clearTimeout(closeTimer);
      closeTimer = null;
    }
    if (!props.recording && !props.voiceStatus) {
      voiceDialog.value = false;
      emits('voice-close');
    }
  }
  watch(
    () => [props.recording, props.voiceStatus],
    ([recording, status]) => {
      if (closeTimer) {
        clearTimeout(closeTimer);
        closeTimer = null;
      }
      if (voiceDialog.value && status === '识别完成，正在发送…') {
        voiceDialog.value = false;
        emits('voice-close');
        return;
      }
      if (voiceDialog.value && !recording && !status) {
        closeTimer = setTimeout(() => {
          closeTimer = null;
          if (!props.recording && !props.voiceStatus) voiceDialog.value = false;
        }, 120);
      }
    },
  );
  onBeforeUnmount(() => {
    if (closeTimer) clearTimeout(closeTimer);
  });
</script>

<style scoped lang="scss">
  .composer-shell {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 10;
    padding: 10rpx 22rpx calc(14rpx + env(safe-area-inset-bottom));
    border-top: 1rpx solid #e9edf4;
    background: rgba(255, 255, 255, 0.96);
    box-shadow: 0 -8rpx 24rpx rgba(35, 58, 99, 0.06);
  }
  .composer {
    display: flex;
    align-items: flex-end;
    gap: 10rpx;
  }
  .icon-btn,
  .send-btn {
    min-width: 68rpx;
    height: 68rpx;
    margin: 0;
    padding: 0;
    border: 0;
    border-radius: 18rpx;
    line-height: 68rpx;
  }
  .icon-btn {
    color: #50617a;
    background: #f0f3f8;
    font-size: 38rpx;
  }
  .send-btn {
    min-width: 110rpx;
    color: #fff;
    background: #103ea6;
    font-size: 24rpx;
  }
  .send-btn[disabled] {
    opacity: 0.45;
  }
  .composer-input {
    min-height: 68rpx;
    max-height: 160rpx;
    padding: 17rpx 18rpx;
    box-sizing: border-box;
    border: 1rpx solid #e4eaf3;
    border-radius: 18rpx;
    color: #1b2a43;
    background: #f9fbfd;
    flex: 1;
    font-size: 25rpx;
    line-height: 1.35;
  }
  .voice-status {
    margin: 8rpx 6rpx 0;
    color: #f28a28;
    font-size: 21rpx;
  }
  .voice-mask {
    position: fixed;
    inset: 0;
    z-index: 30;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 40rpx;
    background: rgba(18, 31, 52, 0.36);
  }
  .voice-dialog {
    width: 590rpx;
    padding: 36rpx 32rpx 28rpx;
    border-radius: 28rpx;
    text-align: center;
    background: #fff;
    box-shadow: 0 24rpx 70rpx rgba(17, 38, 74, 0.2);
  }
  .voice-dialog-title {
    color: #1d2c45;
    font-size: 32rpx;
    font-weight: 700;
  }
  .voice-dialog-status {
    margin-top: 20rpx;
    color: #103ea6;
    font-size: 28rpx;
    font-weight: 600;
  }
  .voice-dialog-hint {
    margin-top: 12rpx;
    color: #7b8799;
    font-size: 22rpx;
  }
  .hold-record-btn {
    display: flex;
    width: 260rpx;
    height: 260rpx;
    margin: 34rpx auto 24rpx;
    padding: 0;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    gap: 14rpx;
    border: 0;
    border-radius: 50%;
    color: #103ea6;
    background: #edf3ff;
    font-size: 24rpx;
    line-height: 1;
  }
  .hold-record-btn.recording {
    color: #fff;
    background: #d66b08;
    transform: scale(1.04);
  }
  .hold-record-btn[disabled] {
    opacity: 0.7;
  }
  .hold-record-icon {
    font-size: 48rpx;
  }
  .voice-cancel-btn {
    width: 180rpx;
    height: 64rpx;
    margin: 0 auto;
    border: 1rpx solid #dfe5ee;
    border-radius: 32rpx;
    color: #68778e;
    background: #fff;
    font-size: 24rpx;
    line-height: 62rpx;
  }
</style>
