<template>
  <ComponentContainerProperty v-model="formData.style">
    <el-form label-width="86px" :model="formData" class="m-t-8px">
      <el-card header="活动设置" class="property-group" shadow="never">
        <el-form-item label="标题"><el-input v-model="formData.title" maxlength="20" show-word-limit /></el-form-item>
        <el-form-item label="绑定活动">
          <el-select v-model="formData.activityIds" multiple filterable :multiple-limit="10" :loading="loading" placeholder="最多选择 10 个活动" class="w-full" @change="normalizeIds">
            <el-option v-for="activity in activityOptions" :key="activity.id" :label="activity.label" :value="activity.id" :disabled="activity.unavailable" />
          </el-select>
        </el-form-item>
        <el-text v-if="hasUnavailable" type="warning" size="small">已下线/不可用的活动会保留 ID，但移动端不会展示。</el-text>
        <el-form-item label="布局" class="m-t-12px">
          <el-radio-group v-model="formData.layout"><el-radio-button value="grid">双列</el-radio-button><el-radio-button value="list">列表</el-radio-button></el-radio-group>
        </el-form-item>
        <el-form-item label="展示数量"><el-input-number v-model="formData.limit" :min="1" :max="10" /></el-form-item>
        <el-form-item label="显示更多"><el-switch v-model="formData.showMore" /></el-form-item>
      </el-card>
    </el-form>
  </ComponentContainerProperty>
</template>

<script setup lang="ts">
import { useVModel } from '@vueuse/core'
import { CpsRebateActivityApi, CpsRebateActivityVO } from '@/api/cps/rebateActivity'
import { CpsRebateActivitiesProperty } from './config'

defineOptions({ name: 'CpsRebateActivitiesProperty' })
const props = defineProps<{ modelValue: CpsRebateActivitiesProperty }>()
const emit = defineEmits(['update:modelValue'])
const formData = useVModel(props, 'modelValue', emit)
const activities = ref<CpsRebateActivityVO[]>([])
const loading = ref(false)
const enabledIds = computed(() => new Set(activities.value.map((item) => item.id)))
const hasUnavailable = computed(() => (formData.value.activityIds || []).some((id) => !enabledIds.value.has(id)))
const activityOptions = computed(() => [
  ...activities.value.map((item) => ({ id: item.id, label: `${item.activityName}（${item.platformCode}）`, unavailable: false })),
  ...(formData.value.activityIds || []).filter((id) => !enabledIds.value.has(id)).map((id) => ({ id, label: `活动 #${id}（已下线/不可用）`, unavailable: true }))
])

const normalizeIds = (ids: number[]) => {
  formData.value.activityIds = [...new Set(ids)].slice(0, 10)
}

onMounted(async () => {
  loading.value = true
  try {
    activities.value = await CpsRebateActivityApi.getEnabledActivityList()
  } finally {
    loading.value = false
  }
})
</script>
