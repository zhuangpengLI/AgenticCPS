<template>
  <s-layout title="返利订单" navbar="inner">
    <view class="top-actions"
      ><text>订单返利以最终结算为准</text
      ><button class="ss-reset-button claim-entry" @tap="openClaimPopup">找回订单</button></view
    >
    <su-sticky bgColor="#fff">
      <su-tabs
        :list="tabMaps"
        :scrollable="true"
        :current="state.currentTab"
        @change="onTabsChange"
      />
    </su-sticky>

    <view class="order-list">
      <view
        v-for="item in state.pagination.list"
        :key="item.id || item.platformOrderId"
        class="order-card"
        @tap="getOrderDetail(item.id)"
      >
        <view class="order-head"
          ><text>{{ platformText(item.platformCode) }}</text
          ><strong>{{ orderStatusText(item.orderStatus) }}</strong></view
        >
        <view class="goods-title ss-line-2">{{ item.itemTitle || '商品订单' }}</view>
        <view class="order-no ss-line-1">订单号：{{ item.platformOrderId || '-' }}</view>
        <view class="amount-grid">
          <view
            ><text>付款金额</text
            ><strong>¥{{ formatMoney(orderPayAmount(item)) }}</strong></view
          >
          <view
            ><text>预估返利</text><strong>¥{{ formatMoney(item.estimateRebate) }}</strong></view
          >
          <view
            ><text>实际返利</text
            ><strong class="rebate">¥{{ formatMoney(item.realRebate) }}</strong></view
          >
        </view>
        <view class="order-foot"
          ><text>{{ rebateFreezeStatusText(item.rebateFreezeStatus) }}</text
          ><text>{{ formatTime(item.payTime || item.createTime) }}</text></view
        >
      </view>
      <view v-if="state.errorMessage && !state.pagination.list.length" class="empty-state"
        ><text>{{ state.errorMessage }}</text
        ><button class="ss-reset-button retry-button" @tap="retryList">重试</button></view
      >
      <s-empty
        v-else-if="state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/data-empty.png"
        text="暂无返利订单"
      />
      <uni-load-more
        v-if="state.pagination.total > 0 || state.loadStatus === 'loading'"
        :status="state.loadStatus"
        @tap="loadMore"
      />
    </view>

    <su-popup
      :show="state.showDetailPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showDetailPopup = false"
    >
      <view v-if="state.currentOrder" class="detail-panel">
        <view class="popup-title">订单详情</view>
        <view class="detail-item"
          ><text>订单号</text><text>{{ state.currentOrder.platformOrderId || '-' }}</text></view
        >
        <view class="detail-item"
          ><text>商品</text><text>{{ state.currentOrder.itemTitle || '-' }}</text></view
        >
        <view class="detail-item"
          ><text>付款金额</text
          ><text
            >¥{{
              formatMoney(orderPayAmount(state.currentOrder))
            }}</text
          ></view
        >
        <view class="detail-item"
          ><text>预估返利</text
          ><text>¥{{ formatMoney(state.currentOrder.estimateRebate) }}</text></view
        >
        <view class="detail-item"
          ><text>实际返利</text><text>¥{{ formatMoney(state.currentOrder.realRebate) }}</text></view
        >
        <view class="detail-item"
          ><text>订单状态</text
          ><text>{{ orderStatusText(state.currentOrder.orderStatus) }}</text></view
        >
        <view class="detail-item"
          ><text>到账状态</text
          ><text>{{ rebateFreezeStatusText(state.currentOrder.rebateFreezeStatus) }}</text></view
        >
        <view class="detail-item"
          ><text>预计到账</text><text>{{ expectedCreditText(state.currentOrder) }}</text></view
        >
      </view>
    </su-popup>

    <su-popup
      :show="state.showClaimPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showClaimPopup = false"
    >
      <view class="claim-panel">
        <view class="popup-title">找回订单</view>
        <view class="claim-notice">提交订单号只用于审核和归因，不会直接增加返利。</view>
        <picker
          :range="claimPlatforms"
          range-key="label"
          :value="state.claimPlatformIndex"
          @change="onClaimPlatformChange"
          ><view class="picker-value"
            >{{ claimPlatforms[state.claimPlatformIndex].label }}<text>请选择</text></view
          ></picker
        >
        <uni-easyinput v-model="state.claimForm.platformOrderId" placeholder="请输入平台订单号" />
        <uni-easyinput v-model="state.claimForm.itemTitle" placeholder="商品名称（选填）" />
        <button
          class="ss-reset-button submit-button ui-BG-Main-Gradient"
          :disabled="state.claimSubmitting"
          @tap="submitClaim"
          >{{ state.claimSubmitting ? '提交中...' : '提交审核' }}</button
        >
        <view v-if="state.claimResult" class="claim-result"
          >申请状态：{{ claimStatusText(state.claimResult.status) }}</view
        >
        <view v-if="state.claims.length" class="claim-history">
          <view class="minor-title">最近申请</view>
          <view
            v-for="item in state.claims.slice(0, 5)"
            :key="item.id || item.platformOrderId"
            class="claim-row"
            ><text class="ss-line-1">{{ item.platformOrderId }}</text
            ><strong>{{ claimStatusText(item.status) }}</strong></view
          >
        </view>
      </view>
    </su-popup>
  </s-layout>
