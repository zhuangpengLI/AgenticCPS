<template>
  <!-- 遍历每个审批节点 -->
  <view
    v-for="(activity, index) in activityNodes"
    :key="activity.id || index"
    class="relative pb-24rpx pl-80rpx"
  >
    <!-- 时间线圆点 -->
    <view
      class="absolute left-12rpx top-8rpx h-52rpx w-52rpx flex items-center justify-center rounded-full bg-blue-500"
    >
      <!-- 节点类型图标 -->
      <wd-icon
        :name="getApprovalNodeTypeIcon(activity.nodeType)"
        size="32rpx"
        color="white"
      />
    </view>

    <!-- 状态小图标 -->
    <view
      v-if="showStatusIcon"
      class="absolute left-48rpx top-44rpx h-16rpx w-16rpx flex items-center justify-center border-2 border-white rounded-full"
      :style="{ backgroundColor: getApprovalNodeColor(activity.status) }"
    >
      <wd-icon
        :name="getApprovalNodeIcon(activity.status, activity.nodeType)"
        size="12rpx"
        color="white"
      />
    </view>

    <!-- 连接线 -->
    <view
      v-if="index < activityNodes.length - 1"
      class="absolute bottom-0 left-38rpx top-64rpx w-2rpx bg-[#e5e5e5]"
    />

    <!-- 节点内容 -->
    <view class="ml-8rpx">
      <!-- 第一行：节点名称、时间 -->
      <view class="mb-8rpx flex items-center justify-between">
        <view class="flex items-center">
          <text class="text-28rpx text-[#333] font-bold">{{ activity.name }}</text>
          <text v-if="activity.status === BpmTaskStatusEnum.SKIP" class="ml-8rpx text-24rpx text-[#999]">
            【跳过】
          </text>
        </view>
        <text
          v-if="activity.status !== BpmTaskStatusEnum.NOT_START && getApprovalNodeTime(activity)"
          class="text-22rpx text-[#999]"
        >
          {{ getApprovalNodeTime(activity) }}
        </text>
      </view>

      <!-- 子流程节点 -->
      <view v-if="activity.nodeType === BpmNodeTypeEnum.CHILD_PROCESS_NODE" class="mb-16rpx">
        <wd-button
          type="primary"
          plain
          size="small"
          :disabled="!activity.processInstanceId"
          @click="handleChildProcess(activity)"
        >
          查看子流程
        </wd-button>
      </view>

      <!-- 需要自定义选择审批人 -->
      <view v-if="shouldShowCustomUserSelect(activity)" class="mb-16rpx">
        <view class="flex flex-wrap items-center">
          <!-- 添加用户按钮 -->
          <UserPicker
            :model-value="getSelectedUserIds(activity.id)"
            type="checkbox"
            use-default-slot
            @confirm="(users) => handleCustomUserSelectConfirm(activity.id, users)"
          >
            <view
              class="mb-8rpx mr-16rpx h-48rpx w-48rpx flex items-center justify-center border-indigo-500 rounded-lg border-solid"
            >
              <wd-icon name="user-add" size="32rpx" color="blue" />
            </view>
          </UserPicker>
          <!-- 已选择的用户 -->
          <view
            v-for="(user, userIndex) in customApproveUsers[activity.id]"
            :key="user.id || userIndex"
            class="mb-8rpx mr-16rpx flex items-center rounded-32rpx bg-[#f5f5f5] pr-16rpx"
          >
            <view class="mr-8rpx h-48rpx w-48rpx flex items-center justify-center rounded-full bg-[#1890ff] text-24rpx text-white">
              {{ user.nickname?.[0] || '?' }}
            </view>
            <text class="text-24rpx text-[#333]">{{ user.nickname }}</text>
          </view>
        </view>
      </view>

      <!-- 审批人员列表 -->
      <view v-else class="mb-16rpx">
        <!-- 情况一：遍历每个审批节点下的【进行中】task 任务 -->
        <view v-if="activity.tasks && activity.tasks.length > 0">
          <view
            v-for="(task, taskIndex) in activity.tasks"
            :key="taskIndex"
            class="mb-16rpx"
          >
            <!-- 审批人信息 -->
            <view v-if="task.assigneeUser || task.ownerUser" class="mb-8rpx flex items-center">
              <!-- TODO @jason 用户头像显示 -->
              <view class="relative mr-8rpx h-48rpx w-48rpx flex items-center justify-center rounded-full bg-[#1890ff] text-24rpx text-white">
                {{ (task.assigneeUser?.nickname || task.ownerUser?.nickname)?.[0] || '?' }}

                <!-- 任务状态小图标 -->
                <view
                  v-if="showStatusIcon && shouldShowTaskStatusIcon(task.status)"
                  class="absolute right--4rpx top-36rpx h-16rpx w-16rpx flex items-center justify-center border-2 border-white rounded-full"
                  :style="{ backgroundColor: getApprovalNodeColor(task.status) }"
                >
                  <wd-icon
                    :name="getApprovalNodeIcon(task.status, activity.nodeType)"
                    size="12rpx"
                    color="white"
                  />
                </view>
              </view>

              <view class="flex-1">
                <view class="flex items-center justify-between">
                  <view class="flex items-center">
                    <text class="text-26rpx text-[#333]">
                      {{ task.assigneeUser?.nickname || task.ownerUser?.nickname }}
                    </text>
                    <text
                      v-if="task.assigneeUser?.deptName || task.ownerUser?.deptName"
                      class="ml-8rpx text-22rpx text-[#999]"
                    >
                      {{ task.assigneeUser?.deptName || task.ownerUser?.deptName }}
                    </text>
                  </view>
                </view>
                <view class="mt-4rpx flex items-center">
                  <text :class="getStatusTextClass(task.status)" class="text-24rpx">
                    {{ getStatusText(task.status) }}
                  </text>
                </view>
              </view>
            </view>

            <!-- 审批意见 -->
            <view
              v-if="shouldShowApprovalReason(task, activity.nodeType)"
              class="mt-8rpx rounded-8rpx bg-[#f5f5f5] p-16rpx"
            >
              <text class="text-24rpx text-[#666]">审批意见：{{ task.reason }}</text>
            </view>

            <!-- 签名 -->
            <view
              v-if="task.signPicUrl && activity.nodeType === BpmNodeTypeEnum.USER_TASK_NODE"
              class="mt-8rpx flex items-center rounded-8rpx bg-[#f5f5f5] p-16rpx"
            >
              <text class="text-24rpx text-[#666]">签名：</text>
              <image
                :src="task.signPicUrl"
                class="ml-8rpx h-96rpx w-288rpx"
                mode="aspectFit"
                @click="previewImage(task.signPicUrl)"
              />
            </view>
          </view>
        </view>

        <!-- 情况二：遍历每个审批节点下的【候选的】task 任务 -->
        <view v-if="activity.candidateUsers && activity.candidateUsers.length > 0">
          <view
            v-for="(user, userIndex) in activity.candidateUsers"
            :key="userIndex"
            class="mb-8rpx flex items-center"
          >
            <view class="relative mr-8rpx h-48rpx w-48rpx flex items-center justify-center rounded-full bg-[#1890ff] text-24rpx text-white">
              {{ user.nickname?.[0] || '?' }}

              <!-- 候选状态图标 -->
              <view
                v-if="showStatusIcon"
                class="absolute right--4rpx top-36rpx h-16rpx w-16rpx flex items-center justify-center border-2 border-white rounded-full"
                :style="{ backgroundColor: getApprovalNodeColor(BpmTaskStatusEnum.NOT_START) }"
              >
                <wd-icon name="time" size="12rpx" color="white" />
              </view>
            </view>

            <view class="flex-1">
              <text class="text-26rpx text-[#333]">{{ user.nickname }}</text>
              <text v-if="user.deptName" class="ml-8rpx text-22rpx text-[#999]">
                {{ user.deptName }}
              </text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { ApprovalNodeInfo } from '@/api/bpm/processInstance'
