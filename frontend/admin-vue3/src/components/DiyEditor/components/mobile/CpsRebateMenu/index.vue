<template>
  <section class="rebate-menu-preview">
    <strong class="title">{{ property.title }}</strong>
    <div class="menu-grid" :style="{ gridTemplateColumns: `repeat(${safeColumns}, minmax(0, 1fr))` }">
      <div v-for="item in visibleItems" :key="item.key" class="menu-item">
        <span class="icon"><Icon :icon="item.icon || destinationMap[item.key]?.icon || 'ep:menu'" /></span>
        <span>{{ item.title || destinationMap[item.key]?.title }}</span>
        <small v-if="destinationMap[item.key]?.requiresAuth">登录</small>
      </div>
    </div>
    <el-empty v-if="!visibleItems.length" description="请启用返利入口" :image-size="40" />
  </section>
</template>

<script setup lang="ts">
import { CpsRebateMenuProperty, REBATE_MENU_DESTINATIONS } from './config'

defineOptions({ name: 'CpsRebateMenu' })
const props = defineProps<{ property: CpsRebateMenuProperty }>()
const destinationMap = Object.fromEntries(REBATE_MENU_DESTINATIONS.map((item) => [item.key, item]))
const safeColumns = computed(() => Math.min(5, Math.max(2, props.property.columns || 4)))
const visibleItems = computed(() => (props.property.items || []).filter((item) => item.enabled).slice(0, 8))
</script>

<style scoped lang="scss">
.rebate-menu-preview { border-radius: 8px; background: #fff; color: #333; }
.title { font-size: 14px; }
.menu-grid { display: grid; gap: 14px 8px; margin-top: 14px; }
.menu-item { position: relative; display: flex; min-width: 0; flex-direction: column; align-items: center; gap: 6px; font-size: 10px; text-align: center; }
.menu-item > span:last-of-type { width: 100%; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.icon { display: flex; width: 32px; height: 32px; align-items: center; justify-content: center; border-radius: 50%; color: #f4513b; background: #fff1ed; font-size: 16px; }
small { position: absolute; top: -5px; right: 2px; color: #aaa; font-size: 8px; }
</style>
