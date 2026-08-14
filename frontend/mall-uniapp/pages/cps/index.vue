<template>
  <s-layout title="返利中心" navbar="inner">
    <view class="hero ui-BG-Main-Gradient">
      <view class="hero-kicker">AgenticCPS · 今日高返</view>
      <view class="hero-title">最高 ¥186.50</view>
      <view class="hero-desc">领券下单，订单结算后返利到账</view>
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
        <view class="quick-item" @tap="goAsset('/pages/cps/exchange')">
          <text class="quick-icon">T</text><text>兑换 Token</text>
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
        <view class="quick-item" @tap="goPage('/pages/cps/scene')">
          <text class="quick-icon quick-icon-aiot">智</text><text>AIoT 推荐</text>
        </view>
      </view>
    </view>

    <view class="featured-card">
      <view class="section-title featured-title"
        ><text>为你精选</text><text class="section-more">更多 ›</text></view
      >
      <view
        v-for="item in featuredGoods"
        :key="item.keyword"
        class="featured-item"
        @tap="startSearch(item.keyword)"
      >
        <view class="featured-thumb">{{ item.mark }}</view>
        <view class="featured-info">
          <text class="featured-name">{{ item.name }}</text>
          <text class="featured-meta">券后 {{ item.price }} · 预计返利 {{ item.rebate }}</text>
        </view>
        <text class="featured-action">去查券</text>
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
  const featuredGoods = [
    { mark: '枕', name: '云朵乳胶枕 高弹护颈', price: '¥129', rebate: '¥12.80', keyword: '乳胶枕' },
    { mark: '锅', name: '多功能电煮锅 1.8L', price: '¥89', rebate: '¥8.20', keyword: '电煮锅' },
    {
      mark: '耳',
      name: '真无线蓝牙耳机 Pro',
      price: '¥199',
      rebate: '¥18.60',
      keyword: '蓝牙耳机',
    },
  ];

  function startSearch(keyword) {
    const content = (typeof keyword === 'string' ? keyword : state.input).trim();
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
    margin: 18rpx 20rpx 0;
    padding: 34rpx 24rpx 70rpx;
    border-radius: 28rpx;
    color: #fff;
  }
  .hero-kicker {
    font-size: 23rpx;
    opacity: 0.9;
  }
  .hero-title {
    margin-top: 12rpx;
    font-size: 52rpx;
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
  .guide-card,
  .featured-card {
    margin: -38rpx 20rpx 20rpx;
    padding: 28rpx;
    border-radius: 22rpx;
    background: #fff;
    box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.06);
  }
  .guide-card {
    margin-top: 0;
  }
  .discovery-card {
    margin-top: 0;
  }
  .featured-card {
    margin-top: 0;
  }
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #222;
  }
  .quick-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
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
  .quick-icon-aiot {
    color: #168f73;
    background: #e9f8f2;
  }
  .featured-title {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .section-more {
    color: #999;
    font-size: 22rpx;
    font-weight: 400;
  }
  .featured-item {
    display: flex;
    align-items: center;
    gap: 18rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #f3f3f3;
  }
  .featured-item:last-child {
    padding-bottom: 0;
    border-bottom: 0;
  }
  .featured-thumb {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 92rpx;
    width: 92rpx;
    height: 92rpx;
    border-radius: 16rpx;
    color: #a36a3f;
    background: #fff2e4;
    font-size: 30rpx;
    font-weight: 700;
  }
  .featured-info {
    flex: 1;
    min-width: 0;
  }
  .featured-name,
  .featured-meta {
    display: block;
  }
  .featured-name {
    overflow: hidden;
    color: #222;
    font-size: 27rpx;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .featured-meta {
    margin-top: 10rpx;
    color: var(--ui-BG-Main);
    font-size: 22rpx;
  }
  .featured-action {
    flex: 0 0 auto;
    padding: 10rpx 16rpx;
    border-radius: 24rpx;
    color: #fff;
    background: var(--ui-BG-Main);
    font-size: 21rpx;
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
