<template>
  <s-layout title="主题好价" navbar="inner">
    <view class="page">
      <scroll-view v-if="state.themes.length" class="theme-tabs" scroll-x>
        <view class="theme-tabs__inner">
          <view
            v-for="theme in state.themes"
            :key="theme.id"
            class="theme-tab"
            :class="{ active: state.themeId === theme.id }"
            @tap="selectTheme(theme)"
          >
            {{ theme.themeName }}
          </view>
        </view>
      </scroll-view>

      <view v-if="activeTheme" class="theme-banner">
        <image
          v-if="activeTheme.coverPic"
          class="theme-banner__image"
          :src="activeTheme.coverPic"
          mode="aspectFill"
        />
        <view class="theme-banner__mask">
          <text class="theme-banner__title">{{ activeTheme.themeName }}</text>
          <text class="theme-banner__desc">{{
            activeTheme.aiSummary || activeTheme.description || '运营精选返利好物'
          }}</text>
        </view>
      </view>

      <view v-if="state.loading" class="loading">正在加载精选好价...</view>
      <view v-else-if="state.error" class="state-card">
        <text>{{ state.error }}</text>
        <button class="retry" @tap="reload">重新加载</button>
      </view>
      <s-empty
        v-else-if="!state.items.length"
        text="当前主题暂无精选商品"
        icon="/static/goods-empty.png"
      />
      <view v-else class="goods-grid">
        <view
          v-for="item in state.items"
          :key="`${item.platformCode}-${item.goodsId}`"
          class="goods-card"
          @tap="goDetail(item)"
        >
          <image
            class="goods-card__image"
            :src="item.mainPic || emptyImage"
            mode="aspectFill"
            @error="item.mainPic = emptyImage"
          />
          <view class="goods-card__body">
            <text class="goods-card__title">{{ item.title }}</text>
            <text v-if="item.recommendReason || item.sellingPoint" class="reason">{{
              item.recommendReason || item.sellingPoint
            }}</text>
            <view class="coupon-row">
              <text v-if="Number(item.couponPrice) > 0" class="coupon"
                >券 ¥{{ money(item.couponPrice) }}</text
              >
              <text v-if="item.monthSales" class="sales">月销 {{ item.monthSales }}</text>
            </view>
            <view class="price-row">
              <view
                ><text class="yen">¥</text
                ><text class="price">{{ money(item.actualPrice) }}</text></view
              >
              <text class="rebate">实时返利进入详情查看</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import CpsMarketingApi from '@/sheep/api/cps/marketing';

  const emptyImage = '/static/goods-empty.png';
  const state = reactive({ themes: [], themeId: null, items: [], loading: false, error: '' });
  const activeTheme = computed(() => state.themes.find((item) => item.id === state.themeId));
  const money = (value) => Number(value || 0).toFixed(2);
  const unwrap = (res, fallback) => {
    if (!res || res.code !== 0) throw new Error(res?.msg || fallback);
    return res.data;
  };

  async function loadThemes() {
    state.loading = true;
    state.error = '';
    try {
      const res = await CpsMarketingApi.getSelectionThemes();
      const data = unwrap(res, '主题加载失败');
      state.themes = Array.isArray(data) ? data : [];
      if (state.themes.length) await selectTheme(state.themes[0]);
    } catch (error) {
      state.error = error?.msg || error?.message || '主题加载失败，请稍后重试';
    } finally {
      state.loading = false;
    }
  }

  async function selectTheme(theme) {
    if (!theme?.id || (state.themeId === theme.id && state.items.length)) return;
    state.themeId = theme.id;
    state.items = [];
    state.loading = true;
    state.error = '';
    try {
      const res = await CpsMarketingApi.getSelectionThemeItems(theme.id);
      const data = unwrap(res, '好价商品加载失败');
      state.items = Array.isArray(data) ? data : [];
    } catch (error) {
      state.error = error?.msg || error?.message || '好价商品加载失败';
    } finally {
      state.loading = false;
    }
  }

  function reload() {
    if (state.themeId) {
      const theme = activeTheme.value;
      state.themeId = null;
      selectTheme(theme);
    } else loadThemes();
  }

  function goDetail(item) {
    sheep.$router.go('/pages/cps/goods-detail', {
      platformCode: item.platformCode,
      goodsId: item.goodsId,
      goodsSign: item.goodsSign || '',
    });
  }

  onLoad(loadThemes);
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    padding-bottom: 30rpx;
    background: #f6f7fb;
  }
  .theme-tabs {
    width: 100%;
    background: #fff;
    white-space: nowrap;
  }
  .theme-tabs__inner {
    display: flex;
    gap: 16rpx;
    padding: 20rpx 24rpx;
  }
  .theme-tab {
    flex: none;
    padding: 14rpx 24rpx;
    border-radius: 999rpx;
    color: #646772;
    background: #f3f4f7;
    font-size: 25rpx;
  }
  .theme-tab.active {
    color: #fff;
    background: linear-gradient(90deg, #ff6b35, #ff3d63);
  }
  .theme-banner {
    position: relative;
    height: 230rpx;
    margin: 24rpx;
    overflow: hidden;
    border-radius: 24rpx;
    background: linear-gradient(135deg, #ff9a62, #ff4f68);
  }
  .theme-banner__image {
    width: 100%;
    height: 100%;
  }
  .theme-banner__mask {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    padding: 28rpx;
    color: #fff;
    background: linear-gradient(transparent, rgba(20, 16, 18, 0.72));
  }
  .theme-banner__title,
  .theme-banner__desc {
    display: block;
  }
  .theme-banner__title {
    font-size: 34rpx;
    font-weight: 700;
  }
  .theme-banner__desc {
    margin-top: 10rpx;
    font-size: 23rpx;
  }
  .goods-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18rpx;
    padding: 0 24rpx;
  }
  .goods-card {
    min-width: 0;
    overflow: hidden;
    border-radius: 18rpx;
    background: #fff;
  }
  .goods-card__image {
    width: 100%;
    height: 330rpx;
    background: #eee;
  }
  .goods-card__body {
    padding: 18rpx;
  }
  .goods-card__title {
    display: -webkit-box;
    height: 76rpx;
    overflow: hidden;
    color: #252731;
    font-size: 27rpx;
    line-height: 38rpx;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  .reason {
    display: block;
    margin-top: 8rpx;
    overflow: hidden;
    color: #8a6a43;
    font-size: 21rpx;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
  .coupon-row,
  .price-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 14rpx;
  }
  .coupon {
    padding: 4rpx 8rpx;
    border-radius: 5rpx;
    color: #ff4d4f;
    background: #fff1f0;
    font-size: 20rpx;
  }
  .sales {
    color: #a0a2aa;
    font-size: 20rpx;
  }
  .yen,
  .price,
  .rebate {
    color: #ff3d52;
  }
  .yen {
    font-size: 21rpx;
  }
  .price {
    font-size: 34rpx;
    font-weight: 700;
  }
  .rebate {
    font-size: 20rpx;
  }
  .loading,
  .state-card {
    padding: 120rpx 24rpx;
    color: #9598a2;
    text-align: center;
  }
  .retry {
    width: 240rpx;
    margin-top: 24rpx;
    border: 1rpx solid #ff8a79;
    border-radius: 36rpx;
    color: #ff4d4f;
    background: #fff;
    font-size: 25rpx;
  }
</style>
