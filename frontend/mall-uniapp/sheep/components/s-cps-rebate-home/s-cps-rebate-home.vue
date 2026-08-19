<template>
  <view class="cps-rebate-home">
    <view class="rebate-hero">
      <view class="hero-kicker">{{ content.kicker }}</view>
      <view class="hero-title">{{ content.title }}</view>
      <view class="hero-description">{{ content.description }}</view>
      <view class="search-box">
        <text class="search-icon _icon-search"></text>
        <input
          v-model="keyword"
          class="search-input"
          :placeholder="content.searchPlaceholder"
          confirm-type="search"
          @confirm="startSearch()"
        />
        <button class="ss-reset-button search-button" @tap="startSearch()">
          {{ content.searchButtonText }}
        </button>
      </view>
    </view>

    <view class="rebate-panel asset-panel">
      <view class="section-title">我的返利</view>
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

    <view class="rebate-panel">
      <view class="section-title">发现好物</view>
      <view class="quick-grid">
        <view
          v-for="item in discoveryItems"
          :key="item.path"
          class="quick-item"
          @tap="goPage(item.path)"
        >
          <text class="quick-icon" :class="{ 'quick-icon-aiot': item.icon === '智' }">
            {{ item.icon }}
          </text>
          <text>{{ item.title }}</text>
        </view>
      </view>
    </view>

    <view v-if="content.showFeatured" class="rebate-panel featured-panel">
      <view class="section-title featured-title" @tap="goPage('/pages/cps/goods')">
        <text>为你精选</text>
        <text class="section-more">更多 ›</text>
      </view>
      <view
        v-for="item in featuredItems"
        :key="item.keyword"
        class="featured-item"
        @tap="startSearch(item.keyword)"
      >
        <image class="featured-thumb" :src="sheep.$url.static(item.image)" mode="aspectFit" />
        <view class="featured-info">
          <text class="featured-name">{{ item.name }}</text>
          <text class="featured-meta">券后 {{ item.price }} · 预计返利 {{ item.rebate }}</text>
        </view>
        <text class="featured-action">去查券</text>
      </view>
    </view>

    <view class="rebate-panel tools-panel">
      <view class="section-title">返利工具</view>
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
      </view>
    </view>

    <view class="rebate-panel guide-panel">
      <view class="section-title">如何获得返利</view>
      <view v-for="(step, index) in steps" :key="step" class="step">
        <text class="step-no">{{ index + 1 }}</text>
        <text>{{ step }}</text>
      </view>
      <view class="notice">返利金额以订单最终结算为准，退款或失效订单不产生返利。</view>
    </view>
  </view>
</template>

<script setup>
  import { computed, ref } from 'vue';
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

  const keyword = ref('');
  const content = computed(() => ({
    kicker: props.data.kicker || 'AgenticCPS · 今日高返',
    title: props.data.title || '最高 ¥186.50',
    description: props.data.description || '领券下单，订单结算后返利到账',
    searchPlaceholder: props.data.searchPlaceholder || '搜商品或粘贴链接/口令',
    searchButtonText: props.data.searchButtonText || '查优惠',
    showFeatured: props.data.showFeatured !== false,
  }));

  const assetItems = [
    { icon: '单', title: '返利订单', path: '/pages/cps/order' },
    { icon: '返', title: '返利钱包', path: '/pages/cps/wallet' },
    { icon: '提', title: '申请提现', path: '/pages/cps/withdraw' },
    { icon: 'T', title: '兑换 Token', path: '/pages/cps/exchange' },
  ];
  const discoveryItems = [
    { icon: '券', title: '查券返利', path: '/pages/cps/goods' },
    { icon: '价', title: '主题好价', path: '/pages/cps/deals' },
    { icon: '活', title: '返利活动', path: '/pages/cps/activity' },
    { icon: '智', title: 'AIoT 推荐', path: '/pages/cps/scene' },
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
  const steps = ['搜索商品或粘贴商品链接', '查看券后价和预估返利', '领券购买，订单结算后返利到账'];

  function startSearch(presetKeyword = '') {
    const contentValue = (presetKeyword || keyword.value).trim();
    if (!contentValue) {
      sheep.$router.go('/pages/cps/goods');
      return;
    }
    const looksLikeContent = /https?:\/\/|\uffe5|\u20ac|tb\.cn|m\.tb|yangkeduo|jd\.com/i.test(
      contentValue,
    );
    sheep.$router.go('/pages/cps/goods', {
      [looksLikeContent ? 'content' : 'keyword']: encodeURIComponent(contentValue),
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
  .cps-rebate-home {
    box-sizing: border-box;
    padding: 18rpx 20rpx 24rpx;
    background: #f7f7f8;
    color: #222;
  }

  .rebate-hero {
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

  .search-box {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 34rpx;
    padding: 10rpx 12rpx 10rpx 24rpx;
    border-radius: 44rpx;
    background: #fff;
  }

  .search-icon {
    flex: 0 0 auto;
    color: #aaa;
    font-size: 28rpx;
  }
  .search-input {
    min-width: 0;
    height: 68rpx;
    flex: 1;
    color: #333;
    font-size: 25rpx;
  }
  .search-button {
    flex: 0 0 136rpx;
    height: 68rpx;
    line-height: 68rpx;
    border-radius: 34rpx;
    color: #fff;
    background: #f4513b;
    font-size: 25rpx;
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

  .featured-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
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
  .featured-item {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding-top: 24rpx;
  }
  .featured-thumb {
    display: flex;
    width: 92rpx;
    height: 92rpx;
    flex: 0 0 92rpx;
    align-items: center;
    justify-content: center;
    border-radius: 16rpx;
    color: #e95235;
    background: #fff1ed;
    font-size: 34rpx;
  }
  .featured-info {
    display: flex;
    min-width: 0;
    flex: 1;
    flex-direction: column;
    gap: 10rpx;
  }
  .featured-name {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: #333;
    font-size: 27rpx;
  }
  .featured-meta {
    color: #999;
    font-size: 22rpx;
  }
  .featured-action {
    flex: 0 0 auto;
    color: #f4513b;
    font-size: 24rpx;
  }

  .step {
    display: flex;
    align-items: center;
    gap: 18rpx;
    margin-top: 20rpx;
    color: #555;
    font-size: 25rpx;
  }
  .step-no {
    display: flex;
    width: 38rpx;
    height: 38rpx;
    flex: 0 0 38rpx;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: #fff;
    background: #f4513b;
    font-size: 22rpx;
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
