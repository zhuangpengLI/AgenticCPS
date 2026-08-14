<template>
  <s-layout title="AIoT 场景推荐" navbar="inner">
    <view class="scene-page">
      <view class="scene-header">
        <view>
          <text class="scene-kicker">AgenticCPS 智能推荐</text>
          <text class="scene-title">把设备数据变成省钱建议</text>
        </view>
        <text class="scene-refresh" @tap="refreshScene">↻</text>
      </view>

      <scroll-view class="scene-tabs" scroll-x :show-scrollbar="false">
        <view
          v-for="item in scenes"
          :key="item.code"
          class="scene-tab"
          :class="{ active: state.selectedScene === item.code }"
          @tap="selectScene(item.code)"
        >
          <text class="scene-tab-icon">{{ item.icon }}</text>
          <text>{{ item.name }}</text>
        </view>
      </scroll-view>

      <view class="sensor-card">
        <view class="sensor-title-row">
          <view>
            <text class="sensor-kicker">当前场景</text>
            <text class="sensor-title">{{ currentScene.name }}</text>
          </view>
          <text class="sensor-status">{{ currentScene.status }}</text>
        </view>
        <view class="sensor-values">
          <view v-for="metric in currentScene.metrics" :key="metric.label" class="sensor-metric">
            <text class="sensor-metric-value">{{ metric.value }}</text>
            <text class="sensor-metric-label">{{ metric.label }}</text>
          </view>
        </view>
        <text class="sensor-desc">{{ currentScene.description }}</text>
      </view>

      <view class="section-heading">
        <text>智能推荐</text>
        <text class="section-subtitle">为什么推荐 ›</text>
      </view>

      <view class="recommendation-list">
        <view
          v-for="item in currentScene.recommendations"
          :key="item.keyword"
          class="recommendation-card"
          @tap="searchGoods(item.keyword)"
        >
          <view class="recommendation-mark">{{ item.mark }}</view>
          <view class="recommendation-info">
            <text class="recommendation-name">{{ item.name }}</text>
            <text class="recommendation-reason">{{ item.reason }}</text>
            <text class="recommendation-rebate">预计返利 {{ item.rebate }}</text>
          </view>
          <view class="recommendation-action">查返利</view>
        </view>
      </view>

      <view class="scene-note">
        <text class="scene-note-title">推荐说明</text>
        <text>推荐结果只用于商品发现和返利决策，不会直接修改订单或资产。</text>
      </view>
    </view>
  </s-layout>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import sheep from '@/sheep';

  const scenes = [
    {
      code: 'living-room',
      icon: '客',
      name: '客厅舒适',
      status: '设备在线',
      metrics: [
        { label: '温度', value: '28°C' },
        { label: '湿度', value: '72%' },
        { label: '空气', value: '偏闷' },
      ],
      description: '温度偏高且湿度较大，优先推荐空气循环和舒适度改善类商品。',
      recommendations: [
        {
          mark: '扇',
          name: '智能空气循环扇',
          reason: '改善空气流通',
          rebate: '¥8.20',
          keyword: '空气循环扇',
        },
        {
          mark: '湿',
          name: '智能加湿器',
          reason: '平衡室内湿度',
          rebate: '¥12.30',
          keyword: '智能加湿器',
        },
        {
          mark: '灯',
          name: '护眼暖光灯',
          reason: '柔和灯光护眼',
          rebate: '¥5.80',
          keyword: '护眼暖光灯',
        },
      ],
    },
    {
      code: 'bedroom',
      icon: '卧',
      name: '卧室睡眠',
      status: '夜间模式',
      metrics: [
        { label: '噪音', value: '42dB' },
        { label: '光照', value: '偏亮' },
        { label: '睡眠', value: '一般' },
      ],
      description: '当前更适合低噪、遮光和睡眠辅助类商品。',
      recommendations: [
        {
          mark: '枕',
          name: '云朵乳胶枕',
          reason: '支撑颈椎睡得更稳',
          rebate: '¥12.80',
          keyword: '乳胶枕',
        },
        {
          mark: '帘',
          name: '智能遮光窗帘',
          reason: '降低夜间光照',
          rebate: '¥18.60',
          keyword: '遮光窗帘',
        },
        {
          mark: '音',
          name: '主动降噪耳机',
          reason: '减少环境噪音',
          rebate: '¥16.20',
          keyword: '降噪耳机',
        },
      ],
    },
    {
      code: 'commute',
      icon: '通',
      name: '通勤出行',
      status: '工作日',
      metrics: [
        { label: '天气', value: '小雨' },
        { label: '通勤', value: '46min' },
        { label: '预算', value: '¥300' },
      ],
      description: '结合天气和通勤时长，优先推荐轻便、耐用的随身好物。',
      recommendations: [
        {
          mark: '伞',
          name: '便携晴雨伞',
          reason: '应对通勤小雨',
          rebate: '¥4.60',
          keyword: '便携晴雨伞',
        },
        {
          mark: '包',
          name: '轻量通勤双肩包',
          reason: '收纳更方便',
          rebate: '¥9.80',
          keyword: '通勤双肩包',
        },
        {
          mark: '杯',
          name: '便携式榨汁杯',
          reason: '路上补充能量',
          rebate: '¥6.30',
          keyword: '便携榨汁杯',
        },
      ],
    },
  ];

  const state = reactive({ selectedScene: scenes[0].code });
  const currentScene = computed(
    () => scenes.find((item) => item.code === state.selectedScene) || scenes[0],
  );

  function selectScene(code) {
    state.selectedScene = code;
  }

  function refreshScene() {
    sheep.$helper.toast('场景数据已刷新');
  }

  function searchGoods(keyword) {
    sheep.$router.go('/pages/cps/goods', { keyword: encodeURIComponent(keyword) });
  }
