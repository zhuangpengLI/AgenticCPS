<template>
  <div v-if="blockList.length" class="message-blocks" data-testid="ai-message-blocks">
    <div class="message-blocks__toolbar">
      <el-button size="small" plain data-testid="ai-message-blocks-export" @click="exportBlocks">
        <Icon icon="ep:download" class="mr-4px" />导出分析结果
      </el-button>
    </div>
    <article
      v-for="block in blockList"
      :key="block.id"
      class="message-block"
      :data-block-type="block.type"
    >
      <header v-if="block.title || block.subtitle" class="message-block__header">
        <div>
          <h4 v-if="block.title">{{ block.title }}</h4>
          <p v-if="block.subtitle">{{ block.subtitle }}</p>
        </div>
        <el-tag v-if="block.type === 'PRODUCT_COMPARE'" size="small" effect="plain">比价</el-tag>
        <el-tag v-else-if="block.type === 'SELECTION_REPORT'" size="small" type="success" effect="plain">
          选品报告
        </el-tag>
        <el-tag v-else-if="block.type === 'ALTERNATIVES_REPORT'" size="small" type="warning" effect="plain">
          高佣替代品
        </el-tag>
        <el-tag v-else-if="block.type === 'GOODS_ANALYSIS'" size="small" type="success" effect="plain">
          商品深度分析
        </el-tag>
        <el-tag v-else-if="block.type === 'ORDER_PROFILE'" size="small" type="info" effect="plain">
          成交画像
        </el-tag>
        <el-tag v-else-if="block.type === 'ORDER_TREND'" size="small" type="info" effect="plain">
          成交趋势
        </el-tag>
      </header>

      <ProductBlock
        v-if="block.type === 'PRODUCT_RECOMMEND' || block.type === 'PRODUCT_COMPARE'"
        :items="block.items || []"
        :compare="block.type === 'PRODUCT_COMPARE'"
      />
      <RebateSummaryBlock v-else-if="block.type === 'REBATE_SUMMARY'" :block="block" />
      <FollowUpBlock v-else-if="block.type === 'FOLLOW_UP'" :block="block" />
      <SelectionReportBlock
        v-else-if="block.type === 'SELECTION_REPORT'"
        :block="block"
        :review-context-id="reviewContextId(block)"
      />
      <SelectionReportBlock
        v-else-if="block.type === 'ALTERNATIVES_REPORT'"
        :block="block"
        :review-context-id="reviewContextId(block)"
      />
      <SelectionReportBlock
        v-else-if="block.type === 'GOODS_ANALYSIS'"
        :block="block"
        :review-context-id="reviewContextId(block)"
      />
      <OrderProfileBlock v-else-if="block.type === 'ORDER_PROFILE'" :block="block" />
      <OrderTrendBlock v-else-if="block.type === 'ORDER_TREND'" :block="block" />
      <div v-else class="message-block__fallback">
        <p v-if="block.summary || block.content || block.subtitle">
          {{ block.summary || block.content || block.subtitle }}
        </p>
        <pre v-else-if="fallbackContent(block)">{{ fallbackContent(block) }}</pre>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import type { ChatMessageBlock } from '@/api/ai/chat/message'
import FollowUpBlock from './blocks/FollowUpBlock.vue'
import ProductBlock from './blocks/ProductBlock.vue'
import RebateSummaryBlock from './blocks/RebateSummaryBlock.vue'
import SelectionReportBlock from './blocks/SelectionReportBlock.vue'
import OrderProfileBlock from './blocks/OrderProfileBlock.vue'
import OrderTrendBlock from './blocks/OrderTrendBlock.vue'

const props = defineProps<{ blocks?: ChatMessageBlock[]; messageId?: number }>()
const blockList = computed(() => props.blocks || [])

const reviewContextId = (block: ChatMessageBlock) => {
  if (!props.messageId || props.messageId <= 0 || !block.id) return ''
  return `${props.messageId}:${block.id}`
}

const fallbackContent = (block: ChatMessageBlock) => {
  const { id, version, type, title, subtitle, summary, content, actions, ...rest } = block
  if (!Object.keys(rest).length) return ''
  try {
    return JSON.stringify(rest, null, 2)
  } catch {
    return ''
  }
}

const csvCell = (value: unknown) => {
  let text = ''
  if (value !== null && value !== undefined) {
    if (typeof value === 'object') {
      try {
        text = JSON.stringify(value)
      } catch {
        text = String(value)
      }
    } else {
      text = String(value)
    }
  }
  return `"${text.replaceAll('"', '""').replaceAll('\n', ' ')}"`
}

const exportBlocks = () => {
  if (!blockList.value.length) return
  const header = ['类型', '商品标题', '平台', '商品ID', '到手价', '佣金比例', '佣金金额', '月销', '评分', '摘要/证据']
  const rows = blockList.value.flatMap((block) => {
    const items = Array.isArray(block.items) && block.items.length ? block.items : [undefined]
    return items.map((item) => [
      block.type,
      item?.title || '',
      item?.platformName || item?.platformCode || '',
      item?.goodsId || item?.goodsSign || '',
      item?.actualPrice ?? '',
      item?.commissionRate ?? '',
      item?.commissionAmount ?? item?.commissionDelta ?? '',
      item?.monthSales ?? '',
      item?.resonanceScore ?? item?.alternativeScore ?? '',
      item?.evidence || block.summary || block.content || block.title || ''
    ])
  })
  const csv = `\ufeff${[header, ...rows].map((row) => row.map(csvCell).join(',')).join('\r\n')}`
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `cps-ai-selection-${new Date().toISOString().slice(0, 10)}.csv`
  anchor.click()
  URL.revokeObjectURL(url)
}
</script>

<style scoped>
.message-blocks {
  display: grid;
  gap: 12px;
  margin: 4px 0 10px;
  min-width: min(720px, 68vw);
}

.message-blocks__toolbar {
  display: flex;
  justify-content: flex-end;
}

.message-block {
  overflow: hidden;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 12px;
  background: var(--el-bg-color);
}

.message-block__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px 10px;
}

.message-block__header h4 {
  margin: 0;
  color: var(--el-text-color-primary);
  font-size: 15px;
}

.message-block__header p {
  margin: 5px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.message-block__fallback {
  padding: 0 16px 14px;
  color: var(--el-text-color-regular);
  line-height: 1.6;
}

.message-block__fallback p,
.message-block__fallback pre {
  margin: 0;
  white-space: pre-wrap;
}

@media (max-width: 768px) {
  .message-blocks {
    min-width: 0;
    width: 100%;
  }
}
</style>
