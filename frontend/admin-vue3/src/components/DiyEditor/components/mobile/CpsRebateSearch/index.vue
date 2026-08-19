<template>
  <section class="rebate-search-preview">
    <div class="kicker">{{ property.kicker }}</div>
    <h3>{{ property.title }}</h3>
    <p>{{ property.description }}</p>
    <div class="search-box">
      <Icon icon="ep:search" />
      <span>{{ property.placeholder }}</span>
      <b>{{ property.buttonText }}</b>
    </div>
  </section>
</template>

<script setup lang="ts">
import { CpsRebateSearchProperty } from './config'

defineOptions({ name: 'CpsRebateSearch' })
defineProps<{ property: CpsRebateSearchProperty }>()
</script>

<style scoped lang="scss">
.rebate-search-preview {
  padding: 18px 16px;
  border-radius: 8px;
  color: #fff;
  background: linear-gradient(135deg, #ff7a45, #f04438);
}
.kicker { font-size: 11px; opacity: 0.9; }
h3 { margin: 7px 0 0; font-size: 22px; line-height: 1.25; }
p { margin: 6px 0 0; font-size: 11px; opacity: 0.9; }
.search-box {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-top: 16px;
  padding: 5px 6px 5px 11px;
  border-radius: 22px;
  color: #999;
  background: #fff;
  font-size: 11px;
}
.search-box span { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.search-box b { padding: 7px 11px; border-radius: 16px; color: #fff; background: #f4513b; font-size: 10px; }
</style>
