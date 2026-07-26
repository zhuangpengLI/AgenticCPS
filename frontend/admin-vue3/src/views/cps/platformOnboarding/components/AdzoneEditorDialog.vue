<template>
  <el-dialog v-model="visible" title="推广位" width="560px" destroy-on-close>
    <el-form :model="form" label-width="120px">
      <el-form-item label="推广位 ID" required><el-input v-model="form.adzoneId" /></el-form-item>
      <el-form-item label="名称"><el-input v-model="form.adzoneName" /></el-form-item>
      <el-form-item label="类型"><el-select v-model="form.adzoneType"><el-option label="通用" value="GENERAL" /><el-option label="渠道" value="CHANNEL" /><el-option label="会员" value="MEMBER" /></el-select></el-form-item>
      <el-form-item label="渠道关系 ID" v-if="form.adzoneType === 'CHANNEL'"><el-input v-model="form.externalRelationId" /></el-form-item>
      <el-form-item label="会员 Special ID" v-if="form.adzoneType === 'MEMBER'"><el-input v-model="form.externalSpecialId" /></el-form-item>
      <el-form-item label="启用"><el-switch v-model="enabled" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
  </el-dialog>
</template>
<script lang="ts" setup>
import { computed, reactive, watch } from 'vue'
import type { AdzoneForm } from '@/api/cps/platformOnboarding'
import { normalizeAdzoneRow } from '@/views/cps/components/adzoneRules'
const props = defineProps<{ modelValue: boolean; row?: AdzoneForm; platformCode: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; save: [AdzoneForm] }>()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const form = reactive<AdzoneForm>({ platformCode: props.platformCode, adzoneId: '', adzoneType: 'GENERAL', isDefault: 0, status: 0 })
const enabled = computed({ get: () => form.status === 0, set: (v: boolean) => (form.status = v ? 0 : 1) })
watch(() => props.row, (row) => Object.assign(form, row || { platformCode: props.platformCode, adzoneId: '', adzoneType: 'GENERAL', isDefault: 0, status: 0 }), { immediate: true })
const submit = () => { emit('save', normalizeAdzoneRow({ ...form })); visible.value = false }
</script>
