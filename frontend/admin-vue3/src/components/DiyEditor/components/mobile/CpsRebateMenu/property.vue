<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="82px" :model="formData" class="m-t-8px">
      <el-card header="入口设置" class="property-group" shadow="never">
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="20" show-word-limit /></el-form-item>
        <el-form-item label="每行数量"><el-input-number v-model="formData.columns" :min="2" :max="5" /></el-form-item>
        <el-alert title="资金类入口的登录校验由系统固定，不能在装修中关闭。" type="info" :closable="false" show-icon class="m-b-12px" />
        <Draggable v-model="formData.items" :limit="8" :min="0">
          <template #default="{ element, index }">
            <el-form-item label="入口">
              <el-select :model-value="element.key" disabled class="w-full">
                <el-option v-for="destination in REBATE_MENU_DESTINATIONS" :key="destination.key" :label="destination.title" :value="destination.key" />
              </el-select>
            </el-form-item>
            <el-form-item label="标题"><el-input v-model="element.title" maxlength="10" /></el-form-item>
            <el-form-item label="图标"><el-input v-model="element.icon" maxlength="60" /></el-form-item>
            <el-form-item label="启用"><el-switch v-model="element.enabled" /></el-form-item>
            <el-button link type="danger" @click="removeItem(index)">移除入口</el-button>
          </template>
        </Draggable>
        <div class="m-t-12px flex gap-8px">
          <el-select v-model="pendingKey" placeholder="选择预定义入口" class="flex-1">
            <el-option v-for="destination in addableDestinations" :key="destination.key" :label="destination.title" :value="destination.key" />
          </el-select>
          <el-button type="primary" :disabled="!pendingKey || formData.items.length >= 8" @click="addItem">添加</el-button>
        </div>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateMenuKey, CpsRebateMenuProperty, REBATE_MENU_DESTINATIONS } from './config'

defineOptions({ name: 'CpsRebateMenuProperty' })
const props = defineProps<{ modelValue: CpsRebateMenuProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
const pendingKey = ref<CpsRebateMenuKey>()
const addableDestinations = computed(() => {
  const usedKeys = new Set((formData.value.items || []).map((item) => item.key))
  return REBATE_MENU_DESTINATIONS.filter((item) => !usedKeys.has(item.key))
})

const addItem = () => {
  const destination = REBATE_MENU_DESTINATIONS.find((item) => item.key === pendingKey.value)
  if (!destination || formData.value.items.length >= 8) return
  formData.value.items.push({ key: destination.key, title: destination.title, icon: destination.icon, enabled: true })
  pendingKey.value = undefined
}
const removeItem = (index: number) => formData.value.items.splice(index, 1)
</script>
