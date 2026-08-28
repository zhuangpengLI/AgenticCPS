<template>
  <view class="cps-rebate-workbench">
    <view class="workbench-hero">
      <view class="hero-kicker">{{ content.kicker }}</view>
      <view class="hero-title">{{ content.title }}</view>
      <view class="hero-description">{{ content.description }}</view>
    </view>

    <view class="rebate-panel asset-panel">
      <view class="section-heading">
        <view>
          <view class="section-title">返利资产</view>
          <view class="section-subtitle">订单、钱包、提现和 Token 统一管理</view>
        </view>
        <text class="section-more" @tap="goAsset('/pages/cps/wallet')">明细 ›</text>
      </view>
      <view class="quick-grid">
        <view
          v-for="item in assetItems"
          :key="item.path"
          class="quick-item"
          @tap="goAsset(item.path)"
        >
          <text class="quick-icon">{{ item.icon }}</text>
          <text>{{ item.title }}</text>
        </view>
      </view>
    </view>

    <view class="rebate-panel tools-panel">
      <view class="section-heading">
        <view>
          <view class="section-title">返利工具</view>
          <view class="section-subtitle">需要时再使用，不和首页导购入口重复</view>
        </view>
      </view>
      <view class="tool-list">
        <view class="tool-item" @tap="goAsset('/pages/cps/transfer')">
          <text class="tool-icon">链</text>
          <view class="tool-info">
            <text class="tool-title">返利转链</text>
            <text class="tool-description">粘贴链接，一键生成专属返利链接</text>
          </view>
          <text class="tool-arrow">›</text>
        </view>
        <view class="tool-item ai-tool" @tap="goAsset('/pages/cps/ai-chat')">
          <text class="tool-icon">AI</text>
          <view class="tool-info">
            <text class="tool-title">返利 AI 对话</text>
            <text class="tool-description">问价格、优惠和返利，智能帮你选</text>
          </view>
          <text class="tool-arrow">›</text>
        </view>
        <view class="tool-item aiot-tool" @tap="goTool('/pages/cps/scene')">
          <text class="tool-icon">智</text>
          <view class="tool-info">
            <text class="tool-title">AIoT 场景推荐</text>
            <text class="tool-description">根据生活场景智能推荐返利好物</text>
          </view>
          <text class="tool-arrow">›</text>
        </view>
      </view>
    </view>

    <view class="rebate-panel featured-panel">
      <view class="section-heading">
        <view>
          <view class="section-title">为你精选</view>
          <view class="section-subtitle">券后价和预估返利一眼看清</view>
        </view>
        <text class="section-more" @tap="goTool('/pages/cps/goods')">更多 ›</text>
      </view>
      <view
        v-for="item in featuredItems"
        :key="item.keyword"
        class="featured-item"
        @tap="goTool(`/pages/cps/goods?keyword=${encodeURIComponent(item.keyword)}`)"
      >
        <image class="featured-thumb" :src="sheep.$url.static(item.image)" mode="aspectFit" />
        <view class="featured-info">
          <text class="featured-name">{{ item.name }}</text>
          <text class="featured-meta">券后 {{ item.price }} · 预计返利 {{ item.rebate }}</text>
        </view>
        <text class="featured-action">去查券</text>
      </view>
    </view>

    <view class="rebate-panel guide-panel">
      <view class="section-title">返利小贴士</view>
      <view class="notice"
        >首页负责找优惠，返利工作台负责管资产。返利金额以订单最终结算为准，退款或失效订单不产生返利。</view
      >
    </view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';

  const props = defineProps({
    data: {
      type: Object,
      default: () => ({}),
    },
    styles: {
      type: Object,
      default: () => ({}),
    },
  });

  const content = computed(() => ({
    kicker: props.data.kicker || 'AgenticCPS · 返利工作台',
    title: props.data.title || '管理每一笔返利',
    description: props.data.description || '订单、钱包、提现和 Token 集中处理',
  }));

  const assetItems = [
    { icon: '单', title: '返利订单', path: '/pages/cps/order' },
    { icon: '返', title: '返利钱包', path: '/pages/cps/wallet' },
    { icon: '提', title: '申请提现', path: '/pages/cps/withdraw' },
    { icon: 'T', title: '兑换 Token', path: '/pages/cps/exchange' },
  ];
  const featuredItems = [
    {
      image: '/static/img/diy_3c/caidandaohang/bijiben.png',
      name: '轻薄笔记本电脑 14 英寸',
      price: '¥3999',
      rebate: '¥86.50',
      keyword: '轻薄笔记本电脑',
    },
    {
      image: '/static/img/diy_3c/caidandaohang/xiangji.png',
      name: '高清微单数码相机',
      price: '¥2699',
      rebate: '¥56.20',
      keyword: '微单相机',
    },
    {
      image: '/static/img/diy_3c/caidandaohang/erji.png',
      name: '真无线蓝牙耳机 Pro',
      price: '¥199',
      rebate: '¥18.60',
      keyword: '蓝牙耳机',
    },
  ];
  function goAsset(path) {
    if (!sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    sheep.$router.go(path);
  }
  function goTool(path) {
    sheep.$router.go(path);
  }
</script>

<style lang="scss" scoped>
  .cps-rebate-workbench {
    box-sizing: border-box;
    padding: 18rpx 20rpx 24rpx;
    background: #f7f7f8;
    color: #222;
  }

  .workbench-hero {
    padding: 34rpx 24rpx 70rpx;
    border-radius: 28rpx;
    color: #fff;
    background: linear-gradient(135deg, #ff7a45 0%, #f04438 100%);
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
  .hero-description {
    margin-top: 14rpx;
    font-size: 24rpx;
    opacity: 0.9;
  }

  .rebate-panel {
    margin-top: 20rpx;
    padding: 28rpx;
    border-radius: 22rpx;
    background: #fff;
    box-shadow: 0 8rpx 28rpx rgba(0, 0, 0, 0.06);
  }
  .asset-panel {
    margin-top: -38rpx;
  }
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
  }
  .section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16rpx;
  }
  .section-subtitle {
    margin-top: 6rpx;
    color: #999;
    font-size: 21rpx;
  }
  .quick-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    margin-top: 26rpx;
  }
  .quick-item {
    display: flex;
    min-width: 0;
    flex-direction: column;
    align-items: center;
    gap: 12rpx;
    color: #333;
    font-size: 24rpx;
  }
  .quick-icon {
    display: flex;
    width: 72rpx;
    height: 72rpx;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: #f4513b;
    background: #fff1ed;
    font-weight: 600;
  }
  .quick-icon-aiot {
    color: #1677ff;
    background: #eaf3ff;
  }
  .tool-list {
    display: grid;
    gap: 16rpx;
    margin-top: 22rpx;
  }
  .tool-item {
    display: flex;
    align-items: center;
    gap: 16rpx;
    padding: 18rpx;
    border-radius: 16rpx;
    background: #fff5f1;
  }
  .tool-item.ai-tool {
    background: #f2f5ff;
  }
  .tool-item.aiot-tool {
    background: #eef8f4;
  }
  .featured-panel {
    margin-top: 20rpx;
  }
  .featured-item {
    display: flex;
    align-items: center;
    gap: 18rpx;
    padding-top: 22rpx;
  }
  .featured-thumb {
    width: 96rpx;
    height: 96rpx;
    flex: 0 0 96rpx;
    border-radius: 16rpx;
    background: #fff4ee;
  }
  .featured-info {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 9rpx;
  }
  .featured-name {
    overflow: hidden;
    color: #333;
    font-size: 26rpx;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .featured-meta {
    color: #999;
    font-size: 21rpx;
  }
  .featured-action {
    flex: 0 0 auto;
    color: #f4513b;
    font-size: 23rpx;
  }
  .tool-icon {
    display: flex;
    width: 72rpx;
    height: 72rpx;
    flex: 0 0 72rpx;
    align-items: center;
    justify-content: center;
    border-radius: 20rpx;
    color: #f4513b;
    background: #ffe4dc;
    font-size: 24rpx;
    font-weight: 700;
  }
  .ai-tool .tool-icon {
    color: #4d63c8;
    background: #e0e7ff;
  }
  .aiot-tool .tool-icon {
    color: #16865c;
    background: #dff5e9;
  }
  .tool-info {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 8rpx;
  }
  .tool-title {
    color: #222;
    font-size: 27rpx;
    font-weight: 600;
  }
  .tool-description {
    overflow: hidden;
    color: #888;
    font-size: 22rpx;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .tool-arrow {
    color: #999;
    font-size: 38rpx;
  }
  .section-more {
    color: #999;
    font-size: 24rpx;
    font-weight: 400;
  }
  .notice {
    margin-top: 24rpx;
    padding: 18rpx 20rpx;
    border-radius: 12rpx;
    color: #9a6d1d;
    background: #fff8e6;
    font-size: 22rpx;
    line-height: 1.6;
  }
</style>
