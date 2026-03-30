<template>
  <view class="yd-page-container pb-[76rpx]">
    <!-- 顶部导航栏 -->
    <wd-navbar
      title="发起请假"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />
    <view class="mx-24rpx mt-24rpx overflow-hidden rounded-16rpx bg-white">
      <!-- 表单内容 -->
      <wd-form ref="formRef" :model="formData" :rules="formRules">
        <wd-cell-group border title="请假信息">
          <wd-picker
            v-model="formData.type"
            :columns="getIntDictOptions(DICT_TYPE.BPM_OA_LEAVE_TYPE)"
            label="请假类型"
            label-width="200rpx"
            prop="type"
            :rules="[{ required: true, message: '请选择请假类型' }]"
            placeholder="请选择请假类型"
          />
          <wd-datetime-picker
            v-model="formData.startTime"
            label="开始时间"
            label-width="200rpx"
            prop="startTime"
            :rules="[{ required: true, message: '请选择开始时间' }]"
            placeholder="请选择开始时间"
          />
          <wd-datetime-picker
            v-model="formData.endTime"
            label="结束时间"
            label-width="200rpx"
            prop="endTime"
            :rules="[{ required: true, message: '请选择结束时间' }]"
            placeholder="请选择结束时间"
          />
          <wd-textarea
            v-model="formData.reason"
            label="请假原因"
            label-width="200rpx"
            prop="reason"
            :rules="[{ required: true, message: '请输入请假原因' }]"
            placeholder="请输入请假原因"
            :maxlength="200"
            show-word-limit
          />
        </wd-cell-group>
      </wd-form>
    </view>
    <!-- 流程预览卡片 -->
    <view class="mx-24rpx mb-120rpx mt-24rpx rounded-16rpx bg-white">
      <view class="p-24rpx">
        <view class="mb-16rpx flex items-center justify-between">
          <text class="text-28rpx text-[#333] font-bold">流程预览</text>
          <wd-loading v-if="processTimeLineLoading" size="32rpx" />
        </view>

        <!-- 流程时间线 -->
        <ProcessInstanceTimeline
          v-if="activityNodes.length > 0"
          :activity-nodes="activityNodes"
          :show-status-icon="false"
          @select-user-confirm="selectUserConfirm"
        />

        <!-- 无流程数据提示 -->
        <view v-else-if="!processTimeLineLoading" class="py-40rpx text-center">
          <text class="text-24rpx text-[#999]">暂无流程预览数据</text>
        </view>
      </view>
    </view>

    <!-- 底部提交按钮 -->
    <view class="yd-detail-footer">
      <view class="yd-detail-footer-actions">
        <wd-button type="primary" class="flex-1" :loading="formLoading" @click="handleSubmit">
          提交
        </wd-button>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { FormInstance } from 'wot-design-uni/components/wd-form/types'
import type { Leave } from '@/api/bpm/oa/leave'
import type { ApprovalNodeInfo } from '@/api/bpm/processInstance'
import { computed, onMounted, ref, watch } from 'vue'
import { useMessage, useToast } from 'wot-design-uni'
import { getProcessDefinition } from '@/api/bpm/definition'
import { createLeave } from '@/api/bpm/oa/leave'
import { getApprovalDetail } from '@/api/bpm/processInstance'
import { getIntDictOptions } from '@/hooks/useDict'
import ProcessInstanceTimeline from '@/pages-bpm/processInstance/detail/components/time-line.vue'
import { navigateBackPlus } from '@/utils'
import { BpmCandidateStrategyEnum, BpmNodeIdEnum, DICT_TYPE } from '@/utils/constants'

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const message = useMessage()
const formLoading = ref(false)
const processTimeLineLoading = ref(false) // 流程预览加载状态

// 流程相关数据
const processDefineKey = 'oa_leave' // 流程定义 Key
const processDefinitionId = ref('')
const activityNodes = ref<ApprovalNodeInfo[]>([]) // 审批节点信息
const startUserSelectTasks = ref<any[]>([]) // 发起人需要选择审批人的用户任务列表
const startUserSelectAssignees = ref<any>({}) // 发起人选择审批人的数据
const tempStartUserSelectAssignees = ref<any>({}) // 临时保存的审批人数据

const formData = ref<Partial<Leave>>({
  type: undefined,
  startTime: undefined,
  endTime: undefined,
  reason: undefined,
})
const formRules = {
  type: [{ required: true, message: '请选择请假类型' }],
  startTime: [{ required: true, message: '请选择开始时间' }],
  endTime: [{ required: true, message: '请选择结束时间' }],
  reason: [{ required: true, message: '请输入请假原因' }],
}
const formRef = ref<FormInstance>()

