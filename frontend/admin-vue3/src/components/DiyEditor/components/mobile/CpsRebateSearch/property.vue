<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="86px" :model="formData" class="m-t-8px">
      <el-card header="搜索文案" class="property-group" shadow="never">
        <el-form-item label="顶部文案"><el-input v-model="formData.kicker" maxlength="30" show-word-limit /></el-form-item>
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="30" show-word-limit /></el-form-item>
        <el-form-item label="说明"><el-input v-model="formData.description" type="textarea" maxlength="80" show-word-limit /></el-form-item>
        <el-form-item label="搜索提示"><el-input v-model="formData.placeholder" maxlength="40" show-word-limit /></el-form-item>
        <el-form-item label="按钮文案"><el-input v-model="formData.buttonText" maxlength="8" show-word-limit /></el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateSearchProperty } from './config'

defineOptions({ name: 'CpsRebateSearchProperty' })
const props = defineProps<{ modelValue: CpsRebateSearchProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
</script>
