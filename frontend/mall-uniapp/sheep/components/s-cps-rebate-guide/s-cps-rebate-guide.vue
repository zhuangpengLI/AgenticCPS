<template>
  <view class="rebate-guide" :style="panelStyle">
    <text class="section-title">{{ content.title }}</text>
    <view v-if="content.steps.length" class="steps">
      <view v-for="(step, index) in content.steps" :key="`${index}-${step}`" class="step">
        <text class="step-index">{{ index + 1 }}</text>
        <text class="step-text">{{ step }}</text>
      </view>
    </view>
    <view v-else class="empty">暂无返利说明</view>
    <view v-if="content.notice" class="notice">
      <text class="notice-mark">!</text>
      <text class="notice-text">{{ content.notice }}</text>
    </view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';

  const props = defineProps({
    data: { type: Object, default: () => ({}) },
    styles: { type: Object, default: () => ({}) },
  });
  const fallbackSteps = [
    '搜索商品或粘贴商品链接',
    '查看券后价和预估返利',
    '领券购买，结算后返利到账',
  ];
  const content = computed(() => {
    const configuredSteps = Array.isArray(props.data.steps) ? props.data.steps : fallbackSteps;
    return {
      title: props.data.title || '如何获得返利',
      steps: configuredSteps
        .map((item) => String(item || '').trim())
        .filter(Boolean)
        .slice(0, 5),
      notice: String(props.data.notice || '预估返利仅供参考，实际金额以订单结算结果为准。').trim(),
    };
  });
  const panelStyle = computed(() => ({
    borderRadius: `${Number(props.styles.borderRadius || 0)}px`,
  }));
</script>

<style lang="scss" scoped>
  .rebate-guide {
    box-sizing: border-box;
    overflow: hidden;
  }
  .section-title {
    display: block;
    color: #24262d;
    font-size: 30rpx;
    font-weight: 650;
  }
  .steps {
    margin-top: 22rpx;
  }
  .step {
    display: flex;
    min-height: 54rpx;
    margin-top: 14rpx;
    align-items: flex-start;
  }
  .step-index {
    display: flex;
    width: 42rpx;
    height: 42rpx;
    flex: 0 0 42rpx;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: #fff;
    background: #e6533c;
    font-size: 21rpx;
    font-weight: 700;
  }
  .step-text {
    margin-left: 18rpx;
    padding-top: 5rpx;
    color: #555861;
    font-size: 25rpx;
    line-height: 1.55;
  }
  .notice {
    display: flex;
    margin-top: 24rpx;
    padding: 18rpx 20rpx;
    align-items: flex-start;
    border-radius: 12rpx;
    color: #8b622c;
    background: #fff8e7;
  }
  .notice-mark {
    display: flex;
    width: 30rpx;
    height: 30rpx;
    flex: 0 0 30rpx;
    align-items: center;
    justify-content: center;
    border: 2rpx solid currentColor;
    border-radius: 50%;
    font-size: 20rpx;
    font-weight: 700;
  }
  .notice-text {
    margin-left: 12rpx;
    font-size: 22rpx;
    line-height: 1.5;
  }
  .empty {
    padding: 40rpx 0 20rpx;
    color: #999ca5;
    font-size: 24rpx;
    text-align: center;
  }
</style>