</template>

<script setup>
  import { reactive, watch } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import { concat } from 'lodash-es';
  import sheep from '@/sheep';
  import { resetPagination } from '@/sheep/helper/utils';
  import { createCpsIdempotencyKey, formatMoney, platformText, toNumber } from '@/sheep/helper/cps';
  import CpsOrderApi from '@/sheep/api/cps/order';
  import { trackCpsEvent } from '@/sheep/helper/cpsAnalytics';

  const state = reactive({
    currentTab: 0,
    loadStatus: '',
    errorMessage: '',
    showDetailPopup: false,
    currentOrder: null,
    showClaimPopup: false,
    claimSubmitting: false,
    claimPlatformIndex: 0,
    claimIdempotencyKey: '',
    claimResult: null,
    claims: [],
    claimForm: { platformOrderId: '', itemTitle: '' },
    pagination: { list: [], total: 0, pageNo: 1, pageSize: 8 },
  });
  const tabMaps = [
    { name: '全部', value: undefined },
    { name: '已付款', value: 'paid' },
    { name: '已收货', value: 'received' },
    { name: '已结算', value: 'settled' },
    { name: '已到账', value: 'credited' },
    { name: '已退款', value: 'refunded' },
    { name: '已失效', value: 'invalid' },
  ];
  const claimPlatforms = [
    { label: '淘宝', value: 'taobao' },
    { label: '淘宝闪购', value: 'eleme' },
    { label: '京东', value: 'jd' },
    { label: '拼多多', value: 'pdd' },
    { label: '抖音', value: 'douyin' },
  ];

  async function getOrderList() {
    if (state.loadStatus === 'loading') return;
    state.loadStatus = 'loading';
    state.errorMessage = '';
    const tab = tabMaps[state.currentTab];
    const params = { pageNo: state.pagination.pageNo, pageSize: state.pagination.pageSize };
    if (tab.value) params.orderStatus = tab.value;
    try {
      const { code, data } = await CpsOrderApi.getOrderPage(params);
      if (code !== 0) {
        state.errorMessage = '订单加载失败';
        state.loadStatus = 'more';
        return;
      }
      const list = data?.list || [];
      state.pagination.list = concat(state.pagination.list, list);
      state.pagination.total = data?.total || 0;
      state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
    } catch (error) {
      state.errorMessage = '网络异常，请重试';
      state.loadStatus = 'more';
    }
  }
  function onTabsChange(event) {
    resetPagination(state.pagination);
    state.currentTab = event.index;
    getOrderList();
  }
  function retryList() {
    resetPagination(state.pagination);
    getOrderList();
  }
  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') return;
    state.pagination.pageNo++;
    getOrderList();
  }
  async function getOrderDetail(id) {
    const { code, data } = await CpsOrderApi.getOrder(id);
    if (code === 0) {
      state.currentOrder = data;
      state.showDetailPopup = true;
    }
  }

  async function openClaimPopup() {
    state.showClaimPopup = true;
    await loadClaims();
  }
  function onClaimPlatformChange(event) {
    state.claimPlatformIndex = Number(event.detail.value || 0);
  }
  async function loadClaims() {
    const { code, data } = await CpsOrderApi.getClaimList();
    if (code === 0) state.claims = data || [];
  }
  function ensureClaimIdempotencyKey() {
    if (!state.claimIdempotencyKey)
      state.claimIdempotencyKey = createCpsIdempotencyKey('cps-order-claim');
  }
  function resetClaimIdempotencyKey() {
    state.claimIdempotencyKey = '';
  }

  async function submitClaim() {
    const platformOrderId = state.claimForm.platformOrderId.trim();
    if (!platformOrderId) {
      sheep.$helper.toast('请输入订单号');
      return;
    }
    if (state.claimSubmitting) return;
    ensureClaimIdempotencyKey();
    state.claimSubmitting = true;
    try {
      const { code, data } = await CpsOrderApi.claimOrder({
        platformCode: claimPlatforms[state.claimPlatformIndex].value,
        platformOrderId,
        itemTitle: state.claimForm.itemTitle.trim() || undefined,
        idempotencyKey: state.claimIdempotencyKey,
      });
      if (code !== 0) {
        trackCpsEvent('cps_order_claim', {
          platformCode: claimPlatforms[state.claimPlatformIndex].value,
          result: 'failed',
        });
        return;
      }
      state.claimResult = data;
      trackCpsEvent('cps_order_claim', {
        platformCode: claimPlatforms[state.claimPlatformIndex].value,
        result: 'success',
        claimStatus: data?.status,
      });
      resetClaimIdempotencyKey();
      await loadClaims();
      if (data?.status === 'APPROVED') {
        resetPagination(state.pagination);
        await getOrderList();
      }
    } catch (error) {
      trackCpsEvent('cps_order_claim', {
        platformCode: claimPlatforms[state.claimPlatformIndex].value,
        result: 'network_error',
      });
      sheep.$helper.toast('提交状态未知，请保持表单不变后重试');
    } finally {
      state.claimSubmitting = false;
    }
  }

  watch(
    () => [state.claimPlatformIndex, state.claimForm.platformOrderId, state.claimForm.itemTitle],
    resetClaimIdempotencyKey,
  );
  function claimStatusText(status) {
    return (
      {
        PENDING_SYNC: '等待同步',
        PENDING_REVIEW: '等待审核',
        APPROVED: '已找回',
        REJECTED: '未通过',
        CONFLICT: '归属冲突',
        ASSET_LOCKED: '资金锁定',
      }[status] ||
      status ||
      '处理中'
    );
  }
  function orderStatusText(status) {
    return (
      {
        created: '已下单',
        paid: '已付款',
        received: '已收货',
        settled: '已结算',
        credited: '已到账',
        refunded: '已退款',
        invalid: '已失效',
      }[status] ||
      status ||
      '待确认'
    );
  }
  function rebateFreezeStatusText(status) {
    return (
      {
        none: '返利待确认',
        frozen: '待入账',
        unfrozen: '已解冻',
        deducted: '已扣减',
        credited: '已到账',
        debt: '已形成欠款',
      }[status] ||
      status ||
      '返利待确认'
    );
  }
  function expectedCreditText(order) {
    if (order.orderStatus === 'credited') return '已到账';
    if (['refunded', 'invalid'].includes(order.orderStatus)) return '不会到账';
    const expectedTime = order.expectedCreditTime || order.settleTime;
    return expectedTime ? formatTime(expectedTime) : '以平台结算为准';
  }
  /**
   * The app order response exposes the paid amount as `finalPrice` (券后价).
   * `payAmount`/`orderAmount` are legacy aliases used by older clients and are
   * not returned by `/cps/order/page`, so prefer the canonical field first.
   * When syncing an older record without finalPrice, derive the amount from
   * itemPrice - couponAmount before falling back to the available snapshot.
   */
  function orderPayAmount(order = {}) {
    const explicitAmount = [order.finalPrice, order.payAmount, order.orderAmount].find(
      (value) => value !== null && value !== undefined && value !== '',
    );
    if (explicitAmount !== undefined) return explicitAmount;
    if (
      order.itemPrice !== null &&
      order.itemPrice !== undefined &&
      order.couponAmount !== null &&
      order.couponAmount !== undefined
    ) {
      return toNumber(order.itemPrice) - toNumber(order.couponAmount);
    }
    return order.itemPrice;
  }
  function formatTime(value) {
    return value ? sheep.$helper.timeFormat(value, 'yyyy-mm-dd hh:MM') : '-';
  }

  onLoad(() => {
    getOrderList();
    loadClaims();
  });
  onReachBottom(loadMore);
