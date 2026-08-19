<template>
  <s-layout title="返利转链" navbar="inner">
    <view class="page-content">
      <s-cps-rebate-transfer :data="componentData" />
      <view class="tip-card">
        <text class="tip-title">返利转链说明</text>
        <text class="tip-text"
          >粘贴淘宝、京东、拼多多或抖音商品链接/口令，生成专属推广链接后再分享或购买。</text
        >
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  const componentData = {
    title: '一键转链',
    description: '粘贴商品链接或口令，生成专属返利链接',
    placeholder: '粘贴商品链接、商品 ID 或口令',
    buttonText: '立即转链',
  };
</script>

<style lang="scss" scoped>
  .page-content {
    padding: 20rpx;
    background: #f7f7f8;
  }
  .tip-card {
    display: flex;
    margin-top: 20rpx;
    padding: 24rpx;
    flex-direction: column;
    gap: 12rpx;
    border-radius: 16rpx;
    background: #fff;
  }
  .tip-title {
    color: #333;
    font-size: 27rpx;
    font-weight: 600;
  }
  .tip-text {
    color: #888;
    font-size: 23rpx;
    line-height: 1.6;
  }
</style>