import { ref } from 'vue'
import UserPicker from '@/components/system-select/user-picker.vue'
import { BpmCandidateStrategyEnum, BpmNodeTypeEnum, BpmTaskStatusEnum } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'

const props = withDefaults(
  defineProps<{
    activityNodes: ApprovalNodeInfo[]
    enableApproveUserSelect?: boolean
    showStatusIcon?: boolean
  }>(),
  {
    showStatusIcon: true,
    enableApproveUserSelect: false,
  },
)

const emit = defineEmits<{
  selectUserConfirm: [activityId: string, userList: any[]]
}>()

// 状态图标映射
const statusIconMap: Record<string, { color: string, icon: string }> = {
  '-2': { color: '#909398', icon: 'skip-forward' }, // 跳过
  '-1': { color: '#909398', icon: 'time' }, // 审批未开始
  '0': { color: '#f59e0b', icon: 'refresh1' }, // 待审批
  '1': { color: '#f59e0b', icon: 'refresh1' }, // 审批中
  '2': { color: '#00b32a', icon: 'check' }, // 审批通过
  '3': { color: '#f46b6c', icon: 'close' }, // 审批不通过
  '4': { color: '#cccccc', icon: 'delete' }, // 已取消
  '5': { color: '#f46b6c', icon: 'arrow-left' }, // 退回
  '6': { color: '#448ef7', icon: 'time' }, // 委派中
  '7': { color: '#00b32a', icon: 'check' }, // 审批通过中
}