</script>

<style lang="scss" scoped>
  .top-actions {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
    padding: 20rpx 24rpx;
    background: #fff7f2;
    color: #8a5a3b;
    font-size: 22rpx;
  }
  .claim-entry {
    flex: 0 0 150rpx;
    height: 58rpx;
    line-height: 58rpx;
    border-radius: 29rpx;
    color: #fff;
    background: var(--ui-BG-Main);
    font-size: 23rpx;
  }
  .order-list {
    padding: 20rpx;
  }
  .order-card {
    margin-bottom: 18rpx;
    padding: 24rpx;
    border-radius: 14rpx;
    background: #fff;
  }
  .order-head,
  .order-foot,
  .picker-value,
  .claim-row {
    display: flex;
    justify-content: space-between;
    gap: 20rpx;
  }
  .order-head {
    color: #777;
    font-size: 24rpx;
  }
  .order-head strong {
    color: var(--ui-BG-Main);
  }
  .goods-title {
    margin-top: 18rpx;
    min-height: 68rpx;
    color: #222;
    font-size: 28rpx;
    font-weight: 600;
    line-height: 34rpx;
  }
  .order-no {
    margin-top: 10rpx;
    color: #999;
    font-size: 21rpx;
  }
  .amount-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 10rpx;
    margin-top: 22rpx;
    padding: 20rpx 8rpx;
    background: #fafafa;
  }
  .amount-grid view {
    min-width: 0;
    text-align: center;
  }
  .amount-grid text,
  .amount-grid strong {
    display: block;
    overflow-wrap: anywhere;
  }
  .amount-grid text {
    color: #999;
    font-size: 20rpx;
  }
  .amount-grid strong {
    margin-top: 9rpx;
    color: #333;
    font-size: 24rpx;
  }
  .amount-grid .rebate {
    color: #fa3534;
  }
  .order-foot {
    margin-top: 18rpx;
    color: #999;
    font-size: 21rpx;
  }
  .detail-panel,
  .claim-panel {
    max-height: 78vh;
    padding: 32rpx 28rpx 54rpx;
    overflow-y: auto;
    background: #fff;
  }
  .popup-title {
    margin-bottom: 22rpx;
    color: #222;
    font-size: 34rpx;
    font-weight: 600;
  }
  .detail-item {
    display: flex;
    justify-content: space-between;
    gap: 28rpx;
    padding: 20rpx 0;
    border-bottom: 1rpx solid #eee;
    color: #777;
    font-size: 25rpx;
  }
  .detail-item text:last-child {
    max-width: 440rpx;
    text-align: right;
    color: #222;
    overflow-wrap: anywhere;
  }
  .claim-notice {
    margin-bottom: 22rpx;
    padding: 18rpx;
    border-radius: 10rpx;
    background: #fff7f2;
    color: #8a5a3b;
    font-size: 22rpx;
    line-height: 1.6;
  }
  .picker-value {
    margin-bottom: 18rpx;
    padding: 22rpx;
    border: 1rpx solid #eee;
    border-radius: 8rpx;
    color: #333;
    font-size: 25rpx;
  }
  .picker-value text {
    color: #999;
  }
  .claim-panel :deep(.uni-easyinput) {
    margin-bottom: 18rpx;
  }
  .submit-button {
    width: 100%;
    height: 78rpx;
    line-height: 78rpx;
    margin-top: 24rpx;
    border-radius: 39rpx;
    color: #fff;
  }
  .claim-result {
    margin-top: 20rpx;
    color: var(--ui-BG-Main);
    font-size: 25rpx;
  }
  .claim-history {
    margin-top: 28rpx;
    padding-top: 22rpx;
    border-top: 1rpx solid #eee;
  }
  .minor-title {
    margin-bottom: 12rpx;
    font-size: 26rpx;
    font-weight: 600;
  }
  .claim-row {
    padding: 15rpx 0;
    color: #777;
    font-size: 23rpx;
  }
  .claim-row text {
    flex: 1;
    min-width: 0;
  }
  .claim-row strong {
    flex-shrink: 0;
    color: #333;
  }
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20rpx;
    padding: 100rpx 30rpx;
    color: #999;
  }
  .retry-button {
    width: 160rpx;
    height: 60rpx;
    line-height: 60rpx;
    border-radius: 30rpx;
    color: #fff;
    background: var(--ui-BG-Main);
  }
  button[disabled] {
    opacity: 0.55;
  }
</style>
