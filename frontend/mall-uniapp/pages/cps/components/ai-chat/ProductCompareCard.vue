<template><view class="compare"><view class="label">按券后价与预估返利综合比较</view><ProductRecommendCard :block="block" @action="$emit('action', $event)" /></view></template>
<script setup>
  import ProductRecommendCard from './ProductRecommendCard.vue';
  defineProps({ block: { type: Object, default: () => ({}) } });
  defineEmits(['action']);
</script>
<style scoped lang="scss">.label { margin-top: 12rpx; color: #63748e; font-size: 21rpx; }</style>
