<template>
  <s-layout title="我的 CPS 订单" navbar="inner">
    <view class="summary-box">
      <view class="summary-card">
        <view class="summary-label">累计订单</view>
        <view class="summary-value">{{ state.pagination.total }}</view>
      </view>
      <view class="summary-card">
        <view class="summary-label">预估返利</view>
        <view class="summary-value money">¥{{ formatMoney(state.estimateTotal) }}</view>
      </view>
      <view class="summary-card">
        <view class="summary-label">实际返利</view>
        <view class="summary-value money">¥{{ formatMoney(state.realTotal) }}</view>
      </view>
    </view>

    <view class="claim-toolbar">
      <button class="claim-button" @tap="openClaimPopup">找回订单</button>
    </view>

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
        class="order-card"
        v-for="item in state.pagination.list"
        :key="item.id"
        @tap="getOrderDetail(item.id)"
      >
        <view class="order-head ss-flex ss-row-between ss-col-center">
          <view class="platform">{{ platformText(item.platformCode) }}</view>
          <view class="status">{{ orderStatusText(item.orderStatus) }}</view>
        </view>

        <view class="goods-row">
          <image
            class="goods-image"
            :src="item.itemPic || '/static/order-empty.png'"
            mode="aspectFill"
          />
          <view class="goods-info">
            <view class="goods-title ss-line-2">{{ item.itemTitle || '未知商品' }}</view>
            <view class="order-no ss-line-1">订单号 {{ item.platformOrderId || '-' }}</view>
            <view class="price-row ss-flex ss-row-between">
              <text>付款 ¥{{ formatMoney(item.finalPrice || item.itemPrice) }}</text>
              <text class="rebate">预估返利 ¥{{ formatMoney(item.estimateRebate) }}</text>
            </view>
          </view>
        </view>

        <view class="rebate-row ss-flex ss-row-between ss-col-center">
          <view class="freeze-status">{{ rebateFreezeStatusText(item.rebateFreezeStatus) }}</view>
          <view class="real-rebate">实际返利 ¥{{ formatMoney(item.realRebate) }}</view>
        </view>
      </view>

      <s-empty
        v-if="state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/order-empty.png"
        text="暂无 CPS 订单"
      />

      <uni-load-more
        v-if="state.pagination.total > 0"
        :status="state.loadStatus"
        :content-text="{ contentdown: '上拉加载更多' }"
        @tap="loadMore"
      />
    </view>

    <su-popup
      :show="state.showClaimPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showClaimPopup = false"
    >
      <view class="claim-panel">
        <view class="detail-title">找回订单</view>
        <view class="claim-field">
          <text class="claim-label">平台</text>
          <picker
            :range="claimPlatforms"
            range-key="label"
            :value="state.claimPlatformIndex"
            @change="onClaimPlatformChange"
          >
            <view class="claim-input">{{ claimPlatforms[state.claimPlatformIndex].label }}</view>
          </picker>
        </view>
        <view class="claim-field">
          <text class="claim-label">平台订单号</text>
          <input
            v-model.trim="state.claimForm.platformOrderId"
            class="claim-input"
            maxlength="128"
            placeholder="请输入订单号"
          />
        </view>
        <view class="claim-field">
          <text class="claim-label">商品名称</text>
          <input
            v-model.trim="state.claimForm.itemTitle"
            class="claim-input"
            maxlength="512"
            placeholder="选填"
          />
        </view>
        <button class="claim-submit" :loading="state.claimSubmitting" @tap="submitClaim">
          提交申领
        </button>

        <view v-if="state.claimResult" class="claim-result">
          <view class="claim-result-head">
            <text>{{ state.claimResult.platformOrderId }}</text>
            <text :class="['claim-status', `is-${state.claimResult.status.toLowerCase()}`]">
              {{ claimStatusText(state.claimResult.status) }}
            </text>
          </view>
          <view class="claim-message">{{ state.claimResult.message }}</view>
        </view>

        <view v-if="state.claims.length" class="claim-history">
          <view class="claim-history-title">申领记录</view>
          <view v-for="claim in state.claims" :key="claim.claimId" class="claim-history-item">
            <view class="claim-result-head">
              <text class="claim-order-no">{{ claim.platformOrderId }}</text>
              <text :class="['claim-status', `is-${claim.status.toLowerCase()}`]">
                {{ claimStatusText(claim.status) }}
              </text>
            </view>
            <view class="claim-message">{{ claim.message }}</view>
          </view>
        </view>
      </view>
    </su-popup>

    <su-popup
      :show="state.showDetailPopup"
      type="bottom"
      round="20"
      showClose
      @close="state.showDetailPopup = false"
    >
      <view class="detail-panel" v-if="state.currentOrder">
        <view class="detail-title">订单详情</view>
        <view class="detail-goods">
          <image
            class="detail-image"
            :src="state.currentOrder.itemPic || '/static/order-empty.png'"
            mode="aspectFill"
          />
          <view class="detail-goods-info">
            <view class="detail-goods-title ss-line-2">
              {{ state.currentOrder.itemTitle || '未知商品' }}
            </view>
            <view class="detail-platform">{{ platformText(state.currentOrder.platformCode) }}</view>
          </view>
        </view>

        <view class="detail-list">
          <view class="detail-item">
            <text>平台订单号</text>
            <text class="value">{{ state.currentOrder.platformOrderId || '-' }}</text>
          </view>
          <view class="detail-item">
            <text>订单状态</text>
            <text class="value">{{ orderStatusText(state.currentOrder.orderStatus) }}</text>
          </view>
          <view class="detail-item">
            <text>返利状态</text>
            <text class="value">
              {{ rebateFreezeStatusText(state.currentOrder.rebateFreezeStatus) }}
            </text>
          </view>
          <view class="detail-item">
            <text>付款金额</text>
            <text class="value">¥{{ formatMoney(state.currentOrder.finalPrice) }}</text>
          </view>
          <view class="detail-item">
            <text>预估返利</text>
            <text class="value rebate">¥{{ formatMoney(state.currentOrder.estimateRebate) }}</text>
          </view>
          <view class="detail-item">
            <text>实际返利</text>
            <text class="value rebate">¥{{ formatMoney(state.currentOrder.realRebate) }}</text>
          </view>
          <view class="detail-item">
            <text>同步时间</text>
            <text class="value">{{ formatTime(state.currentOrder.syncTime) }}</text>
          </view>
          <view class="detail-item">
            <text>结算时间</text>
            <text class="value">{{ formatTime(state.currentOrder.settleTime) }}</text>
          </view>
        </view>
      </view>
    </su-popup>
  </s-layout>
