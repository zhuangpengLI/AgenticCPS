<template>
  <view class="rebate-menu" :style="panelStyle">
    <text class="section-title">{{ content.title }}</text>
    <view v-if="menuItems.length" class="menu-grid">
      <view
        v-for="item in menuItems"
        :key="item.key"
        class="menu-item"
        :style="itemStyle"
        @tap="openMenu(item.key)"
      >
        <view class="menu-icon" :class="`menu-icon--${item.key}`">{{ iconText[item.key] }}</view>
        <text class="menu-title">{{ item.title }}</text>
      </view>
    </view>
    <view v-else class="empty">暂无可用返利服务</view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';

  const props = defineProps({
    data: { type: Object, default: () => ({}) },
    styles: { type: Object, default: () => ({}) },
  });

  const MENU_ROUTES = Object.freeze({
    goods: { path: '/pages/cps/goods', requiresAuth: false },
    orders: { path: '/pages/cps/order', requiresAuth: true },
    wallet: { path: '/pages/cps/wallet', requiresAuth: true },
    withdraw: { path: '/pages/cps/withdraw', requiresAuth: true },
    exchange: { path: '/pages/cps/exchange', requiresAuth: true },
    transfer: { path: '/pages/cps/transfer', requiresAuth: true },
    aiChat: { path: '/pages/cps/ai-chat', requiresAuth: true },
    activities: { path: '/pages/cps/activity', requiresAuth: false },
    selection: { path: '/pages/cps/deals', requiresAuth: false },
    history: { path: '/pages/cps/history', requiresAuth: true },
    favorites: { path: '/pages/cps/favorites', requiresAuth: true },
    aiot: { path: '/pages/cps/scene', requiresAuth: false },
  });
  const defaultItems = [
    { key: 'goods', title: '商品查询', enabled: true },
    { key: 'orders', title: '返利订单', enabled: true },
    { key: 'wallet', title: '返利钱包', enabled: true },
    { key: 'withdraw', title: '申请提现', enabled: true },
    { key: 'exchange', title: '兑换 Token', enabled: true },
    { key: 'transfer', title: '返利转链', enabled: true },
    { key: 'aiChat', title: '返利 AI 对话', enabled: true },
    { key: 'activities', title: '返利活动', enabled: true },
    { key: 'selection', title: '主题好价', enabled: true },
    { key: 'history', title: '返利足迹', enabled: true },
  ];
  const iconText = Object.freeze({
    goods: '券',
    orders: '单',
    wallet: '返',
    withdraw: '提',
    exchange: 'T',
    activities: '活',
    selection: '选',
    history: '迹',
    favorites: '藏',
    aiot: '智',
    transfer: '链',
    aiChat: 'AI',
  });
  const content = computed(() => ({
    title: props.data.title || '返利服务',
    columns: Math.min(5, Math.max(2, Number(props.data.columns) || 4)),
  }));
  const menuItems = computed(() => {
    const source = Array.isArray(props.data.items) ? props.data.items : defaultItems;
    return source
      .filter((item) => item && item.enabled !== false && MENU_ROUTES[item.key])
      .slice(0, 10)
      .map((item) => ({ ...item, title: String(item.title || '').trim() || item.key }));
  });
  const itemStyle = computed(() => ({ width: `${100 / content.value.columns}%` }));
  const panelStyle = computed(() => ({
    borderRadius: `${Number(props.styles.borderRadius || 0)}px`,
  }));

  function openMenu(key) {
    const destination = MENU_ROUTES[key];
    if (!destination) return;
    if (destination.requiresAuth && !sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    sheep.$router.go(destination.path);
  }
</script>

<style lang="scss" scoped>
  .rebate-menu {
    box-sizing: border-box;
    overflow: hidden;
  }
  .section-title {
    display: block;
    color: #24262d;
    font-size: 30rpx;
    font-weight: 650;
  }
  .menu-grid {
    display: flex;
    margin-top: 18rpx;
    flex-wrap: wrap;
  }
  .menu-item {
    display: flex;
    min-width: 0;
    padding: 14rpx 6rpx 18rpx;
    align-items: center;
    flex-direction: column;
    box-sizing: border-box;
  }
  .menu-icon {
    display: flex;
    width: 76rpx;
    height: 76rpx;
    align-items: center;
    justify-content: center;
    border-radius: 22rpx;
    color: #e6533c;
    background: #fff0eb;
    font-size: 28rpx;
    font-weight: 700;
  }
  .menu-icon--wallet,
  .menu-icon--history,
  .menu-icon--aiot {
    color: #2878c8;
    background: #eef6ff;
  }
  .menu-icon--exchange,
  .menu-icon--selection,
  .menu-icon--favorites {
    color: #8b63ba;
    background: #f7f0ff;
  }
  .menu-title {
    max-width: 100%;
    margin-top: 12rpx;
    overflow: hidden;
    color: #555861;
    font-size: 23rpx;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .empty {
    padding: 40rpx 0 24rpx;
    color: #999ca5;
    font-size: 24rpx;
    text-align: center;
  }
</style>
