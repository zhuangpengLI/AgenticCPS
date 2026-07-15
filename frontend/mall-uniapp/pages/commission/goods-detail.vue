<template>
  <s-layout title="商品详情" navbar="inner">
    <view class="detail-page" v-if="state.goods">
      <image
        class="main-image"
        :src="state.goods.mainPic || '/static/goods-empty.png'"
        mode="aspectFill"
      />

      <view class="info-panel">
        <view class="title ss-line-2">{{ state.goods.title || '未知商品' }}</view>
        <view class="meta ss-line-1">
          {{ platformText(state.goods.platformCode) }} ·
          {{ state.goods.shopName || state.goods.brandName || '未知店铺' }}
        </view>
        <view class="price-row">
          <text class="actual-price">¥{{ formatMoney(state.goods.actualPrice) }}</text>
          <text class="origin-price">¥{{ formatMoney(state.goods.originalPrice) }}</text>
          <text class="coupon" v-if="toNumber(state.goods.couponPrice) > 0">
            券 ¥{{ formatMoney(state.goods.couponPrice) }}
          </text>
        </view>
        <view class="selling-point ss-line-2" v-if="state.goods.sellingPoint">
          {{ state.goods.sellingPoint }}
        </view>
      </view>

      <view class="metric-panel">
        <view class="metric-item">
          <text class="label">佣金比例</text>
          <text class="value">{{ formatRate(state.goods.commissionRate) }}</text>
        </view>
        <view class="metric-item">
          <text class="label">预估返利</text>
          <text class="value rebate">¥{{ formatMoney(state.goods.estimateRebateAmount) }}</text>
        </view>
        <view class="metric-item">
          <text class="label">月销量</text>
          <text class="value">{{ state.goods.monthSales || 0 }}</text>
        </view>
      </view>

      <view class="coupon-panel">
        <view class="section-title">优惠券</view>
        <view class="detail-line">
          <text>券金额</text>
          <text class="value">¥{{ formatMoney(state.goods.couponPrice) }}</text>
        </view>
        <view class="detail-line">
          <text>使用门槛</text>
          <text class="value">¥{{ formatMoney(state.goods.couponConditions) }}</text>
        </view>
        <view class="detail-line">
          <text>剩余数量</text>
          <text class="value">{{ state.goods.couponRemainNum ?? '-' }}</text>
        </view>
        <view class="detail-line">
          <text>有效期</text>
          <text class="value">{{ state.goods.couponEndTime || '-' }}</text>
        </view>
      </view>

      <view class="goods-panel">
        <view class="section-title">商品信息</view>
        <view class="detail-line">
          <text>商品 ID</text>
          <text class="value">{{ state.goods.goodsId || '-' }}</text>
        </view>
        <view class="detail-line" v-if="state.goods.goodsSign">
          <text>goodsSign</text>
          <text class="value">{{ state.goods.goodsSign }}</text>
        </view>
        <view class="detail-line">
          <text>来源</text>
          <text class="value">{{ state.goods.source || state.goods.vendorCode || '-' }}</text>
        </view>
        <view class="detail-line">
          <text>类目</text>
          <text class="value">{{ state.goods.categoryName || '-' }}</text>
        </view>
        <view class="detail-line" v-if="state.goods.itemLink">
          <text>原始链接</text>
          <text class="value link-text">{{ state.goods.itemLink }}</text>
        </view>
      </view>

      <view class="bottom-bar">
        <button class="ss-reset-button link-btn ui-BG-Main-Gradient" @tap="generateLink">
          生成推广链接
        </button>
      </view>
    </view>

    <s-empty
      v-if="!state.goods && !state.loading"
      icon="/static/goods-empty.png"
      text="商品详情不可用"
    />

    <su-popup
      :show="state.showLinkPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showLinkPopup = false"
    >
      <view class="link-panel" v-if="state.currentLink">
        <view class="link-title">推广链接</view>
        <view class="link-item" v-if="state.currentLink.shortUrl">
          <text>短链</text>
          <text class="value">{{ state.currentLink.shortUrl }}</text>
        </view>
        <view class="link-item" v-if="state.currentLink.mobileUrl">
          <text>移动链接</text>
          <text class="value">{{ state.currentLink.mobileUrl }}</text>
        </view>
        <view class="link-item" v-if="state.currentLink.longUrl">
          <text>长链</text>
          <text class="value">{{ state.currentLink.longUrl }}</text>
        </view>
        <view class="link-item" v-if="state.currentLink.tpwd">
          <text>淘口令</text>
          <text class="value">{{ state.currentLink.tpwd }}</text>
        </view>
      </view>
    </su-popup>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import CpsGoodsApi from '@/sheep/api/cps/goods';

  const state = reactive({
    loading: false,
    goods: null,
    query: {
      platformCode: '',
      goodsId: '',
      goodsSign: '',
    },
    showLinkPopup: false,
    currentLink: null,
  });

  async function getDetail() {
    if (!state.query.platformCode || (!state.query.goodsId && !state.query.goodsSign)) {
      sheep.$helper.toast('缺少商品参数');
      return;
    }
    state.loading = true;
    const { code, data } = await CpsGoodsApi.getDetail(state.query);
    state.loading = false;
    if (code !== 0) {
      return;
    }
    state.goods = data;
  }

  async function generateLink() {
    if (!state.goods) {
      return;
    }
    const { code, data } = await CpsGoodsApi.generateLink({
      platformCode: state.goods.platformCode,
      goodsId: state.goods.goodsId,
      goodsSign: state.goods.goodsSign,
    });
    if (code !== 0 || !data) {
      return;
    }
    state.currentLink = data;
    state.showLinkPopup = true;
  }

  function platformText(platformCode) {
    const map = {
      taobao: '淘宝',
      jd: '京东',
      pdd: '拼多多',
      douyin: '抖音',
      meituan: '美团',
      eleme: '饿了么',
      didi: '滴滴',
      vip: '唯品会',
    };
    return map[platformCode] || platformCode || '未知平台';
  }

  function formatMoney(value) {
    return toNumber(value).toFixed(2);
  }

  function formatRate(value) {
    return `${toNumber(value).toFixed(2)}%`;
  }

  function toNumber(value) {
    const num = Number(value || 0);
    return Number.isFinite(num) ? num : 0;
  }

  onLoad((options = {}) => {
    state.query.platformCode = decodeURIComponent(options.platformCode || '');
    state.query.goodsId = decodeURIComponent(options.goodsId || '');
    state.query.goodsSign = decodeURIComponent(options.goodsSign || '');
    getDetail();
  });
