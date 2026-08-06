<template>
  <s-layout title="返利提现" navbar="inner">
    <view class="balance-panel ui-BG-Main-Gradient">
      <view class="balance-label">可提现返利</view>
      <view class="balance-value">¥{{ formatMoney(state.account.withdrawableBalance) }}</view>
      <view class="balance-sub"
        >冻结 ¥{{ formatMoney(state.account.frozenBalance)
        }}<text v-if="toNumber(state.account.debtBalance) > 0"
          >· 欠款 ¥{{ formatMoney(state.account.debtBalance) }}</text
        ></view
      >
    </view>

    <view class="draw-card">
      <view class="section-title">提现金额</view>
      <view class="amount-input"
        ><text>¥</text><input v-model="state.form.amountYuan" type="digit" placeholder="0.00"
      /></view>
      <button class="ss-reset-button all-button" @tap="fillAll">全部提现</button>
      <view class="section-title method-title">收款方式</view>
      <view class="method-row">
        <button
          v-for="item in withdrawTypes"
          :key="item.value"
          class="ss-reset-button method-button"
          :class="{ active: state.form.withdrawType === item.value }"
          @tap="state.form.withdrawType = item.value"
          >{{ item.label }}</button
        >
      </view>
      <uni-easyinput v-model="state.form.withdrawAccount" placeholder="请输入收款账号" />
      <uni-easyinput v-model="state.form.withdrawAccountName" placeholder="请输入收款姓名" />
      <view class="form-tip">提交后将进入审核，请确保收款信息准确。</view>
      <button
        class="ss-reset-button submit-button ui-BG-Main-Gradient"
        :disabled="state.submitting"
        @tap="onConfirm"
        >{{ state.submitting ? '提交中...' : '确认提现' }}</button
      >
    </view>

    <view class="record-section">
      <view class="record-head"
        ><view class="section-title">提现记录</view
        ><text>共 {{ state.pagination.total }} 笔</text></view
      >
      <view
        v-for="item in state.pagination.list"
        :key="item.id"
        class="withdraw-row"
        @tap="getWithdraw(item.id)"
      >
        <view
          ><strong>{{ item.withdrawNo || '提现申请' }}</strong
          ><text
            >{{ withdrawTypeText(item.withdrawType) }} · {{ formatTime(item.createTime) }}</text
          ></view
        >
        <view class="withdraw-side"
          ><strong>¥{{ formatCentMoney(item.amountCent) }}</strong
          ><text>{{ withdrawStatusText(item.status) }}</text></view
        >
      </view>
      <view v-if="state.errorMessage && !state.pagination.list.length" class="empty-state"
        ><text>{{ state.errorMessage }}</text
        ><button class="ss-reset-button retry-button" @tap="retryRecords">重试</button></view
      >
      <s-empty
        v-else-if="state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/data-empty.png"
        text="暂无提现记录"
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
      <view v-if="state.currentWithdraw" class="detail-panel">
        <view class="detail-title">提现详情</view>
        <view class="detail-item"
          ><text>提现单号</text><text>{{ state.currentWithdraw.withdrawNo || '-' }}</text></view
        >
        <view class="detail-item"
          ><text>提现金额</text
          ><text>¥{{ formatCentMoney(state.currentWithdraw.amountCent) }}</text></view
        >
        <view class="detail-item"
          ><text>申请状态</text
          ><text>{{ withdrawStatusText(state.currentWithdraw.status) }}</text></view
        >
        <view class="detail-item"
          ><text>打款状态</text
          ><text>{{ transferStatusText(state.currentWithdraw.transferStatus) }}</text></view
        >
        <view class="detail-item"
          ><text>审核备注</text><text>{{ state.currentWithdraw.reviewNote || '-' }}</text></view
        >
        <view class="detail-item"
          ><text>打款时间</text
          ><text>{{ formatTime(state.currentWithdraw.transferTime) }}</text></view
        >
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
  import {
    createCpsIdempotencyKey,
    formatCentMoney,
    formatMoney,
    toNumber,
  } from '@/sheep/helper/cps';
  import CpsRebateApi from '@/sheep/api/cps/rebate';
  import CpsWithdrawApi from '@/sheep/api/cps/withdraw';
  import { trackCpsEvent } from '@/sheep/helper/cpsAnalytics';

  const state = reactive({
    account: { withdrawableBalance: 0, frozenBalance: 0, debtBalance: 0 },
    form: { amountYuan: '', withdrawType: 'alipay', withdrawAccount: '', withdrawAccountName: '' },
    withdrawIdempotencyKey: '',
    submitting: false,
    loadStatus: '',
    errorMessage: '',
    showDetailPopup: false,
    currentWithdraw: null,
    pagination: { list: [], total: 0, pageNo: 1, pageSize: 8 },
  });
  const withdrawTypes = [
    { label: '支付宝', value: 'alipay' },
    { label: '微信', value: 'wechat' },
  ];

  async function getAccount() {
    const { code, data } = await CpsRebateApi.getAccount();
    if (code === 0) state.account = data || {};
  }
  async function getWithdrawPage() {
    if (state.loadStatus === 'loading') return;
    state.loadStatus = 'loading';
    state.errorMessage = '';
    try {
      const { code, data } = await CpsWithdrawApi.getWithdrawPage({
        pageNo: state.pagination.pageNo,
        pageSize: state.pagination.pageSize,
      });
      if (code !== 0) {
        state.errorMessage = '提现记录加载失败';
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
  async function getWithdraw(id) {
    const { code, data } = await CpsWithdrawApi.getWithdraw(id);
    if (code === 0) {
      state.currentWithdraw = data;
      state.showDetailPopup = true;
    }
  }
  function ensureWithdrawIdempotencyKey() {
    if (!state.withdrawIdempotencyKey)
      state.withdrawIdempotencyKey = createCpsIdempotencyKey('cps-withdraw');
  }
  function resetWithdrawIdempotencyKey() {
    state.withdrawIdempotencyKey = '';
  }

  async function onConfirm() {
    const amountCent = Math.round(toNumber(state.form.amountYuan) * 100);
    if (
      amountCent <= 0 ||
      amountCent > Math.round(toNumber(state.account.withdrawableBalance) * 100)
    ) {
      sheep.$helper.toast('提现金额必须大于 0 且不能超过可提现余额');
      return;
    }
    if (!state.form.withdrawType) {
      sheep.$helper.toast('请选择提现方式');
      return;
    }
    if (!state.form.withdrawAccount.trim()) {
      sheep.$helper.toast('请输入收款账号');
      return;
    }
    if (state.submitting) return;
    ensureWithdrawIdempotencyKey();
    state.submitting = true;
    try {
      const { code } = await CpsWithdrawApi.createWithdraw({
        amountCent,
        withdrawType: state.form.withdrawType,
        withdrawAccount: state.form.withdrawAccount.trim(),
        withdrawAccountName: state.form.withdrawAccountName.trim(),
        idempotencyKey: state.withdrawIdempotencyKey,
      });
      if (code !== 0) {
        trackCpsEvent('cps_withdraw_submit', {
          withdrawType: state.form.withdrawType,
          result: 'failed',
        });
        return;
      }
      trackCpsEvent('cps_withdraw_submit', {
        withdrawType: state.form.withdrawType,
        result: 'success',
      });
      sheep.$helper.toast('提现申请已提交');
      resetWithdrawIdempotencyKey();
      state.form.amountYuan = '';
      state.form.withdrawAccount = '';
      state.form.withdrawAccountName = '';
      resetPagination(state.pagination);
      await Promise.all([getAccount(), getWithdrawPage()]);
    } catch (error) {
      trackCpsEvent('cps_withdraw_submit', {
        withdrawType: state.form.withdrawType,
        result: 'network_error',
      });
      sheep.$helper.toast('提交状态未知，请保持表单不变后重试');
    } finally {
      state.submitting = false;
    }
  }

  watch(
    () => [
      state.form.amountYuan,
      state.form.withdrawType,
      state.form.withdrawAccount,
      state.form.withdrawAccountName,
    ],
    resetWithdrawIdempotencyKey,
  );
  function fillAll() {
    state.form.amountYuan = formatMoney(state.account.withdrawableBalance);
  }
  function retryRecords() {
    resetPagination(state.pagination);
    getWithdrawPage();
  }
  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') return;
    state.pagination.pageNo++;
    getWithdrawPage();
  }
  function withdrawTypeText(value) {
    return { alipay: '支付宝', wechat: '微信' }[value] || value || '未知方式';
  }
  function withdrawStatusText(value) {
    return (
      {
        created: '待审核',
        reviewing: '打款中',
        success: '提现成功',
        failed: '提现失败',
        rejected: '已驳回',
        closed: '已关闭',
      }[value] ||
      value ||
      '处理中'
    );
  }
  function transferStatusText(value) {
    return (
      {
        none: '未打款',
        pending: '打款中',
        success: '打款成功',
        failed: '打款失败',
        closed: '已关闭',
      }[value] ||
      value ||
      '未打款'
    );
  }
  function formatTime(value) {
    return value ? sheep.$helper.timeFormat(value, 'yyyy-mm-dd hh:MM') : '-';
  }

  onLoad(() => {
    getAccount();
    getWithdrawPage();
  });
  onReachBottom(loadMore);
</script>

<style lang="scss" scoped>
  .balance-panel {
    padding: 38rpx 30rpx 76rpx;
    color: #fff;
  }
  .balance-label {
    font-size: 25rpx;
  }
  .balance-value {
    margin-top: 16rpx;
    font-size: 58rpx;
    font-weight: 700;
    overflow-wrap: anywhere;
  }
  .balance-sub {
    margin-top: 15rpx;
    font-size: 22rpx;
    opacity: 0.88;
  }
  .balance-sub text {
    margin-left: 14rpx;
  }
  .draw-card,
  .record-section {
    margin: -42rpx 24rpx 24rpx;
    padding: 28rpx;
    border-radius: 14rpx;
    background: #fff;
  }
  .record-section {
    margin-top: 0;
  }
  .section-title {
    color: #222;
    font-size: 28rpx;
    font-weight: 600;
  }
  .amount-input {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin-top: 20rpx;
    padding-bottom: 18rpx;
    border-bottom: 1rpx solid #ddd;
  }
  .amount-input text {
    color: #333;
    font-size: 42rpx;
  }
  .amount-input input {
    flex: 1;
    min-width: 0;
    height: 70rpx;
    font-size: 44rpx;
  }
  .all-button {
    margin: 14rpx 0 0 auto;
    color: var(--ui-BG-Main);
    font-size: 23rpx;
  }
  .method-title {
    margin-top: 28rpx;
  }
  .method-row {
    display: flex;
    gap: 16rpx;
    margin: 18rpx 0 22rpx;
  }
  .method-button {
    flex: 1;
    height: 68rpx;
    line-height: 68rpx;
    border: 1rpx solid #ddd;
    border-radius: 8rpx;
    color: #555;
    font-size: 25rpx;
  }
  .method-button.active {
    color: var(--ui-BG-Main);
    border-color: var(--ui-BG-Main);
    background: rgba(var(--ui-BG-Main-rgb), 0.08);
  }
  .draw-card :deep(.uni-easyinput) {
    margin-top: 16rpx;
  }
  .form-tip {
    margin-top: 18rpx;
    color: #999;
    font-size: 21rpx;
    line-height: 1.5;
  }
  .submit-button {
    width: 100%;
    height: 78rpx;
    line-height: 78rpx;
    margin-top: 28rpx;
    border-radius: 39rpx;
    color: #fff;
  }
  .record-head,
  .withdraw-row,
  .detail-item {
    display: flex;
    justify-content: space-between;
    gap: 24rpx;
  }
  .record-head {
    align-items: center;
    margin-bottom: 8rpx;
    color: #999;
    font-size: 22rpx;
  }
  .withdraw-row {
    padding: 22rpx 0;
    border-bottom: 1rpx solid #eee;
  }
  .withdraw-row > view {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 10rpx;
  }
  .withdraw-row strong {
    color: #333;
    font-size: 25rpx;
    overflow-wrap: anywhere;
  }
  .withdraw-row text {
    color: #999;
    font-size: 21rpx;
  }
  .withdraw-side {
    flex-shrink: 0;
    text-align: right;
  }
  .withdraw-side strong {
    color: #fa3534;
    font-size: 28rpx;
  }
  .detail-panel {
    max-height: 78vh;
    padding: 32rpx 28rpx 54rpx;
    overflow-y: auto;
    background: #fff;
  }
  .detail-title {
    margin-bottom: 20rpx;
    color: #222;
    font-size: 34rpx;
    font-weight: 600;
  }
  .detail-item {
    padding: 21rpx 0;
    border-bottom: 1rpx solid #eee;
    color: #777;
    font-size: 25rpx;
  }
  .detail-item text:last-child {
    max-width: 440rpx;
    color: #222;
    text-align: right;
    overflow-wrap: anywhere;
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
  button[disabled] {
    opacity: 0.55;
  }
</style>
