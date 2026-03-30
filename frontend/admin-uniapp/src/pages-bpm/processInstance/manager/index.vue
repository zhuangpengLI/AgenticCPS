<template>
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      title="流程管理"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 搜索组件 -->
    <SearchForm @search="handleQuery" @reset="handleReset" />

    <!-- 流程实例列表 -->
    <view class="p-24rpx">
      <view
        v-for="item in list"
        :key="item.id"
        class="mb-24rpx overflow-hidden rounded-12rpx bg-white shadow-sm"
        @click="handleDetail(item)"
      >
        <view class="p-24rpx">
          <view class="mb-16rpx flex items-center justify-between">
            <view class="mr-16rpx flex-1">
              <view class="line-clamp-1 text-32rpx text-[#333] font-semibold">
                {{ item.name }}
              </view>
              <view class="mt-8rpx text-24rpx text-[#999]">
                {{ item.categoryName || '-' }}
              </view>
            </view>
            <DictTag :type="DICT_TYPE.BPM_PROCESS_INSTANCE_STATUS" :value="item.status" />
          </view>
          <view class="mb-12rpx flex items-center">
            <view class="mr-8rpx h-48rpx w-48rpx flex items-center justify-center rounded-full bg-[#1890ff] text-20rpx text-white">
              {{ item.startUser?.nickname?.[0] || '?' }}
            </view>
            <view class="flex-1">
              <view class="text-28rpx text-[#333]">
                {{ item.startUser?.nickname || '-' }}
              </view>
              <view class="text-24rpx text-[#999]">
                {{ item.startUser?.deptName || '-' }}
              </view>
            </view>
          </view>
          <view class="mb-12rpx rounded-8rpx bg-[#f7f8f9] p-16rpx">
            <view class="mb-8rpx flex items-center justify-between text-26rpx">
              <text class="text-[#999]">发起时间</text>
              <text class="text-[#333]">{{ formatDateTime(item.startTime) }}</text>
            </view>
            <view v-if="item.endTime" class="flex items-center justify-between text-26rpx">
              <text class="text-[#999]">结束时间</text>
              <text class="text-[#333]">{{ formatDateTime(item.endTime) }}</text>
            </view>
          </view>
          <view v-if="item.tasks && item.tasks.length > 0" class="mb-12rpx">
            <view class="mb-8rpx text-26rpx text-[#999]">
              当前审批任务
            </view>
            <view class="flex flex-wrap gap-8rpx">
              <wd-tag
                v-for="task in item.tasks"
                :key="task.id"
                type="primary"
                plain
                @click.stop="handleTaskDetail(item, task)"
              >
                {{ task.name }}
              </wd-tag>
            </view>
          </view>
          <view
            v-if="item.status === BpmProcessInstanceStatus.RUNNING"
            class="flex items-center justify-end border-t border-[#f0f0f0] -mt-8"
          >
            <wd-button size="small" type="error" plain @click.stop="handleCancel(item)">
              取消流程
            </wd-button>
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view v-if="loadMoreState !== 'loading' && list.length === 0" class="py-100rpx text-center">
        <wd-status-tip image="content" tip="暂无流程实例" />
      </view>
      <wd-loadmore
        v-if="list.length > 0"
        :state="loadMoreState"
        @reload="loadMore"
      />
    </view>
  </view>
</template>

<script lang="ts" setup>
import type { ProcessInstance } from '@/api/bpm/processInstance'
import type { LoadMoreState } from '@/http/types'
import { onReachBottom } from '@dcloudio/uni-app'
import { onMounted, ref } from 'vue'
import { useToast } from 'wot-design-uni'
import {
  cancelProcessInstanceByAdmin,
  getProcessInstanceManagerPage,
} from '@/api/bpm/processInstance'
import DictTag from '@/components/dict-tag/dict-tag.vue'
import { navigateBackPlus } from '@/utils'
import { DICT_TYPE } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import SearchForm from './components/search-form.vue'

// 流程实例状态枚举
const BpmProcessInstanceStatus = {
  RUNNING: 1, // 进行中
  APPROVE: 2, // 审批通过
  REJECT: 3, // 审批不通过
  CANCEL: 4, // 已取消
}

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const toast = useToast()
const total = ref(0)
const list = ref<(ProcessInstance & { tasks?: { id: string, name: string }[] })[]>([])
const loadMoreState = ref<LoadMoreState>('loading')
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
})

/** 返回上一页 */
function handleBack() {
  navigateBackPlus()
}

/** 查询流程实例列表 */
async function getList() {
  loadMoreState.value = 'loading'
  try {
    const data = await getProcessInstanceManagerPage(queryParams.value)
    list.value = [...list.value, ...data.list]
    total.value = data.total
    loadMoreState.value = list.value.length >= total.value ? 'finished' : 'loading'
  } catch {
    queryParams.value.pageNo = queryParams.value.pageNo > 1 ? queryParams.value.pageNo - 1 : 1
    loadMoreState.value = 'error'
  }
}

/** 搜索按钮操作 */
function handleQuery(data?: Record<string, any>) {
  queryParams.value = {
    ...data,
    pageNo: 1,
    pageSize: queryParams.value.pageSize,
  }
  list.value = []
  getList()
}

/** 重置按钮操作 */
function handleReset() {
  handleQuery()
}

/** 加载更多 */
function loadMore() {
  if (loadMoreState.value === 'finished') {
    return
  }
  queryParams.value.pageNo++
  getList()
}

/** 查看详情 */
function handleDetail(item: ProcessInstance) {
  uni.navigateTo({ url: `/pages-bpm/processInstance/detail/index?id=${item.id}` })
}

/** 查看任务详情 */
function handleTaskDetail(row: ProcessInstance, task: { id: string, name: string }) {
  uni.navigateTo({ url: `/pages-bpm/processInstance/detail/index?id=${row.id}&taskId=${task.id}` })
}

/** 取消流程实例 */
function handleCancel(item: ProcessInstance) {
  uni.showModal({
    title: '取消流程',
    editable: true,
    placeholderText: '请输入取消原因',
    success: async (res) => {
      if (!res.confirm) {
        return
      }
      const reason = res.content?.trim()
      if (!reason) {
        toast.error('请输入取消原因')
        return
      }
      try {
        await cancelProcessInstanceByAdmin(item.id, reason)
        toast.success('取消成功')
        // 刷新列表
        queryParams.value.pageNo = 1
        list.value = []
        await getList()
      } catch (error) {
        console.error('取消流程失败:', error)
      }
    },
  })
}

/** 触底加载更多 */
onReachBottom(() => {
  loadMore()
})

/** 初始化 */
onMounted(() => {
  getList()
})
</script>

<style lang="scss" scoped>
</style>
