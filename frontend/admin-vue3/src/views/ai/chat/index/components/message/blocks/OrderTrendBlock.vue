<template>
  <div class="order-trend">
    <el-alert
      v-if="block.error"
      title="趋势分析失败"
      :description="String(block.error)"
      type="error"
      :closable="false"
      show-icon
    />

    <div class="order-trend__metrics">
      <div v-for="metric in metrics" :key="metric.label" class="order-trend__metric">
        <span>{{ metric.label }}</span>
        <strong>{{ metric.value }}</strong>
      </div>
    </div>

    <section v-if="points.length">
      <h5>成交走势</h5>
      <div class="order-trend__points">
        <div v-for="point in points" :key="String(point.period)" class="order-trend__point">
          <div class="order-trend__point-head">
            <span>{{ point.period || '时间未知' }}</span>
            <span>{{ point.orderCount || 0 }} 单 · ¥{{ number(point.gmv) }}</span>
          </div>
          <el-progress :percentage="percentage(point.gmv)" :show-text="false" :stroke-width="7" />
        </div>
      </div>
    </section>

    <el-empty
      v-if="!points.length && !block.error"
      description="当前时间范围内暂无可用订单"
      :image-size="64"
    />

    <section v-if="insights.length">
      <h5>趋势结论</h5>
      <ul class="order-trend__insights">
        <li v-for="(insight, index) in insights" :key="index">{{ display(insight) }}</li>
      </ul>
    </section>

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
type TrendPoint = Record<string, unknown> & { period?: string; orderCount?: number; gmv?: string | number }

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

const points = computed(
  () => (Array.isArray(props.block.points) ? props.block.points : []) as TrendPoint[]
)
const insights = computed(() => (Array.isArray(props.block.insights) ? props.block.insights : []) as unknown[])
const limitations = computed(
  () => (Array.isArray(props.block.dataLimitations) ? props.block.dataLimitations : []) as unknown[]
)
const maxGmv = computed(() => Math.max(0, ...points.value.map((point) => Number(point.gmv) || 0)))
const percentage = (value: unknown) =>
  maxGmv.value <= 0 ? 0 : Math.max(0, Math.min(100, Math.round(((Number(value) || 0) / maxGmv.value) * 100)))
const metrics = computed(() => [
  { label: '统计周期', value: `${props.block.days || '-'} 天` },
  { label: '订单数', value: `${props.block.analyzedOrders || 0} 单` },
  { label: '成交金额', value: `¥${number(props.block.totalGmv)}` },
  { label: '实际返利', value: `¥${number(props.block.totalRealRebate)}` }
])
</script>

<style scoped>
.order-trend { display: grid; gap: 14px; padding: 0 14px 14px; }
.order-trend__metrics { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 8px; }
.order-trend__metric { display: grid; gap: 5px; padding: 10px; border-radius: 10px; background: var(--el-fill-color-lighter); }
.order-trend__metric span, .order-trend__point-head { color: var(--el-text-color-secondary); font-size: 12px; }
.order-trend__metric strong { color: var(--el-text-color-primary); font-size: 16px; }
.order-trend h5 { margin: 0 0 8px; color: var(--el-text-color-primary); }
.order-trend__points { display: grid; gap: 9px; }
.order-trend__point { display: grid; gap: 5px; }
.order-trend__point-head { display: flex; justify-content: space-between; gap: 10px; }
.order-trend__insights { margin: 0; padding-left: 20px; line-height: 1.7; }
@media (max-width: 768px) { .order-trend__metrics { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
</style>
