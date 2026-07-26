<template>
  <el-form ref="formRef" :model="draft.platform" label-width="110px">
    <el-alert v-if="unsupported" type="warning" title="该平台暂无可用适配器，请先选择已注册的平台能力" show-icon class="mb-12px" />
    <el-form-item label="平台编码" prop="platformCode" required>
      <el-select v-model="draft.platform.platformCode" :disabled="mode === 'RECONFIGURE'" filterable @change="onPlatformChange">
        <el-option v-for="item in capabilities" :key="item.platformCode" :label="item.platformName || item.platformCode" :value="item.platformCode" />
      </el-select>
    </el-form-item>
    <el-form-item label="平台名称" required><el-input v-model="draft.platform.platformName" /></el-form-item>
    <el-form-item label="Logo"><el-input v-model="draft.platform.platformLogo" /></el-form-item>
    <el-form-item label="服务费率"><el-input-number v-model="draft.platform.platformServiceRate" :min="0" :max="100" :precision="2" /></el-form-item>
    <el-form-item label="排序"><el-input-number v-model="draft.platform.sort" :min="0" /></el-form-item>
    <el-form-item label="备注"><el-input v-model="draft.platform.remark" type="textarea" /></el-form-item>
  </el-form>
</template>
<script lang="ts" setup>
import { computed, onMounted } from 'vue'
import type { PlatformCapability, PlatformOnboardingDraft } from '@/api/cps/platformOnboarding'
import { PlatformOnboardingApi } from '@/api/cps/platformOnboarding'

const props = defineProps<{ draft: PlatformOnboardingDraft; mode: 'CREATE' | 'RECONFIGURE' }>()
const capabilities = ref<PlatformCapability[]>([])
const unsupported = computed(() => Boolean(props.draft.platform.platformCode) && !capabilities.value.some((item) => item.platformCode === props.draft.platform.platformCode))
const onPlatformChange = (code: string) => {
  props.draft.platformCode = code
  const capability = capabilities.value.find((item) => item.platformCode === code)
  if (capability && !props.draft.platform.platformName) props.draft.platform.platformName = capability.platformName || code
}
const validate = async () => {
  if (!props.draft.platform.platformCode?.trim() || !props.draft.platform.platformName?.trim()) return false
  return !unsupported.value
}
defineExpose({ validate })
onMounted(async () => {
  capabilities.value = (await PlatformOnboardingApi.getPlatformCapabilities()) || []
})
</script>