</template>

<script setup>
  import { reactive } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import { concat } from 'lodash-es';
  import sheep from '@/sheep';
  import { resetPagination } from '@/sheep/helper/utils';
  import CpsOrderApi from '@/sheep/api/cps/order';

  const state = reactive({
    currentTab: 0,
    loadStatus: '',
    estimateTotal: 0,
    realTotal: 0,
    showDetailPopup: false,
    currentOrder: null,
    showClaimPopup: false,
    claimSubmitting: false,
    claimPlatformIndex: 0,
    claimResult: null,
    claims: [],
    claimForm: {
      platformOrderId: '',
      itemTitle: '',
    },
    pagination: {
      list: [],
      total: 0,
      pageNo: 1,
      pageSize: 8,
    },
  });

  const tabMaps = [
    { name: '全部', value: undefined },
    { name: '已付款', value: 'paid' },
    { name: '已结算', value: 'settled' },
    { name: '已到账', value: 'credited' },
    { name: '已退款', value: 'refunded' },
  ];

  const claimPlatforms = [
    { label: '淘宝', value: 'taobao' },
    { label: '淘宝闪购', value: 'eleme' },
    { label: '京东', value: 'jd' },
    { label: '拼多多', value: 'pdd' },
    { label: '抖音', value: 'douyin' },
  ];

  function onTabsChange(e) {
    resetPagination(state.pagination);
    state.currentTab = e.index;
    state.estimateTotal = 0;
    state.realTotal = 0;
    getOrderList();
  }

  async function getOrderList() {
    if (state.loadStatus === 'loading') {
      return;
    }
    state.loadStatus = 'loading';
    const tab = tabMaps[state.currentTab];
    const params = {
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
    };
    if (tab.value) {
      params.orderStatus = tab.value;
    }
    const { code, data } = await CpsOrderApi.getOrderPage(params);
    if (code !== 0) {
      state.loadStatus = 'more';
      return;
    }
    const list = data?.list || [];
    state.pagination.list = concat(state.pagination.list, list);
    state.pagination.total = data?.total || 0;
    state.estimateTotal = state.pagination.list.reduce(
      (sum, item) => sum + toNumber(item.estimateRebate),
      0,
    );
    state.realTotal = state.pagination.list.reduce(
      (sum, item) => sum + toNumber(item.realRebate),
      0,
    );
    state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
  }

  async function getOrderDetail(id) {
    const { code, data } = await CpsOrderApi.getOrder(id);
    if (code !== 0) {
      return;
    }
    state.currentOrder = data;
    state.showDetailPopup = true;
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
    if (code === 0) {
      state.claims = data || [];
    }
  }

  async function submitClaim() {
    const platformOrderId = state.claimForm.platformOrderId.trim();
    if (!platformOrderId) {
      uni.showToast({ title: '请输入订单号', icon: 'none' });
      return;
    }
    const platformCode = claimPlatforms[state.claimPlatformIndex].value;
    state.claimSubmitting = true;
    try {
      const { code, data } = await CpsOrderApi.claimOrder({
        platformCode,
        platformOrderId,
        itemTitle: state.claimForm.itemTitle.trim() || undefined,
        idempotencyKey: createClaimIdempotencyKey(platformCode, platformOrderId),
      });
      if (code !== 0) {
        return;
      }
      state.claimResult = data;
      await loadClaims();
      if (data?.status === 'APPROVED') {
        resetPagination(state.pagination);
        state.estimateTotal = 0;
        state.realTotal = 0;
        await getOrderList();
      }
    } finally {
      state.claimSubmitting = false;
    }
  }

  function createClaimIdempotencyKey(platformCode, platformOrderId) {
    return `claim:${platformCode}:${platformOrderId}:${Date.now()}:${Math.random().toString(36).slice(2, 10)}`;
  }

  function claimStatusText(status) {
    const map = {
      PENDING_SYNC: '等待同步',
      PENDING_REVIEW: '等待审核',
      APPROVED: '已找回',
      REJECTED: '未通过',
      CONFLICT: '归属冲突',
      ASSET_LOCKED: '资金锁定',
    };
    return map[status] || status || '处理中';
  }

  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo++;
    getOrderList();
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

  function orderStatusText(orderStatus) {
    const map = {
      created: '已下单',
      paid: '已付款',
      received: '已收货',
      settled: '已结算',
      credited: '已到账',
      refunded: '已退款',
      invalid: '已失效',
    };
    return map[orderStatus] || orderStatus || '未知状态';
  }

  function rebateFreezeStatusText(rebateFreezeStatus) {
    const map = {
      none: '未冻结',
      frozen: '返利冻结中',
      unfrozen: '已解冻',
      deducted: '已扣减',
      credited: '已到账',
      debt: '已形成欠款',
    };
    return map[rebateFreezeStatus] || rebateFreezeStatus || '返利待确认';
  }

  function formatMoney(value) {
    return toNumber(value).toFixed(2);
  }

  function toNumber(value) {
    const num = Number(value || 0);
    return Number.isFinite(num) ? num : 0;
  }

  function formatTime(value) {
    if (!value) {
      return '-';
    }
    return sheep.$helper.timeFormat(value, 'yyyy-mm-dd hh:MM:ss');
  }

  onLoad(() => {
    getOrderList();
    loadClaims();
  });

  onReachBottom(() => {
    loadMore();
  });
