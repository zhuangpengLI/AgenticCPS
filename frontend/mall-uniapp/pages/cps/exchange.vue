<template>
  <s-layout title="返利兑换 Token" navbar="inner">
    <view class="page">
      <view class="balance-card">
        <text class="label">可兑换返利</text>
        <view
          ><text class="currency">¥</text
          ><text class="balance">{{ money(state.exchangeableBalance) }}</text></view
        >
        <text class="hint">仅可用且允许兑换的返利可用于兑换</text>
      </view>

      <view class="form-card">
        <text class="section-title">兑换金额</text>
        <view class="amount-input">
          <text>¥</text>
          <input
            v-model="state.amount"
            type="digit"
            maxlength="12"
            placeholder="最低 0.01 元"
            @input="onAmountInput"
          />
          <text class="all" @tap="useAll">全部</text>
        </view>
        <text v-if="amountError" class="error">{{ amountError }}</text>
        <button
          class="preview-button"
          :disabled="state.previewing || !!amountError"
          @tap="previewExchange"
        >
          {{ state.previewing ? '预估中...' : '预估可得 Token' }}
        </button>
      </view>

      <view v-if="state.preview" class="preview-card">
        <view class="preview-row"
          ><text>兑换返利</text><text>¥{{ money(state.preview.sourceAmount) }}</text></view
        >
        <view class="preview-row"
          ><text>兑换比例</text
          ><text>1 元 ≈ {{ state.preview.exchangeRate || 0 }} Token</text></view
        >
        <view v-if="Number(state.preview.fee) > 0" class="preview-row"
          ><text>服务费用</text><text>¥{{ money(state.preview.fee) }}</text></view
        >
        <view v-if="Number(state.preview.bonusTokens) > 0" class="preview-row bonus"
          ><text>活动加赠</text><text>+{{ state.preview.bonusTokens }} Token</text></view
        >
        <view class="preview-total"
          ><text>预计到账</text
          ><text
            >{{ state.preview.actualTokens ?? state.preview.targetTokens ?? 0 }} Token</text
          ></view
        >
        <button class="submit-button" :disabled="state.submitting" @tap="submitExchange">
          {{ state.submitting ? '提交中...' : '确认兑换' }}
        </button>
      </view>

      <view v-if="state.order" class="order-card">
        <view class="order-head">
          <text class="section-title">兑换进度</text>
          <text class="status" :class="statusClass(state.order.status)">{{
            statusText(state.order.status)
          }}</text>
        </view>
        <view class="order-row"
          ><text>兑换单号</text><text selectable>{{ state.order.exchangeOrderNo }}</text></view
        >
        <view class="order-row"
          ><text>兑换金额</text><text>¥{{ money(state.order.sourceAmount) }}</text></view
        >
        <view class="order-row"
          ><text>Token 数量</text><text>{{ state.order.targetTokens || 0 }}</text></view
        >
        <text v-if="state.order.failureReason" class="failure">{{
          state.order.failureReason
        }}</text>
        <text v-if="state.polling" class="polling"
          >正在确认到账状态（{{ state.pollCount }}/{{ maxPollCount }}）...</text
        >
        <button v-else-if="isPending(state.order.status)" class="retry-status" @tap="startPolling"
          >刷新状态</button
        >
      </view>

      <view class="rules">
        <text class="section-title">兑换说明</text>
        <text>1. 兑换提交后会先冻结相应返利，处理中请勿重复提交。</text>
        <text>2. 兑换成功后 Token 到账；失败时系统会按处理结果退回返利。</text>
        <text>3. 网络超时重试会沿用同一请求标识，不会重复扣减。</text>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import { onLoad, onUnload } from '@dcloudio/uni-app';
  import sheep from '@/sheep';
  import CpsRebateApi from '@/sheep/api/cps/rebate';
  import CpsExchangeApi from '@/sheep/api/cps/exchange';

  const maxPollCount = 6;
  let pollTimer = null;
  const state = reactive({
    exchangeableBalance: 0,
    amount: '',
    preview: null,
    order: null,
    idempotencyKey: '',
    previewing: false,
    submitting: false,
    polling: false,
    pollCount: 0,
  });

  const parsedAmount = computed(() => Number(state.amount));
  const amountError = computed(() => {
    if (!state.amount) return '请输入兑换金额';
    if (!Number.isFinite(parsedAmount.value) || parsedAmount.value < 0.01)
      return '兑换金额不能低于 0.01 元';
    if (!/^\d+(\.\d{0,2})?$/.test(state.amount)) return '金额最多保留两位小数';
    if (parsedAmount.value > Number(state.exchangeableBalance || 0))
      return '兑换金额不能超过可兑换返利';
    return '';
  });

  const money = (value) => Number(value || 0).toFixed(2);
  const unwrap = (res, fallback) => {
    if (!res || res.code !== 0) throw new Error(res?.msg || fallback);
    return res.data;
  };
  const isPending = (status) => ['INIT', 'FROZEN', 'CREDITED', 'PROCESSING'].includes(status);
  const statusText = (status) =>
    ({
      INIT: '待处理',
      FROZEN: '返利已冻结',
      CREDITED: 'Token 入账中',
      PROCESSING: '处理中',
      SUCCESS: '兑换成功',
      FAILED: '兑换失败',
      ROLLBACK_REQUIRED: '退款处理中',
      CANCELED: '已取消',
    }[status] ||
    status ||
    '未知状态');
  const statusClass = (status) =>
    status === 'SUCCESS'
      ? 'success'
      : isPending(status) || status === 'ROLLBACK_REQUIRED'
      ? 'pending'
      : 'failed';

  async function loadAccount() {
    try {
      const res = await CpsRebateApi.getAccount();
      const account = unwrap(res, '可兑换余额加载失败') || {};
      state.exchangeableBalance = Number(account.exchangeableBalance || 0);
    } catch (error) {
      sheep.$helper.toast(error?.msg || error?.message || '可兑换余额加载失败');
    }
  }

  function onAmountInput() {
    state.preview = null;
    state.order = null;
    state.idempotencyKey = '';
    stopPolling();
  }

  function useAll() {
    state.amount = money(state.exchangeableBalance);
    onAmountInput();
  }

  async function previewExchange() {
    if (amountError.value || state.previewing) return;
    state.previewing = true;
    try {
      const res = await CpsExchangeApi.preview(money(parsedAmount.value));
      state.preview = unwrap(res, '兑换预估失败');
      if (!state.preview) throw new Error('暂时无法计算兑换结果');
    } catch (error) {
      state.preview = null;
      sheep.$helper.toast(error?.msg || error?.message || '兑换预估失败');
    } finally {
      state.previewing = false;
    }
  }

  async function submitExchange() {
    if (amountError.value || !state.preview || state.submitting) return;
    if (!state.idempotencyKey) state.idempotencyKey = createIdempotencyKey();
    state.submitting = true;
    try {
      const res = await CpsExchangeApi.submit({
        amount: money(parsedAmount.value),
        idempotencyKey: state.idempotencyKey,
      });
      state.order = unwrap(res, '兑换提交失败');
      if (!state.order?.exchangeOrderNo) throw new Error('兑换单创建失败，请使用原请求重试');
      if (state.order.status === 'SUCCESS') finishSuccess();
      else if (isPending(state.order.status)) startPolling();
      else showOrderFailure();
    } catch (error) {
      sheep.$helper.toast(error?.msg || error?.message || '提交结果未知，请勿修改金额并重试');
    } finally {
      state.submitting = false;
    }
  }

  function createIdempotencyKey() {
    return `cps-ex-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;
  }

  function startPolling() {
    if (!state.order?.exchangeOrderNo || state.polling) return;
    stopPolling();
    state.polling = true;
    state.pollCount = 0;
    pollStatus();
  }

  async function pollStatus() {
    if (!state.polling || !state.order?.exchangeOrderNo) return;
    state.pollCount += 1;
    try {
      const res = await CpsExchangeApi.getStatus(state.order.exchangeOrderNo);
      state.order = unwrap(res, '兑换状态查询失败') || state.order;
      if (state.order.status === 'SUCCESS') {
        finishSuccess();
        return;
      }
      if (!isPending(state.order.status)) {
        stopPolling();
        showOrderFailure();
        return;
      }
    } catch (error) {
      if (state.pollCount >= maxPollCount) sheep.$helper.toast('状态查询暂时失败，可稍后手动刷新');
    }
    if (state.pollCount >= maxPollCount) {
      stopPolling();
      return;
    }
    pollTimer = setTimeout(pollStatus, 2000);
  }

  function finishSuccess() {
    stopPolling();
    state.idempotencyKey = '';
    state.preview = null;
    sheep.$helper.toast('兑换成功');
    loadAccount();
  }

  function showOrderFailure() {
    const message =
      state.order?.failureReason ||
      (state.order?.status === 'ROLLBACK_REQUIRED'
        ? '返利退回处理中，请稍后查看'
        : '兑换未成功，请稍后重试');
    sheep.$helper.toast(message);
  }

  function stopPolling() {
    state.polling = false;
    if (pollTimer) clearTimeout(pollTimer);
    pollTimer = null;
  }

  onLoad(loadAccount);
  onUnload(stopPolling);
</script>

<style lang="scss" scoped>
  .page {
    min-height: 100vh;
    padding: 24rpx;
    background: #f6f7fb;
    box-sizing: border-box;
  }
  .balance-card {
    padding: 38rpx 30rpx;
    border-radius: 24rpx;
    color: #fff;
    background: linear-gradient(135deg, #ff6b35, #ff3d63);
  }
  .label,
  .hint {
    display: block;
  }
  .label {
    font-size: 25rpx;
    opacity: 0.9;
  }
  .currency {
    font-size: 30rpx;
  }
  .balance {
    margin-left: 6rpx;
    font-size: 58rpx;
    font-weight: 700;
  }
  .hint {
    margin-top: 10rpx;
    font-size: 21rpx;
    opacity: 0.8;
  }
  .form-card,
  .preview-card,
  .order-card,
  .rules {
    margin-top: 22rpx;
    padding: 28rpx;
    border-radius: 20rpx;
    background: #fff;
  }
  .section-title {
    color: #292b34;
    font-size: 29rpx;
    font-weight: 600;
  }
  .amount-input {
    display: flex;
    align-items: center;
    gap: 14rpx;
    margin-top: 24rpx;
    padding: 18rpx 0;
    border-bottom: 1rpx solid #eee;
    color: #282a32;
    font-size: 38rpx;
  }
  .amount-input input {
    min-width: 0;
    flex: 1;
    height: 60rpx;
    font-size: 38rpx;
  }
  .all {
    color: #ff4d4f;
    font-size: 24rpx;
  }
  .error {
    display: block;
    margin-top: 12rpx;
    color: #e84d4f;
    font-size: 22rpx;
  }
  .preview-button,
  .submit-button,
  .retry-status {
    margin-top: 26rpx;
    height: 78rpx;
    line-height: 78rpx;
    border-radius: 39rpx;
    color: #fff;
    background: linear-gradient(90deg, #ff6b35, #ff3d63);
    font-size: 27rpx;
  }
  button[disabled] {
    opacity: 0.55;
  }
  .preview-row,
  .preview-total,
  .order-head,
  .order-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .preview-row {
    padding: 14rpx 0;
    color: #747781;
    font-size: 24rpx;
  }
  .bonus {
    color: #ff704c;
  }
  .preview-total {
    margin-top: 14rpx;
    padding-top: 22rpx;
    border-top: 1rpx solid #eee;
    color: #ff3d52;
    font-size: 31rpx;
    font-weight: 600;
  }
  .order-head {
    margin-bottom: 16rpx;
  }
  .status {
    font-size: 24rpx;
  }
  .status.success {
    color: #24a56a;
  }
  .status.pending {
    color: #e59221;
  }
  .status.failed {
    color: #d94b4b;
  }
  .order-row {
    padding: 12rpx 0;
    color: #747781;
    font-size: 23rpx;
  }
  .failure,
  .polling {
    display: block;
    margin-top: 16rpx;
    padding: 16rpx;
    border-radius: 10rpx;
    font-size: 22rpx;
  }
  .failure {
    color: #c84343;
    background: #fff1f0;
  }
  .polling {
    color: #9a6b20;
    background: #fff8e8;
  }
  .retry-status {
    color: #ff4d4f;
    background: #fff;
    border: 1rpx solid #ff8a79;
  }
  .rules text:not(.section-title) {
    display: block;
    margin-top: 15rpx;
    color: #898c95;
    font-size: 22rpx;
    line-height: 1.6;
  }
</style>
