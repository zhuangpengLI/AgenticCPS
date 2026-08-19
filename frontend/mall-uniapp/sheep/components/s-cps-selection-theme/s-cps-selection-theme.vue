<template>
  <view class="selection-theme" :style="panelStyle">
    <view class="section-head">
      <text class="section-title">{{ content.title }}</text>
      <text v-if="content.showMore" class="more" @tap="goMore">更多 ›</text>
    </view>
    <view v-if="state.loading" class="state loading">精选商品加载中...</view>
    <view v-else-if="state.error" class="state error">
      <text>{{ state.error }}</text>
      <text class="retry" @tap="loadItems">重新加载</text>
    </view>
    <view v-else-if="!state.items.length" class="state empty">当前主题暂无精选商品</view>
    <view v-else class="goods-grid">
      <view
        v-for="item in state.items"
        :key="`${item.platformCode}-${item.goodsId || item.goodsSign}`"
        class="goods-card"
        :style="itemStyle"
        @tap="goDetail(item)"
      >
        <image
          class="goods-image"
          :src="item.imageFailed ? emptyImage : item.mainPic || emptyImage"
          mode="aspectFill"
          @error="item.imageFailed = true"
        />
        <view class="goods-body">
          <text class="goods-title">{{ item.title || item.goodsName || '精选商品' }}</text>
          <view class="price-row">
            <text class="price">¥{{ money(item.actualPrice ?? item.price) }}</text>
            <text v-if="Number(item.couponPrice) > 0" class="coupon">
              券 {{ money(item.couponPrice) }}
            </text>
          </view>
          <text class="rebate">实时返利进入详情查看</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { computed, onMounted, reactive, watch } from 'vue';
  import sheep from '@/sheep';
  import CpsMarketingApi from '@/sheep/api/cps/marketing';

  const props = defineProps({
    data: { type: Object, default: () => ({}) },
    styles: { type: Object, default: () => ({}) },
  });
  const emptyImage = '/static/goods-empty.png';
  const state = reactive({ loading: false, error: '', items: [] });
  let loadVersion = 0;

  const themeId = computed(() => {
    const id = Number(props.data.themeId);
    return Number.isSafeInteger(id) && id > 0 ? id : null;
  });
  const content = computed(() => ({
    title: props.data.title || '主题好价',
    limit: Math.min(20, Math.max(2, Number(props.data.limit) || 6)),
    columns: Math.min(3, Math.max(1, Number(props.data.columns) || 2)),
    showMore: props.data.showMore !== false,
  }));
  const itemStyle = computed(() => ({ width: `calc(${100 / content.value.columns}% - 12rpx)` }));
  const panelStyle = computed(() => ({
    borderRadius: `${Number(props.styles.borderRadius || 0)}px`,
  }));

  async function loadItems() {
    const version = ++loadVersion;
    const id = themeId.value;
    state.error = '';
    state.items = [];
    if (!id) {
      state.loading = false;
      return;
    }
    state.loading = true;
    try {
      const res = await CpsMarketingApi.getSelectionThemeItems(id);
      if (!res || res.code !== 0) throw new Error(res?.msg || '精选商品加载失败');
      if (version !== loadVersion) return;
      state.items = (Array.isArray(res.data) ? res.data : []).slice(0, content.value.limit);
    } catch (error) {
      if (version !== loadVersion) return;
      state.error = error?.msg || error?.message || '精选商品加载失败，请稍后重试';
    } finally {
      if (version === loadVersion) state.loading = false;
    }
  }

  function money(value) {
    const amount = Number(value || 0);
    return Number.isFinite(amount) ? amount.toFixed(2) : '0.00';
  }
  function goDetail(item) {
    sheep.$router.go('/pages/cps/goods-detail', {
      platformCode: item.platformCode || '',
      goodsId: item.goodsId || '',
      goodsSign: item.goodsSign || '',
    });
  }
  function goMore() {
    sheep.$router.go('/pages/cps/deals');
  }

  onMounted(loadItems);
  watch(themeId, loadItems);
</script>

<style lang="scss" scoped>
  .selection-theme {
    box-sizing: border-box;
    overflow: hidden;
  }
  .section-head,
  .price-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .section-title {
    color: #24262d;
    font-size: 30rpx;
    font-weight: 650;
  }
  .more {
    padding: 8rpx 0 8rpx 20rpx;
    color: #8b8e97;
    font-size: 23rpx;
  }
  .state {
    display: flex;
    min-height: 170rpx;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    color: #999ca5;
    font-size: 24rpx;
    text-align: center;
  }
  .retry {
    margin-top: 14rpx;
    color: #e6533c;
    font-weight: 600;
  }
  .goods-grid {
    display: flex;
    gap: 18rpx;
    margin-top: 20rpx;
    flex-wrap: wrap;
  }
  .goods-card {
    min-width: 0;
    overflow: hidden;
    border-radius: 14rpx;
    background: #fff;
    box-shadow: 0 6rpx 20rpx rgba(30, 34, 45, 0.07);
    box-sizing: border-box;
  }
  .goods-image {
    width: 100%;
    height: 240rpx;
    background: #f0f1f3;
  }
  .goods-body {
    padding: 16rpx;
  }
  .goods-title {
    display: -webkit-box;
    min-height: 68rpx;
    overflow: hidden;
    color: #2b2d34;
    font-size: 24rpx;
    line-height: 1.45;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }
  .price-row {
    gap: 8rpx;
    margin-top: 12rpx;
  }
  .price {
    color: #e6533c;
    font-size: 28rpx;
    font-weight: 700;
  }
  .coupon {
    padding: 3rpx 7rpx;
    border: 1rpx solid #f1a08e;
    border-radius: 5rpx;
    color: #e6533c;
    font-size: 18rpx;
  }
  .rebate {
    display: block;
    margin-top: 8rpx;
    overflow: hidden;
    color: #7b7e87;
    font-size: 19rpx;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>
