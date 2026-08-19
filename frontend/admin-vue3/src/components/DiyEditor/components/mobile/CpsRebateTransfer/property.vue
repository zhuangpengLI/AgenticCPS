<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="86px" :model="formData" class="m-t-8px">
      <el-card header="转链文案" class="property-group" shadow="never">
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="20" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="formData.description" type="textarea" maxlength="60" /></el-form-item>
        <el-form-item label="输入提示"><el-input v-model="formData.placeholder" maxlength="40" /></el-form-item>
        <el-form-item label="按钮文案"><el-input v-model="formData.buttonText" maxlength="8" /></el-form-item>
        <el-form-item label="默认平台"><el-select v-model="formData.platformCode"><el-option label="淘宝" value="taobao" /><el-option label="京东" value="jd" /><el-option label="拼多多" value="pdd" /><el-option label="抖音" value="douyin" /></el-select></el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateTransferProperty } from './config'

defineOptions({ name: 'CpsRebateTransferProperty' })
const props = defineProps<{ modelValue: CpsRebateTransferProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
</script>
