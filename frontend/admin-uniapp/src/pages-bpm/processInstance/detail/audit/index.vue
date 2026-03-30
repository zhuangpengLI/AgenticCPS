<template>
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      :title="isApprove ? '审批同意' : '审批拒绝'"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 审批表单 -->
    <view class="p-24rpx">
      <wd-form ref="formRef" :model="formData" :rules="formRules">
        <wd-cell-group border>
          <!-- 下一个节点的审批人 -->
          <view v-if="isApprove && nextAssigneesActivityNode.length > 0" class="p-24rpx">
            <view class="mb-16rpx flex items-center">
              <text class="mr-8rpx text-[#f56c6c]">*</text>
              <text class="text-28rpx text-[#333]">下一个节点的审批人</text>
            </view>
            <ProcessInstanceTimeline
              :activity-nodes="nextAssigneesActivityNode"
              :show-status-icon="false"
              :enable-approve-user-select="true"
              @select-user-confirm="selectNextAssigneesConfirm"
            />
          </view>

          <!-- 签名 -->
          <view v-if="isApprove && taskInfo?.signEnable" class="border-b border-[#eee] p-24rpx">
            <view class="mb-16rpx flex items-center">
              <text class="mr-8rpx text-[#f56c6c]">*</text>
              <text class="text-28rpx text-[#333]">签名</text>
            </view>
            <view class="flex items-center gap-16rpx">
              <wd-button type="primary" size="small" @click="openSignatureModal">
                {{ formData.signPicUrl ? '重新签名' : '点击签名' }}
              </wd-button>
              <image
                v-if="formData.signPicUrl"
                :src="formData.signPicUrl"
                class="h-80rpx w-192rpx"
                mode="aspectFit"
                @click="previewSignature"
              />
            </view>
          </view>

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
      </wd-form>

      <!-- 提交按钮 -->
      <view class="mt-48rpx">
        <wd-button
          :type="isApprove ? 'primary' : 'error'"
          block
          :loading="formLoading"
          :disabled="formLoading"
          @click="handleSubmit"
        >
          {{ isApprove ? '同意' : '拒绝' }}
        </wd-button>
      </view>
    </view>

    <!-- 签名弹窗 -->
    <wd-popup v-model="showSignatureModal" position="bottom" custom-style="height: 60vh;">
      <view class="h-full flex flex-col">
        <view class="flex items-center justify-between border-b border-[#eee] p-24rpx">
          <text class="text-32rpx text-[#333] font-bold">手写签名</text>
          <wd-icon name="close" size="40rpx" @click="showSignatureModal = false" />
        </view>
        <view class="flex-1 p-24rpx">
          <wd-signature
            :height="300"
            :export-scale="2"
            background-color="#ffffff"
            @confirm="handleSignatureConfirm"
            @clear="handleSignatureClear"
          />
        </view>
      </view>
    </wd-popup>
  </view>
</template>

<script lang="ts" setup>
import type { FormInstance } from 'wot-design-uni/components/wd-form/types'
import type { ApprovalNodeInfo } from '@/api/bpm/processInstance'
import type { Task } from '@/api/bpm/task'
import { computed, onMounted, reactive, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import { getApprovalDetail, getNextApproveNodes } from '@/api/bpm/processInstance'
import { approveTask, rejectTask } from '@/api/bpm/task'
import ProcessInstanceTimeline from '@/pages-bpm/processInstance/detail/components/time-line.vue'
import { getEnvBaseUrl, navigateBackPlus } from '@/utils'
import { BpmCandidateStrategyEnum } from '@/utils/constants'

const props = defineProps<{
  processInstanceId?: string
  taskId?: string
  pass?: string
}>()

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})
const taskId = computed(() => props.taskId || '')
const processInstanceId = computed(() => props.processInstanceId)
const isApprove = computed(() => props.pass !== 'false') // true: 同意, false: 拒绝
const toast = useToast()
const formLoading = ref(false)
const taskInfo = ref<Task | null>(null) // 任务信息

const nextAssigneesActivityNode = ref<ApprovalNodeInfo[]>([]) // 下一个节点审批人列表
const approveUserSelectTasks = ref<ApprovalNodeInfo[]>([]) // 需要选择审批人的节点列表
const approveUserSelectAssignees = ref<Record<string, number[]>>({}) // 审批人选择的审批人数据

// 签名相关
const showSignatureModal = ref(false)

const formData = reactive({
  reason: '',
  signPicUrl: '', // 签名图片 URL
})

const formRules = computed(() => {
  let rules = {}
  if (taskInfo.value?.reasonRequire) {
    rules = {
      reason: [
        { required: true, message: '审批意见不能为空' },
      ],
    }
  }
  return rules
})

