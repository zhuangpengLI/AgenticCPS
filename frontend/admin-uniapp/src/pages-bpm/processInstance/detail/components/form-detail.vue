<!-- 表单详情：流程表单/业务表单 -->
<template>
  <view class="mx-24rpx mt-24rpx overflow-hidden rounded-16rpx bg-white">
    <!-- 标题 -->
    <view class="px-24rpx pt-24rpx text-28rpx text-[#333] font-bold">
      审批详情
    </view>
    <!-- 表单内容：业务表单 -->
    <template v-if="processDefinition?.formType === BpmModelFormType.CUSTOM">
      <!-- OA 请假详情 -->
      <LeaveDetail
        v-if="processDefinition?.formCustomViewPath === '/bpm/oa/leave/detail'"
        :id="processInstance?.businessKey"
        embedded
      />
      <!-- 未配置的业务表单 -->
      <view v-else class="px-24rpx py-32rpx text-26rpx text-[#999]">
        暂不支持该业务表单，请参考 LeaveDetail 配置
      </view>
    </template>
    <!-- TODO @jason：表单内容：流程表单 -->
    <template v-else-if="processDefinition?.formType === BpmModelFormType.NORMAL">
      <view class="px-24rpx py-32rpx text-26rpx text-[#999]">
        流程表单仅 PC 端支持预览
      </view>
    </template>
  </view>
</template>

<script lang="ts" setup>
import type { ProcessDefinition, ProcessInstance } from '@/api/bpm/processInstance'
// 特殊：业务表单组件（uniapp 小程序不支持动态组件，需要静态导入）
import LeaveDetail from '@/pages-bpm/oa/leave/detail/index.vue'
import { BpmModelFormType } from '@/utils/constants'

defineProps<{
  /** 流程定义 */
  processDefinition?: ProcessDefinition
  /** 流程实例 */
  processInstance?: ProcessInstance
}>()
</script>
