<template>
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      :title="isDelegate ? '委派任务' : '转办任务'"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 操作表单 -->
    <view class="p-24rpx">
      <wd-form ref="formRef" :model="formData" :rules="formRules">
        <wd-cell-group border>
          <!-- 用户选择 -->
          <UserPicker
            v-model="formData.userId"
            prop="userId"
            type="radio"
            :label="`${isDelegate ? '接收人' : '新审批人'}：`"
            :placeholder="`请选择${isDelegate ? '接收人' : '新审批人'}`"
          />

          <!-- 审批意见 -->
          <wd-textarea
            v-model="formData.reason"
            prop="reason"
            label="审批意见："
            label-width="180rpx"
            placeholder="请输入审批意见"
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
            {{ isDelegate ? '委派' : '转办' }}
          </wd-button>
        </view>
      </wd-form>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { FormInstance } from 'wot-design-uni/components/wd-form/types'
import { computed, reactive, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { delegateTask, transferTask } from '@/api/bpm/task'
import UserPicker from '@/components/system-select/user-picker.vue'
import { navigateBackPlus } from '@/utils'

const props = defineProps<{
  processInstanceId: string
  taskId: string
  type: string // 'delegate' 或 'transfer'
}>()

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const taskId = computed(() => props.taskId)
const processInstanceId = computed(() => props.processInstanceId)
const operationType = computed(() => props.type || 'transfer') // 默认转办
const isDelegate = computed(() => operationType.value === 'delegate')
const toast = useToast()
const formLoading = ref(false)
const formData = reactive({
  userId: undefined as number | undefined,
  reason: '',
})
const formRules = {
  userId: [
    { required: true, message: `请选择${isDelegate.value ? '接收人' : '新审批人'}` },
  ],
  reason: [
    { required: true, message: '审批意见不能为空' },
  ],
}
const formRef = ref<FormInstance>()

/** 返回上一页 */
function handleBack() {
  navigateBackPlus(`/pages-bpm/processInstance/detail/index?id=${processInstanceId.value}&taskId=${taskId.value}`)
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
    const data = {
      id: taskId.value as string,
      reason: formData.reason,
    }
    if (isDelegate.value) {
      await delegateTask({
        ...data,
        delegateUserId: String(formData.userId),
      })
    } else {
      await transferTask({
        ...data,
        assigneeUserId: String(formData.userId),
      })
    }
    toast.success(`${isDelegate.value ? '委派' : '转办'}成功`)
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages-bpm/processInstance/detail/index?id=${processInstanceId.value}&taskId=${taskId.value}`,
      })
    }, 500)
  } finally {
    formLoading.value = false
  }
}

/** 页面加载时 */
onMounted(() => {
  /** 初始化校验 */
  if (!props.taskId || !props.processInstanceId) {
    toast.show('参数错误')
  }
})
</script>
