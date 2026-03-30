<!-- 操作按钮 -->
<template>
  <!-- 有待审批的任务 -->
  <view v-if="runningTask" class="yd-detail-footer">
    <view class="w-full flex items-center">
      <!-- 左侧操作按钮 -->
      <view v-for="(action, idx) in leftOperations" :key="idx" class="mr-32rpx w-60rpx flex flex-col items-center" @click="handleOperation(action.operationType)">
        <wd-icon :name="action.iconName" size="40rpx" color="#1890ff" />
        <text class="mt-4rpx text-22rpx text-[#333]">{{ action.displayName }}</text>
      </view>
      <!-- 更多操作按钮 -->
      <view v-if="moreOperations.length > 0" class="mr-32rpx w-60rpx flex flex-col items-center" @click="handleShowMore">
        <wd-icon name="ellipsis" size="40rpx" color="#1890ff" />
        <text class="mt-4rpx text-22rpx text-[#333]">更多</text>
      </view>
      <!-- 更多操作 ActionSheet -->
      <wd-action-sheet v-if="moreOperations.length > 0" v-model="showMoreActions" :actions="moreOperations" title="请选择操作" @select="handleMoreAction" />

      <!-- 右侧按钮，TODO @jason：是否一定要保留两个按钮（需要的哈） -->
      <view class="flex flex-1 gap-16rpx">
        <wd-button
          v-for="(action, idx) in rightOperations"
          :key="idx"
          :plain="action.plain"
          :type="action.btnType"
          :round="false"
          class="flex-1"
          custom-style="min-width: 200rpx; width: 200rpx;"
          @click="handleOperation(action.operationType)"
        >
          {{ action.displayName }}
        </wd-button>
      </view>
    </view>
  </view>
  <!--  无待审批的任务 仅显示取消按钮。TODO @jason：看看还需要显示（这个微信交流下） -->
  <view v-if="!runningTask && isShowProcessStartCancel()" class="yd-detail-footer">
    <wd-button
      plain
      type="primary"
      :round="false"
      block
      @click="handleOperation(BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL)"
    >
      取消
    </wd-button>
  </view>
</template>

<script lang="ts" setup>
import type { Action } from 'wot-design-uni/components/wd-action-sheet/types'
import type { ButtonType } from 'wot-design-uni/components/wd-button/types'
import type { ProcessInstance } from '@/api/bpm/processInstance'
import type { Task } from '@/api/bpm/task'
import { useUserStore } from '@/store'
import {
  BpmProcessInstanceStatus,
  BpmTaskOperationButtonTypeEnum,
  BpmTaskStatusEnum,
  OPERATION_BUTTON_NAME,
} from '@/utils/constants'

const showMoreActions = ref(false)

type MoreOperationType = Action & {
  operationType: number
}

interface LeftOperationType {
  operationType: number
  iconName: string
  displayName: string
}

interface RightOperationType {
  operationType: number
  btnType: ButtonType
  displayName: string
  plain: boolean
}
const operationIconsMap: Record<number, string> = {
  [BpmTaskOperationButtonTypeEnum.TRANSFER]: 'transfer',
  [BpmTaskOperationButtonTypeEnum.ADD_SIGN]: 'add',
  [BpmTaskOperationButtonTypeEnum.DELEGATE]: 'share',
  [BpmTaskOperationButtonTypeEnum.RETURN]: 'arrow-left',
  [BpmTaskOperationButtonTypeEnum.COPY]: 'copy',
  [BpmTaskOperationButtonTypeEnum.DELETE_SIGN]: 'remove',
  [BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL]: 'stop-circle',
}

const userStore = useUserStore()
const leftOperations = ref<LeftOperationType[]>([]) //  左侧操作按钮 【最多两个】{转办, 委派, 退回, 加签， 抄送等}
const rightOperationTypes = [] // 右侧操作按钮【最多两个】{通过，拒绝, 取消}
const rightOperations = ref<RightOperationType[]>([])
const moreOperations = ref<MoreOperationType[]>([]) // 更多操作
const runningTask = ref<Task>()
const processInstance = ref<ProcessInstance>()
const reasonRequire = ref<boolean>(false)

