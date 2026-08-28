<template>
  <s-layout title="本地生活" navbar="none" :bgStyle="{ color: '#f6f7f8' }">
    <view class="local-life-page">
      <view class="page-header">
        <view class="back-button" aria-label="返回" @tap="goBack">‹</view>
        <text class="page-title">本地生活</text>
        <view class="header-spacer" />
      </view>

      <view class="hero-promos">
        <view
          v-for="promo in promos"
          :key="promo.title"
          class="promo-card"
          :class="promo.theme"
          @tap="openPromo(promo)"
        >
          <view class="promo-art" :class="promo.artClass">
            <text class="promo-art-main">{{ promo.art }}</text>
            <text class="promo-art-spark">✦</text>
          </view>
          <view class="promo-content">
            <view class="promo-badge">{{ promo.badge }}</view>
            <text class="promo-title">{{ promo.title }}</text>
            <text class="promo-subtitle">{{ promo.subtitle }}</text>
            <view class="promo-button">{{ promo.action }} <text>›</text></view>
          </view>
        </view>
      </view>

      <view class="service-panel panel-card">
        <swiper
          class="service-swiper"
          :current="servicePage"
          :duration="260"
          :indicator-dots="false"
          :autoplay="false"
          @change="onServicePageChange"
        >
          <swiper-item v-for="(page, pageIndex) in servicePages" :key="pageIndex">
            <view class="service-grid">
              <view
                v-for="service in page"
                :key="service.title"
                class="service-item"
                @tap="openService(service)"
              >
                <view
                  class="service-icon"
                  :class="service.iconClass"
                  :style="{
                    background: service.background,
                    color: service.color,
                    borderColor: service.borderColor,
                  }"
                >
                  <text>{{ service.icon }}</text>
                </view>
                <text class="service-title">{{ service.title }}</text>
                <text class="service-desc">{{ service.desc }}</text>
              </view>
            </view>
          </swiper-item>
        </swiper>
        <view class="pager">
          <view
            v-for="(_, index) in servicePages"
            :key="index"
            class="pager-item"
            :class="{ 'pager-active': servicePage === index }"
          />
        </view>
      </view>

      <view class="banner-card" @tap="openActivity('美团电影')">
        <view class="banner-brand"><text class="brand-mark">美团</text><text>美团</text></view>
        <view class="banner-copy">
          <text class="banner-kicker">购票享优惠</text>
          <text class="banner-title">用美团订票<br />享清凉夏日</text>
          <text class="banner-action">抽红包下单拿返利！</text>
        </view>
        <view class="banner-mascot">☀</view>
      </view>

      <view class="section-head">
        <text class="section-title">精选活动</text>
        <text class="section-line" />
      </view>

      <view class="activity-grid">
        <view
          v-for="activity in activities"
          :key="activity.title"
          class="activity-card"
          @tap="openActivity(activity.keyword)"
        >
          <view class="activity-image" :class="activity.theme">
            <text class="activity-brand">{{ activity.brand }}</text>
            <text class="activity-image-title">{{ activity.imageTitle }}</text>
            <text class="activity-image-subtitle">{{ activity.imageSubtitle }}</text>
          </view>
          <view class="activity-body">
            <text class="activity-title">{{ activity.title }}</text>
            <text class="activity-desc">{{ activity.desc }}</text>
            <view class="activity-button">购买最高再返10% <text>›</text></view>
          </view>
        </view>
      </view>

      <view class="more-card">
        <text class="more-title">更多活动每日更新</text>
        <text class="more-desc">折扣享不停</text>
        <view class="more-button" @tap="openActivity('本地生活')">敬请期待 ›</view>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, ref } from 'vue';
  import sheep from '@/sheep';

  const promos = [
    {
      badge: '最高返10%',
      title: '饿了么',
      subtitle: '红包最高66元',
      action: '领红包',
      art: '🍔',
      artClass: 'eleme-art',
      theme: 'promo-orange',
      keyword: '饿了么',
    },
    {
      badge: '领券下单返0.5%',
      title: '美团外卖',
      subtitle: '红包最高66元',
      action: '领红包',
      art: '🦁',
      artClass: 'meituan-art',
      theme: 'promo-red',
      keyword: '美团外卖',
    },
  ];

  const services = [
    {
      icon: 'M',
      title: '麦当劳',
      desc: '约返10%',
      background: '#ffd21a',
      color: '#d51f28',
      iconClass: 'service-icon-mcd',
      keyword: '麦当劳',
    },
    {
      icon: 'P',
      title: '必胜客',
      desc: '7折再返6%',
      background: '#fff',
      color: '#d92f36',
      borderColor: '#eceef2',
      iconClass: 'service-icon-pizza',
      keyword: '必胜客',
    },
    {
      icon: 'BK',
      title: '汉堡王',
      desc: '下单返3%',
      background: '#f8f9fb',
      color: '#e86c25',
      borderColor: '#eceef2',
      iconClass: 'service-icon-bk',
      keyword: '汉堡王',
    },
    {
      icon: '喜',
      title: '喜茶',
      desc: '下单返1%',
      background: '#fff',
      color: '#202226',
      borderColor: '#eceef2',
      iconClass: 'service-icon-hetea',
      keyword: '喜茶',
    },
    {
      icon: '奈',
      title: '奈雪的茶',
      desc: '下单返5%',
      background: '#a8cd16',
      color: '#fff',
      iconClass: 'service-icon-naixue',
      keyword: '奈雪的茶',
    },
    {
      icon: '瑞',
      title: '瑞幸',
      desc: '5折再返3%',
      background: '#182a94',
      color: '#fff',
      iconClass: 'service-icon-luckin',
      keyword: '瑞幸',
    },
    {
      icon: '星',
      title: '星巴克',
      desc: '8折再返3%',
      background: '#00704a',
      color: '#fff',
      iconClass: 'service-icon-starbucks',
      keyword: '星巴克',
    },
    {
      icon: '果',
      title: '百果园',
      desc: '下单返3%',
      background: '#fff',
      color: '#2a9e65',
      borderColor: '#eceef2',
      iconClass: 'service-icon-pagoda',
      keyword: '百果园',
    },
    {
      icon: '♫',
      title: '影音充值',
      desc: '低至5折',
      background: '#6c16ee',
      color: '#fff',
      iconClass: 'service-icon-video',
      keyword: '影音充值',
    },
    {
      icon: '餐',
      title: '到店美食',
      desc: '约返1%',
      background: '#ff6c72',
      color: '#fff',
      iconClass: 'service-icon-food',
      keyword: '到店美食',
    },
    {
      icon: '携',
      title: '携程',
      desc: '酒店齐全',
      background: 'linear-gradient(145deg,#29a9ed,#0876da)',
      keyword: '携程酒店',
    },
    {
      icon: '京',
      title: '京东外卖',
      desc: '约返0.3%',
      background: 'linear-gradient(145deg,#ff453f,#d90000)',
      keyword: '京东外卖',
    },
    {
      icon: '车',
      title: '打车出行',
      desc: '约返1.5%',
      background: 'linear-gradient(145deg,#18bdf2,#138de0)',
      keyword: '打车出行',
    },
    {
      icon: '影',
      title: '影院优惠',
      desc: '约返3%',
      background: 'linear-gradient(145deg,#ffad17,#f47c00)',
      keyword: '电影票',
    },
    {
      icon: '寄',
      title: '寄件优惠',
      desc: '下单返5%',
      background: 'linear-gradient(145deg,#72cb17,#25a600)',
      keyword: '寄件优惠',
    },
    { icon: '美', title: '美团酒店', desc: '约返3%', background: '#fff', keyword: '美团酒店' },
    {
      icon: '飞',
      title: '飞猪旅行',
      desc: '最高返3%',
      background: 'linear-gradient(145deg,#ffd500,#f2ae00)',
      keyword: '飞猪旅行',
    },
    { icon: '艺', title: '艺龙旅行', desc: '约返3%', background: '#fff', keyword: '艺龙旅行' },
    { icon: 'K', title: '肯德基', desc: '下单返5%', background: '#fff', keyword: '肯德基' },
    {
      icon: '花',
      title: '品质鲜花',
      desc: '下单返5%',
      background: 'linear-gradient(145deg,#c750e9,#8f35c5)',
      keyword: '品质鲜花',
    },
  ];
  const servicePages = computed(() => {
    const pages = [];
    for (let index = 0; index < services.length; index += 10) {
      pages.push(services.slice(index, index + 10));
    }
    return pages;
  });
  const servicePage = ref(0);

  const activities = [
    {
      brand: '饿了么',
      imageTitle: '个护满10起',
      imageSubtitle: '精选好物叠加红包',
      title: '屈臣氏品牌馆',
      desc: '领大额红包福利',
      theme: 'activity-blue',
      keyword: '屈臣氏',
    },
    {
      brand: '饿了么',
      imageTitle: '解锁秋天的',
      imageSubtitle: '第一份礼物',
      title: '名创优品品牌馆',
      desc: '叠券最高99-65',
      theme: 'activity-pink',
      keyword: '名创优品',
    },
    {
      brand: '饿了么',
      imageTitle: '抢超值组合',
      imageSubtitle: '红包马上领',
      title: '天天领红包（全场通用）',
      desc: '最高抢66元大红包',
      theme: 'activity-cyan',
      keyword: '天天领红包',
    },
    {
      brand: '饿了么',
      imageTitle: '每日晚8点',
      imageSubtitle: '免单大红包',
      title: '闪购夜宵专享会场',
      desc: '每晚8点抢免单红包',
      theme: 'activity-purple',
      keyword: '夜宵红包',
    },
    {
      brand: '饿了么',
      imageTitle: '超级人脉大补贴上线',
      imageSubtitle: '拾5~10叠加红包',
      title: '品牌馆',
      desc: '抢5-10叠加红包',
      theme: 'activity-green',
      keyword: '品牌馆',
    },
  ];

  function goBack() {
    uni.navigateBack({ delta: 1, fail: () => sheep.$router.go('/pages/index/index') });
  }

  function openActivity(keyword) {
    sheep.$router.go('/pages/cps/activity', { keyword: encodeURIComponent(keyword) });
  }

  function openPromo(promo) {
    openActivity(promo.keyword);
  }

  function openService(service) {
    openActivity(service.keyword);
  }

  function onServicePageChange(event) {
    servicePage.value = event.detail.current;
  }
