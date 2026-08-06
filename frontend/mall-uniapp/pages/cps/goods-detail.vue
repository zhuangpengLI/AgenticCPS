<template>
  <s-layout title="商品返利详情" navbar="inner">
    <view v-if="state.loading" class="detail-skeleton"
      ><view class="image-placeholder" /><view class="line-placeholder" /><view
        class="line-placeholder short"
    /></view>
    <view v-else-if="state.errorMessage" class="error-state"
      ><text>{{ state.errorMessage }}</text
      ><button class="ss-reset-button retry-button" @tap="getDetail">重试</button></view
    >
    <view v-else-if="state.goods" class="detail-page">
      <image
        class="main-image"
        :src="
          state.imageFailed
            ? '/static/goods-empty.png'
            : state.goods.mainPic || '/static/goods-empty.png'
        "
        mode="aspectFill"
        @error="state.imageFailed = true"
      />
      <view class="info-panel">
        <view class="title ss-line-2">{{ state.goods.title || '未知商品' }}</view>
        <view class="meta"
          >{{ platformText(state.goods.platformCode) }} ·
          {{ state.goods.shopName || state.goods.brandName || '商家' }}</view
        >
        <view class="price-row"
          ><text class="actual-price">券后 ¥{{ formatMoney(state.goods.actualPrice) }}</text
          ><text class="origin-price">¥{{ formatMoney(state.goods.originalPrice) }}</text></view
        >
        <view class="benefit-row">
          <text v-if="toNumber(state.goods.couponPrice) > 0" class="coupon"
            >优惠券 ¥{{ formatMoney(state.goods.couponPrice) }}</text
          >
          <text class="rebate">预估返利 ¥{{ formatMoney(state.goods.estimateRebateAmount) }}</text>
        </view>
        <view v-if="state.goods.sellingPoint" class="selling-point">{{
          state.goods.sellingPoint
        }}</view>
      </view>

      <view class="metric-panel">
        <view
          ><text>月销量</text><strong>{{ state.goods.monthSales || 0 }}</strong></view
        >
        <view
          ><text>剩余优惠券</text><strong>{{ state.goods.couponRemainNum || 0 }}</strong></view
        >
        <view
          ><text>预估返利</text
          ><strong class="rebate-value"
            >¥{{ formatMoney(state.goods.estimateRebateAmount) }}</strong
          ></view
        >
      </view>

      <view class="coupon-panel">
        <view class="section-title">优惠信息</view>
        <view class="detail-line"
          ><text>优惠券金额</text><text>¥{{ formatMoney(state.goods.couponPrice) }}</text></view
        >
        <view class="detail-line"
          ><text>使用门槛</text
          ><text>{{
            toNumber(state.goods.couponConditions) > 0
              ? `满 ¥${formatMoney(state.goods.couponConditions)} 可用`
              : '无门槛'
          }}</text></view
        >
        <view class="detail-line"
          ><text>有效期至</text><text>{{ formatTime(state.goods.couponEndTime) }}</text></view
        >
      </view>
      <view class="rebate-notice"
        >页面金额为预估，实际返利以订单最终结算为准。退款、失效或未正确归因的订单可能无法获得返利。</view
      >
      <view class="bottom-spacer" />
    </view>

    <view v-if="state.goods" class="fixed-action">
      <view class="action-saving"
        ><text>券后价</text><strong>¥{{ formatMoney(state.goods.actualPrice) }}</strong></view
      >
      <button
        class="ss-reset-button favorite-button"
        :disabled="state.favoriteLoading || state.favorited"
        @tap="favoriteGoods"
        >{{ state.favoriteLoading ? '处理中' : state.favorited ? '已收藏' : '收藏' }}</button
      >
      <button
        class="ss-reset-button buy-button ui-BG-Main-Gradient"
        :disabled="state.linkLoading"
        @tap="generateLink"
        >{{ state.linkLoading ? '生成中...' : '领券购买' }}</button
      >
    </view>

    <su-popup
      :show="state.showActionPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showActionPopup = false"
    >
      <view class="action-panel">
        <view class="action-title">领券购买</view>
        <view class="action-tip">{{
          state.promotionAction?.type === 'tpwd'
            ? '复制口令后去淘宝打开'
            : `打开${platformText(state.promotionAction?.platformCode)}购买链接`
        }}</view>
        <button
          class="ss-reset-button popup-button ui-BG-Main-Gradient"
          :disabled="state.actionExecuting"
          @tap="runPromotionAction"
          >{{
            state.actionExecuting
              ? '处理中...'
              : state.promotionAction?.type === 'tpwd'
              ? '复制口令'
              : '打开链接'
          }}</button
        >
        <button
          v-if="state.promotionAction?.type === 'url'"
          class="ss-reset-button copy-backup"
          @tap="copyPromotionBackup"
          >复制链接备用</button
        >
        <button
          v-if="state.actionError"
          class="ss-reset-button retry-link"
          @tap="runPromotionAction"
          >处理失败，点击重试</button
        >
      </view>
    </su-popup>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  import CpsGoodsApi from '@/sheep/api/cps/goods';
  import CpsMemberGoodsApi from '@/sheep/api/cps/memberGoods';
  import { trackCpsEvent } from '@/sheep/helper/cpsAnalytics';
  import {
    createPromotionAction,
    copyPromotionValue,
    executePromotionAction,
    formatMoney,
    platformText,
    toNumber,
  } from '@/sheep/helper/cps';

  const state = reactive({
    loading: false,
    errorMessage: '',
    imageFailed: false,
    goods: null,
    query: { platformCode: '', goodsId: '', goodsSign: '' },
    linkLoading: false,
    showActionPopup: false,
    promotionAction: null,
    actionExecuting: false,
    actionError: false,
    favoriteLoading: false,
    favorited: false,
  });

  function buildSnapshot(goods) {
    return {
      platformCode: goods.platformCode,
      goodsId: goods.goodsId,
      goodsSign: goods.goodsSign,
      title: goods.title,
      mainPic: goods.mainPic,
      originalPrice: goods.originalPrice,
      actualPrice: goods.actualPrice,
      couponPrice: goods.couponPrice,
      estimateRebateAmount: goods.estimateRebateAmount,
      monthSales: goods.monthSales,
      shopName: goods.shopName,
    };
  }

  async function recordHistory(goods) {
    if (!sheep.$store('user').isLogin) return;
    try {
      await CpsMemberGoodsApi.recordHistory(buildSnapshot(goods));
    } catch (error) {
      // 足迹是辅助能力，不阻断商品浏览。
    }
  }

  async function getDetail() {
    if (!state.query.platformCode || (!state.query.goodsId && !state.query.goodsSign)) {
      state.errorMessage = '缺少商品参数';
      return;
    }
    if (state.loading) return;
    state.loading = true;
    state.errorMessage = '';
    try {
      const { code, data } = await CpsGoodsApi.getDetail(state.query);
      if (code !== 0 || !data) {
        trackCpsEvent('cps_goods_detail', {
          platformCode: state.query.platformCode,
          result: 'failed',
        });
        state.errorMessage = '商品信息加载失败';
        return;
      }
      state.goods = data;
      recordHistory(data);
      trackCpsEvent('cps_goods_detail', {
        platformCode: data.platformCode,
        result: 'success',
      });
    } catch (error) {
      trackCpsEvent('cps_goods_detail', {
        platformCode: state.query.platformCode,
        result: 'network_error',
      });
      state.errorMessage = '网络异常，请稍后重试';
    } finally {
      state.loading = false;
    }
  }

  async function generateLink() {
    if (!sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    if (!state.goods || state.linkLoading) return;
    state.linkLoading = true;
    try {
      const { code, data } = await CpsGoodsApi.generateLink({
        platformCode: state.goods.platformCode,
        goodsId: state.goods.goodsId,
        goodsSign: state.goods.goodsSign,
      });
      if (code !== 0 || !data) {
        trackCpsEvent('cps_promotion_link', {
          platformCode: state.goods.platformCode,
          result: 'failed',
        });
        sheep.$helper.toast('生成购买链接失败，请重试');
        return;
      }
      const action = createPromotionAction(data, state.goods.platformCode);
      if (!action.value) {
        trackCpsEvent('cps_promotion_link', {
          platformCode: state.goods.platformCode,
          result: 'missing_action',
        });
        sheep.$helper.toast('暂未获取到可用购买链接');
        return;
      }
      state.promotionAction = action;
      state.actionError = false;
      state.showActionPopup = true;
      trackCpsEvent('cps_promotion_link', {
        platformCode: action.platformCode,
        result: 'success',
        actionType: action.type,
      });
    } catch (error) {
      trackCpsEvent('cps_promotion_link', {
        platformCode: state.goods.platformCode,
        result: 'network_error',
      });
      sheep.$helper.toast('生成购买链接失败，请重试');
    } finally {
      state.linkLoading = false;
    }
  }

  async function favoriteGoods() {
    if (!sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    if (!state.goods || state.favoriteLoading || state.favorited) return;
    state.favoriteLoading = true;
    try {
      const { code } = await CpsMemberGoodsApi.createFavorite(buildSnapshot(state.goods));
      if (code !== 0) {
        sheep.$helper.toast('收藏失败，请重试');
        return;
      }
      state.favorited = true;
      sheep.$helper.toast('已收藏，可在返利收藏中查看');
    } catch (error) {
      sheep.$helper.toast('收藏失败，请重试');
    } finally {
      state.favoriteLoading = false;
    }
  }

  async function runPromotionAction() {
    if (state.actionExecuting) return;
    state.actionExecuting = true;
    state.actionError = false;
    try {
      await executePromotionAction(state.promotionAction);
      sheep.$helper.toast(state.promotionAction.displayText);
    } catch (error) {
      state.actionError = true;
      sheep.$helper.toast('打开或复制失败，请重试');
    } finally {
      state.actionExecuting = false;
    }
  }
  async function copyPromotionBackup() {
    try {
      await copyPromotionValue(
        state.promotionAction?.fallbackValue || state.promotionAction?.value,
      );
      sheep.$helper.toast('购买链接已复制');
    } catch (error) {
      sheep.$helper.toast('复制失败，请重试');
    }
  }
  function formatTime(value) {
    return value ? sheep.$helper.timeFormat(value, 'yyyy-mm-dd') : '暂无';
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
    min-height: 100vh;
  }
  .main-image,
  .image-placeholder {
    width: 100%;
    height: 750rpx;
    background: #f2f2f2;
  }
  .info-panel,
  .metric-panel,
  .coupon-panel,
  .rebate-notice {
    margin: 20rpx;
    padding: 26rpx;
    border-radius: 14rpx;
    background: #fff;
  }
  .title {
    font-size: 31rpx;
    font-weight: 600;
    line-height: 43rpx;
    color: #222;
  }
  .meta {
    margin-top: 12rpx;
    color: #999;
    font-size: 23rpx;
  }
  .price-row,
  .benefit-row {
    display: flex;
    align-items: baseline;
    gap: 16rpx;
    margin-top: 18rpx;
  }
  .actual-price {
    color: #fa3534;
    font-size: 36rpx;
    font-weight: 700;
  }
  .origin-price {
    color: #aaa;
    font-size: 23rpx;
    text-decoration: line-through;
  }
  .benefit-row {
    justify-content: space-between;
    align-items: center;
  }
  .coupon {
    padding: 5rpx 10rpx;
    border-radius: 6rpx;
    color: #fa3534;
    background: #fff0ef;
    font-size: 22rpx;
  }
  .rebate,
  .rebate-value {
    color: #8a5a3b;
    font-size: 25rpx;
    font-weight: 600;
  }
  .selling-point {
    margin-top: 20rpx;
    padding: 18rpx;
    border-radius: 8rpx;
    background: #fff8f4;
    color: #76513b;
    font-size: 24rpx;
    line-height: 1.6;
  }
  .metric-panel {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10rpx;
  }
  .metric-panel view {
    min-width: 0;
    text-align: center;
  }
  .metric-panel text,
  .metric-panel strong {
    display: block;
    overflow-wrap: anywhere;
  }
  .metric-panel text {
    color: #999;
    font-size: 21rpx;
  }
  .metric-panel strong {
    margin-top: 12rpx;
    color: #333;
    font-size: 26rpx;
  }
  .section-title {
    margin-bottom: 12rpx;
    font-size: 29rpx;
    font-weight: 600;
    color: #222;
  }
  .detail-line {
    display: flex;
    justify-content: space-between;
    gap: 24rpx;
    padding: 18rpx 0;
    border-bottom: 1rpx solid #eee;
    color: #777;
    font-size: 24rpx;
  }
  .detail-line text:last-child {
    color: #333;
    text-align: right;
  }
  .rebate-notice {
    color: #8a5a3b;
    background: #fff7f2;
    font-size: 22rpx;
    line-height: 1.65;
  }
  .bottom-spacer {
    height: 130rpx;
  }
  .fixed-action {
    position: fixed;
    z-index: 20;
    right: 0;
    bottom: 0;
    left: 0;
    display: flex;
    align-items: center;
    gap: 22rpx;
    padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
    background: #fff;
    box-shadow: 0 -4rpx 20rpx rgba(0, 0, 0, 0.08);
  }
  .action-saving {
    flex: 1;
    font-size: 21rpx;
    color: #999;
  }
  .action-saving strong {
    display: block;
    margin-top: 4rpx;
    color: #fa3534;
    font-size: 31rpx;
  }
  .buy-button {
    flex: 0 0 280rpx;
    height: 78rpx;
    line-height: 78rpx;
    border-radius: 39rpx;
    color: #fff;
    font-size: 28rpx;
  }
  .favorite-button {
    flex: 0 0 92rpx;
    color: var(--ui-BG-Main);
    font-size: 23rpx;
  }
  .action-panel {
    padding: 34rpx 28rpx 54rpx;
    background: #fff;
  }
  .action-title {
    font-size: 34rpx;
    font-weight: 600;
  }
  .action-tip {
    margin-top: 18rpx;
    color: #777;
    font-size: 25rpx;
  }
  .popup-button {
    width: 100%;
    height: 80rpx;
    line-height: 80rpx;
    margin-top: 30rpx;
    border-radius: 40rpx;
    color: #fff;
  }
  .retry-link {
    margin: 20rpx auto 0;
    color: #fa3534;
    font-size: 23rpx;
  }
  .copy-backup {
    margin: 20rpx auto 0;
    color: var(--ui-BG-Main);
    font-size: 23rpx;
  }
  .detail-skeleton {
    padding-bottom: 30rpx;
  }
  .line-placeholder {
    height: 34rpx;
    margin: 28rpx 24rpx 0;
    border-radius: 17rpx;
    background: #eee;
  }
  .line-placeholder.short {
    width: 60%;
  }
  .error-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 24rpx;
    padding: 160rpx 30rpx;
    color: #888;
  }
  .retry-button {
    width: 180rpx;
    height: 62rpx;
    line-height: 62rpx;
    border-radius: 31rpx;
    color: #fff;
    background: var(--ui-BG-Main);
  }
  button[disabled] {
    opacity: 0.55;
  }
</style>
