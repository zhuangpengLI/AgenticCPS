<template>
  <s-layout title="返利中心" navbar="inner">
    <view class="hero ui-BG-Main-Gradient">
      <view class="hero-title">领券下单，返利省钱</view>
      <view class="hero-desc">支持商品关键词、链接和口令查询</view>
      <view class="search-box">
        <uni-easyinput
          v-model="state.input"
          :inputBorder="false"
          placeholder="搜商品或粘贴链接/口令"
          @confirm="startSearch"
        />
        <button class="ss-reset-button search-button" @tap="startSearch">查优惠</button>
      </view>
    </view>

    <view class="quick-card">
      <view class="section-title">我的返利</view>
      <view class="quick-grid">
        <view class="quick-item" @tap="goAsset('/pages/cps/order')">
          <text class="quick-icon">单</text><text>返利订单</text>
        </view>
        <view class="quick-item" @tap="goAsset('/pages/cps/wallet')">
          <text class="quick-icon">返</text><text>返利钱包</text>
        </view>
        <view class="quick-item" @tap="goAsset('/pages/cps/withdraw')">
          <text class="quick-icon">提</text><text>申请提现</text>
        </view>
      </view>
    </view>

    <view class="quick-card discovery-card">
      <view class="section-title">发现好物</view>
      <view class="quick-grid discovery-grid">
        <view class="quick-item" @tap="goPage('/pages/cps/activity')">
          <text class="quick-icon">活</text><text>返利活动</text>
        </view>
        <view class="quick-item" @tap="goPage('/pages/cps/deals')">
          <text class="quick-icon">价</text><text>主题好价</text>
        </view>
        <view class="quick-item" @tap="goAsset('/pages/cps/history')">
          <text class="quick-icon">迹</text><text>返利足迹</text>
        </view>
        <view class="quick-item" @tap="goAsset('/pages/cps/favorites')">
          <text class="quick-icon">藏</text><text>返利收藏</text>
        </view>
      </view>
    </view>

    <view class="guide-card">
      <view class="section-title">如何获得返利</view>
      <view class="step"><text class="step-no">1</text><text>搜索商品或粘贴商品链接</text></view>
      <view class="step"><text class="step-no">2</text><text>查看券后价和预估返利</text></view>
      <view class="step"
        ><text class="step-no">3</text><text>领券购买，订单结算后返利到账</text></view
      >
      <view class="notice">返利金额以订单最终结算为准，退款或失效订单不产生返利。</view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';

  const state = reactive({ input: '' });

  function startSearch() {
    const content = state.input.trim();
    if (!content) {
      sheep.$helper.toast('请输入商品关键词或粘贴链接');
      return;
    }
    const looksLikeContent = /https?:\/\/|\uffe5|\u20ac|tb\.cn|m\.tb|yangkeduo|jd\.com/i.test(
      content,
    );
    sheep.$router.go('/pages/cps/goods', {
      [looksLikeContent ? 'content' : 'keyword']: encodeURIComponent(content),
    });
  }

  function goAsset(path) {
    if (!sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    sheep.$router.go(path);
  }

  function goPage(path) {
    sheep.$router.go(path);
  }
</script>

<style lang="scss" scoped>
  .hero {
    padding: 46rpx 24rpx 74rpx;
    color: #fff;
  }
  .hero-title {
    font-size: 42rpx;
    font-weight: 700;
  }
  .hero-desc {
    margin-top: 14rpx;
    font-size: 24rpx;
    opacity: 0.9;
  }
  .search-box {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 34rpx;
    padding: 10rpx 12rpx 10rpx 24rpx;
    background: #fff;
    border-radius: 44rpx;
  }
  .search-button {
    flex: 0 0 136rpx;
    height: 68rpx;
    line-height: 68rpx;
    border-radius: 34rpx;
    color: #fff;
    background: var(--ui-BG-Main);
    font-size: 25rpx;
  }
  .quick-card,
  .guide-card {
    margin: -38rpx 24rpx 24rpx;
    padding: 28rpx;
    border-radius: 18rpx;
    background: #fff;
    box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.06);
  }
  .guide-card {
    margin-top: 0;
  }
  .discovery-card {
    margin-top: 0;
  }
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #222;
  }
  .quick-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-top: 26rpx;
  }
  .discovery-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .quick-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12rpx;
    font-size: 24rpx;
    color: #333;
  }
  .quick-icon {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    color: var(--ui-BG-Main);
    background: rgba(var(--ui-BG-Main-rgb), 0.1);
    font-weight: 600;
  }
  .step {
    display: flex;
    align-items: center;
    gap: 18rpx;
    margin-top: 24rpx;
    color: #444;
    font-size: 25rpx;
  }
  .step-no {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 42rpx;
    height: 42rpx;
    border-radius: 50%;
    color: #fff;
    background: var(--ui-BG-Main);
  }
  .notice {
    margin-top: 28rpx;
    padding: 20rpx;
    border-radius: 10rpx;
    background: #fff7f2;
    color: #8a5a3b;
    font-size: 22rpx;
    line-height: 1.6;
  }
</style>
