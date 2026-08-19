<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="76px" :model="formData" class="m-t-8px">
      <el-card header="返利说明" class="property-group" shadow="never">
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="20" show-word-limit /></el-form-item>
        <el-text type="info" size="small">步骤可拖动排序，至少 2 项、最多 5 项。</el-text>
        <Draggable v-model="formData.steps" :empty-item="''" :min="2" :limit="5" class="m-t-8px">
          <template #default="{ index }"><el-input v-model="formData.steps[index]" maxlength="40" show-word-limit placeholder="请输入步骤说明" @blur="normalizeSteps" /></template>
        </Draggable>
        <el-form-item label="风险提示" class="m-t-12px"><el-input v-model="formData.notice" type="textarea" maxlength="100" show-word-limit /></el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateGuideProperty } from './config'

defineOptions({ name: 'CpsRebateGuideProperty' })
const props = defineProps<{ modelValue: CpsRebateGuideProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
const normalizeSteps = () => {
  const steps = formData.value.steps.map((step) => step.trim()).filter(Boolean).slice(0, 5)
  while (steps.length < 2) steps.push('请输入步骤说明')
  formData.value.steps = steps
}
</script>
