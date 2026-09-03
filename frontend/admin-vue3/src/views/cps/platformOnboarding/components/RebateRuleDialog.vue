<template>
  <el-dialog v-model="visible" title="返利规则" width="560px">
    <el-alert title="平台工作台仅支持平台默认和会员等级规则；全局及个人会员规则请在原有返利模块维护" type="warning" show-icon class="mb-12px" />
    <el-form :model="form" label-width="120px">
      <el-form-item label="返利比例" required><el-input-number v-model="form.rebateRate" :min="0" :max="100" :precision="2" /></el-form-item>
      <el-form-item label="优先级" required><el-input-number v-model="form.priority" :min="0" :precision="0" /></el-form-item>
      <el-form-item label="冻结门槛金额">
        <el-input-number
          v-model="form.freezeThresholdAmount"
          :min="0"
          :precision="2"
          :step="0.01"
          placeholder="0 或留空表示不冻结"
        />
        <span class="ml-10px text-gray-500">实际返利高于该金额才冻结（元）</span>
      </el-form-item>
      <el-form-item label="冻结天数">
        <el-input-number
          v-model="form.freezeDays"
          :min="1"
          :max="365"
          :precision="0"
          :step="1"
          placeholder="请输入冻结天数"
        />
        <span class="ml-10px text-gray-500">冻结期满后自动到账</span>
      </el-form-item>
      <el-collapse accordion class="mt-12px">
        <el-collapse-item title="高级设置" name="advanced">
          <el-form-item label="会员等级"><MemberLevelSelect v-model="form.memberLevelId" /></el-form-item>
          <el-form-item label="最低金额"><el-input-number v-model="form.minRebateAmount" :min="0" /></el-form-item>
          <el-form-item label="最高金额"><el-input-number v-model="form.maxRebateAmount" :min="0" /></el-form-item>
        </el-collapse-item>
      </el-collapse>
    </el-form>
    <template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">确定</el-button></template>
  </el-dialog>
</template>
<script lang="ts" setup>
import { computed, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { RebateRuleForm } from '@/api/cps/platformOnboarding'
import MemberLevelSelect from '@/views/member/level/components/MemberLevelSelect.vue'
const props = defineProps<{ modelValue: boolean; row?: RebateRuleForm; platformCode: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; save: [RebateRuleForm] }>()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const emptyForm = (): RebateRuleForm => ({
  platformCode: props.platformCode,
  status: 1,
  rebateRate: undefined,
  freezeThresholdAmount: undefined,
  freezeDays: undefined,
  priority: 0
})
const form = reactive<RebateRuleForm>(emptyForm())
watch(
  () => props.row,
  (row) => Object.assign(form, emptyForm(), row || {}, { priority: row?.priority ?? 0 }),
  { immediate: true }
)
const submit = () => {
  const scope = (form as any).scope
  if (scope === 'GLOBAL') {
    ElMessage.error('不支持全局返利规则')
    return
  }
  if (scope === 'PERSONAL' || form.memberId) {
    ElMessage.error('不支持个人会员返利规则')
    return
  }
  if (form.rebateRate == null) {
    ElMessage.error('请输入返利比例')
    return
  }
  if (form.priority == null || !Number.isInteger(form.priority) || form.priority < 0) {
    ElMessage.error('请输入非负整数优先级')
    return
  }
  if (form.freezeThresholdAmount != null && form.freezeThresholdAmount < 0) {
    ElMessage.error('冻结门槛金额不能小于0')
    return
  }
  if (form.freezeDays != null && (!Number.isInteger(form.freezeDays) || form.freezeDays < 1 || form.freezeDays > 365)) {
    ElMessage.error('冻结天数必须为1至365的整数')
    return
  }
  if ((form.freezeThresholdAmount ?? 0) > 0 && form.freezeDays == null) {
    ElMessage.error('设置冻结门槛后请输入冻结天数')
    return
  }
  emit('save', { ...form })
  visible.value = false
}
</script>