</script>

<style lang="scss" scoped>
  .summary-box {
    display: flex;
    gap: 16rpx;
    padding: 24rpx;
    background: $bg-page;
  }

  .summary-card {
    flex: 1;
    min-width: 0;
    padding: 22rpx 18rpx;
    background: #ffffff;
    border-radius: 10rpx;
  }

  .summary-label {
    font-size: 22rpx;
    color: #888888;
    line-height: 1.4;
  }

  .summary-value {
    margin-top: 12rpx;
    font-size: 32rpx;
    font-weight: 600;
    color: #222222;
    line-height: 1.2;
    word-break: break-all;
  }

  .summary-value.money {
    color: var(--ui-BG-Main);
    font-size: 28rpx;
  }

  .claim-toolbar {
    display: flex;
    justify-content: flex-end;
    padding: 0 24rpx 20rpx;
    background: $bg-page;
  }

  .claim-button,
  .claim-submit {
    min-width: 176rpx;
    height: 72rpx;
    margin: 0;
    padding: 0 28rpx;
    border-radius: 8rpx;
    background: var(--ui-BG-Main);
    color: #ffffff;
    font-size: 26rpx;
    line-height: 72rpx;
  }

  .claim-button::after,
  .claim-submit::after {
    border: 0;
  }

  .claim-panel {
    max-height: 82vh;
    padding: 32rpx 28rpx 48rpx;
    overflow-y: auto;
    background: #ffffff;
  }

  .claim-field {
    margin-bottom: 24rpx;
  }

  .claim-label {
    display: block;
    margin-bottom: 12rpx;
    color: #555555;
    font-size: 24rpx;
  }

  .claim-input {
    box-sizing: border-box;
    width: 100%;
    min-height: 76rpx;
    padding: 18rpx 20rpx;
    border: 1rpx solid #dddddd;
    border-radius: 8rpx;
    color: #222222;
    font-size: 26rpx;
    line-height: 40rpx;
  }

  .claim-submit {
    width: 100%;
    margin-top: 12rpx;
  }

  .claim-result,
  .claim-history-item {
    margin-top: 24rpx;
    padding: 20rpx;
    border: 1rpx solid #eeeeee;
    border-radius: 8rpx;
  }

  .claim-result-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 20rpx;
    color: #222222;
    font-size: 25rpx;
  }

  .claim-order-no {
    min-width: 0;
    overflow-wrap: anywhere;
  }

  .claim-status {
    flex: 0 0 auto;
    color: #666666;
  }

  .claim-status.is-approved {
    color: #19be6b;
  }

  .claim-status.is-rejected,
  .claim-status.is-conflict,
  .claim-status.is-asset_locked {
    color: #fa3534;
  }

  .claim-message {
    margin-top: 12rpx;
    color: #777777;
    font-size: 23rpx;
    line-height: 34rpx;
    overflow-wrap: anywhere;
  }

  .claim-history {
    margin-top: 30rpx;
  }

  .claim-history-title {
    color: #222222;
    font-size: 28rpx;
    font-weight: 600;
  }

  .order-list {
    padding: 20rpx 20rpx 40rpx;
  }

  .order-card {
    margin-bottom: 20rpx;
    padding: 22rpx;
    background: #ffffff;
    border-radius: 10rpx;
  }

  .order-head {
    margin-bottom: 18rpx;
  }

  .platform {
    font-size: 26rpx;
    font-weight: 600;
    color: #333333;
  }

  .status {
    font-size: 24rpx;
    color: var(--ui-BG-Main);
  }

  .goods-row,
  .detail-goods {
    display: flex;
  }

  .goods-image {
    flex: 0 0 148rpx;
    width: 148rpx;
    height: 148rpx;
    border-radius: 8rpx;
    background: #f5f5f5;
  }

  .goods-info {
    flex: 1;
    min-width: 0;
    margin-left: 20rpx;
  }

  .goods-title {
    min-height: 70rpx;
    font-size: 28rpx;
    font-weight: 500;
    color: #222222;
    line-height: 36rpx;
  }

  .order-no {
    margin-top: 12rpx;
    font-size: 22rpx;
    color: #999999;
  }

  .price-row {
    margin-top: 14rpx;
    font-size: 23rpx;
    color: #666666;
  }

  .rebate {
    color: #fa3534;
  }

  .rebate-row {
    margin-top: 20rpx;
    padding-top: 18rpx;
    border-top: 1rpx solid #eeeeee;
    font-size: 24rpx;
  }

  .freeze-status {
    color: #666666;
  }

  .real-rebate {
    color: #fa3534;
    font-weight: 600;
  }

  .detail-panel {
    max-height: 78vh;
    padding: 32rpx 28rpx 48rpx;
    overflow-y: auto;
    background: #ffffff;
  }

  .detail-title {
    margin-bottom: 28rpx;
    font-size: 34rpx;
    font-weight: 600;
    color: #222222;
  }

  .detail-image {
    flex: 0 0 132rpx;
    width: 132rpx;
    height: 132rpx;
    border-radius: 8rpx;
    background: #f5f5f5;
  }

  .detail-goods-info {
    flex: 1;
    min-width: 0;
    margin-left: 20rpx;
  }

  .detail-goods-title {
    font-size: 28rpx;
    font-weight: 500;
    color: #222222;
    line-height: 36rpx;
  }

  .detail-platform {
    margin-top: 18rpx;
    font-size: 24rpx;
    color: #888888;
  }

  .detail-list {
    margin-top: 28rpx;
  }

  .detail-item {
    display: flex;
    justify-content: space-between;
    gap: 24rpx;
    padding: 22rpx 0;
    border-bottom: 1rpx solid #eeeeee;
    font-size: 26rpx;
    color: #777777;
  }

  .detail-item .value {
    max-width: 430rpx;
    color: #222222;
    text-align: right;
    word-break: break-all;
  }
</style>
