<template>
  <div class="cps-rebate-home-preview">
    <div class="rebate-hero">
      <div class="hero-kicker">{{ property.kicker }}</div>
      <div class="hero-title">{{ property.title }}</div>
      <div class="hero-description">{{ property.description }}</div>
      <div class="hero-search">
        <Icon icon="ep:search" />
        <span>{{ property.searchPlaceholder }}</span>
        <b>{{ property.searchButtonText }}</b>
      </div>
    </div>

    <section class="rebate-panel">
      <div class="section-title">我的返利</div>
      <div class="quick-grid">
        <div v-for="item in assetItems" :key="item.title" class="quick-item">
          <span class="quick-icon">{{ item.icon }}</span>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </section>

    <section class="rebate-panel discovery-panel">
      <div class="section-title">发现好物</div>
      <div class="quick-grid">
        <div v-for="item in discoveryItems" :key="item.title" class="quick-item">
          <span class="quick-icon">{{ item.icon }}</span>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </section>

    <section v-if="property.showFeatured" class="rebate-panel featured-panel">
      <div class="section-title featured-title">为你精选 <span>更多 ›</span></div>
      <div v-for="item in featuredItems" :key="item.name" class="featured-item">
        <img class="featured-thumb" :src="resolveMallStaticUrl(item.image)" :alt="item.name" />
        <span class="featured-info">
          <strong>{{ item.name }}</strong>
          <small>券后 {{ item.price }} · 预计返利 {{ item.rebate }}</small>
        </span>
        <b>去查券</b>
      </div>
    </section>

    <section class="rebate-panel guide-panel">
      <div class="section-title">如何获得返利</div>
      <div v-for="(step, index) in steps" :key="step" class="step">
        <span>{{ index + 1 }}</span>
        <em>{{ step }}</em>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { CpsRebateHomeProperty } from './config'
import { resolveMallStaticUrl } from '@/utils/mallAsset'

defineOptions({ name: 'CpsRebateHome' })
defineProps<{ property: CpsRebateHomeProperty }>()

const assetItems = [
  { icon: '单', title: '返利订单' },
  { icon: '返', title: '返利钱包' },
  { icon: '提', title: '申请提现' },
  { icon: 'T', title: '兑换 Token' }
]
const discoveryItems = [
  { icon: '活', title: '返利活动' },
  { icon: '价', title: '主题好价' },
  { icon: '迹', title: '返利足迹' },
  { icon: '智', title: 'AIoT 推荐' }
]
const featuredItems = [
  {
    image: '/static/img/diy_3c/caidandaohang/bijiben.png',
    name: '轻薄笔记本电脑 14 英寸',
    price: '¥3999',
    rebate: '¥86.50'
  },
  {
    image: '/static/img/diy_3c/caidandaohang/xiangji.png',
    name: '高清微单数码相机',
    price: '¥2699',
    rebate: '¥56.20'
  },
  {
    image: '/static/img/diy_3c/caidandaohang/erji.png',
    name: '真无线蓝牙耳机 Pro',
    price: '¥199',
    rebate: '¥18.60'
  }
]
const steps = ['搜索商品或粘贴商品链接', '查看券后价和预估返利', '领券购买，订单结算后返利到账']
</script>

<style scoped lang="scss">
.cps-rebate-home-preview {
  overflow: hidden;
  padding: 12px 10px;
  background: #f7f7f8;
  color: #222;
}

.rebate-hero {
  padding: 18px 16px 32px;
  border-radius: 14px;
  color: #fff;
  background: linear-gradient(135deg, #ff7a45, #f04438);
}

.hero-kicker {
  font-size: 11px;
  opacity: 0.9;
}
.hero-title {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
}
.hero-description {
  margin-top: 6px;
  font-size: 11px;
  opacity: 0.9;
}
.hero-search {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 20px;
  padding: 5px 6px 5px 11px;
  border-radius: 22px;
  color: #999;
  background: #fff;
  font-size: 11px;
}
.hero-search span {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.hero-search b {
  padding: 8px 11px;
  border-radius: 17px;
  color: #fff;
  background: #ff5a3c;
  font-size: 10px;
  font-weight: 500;
}

.rebate-panel {
  margin-top: -16px;
  padding: 16px 14px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 4px 14px rgb(0 0 0 / 6%);
}
.discovery-panel,
.featured-panel,
.guide-panel {
  margin-top: 10px;
}
.section-title {
  font-size: 14px;
  font-weight: 600;
}
.quick-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}
.quick-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  gap: 7px;
  color: #333;
  font-size: 10px;
}
.quick-icon {
  display: flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #f4513b;
  background: #fff1ed;
  font-size: 13px;
  font-weight: 600;
}
.featured-title {
  display: flex;
  justify-content: space-between;
}
.featured-title span {
  color: #999;
  font-size: 10px;
  font-weight: 400;
}
.featured-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
}
.featured-thumb {
  width: 36px;
  height: 36px;
  border-radius: 7px;
  background: #fff1ed;
  object-fit: contain;
}
.featured-info {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
  gap: 4px;
}
.featured-info strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
  font-weight: 500;
}
.featured-info small {
  color: #999;
  font-size: 9px;
}
.featured-item > b {
  color: #f4513b;
  font-size: 10px;
  font-weight: 500;
}
.step {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  color: #555;
  font-size: 10px;
}
.step span {
  display: flex;
  width: 18px;
  height: 18px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  color: #fff;
  background: #f4513b;
  font-size: 10px;
}
.step em {
  font-style: normal;
}
</style>