// 节点类型图标映射 TODO 图标重新选一下
const nodeTypeSvgMap: Record<number, { color: string, icon: string }> = {
  [BpmNodeTypeEnum.END_EVENT_NODE]: { color: '#909398', icon: 'poweroff' },
  [BpmNodeTypeEnum.START_USER_NODE]: { color: '#909398', icon: 'user' },
  [BpmNodeTypeEnum.USER_TASK_NODE]: { color: '#ff943e', icon: 'user-talk' },
  [BpmNodeTypeEnum.TRANSACTOR_NODE]: { color: '#ff943e', icon: 'edit' },
  [BpmNodeTypeEnum.COPY_TASK_NODE]: { color: '#3296fb', icon: 'copy' },
  [BpmNodeTypeEnum.CONDITION_NODE]: { color: '#14bb83', icon: 'branch' },
  [BpmNodeTypeEnum.PARALLEL_BRANCH_NODE]: { color: '#14bb83', icon: 'branch' },
  [BpmNodeTypeEnum.CHILD_PROCESS_NODE]: { color: '#14bb83', icon: 'cluster' },
}

const onlyStatusIconShow = [BpmTaskStatusEnum.NOT_START, BpmTaskStatusEnum.RUNNING, BpmTaskStatusEnum.WAIT] // 只有状态是 -1、0、1 才展示头像右小角状态小 icon

// 响应式数据
const customApproveUsers = ref<Record<string, any[]>>({})
const showUserPicker = ref(false)
const selectedUserIds = ref<number[]>([])
const selectedActivityNodeId = ref<string>()

/** 获取审批节点类型图标 */
function getApprovalNodeTypeIcon(nodeType: number) {
  return nodeTypeSvgMap[nodeType]?.icon || 'time'
}

/** 获取审批节点图标 */
function getApprovalNodeIcon(taskStatus: number, nodeType: number) {
  if (taskStatus === BpmTaskStatusEnum.NOT_START) {
    return statusIconMap[taskStatus]?.icon || 'time'
  }
  return statusIconMap[taskStatus]?.icon || 'time'
}

/** 获取审批节点颜色 */
function getApprovalNodeColor(taskStatus: number) {
  return statusIconMap[taskStatus]?.color || '#909398'
}

