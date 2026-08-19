<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="96px" :model="formData" class="m-t-8px">
      <el-card header="资产展示" class="property-group" shadow="never">
        <el-alert title="画布只显示示例金额，移动端登录后读取当前会员真实资产。" type="info" :closable="false" show-icon class="m-b-12px" />
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="20" show-word-limit /></el-form-item>
        <el-form-item label="可用返利"><el-switch v-model="formData.showAvailable" /></el-form-item>
        <el-form-item label="冻结金额"><el-switch v-model="formData.showFrozen" /></el-form-item>
        <el-form-item label="累计返利"><el-switch v-model="formData.showTotal" /></el-form-item>
        <el-form-item label="游客提示"><el-input v-model="formData.guestText" maxlength="30" show-word-limit /></el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateAssetsProperty } from './config'

defineOptions({ name: 'CpsRebateAssetsProperty' })
const props = defineProps<{ modelValue: CpsRebateAssetsProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
</script>
