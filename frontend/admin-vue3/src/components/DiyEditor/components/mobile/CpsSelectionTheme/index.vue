<template>
  <section class="selection-theme-preview">
    <header>
      <div><strong>{{ property.title }}</strong><small v-if="theme">{{ theme.themeName }}</small></div>
      <span v-if="property.showMore">更多 ›</span>
    </header>
    <div v-if="loading" class="state"><el-icon class="is-loading"><Loading /></el-icon> 正在加载选品</div>
    <el-alert v-else-if="error" :title="error" type="warning" :closable="false" show-icon />
    <el-empty v-else-if="!items.length" description="请选择已发布选品主题" :image-size="48" />
    <div v-else class="goods-grid" :style="{ gridTemplateColumns: `repeat(${safeColumns}, minmax(0, 1fr))` }">
      <article v-for="item in items" :key="item.id">
        <el-image :src="item.mainPic" fit="cover"><template #error><div class="image-empty"><Icon icon="ep:picture" /></div></template></el-image>
        <b>{{ item.title || `商品 ${item.goodsId}` }}</b>
        <span>券后 ¥{{ formatAmount(item.actualPrice) }}</span>
        <small v-if="item.commissionAmount">预计返利 ¥{{ formatAmount(item.commissionAmount) }}</small>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { CpsSelectionThemeApi, CpsSelectionThemeItemVO, CpsSelectionThemeVO } from '@/api/cps/selectionTheme'
import { CpsSelectionThemeProperty } from './config'

defineOptions({ name: 'CpsSelectionTheme' })
const props = defineProps<{ property: CpsSelectionThemeProperty }>()
const theme = ref<CpsSelectionThemeVO>()
const items = ref<CpsSelectionThemeItemVO[]>([])
const loading = ref(false)
const error = ref('')
let requestVersion = 0
const safeColumns = computed(() => Math.min(3, Math.max(1, props.property.columns || 2)))
const formatAmount = (amount?: number) => Number(amount || 0).toFixed(2)

watch(
  () => [props.property.themeId, props.property.limit] as const,
  async () => {
    const version = ++requestVersion
    theme.value = undefined
    items.value = []
    error.value = ''
    loading.value = false
    if (!props.property.themeId) return
    loading.value = true
    try {
      const [themeResult, itemResult] = await Promise.all([
        CpsSelectionThemeApi.getTheme(props.property.themeId),
        CpsSelectionThemeApi.listItems(props.property.themeId)
      ])
      if (version !== requestVersion) return
      theme.value = themeResult
      items.value = itemResult.filter((item) => item.status === 'ENABLED').slice(0, props.property.limit || 6)
      if (themeResult.status !== 'PUBLISHED') error.value = '选品主题已下线/不可用'
    } catch {
      if (version === requestVersion) error.value = '选品主题已下线或暂时无法加载'
    } finally {
      if (version === requestVersion) loading.value = false
    }
  },
  { immediate: true, deep: true }
)
</script>

<style scoped lang="scss">
.selection-theme-preview { border-radius: 8px; color: #333; }
header { display: flex; align-items: center; justify-content: space-between; font-size: 14px; }
header div { display: flex; min-width: 0; align-items: baseline; gap: 6px; }
header small, header span { overflow: hidden; color: #999; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.state { padding: 24px 0; color: #999; font-size: 11px; text-align: center; }
.goods-grid { display: grid; gap: 8px; margin-top: 12px; }
article { display: flex; min-width: 0; flex-direction: column; overflow: hidden; border-radius: 6px; background: #fff; }
.el-image, .image-empty { width: 100%; aspect-ratio: 1; background: #f3f3f3; }
.image-empty { display: flex; align-items: center; justify-content: center; color: #bbb; }
article b, article span, article small { overflow: hidden; padding: 0 7px; text-overflow: ellipsis; white-space: nowrap; }
article b { margin-top: 7px; font-size: 10px; }
article span { margin-top: 4px; color: #f4513b; font-size: 11px; }
article small { padding-bottom: 7px; color: #999; font-size: 9px; }
</style>
