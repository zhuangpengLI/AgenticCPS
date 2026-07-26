<template>
  <el-dialog v-model="visible" title="返利规则" width="560px">
    <el-alert title="平台工作台仅支持平台默认和会员等级规则；全局及个人会员规则请在原有返利模块维护" type="warning" show-icon class="mb-12px" />
    <el-form :model="form" label-width="120px">
      <el-form-item label="会员等级"><MemberLevelSelect v-model="form.memberLevelId" /></el-form-item>
      <el-form-item label="返利比例" required><el-input-number v-model="form.rebateRate" :min="0" :max="100" :precision="2" /></el-form-item>
      <el-form-item label="最低金额"><el-input-number v-model="form.minRebateAmount" :min="0" /></el-form-item><el-form-item label="最高金额"><el-input-number v-model="form.maxRebateAmount" :min="0" /></el-form-item>
      <el-form-item label="优先级"><el-input-number v-model="form.priority" :min="0" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
  </el-dialog>
</template>
<script lang="ts" setup>
import { computed, reactive, watch } from 'vue'
import type { RebateRuleForm } from '@/api/cps/platformOnboarding'
import MemberLevelSelect from '@/views/member/level/components/MemberLevelSelect.vue'
const props = defineProps<{ modelValue: boolean; row?: RebateRuleForm; platformCode: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; save: [RebateRuleForm] }>()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const form = reactive<RebateRuleForm>({ platformCode: props.platformCode, status: 0, rebateRate: undefined })
watch(() => props.row, (row) => Object.assign(form, row || { platformCode: props.platformCode, status: 0, rebateRate: undefined }), { immediate: true })
const submit = () => { if (form.memberId) { ElMessage.error('不支持个人会员返利规则'); return }; if (form.rebateRate == null) { ElMessage.error('请输入返利比例'); return }; emit('save', { ...form }); visible.value = false }
</script>
