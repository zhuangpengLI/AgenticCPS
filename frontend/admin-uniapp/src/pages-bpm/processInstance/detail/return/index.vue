<template>
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      title="退回任务"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 操作表单 -->
    <view class="p-24rpx">
      <wd-form ref="formRef" :model="formData" :rules="formRules">
        <wd-cell-group border>
          <!-- 退回节点选择 -->
          <wd-picker
            v-model="formData.targetActivityId"
            label="退回节点："
            prop="targetActivityId"
            :columns="activityOptions"
            value-key="taskDefinitionKey"
            label-key="name"
            placeholder="请选择退回节点"
          />

          <!-- 退回原因 -->
          <wd-textarea
            v-model="formData.reason"
            prop="reason"
            label="退回原因："
            label-width="180rpx"
            placeholder="请输入退回原因"
            :maxlength="500"
            show-word-limit
            clearable
          />
        </wd-cell-group>
        <!-- 提交按钮 -->
        <view class="mt-48rpx">
          <wd-button
            type="primary"
            block
            :loading="formLoading"
            :disabled="formLoading"
            @click="handleSubmit"
          >
            退回
          </wd-button>
        </view>
      </wd-form>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { FormInstance } from 'wot-design-uni/components/wd-form/types'
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { getTaskListByReturn, returnTask } from '@/api/bpm/task'
import { navigateBackPlus } from '@/utils'

const props = defineProps<{
  processInstanceId: string
  taskId: string
}>()

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const taskId = computed(() => props.taskId)
const processInstanceId = computed(() => props.processInstanceId)
const toast = useToast()
const formLoading = ref(false)
const activityOptions = ref<any[]>([])
const formData = reactive({
  targetActivityId: '',
  reason: '',
})
const formRules = {
  targetActivityId: [
    { required: true, message: '退回节点不能为空' },
  ],
  reason: [
    { required: true, message: '退回原因不能为空' },
  ],
}
const formRef = ref<FormInstance>()

/** 返回上一页 */
function handleBack() {
  navigateBackPlus(`/pages-bpm/processInstance/detail/index?id=${processInstanceId.value}&taskId=${taskId.value}`)
}

/** 获取可退回的节点列表 */
async function loadReturnTaskList() {
  const result = await getTaskListByReturn(taskId.value)
  activityOptions.value = result
}

/** 提交操作 */
async function handleSubmit() {
  if (formLoading.value) {
    return
  }
  const { valid } = await formRef.value!.validate()
  if (!valid) {
    return
  }
  formLoading.value = true
  try {
    await returnTask({
      id: taskId.value as string,
      targetTaskDefinitionKey: formData.targetActivityId,
      reason: formData.reason,
    })

    toast.success('退回成功')
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages-bpm/processInstance/detail/index?id=${processInstanceId.value}&taskId=${taskId.value}`,
      })
    }, 500)
  } finally {
    formLoading.value = false
  }
}

/** 页面加载时获取可退回节点列表 */
onMounted(() => {
  /** 初始化校验 */
  if (!props.taskId || !props.processInstanceId) {
    toast.show('参数错误')
    return
  }
  loadReturnTaskList()
})
</script>