</script>

<style lang="scss" scoped>
  .scene-page {
    padding: 18rpx 20rpx 48rpx;
    background: #fffaf6;
  }
  .scene-header,
  .sensor-title-row,
  .section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .scene-header {
    padding: 8rpx 4rpx 20rpx;
  }
  .scene-kicker,
  .sensor-kicker {
    display: block;
    color: #8a5a3b;
    font-size: 22rpx;
  }
  .scene-title {
    display: block;
    margin-top: 8rpx;
    color: #222;
    font-size: 38rpx;
    font-weight: 700;
  }
  .scene-refresh {
    color: var(--ui-BG-Main);
    font-size: 42rpx;
  }
  .scene-tabs {
    width: 100%;
    white-space: nowrap;
  }
  .scene-tab {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 150rpx;
    height: 118rpx;
    margin-right: 14rpx;
    border-radius: 20rpx;
    color: #777;
    background: #fff;
    font-size: 23rpx;
  }
  .scene-tab.active {
    color: var(--ui-BG-Main);
    background: #fff1e7;
    box-shadow: 0 8rpx 22rpx rgba(255, 106, 0, 0.12);
  }
  .scene-tab-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 54rpx;
    height: 54rpx;
    margin-bottom: 10rpx;
    border-radius: 50%;
    color: #fff;
    background: var(--ui-BG-Main);
    font-size: 24rpx;
    font-weight: 700;
  }
  .sensor-card {
    margin-top: 18rpx;
    padding: 28rpx;
    border-radius: 24rpx;
    background: #ffeedf;
  }
  .sensor-title {
    display: block;
    margin-top: 8rpx;
    color: #222;
    font-size: 34rpx;
    font-weight: 700;
  }
  .sensor-status {
    padding: 8rpx 16rpx;
    border-radius: 24rpx;
    color: #168f73;
    background: #e9f8f2;
    font-size: 21rpx;
  }
  .sensor-values {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12rpx;
    margin-top: 24rpx;
  }
  .sensor-metric {
    padding: 16rpx 12rpx;
    border-radius: 14rpx;
    background: rgba(255, 255, 255, 0.65);
  }
  .sensor-metric-value,
  .sensor-metric-label {
    display: block;
  }
  .sensor-metric-value {
    color: #222;
    font-size: 28rpx;
    font-weight: 700;
  }
  .sensor-metric-label {
    margin-top: 4rpx;
    color: #8a5a3b;
    font-size: 20rpx;
  }
  .sensor-desc {
    display: block;
    margin-top: 18rpx;
    color: #8a5a3b;
    font-size: 22rpx;
    line-height: 1.6;
  }
  .section-heading {
    margin: 28rpx 4rpx 16rpx;
    color: #222;
    font-size: 32rpx;
    font-weight: 700;
  }
  .section-subtitle {
    color: #999;
    font-size: 22rpx;
    font-weight: 400;
  }
  .recommendation-card {
    display: flex;
    align-items: center;
    gap: 18rpx;
    margin-bottom: 14rpx;
    padding: 20rpx;
    border-radius: 20rpx;
    background: #fff;
    box-shadow: 0 8rpx 22rpx rgba(0, 0, 0, 0.04);
  }
  .recommendation-mark {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: 0 0 86rpx;
    width: 86rpx;
    height: 86rpx;
    border-radius: 18rpx;
    color: #a36a3f;
    background: #fff2e4;
    font-size: 28rpx;
    font-weight: 700;
  }
  .recommendation-info {
    flex: 1;
    min-width: 0;
  }
  .recommendation-name,
  .recommendation-reason,
  .recommendation-rebate {
    display: block;
  }
  .recommendation-name {
    color: #222;
    font-size: 27rpx;
    font-weight: 600;
  }
  .recommendation-reason {
    margin-top: 7rpx;
    color: #888;
    font-size: 22rpx;
  }
  .recommendation-rebate {
    margin-top: 8rpx;
    color: var(--ui-BG-Main);
    font-size: 22rpx;
  }
  .recommendation-action {
    flex: 0 0 auto;
    padding: 10rpx 16rpx;
    border-radius: 24rpx;
    color: #fff;
    background: var(--ui-BG-Main);
    font-size: 21rpx;
  }
  .scene-note {
    margin-top: 22rpx;
    padding: 20rpx;
    border-radius: 18rpx;
    color: #8a5a3b;
    background: #fff2e8;
    font-size: 21rpx;
    line-height: 1.6;
  }
  .scene-note-title {
    display: block;
    margin-bottom: 6rpx;
    color: #222;
    font-size: 24rpx;
    font-weight: 600;
  }
</style>
