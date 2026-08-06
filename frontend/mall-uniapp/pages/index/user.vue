<!-- 个人中心：支持装修 -->
<template>
  <s-layout
    title="我的"
    tabbar="/pages/index/user"
    navbar="custom"
    :bgStyle="template.page"
    :navbarStyle="template.navigationBar"
    onShareAppMessage
  >
    <view class="cps-menu">
      <view class="cps-menu__title">我的返利</view>
      <view class="cps-menu__items ss-flex">
        <view class="cps-menu__item" @tap="sheep.$router.go('/pages/cps/index')">
          <view class="cps-menu__icon">券</view>
          <view>返利中心</view>
        </view>
        <view class="cps-menu__item" @tap="sheep.$router.go('/pages/cps/order')">
          <view class="cps-menu__icon">单</view>
          <view>返利订单</view>
        </view>
        <view class="cps-menu__item" @tap="sheep.$router.go('/pages/cps/withdraw')">
          <view class="cps-menu__icon">¥</view>
          <view>提现</view>
        </view>
      </view>
    </view>
    <s-block
      v-for="(item, index) in template.components"
      :key="index"
      :styles="item.property.style"
    >
      <s-block-item :type="item.id" :data="item.property" :styles="item.property.style" />
    </s-block>
  </s-layout>
</template>

<script setup>
  import { computed } from 'vue';
  import { onShow, onPageScroll, onPullDownRefresh } from '@dcloudio/uni-app';
  import sheep from '@/sheep';

  // 隐藏原生tabBar
  uni.hideTabBar({
    fail: () => {},
  });

  const template = computed(() => sheep.$store('app').template.user);

  onShow(() => {
    sheep.$store('user').updateUserData();
  });

  onPullDownRefresh(() => {
    sheep.$store('user').updateUserData();
    setTimeout(function () {
      uni.stopPullDownRefresh();
    }, 800);
  });

  onPageScroll(() => {});
</script>

<style lang="scss" scoped>
  .cps-menu {
    box-sizing: border-box;
    margin: 20rpx;
    padding: 28rpx 20rpx;
    border-radius: 20rpx;
    background: #fff;
    box-shadow: 0 6rpx 20rpx rgba(0, 0, 0, 0.05);
  }

  .cps-menu__title {
    padding: 0 12rpx 24rpx;
    color: #333;
    font-size: 30rpx;
    font-weight: 600;
  }

  .cps-menu__items {
    justify-content: space-around;
  }

  .cps-menu__item {
    width: 30%;
    color: #555;
    font-size: 25rpx;
    text-align: center;
  }

  .cps-menu__icon {
    width: 64rpx;
    height: 64rpx;
    margin: 0 auto 12rpx;
    border-radius: 20rpx;
    color: #ff4d4f;
    background: #fff1f0;
    font-size: 28rpx;
    font-weight: 600;
    line-height: 64rpx;
  }
</style>
