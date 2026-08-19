<template>
  <view class="rebate-assets" :style="panelStyle">
    <view class="section-head">
      <text class="section-title">{{ content.title }}</text>
      <text v-if="isLogin" class="more" @tap="goWallet">明细 ›</text>
    </view>

    <view v-if="!isLogin" class="state guest" @tap="requestLogin">
      <text class="state-title">{{ content.guestText }}</text>
      <text class="state-action">立即登录</text>
    </view>
    <view v-else-if="state.loading" class="state loading">
      <text>返利资产加载中...</text>
    </view>
    <view v-else-if="state.error" class="state error">
      <text>{{ state.error }}</text>
      <text class="state-action" @tap="loadAccount">重新加载</text>
    </view>
    <view v-else-if="!state.account" class="state empty">
      <text>暂无返利资产</text>
    </view>
    <view v-else class="asset-grid">
      <view v-if="content.showAvailable" class="asset-item main" @tap="goWallet">
        <text class="asset-label">可用返利</text>
        <text class="asset-value">¥{{ formatMoney(state.account.availableBalance) }}</text>
      </view>
      <view v-if="content.showFrozen" class="asset-item" @tap="goWallet">
        <text class="asset-label">冻结中</text>
        <text class="asset-value">¥{{ formatMoney(state.account.frozenBalance) }}</text>
      </view>
      <view v-if="content.showTotal" class="asset-item" @tap="goWallet">
        <text class="asset-label">累计返利</text>
        <text class="asset-value">¥{{ formatMoney(state.account.totalRebate) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { computed, onMounted, reactive, watch } from 'vue';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  import { formatMoney } from '@/sheep/helper/cps';
  import CpsRebateApi from '@/sheep/api/cps/rebate';

  const props = defineProps({
    data: { type: Object, default: () => ({}) },
    styles: { type: Object, default: () => ({}) },
  });
  const state = reactive({ loading: false, error: '', account: null });
  let loadVersion = 0;
  const isLogin = computed(() => sheep.$store('user').isLogin);
  const content = computed(() => ({
    title: props.data.title || '我的返利',
    showAvailable: props.data.showAvailable !== false,
    showFrozen: props.data.showFrozen !== false,
    showTotal: props.data.showTotal !== false,
    guestText: props.data.guestText || '登录后查看返利资产',
  }));
  const panelStyle = computed(() => ({
    borderRadius: `${Number(props.styles.borderRadius || 0)}px`,
  }));

  async function loadAccount() {
    const version = ++loadVersion;
    if (!isLogin.value) {
      state.account = null;
      state.error = '';
      state.loading = false;
      return;
    }
    state.loading = true;
    state.error = '';
    try {
      const res = await CpsRebateApi.getAccount();
      if (!res || res.code !== 0) throw new Error(res?.msg || '返利资产加载失败');
      if (version !== loadVersion || !isLogin.value) return;
      state.account = res.data || null;
    } catch (error) {
      if (version !== loadVersion) return;
      state.account = null;
      state.error = error?.msg || error?.message || '返利资产加载失败，请稍后重试';
    } finally {
      if (version === loadVersion) state.loading = false;
    }
  }

  function requestLogin() {
    showAuthModal();
  }
  function goWallet() {
    if (!isLogin.value) {
      showAuthModal();
      return;
    }
    sheep.$router.go('/pages/cps/wallet');
  }

  onMounted(loadAccount);
  watch(isLogin, loadAccount);
</script>

<style lang="scss" scoped>
  .rebate-assets {
    box-sizing: border-box;
    overflow: hidden;
  }
  .section-head {
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
    min-height: 132rpx;
    margin-top: 20rpx;
    padding: 22rpx;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    border-radius: 14rpx;
    color: #8b8e97;
    background: #f7f8fa;
    font-size: 24rpx;
    text-align: center;
  }
  .guest {
    background: #fff6ef;
  }
  .state-title {
    color: #5f626b;
  }
  .state-action {
    margin-top: 12rpx;
    color: #e6533c;
    font-weight: 600;
  }
  .asset-grid {
    display: flex;
    gap: 14rpx;
    margin-top: 20rpx;
  }
  .asset-item {
    display: flex;
    min-width: 0;
    min-height: 132rpx;
    padding: 20rpx 12rpx;
    flex: 1;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    border: 1rpx solid #eceef2;
    border-radius: 14rpx;
    background: #fafbfc;
  }
  .asset-item.main {
    border-color: #ffd8c9;
    background: #fff4ee;
  }
  .asset-label {
    color: #7b7e87;
    font-size: 22rpx;
  }
  .asset-value {
    max-width: 100%;
    margin-top: 10rpx;
    overflow: hidden;
    color: #24262d;
    font-size: 28rpx;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .main .asset-value {
    color: #e6533c;
  }
</style>
