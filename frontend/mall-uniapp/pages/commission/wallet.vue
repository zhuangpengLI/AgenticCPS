<template>
  <s-layout title="我的返利" navbar="inner">
    <view class="account-panel">
      <view class="balance-card primary">
        <view class="balance-label">可用返利</view>
        <view class="balance-value">¥{{ formatMoney(state.account.availableBalance) }}</view>
      </view>
      <view class="balance-grid">
        <view class="balance-card" v-for="item in accountCards" :key="item.key">
          <view class="balance-label">{{ item.label }}</view>
          <view class="balance-value small">¥{{ formatMoney(item.value) }}</view>
        </view>
      </view>
      <view class="debt-summary">
        <view>
          <view class="summary-label">当前欠款</view>
          <view class="summary-value debt">
            ¥{{ formatCentMoney(state.debtSummary.outstandingDebtCent) }}
          </view>
        </view>
        <view>
          <view class="summary-label">累计偿还</view>
          <view class="summary-value"
            >¥{{ formatCentMoney(state.debtSummary.repaidDebtCent) }}</view
          >
        </view>
        <view>
          <view class="summary-label">减免欠款</view>
          <view class="summary-value"
            >¥{{ formatCentMoney(state.debtSummary.waivedDebtCent) }}</view
          >
        </view>
      </view>
    </view>

    <su-sticky bgColor="#fff">
      <su-tabs
        :list="tabMaps"
        :scrollable="false"
        :current="state.currentTab"
        @change="onTabsChange"
      />
    </su-sticky>

    <view class="record-list">
      <view v-if="state.currentTab === 0">
        <view class="record-card" v-for="item in state.pagination.list" :key="item.id">
          <view class="record-head ss-flex ss-row-between ss-col-center">
            <view class="record-title ss-line-1">{{ item.itemTitle || '返利记录' }}</view>
            <view class="record-amount">+¥{{ formatMoney(item.rebateAmount) }}</view>
          </view>
          <view class="record-meta ss-line-1">
            {{ platformText(item.platformCode) }} · 订单 {{ item.platformOrderId || '-' }}
          </view>
          <view class="record-foot ss-flex ss-row-between ss-col-center">
            <text>{{ formatTime(item.createTime) }}</text>
            <text
              >{{ rebateTypeText(item.rebateType) }} ·
              {{ rebateStatusText(item.rebateStatus) }}</text
            >
          </view>
        </view>
      </view>

      <view v-else>
        <view class="record-card" v-for="item in state.pagination.list" :key="item.id">
          <view class="record-head ss-flex ss-row-between ss-col-center">
            <view class="record-title ss-line-1">{{ debtBusinessText(item.businessType) }}</view>
            <view class="record-amount debt">
              {{ formatSignedCentMoney(item.debtChangeCent) }}
            </view>
          </view>
          <view class="record-meta ss-line-1">{{ item.reason || '偿债流水' }}</view>
          <view class="record-foot ss-flex ss-row-between ss-col-center">
            <text>{{ formatTime(item.createTime) }}</text>
            <text>剩余 ¥{{ formatCentMoney(item.debtAfterCent) }}</text>
          </view>
        </view>
      </view>

      <s-empty
        v-if="state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/data-empty.png"
        :text="state.currentTab === 0 ? '暂无返利明细' : '暂无偿债流水'"
      />

      <uni-load-more
        v-if="state.pagination.total > 0"
        :status="state.loadStatus"
        :content-text="{ contentdown: '上拉加载更多' }"
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
  import CpsRebateApi from '@/sheep/api/cps/rebate';

  const state = reactive({
    currentTab: 0,
    loadStatus: '',
    account: {
      availableBalance: 0,
      pendingRebate: 0,
      frozenBalance: 0,
      debtBalance: 0,
      withdrawableBalance: 0,
      exchangeableBalance: 0,
    },
    debtSummary: {
      outstandingDebtCent: 0,
      repaidDebtCent: 0,
      waivedDebtCent: 0,
    },
    pagination: {
      list: [],
      total: 0,
      pageNo: 1,
      pageSize: 8,
    },
  });

  const tabMaps = [
    { name: '返利明细', value: 'records' },
    { name: '偿债流水', value: 'debts' },
  ];

  const accountCards = computed(() => [
    { key: 'pendingRebate', label: '待入账', value: state.account.pendingRebate },
    { key: 'frozenBalance', label: '冻结中', value: state.account.frozenBalance },
    { key: 'debtBalance', label: '欠款', value: state.account.debtBalance },
    { key: 'withdrawableBalance', label: '可提现', value: state.account.withdrawableBalance },
    { key: 'exchangeableBalance', label: '可兑换', value: state.account.exchangeableBalance },
  ]);

  async function getAccount() {
    const { code, data } = await CpsRebateApi.getAccount();
    if (code !== 0) {
      return;
    }
    state.account = data || {};
  }

  async function getDebtSummary() {
    const { code, data } = await CpsRebateApi.getDebtSummary();
    if (code !== 0) {
      return;
    }
    state.debtSummary = data || {};
  }

  async function getRecordPage() {
    if (state.loadStatus === 'loading') {
      return;
    }
    state.loadStatus = 'loading';
    const params = {
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
    };
    const { code, data } = await (state.currentTab === 0
      ? CpsRebateApi.getRecordPage(params)
      : CpsRebateApi.getDebtRepaymentPage(params));
    if (code !== 0) {
      state.loadStatus = 'more';
      return;
    }
    const list = data?.list || [];
    state.pagination.list = concat(state.pagination.list, list);
    state.pagination.total = data?.total || 0;
    state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
  }

  function onTabsChange(e) {
    resetPagination(state.pagination);
    state.currentTab = e.index;
    getRecordPage();
  }

  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo++;
    getRecordPage();
  }

  function formatMoney(value) {
    return toNumber(value).toFixed(2);
  }

  function formatCentMoney(value) {
    return (toNumber(value) / 100).toFixed(2);
  }

  function formatSignedCentMoney(value) {
    const amount = toNumber(value);
    const prefix = amount > 0 ? '+' : '';
    return `${prefix}¥${formatCentMoney(amount)}`;
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

  function rebateTypeText(rebateType) {
    const map = {
      rebate: '返利',
      refund: '退款',
      adjust: '调账',
      exchange: '兑换',
      withdraw: '提现',
    };
    return map[rebateType] || rebateType || '返利';
  }

  function rebateStatusText(rebateStatus) {
    const map = {
      pending: '待入账',
      available: '可用',
      frozen: '冻结中',
      deducted: '已扣减',
      invalid: '已失效',
      refunded: '已退款',
    };
    return map[rebateStatus] || rebateStatus || '待确认';
  }

  function debtBusinessText(businessType) {
    const map = {
      refund: '退款欠款',
      repayment: '返利抵扣',
      waive: '欠款减免',
      adjust: '欠款调账',
      exchange: '兑换扣减',
      withdraw: '提现扣减',
    };
    return map[businessType] || businessType || '偿债流水';
  }

  onLoad(async (options) => {
    if (options.type === 'debt') {
      state.currentTab = 1;
    }
    await Promise.all([getAccount(), getDebtSummary()]);
    getRecordPage();
  });

  onReachBottom(() => {
    loadMore();
  });
</script>

<style lang="scss" scoped>
  .account-panel {
    padding: 24rpx;
    background: $bg-page;
  }

  .balance-card {
    min-width: 0;
    padding: 22rpx 20rpx;
    background: #ffffff;
    border-radius: 10rpx;
  }

  .balance-card.primary {
    padding: 34rpx 28rpx;
    background: var(--ui-BG-Main);
  }

  .balance-card.primary .balance-label,
  .balance-card.primary .balance-value {
    color: #ffffff;
  }

  .balance-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 16rpx;
    margin-top: 16rpx;
  }

  .balance-label,
  .summary-label {
    font-size: 22rpx;
    color: #888888;
    line-height: 1.4;
  }

  .balance-value {
    margin-top: 12rpx;
    font-size: 42rpx;
    font-weight: 600;
    color: #222222;
    line-height: 1.2;
    word-break: break-all;
  }

  .balance-value.small {
    font-size: 28rpx;
    color: var(--ui-BG-Main);
  }

  .debt-summary {
    display: flex;
    justify-content: space-between;
    gap: 18rpx;
    margin-top: 16rpx;
    padding: 22rpx 20rpx;
    background: #ffffff;
    border-radius: 10rpx;
  }

  .debt-summary > view {
    flex: 1;
    min-width: 0;
  }

  .summary-value {
    margin-top: 10rpx;
    font-size: 26rpx;
    font-weight: 600;
    color: #333333;
    word-break: break-all;
  }

  .summary-value.debt {
    color: #fa3534;
  }

  .record-list {
    padding: 20rpx 20rpx 40rpx;
  }

  .record-card {
    margin-bottom: 20rpx;
    padding: 24rpx;
    background: #ffffff;
    border-radius: 10rpx;
  }

  .record-head {
    gap: 20rpx;
  }

  .record-title {
    flex: 1;
    min-width: 0;
    font-size: 28rpx;
    font-weight: 600;
    color: #222222;
  }

  .record-amount {
    flex-shrink: 0;
    font-size: 28rpx;
    font-weight: 600;
    color: #fa3534;
  }

  .record-amount.debt {
    color: #333333;
  }

  .record-meta {
    margin-top: 16rpx;
    font-size: 24rpx;
    color: #666666;
  }

  .record-foot {
    margin-top: 18rpx;
    padding-top: 16rpx;
    border-top: 1rpx solid #eeeeee;
    font-size: 22rpx;
    color: #999999;
    gap: 20rpx;
  }
</style>
