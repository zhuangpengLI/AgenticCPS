<template>
  <!-- eslint-disable vue/no-mutating-props -->
  <div>
    <el-alert title="主供应商用于运行时请求；备用供应商仅保存为候选配置，当前不自动故障切换" type="info" show-icon class="mb-12px" />
    <el-button type="primary" @click="openEditor()">添加供应商</el-button>
    <el-table :data="draft.vendors" class="mt-12px" border>
      <el-table-column prop="vendorName" label="供应商" />
      <el-table-column prop="vendorCode" label="编码" />
      <el-table-column label="角色"><template #default="{ row }">{{ row.vendorCode === draft.primaryVendorCode ? '主供应商' : '备用供应商' }}</template></el-table-column>
      <el-table-column label="凭证"><template #default="{ row }">{{ credentialsConfigured(row) ? '已配置（已脱敏）' : '未配置' }}</template></el-table-column>
      <el-table-column label="操作" width="220"><template #default="{ row, $index }"><el-button link @click="openEditor(row, $index)">编辑</el-button><el-button link type="danger" @click="remove($index)">移除</el-button><el-button link @click="testVendor(row)">测试</el-button></template></el-table-column>
    </el-table>
    <el-form-item label="主供应商" class="mt-16px" required><el-select v-model="draft.primaryVendorCode"><el-option v-for="row in draft.vendors" :key="row.vendorCode" :label="row.vendorName || row.vendorCode" :value="row.vendorCode" /></el-select></el-form-item>
    <CheckResultPanel :result="vendorResult" />
    <VendorEditorDialog v-model="dialogVisible" :vendor="editingVendor" :descriptor="editingDescriptor" :descriptors="availableDescriptors" :platform-code="draft.platform.platformCode || draft.platformCode" @save="saveVendor" />
  </div>
</template>
<script lang="ts" setup>
/* eslint-disable vue/no-mutating-props */
import { computed, ref } from 'vue'
import type { PlatformOnboardingDraft, VendorDescriptor, VendorForm, OnboardingCheckResult } from '@/api/cps/platformOnboarding'
import CheckResultPanel from './CheckResultPanel.vue'
import VendorEditorDialog from './VendorEditorDialog.vue'

const props = defineProps<{ draft: PlatformOnboardingDraft; descriptors?: VendorDescriptor[] }>()
const availableDescriptors = computed(() => props.descriptors?.filter((item) => !props.draft.platform.platformCode || item.platformCode === props.draft.platform.platformCode) || [])
const dialogVisible = ref(false)
const editingVendor = ref<VendorForm>()
const editingIndex = ref(-1)
const editingDescriptor = ref<VendorDescriptor>()
const vendorResult = ref<OnboardingCheckResult>()
const fieldConfigured = (vendor: VendorForm, fieldName: string) => {
  if (vendor.configuredFields?.includes(fieldName)) return true
  if (fieldName === 'appKey') return Boolean(vendor.appKeyConfigured || vendor.apiKeyConfigured || vendor.appKey || vendor.apiKey)
  if (fieldName === 'appSecret') return Boolean(vendor.appSecretConfigured || vendor.appSecret)
  if (fieldName === 'authToken') return Boolean(vendor.authTokenConfigured || vendor.authToken)
  if (fieldName === 'apiBaseUrl') return Boolean(vendor.apiBaseUrlConfigured || vendor.apiBaseUrl)
  return false
}
const credentialsConfigured = (vendor: VendorForm) => {
  const descriptor = availableDescriptors.value.find((item) => item.vendorCode === vendor.vendorCode)
  if (!descriptor) {
    return Boolean(vendor.appKeyConfigured || vendor.apiKeyConfigured || vendor.appSecretConfigured || vendor.authTokenConfigured || vendor.appKey || vendor.apiKey || vendor.appSecret || vendor.authToken)
  }
  return descriptor.configSchema.fields
    .filter((field) => field.required && field.sensitive)
    .every((field) => fieldConfigured(vendor, field.name))
}
const openEditor = (vendor?: VendorForm, index = -1) => { editingVendor.value = vendor; editingIndex.value = index; editingDescriptor.value = availableDescriptors.value.find((item) => item.vendorCode === vendor?.vendorCode); dialogVisible.value = true }
const saveVendor = (vendor: VendorForm) => { if (editingIndex.value >= 0) props.draft.vendors.splice(editingIndex.value, 1, vendor); else props.draft.vendors.push(vendor); if (!props.draft.primaryVendorCode) props.draft.primaryVendorCode = vendor.vendorCode }
const remove = (index: number) => { const removed = props.draft.vendors.splice(index, 1)[0]; if (removed?.vendorCode === props.draft.primaryVendorCode) props.draft.primaryVendorCode = props.draft.vendors[0]?.vendorCode || '' }
const testVendor = (vendor: VendorForm) => { vendorResult.value = { success: Boolean(vendor.vendorCode && credentialsConfigured(vendor)), items: [{ section: vendor.vendorName || vendor.vendorCode, code: 'VENDOR_PREFLIGHT', message: '凭证预检已完成；真实连接测试请在检测与启用步骤执行' }] } }
const validate = async () => {
  const codes = props.draft.vendors.map((vendor) => vendor.vendorCode.trim())
  const primary = props.draft.vendors.find((vendor) => vendor.vendorCode === props.draft.primaryVendorCode)
  const unique = new Set(codes).size === codes.length
  const supported = !availableDescriptors.value.length || props.draft.vendors.every((vendor) => availableDescriptors.value.some((item) => item.vendorCode === vendor.vendorCode))
  return Boolean(props.draft.primaryVendorCode && primary?.status === 1 && props.draft.vendors.length >= 1 && unique && supported)
}
defineExpose({ validate })
</script>