/** 初始化 */
function init(theProcessInstance: ProcessInstance, task: Task) {
  processInstance.value = theProcessInstance
  runningTask.value = task
  if (task) {
    reasonRequire.value = task.reasonRequire ?? false
    // TODO @jason：这里的判断，是否可以简化哈？就是默认计算出按钮，然后根据数量，去渲染具体的按钮。
    // 右侧按钮
    if (isHandleTaskStatus() && isShowButton(BpmTaskOperationButtonTypeEnum.REJECT)) {
      rightOperationTypes.push(BpmTaskOperationButtonTypeEnum.REJECT)
      rightOperations.value.push({
        operationType: BpmTaskOperationButtonTypeEnum.REJECT,
        displayName: getButtonDisplayName(BpmTaskOperationButtonTypeEnum.REJECT),
        btnType: 'error',
        plain: true,
      })
    }
    if (isHandleTaskStatus() && isShowButton(BpmTaskOperationButtonTypeEnum.APPROVE)) {
      rightOperationTypes.push(BpmTaskOperationButtonTypeEnum.APPROVE)
      rightOperations.value.push({
        operationType: BpmTaskOperationButtonTypeEnum.APPROVE,
        displayName: getButtonDisplayName(BpmTaskOperationButtonTypeEnum.APPROVE),
        btnType: 'primary',
        plain: false,
      })
    }

    // 左侧操作，和更多操作
    Object.keys(task.buttonsSetting || {}).forEach((key) => {
      const operationType = Number(key)
      if (task.buttonsSetting[key].enable && isHandleTaskStatus()
        && !rightOperationTypes.includes(operationType)) {
        if (leftOperations.value.length >= 2) {
          moreOperations.value.push({
            name: getButtonDisplayName(operationType),
            operationType,
          })
        } else {
          leftOperations.value.push({
            operationType,
            iconName: operationIconsMap[operationType],
            displayName: getButtonDisplayName(operationType),
          })
        }
      }
    })

    // 减签操作的显示
    if (isShowDeleteSign()) {
      if (leftOperations.value.length >= 2) {
        moreOperations.value.push({
          name: getButtonDisplayName(BpmTaskOperationButtonTypeEnum.DELETE_SIGN),
          operationType: BpmTaskOperationButtonTypeEnum.DELETE_SIGN,
        })
      } else {
        leftOperations.value.push({
          operationType: BpmTaskOperationButtonTypeEnum.DELETE_SIGN,
          iconName: operationIconsMap[BpmTaskOperationButtonTypeEnum.DELETE_SIGN],
          displayName: getButtonDisplayName(BpmTaskOperationButtonTypeEnum.DELETE_SIGN),
        })
      }
    }
  }

  // 是否显示流程取消
  if (isShowProcessStartCancel()) {
    if (rightOperationTypes.length < 2) {
      rightOperationTypes.push(BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL)
      rightOperations.value.push({
        operationType: BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL,
        displayName: getButtonDisplayName(BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL),
        btnType: 'primary',
        plain: true,
      })
    } else {
      if (leftOperations.value.length >= 2) {
        moreOperations.value.push({
          name: getButtonDisplayName(BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL),
          operationType: BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL,
        })
      } else {
        leftOperations.value.push({
          operationType: BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL,
          iconName: operationIconsMap[BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL],
          displayName: getButtonDisplayName(BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL),
        })
      }
    }
  }
}

