<template>
  <div class="cps-rebate-home-preview">
    <div class="rebate-hero">
      <div class="hero-kicker">{{ property.kicker }}</div>
      <div class="hero-title">今日高返</div>
      <div class="hero-amount">{{ property.title }}</div>
      <div class="hero-description">{{ property.description }}</div>
      <div class="hero-search">
        <Icon icon="ep:search" />
        <span>{{ property.searchPlaceholder }}</span>
        <b>{{ property.searchButtonText }}</b>
      </div>
    </div>

    <section class="rebate-panel">
      <div class="section-title">快捷入口</div>
      <div class="quick-grid">
        <div v-for="item in discoveryItems" :key="item.title" class="quick-item">
          <span class="quick-icon" :class="{ 'quick-icon-ai': item.icon === 'AI' }">{{
            item.icon
          }}</span>
          <span>{{ item.title }}</span>
        </div>
      </div>
    </section>

    <section class="rebate-panel asset-entry-panel">
      <div class="section-title">我的返利 <span>进入返利工作台 ›</span></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { CpsRebateHomeProperty } from './config'

defineOptions({ name: 'CpsRebateHome' })
defineProps<{ property: CpsRebateHomeProperty }>()

const discoveryItems = [
  { icon: '券', title: '查券返利' },
  { icon: '价', title: '主题好价' },
  { icon: '活', title: '返利活动' },
  { icon: 'AI', title: '返利 AI 对话' }
]
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
  font-size: 18px;
  font-weight: 700;
}
.hero-amount {
  margin-top: 2px;
  font-size: 24px;
  font-weight: 800;
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
.asset-entry-panel {
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
.quick-icon-ai {
  color: #4d63c8;
  background: #e0e7ff;
}
.asset-entry-panel .section-title {
  display: flex;
  justify-content: space-between;
}
.asset-entry-panel .section-title span {
  color: #999;
  font-size: 10px;
  font-weight: 400;
}
</style>
