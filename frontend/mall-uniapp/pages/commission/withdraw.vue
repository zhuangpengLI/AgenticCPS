<template>
  <s-layout title="申请提现" class="withdraw-wrap" navbar="inner">
    <view class="balance-panel">
      <view class="balance-label">可提现返利</view>
      <view class="balance-value">¥{{ formatMoney(state.account.withdrawableBalance) }}</view>
      <view class="balance-sub">
        冻结 ¥{{ formatMoney(state.account.frozenBalance) }} · 欠款 ¥{{
          formatMoney(state.account.debtBalance)
        }}
      </view>
    </view>

    <view class="draw-card">
      <view class="section-title">提现方式</view>
      <view class="method-row">
        <button
          class="ss-reset-button method-btn"
          :class="{ active: state.form.withdrawType === 'alipay' }"
          @tap="state.form.withdrawType = 'alipay'"
        >
          支付宝
        </button>
        <button
          class="ss-reset-button method-btn"
          :class="{ active: state.form.withdrawType === 'wechat' }"
          @tap="state.form.withdrawType = 'wechat'"
        >
          微信
        </button>
      </view>

      <view class="section-title">提现金额</view>
      <view class="input-box ss-flex ss-col-center border-bottom">
        <view class="unit">¥</view>
        <uni-easyinput
          :inputBorder="false"
          class="ss-flex-1 ss-p-l-10"
          v-model="state.form.amountYuan"
          type="digit"
          placeholder="请输入提现金额"
        />
      </view>

      <view class="section-title">收款账号</view>
      <view class="input-box ss-flex ss-col-center border-bottom">
        <uni-easyinput
          :inputBorder="false"
          class="ss-flex-1"
          v-model="state.form.withdrawAccount"
          placeholder="请输入支付宝账号或微信 OpenID"
        />
      </view>

      <view class="section-title">收款姓名</view>
      <view class="input-box ss-flex ss-col-center border-bottom">
        <uni-easyinput
          :inputBorder="false"
          class="ss-flex-1"
          v-model="state.form.withdrawAccountName"
          placeholder="请输入收款姓名"
        />
      </view>

      <button class="ss-reset-button save-btn ui-BG-Main-Gradient ui-Shadow-Main" @tap="onConfirm">
        确认提现
      </button>
    </view>

    <view class="record-section">
      <view class="section-head ss-flex ss-row-between ss-col-center">
        <view class="section-title">提现记录</view>
        <view class="record-count">共 {{ state.pagination.total }} 笔</view>
      </view>

      <view
        class="withdraw-card"
        v-for="item in state.pagination.list"
        :key="item.id"
        @tap="getWithdraw(item.id)"
      >
        <view class="withdraw-head ss-flex ss-row-between ss-col-center">
          <view class="withdraw-no ss-line-1">{{ item.withdrawNo || '提现申请' }}</view>
          <view class="withdraw-amount">¥{{ formatCentMoney(item.amountCent) }}</view>
        </view>
        <view class="withdraw-meta">
          {{ withdrawTypeText(item.withdrawType) }} · {{ withdrawStatusText(item.status) }}
        </view>
        <view class="withdraw-foot ss-flex ss-row-between ss-col-center">
          <text>{{ formatTime(item.createTime) }}</text>
          <text>{{ transferStatusText(item.transferStatus) }}</text>
        </view>
      </view>

      <s-empty
        v-if="state.pagination.total === 0 && state.loadStatus !== 'loading'"
        icon="/static/data-empty.png"
        text="暂无提现记录"
      />

      <uni-load-more
        v-if="state.pagination.total > 0"
        :status="state.loadStatus"
        :content-text="{ contentdown: '上拉加载更多' }"
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
      <view class="detail-panel" v-if="state.currentWithdraw">
        <view class="detail-title">提现详情</view>
        <view class="detail-list">
          <view class="detail-item">
            <text>提现单号</text>
            <text class="value">{{ state.currentWithdraw.withdrawNo || '-' }}</text>
          </view>
          <view class="detail-item">
            <text>提现金额</text>
            <text class="value">¥{{ formatCentMoney(state.currentWithdraw.amountCent) }}</text>
          </view>
          <view class="detail-item">
            <text>提现方式</text>
            <text class="value">{{ withdrawTypeText(state.currentWithdraw.withdrawType) }}</text>
          </view>
          <view class="detail-item">
            <text>申请状态</text>
            <text class="value">{{ withdrawStatusText(state.currentWithdraw.status) }}</text>
          </view>
          <view class="detail-item">
            <text>打款状态</text>
            <text class="value">{{
              transferStatusText(state.currentWithdraw.transferStatus)
            }}</text>
          </view>
          <view class="detail-item">
            <text>审核备注</text>
            <text class="value">{{ state.currentWithdraw.reviewNote || '-' }}</text>
          </view>
          <view class="detail-item">
            <text>打款时间</text>
            <text class="value">{{ formatTime(state.currentWithdraw.transferTime) }}</text>
          </view>
          <view class="detail-item">
            <text>申请时间</text>
            <text class="value">{{ formatTime(state.currentWithdraw.createTime) }}</text>
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
  import CpsRebateApi from '@/sheep/api/cps/rebate';
  import CpsWithdrawApi from '@/sheep/api/cps/withdraw';

  const state = reactive({
    account: {
      withdrawableBalance: 0,
      frozenBalance: 0,
      debtBalance: 0,
    },
    form: {
      amountYuan: undefined,
      withdrawType: 'alipay',
      withdrawAccount: '',
      withdrawAccountName: '',
    },
    loadStatus: '',
    showDetailPopup: false,
    currentWithdraw: null,
    pagination: {
      list: [],
      total: 0,
      pageNo: 1,
      pageSize: 8,
    },
  });

  async function getAccount() {
    const { code, data } = await CpsRebateApi.getAccount();
    if (code !== 0) {
      return;
    }
    state.account = data || {};
  }

  async function getWithdrawPage() {
    if (state.loadStatus === 'loading') {
      return;
    }
    state.loadStatus = 'loading';
    const { code, data } = await CpsWithdrawApi.getWithdrawPage({
      pageNo: state.pagination.pageNo,
      pageSize: state.pagination.pageSize,
    });
    if (code !== 0) {
      state.loadStatus = 'more';
      return;
    }
    const list = data?.list || [];
    state.pagination.list = concat(state.pagination.list, list);
    state.pagination.total = data?.total || 0;
    state.loadStatus = state.pagination.list.length < state.pagination.total ? 'more' : 'noMore';
  }

  async function getWithdraw(id) {
    const { code, data } = await CpsWithdrawApi.getWithdraw(id);
    if (code !== 0) {
      return;
    }
    state.currentWithdraw = data;
    state.showDetailPopup = true;
  }

  async function onConfirm() {
    const amountCent = Math.round(toNumber(state.form.amountYuan) * 100);
    if (
      amountCent <= 0 ||
      amountCent > Math.round(toNumber(state.account.withdrawableBalance) * 100)
    ) {
      sheep.$helper.toast('请输入正确的提现金额');
      return;
    }
    if (!state.form.withdrawType) {
      sheep.$helper.toast('请选择提现方式');
      return;
    }
    if (!state.form.withdrawAccount) {
      sheep.$helper.toast('请输入收款账号');
      return;
    }
    const idempotencyKey = createIdempotencyKey();
    const { code } = await CpsWithdrawApi.createWithdraw({
      amountCent,
      withdrawType: state.form.withdrawType,
      withdrawAccount: state.form.withdrawAccount,
      withdrawAccountName: state.form.withdrawAccountName,
      idempotencyKey,
    });
    if (code !== 0) {
      return;
    }
    sheep.$helper.toast('提现申请已提交');
    state.form.amountYuan = undefined;
    state.form.withdrawAccount = '';
    state.form.withdrawAccountName = '';
    resetPagination(state.pagination);
    await Promise.all([getAccount(), getWithdrawPage()]);
  }

  function loadMore() {
    if (state.loadStatus === 'loading' || state.loadStatus === 'noMore') {
      return;
    }
    state.pagination.pageNo++;
    getWithdrawPage();
  }

  function createIdempotencyKey() {
    return `cps-withdraw:${Date.now()}:${Math.random().toString(36).slice(2, 10)}`;
  }

  function formatMoney(value) {
    return toNumber(value).toFixed(2);
  }

  function formatCentMoney(value) {
    return (toNumber(value) / 100).toFixed(2);
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

  function withdrawTypeText(withdrawType) {
    const map = {
      alipay: '支付宝',
      wechat: '微信',
    };
    return map[withdrawType] || withdrawType || '未知方式';
  }

  function withdrawStatusText(status) {
    const map = {
      created: '待审核',
      reviewing: '打款中',
      success: '提现成功',
      failed: '提现失败',
      rejected: '已驳回',
      closed: '已关闭',
    };
    return map[status] || status || '处理中';
  }

  function transferStatusText(transferStatus) {
    const map = {
      none: '未打款',
      pending: '打款中',
      success: '打款成功',
      failed: '打款失败',
      closed: '已关闭',
    };
    return map[transferStatus] || transferStatus || '未打款';
  }

  onLoad(() => {
    getAccount();
    getWithdrawPage();
  });

  onReachBottom(() => {
    loadMore();
  });
</script>

<style lang="scss" scoped>
  .balance-panel {
    padding: 34rpx 30rpx 76rpx;
    background: var(--ui-BG-Main);
    color: #ffffff;
  }

  .balance-label {
    font-size: 26rpx;
    line-height: 1.4;
  }

  .balance-value {
    margin-top: 18rpx;
    font-size: 58rpx;
    font-weight: 600;
    line-height: 1.15;
    word-break: break-all;
  }

  .balance-sub {
    margin-top: 18rpx;
    font-size: 24rpx;
    opacity: 0.85;
  }

  .draw-card,
  .record-section {
    margin: -44rpx 24rpx 24rpx;
    padding: 28rpx;
    background: #ffffff;
    border-radius: 10rpx;
  }

  .record-section {
    margin-top: 24rpx;
  }

  .section-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #222222;
  }

  .method-row {
    display: flex;
    gap: 18rpx;
    margin: 22rpx 0 32rpx;
  }

  .method-btn {
    flex: 1;
    height: 72rpx;
    line-height: 72rpx;
    border: 1rpx solid #dddddd;
    border-radius: 8rpx;
    font-size: 26rpx;
    color: #333333;
    background: #ffffff;
  }

  .method-btn.active {
    color: var(--ui-BG-Main);
    border-color: var(--ui-BG-Main);
    background: rgba(var(--ui-BG-Main-rgb), 0.08);
  }

  .input-box {
    height: 92rpx;
    margin-bottom: 30rpx;
  }

  .unit {
    font-size: 42rpx;
    color: #333333;
    font-weight: 500;
  }

  .save-btn {
    width: 100%;
    height: 82rpx;
    line-height: 82rpx;
    border-radius: 41rpx;
    margin-top: 42rpx;
    font-size: 28rpx;
    color: #ffffff;
  }

  .section-head {
    margin-bottom: 20rpx;
  }

  .record-count {
    font-size: 24rpx;
    color: #999999;
  }

  .withdraw-card {
    padding: 22rpx 0;
    border-bottom: 1rpx solid #eeeeee;
  }

  .withdraw-head {
    gap: 20rpx;
  }

  .withdraw-no {
    flex: 1;
    min-width: 0;
    font-size: 27rpx;
    font-weight: 600;
    color: #222222;
  }

  .withdraw-amount {
    flex-shrink: 0;
    font-size: 28rpx;
    font-weight: 600;
    color: #fa3534;
  }

  .withdraw-meta {
    margin-top: 14rpx;
    font-size: 24rpx;
    color: #666666;
  }

  .withdraw-foot {
    margin-top: 16rpx;
    font-size: 22rpx;
    color: #999999;
    gap: 20rpx;
  }

  .detail-panel {
    max-height: 78vh;
    padding: 32rpx 28rpx 48rpx;
    overflow-y: auto;
    background: #ffffff;
  }

  .detail-title {
    margin-bottom: 24rpx;
    font-size: 34rpx;
    font-weight: 600;
    color: #222222;
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