/** 获取审批节点时间 */
function getApprovalNodeTime(node: ApprovalNodeInfo) {
  if (node.nodeType === BpmNodeTypeEnum.START_USER_NODE && node.startTime) {
    return formatDateTime(node.startTime)
  }
  if (node.endTime) {
    return formatDateTime(node.endTime)
  }
  if (node.startTime) {
    return formatDateTime(node.startTime)
  }
  return ''
}

/** 是否显示任务状态图标 */
function shouldShowTaskStatusIcon(status: number) {
  return onlyStatusIconShow.includes(status)
}

/** 判断是否需要显示自定义选择审批人 */
function shouldShowCustomUserSelect(activity: ApprovalNodeInfo) {
  return (
    (!activity.tasks || activity.tasks.length === 0)
    && ((BpmCandidateStrategyEnum.START_USER_SELECT === activity.candidateStrategy
      && (!activity.candidateUsers || activity.candidateUsers.length === 0))
    || (props.enableApproveUserSelect
      && BpmCandidateStrategyEnum.APPROVE_USER_SELECT === activity.candidateStrategy))
  )
}

/** 判断是否需要显示审批意见 */
function shouldShowApprovalReason(task: any, nodeType: number) {
  return (
    task.reason
    && [BpmNodeTypeEnum.END_EVENT_NODE, BpmNodeTypeEnum.USER_TASK_NODE].includes(nodeType)
  )
}

/** 获取状态文本样式类 */
function getStatusTextClass(status: number) {
  const colorMap: Record<number, string> = {
    [BpmTaskStatusEnum.RUNNING]: 'text-[#ff943e]',
    [BpmTaskStatusEnum.APPROVE]: 'text-[#00b32a]',
    [BpmTaskStatusEnum.REJECT]: 'text-[#f46b6c]',
    [BpmTaskStatusEnum.CANCEL]: 'text-[#cccccc]',
    [BpmTaskStatusEnum.RETURN]: 'text-[#f46b6c]',
  }
  return colorMap[status] || 'text-[#666]'
}

/** 获取状态文本 */
function getStatusText(status: number) {
  const textMap: Record<number, string> = {
    [BpmTaskStatusEnum.NOT_START]: '未开始',
    [BpmTaskStatusEnum.RUNNING]: '待审批',
    [BpmTaskStatusEnum.APPROVE]: '已通过',
    [BpmTaskStatusEnum.REJECT]: '已拒绝',
    [BpmTaskStatusEnum.CANCEL]: '已取消',
    [BpmTaskStatusEnum.RETURN]: '已退回',
    [BpmTaskStatusEnum.SKIP]: '已跳过',
  }
  return textMap[status] || '未知'
}

/** 用户选择确认 */
function handleCustomUserSelectConfirm(activityId: string, users: any[]) {
  customApproveUsers.value[activityId] = users || []
  emit('selectUserConfirm', activityId, users)
}

/** 获取选中的用户ID数组 */
function getSelectedUserIds(activityId: string): number[] {
  const users = customApproveUsers.value[activityId] || []
  return users.map(user => user.id).filter(id => id !== undefined)
}

/** 跳转子流程 */
function handleChildProcess(activity: ApprovalNodeInfo) {
  if (!activity.processInstanceId) {
    return
  }
  uni.navigateTo({
    url: `/pages-bpm/processInstance/detail/index?id=${activity.processInstanceId}`,
  })
}

/** 预览图片 */
function previewImage(url: string) {
  uni.previewImage({
    urls: [url],
    current: url,
  })
}

/** 设置自定义审批人 */
function setCustomApproveUsers(activityId: string, users: any[]) {
  customApproveUsers.value[activityId] = users || []
}

/** 批量设置多个节点的自定义审批人 */
function batchSetCustomApproveUsers(data: Record<string, any[]>) {
  Object.keys(data).forEach((activityId) => {
    customApproveUsers.value[activityId] = data[activityId] || []
  })
}

// 暴露方法给父组件
defineExpose({
  setCustomApproveUsers,
  batchSetCustomApproveUsers,
})
</script>
