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
            label="表达式名字"
            label-width="180rpx"
            prop="name"
            clearable
            placeholder="请输入表达式名字"
          />
          <wd-cell title="表达式状态" title-width="180rpx" prop="status" center>
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
          <wd-textarea
            v-model="formData.expression"
            label="表达式"
            label-width="180rpx"
            prop="expression"
            clearable
            placeholder="请输入表达式"
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
import type { ProcessExpression } from '@/api/bpm/process-expression'
import { computed, onMounted, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { createProcessExpression, getProcessExpression, updateProcessExpression } from '@/api/bpm/process-expression'
import { getIntDictOptions } from '@/hooks/useDict'
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

const toast = useToast()
const getTitle = computed(() => props.id ? '编辑流程表达式' : '新增流程表达式')
const formLoading = ref(false)
const formData = ref<ProcessExpression>({
  id: undefined,
  name: '',
  status: CommonStatusEnum.ENABLE,
  expression: '',
})
const formRules = {
  name: [{ required: true, message: '表达式名字不能为空' }],
  status: [{ required: true, message: '表达式状态不能为空' }],
  expression: [{ required: true, message: '表达式不能为空' }],
}
const formRef = ref<FormInstance>()

/** 返回上一页 */
function handleBack() {
  navigateBackPlus('/pages-bpm/process-expression/index')
}

/** 加载流程表达式详情 */
async function getDetail() {
  if (!props.id) {
    return
  }
  formData.value = await getProcessExpression(props.id)
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
      await updateProcessExpression(formData.value)
      toast.success('修改成功')
    } else {
      await createProcessExpression(formData.value)
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
