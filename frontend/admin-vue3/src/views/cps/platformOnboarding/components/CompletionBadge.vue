<template>
  <div class="inline-flex items-center gap-2" :title="missingTitle">
    <el-progress
      :percentage="percentage"
      :stroke-width="8"
      :show-text="false"
      class="w-70px"
      :status="percentage === 100 ? 'success' : undefined"
    />
    <span class="text-xs" :class="percentage === 100 ? 'text-green-600' : 'text-gray-600'">
      {{ percentage }}%
    </span>
    <el-tag v-if="missingItems.length" type="warning" size="small">
      待补 {{ missingItems.length }} 项
    </el-tag>
    <el-tag v-else type="success" size="small">已完成</el-tag>
  </div>
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    percentage?: number
    missingItems?: string[]
  }>(),
  {
    percentage: 0,
    missingItems: () => []
  }
)

const percentage = computed(() => Math.min(100, Math.max(0, Math.round(props.percentage ?? 0))))
const missingItems = computed(() => props.missingItems ?? [])
const missingTitle = computed(() =>
  missingItems.value.length ? `待补配置：${missingItems.value.join('、')}` : '配置完整'
)
</script>
