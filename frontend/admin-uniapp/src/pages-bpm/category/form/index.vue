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
            label="分类名"
            label-width="180rpx"
            prop="name"
            clearable
            placeholder="请输入分类名"
          />
          <wd-input
            v-model="formData.code"
            label="分类标志"
            label-width="180rpx"
            prop="code"
            clearable
            placeholder="请输入分类标志"
          />
          <wd-textarea
            v-model="formData.description"
            label="分类描述"
            label-width="180rpx"
            prop="description"
            clearable
            placeholder="请输入分类描述"
          />
          <wd-cell title="分类状态" title-width="180rpx" prop="status" center>
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
          <wd-input
            v-model.number="formData.sort"
            label="分类排序"
            label-width="180rpx"
            prop="sort"
            type="number"
            clearable
            placeholder="请输入分类排序"
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
import type { Category } from '@/api/bpm/category'
import { computed, onMounted, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { createCategory, getCategory, updateCategory } from '@/api/bpm/category'
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
const getTitle = computed(() => props.id ? '编辑流程分类' : '新增流程分类')
const formLoading = ref(false)
const formData = ref<Category>({
  id: undefined,
  name: '',
  code: '',
  status: CommonStatusEnum.ENABLE,
  description: '',
  sort: 0,
})
const formRules = {
  name: [{ required: true, message: '分类名不能为空' }],
  code: [{ required: true, message: '分类标志不能为空' }],
  status: [{ required: true, message: '分类状态不能为空' }],
}
const formRef = ref<FormInstance>()

/** 返回上一页 */
function handleBack() {
  navigateBackPlus('/pages-bpm/category/index')
}

/** 加载流程分类详情 */
async function getDetail() {
  if (!props.id) {
    return
  }
  formData.value = await getCategory(props.id)
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
      await updateCategory(formData.value)
      toast.success('修改成功')
    } else {
      await createCategory(formData.value)
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