const formRef = ref<FormInstance>()

/** 返回上一页 */
function handleBack() {
  navigateBackPlus(`/pages-bpm/processInstance/detail/index?id=${processInstanceId.value}&taskId=${taskId.value}`)
}

/** 加载任务信息 */
async function loadTaskInfo() {
  const data = await getApprovalDetail({
    processInstanceId: processInstanceId.value,
    taskId: taskId.value,
  })
  taskInfo.value = data?.todoTask || null
}

/** 加载下一个节点审批人 */
async function loadNextApproveNodes() {
  if (!isApprove.value) {
    return
  }
  const params = {
    processInstanceId: processInstanceId.value,
    taskId: taskId.value,
  }
  const data = await getNextApproveNodes(params)
  if (data && data.length > 0) {
    nextAssigneesActivityNode.value = data
    // 获取审批人自选的任务
    approveUserSelectTasks.value = data.filter(
      (node: ApprovalNodeInfo) =>
        BpmCandidateStrategyEnum.APPROVE_USER_SELECT === node.candidateStrategy,
    ) || []
  }
}

/** 选择下一个节点审批人确认 */
function selectNextAssigneesConfirm(activityId: string, userList: any[]) {
  approveUserSelectAssignees.value[activityId] = userList.map(user => user.id)
}

/** 打开签名弹窗 */
function openSignatureModal() {
  showSignatureModal.value = true
}

/** 签名确认 */
async function handleSignatureConfirm(result: { tempFilePath: string, base64: string }) {
  toast.loading('上传中...')
  try {
    // 上传签名图片
    const url = await uploadSignatureFile(result.tempFilePath)
    formData.signPicUrl = url
    showSignatureModal.value = false
    toast.success('签名成功')
  } catch (err) {
    console.error('上传失败：', err)
    toast.show('上传失败')
  }
}

/** 上传签名文件 */
function uploadSignatureFile(tempFilePath: string): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.uploadFile({
      url: `${getEnvBaseUrl()}/infra/file/upload`,
      filePath: tempFilePath,
      name: 'file',
      success: (uploadFileRes) => {
        try {
          const data = JSON.parse(uploadFileRes.data)
          if (data.code === 0 && data.data) {
            resolve(data.data)
          } else {
            reject(new Error(data.msg || '上传失败'))
          }
        } catch (err) {
          reject(err)
        }
      },
      fail: (err) => {
        console.error('上传失败:', err)
        reject(err)
      },
    })
  })
}

/** 签名清除 */
function handleSignatureClear() {
  formData.signPicUrl = ''
}

/** 预览签名 */
function previewSignature() {
  if (formData.signPicUrl) {
    uni.previewImage({
      urls: [formData.signPicUrl],
      current: formData.signPicUrl,
    })
  }
}

/** 提交审批 */
async function handleSubmit() {
  if (formLoading.value) {
    return
  }

  const { valid } = await formRef.value!.validate()
  if (!valid) {
    return
  }
  // 验证签名
  if (isApprove.value && taskInfo.value?.signEnable && !formData.signPicUrl) {
    toast.show('请先进行签名')
    return
  }

  // 验证审批人选择
  if (isApprove.value && approveUserSelectTasks.value.length > 0) {
    for (const task of approveUserSelectTasks.value) {
      if (!approveUserSelectAssignees.value[task.id] || approveUserSelectAssignees.value[task.id].length === 0) {
        toast.show(`请选择「${task.name}」的审批人`)
        return
      }
    }
  }

  formLoading.value = true
  try {
    if (isApprove.value) {
      // 审批通过
      await approveTask({
        id: taskId.value as string,
        reason: formData.reason,
        signPicUrl: formData.signPicUrl || undefined,
        nextAssignees: Object.keys(approveUserSelectAssignees.value).length > 0
          ? approveUserSelectAssignees.value
          : undefined,
      })
    } else {
      // 审批拒绝
      await rejectTask({
        id: taskId.value as string,
        reason: formData.reason,
      })
    }
    toast.success('审批成功')
    setTimeout(() => {
      uni.redirectTo({
        url: `/pages-bpm/processInstance/detail/index?id=${processInstanceId.value}&taskId=${taskId.value}`,
      })
    }, 1000)
  } finally {
    formLoading.value = false
  }
}

/** 页面加载时 */
onMounted(async () => {
  /** 初始化校验 */
  if (!props.taskId || !props.processInstanceId) {
    toast.show('参数错误')
    return
  }
  try {
    toast.loading('加载中...')
    // 加载任务信息和下一个节点审批人
    await loadTaskInfo()
    await loadNextApproveNodes()
  } finally {
    toast.close()
  }
})
</script>
