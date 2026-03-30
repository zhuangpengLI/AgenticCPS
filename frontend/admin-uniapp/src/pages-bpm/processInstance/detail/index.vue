<template>
  <view class="yd-page-container pb-[80rpx]">
    <!-- 顶部导航栏 -->
    <wd-navbar
      title="审批详情"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 区域：流程信息（基本信息） -->
    <view class="relative mx-24rpx mt-24rpx overflow-hidden rounded-16rpx bg-white">
      <!-- 审批状态图标（盖章效果） -->
      <image
        v-if="processInstance?.status !== undefined"
        :src="getStatusIcon(processInstance?.status)"
        class="absolute right-20rpx top-20rpx z-10 h-144rpx w-144rpx"
        mode="aspectFit"
      />
      <view class="p-24rpx">
        <!-- 标题 -->
        <view class="mb-16rpx pr-160rpx">
          <text class="text-32rpx text-[#333] font-bold">{{ processInstance?.name }}</text>
        </view>
        <!-- 发起人信息 -->
        <view class="flex items-center">
          <view class="mr-12rpx h-64rpx w-64rpx flex items-center justify-center rounded-full bg-[#1890ff] text-white">
            {{ processInstance?.startUser?.nickname?.[0] || '?' }}
          </view>
          <view>
            <text class="text-28rpx text-[#333]">{{ processInstance?.startUser?.nickname }}</text>
            <text v-if="processInstance?.startUser?.deptName" class="ml-8rpx text-24rpx text-[#999]">
              {{ processInstance?.startUser?.deptName }}
            </text>
          </view>
        </view>
        <!-- 提交时间 -->
        <view class="mt-16rpx text-24rpx text-[#999]">
          提交于 {{ formatDateTime(processInstance?.startTime) }}
        </view>
      </view>
    </view>

    <!-- 区域：审批详情（表单） -->
    <FormDetail :process-definition="processDefinition" :process-instance="processInstance" />

    <!-- 区域：审批进度 -->
    <view class="mx-24rpx mt-24rpx rounded-16rpx bg-white">
      <view class="p-24rpx">
        <view class="mb-16rpx flex">
          <text class="text-28rpx text-[#333] font-bold">审批进度</text>
        </view>
        <!-- 流程时间线 -->
        <ProcessInstanceTimeline :activity-nodes="activityNodes" />
      </view>
    </view>

    <!-- TODO 待开发：区域：流程评论 -->

    <!-- 区域：底部操作栏 -->
    <ProcessInstanceOperationButton ref="operationButtonRef" />
  </view>
</template>

<script lang="ts" setup>
import type { ApprovalNodeInfo, ProcessDefinition, ProcessInstance } from '@/api/bpm/processInstance'
import type { Task } from '@/api/bpm/task'
import { onMounted, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { getApprovalDetail } from '@/api/bpm/processInstance'
import { getTaskListByProcessInstanceId } from '@/api/bpm/task'
import { navigateBackPlus } from '@/utils'
import { BpmProcessInstanceStatus } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import FormDetail from './components/form-detail.vue'
import ProcessInstanceOperationButton from './components/operation-button.vue'
import ProcessInstanceTimeline from './components/time-line.vue'

const props = defineProps<{
  id: string // 流程实例的编号
  taskId?: string // 任务编号
}>()

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const processInstance = ref<ProcessInstance>()
const processDefinition = ref<ProcessDefinition>()
const tasks = ref<Task[]>([])

const activityNodes = ref<ApprovalNodeInfo[]>([]) // 审批节点信息

const operationButtonRef = ref() // 操作按钮组件 ref

/** 返回上一页 */
function handleBack() {
  navigateBackPlus('/pages/bpm/index')
}

/** 获取状态图标 */
function getStatusIcon(status?: number): string {
  // 状态映射： 1-审批中, 2-审批通过, 3-审批不通过, 4-已取消. -1 未开始不会出现
  const iconMap: Record<number, string> = {
    [BpmProcessInstanceStatus.RUNNING]: '/static/my-icons/bpm/bpm-running.svg', // 待审批
    [BpmProcessInstanceStatus.APPROVE]: '/static/my-icons/bpm/bpm-approve.svg', // 审批通过
    [BpmProcessInstanceStatus.REJECT]: '/static/my-icons/bpm/bpm-reject.svg', // 审批不通过
    [BpmProcessInstanceStatus.CANCEL]: '/static/my-icons/bpm/bpm-cancel.svg', // 已取消
  }
  return iconMap[status ?? 1]
}

/** 加载流程实例 */
async function loadProcessInstance() {
  const data = await getApprovalDetail({
    processInstanceId: props.id,
    taskId: props.taskId,
  })
  if (!data || !data.processInstance) {
    toast.show('查询不到审批详情信息')
    return
  }
  processInstance.value = data.processInstance
  processDefinition.value = data.processDefinition
  // 获取审批节点，显示 Timeline 的数据
  activityNodes.value = data.activityNodes

  operationButtonRef.value?.init(data.processInstance, data.todoTask)
}

/** 加载任务列表 */
async function loadTasks() {
  tasks.value = await getTaskListByProcessInstanceId(props.id)
}

/** 初始化 */
onMounted(async () => {
  if (!props.id) {
    toast.show('参数错误')
    return
  }
  await Promise.all([loadProcessInstance(), loadTasks()])
})
</script>
