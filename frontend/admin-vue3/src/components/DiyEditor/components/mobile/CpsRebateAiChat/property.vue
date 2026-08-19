<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="86px" :model="formData" class="m-t-8px">
      <el-card header="AI 对话文案" class="property-group" shadow="never">
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="20" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="formData.description" type="textarea" maxlength="60" /></el-form-item>
        <el-form-item label="问题提示"><el-input v-model="formData.placeholder" maxlength="60" /></el-form-item>
        <el-form-item label="按钮文案"><el-input v-model="formData.buttonText" maxlength="8" /></el-form-item>
        <el-form-item label="角色编号"><el-input-number v-model="formData.roleId" :min="1" :controls="false" placeholder="默认返利角色" /></el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateAiChatProperty } from './config'

defineOptions({ name: 'CpsRebateAiChatProperty' })
const props = defineProps<{ modelValue: CpsRebateAiChatProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
</script>
