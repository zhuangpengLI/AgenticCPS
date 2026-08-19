<template>
  <section class="rebate-assets-preview">
    <header>
      <strong>{{ property.title }}</strong>
      <el-tag size="small" type="info">预览示例</el-tag>
    </header>
    <div v-if="assetItems.length" class="asset-grid">
      <div v-for="item in assetItems" :key="item.key" class="asset-item">
        <b>{{ item.amount }}</b>
        <span>{{ item.label }}</span>
      </div>
    </div>
    <div v-else class="guest-hint">{{ property.guestText }}</div>
  </section>
</template>

<script setup lang="ts">
import { CpsRebateAssetsProperty } from './config'

defineOptions({ name: 'CpsRebateAssets' })
const props = defineProps<{ property: CpsRebateAssetsProperty }>()

const assetItems = computed(() =>
  [
    { key: 'available', label: '可用返利', amount: '¥ 86.50', show: props.property.showAvailable },
    { key: 'frozen', label: '冻结中', amount: '¥ 24.80', show: props.property.showFrozen },
    { key: 'total', label: '累计返利', amount: '¥ 328.60', show: props.property.showTotal }
  ].filter((item) => item.show)
)
</script>

<style scoped lang="scss">
.rebate-assets-preview { border-radius: 8px; background: #fff; color: #333; }
header { display: flex; align-items: center; justify-content: space-between; font-size: 14px; }
.asset-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(70px, 1fr)); margin-top: 14px; }
.asset-item { display: flex; min-width: 0; flex-direction: column; align-items: center; gap: 5px; }
.asset-item + .asset-item { border-left: 1px solid #f0f0f0; }
.asset-item b { color: #f4513b; font-size: 15px; }
.asset-item span, .guest-hint { color: #888; font-size: 10px; }
.guest-hint { padding: 18px 0 4px; text-align: center; }
</style>
