<template>
  <s-layout title="返利钱包" navbar="inner" tabbar="/pages/cps/wallet">
    <view class="balance-panel ui-BG-Main-Gradient">
      <view class="balance-label">可用返利</view>
      <view class="balance-value">¥{{ formatMoney(state.account.availableBalance) }}</view>
      <button class="ss-reset-button withdraw-entry" @tap="goWithdraw">去提现</button>
    </view>

    <view class="asset-card">
      <view v-for="item in accountCards" :key="item.key" class="asset-item"
        ><text>{{ item.label }}</text
        ><strong>¥{{ formatMoney(item.value) }}</strong></view
      >
    </view>

    <view class="exchange-card" @tap="goExchange">
      <view><strong>返利兑换 Token</strong><text>将可用返利兑换为 Agentic Token</text></view>
      <text class="exchange-arrow">去兑换 ›</text>
    </view>

    <view v-if="hasDebt" class="debt-card">
      <view class="debt-head"
        ><text>待偿还欠款</text
        ><strong>¥{{ formatCentMoney(state.debtSummary.outstandingDebtCent) }}</strong></view
      >
      <view class="debt-desc">后续可用返利可能优先用于偿还欠款</view>
    </view>

    <view class="record-section">
      <su-tabs v-if="hasDebt" :list="tabMaps" :current="state.currentTab" @change="onTabsChange" />
      <view v-else class="section-title">返利明细</view>

      <template v-if="state.currentTab === 0">
        <view v-for="item in state.pagination.list" :key="item.id" class="record-row">
          <view class="record-main"
            ><text>{{ rebateTypeText(item.rebateType) }}</text
            ><text class="record-time">{{ formatTime(item.createTime) }}</text></view
          >
          <view class="record-side"
            ><strong :class="{ income: toNumber(item.rebateAmount) > 0 }">{{
              signedMoney(item.rebateAmount)
            }}</strong
            ><text>{{ rebateStatusText(item.rebateStatus || item.status) }}</text></view
          >
        </view>
      </template>
      <template v-else>
        <view v-for="item in state.pagination.list" :key="item.id" class="record-row">
          <view class="record-main"
            ><text>{{ debtBusinessText(item.businessType) }}</text
            ><text class="record-time">{{ formatTime(item.createTime) }}</text></view
          >
          <view class="record-side"
            ><strong>{{ formatSignedCentMoney(item.debtChangeCent) }}</strong
            ><text>余额 ¥{{ formatCentMoney(item.debtAfterCent) }}</text></view
          >
        </view>
      </template>

      <view v-if="state.errorMessage && !state.pagination.list.length" class="empty-state"
        ><text>{{ state.errorMessage }}</text
        ><button class="ss-reset-button retry-button" @tap="retryRecords">重试</button></view
      >
      <s-empty
        v-else-if="state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/data-empty.png"
        :text="state.currentTab === 0 ? '暂无返利明细' : '暂无偿债流水'"
      />
      <uni-load-more
        v-if="state.pagination.total > 0 || state.loadStatus === 'loading'"
        :status="state.loadStatus"
        @tap="loadMore"
      />
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import { onLoad, onReachBottom } from '@dcloudio/uni-app';
  import { concat } from 'lodash-es';
  import sheep from '@/sheep';
  import { resetPagination } from '@/sheep/helper/utils';
  import { formatCentMoney, formatMoney, toNumber } from '@/sheep/helper/cps';
  import CpsRebateApi from '@/sheep/api/cps/rebate';

  const state = reactive({
    currentTab: 0,
    loadStatus: '',
    errorMessage: '',
    account: {
      availableBalance: 0,
      pendingRebate: 0,
      frozenBalance: 0,
      debtBalance: 0,
      withdrawableBalance: 0,
      exchangeableBalance: 0,
    },
    debtSummary: { outstandingDebtCent: 0, repaidDebtCent: 0, waivedDebtCent: 0 },
    pagination: { list: [], total: 0, pageNo: 1, pageSize: 8 },
  });
  const tabMaps = [
    { name: '返利明细', value: 'records' },
    { name: '偿债流水', value: 'debts' },
  ];
  const hasDebt = computed(
    () =>
      toNumber(state.debtSummary.outstandingDebtCent) > 0 ||
      toNumber(state.account.debtBalance) > 0,
  );
  const accountCards = computed(() => [
    { key: 'pendingRebate', label: '待入账', value: state.account.pendingRebate },
    { key: 'frozenBalance', label: '冻结中', value: state.account.frozenBalance },
    { key: 'withdrawableBalance', label: '可提现', value: state.account.withdrawableBalance },
  ]);

  async function getAccount() {
    const { code, data } = await CpsRebateApi.getAccount();
    if (code === 0) state.account = data || {};
  }
  async function getDebtSummary() {
    const { code, data } = await CpsRebateApi.getDebtSummary();
    if (code === 0) state.debtSummary = data || {};
  }
  async function getRecordPage() {
    if (state.loadStatus === 'loading') return;
    state.loadStatus = 'loading';
    state.errorMessage = '';
    const params = { pageNo: state.pagination.pageNo, pageSize: state.pagination.pageSize };
    try {
      const { code, data } = await (state.currentTab === 0
        ? CpsRebateApi.getRecordPage(params)
        : CpsRebateApi.getDebtRepaymentPage(params));
      if (code !== 0) {
        state.errorMessage = '明细加载失败';
        state.loadStatus = 'more';
        return;
      }
      const list = data?.list || [];
      state.pagination.list = concat(state.pagination.list, list);
      state.pagination.total = data?.total || 0;
      state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
    } catch (error) {
      if (error?.code === 401) {
        state.loadStatus = 'more';
        return;
      }
      state.errorMessage = '网络异常，请重试';
      state.loadStatus = 'more';
    }
  }
  function onTabsChange(event) {
    state.currentTab = event.index;
    resetPagination(state.pagination);
    getRecordPage();
  }
  function retryRecords() {
    resetPagination(state.pagination);
    getRecordPage();
  }
  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') return;
    state.pagination.pageNo++;
    getRecordPage();
  }
  function goWithdraw() {
    sheep.$router.go('/pages/cps/withdraw');
  }
  function goExchange() {
    sheep.$router.go('/pages/cps/exchange');
  }
  function signedMoney(value) {
    const amount = toNumber(value);
    return `${amount > 0 ? '+' : ''}¥${formatMoney(amount)}`;
  }
  function formatSignedCentMoney(value) {
    const amount = toNumber(value);
    return `${amount > 0 ? '+' : ''}¥${formatCentMoney(amount)}`;
  }
  function rebateTypeText(value) {
    return (
      { rebate: '返利', refund: '退款', adjust: '调账', exchange: '兑换', withdraw: '提现' }[
        value
      ] ||
      value ||
      '返利'
    );
  }
  function rebateStatusText(value) {
    return (
      {
        pending: '待入账',
        available: '可用',
        frozen: '冻结中',
        deducted: '已扣减',
        invalid: '已失效',
        refunded: '已退款',
        received: '已到账',
        rebate_received: '已到账',
        // 兼容历史记录中的旧状态值，仅用于展示。
        credited: '已到账',
      }[value] ||
      value ||
      '待确认'
    );
  }
  function debtBusinessText(value) {
    return (
      {
        refund: '退款欠款',
        repayment: '返利抵扣',
        waive: '欠款减免',
        adjust: '欠款调账',
        exchange: '兑换扣减',
        withdraw: '提现扣减',
      }[value] ||
      value ||
      '偿债流水'
    );
  }
  function formatTime(value) {
    return value ? sheep.$helper.timeFormat(value, 'yyyy-mm-dd hh:MM') : '-';
  }

  onLoad(async (options = {}) => {
    try {
      await Promise.all([getAccount(), getDebtSummary()]);
      if (options.type === 'debt' && hasDebt.value) state.currentTab = 1;
      await getRecordPage();
    } catch (error) {
      if (error?.code !== 401) state.errorMessage = '网络异常，请重试';
    }
  });
  onReachBottom(loadMore);
