<template><view v-if="visible" class="mask" @tap.self="$emit('close')"><view class="drawer"><view class="title">历史会话 <text @tap="$emit('close')">×</text></view><scroll-view scroll-y class="list"><view v-for="item in conversations" :key="item.id" class="row" @tap="$emit('select', item)"><view class="conversation-info"><view class="conversation-title">{{ item.title || item.roleName || 'AI 会话' }}</view><text>{{ item.updateTime || item.createTime || '' }}</text></view><text class="delete" @tap.stop="$emit('delete', item)">删除</text></view><view v-if="!conversations.length" class="empty">暂无历史会话</view></scroll-view></view></view></template>
<script setup>
  defineProps({ visible: Boolean, conversations: { type: Array, default: () => [] } });
  defineEmits(['close', 'select', 'delete']);
</script>
<style scoped lang="scss">
  .mask { position: fixed; top: 0; right: 0; bottom: 0; left: 0; z-index: 30; background: rgba(14,27,50,.36); }
  .drawer { position: absolute; top: 0; right: 0; bottom: 0; width: 620rpx; padding: 34rpx 26rpx calc(24rpx + env(safe-area-inset-bottom)); box-sizing: border-box; background: #fff; }
  .title { display: flex; color: #172844; font-size: 31rpx; font-weight: 800; justify-content: space-between; }
  .title text { width: 52rpx; text-align: center; }
  .list { height: calc(100% - 80rpx); margin-top: 24rpx; }
  .row { display: flex; padding: 22rpx 16rpx; border-bottom: 1rpx solid #edf0f5; color: #263653; font-size: 25rpx; align-items: center; gap: 16rpx; }
  .conversation-info { min-width: 0; flex: 1; }
  .conversation-title { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .conversation-info text { display: block; margin-top: 7rpx; color: #77859a; font-size: 20rpx; }
  .delete { flex: none; margin-top: 0; padding: 8rpx 0 8rpx 16rpx; color: #e34d59; font-size: 22rpx; }
  .empty { padding: 80rpx 0; color: #77859a; text-align: center; }
</style>
