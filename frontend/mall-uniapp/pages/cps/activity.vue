<template>
  <s-layout title="返利活动" navbar="inner">
    <view class="page">
      <view class="hero">
        <text class="hero__title">限时返利活动</text>
        <text class="hero__desc">活动规则与返利金额以平台最终结算为准</text>
      </view>

      <view v-if="state.loading" class="skeleton-list">
        <view v-for="item in 3" :key="item" class="skeleton-card" />
      </view>
      <view v-else-if="state.error" class="state-card">
        <text>{{ state.error }}</text>
        <button class="outline-button" @tap="loadActivities">重新加载</button>
      </view>
      <s-empty
        v-else-if="!state.list.length"
        text="暂无进行中的返利活动"
        icon="/static/goods-empty.png"
      />
      <view v-else class="activity-list">
        <view v-for="item in state.list" :key="item.id" class="activity-card">
          <image
            class="activity-card__image"
            :src="item.mainPic || emptyImage"
            mode="aspectFill"
            @error="item.mainPic = emptyImage"
          />
          <view class="activity-card__body">
            <view class="title-row">
              <text class="activity-card__title">{{ item.activityName }}</text>
              <text v-if="item.tagText" class="tag">{{ item.tagText }}</text>
            </view>
            <text v-if="item.shortDesc" class="desc">{{ item.shortDesc }}</text>
            <text v-if="item.rebateDesc" class="rebate">{{ item.rebateDesc }}</text>
            <view class="meta-row">
              <text>{{ platformName(item.platformCode) }}</text>
              <text v-if="item.endTime">截至 {{ formatDate(item.endTime) }}</text>
            </view>
            <view class="actions">
              <button class="outline-button" @tap="viewActivity(item)">查看活动</button>
              <button
                class="primary-button"
                :disabled="state.promotingId === item.id"
                @tap="promote(item)"
              >
                {{ state.promotingId === item.id ? '生成中...' : '获取活动入口' }}
              </button>
            </view>
          </view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  import CpsMarketingApi from '@/sheep/api/cps/marketing';

  const emptyImage = '/static/goods-empty.png';
  const state = reactive({ loading: false, error: '', list: [], promotingId: null });

  const platformNames = {
    TAOBAO: '淘宝',
    TMALL: '天猫',
    JD: '京东',
    PDD: '拼多多',
    MEITUAN: '美团',
  };
  const platformName = (code) => platformNames[String(code || '').toUpperCase()] || '精选活动';
  const formatDate = (value) => (value ? String(value).replace('T', ' ').slice(0, 10) : '');
  const unwrap = (res, fallback) => {
    if (!res || res.code !== 0) throw new Error(res?.msg || fallback);
    return res.data;
  };

  async function loadActivities() {
    state.loading = true;
    state.error = '';
    try {
      const res = await CpsMarketingApi.getActivityCenter();
      const data = unwrap(res, '活动加载失败');
      state.list = Array.isArray(data) ? data : [];
    } catch (error) {
      state.error = error?.msg || error?.message || '活动加载失败，请稍后重试';
    } finally {
      state.loading = false;
    }
  }

  function viewActivity(item) {
    if (item.searchKeyword) {
      sheep.$router.go('/pages/cps/goods', { keyword: encodeURIComponent(item.searchKeyword) });
      return;
    }
    if (item.jumpUrl) {
      openOrCopy(item.jumpUrl, `已复制${platformName(item.platformCode)}活动链接`);
      return;
    }
    promote(item);
  }

  async function promote(item) {
    if (state.promotingId) return;
    if (!sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    state.promotingId = item.id;
    try {
      const res = await CpsMarketingApi.generateActivityPromotion({ activityId: item.id });
      const promotion = unwrap(res, '活动入口生成失败') || {};
      if (
        promotion.linkStatus &&
        !['SUCCESS', 'INTERNAL_FALLBACK'].includes(promotion.linkStatus)
      ) {
        throw new Error(promotion.linkMessage || '活动入口暂不可用');
      }
      const value = promotion.tpwd || promotion.promotionUrl || promotion.promotionContent;
      if (!value) throw new Error('活动入口生成失败，请稍后重试');
      openOrCopy(value, promotion.tpwd ? '口令已复制，请打开对应平台' : '活动链接已复制');
    } catch (error) {
      sheep.$helper.toast(error?.msg || error?.message || '活动入口生成失败');
    } finally {
      state.promotingId = null;
    }
  }

  function openOrCopy(value, message) {
    // #ifdef H5
    if (/^https?:\/\//i.test(value)) {
      window.location.assign(value);
      return;
    }
    // #endif
    uni.setClipboardData({
      data: value,
      success: () => sheep.$helper.toast(message),
      fail: () => sheep.$helper.toast('复制失败，请重试'),
    });
  }

  onLoad(loadActivities);
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    padding: 24rpx;
    background: #f6f7fb;
    box-sizing: border-box;
  }
  .hero {
    padding: 34rpx;
    border-radius: 24rpx;
    color: #fff;
    background: linear-gradient(135deg, #ff6b35, #ff3d63);
  }
  .hero__title,
  .hero__desc {
    display: block;
  }
  .hero__title {
    font-size: 38rpx;
    font-weight: 700;
  }
  .hero__desc {
    margin-top: 12rpx;
    font-size: 24rpx;
    opacity: 0.88;
  }
  .activity-list,
  .skeleton-list {
    margin-top: 24rpx;
  }
  .activity-card {
    overflow: hidden;
    margin-bottom: 24rpx;
    border-radius: 20rpx;
    background: #fff;
    box-shadow: 0 8rpx 30rpx rgba(38, 40, 64, 0.06);
  }
  .activity-card__image {
    width: 100%;
    height: 280rpx;
    background: #f0f1f5;
  }
  .activity-card__body {
    padding: 24rpx;
  }
  .title-row,
  .meta-row,
  .actions {
    display: flex;
    align-items: center;
  }
  .title-row {
    gap: 12rpx;
  }
  .activity-card__title {
    flex: 1;
    font-size: 30rpx;
    font-weight: 600;
    color: #20222a;
  }
  .tag {
    padding: 5rpx 12rpx;
    border-radius: 999rpx;
    color: #ff4d4f;
    background: #fff1f0;
    font-size: 20rpx;
  }
  .desc,
  .rebate {
    display: block;
    margin-top: 12rpx;
    font-size: 24rpx;
    line-height: 1.6;
  }
  .desc {
    color: #7a7d87;
  }
  .rebate {
    color: #ff4d4f;
    font-weight: 600;
  }
  .meta-row {
    justify-content: space-between;
    margin-top: 18rpx;
    color: #999ca5;
    font-size: 22rpx;
  }
  .actions {
    gap: 16rpx;
    margin-top: 22rpx;
  }
  .outline-button,
  .primary-button {
    flex: 1;
    height: 72rpx;
    line-height: 72rpx;
    border-radius: 36rpx;
    font-size: 26rpx;
  }
  .outline-button {
    color: #ff4d4f;
    background: #fff;
    border: 1rpx solid #ffb0a8;
  }
  .primary-button {
    color: #fff;
    background: linear-gradient(90deg, #ff6b35, #ff3d63);
  }
  .primary-button[disabled] {
    opacity: 0.6;
  }
  .state-card {
    margin-top: 100rpx;
    color: #8d9099;
    text-align: center;
  }
  .state-card .outline-button {
    width: 240rpx;
    margin-top: 28rpx;
  }
  .skeleton-card {
    height: 430rpx;
    margin-bottom: 24rpx;
    border-radius: 20rpx;
    background: linear-gradient(90deg, #eee 25%, #f7f7f7 37%, #eee 63%);
    background-size: 400% 100%;
    animation: shimmer 1.4s infinite;
  }
  @keyframes shimmer {
    0% {
      background-position: 100% 0;
    }
    100% {
      background-position: 0 0;
    }
  }
</style>
