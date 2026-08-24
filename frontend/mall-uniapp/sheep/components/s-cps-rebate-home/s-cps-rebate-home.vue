<template>
  <view class="rebate-home">
    <view class="top-shell">
      <view class="top-row">
        <view class="brand">
          <view class="brand-mark">返</view>
          <view>
            <text class="brand-name">Agentic 返利</text>
            <text class="brand-subtitle">先领券，再下单</text>
          </view>
        </view>
        <view class="compare-search">
          <input
            v-model="keyword"
            class="compare-input"
            :placeholder="content.searchPlaceholder"
            confirm-type="search"
            @confirm="startSearch()"
          />
          <view class="compare-action" @tap="startSearch()">搜</view>
        </view>
      </view>
      <view class="hero-copy">
        <text class="hero-title">全网优惠，一站查清</text>
        <text class="hero-desc">比价 · 查券 · 返利，买之前先搜一搜</text>
      </view>
    </view>

    <view class="transfer-wrap">
      <view class="tutorial-tip">
        <text class="tutorial-badge">新手</text>
        <text>复制商品链接，粘贴后生成你的专属返利链接</text>
        <text class="tutorial-arrow">›</text>
      </view>
      <s-cps-rebate-transfer
        :data="{
          title: '粘贴商品链接',
          description: '支持商品链接、商品 ID 和口令，自动识别平台',
          placeholder: '在这里粘贴商品链接或口令',
          buttonText: '一键查券并转链',
        }"
        :styles="{ borderRadius: 16 }"
      />
    </view>

    <view class="section-card marketplace-section">
      <view class="section-heading">
        <view>
          <text class="section-title">热门商城</text>
          <text class="section-subtitle">多平台同款比价，优惠返利不错过</text>
        </view>
        <text class="section-more" @tap="goPage('/pages/cps/goods')">全部商城 ›</text>
      </view>
      <view class="marketplace-grid">
        <view
          v-for="item in marketplaces"
          :key="item.platformCode"
          class="marketplace-item"
          @tap="goMarketplace(item)"
        >
          <view class="marketplace-logo" :style="{ background: item.background }">
            <text>{{ item.mark }}</text>
          </view>
          <text class="marketplace-name">{{ item.title }}</text>
          <text class="marketplace-tip">{{ item.tip }}</text>
        </view>
      </view>
    </view>

    <view class="section-card local-section">
      <view class="section-heading">
        <view>
          <text class="section-title">生活返利</text>
          <text class="section-subtitle">外卖、出行、住宿和娱乐都能省</text>
        </view>
        <text class="section-more" @tap="goPage('/pages/cps/activity')">更多活动 ›</text>
      </view>
      <view class="local-grid">
        <view
          v-for="item in localServices"
          :key="item.title"
          class="local-item"
          @tap="goLocalService(item)"
        >
          <view class="local-icon" :style="{ color: item.color, background: item.iconBg }">
            {{ item.icon }}
          </view>
          <text class="local-title">{{ item.title }}</text>
          <text class="local-rebate">{{ activityLabel(item) }}</text>
        </view>
      </view>
      <view class="rebate-notice"> 页面展示为活动预估权益，实际返利以平台订单最终结算为准 </view>
    </view>

    <view class="section-card asset-section">
      <view class="section-heading compact-heading">
        <text class="section-title">我的返利</text>
        <text class="section-more" @tap="goPage(content.centerPath)">
          {{ content.centerTitle }} ›
        </text>
      </view>
      <view class="asset-grid">
        <view
          v-for="item in assetItems"
          :key="item.path"
          class="asset-item"
          @tap="goAsset(item.path)"
        >
          <view class="asset-icon">{{ item.icon }}</view>
          <text>{{ item.title }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { computed, onMounted, reactive, ref } from 'vue';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  import CpsMarketingApi from '@/sheep/api/cps/marketing';

  const props = defineProps({
    data: {
      type: Object,
      default: () => ({}),
    },
    styles: {
      type: Object,
      default: () => ({}),
    },
  });

  const keyword = ref('');
  const state = reactive({ activities: [] });
  const content = computed(() => ({
    searchPlaceholder: props.data.searchPlaceholder || props.data.placeholder || '全网比价',
    centerTitle: props.data.centerTitle || '返利中心',
    centerPath: props.data.centerPath || '/pages/cps/index',
  }));

  const marketplaces = [
    {
      mark: '淘',
      title: '淘宝',
      platformCode: 'taobao',
      keyword: '淘宝爆款',
      tip: '领券返利',
      background: 'linear-gradient(145deg, #ff7a24, #ff3d00)',
    },
    {
      mark: '京',
      title: '京东',
      platformCode: 'jd',
      keyword: '京东好物',
      tip: '好价直降',
      background: 'linear-gradient(145deg, #ff4b45, #d7000f)',
    },
    {
      mark: '拼',
      title: '拼多多',
      platformCode: 'pdd',
      keyword: '百亿补贴',
      tip: '精选补贴',
      background: 'linear-gradient(145deg, #ff4b4b, #e02e24)',
    },
    {
      mark: '唯',
      title: '唯品会',
      platformCode: 'vip',
      keyword: '品牌特卖',
      tip: '品牌折扣',
      background: 'linear-gradient(145deg, #ff4f9a, #dd147c)',
    },
    {
      mark: '抖',
      title: '抖音',
      platformCode: 'douyin',
      keyword: '抖音好物',
      tip: '直播好价',
      background: 'linear-gradient(145deg, #25252d, #09090b)',
    },
  ];

  const localServices = [
    {
      icon: '餐',
      title: '外卖红包',
      platformCode: 'meituan',
      keyword: '外卖',
      fallback: '领券再返利',
      color: '#ff8a00',
      iconBg: '#fff3df',
    },
    {
      icon: '车',
      title: '打车返利',
      platformCode: 'didi',
      keyword: '打车',
      fallback: '出行享优惠',
      color: '#ff6a2a',
      iconBg: '#fff0e9',
    },
    {
      icon: '住',
      title: '酒店返利',
      platformCode: 'fliggy',
      keyword: '酒店',
      fallback: '订房也返利',
      color: '#6d5ee8',
      iconBg: '#f0edff',
    },
    {
      icon: '城',
      title: '本地生活',
      platformCode: 'local_life',
      keyword: '本地生活',
      fallback: '吃喝玩乐省',
      color: '#10a36a',
      iconBg: '#e8f8f1',
    },
    {
      icon: '影',
      title: '电影票',
      platformCode: '',
      keyword: '电影票',
      fallback: '购票享返利',
      color: '#f3a600',
      iconBg: '#fff8dd',
    },
    {
      icon: '券',
      title: '限时高返',
      platformCode: '',
      keyword: '高返',
      fallback: '每日精选',
      color: '#ee3e4d',
      iconBg: '#ffedef',
    },
  ];

  const assetItems = [
    { icon: '单', title: '返利订单', path: '/pages/cps/order' },
    { icon: '钱', title: '返利钱包', path: '/pages/cps/wallet' },
    { icon: '提', title: '申请提现', path: '/pages/cps/withdraw' },
    { icon: 'T', title: '兑换 Token', path: '/pages/cps/exchange' },
  ];

  function startSearch(presetKeyword = '') {
    const value = (presetKeyword || keyword.value).trim();
    if (!value) {
      sheep.$router.go('/pages/cps/goods');
      return;
    }
    const looksLikeContent = /https?:\/\/|￥|€|tb\.cn|m\.tb|yangkeduo|jd\.com/i.test(value);
    sheep.$router.go('/pages/cps/goods', {
      [looksLikeContent ? 'content' : 'keyword']: encodeURIComponent(value),
    });
  }

  function goMarketplace(item) {
    sheep.$router.go('/pages/cps/goods', {
      platformCode: encodeURIComponent(item.platformCode),
      keyword: encodeURIComponent(item.keyword),
    });
  }

  function goLocalService(item) {
    const params = { keyword: encodeURIComponent(item.keyword) };
    if (item.platformCode) params.platformCode = encodeURIComponent(item.platformCode);
    sheep.$router.go('/pages/cps/activity', params);
  }

  function activityLabel(item) {
    const match = state.activities.find((activity) => {
      const platformMatched =
        item.platformCode &&
        String(activity.platformCode || '').toLowerCase() === item.platformCode;
      const searchableText = `${activity.activityName || ''} ${activity.activityType || ''} ${
        activity.shortDesc || ''
      }`;
      return platformMatched || searchableText.includes(item.keyword);
    });
    return match?.rebateDesc || match?.tagText || item.fallback;
  }

  async function loadActivityHighlights() {
    try {
      const response = await CpsMarketingApi.getActivityCenter();
      state.activities = response?.code === 0 && Array.isArray(response.data) ? response.data : [];
    } catch (error) {
      state.activities = [];
    }
  }

  function goAsset(path) {
    if (!sheep.$store('user').isLogin) {
      showAuthModal();
      return;
    }
    sheep.$router.go(path);
  }

  function goPage(path) {
    sheep.$router.go(path);
  }

  onMounted(loadActivityHighlights);
</script>

<style lang="scss" scoped>
  .rebate-home {
    box-sizing: border-box;
    min-height: 100vh;
    padding: 0 20rpx 28rpx;
    overflow: hidden;
    color: #24262d;
    background: #f5f6f8;
  }
  .top-shell {
    margin: 0 -20rpx;
    padding: 20rpx 26rpx 58rpx;
    color: #fff;
    background: radial-gradient(circle at 94% 10%, rgba(255, 255, 255, 0.22), transparent 30%),
      linear-gradient(135deg, #10b86d 0%, #00cf73 54%, #00b970 100%);
  }
  .top-row,
  .brand,
  .compare-search,
  .section-heading,
  .asset-grid {
    display: flex;
    align-items: center;
  }
  .top-row,
  .section-heading {
    justify-content: space-between;
  }
  .brand {
    min-width: 0;
    gap: 12rpx;
  }
  .brand-mark {
    display: flex;
    width: 64rpx;
    height: 64rpx;
    flex: 0 0 64rpx;
    align-items: center;
    justify-content: center;
    border: 2rpx solid rgba(255, 255, 255, 0.78);
    border-radius: 18rpx 7rpx 18rpx 7rpx;
    color: #04bd68;
    background: #fff;
    font-size: 32rpx;
    font-weight: 800;
  }
  .brand-name,
  .brand-subtitle,
  .hero-title,
  .hero-desc,
  .section-title,
  .section-subtitle,
  .marketplace-name,
  .marketplace-tip,
  .local-title,
  .local-rebate {
    display: block;
  }
  .brand-name {
    font-size: 29rpx;
    font-weight: 700;
    white-space: nowrap;
  }
  .brand-subtitle {
    margin-top: 3rpx;
    font-size: 19rpx;
    opacity: 0.82;
  }
  .compare-search {
    width: 280rpx;
    height: 68rpx;
    padding: 0 8rpx 0 22rpx;
    border: 3rpx solid rgba(255, 255, 255, 0.56);
    border-radius: 36rpx;
    background: #fff;
    box-sizing: border-box;
  }
  .compare-input {
    min-width: 0;
    height: 64rpx;
    flex: 1;
    color: #2f3137;
    font-size: 25rpx;
  }
  .compare-action {
    display: flex;
    width: 50rpx;
    height: 50rpx;
    flex: 0 0 50rpx;
    align-items: center;
    justify-content: center;
    border-radius: 50%;
    color: #fff;
    background: #00bf6b;
    font-size: 21rpx;
    font-weight: 700;
  }
  .hero-copy {
    margin-top: 38rpx;
  }
  .hero-title {
    font-size: 42rpx;
    font-weight: 800;
    letter-spacing: 1rpx;
  }
  .hero-desc {
    margin-top: 10rpx;
    font-size: 23rpx;
    opacity: 0.9;
  }
  .transfer-wrap {
    position: relative;
    z-index: 1;
    margin-top: -36rpx;
    overflow: hidden;
    border: 1rpx solid rgba(19, 191, 109, 0.12);
    border-radius: 24rpx;
    background: #fff;
    box-shadow: 0 14rpx 40rpx rgba(24, 73, 50, 0.1);
  }
  .tutorial-tip {
    display: flex;
    align-items: center;
    gap: 10rpx;
    padding: 20rpx 24rpx 0;
    color: #8a5a17;
    font-size: 21rpx;
  }
  .tutorial-badge {
    padding: 5rpx 10rpx;
    border-radius: 8rpx;
    color: #ae6d10;
    background: #fff1d4;
    font-size: 19rpx;
  }
  .tutorial-arrow {
    margin-left: auto;
    font-size: 30rpx;
  }
  .section-card {
    margin-top: 20rpx;
    padding: 28rpx 24rpx;
    border-radius: 24rpx;
    background: #fff;
    box-shadow: 0 8rpx 24rpx rgba(31, 43, 38, 0.045);
  }
  .section-heading > view {
    min-width: 0;
  }
  .section-title {
    color: #202226;
    font-size: 30rpx;
    font-weight: 750;
  }
  .section-subtitle {
    margin-top: 6rpx;
    color: #999da4;
    font-size: 20rpx;
  }
  .section-more {
    flex: 0 0 auto;
    margin-left: 18rpx;
    color: #7c8188;
    font-size: 21rpx;
  }
  .marketplace-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    margin-top: 28rpx;
  }
  .marketplace-item {
    display: flex;
    min-width: 0;
    flex-direction: column;
    align-items: center;
  }
  .marketplace-logo {
    display: flex;
    width: 82rpx;
    height: 82rpx;
    align-items: center;
    justify-content: center;
    border-radius: 24rpx;
    color: #fff;
    box-shadow: inset 0 0 0 2rpx rgba(255, 255, 255, 0.18);
    font-size: 36rpx;
    font-weight: 800;
  }
  .marketplace-name {
    margin-top: 13rpx;
    font-size: 23rpx;
    white-space: nowrap;
  }
  .marketplace-tip {
    margin-top: 5rpx;
    color: #a0a3a9;
    font-size: 18rpx;
    white-space: nowrap;
  }
  .local-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 14rpx;
    margin-top: 24rpx;
  }
  .local-item {
    display: flex;
    min-height: 192rpx;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    border: 1rpx solid #f0f1f3;
    border-radius: 20rpx;
    background: linear-gradient(180deg, #fff, #fbfbfc);
  }
  .local-icon {
    display: flex;
    width: 66rpx;
    height: 66rpx;
    align-items: center;
    justify-content: center;
    border-radius: 20rpx;
    font-size: 28rpx;
    font-weight: 800;
  }
  .local-title {
    margin-top: 12rpx;
    font-size: 24rpx;
    font-weight: 650;
  }
  .local-rebate {
    max-width: 156rpx;
    margin-top: 8rpx;
    padding: 4rpx 10rpx;
    overflow: hidden;
    border-radius: 7rpx;
    color: #ef4444;
    background: #fff0f1;
    font-size: 18rpx;
    text-overflow: ellipsis;
    white-space: nowrap;
    box-sizing: border-box;
  }
  .rebate-notice {
    margin-top: 20rpx;
    padding: 14rpx 18rpx;
    border-radius: 12rpx;
    color: #9b7a47;
    background: #fff9ec;
    font-size: 19rpx;
    line-height: 1.5;
  }
  .compact-heading {
    margin-bottom: 22rpx;
  }
  .asset-grid {
    justify-content: space-between;
  }
  .asset-item {
    display: flex;
    width: 25%;
    flex-direction: column;
    align-items: center;
    gap: 10rpx;
    color: #4d5157;
    font-size: 21rpx;
  }
  .asset-icon {
    display: flex;
    width: 60rpx;
    height: 60rpx;
    align-items: center;
    justify-content: center;
    border-radius: 18rpx;
    color: #08ae65;
    background: #eaf9f2;
    font-size: 23rpx;
    font-weight: 700;
  }
</style>