</script>

<style lang="scss" scoped>
  .detail-page {
    padding-bottom: 132rpx;
  }

  .main-image {
    width: 100%;
    height: 520rpx;
    background: #f5f5f5;
  }

  .info-panel,
  .metric-panel,
  .coupon-panel,
  .goods-panel {
    margin-top: 18rpx;
    padding: 24rpx;
    background: #ffffff;
  }

  .title {
    font-size: 34rpx;
    font-weight: 600;
    color: #222222;
    line-height: 44rpx;
  }

  .meta,
  .selling-point {
    margin-top: 14rpx;
    font-size: 24rpx;
    color: #777777;
  }

  .selling-point {
    color: #555555;
    line-height: 34rpx;
  }

  .price-row {
    display: flex;
    align-items: baseline;
    gap: 14rpx;
    margin-top: 18rpx;
  }

  .actual-price {
    font-size: 42rpx;
    font-weight: 700;
    color: #fa3534;
  }

  .origin-price {
    font-size: 24rpx;
    color: #999999;
    text-decoration: line-through;
  }

  .coupon {
    padding: 4rpx 10rpx;
    border: 1rpx solid #fa3534;
    border-radius: 6rpx;
    font-size: 22rpx;
    color: #fa3534;
  }

  .metric-panel {
    display: flex;
    gap: 16rpx;
  }

  .metric-item {
    flex: 1;
    min-width: 0;
  }

  .label {
    display: block;
    font-size: 22rpx;
    color: #888888;
  }

  .metric-item .value {
    display: block;
    margin-top: 12rpx;
    font-size: 30rpx;
    font-weight: 600;
    color: #222222;
    word-break: break-all;
  }

  .metric-item .value.rebate {
    color: #fa3534;
  }

  .section-title {
    margin-bottom: 10rpx;
    font-size: 30rpx;
    font-weight: 600;
    color: #222222;
  }

  .detail-line {
    display: flex;
    justify-content: space-between;
    gap: 24rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #eeeeee;
    font-size: 25rpx;
    color: #777777;
  }

  .detail-line .value {
    max-width: 470rpx;
    color: #222222;
    text-align: right;
    word-break: break-all;
  }

  .link-text {
    line-height: 34rpx;
  }

  .bottom-bar {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
    background: #ffffff;
    box-shadow: 0 -8rpx 24rpx rgba(0, 0, 0, 0.06);
  }

  .link-btn {
    height: 76rpx;
    line-height: 76rpx;
    border-radius: 38rpx;
    font-size: 28rpx;
    color: #ffffff;
  }

  .link-panel {
    max-height: 78vh;
    padding: 32rpx 28rpx 48rpx;
    overflow-y: auto;
    background: #ffffff;
  }

  .link-title {
    margin-bottom: 24rpx;
    font-size: 34rpx;
    font-weight: 600;
    color: #222222;
  }

  .link-item {
    padding: 20rpx 0;
    border-bottom: 1rpx solid #eeeeee;
    font-size: 26rpx;
    color: #777777;
  }

  .link-item .value {
    display: block;
    margin-top: 10rpx;
    color: #222222;
    word-break: break-all;
  }
</style>