</script>

<style lang="scss" scoped>
  .local-life-page {
    min-height: 100vh;
    padding: 0 24rpx 40rpx;
    color: #202226;
    background: #f6f7f8;
    box-sizing: border-box;
  }
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    height: 112rpx;
    margin: 0 -24rpx;
    padding: env(safe-area-inset-top) 28rpx 0;
    background: linear-gradient(100deg, #d5f8ff, #fffce7);
    box-sizing: border-box;
  }
  .back-button {
    width: 56rpx;
    color: #1f2328;
    font-size: 70rpx;
    font-weight: 200;
    line-height: 48rpx;
    transform: translateY(-4rpx);
  }
  .header-spacer {
    width: 56rpx;
  }
  .page-title {
    font-size: 40rpx;
    font-weight: 800;
    letter-spacing: 2rpx;
  }
  .hero-promos {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18rpx;
    margin-top: 18rpx;
  }
  .promo-card {
    position: relative;
    min-height: 436rpx;
    overflow: hidden;
    border: 6rpx solid #ff5c1b;
    border-radius: 28rpx;
    color: #fff;
    box-sizing: border-box;
  }
  .promo-orange {
    background: linear-gradient(165deg, #ff8b28, #ff3b12 70%);
  }
  .promo-red {
    border-color: #ff5417;
    background: linear-gradient(165deg, #ff5b18, #fc064b 72%);
  }
  .promo-art {
    height: 170rpx;
    padding: 20rpx 12rpx 0;
    background: rgba(255, 255, 255, 0.96);
    color: #ff7a17;
    text-align: center;
    box-sizing: border-box;
  }
  .promo-art-main {
    display: block;
    font-size: 108rpx;
    line-height: 1;
    filter: drop-shadow(0 8rpx 4rpx rgba(0, 0, 0, 0.12));
  }
  .promo-art-spark {
    position: absolute;
    top: 62rpx;
    right: 20rpx;
    color: #ffd52c;
    font-size: 36rpx;
  }
  .meituan-art {
    color: #ffc400;
  }
  .promo-content {
    padding: 16rpx 18rpx 20rpx;
    text-align: center;
  }
  .promo-badge {
    display: inline-block;
    padding: 5rpx 12rpx;
    border-radius: 18rpx;
    background: rgba(255, 255, 255, 0.2);
    font-size: 19rpx;
  }
  .promo-title {
    display: block;
    margin-top: 8rpx;
    font-size: 31rpx;
    font-weight: 800;
  }
  .promo-subtitle {
    display: block;
    margin-top: 6rpx;
    font-size: 23rpx;
  }
  .promo-button {
    margin: 18rpx auto 0;
    padding: 13rpx 12rpx;
    border-radius: 999rpx;
    color: #ff3211;
    background: linear-gradient(#fff76b, #ffd632);
    font-size: 27rpx;
    font-weight: 800;
  }
  .promo-button text,
  .activity-button text {
    margin-left: 8rpx;
    font-size: 30rpx;
  }
  .panel-card {
    border-radius: 24rpx;
    background: #fff;
    box-shadow: 0 6rpx 24rpx rgba(26, 31, 38, 0.05);
  }
  .service-panel {
    margin-top: 18rpx;
    padding: 24rpx 16rpx 18rpx;
  }
  .service-swiper {
    height: 360rpx;
  }
  .service-grid {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    row-gap: 24rpx;
  }
  .service-item {
    display: flex;
    min-width: 0;
    flex-direction: column;
    align-items: center;
  }
  .service-icon {
    display: flex;
    width: 96rpx;
    height: 96rpx;
    align-items: center;
    justify-content: center;
    border-radius: 28rpx;
    color: #fff;
    font-size: 40rpx;
    font-weight: 800;
    border: 1rpx solid transparent;
    box-shadow: 0 4rpx 8rpx rgba(0, 0, 0, 0.08);
  }
  .service-icon-bk {
    font-size: 25rpx;
    letter-spacing: -2rpx;
  }
  .service-icon-video {
    font-size: 52rpx;
  }
  .service-icon-food {
    font-size: 34rpx;
  }
  .service-title {
    margin-top: 10rpx;
    font-size: 23rpx;
    font-weight: 700;
    white-space: nowrap;
  }
  .service-desc {
    margin-top: 3rpx;
    color: #ef4b47;
    font-size: 19rpx;
    white-space: nowrap;
  }
  .pager {
    display: flex;
    width: auto;
    height: 8rpx;
    align-items: center;
    justify-content: center;
    gap: 8rpx;
    margin: 20rpx auto 0;
  }
  .pager-item {
    width: 24rpx;
    height: 8rpx;
    border-radius: 999rpx;
    background: #d7d9dc;
  }
  .pager-item.pager-active {
    width: 58rpx;
    background: #16b869;
  }
  .banner-card {
    position: relative;
    display: flex;
    min-height: 184rpx;
    overflow: hidden;
    align-items: center;
    margin-top: 20rpx;
    padding: 24rpx 28rpx;
    border-radius: 24rpx;
    color: #fff;
    background: linear-gradient(100deg, #05cce2, #00a9ed 55%, #f9d20b);
    box-sizing: border-box;
  }
  .banner-brand {
    position: absolute;
    top: 14rpx;
    left: 20rpx;
    display: flex;
    align-items: center;
    gap: 6rpx;
    font-size: 25rpx;
    font-weight: 800;
  }
  .brand-mark {
    padding: 3rpx 6rpx;
    border-radius: 6rpx;
    color: #fff;
    background: #f4c400;
    font-size: 18rpx;
  }
  .banner-copy {
    margin-left: 148rpx;
  }
  .banner-kicker {
    display: block;
    color: #e33b32;
    font-size: 23rpx;
    font-weight: 800;
  }
  .banner-title {
    display: block;
    margin-top: 4rpx;
    font-size: 32rpx;
    font-weight: 900;
    line-height: 1.1;
  }
  .banner-action {
    display: inline-block;
    margin-top: 10rpx;
    padding: 5rpx 14rpx;
    border-radius: 999rpx;
    color: #146c44;
    background: #ffeb25;
    font-size: 20rpx;
    font-weight: 700;
  }
  .banner-mascot {
    position: absolute;
    right: 32rpx;
    bottom: 16rpx;
    font-size: 104rpx;
  }
  .section-head {
    display: flex;
    position: relative;
    align-items: center;
    justify-content: center;
    margin: 22rpx 0 18rpx;
    padding: 22rpx 0 12rpx;
    border-radius: 22rpx;
    background: #fff;
  }
  .section-title {
    font-size: 34rpx;
    font-weight: 800;
  }
  .section-line {
    position: absolute;
    width: 90rpx;
    height: 8rpx;
    margin-top: 64rpx;
    border-radius: 999rpx;
    background: #f33a36;
  }
  .activity-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18rpx;
  }
  .activity-card {
    overflow: hidden;
    border-radius: 24rpx;
    background: #fff;
    box-shadow: 0 4rpx 18rpx rgba(25, 34, 46, 0.05);
  }
  .activity-image {
    display: flex;
    min-height: 220rpx;
    flex-direction: column;
    justify-content: center;
    padding: 24rpx;
    color: #fff;
    box-sizing: border-box;
  }
  .activity-blue {
    background: linear-gradient(145deg, #04cbe6, #078be8);
  }
  .activity-pink {
    background: linear-gradient(145deg, #ffd8e7, #fa4b9b);
  }
  .activity-cyan {
    background: linear-gradient(145deg, #18caff, #087eea);
  }
  .activity-purple {
    background: linear-gradient(145deg, #3d0787, #17004c);
  }
  .activity-green {
    background: linear-gradient(145deg, #0d7770, #16b36e);
  }
  .activity-brand {
    align-self: flex-start;
    padding: 4rpx 10rpx;
    border-radius: 8rpx;
    background: rgba(0, 0, 0, 0.2);
    font-size: 20rpx;
    font-weight: 700;
  }
  .activity-image-title {
    margin-top: 22rpx;
    font-size: 28rpx;
    font-weight: 900;
  }
  .activity-image-subtitle {
    margin-top: 8rpx;
    font-size: 20rpx;
    opacity: 0.92;
  }
  .activity-body {
    padding: 18rpx 16rpx 20rpx;
    text-align: center;
  }
  .activity-title,
  .activity-desc {
    display: block;
  }
  .activity-title {
    overflow: hidden;
    font-size: 27rpx;
    font-weight: 800;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .activity-desc {
    margin-top: 7rpx;
    color: #ef4b47;
    font-size: 22rpx;
    font-weight: 700;
  }
  .activity-button {
    margin-top: 16rpx;
    padding: 12rpx 6rpx;
    border-radius: 999rpx;
    color: #fff;
    background: linear-gradient(90deg, #ff4c45, #f5343c);
    font-size: 21rpx;
    font-weight: 700;
  }
  .more-card {
    margin-top: 18rpx;
    padding: 42rpx 24rpx 24rpx;
    border-radius: 24rpx;
    color: #777;
    background: #e5e5e7;
    text-align: center;
  }
  .more-title,
  .more-desc {
    display: block;
  }
  .more-title {
    color: #24262b;
    font-size: 29rpx;
    font-weight: 800;
  }
  .more-desc {
    margin-top: 8rpx;
    font-size: 23rpx;
  }
  .more-button {
    margin: 24rpx auto 0;
    padding: 14rpx;
    border-radius: 999rpx;
    color: #fff;
    background: #c9c9ca;
    font-size: 24rpx;
  }
</style>
