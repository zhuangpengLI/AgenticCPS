<template>
  <view v-if="blocks && blocks.length" class="blocks">
    <view v-for="block in blocks" :key="block.id || `${block.type}-${block.title}`" class="block">
      <view v-if="block.title" class="block-title"><text class="block-mark">✦</text>{{ block.title }}</view>
      <view v-if="block.subtitle" class="block-subtitle">{{ block.subtitle }}</view>
      <view v-if="block.body" class="block-body">{{ block.body }}</view>
      <RebateSummaryCard v-if="block.type === 'REBATE_SUMMARY'" :block="block" />
      <MemberBenefitCard v-else-if="block.type === 'MEMBER_BENEFITS'" :block="block" />
      <ProductCompareCard v-else-if="block.type === 'PRODUCT_COMPARE'" :block="block" @action="$emit('action', $event)" />
      <ProductRecommendCard v-else-if="block.items && block.items.length" :block="block" @action="$emit('action', $event)" />
      <FollowUpActions :actions="block.actions || []" @action="$emit('action', { action: $event, block })" />
    </view>
  </view>
</template>

<script setup>
  import FollowUpActions from './FollowUpActions.vue';
  import MemberBenefitCard from './MemberBenefitCard.vue';
  import ProductCompareCard from './ProductCompareCard.vue';
  import ProductRecommendCard from './ProductRecommendCard.vue';
  import RebateSummaryCard from './RebateSummaryCard.vue';
  defineProps({ blocks: { type: Array, default: () => [] } });
  defineEmits(['action']);
</script>

<style scoped lang="scss">
  .blocks { margin-top: 18rpx; }
  .block { margin-bottom: 18rpx; padding: 24rpx; border: 1rpx solid #e8edf5; border-radius: 22rpx; background: #fff; box-shadow: 0 10rpx 26rpx rgba(35,58,99,.06); }
  .block-title { color: #15233f; font-size: 29rpx; font-weight: 800; }
  .block-mark { margin-right: 10rpx; color: #f28a28; }
  .block-subtitle, .block-body { margin-top: 10rpx; color: #708099; font-size: 24rpx; line-height: 1.6; }
</style>
