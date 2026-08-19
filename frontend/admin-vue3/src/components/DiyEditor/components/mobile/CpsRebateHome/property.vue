<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="92px" :model="formData" class="m-t-8px">
      <el-card header="返利首页文案" class="property-group" shadow="never">
        <el-form-item label="顶部文案">
          <el-input v-model="formData.kicker" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="返利标题">
          <el-input v-model="formData.title" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="说明文字">
          <el-input v-model="formData.description" maxlength="60" show-word-limit />
        </el-form-item>
        <el-form-item label="搜索提示">
          <el-input v-model="formData.searchPlaceholder" maxlength="40" show-word-limit />
        </el-form-item>
        <el-form-item label="搜索按钮">
          <el-input v-model="formData.searchButtonText" maxlength="8" show-word-limit />
        </el-form-item>
        <el-form-item label="精选商品">
          <el-switch v-model="formData.showFeatured" />
        </el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateHomeProperty } from './config'

defineOptions({ name: 'CpsRebateHomeProperty' })
const props = defineProps<{ modelValue: CpsRebateHomeProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
</script>