</script>

<style lang="scss" scoped>
  .balance-panel {
    position: relative;
    padding: 38rpx 30rpx 80rpx;
    color: #fff;
  }
  .balance-label {
    font-size: 25rpx;
    opacity: 0.9;
  }
  .balance-value {
    margin-top: 16rpx;
    font-size: 58rpx;
    font-weight: 700;
    overflow-wrap: anywhere;
  }
  .withdraw-entry {
    position: absolute;
    right: 28rpx;
    bottom: 88rpx;
    width: 144rpx;
    height: 60rpx;
    line-height: 60rpx;
    border-radius: 30rpx;
    color: var(--ui-BG-Main);
    background: #fff;
    font-size: 24rpx;
  }
  .asset-card {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin: -44rpx 24rpx 20rpx;
    padding: 25rpx 10rpx;
    border-radius: 14rpx;
    background: #fff;
    box-shadow: 0 7rpx 24rpx rgba(0, 0, 0, 0.06);
  }
  .exchange-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 20rpx;
    margin: 0 24rpx 20rpx;
    padding: 24rpx 26rpx;
    border-radius: 14rpx;
    background: #fff;
    box-shadow: 0 7rpx 24rpx rgba(0, 0, 0, 0.05);
  }
  .exchange-card view {
    display: flex;
    min-width: 0;
    flex-direction: column;
    gap: 8rpx;
  }
  .exchange-card strong {
    color: #222;
    font-size: 27rpx;
  }
  .exchange-card view text {
    color: #999;
    font-size: 21rpx;
  }
  .exchange-arrow {
    flex-shrink: 0;
    color: var(--ui-BG-Main);
    font-size: 23rpx;
  }
  .asset-item {
    min-width: 0;
    text-align: center;
  }
  .asset-item text,
  .asset-item strong {
    display: block;
    overflow-wrap: anywhere;
  }
  .asset-item text {
    color: #999;
    font-size: 21rpx;
  }
  .asset-item strong {
    margin-top: 11rpx;
    color: #333;
    font-size: 27rpx;
  }
  .debt-card {
    margin: 0 24rpx 20rpx;
    padding: 22rpx;
    border-radius: 12rpx;
    background: #fff7f2;
  }
  .debt-head {
    display: flex;
    justify-content: space-between;
    color: #8a5a3b;
    font-size: 25rpx;
  }
  .debt-head strong {
    color: #fa3534;
  }
  .debt-desc {
    margin-top: 12rpx;
    color: #9a765f;
    font-size: 21rpx;
  }
  .record-section {
    margin: 0 24rpx 30rpx;
    padding: 24rpx;
    border-radius: 14rpx;
    background: #fff;
  }
  .section-title {
    margin-bottom: 10rpx;
    color: #222;
    font-size: 29rpx;
    font-weight: 600;
  }
  .record-row {
    display: flex;
    justify-content: space-between;
    gap: 24rpx;
    padding: 22rpx 0;
    border-bottom: 1rpx solid #eee;
  }
  .record-main,
  .record-side {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 10rpx;
    color: #333;
    font-size: 25rpx;
  }
  .record-side {
    flex-shrink: 0;
    text-align: right;
    color: #999;
    font-size: 21rpx;
  }
  .record-side strong {
    color: #333;
    font-size: 27rpx;
  }
  .record-side strong.income {
    color: #fa3534;
  }
  .record-time {
    color: #999;
    font-size: 21rpx;
  }
  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20rpx;
    padding: 80rpx 30rpx;
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
</style>
