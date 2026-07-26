<template>
  <el-alert title="草稿仅影响运行时配置，发布前必须通过当前指纹的连接测试" type="info" show-icon />
  <el-descriptions v-if="runtimePayload || draftPayload" :column="2" border class="mt-12px">
    <el-descriptions-item label="当前运行配置">{{ runtimeLabel }}</el-descriptions-item>
    <el-descriptions-item label="待发布草稿">{{ draftLabel }}</el-descriptions-item>
  </el-descriptions>
</template>
<script lang="ts" setup>
import { computed } from 'vue'
import type { PlatformOnboardingPayload } from '@/api/cps/platformOnboarding'
const props = defineProps<{ runtimePayload?: PlatformOnboardingPayload; draftPayload?: PlatformOnboardingPayload }>()
const label = (payload?: PlatformOnboardingPayload) => {
  if (!payload) return '无'
  const vendor = payload.vendors?.find((item) => item.vendorCode === payload.primaryVendorCode)
  const vendorLabel = vendor?.vendorName || payload.primaryVendorCode || '未设置主供应商'
  return `${payload.platform.platformName || payload.platform.platformCode} / ${vendorLabel}`
}
const runtimeLabel = computed(() => props.runtimePayload ? label(props.runtimePayload) : '未发布')
const draftLabel = computed(() => props.draftPayload ? `${label(props.draftPayload)}（可继续完善）` : '无')
</script>
