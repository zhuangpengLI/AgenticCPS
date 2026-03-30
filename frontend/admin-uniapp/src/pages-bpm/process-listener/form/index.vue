<template>
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      :title="getTitle"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 表单区域 -->
    <view>
      <wd-form ref="formRef" :model="formData" :rules="formRules">
        <wd-cell-group border>
          <wd-input
            v-model="formData.name"
            label="监听器名字"
            label-width="180rpx"
            prop="name"
            clearable
            placeholder="请输入监听器名字"
          />
          <wd-cell title="监听器状态" title-width="180rpx" prop="status" center>
            <wd-radio-group v-model="formData.status" shape="button">
              <wd-radio
                v-for="dict in getIntDictOptions(DICT_TYPE.COMMON_STATUS)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </wd-radio>
            </wd-radio-group>
          </wd-cell>
          <wd-cell title="监听器类型" title-width="180rpx" prop="type" center>
            <wd-radio-group v-model="formData.type" shape="button" @change="handleTypeChange">
              <wd-radio
                v-for="dict in getStrDictOptions(DICT_TYPE.BPM_PROCESS_LISTENER_TYPE)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </wd-radio>
            </wd-radio-group>
          </wd-cell>
          <wd-cell title="监听事件" title-width="180rpx" prop="event" center>
            <wd-radio-group v-model="formData.event" shape="button">
              <wd-radio
                v-for="option in eventOptions"
                :key="option.value"
                :value="option.value"
              >
                {{ option.label }}
              </wd-radio>
            </wd-radio-group>
          </wd-cell>
          <wd-cell title="值类型" title-width="180rpx" prop="valueType" center>
            <wd-radio-group v-model="formData.valueType" shape="button" @change="handleValueTypeChange">
              <wd-radio
                v-for="dict in getStrDictOptions(DICT_TYPE.BPM_PROCESS_LISTENER_VALUE_TYPE)"
                :key="dict.value"
                :value="dict.value"
              >
                {{ dict.label }}
              </wd-radio>
            </wd-radio-group>
          </wd-cell>
          <wd-input
            v-model="formData.value"
            :label="valueLabel"
            label-width="180rpx"
            prop="value"
            clearable
            :placeholder="valuePlaceholder"
          />
        </wd-cell-group>
      </wd-form>
    </view>

    <!-- 底部保存按钮 -->
    <view class="yd-detail-footer">
      <wd-button
        type="primary"
        block
        :loading="formLoading"
        @click="handleSubmit"
      >
        保存
      </wd-button>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { FormInstance } from 'wot-design-uni/components/wd-form/types'
import type { ProcessListener } from '@/api/bpm/process-listener'
import { computed, onMounted, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { createProcessListener, getProcessListener, updateProcessListener } from '@/api/bpm/process-listener'
import { getIntDictOptions, getStrDictOptions } from '@/hooks/useDict'
import { navigateBackPlus } from '@/utils'
import { CommonStatusEnum, DICT_TYPE } from '@/utils/constants'

const props = defineProps<{
  id?: number | any
}>()

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

/** 执行监听器事件选项 */
const EVENT_EXECUTION_OPTIONS = [
  { label: '开始', value: 'start' },
  { label: '结束', value: 'end' },
]

/** 任务监听器事件选项 */
const EVENT_OPTIONS = [
  { label: '创建', value: 'create' },
  { label: '指派', value: 'assignment' },
  { label: '完成', value: 'complete' },
  { label: '删除', value: 'delete' },
  { label: '更新', value: 'update' },
  { label: '超时', value: 'timeout' },
]

const toast = useToast()
const getTitle = computed(() => props.id ? '编辑流程监听器' : '新增流程监听器')
const formLoading = ref(false)
const formData = ref<ProcessListener>({
  id: undefined,
  name: '',
  type: '',
  status: CommonStatusEnum.ENABLE,
  event: '',
  valueType: '',
  value: '',
})
const formRules = {
  name: [{ required: true, message: '监听器名字不能为空' }],
  type: [{ required: true, message: '监听器类型不能为空' }],
  status: [{ required: true, message: '监听器状态不能为空' }],
  event: [{ required: true, message: '监听事件不能为空' }],
  valueType: [{ required: true, message: '值类型不能为空' }],
  value: [{ required: true, message: '值不能为空' }],
}
const formRef = ref<FormInstance>()

/** 根据类型获取事件选项 */
const eventOptions = computed(() => {
  return formData.value.type === 'execution' ? EVENT_EXECUTION_OPTIONS : EVENT_OPTIONS
})

/** 值标签 */
const valueLabel = computed(() => {
  return formData.value.valueType === 'class' ? '类路径' : '表达式'
})

/** 值占位符 */
const valuePlaceholder = computed(() => {
  return formData.value.valueType === 'class' ? '请输入类路径' : '请输入表达式'
})

/** 返回上一页 */
function handleBack() {
  navigateBackPlus('/pages-bpm/process-listener/index')
}

/** 类型变更时清空事件 */
function handleTypeChange() {
  formData.value.event = ''
}

/** 值类型变更时清空值 */
function handleValueTypeChange() {
  formData.value.value = ''
}

/** 加载流程监听器详情 */
async function getDetail() {
  if (!props.id) {
    return
  }
  formData.value = await getProcessListener(props.id)
}

/** 提交表单 */
async function handleSubmit() {
  const { valid } = await formRef.value.validate()
  if (!valid) {
    return
  }

  formLoading.value = true
  try {
    if (props.id) {
      await updateProcessListener(formData.value)
      toast.success('修改成功')
    } else {
      await createProcessListener(formData.value)
      toast.success('新增成功')
    }
    setTimeout(() => {
      handleBack()
    }, 500)
  } finally {
    formLoading.value = false
  }
}

/** 初始化 */
onMounted(() => {
  getDetail()
})
</script>

<style lang="scss" scoped>
</style>
