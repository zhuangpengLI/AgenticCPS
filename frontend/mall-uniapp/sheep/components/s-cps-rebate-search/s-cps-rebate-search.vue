<template>
  <view class="rebate-search" :style="panelStyle">
    <text class="kicker">{{ content.kicker }}</text>
    <text class="title">{{ content.title }}</text>
    <text class="description">{{ content.description }}</text>
    <view class="search-box">
      <text class="search-icon _icon-search" />
      <input
        v-model="keyword"
        class="search-input"
        :placeholder="content.placeholder"
        confirm-type="search"
        @confirm="startSearch"
      />
      <button class="ss-reset-button search-button" @tap="startSearch">
        {{ content.buttonText }}
      </button>
    </view>
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import sheep from '@/sheep';

  const props = defineProps({
    data: { type: Object, default: () => ({}) },
    styles: { type: Object, default: () => ({}) },
  });

  const keyword = ref('');
  const content = computed(() => ({
    kicker: props.data.kicker || 'AgenticCPS · 今日高返',
    title: props.data.title || '先查优惠，再下单',
    description: props.data.description || '搜索商品或粘贴链接/口令，查看券后价与预估返利',
    placeholder: props.data.placeholder || '搜商品或粘贴链接/口令',
    buttonText: props.data.buttonText || '查优惠',
  }));
  const panelStyle = computed(() => ({
    borderRadius: `${Number(props.styles.borderRadius || 0)}px`,
  }));

  function startSearch() {
    const value = keyword.value.trim();
    if (!value) {
      sheep.$router.go('/pages/cps/goods');
      return;
    }
    const isProductContent = /https?:\/\/|￥|€|tb\.cn|m\.tb|yangkeduo|jd\.com/i.test(value);
    sheep.$router.go('/pages/cps/goods', {
      [isProductContent ? 'content' : 'keyword']: encodeURIComponent(value),
    });
  }
</script>

<style lang="scss" scoped>
  .rebate-search {
    box-sizing: border-box;
    padding: 34rpx 30rpx;
    overflow: hidden;
    color: #fff;
    background: linear-gradient(135deg, #e34b38 0%, #ff843d 68%, #ffc45f 100%);
  }
  .kicker,
  .title,
  .description {
    display: block;
  }
  .kicker {
    font-size: 22rpx;
    opacity: 0.86;
  }
  .title {
    margin-top: 10rpx;
    font-size: 40rpx;
    font-weight: 700;
  }
  .description {
    margin-top: 10rpx;
    font-size: 24rpx;
    line-height: 1.5;
    opacity: 0.9;
  }
  .search-box {
    display: flex;
    align-items: center;
    height: 84rpx;
    margin-top: 28rpx;
    padding-left: 24rpx;
    border: 1rpx solid rgba(255, 255, 255, 0.5);
    border-radius: 16rpx;
    background: #fff;
  }
  .search-icon {
    flex: 0 0 auto;
    color: #a2a4aa;
    font-size: 30rpx;
  }
  .search-input {
    min-width: 0;
    height: 84rpx;
    margin-left: 14rpx;
    flex: 1;
    color: #24262d;
    font-size: 26rpx;
  }
  .search-button {
    flex: 0 0 auto;
    height: 64rpx;
    margin: 0 10rpx;
    padding: 0 26rpx;
    border-radius: 12rpx;
    color: #fff;
    background: #20222a;
    font-size: 24rpx;
    line-height: 64rpx;
  }
</style>
