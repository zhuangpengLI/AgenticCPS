<template>
  <view class="grid">
    <view v-for="item in items" :key="item.label" class="item"><text>{{ item.label }}</text><text class="value">{{ item.value }}</text></view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';
  const props = defineProps({ block: { type: Object, default: () => ({}) } });
  const money = (value) => value === undefined || value === null || value === '' ? '--' : Number(value).toFixed(2);
  const items = computed(() => [
    { label: '可用返利', value: `¥${money(props.block.availableBalance)}` },
    { label: '待结算', value: `¥${money(props.block.frozenBalance)}` },
    { label: '累计返利', value: `¥${money(props.block.totalRebate)}` },
  ]);
</script>

<style scoped lang="scss">
  .grid { display: flex; margin-top: 18rpx; gap: 12rpx; }
  .item { min-width: 0; padding: 16rpx; border-radius: 14rpx; background: #effaf4; flex: 1; }
  .item text { display: block; }
  .item text { color: #687887; font-size: 20rpx; }
  .item .value { margin-top: 8rpx; color: #11845b; font-size: 27rpx; font-weight: 700; }
</style>
