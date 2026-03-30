<template>
  <view class="yd-page-container">
    <!-- 顶部导航栏 -->
    <wd-navbar
      title="任务管理"
      left-arrow placeholder safe-area-inset-top fixed
      @click-left="handleBack"
    />

    <!-- 搜索组件 -->
    <SearchForm @search="handleQuery" @reset="handleReset" />

    <!-- 任务列表 -->
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
                {{ item.processInstance?.name || '-' }}
              </view>
              <view class="mt-8rpx text-24rpx text-[#999]">
                当前任务：{{ item.name }}
              </view>
            </view>
            <DictTag :type="DICT_TYPE.BPM_TASK_STATUS" :value="item.status" />
          </view>
          <view class="mb-12rpx flex items-center">
            <view class="mr-8rpx h-48rpx w-48rpx flex items-center justify-center rounded-full bg-[#1890ff] text-20rpx text-white">
              {{ item.processInstance?.startUser?.nickname?.[0] || '?' }}
            </view>
            <view class="flex-1">
              <view class="text-28rpx text-[#333]">
                发起人：{{ item.processInstance?.startUser?.nickname || '-' }}
              </view>
              <view class="text-24rpx text-[#999]">
                审批人：{{ item.assigneeUser?.nickname || '-' }}
              </view>
            </view>
          </view>
          <view class="rounded-8rpx bg-[#f7f8f9] p-16rpx">
            <view class="mb-8rpx flex items-center justify-between text-26rpx">
              <text class="text-[#999]">任务开始时间</text>
              <text class="text-[#333]">{{ formatDateTime(item.createTime) }}</text>
            </view>
            <view v-if="item.endTime" class="mb-8rpx flex items-center justify-between text-26rpx">
              <text class="text-[#999]">任务结束时间</text>
              <text class="text-[#333]">{{ formatDateTime(item.endTime) }}</text>
            </view>
            <view v-if="item.reason" class="flex items-center justify-between text-26rpx">
              <text class="text-[#999]">审批建议</text>
              <text class="line-clamp-1 ml-16rpx flex-1 text-right text-[#333]">{{ item.reason }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 加载更多 -->
      <view v-if="loadMoreState !== 'loading' && list.length === 0" class="py-100rpx text-center">
        <wd-status-tip image="content" tip="暂无任务" />
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
import type { Task } from '@/api/bpm/task'
import type { LoadMoreState } from '@/http/types'
import { onReachBottom } from '@dcloudio/uni-app'
import { onMounted, ref } from 'vue'
import { getTaskManagerPage } from '@/api/bpm/task'
import DictTag from '@/components/dict-tag/dict-tag.vue'
import { navigateBackPlus } from '@/utils'
import { DICT_TYPE } from '@/utils/constants'
import { formatDateTime } from '@/utils/date'
import SearchForm from './components/search-form.vue'

definePage({
  style: {
    navigationBarTitleText: '',
    navigationStyle: 'custom',
  },
})

const total = ref(0)
const list = ref<Task[]>([])
const loadMoreState = ref<LoadMoreState>('loading')
const queryParams = ref({
  pageNo: 1,
  pageSize: 10,
})

/** 返回上一页 */
function handleBack() {
  navigateBackPlus()
}

/** 查询任务列表 */
async function getList() {
  loadMoreState.value = 'loading'
  try {
    const data = await getTaskManagerPage(queryParams.value)
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

/** 查看详情（历史） */
function handleDetail(item: Task) {
  if (item.processInstance?.id) {
    uni.navigateTo({ url: `/pages-bpm/processInstance/detail/index?id=${item.processInstance.id}` })
  }
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
