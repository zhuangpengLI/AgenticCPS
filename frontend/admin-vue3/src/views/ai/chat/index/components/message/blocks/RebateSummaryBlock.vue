<template>
  <div class="rebate-summary">
    <div v-for="metric in metrics" :key="metric.label" class="rebate-summary__metric">
      <span>{{ metric.label }}</span>
      <strong>{{ metric.value }}</strong>
    </div>
    <el-tag v-if="block.accountStatus" effect="plain" type="success">
      {{ block.accountStatus }}
    </el-tag>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessageBlock } from '@/api/ai/chat/message'

const props = defineProps<{ block: ChatMessageBlock }>()

const money = (value: unknown) => {
  if (value === undefined || value === null || value === '') return '—'
  const numeric = Number(value)
  return `¥${Number.isFinite(numeric) ? numeric.toFixed(2) : String(value)}`
}

const metrics = computed(() => [
  { label: '可用返利', value: money(props.block.availableBalance) },
  { label: '冻结返利', value: money(props.block.frozenBalance) },
  { label: '累计返利', value: money(props.block.totalRebate) },
  { label: '已提现', value: money(props.block.withdrawnAmount) }
])
</script>

<style scoped>
.rebate-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(100px, 1fr));
  gap: 10px;
  padding: 0 14px 14px;
}

.rebate-summary__metric {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.rebate-summary__metric span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.rebate-summary__metric strong {
  color: var(--el-color-primary);
  font-size: 18px;
}

@media (max-width: 768px) {
  .rebate-summary {
    grid-template-columns: repeat(2, minmax(100px, 1fr));
  }
}
</style>
