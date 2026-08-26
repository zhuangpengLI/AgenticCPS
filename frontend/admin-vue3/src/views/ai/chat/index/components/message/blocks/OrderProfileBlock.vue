<template>
  <div class="order-profile">
    <el-alert
      v-if="block.error"
      title="成交画像分析失败"
      :description="String(block.error)"
      type="error"
      :closable="false"
      show-icon
    />

    <p v-if="summary" class="order-profile__summary">{{ summary }}</p>

    <div class="order-profile__metrics">
      <div v-for="metric in metrics" :key="metric.label" class="order-profile__metric">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </div>
    </div>

    <section v-if="platformRows.length">
      <h5>平台分布</h5>
      <div class="order-profile__rows">
        <div v-for="row in platformRows" :key="row.key" class="order-profile__row">
          <span>{{ row.name }}</span>
          <span>{{ row.orderCount }} 单 · ¥{{ row.gmv }}</span>
        </div>
      </div>
    </section>

    <section v-if="priceBandRows.length">
      <h5>价格带</h5>
      <div class="order-profile__tags">
        <el-tag v-for="row in priceBandRows" :key="row.key" size="small" effect="plain">
          {{ row.name }}：{{ row.orderCount }} 单
        </el-tag>
      </div>
    </section>

    <section v-if="topProducts.length">
      <h5>成交商品</h5>
      <ol class="order-profile__products">
        <li v-for="(product, index) in topProducts" :key="productKey(product, index)">
          <span>{{ product.title || product.name || '未命名商品' }}</span>
          <small>{{ product.orderCount || 0 }} 单 · ¥{{ number(product.gmv ?? product.amount) }}</small>
        </li>
      </ol>
    </section>

    <section v-if="insights.length">
      <h5>分析结论</h5>
      <ul class="order-profile__list">
        <li v-for="(insight, index) in insights" :key="index">{{ display(insight) }}</li>
      </ul>
    </section>

    <el-empty
      v-if="!platformRows.length && !priceBandRows.length && !topProducts.length && !block.error"
      description="当前时间范围内暂无可用订单"
      :image-size="64"
    />

    <el-alert
      v-if="limitations.length"
      title="数据边界"
      :description="limitations.map(display).join('；')"
      type="info"
      :closable="false"
      show-icon
    />
  </div>
</template>

<script setup lang="ts">
import type { ChatMessageBlock } from '@/api/ai/chat/message'

const props = defineProps<{ block: ChatMessageBlock }>()
type Row = Record<string, any> & { key: string; name: string; orderCount: string | number; gmv: string | number }

const asRows = (value: unknown): Row[] => {
  if (!Array.isArray(value)) return []
  return value.map((item, index) => {
    const row = (item && typeof item === 'object' ? item : {}) as Record<string, any>
    return {
      ...row,
      key: String(row.platform || row.platformName || row.name || row.band || index),
      name: String(row.platform || row.platformName || row.name || row.band || '未分类'),
      orderCount: row.orderCount ?? row.orders ?? 0,
      gmv: number(row.gmv ?? row.amount ?? 0)
    }
  })
}

const number = (value: unknown) => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed.toFixed(2).replace(/\.00$/, '') : String(value ?? '-')
}

const display = (value: unknown): string => {
  if (value === null || value === undefined) return ''
  if (typeof value !== 'object') return String(value)
  return Object.entries(value as Record<string, unknown>)
    .map(([key, item]) => `${key}：${display(item)}`)
    .join('，')
}

const summary = computed(() => {
  const days = props.block.days
  const orders = props.block.analyzedOrders
  return days || orders ? `统计近 ${days || '-'} 天，共分析 ${orders || 0} 笔订单。` : ''
})

const metrics = computed(() => [
  { label: '成交金额', value: `¥${number(props.block.gmv)}` },
  { label: '预计返利', value: `¥${number(props.block.estimatedRebate)}` },
  { label: '实际返利', value: `¥${number(props.block.realRebate)}` },
  { label: '客单价', value: `¥${number(props.block.averageOrderValue)}` }
])

const platformRows = computed(() => asRows(props.block.platformBreakdown))
const priceBandRows = computed(() => asRows(props.block.priceBandBreakdown))
const topProducts = computed(() => (Array.isArray(props.block.topProducts) ? props.block.topProducts : []) as Record<string, any>[])
const insights = computed(() => (Array.isArray(props.block.insights) ? props.block.insights : []) as unknown[])
const limitations = computed(() => (Array.isArray(props.block.dataLimitations) ? props.block.dataLimitations : []) as unknown[])

const productKey = (item: Record<string, any>, index: number) => String(item.goodsId || item.itemId || item.title || index)
</script>

<style scoped>
.order-profile { display: grid; gap: 14px; padding: 0 14px 14px; }
.order-profile__summary { margin: 0; padding: 12px; border-radius: 10px; background: var(--el-color-primary-light-9); line-height: 1.6; }
.order-profile__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.order-profile__metric { display: grid; gap: 5px; padding: 10px; border-radius: 10px; background: var(--el-fill-color-lighter); }
.order-profile__metric span, .order-profile__row, .order-profile__products small { color: var(--el-text-color-secondary); font-size: 12px; }
.order-profile__metric strong { color: var(--el-text-color-primary); font-size: 16px; }
.order-profile h5 { margin: 0 0 8px; color: var(--el-text-color-primary); }
.order-profile__rows { display: grid; gap: 6px; }
.order-profile__row { display: flex; justify-content: space-between; gap: 10px; padding: 8px 10px; border-radius: 8px; background: var(--el-fill-color-lighter); }
.order-profile__tags { display: flex; flex-wrap: wrap; gap: 8px; }
.order-profile__products, .order-profile__list { margin: 0; padding-left: 20px; line-height: 1.7; }
.order-profile__products li { display: flex; justify-content: space-between; gap: 10px; }
@media (max-width: 768px) { .order-profile__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); } .order-profile__products li { display: grid; gap: 2px; } }
</style>
