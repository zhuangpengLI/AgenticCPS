<template>
  <div v-if="executions?.length" class="mb-8px flex flex-col gap-6px" data-testid="tool-executions">
    <div
      v-for="execution in executions"
      :key="execution.executionId"
      class="flex items-center gap-6px rounded-6px bg-[var(--el-bg-color)] px-9px py-6px text-13px"
    >
      <Icon
        :icon="statusIcon(execution.status)"
        :class="statusClass(execution.status)"
        class="shrink-0"
      />
      <span>{{ friendly(execution.message || execution.label) }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ToolExecutionVO } from '@/api/ai/chat/message'
import { toFriendlyToolText } from '../../toolActions'

defineProps<{
  executions?: ToolExecutionVO[]
}>()

const friendly = (value?: string) => toFriendlyToolText(value)

const statusIcon = (status: string) => {
  if (status === 'SUCCEEDED' || status === 'SUCCESS') return 'ep:circle-check-filled'
  if (status === 'FAILED' || status === 'FAILURE') return 'ep:circle-close-filled'
  return 'ep:loading'
}

const statusClass = (status: string) => {
  if (status === 'SUCCEEDED' || status === 'SUCCESS') return 'text-[var(--el-color-success)]'
  if (status === 'FAILED' || status === 'FAILURE') return 'text-[var(--el-color-danger)]'
  return 'animate-spin text-[var(--el-color-primary)]'
}
</script>