// 计算请假天数
const leaveDays = computed(() => {
  if (!formData.value.startTime || !formData.value.endTime) {
    return 0
  }
  const start = new Date(formData.value.startTime)
  const end = new Date(formData.value.endTime)
  return Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24))
})

/** 返回上一页 */
function handleBack() {
  message.confirm({
    title: '提示',
    msg: '确定要返回吗？请先保存您填写的信息！',
  }).then(({ action }) => {
    if (action === 'confirm') {
      navigateBackPlus('/pages-bpm/oa/leave/index')
    }
  })
}

/** 获取流程审批详情 */
async function getProcessApprovalDetail() {
  if (!processDefinitionId.value) {
    return
  }

  processTimeLineLoading.value = true
  try {
    const data = await getApprovalDetail({
      processDefinitionId: processDefinitionId.value,
      activityId: BpmNodeIdEnum.START_USER_NODE_ID,
      processVariablesStr: JSON.stringify({
        day: leaveDays.value,
      }),
    })

    if (!data) {
      toast.show('查询不到审批详情信息！')
      return
    }

    // 获取审批节点，显示 Timeline 的数据
    activityNodes.value = data.activityNodes || []

    // 获取发起人自选的任务
    startUserSelectTasks.value = data.activityNodes?.filter(
      (node: ApprovalNodeInfo) =>
        BpmCandidateStrategyEnum.START_USER_SELECT === node.candidateStrategy,
    ) || []

    // 恢复之前的选择审批人
    if (startUserSelectTasks.value.length > 0) {
      for (const node of startUserSelectTasks.value) {
        startUserSelectAssignees.value[node.id]
          = tempStartUserSelectAssignees.value[node.id]
            && tempStartUserSelectAssignees.value[node.id].length > 0
            ? tempStartUserSelectAssignees.value[node.id]
            : []
      }
    }
  } catch (error) {
    console.error('获取流程审批详情失败:', error)
  } finally {
    processTimeLineLoading.value = false
  }
}

/** 选择审批人确认 */
function selectUserConfirm(id: string, userList: any[]) {
  startUserSelectAssignees.value[id] = userList?.map((item: any) => item.id) || []
}

/** 提交表单 */
async function handleSubmit() {
  const { valid } = await formRef.value!.validate()
  if (!valid) {
    return
  }
  if (formData.value.startTime! >= formData.value.endTime!) {
    toast.show('结束时间必须大于开始时间')
    return
  }

  // 校验指定审批人
  if (startUserSelectTasks.value.length > 0) {
    for (const userTask of startUserSelectTasks.value) {
      if (
        Array.isArray(startUserSelectAssignees.value[userTask.id])
        && startUserSelectAssignees.value[userTask.id].length === 0
      ) {
        toast.show(`请选择${userTask.name}的审批人`)
        return
      }
    }
  }

  formLoading.value = true
  try {
    const submitData = { ...formData.value }
    // 设置指定审批人
    if (startUserSelectTasks.value.length > 0) {
      submitData.startUserSelectAssignees = startUserSelectAssignees.value
    }

    await createLeave(submitData)
    uni.showToast({ title: '提交成功', icon: 'success' })
    setTimeout(() => {
      navigateBackPlus('/pages-bpm/oa/leave/index')
    }, 1500)
  } finally {
    formLoading.value = false
  }
}

// 监听表单数据变化，重新预测流程节点
watch(
  () => [formData.value.startTime, formData.value.endTime, formData.value.type],
  (newValue, oldValue) => {
    if (!oldValue || !oldValue.some(v => v !== undefined)) {
      return
    }
    if (newValue && newValue.some(v => v !== undefined)) {
      // 记录之前的节点审批人
      tempStartUserSelectAssignees.value = { ...startUserSelectAssignees.value }
      startUserSelectAssignees.value = {}
      // 加载最新的审批详情，主要用于节点预测
      getProcessApprovalDetail()
    }
  },
  { deep: true },
)

// 组件初始化
onMounted(async () => {
  try {
    // 获取流程定义详情
    const processDefinitionDetail = await getProcessDefinition(undefined, processDefineKey)
    if (!processDefinitionDetail) {
      toast.show('OA 请假的流程模型未配置，请检查！')
      return
    }
    processDefinitionId.value = processDefinitionDetail.id

    // 获取流程审批详情
    await getProcessApprovalDetail()
  } catch (error) {
    console.error('初始化流程失败:', error)
    toast.show('初始化流程失败，请稍后重试')
  }
})
</script>