/** 跳转到相应的操作页面 */
function handleOperation(operationType: number) {
  switch (operationType) {
    case BpmTaskOperationButtonTypeEnum.APPROVE:
      uni.navigateTo({ url: `/pages-bpm/processInstance/detail/audit/index?processInstanceId=${processInstance.value.id}&taskId=${runningTask.value?.id}&pass=true` })
      break
    case BpmTaskOperationButtonTypeEnum.REJECT:
      uni.navigateTo({ url: `/pages-bpm/processInstance/detail/audit/index?processInstanceId=${processInstance.value.id}&taskId=${runningTask.value?.id}&pass=false` })
      break
    case BpmTaskOperationButtonTypeEnum.DELEGATE:
      uni.navigateTo({
        url: `/pages-bpm/processInstance/detail/reassign/index?processInstanceId=${runningTask.value.processInstanceId}&taskId=${runningTask.value.id}&type=delegate`,
      })
      break
    case BpmTaskOperationButtonTypeEnum.TRANSFER:
      uni.navigateTo({
        url: `/pages-bpm/processInstance/detail/reassign/index?processInstanceId=${runningTask.value.processInstanceId}&taskId=${runningTask.value.id}&type=transfer`,
      })
      break
    case BpmTaskOperationButtonTypeEnum.ADD_SIGN:
      uni.navigateTo({
        url: `/pages-bpm/processInstance/detail/add-sign/index?processInstanceId=${runningTask.value.processInstanceId}&taskId=${runningTask.value.id}`,
      })
      break
    case BpmTaskOperationButtonTypeEnum.RETURN:
      uni.navigateTo({
        url: `/pages-bpm/processInstance/detail/return/index?processInstanceId=${runningTask.value.processInstanceId}&taskId=${runningTask.value.id}`,
      })
      break
    case BpmTaskOperationButtonTypeEnum.DELETE_SIGN:
      uni.navigateTo({
        url: `/pages-bpm/processInstance/detail/delete-sign/index?processInstanceId=${runningTask.value.processInstanceId}&taskId=${runningTask.value.id}&children=${encodeURIComponent(JSON.stringify(runningTask.value.children || []))}`,
      })
      break
    case BpmTaskOperationButtonTypeEnum.PROCESS_START_CANCEL:
      uni.navigateTo({
        url: `/pages-bpm/processInstance/detail/process-cancel/index?processInstanceId=${processInstance.value.id}&taskId=${runningTask.value?.id}`,
      })
      break
  }
}

/** 显示更多操作 */
function handleShowMore() {
  showMoreActions.value = true
}

/** 处理更多操作选择 */
function handleMoreAction(action: { item: MoreOperationType }) {
  handleOperation(action.item.operationType)
  showMoreActions.value = false
}

/** 获取按钮的显示名称 */
function getButtonDisplayName(btnType: BpmTaskOperationButtonTypeEnum) {
  let displayName = OPERATION_BUTTON_NAME.get(btnType)
  if (
    runningTask.value?.buttonsSetting
    && runningTask.value?.buttonsSetting[btnType]
  ) {
    displayName = runningTask.value.buttonsSetting[btnType].displayName
  }
  return displayName
}

/** 是否显示按钮 */
function isShowButton(btnType: BpmTaskOperationButtonTypeEnum): boolean {
  let isShow = true
  if (
    runningTask.value?.buttonsSetting
    && runningTask.value?.buttonsSetting[btnType]
  ) {
    isShow = runningTask.value.buttonsSetting[btnType].enable
  }
  return isShow
}

/** 任务是否为处理中状态 */
function isHandleTaskStatus() {
  let canHandle = false
  if (BpmTaskStatusEnum.RUNNING === runningTask.value?.status) {
    canHandle = true
  }
  return canHandle
}

/** 流程状态是否为结束状态 */
function isEndProcessStatus(status: number) {
  let isEndStatus = false
  if (
    BpmProcessInstanceStatus.APPROVE === status
    || BpmProcessInstanceStatus.REJECT === status
    || BpmProcessInstanceStatus.CANCEL === status
  ) {
    isEndStatus = true
  }
  return isEndStatus
}

/** 流程发起人是否为当前用户 */
function isProcessStartUser() {
  let isStartUser = false
  if (userStore.userInfo?.userId === processInstance.value?.startUser?.id) {
    isStartUser = true
  }
  return isStartUser
}

/** 是否显示减签 */
function isShowDeleteSign() {
  return runningTask.value?.children?.length > 0
}

/** 是否显示流程发起人取消 */
function isShowProcessStartCancel() {
  return isProcessStartUser() && !isEndProcessStatus(processInstance.value?.status)
}

/** 暴露方法 */
defineExpose({ init })
</script>
