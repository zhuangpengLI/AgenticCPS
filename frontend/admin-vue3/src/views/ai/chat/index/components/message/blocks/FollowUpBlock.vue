<template>
  <div class="follow-up">
    <div v-if="block.command || block.tpwd" class="follow-up__command">
      <span>{{ block.commandLabel || '推广口令' }}</span>
      <code>{{ block.command || block.tpwd }}</code>
      <el-button size="small" @click="copyText(String(block.command || block.tpwd))">复制</el-button>
    </div>
    <div class="follow-up__meta">
      <span v-if="block.actualPrice !== undefined">到手价 ¥{{ block.actualPrice }}</span>
      <span v-if="block.commissionAmount !== undefined">预估佣金 ¥{{ block.commissionAmount }}</span>
    </div>
    <div class="follow-up__actions">
      <a
        v-if="promotionUrl"
        :href="promotionUrl"
        target="_blank"
        rel="noopener noreferrer"
      >打开推广链接</a>
      <el-button v-if="promotionUrl" size="small" @click="copyText(promotionUrl)">复制链接</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'
import type { ChatMessageBlock } from '@/api/ai/chat/message'

const props = defineProps<{ block: ChatMessageBlock }>()
const message = useMessage()
const { copy } = useClipboard({ legacy: true })

const promotionUrl = computed(() => {
  const url = props.block.shortUrl || props.block.mobileUrl || props.block.promotionUrl
  return typeof url === 'string' && /^https?:\/\//i.test(url) ? url : ''
})

const copyText = async (value: string) => {
  await copy(value)
  message.success('复制成功！')
}
</script>

<style scoped>
.follow-up {
  display: grid;
  gap: 12px;
  padding: 0 14px 14px;
}

.follow-up__command {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.follow-up__command code {
  min-width: 0;
  flex: 1;
  overflow-wrap: anywhere;
  color: var(--el-color-primary);
}

.follow-up__meta,
.follow-up__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.follow-up__actions a {
  color: var(--el-color-primary);
  text-decoration: none;
}
</style>
