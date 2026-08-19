<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="86px" :model="formData" class="m-t-8px">
      <el-card header="选品设置" class="property-group" shadow="never">
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="20" show-word-limit /></el-form-item>
        <el-form-item label="选品主题">
          <el-select v-model="formData.themeId" clearable filterable :loading="loading" placeholder="选择已发布主题" class="w-full">
            <el-option v-for="theme in themeOptions" :key="theme.id" :label="theme.label" :value="theme.id" :disabled="theme.unavailable" />
          </el-select>
        </el-form-item>
        <el-text v-if="hasUnavailable" type="warning" size="small">当前主题已下线/不可用，配置 ID 已保留。</el-text>
        <el-form-item label="展示数量" class="m-t-12px"><el-input-number v-model="formData.limit" :min="2" :max="20" /></el-form-item>
        <el-form-item label="每行数量"><el-input-number v-model="formData.columns" :min="1" :max="3" /></el-form-item>
        <el-form-item label="显示更多"><el-switch v-model="formData.showMore" /></el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsSelectionThemeApi, CpsSelectionThemeVO } from '@/api/cps/selectionTheme'
import { CpsSelectionThemeProperty } from './config'

defineOptions({ name: 'CpsSelectionThemeProperty' })
const props = defineProps<{ modelValue: CpsSelectionThemeProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
const themes = ref<CpsSelectionThemeVO[]>([])
const loading = ref(false)
const publishedIds = computed(() => new Set(themes.value.map((item) => item.id)))
const hasUnavailable = computed(() => !!formData.value.themeId && !publishedIds.value.has(formData.value.themeId))
const themeOptions = computed(() => [
  ...themes.value.map((item) => ({ id: item.id, label: item.themeName, unavailable: false })),
  ...(hasUnavailable.value && formData.value.themeId
    ? [{ id: formData.value.themeId, label: `主题 #${formData.value.themeId}（已下线/不可用）`, unavailable: true }]
    : [])
])

onMounted(async () => {
  loading.value = true
  try {
    const data = await CpsSelectionThemeApi.getThemePage({ pageNo: 1, pageSize: 100, status: 'PUBLISHED' })
    themes.value = data.list
  } finally {
    loading.value = false
  }
})
</script>
