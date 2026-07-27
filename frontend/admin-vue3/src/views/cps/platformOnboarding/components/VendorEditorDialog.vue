<template>
  <el-dialog v-model="visible" :title="editing ? '编辑供应商' : '添加供应商'" width="640px" destroy-on-close>
    <el-form :model="form" label-width="120px">
      <el-form-item label="供应商编码" required>
        <el-select v-if="!editing && descriptors?.length" v-model="form.vendorCode" filterable @change="applyDescriptor">
          <el-option v-for="item in descriptors" :key="item.vendorCode" :label="item.vendorCode" :value="item.vendorCode" />
        </el-select>
        <el-input v-else v-model="form.vendorCode" />
      </el-form-item>
      <el-form-item label="供应商名称" required><el-input v-model="form.vendorName" /></el-form-item>
      <el-form-item label="供应商类型"><el-input v-model="form.vendorType" /></el-form-item>
      <el-form-item v-for="field in fields" :key="field.name" :label="field.name" :required="field.required">
        <el-input v-if="field.sensitive" v-model="secretValues[field.name]" type="password" show-password :placeholder="configured(field.name) ? '已配置（留空则保持不变）' : '请输入凭证'" autocomplete="new-password" />
        <el-input v-else v-model="form[field.name]" />
      </el-form-item>
      <el-form-item label="优先级"><el-input-number v-model="form.priority" :min="0" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="enabled" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
  </el-dialog>
</template>
<script lang="ts" setup>
import { computed, reactive, ref, watch } from 'vue'
import type { VendorConfigField, VendorDescriptor, VendorForm } from '@/api/cps/platformOnboarding'

const props = defineProps<{ modelValue: boolean; vendor?: VendorForm; descriptor?: VendorDescriptor; descriptors?: VendorDescriptor[]; platformCode: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; save: [VendorForm] }>()
const visible = computed({ get: () => props.modelValue, set: (value) => emit('update:modelValue', value) })
const editing = computed(() => Boolean(props.vendor))
const activeDescriptor = computed(() => props.descriptor || props.descriptors?.find((item) => item.vendorCode === form.vendorCode))
const fields = computed<VendorConfigField[]>(() => activeDescriptor.value?.configSchema?.fields || [])
const emptyForm = () => ({ vendorCode: '', vendorName: '', vendorType: 'aggregator', platformCode: props.platformCode, priority: 0, status: 1 })
const form = reactive<any>(emptyForm())
const secretValues = reactive<Record<string, string>>({})
const enabled = computed({ get: () => form.status === 1, set: (v: boolean) => (form.status = v ? 1 : 0) })
const applyDescriptor = (vendorCode: string) => {
  const descriptor = props.descriptors?.find((item) => item.vendorCode === vendorCode)
  if (descriptor) {
    form.vendorType = descriptor.vendorType
    form.platformCode = descriptor.platformCode || props.platformCode
    form.vendorName = descriptor.vendorCode
  }
}
const configured = (name: string) => Boolean(props.vendor?.configuredFields?.includes(name) || (props.vendor as any)?.[`${name}Configured`])
watch(() => props.vendor, (value) => {
  Object.assign(form, emptyForm(), value || {})
  Object.keys(secretValues).forEach((key) => delete secretValues[key])
}, { immediate: true })
const submit = () => {
  const next = { ...form }
  fields.value.filter((field) => field.sensitive).forEach((field) => {
    if (secretValues[field.name]) next[field.name] = secretValues[field.name]
  })
  const builtIn = new Set(['vendorCode', 'vendorName', 'vendorType', 'platformCode', 'appKey', 'appSecret', 'authToken', 'apiKey', 'apiBaseUrl', 'defaultAdzoneId', 'priority', 'status', 'remark', 'extraConfig'])
  const schemaExtras = fields.value
    .filter((field) => !builtIn.has(field.name) && !field.sensitive && form[field.name] !== undefined && form[field.name] !== '')
    .reduce<Record<string, unknown>>((result, field) => ({ ...result, [field.name]: form[field.name] }), {})
  if (Object.keys(schemaExtras).length) {
    let existing: Record<string, unknown> = {}
    try { existing = form.extraConfig ? JSON.parse(form.extraConfig) : {} } catch { existing = {} }
    next.extraConfig = JSON.stringify({ ...existing, ...schemaExtras })
  }
  emit('save', next as VendorForm)
  visible.value = false
}
</script>
