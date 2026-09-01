<template>
  <ContentWrap>
    <div class="page-header">
      <div>
        <div class="page-title">同步任务监控</div>
        <div class="page-subtitle">实时、夜间及近 30 天补偿任务状态</div>
      </div>
      <div class="page-actions">
        <el-button @click="router.push('/cps/order')">
          <Icon icon="ep:arrow-left" class="mr-5px" />返回订单管理
        </el-button>
        <el-button @click="loadSyncMonitor"
          ><Icon icon="ep:refresh" class="mr-5px" />刷新</el-button
        >
        <el-button type="primary" plain @click="syncBatchDialogVisible = true">
          <Icon icon="ep:plus" class="mr-5px" />创建补偿批次
        </el-button>
      </div>
    </div>

    <div class="sync-metrics-grid">
      <div
        ><span>运行中批次</span><b>{{ syncMetrics.runningBatches ?? '-' }}</b></div
      >
      <div
        ><span>待执行窗口</span><b>{{ syncMetrics.pendingWindows ?? '-' }}</b></div
      >
      <div
        ><span>重试窗口</span><b>{{ syncMetrics.retryWindows ?? '-' }}</b></div
      >
      <div
        ><span>死信窗口</span><b class="danger-text">{{ syncMetrics.deadWindows ?? '-' }}</b></div
      >
      <div
        ><span>窗口成功率</span><b>{{ formatRate(syncMetrics.successRate) }}</b></div
      >
      <div
        ><span>最大延迟</span
        ><b>{{
          syncMetrics.maxDelayMinutes != null ? `${syncMetrics.maxDelayMinutes}分钟` : '-'
        }}</b></div
      >
    </div>

    <el-table v-loading="syncBatchLoading" :data="syncBatches" size="small">
      <el-table-column label="批次" prop="id" width="80" />
      <el-table-column label="平台" width="90"
        ><template #default="scope">{{
          platformLabel(scope.row.platformCode)
        }}</template></el-table-column
      >
      <el-table-column label="轨道" width="100"
        ><template #default="scope">{{
          syncQueryTypeLabel(scope.row.queryType)
        }}</template></el-table-column
      >
      <el-table-column label="时间范围" min-width="250"
        ><template #default="scope"
          >{{ formatSyncTime(scope.row.startTime) }} 至 {{ formatSyncTime(scope.row.endTime) }}</template
        ></el-table-column
      >
      <el-table-column label="进度" width="150"
        ><template #default="scope"
          >{{ scope.row.successWindows || 0 }}/{{ scope.row.totalWindows || 0 }}</template
        ></el-table-column
      >
      <el-table-column label="状态" width="110"
        ><template #default="scope"
          ><el-tag size="small" :type="syncStatusTagType(scope.row.status)">{{
            syncStatusLabel(scope.row.status)
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="操作" fixed="right" width="260">
        <template #default="scope">
          <el-button link type="primary" @click="openSyncWindows(scope.row)">窗口</el-button>
          <el-button
            v-if="scope.row.status === 'RUNNING'"
            link
            type="warning"
            @click="operateSyncBatch(scope.row, 'pause')"
            >暂停</el-button
          >
          <el-button
            v-if="scope.row.status === 'PAUSED'"
            link
            type="success"
            @click="operateSyncBatch(scope.row, 'resume')"
            >恢复</el-button
          >
          <el-button
            v-if="['RUNNING', 'PAUSED'].includes(scope.row.status)"
            link
            type="danger"
            @click="operateSyncBatch(scope.row, 'cancel')"
            >取消</el-button
          >
          <el-button
            v-if="scope.row.status !== 'RUNNING'"
            link
            type="danger"
            @click="deleteSyncBatch(scope.row)"
            >删除</el-button
          >
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <el-dialog
    v-model="syncBatchDialogVisible"
    title="创建同步补偿批次"
    width="520px"
    destroy-on-close
  >
    <el-form label-width="90px">
      <el-form-item label="平台" required
        ><el-select v-model="syncBatchForm.platformCode" class="w-full"
          ><el-option label="淘宝" value="taobao" /><el-option label="京东" value="jd" /><el-option
            label="拼多多"
            value="pdd" /></el-select
      ></el-form-item>
      <el-form-item label="同步轨道" required
        ><el-select v-model="syncBatchForm.queryType" class="w-full"
          ><el-option :value="2" label="付款订单" /><el-option
            :value="3"
            label="结算订单" /><el-option :value="4" label="订单更新时间" /></el-select
      ></el-form-item>
      <el-form-item label="时间范围" required
        ><el-date-picker
          v-model="syncBatchForm.dateRange"
          type="datetimerange"
          value-format="YYYY-MM-DD HH:mm:ss"
          class="w-full"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="syncBatchDialogVisible = false">取消</el-button
      ><el-button type="primary" :loading="syncBatchCreating" @click="createSyncBatch"
        >创建</el-button
      ></template
    >
  </el-dialog>

  <el-dialog v-model="syncWindowDialogVisible" title="同步窗口" width="900px" destroy-on-close>
    <div v-loading="syncWindowsLoading" class="sync-window-panel">
      <div class="sync-window-legend">
        <span><i class="sync-window-dot is-success"></i>已执行</span>
        <span><i class="sync-window-dot is-pending"></i>未执行</span>
        <span class="sync-window-page-hint">每页 10 天</span>
      </div>
      <el-empty
        v-if="!syncWindowsLoading && !syncWindowDateRows.length"
        description="暂无同步窗口"
        :image-size="72"
      />
      <div v-else class="sync-window-list">
        <div
          v-for="dateRow in pagedSyncWindowDateRows"
          :key="dateRow.date"
          class="sync-window-day"
        >
          <div class="sync-window-date">{{ dateRow.date }}</div>
          <div class="sync-window-points">
            <el-popover
              v-for="window in dateRow.windows"
              :key="window.id"
              placement="top"
              :width="280"
              trigger="hover"
            >
              <template #reference>
                <button
                  type="button"
                  class="sync-window-point"
                  :class="windowStatusClass(window)"
                  :aria-label="formatWindowPointLabel(window)"
                >
                  <i class="sync-window-dot"></i>
                  <span>{{ formatWindowPointTime(window.windowStart) }}</span>
                </button>
              </template>
              <div class="sync-window-popover">
                <div>
                  {{ formatSyncTime(window.windowStart) }} 至
                  {{ formatSyncTime(window.windowEnd) }}
                </div>
                <div>状态：{{ syncStatusLabel(window.status) }}</div>
                <div v-if="window.retryCount">重试：{{ window.retryCount }} 次</div>
                <div v-if="window.lastErrorMessage" class="sync-window-error">
                  {{ window.lastErrorMessage }}
                </div>
                <el-button
                  v-if="window.status === 'DEAD'"
                  link
                  type="primary"
                  class="mt-4px"
                  @click="replaySyncWindow(window)"
                >
                  重放
                </el-button>
              </div>
            </el-popover>
          </div>
        </div>
      </div>
      <el-pagination
        v-if="syncWindowDateRows.length > syncWindowPageSize"
        v-model:current-page="syncWindowPageNo"
        class="sync-window-pagination"
        background
        layout="prev, pager, next, total"
        :page-size="syncWindowPageSize"
        :total="syncWindowDateRows.length"
      />
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import * as OrderApi from '@/api/cps/order'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'

defineOptions({ name: 'CpsOrderSyncMonitor' })

type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'
const router = useRouter()
const message = useMessage()
const syncBatches = ref<OrderApi.CpsOrderSyncBatchVO[]>([])
const syncBatchLoading = ref(false)
const syncMetrics = ref<OrderApi.CpsOrderSyncMetricsVO>({
  runningBatches: 0,
  pendingWindows: 0,
  retryWindows: 0,
  deadWindows: 0,
  successRate: 0
})
const syncBatchDialogVisible = ref(false)
const syncBatchCreating = ref(false)
const syncBatchForm = reactive<{
  platformCode: string
  queryType: number
  dateRange: [string, string] | null
}>({ platformCode: 'taobao', queryType: 4, dateRange: null })
const syncWindowDialogVisible = ref(false)
const syncWindows = ref<OrderApi.CpsOrderSyncWindowVO[]>([])
const syncWindowsLoading = ref(false)
const syncWindowPageNo = ref(1)
const syncWindowPageSize = 10
const syncWindowDateRows = computed(() => {
  const rows = new Map<string, OrderApi.CpsOrderSyncWindowVO[]>()
  syncWindows.value.forEach((window) => {
    const date = dayjs(window.windowStart).format('YYYY-MM-DD')
    const windows = rows.get(date) || []
    windows.push(window)
    rows.set(date, windows)
  })
  return Array.from(rows, ([date, windows]) => ({ date, windows }))
})
const pagedSyncWindowDateRows = computed(() => {
  const start = (syncWindowPageNo.value - 1) * syncWindowPageSize
  return syncWindowDateRows.value.slice(start, start + syncWindowPageSize)
})

const platformLabel = (code: string) =>
  ({ taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音', eleme: '淘宝闪购' })[code] || code
const syncStatusLabel = (status?: string) =>
  ({
    PENDING: '待执行',
    RUNNING: '执行中',
    PAUSED: '已暂停',
    RETRY_WAIT: '等待重试',
    SUCCESS: '已完成',
    DEAD: '死信',
    CANCELLED: '已取消'
  })[status || ''] ||
  status ||
  '-'
const syncStatusTagType = (status?: string): ElTagType =>
  (({
    SUCCESS: 'success',
    RUNNING: 'primary',
    PAUSED: 'warning',
    RETRY_WAIT: 'warning',
    DEAD: 'danger',
    CANCELLED: 'info'
  })[status || ''] as ElTagType) || 'info'
const syncQueryTypeLabel = (queryType?: number) =>
  ({ 1: '创建时间', 2: '付款时间', 3: '结算时间', 4: '更新时间' })[queryType || 0] || '-'
const formatRate = (rate?: number) => (rate == null ? '-' : `${(rate * 100).toFixed(1)}%`)
const formatSyncTime = (value?: string | number) =>
  value == null ? '-' : dayjs(value).format('YYYY-MM-DD HH:mm:ss')
const formatWindowPointTime = (value?: string | number) =>
  value == null ? '-' : dayjs(value).format('HH:mm')
const formatWindowPointLabel = (window: OrderApi.CpsOrderSyncWindowVO) =>
  formatSyncTime(window.windowStart) + '，' + syncStatusLabel(window.status)
const windowStatusClass = (window: OrderApi.CpsOrderSyncWindowVO) =>
  window.status === 'SUCCESS' ? 'is-success' : 'is-pending'

const loadSyncMonitor = async () => {
  syncBatchLoading.value = true
  try {
    const [page, metrics] = await Promise.all([
      OrderApi.getOrderSyncBatchPage({ pageNo: 1, pageSize: 10 }),
      OrderApi.getOrderSyncMetrics()
    ])
    syncBatches.value = page.list || []
    syncMetrics.value = metrics || syncMetrics.value
  } finally {
    syncBatchLoading.value = false
  }
}
const createSyncBatch = async () => {
  const [startTime, endTime] = syncBatchForm.dateRange || []
  if (!startTime || !endTime) return message.warning('请选择时间范围')
  syncBatchCreating.value = true
  try {
    await OrderApi.createOrderSyncBatch({
      platformCode: syncBatchForm.platformCode,
      vendorCode: syncBatchForm.platformCode === 'taobao' ? 'dataoke' : undefined,
      queryType: syncBatchForm.queryType,
      startTime: dayjs(startTime).valueOf(),
      endTime: dayjs(endTime).valueOf(),
      batchType: 'MANUAL'
    })
    message.success('同步批次已创建')
    syncBatchDialogVisible.value = false
    await loadSyncMonitor()
  } finally {
    syncBatchCreating.value = false
  }
}
const operateSyncBatch = async (
  row: OrderApi.CpsOrderSyncBatchVO,
  action: 'pause' | 'resume' | 'cancel'
) => {
  const fn =
    action === 'pause'
      ? OrderApi.pauseOrderSyncBatch
      : action === 'resume'
        ? OrderApi.resumeOrderSyncBatch
        : OrderApi.cancelOrderSyncBatch
  await fn(row.id)
  message.success('操作成功')
  await loadSyncMonitor()
}
const deleteSyncBatch = async (row: OrderApi.CpsOrderSyncBatchVO) => {
  try {
    await message.delConfirm('确认删除该同步批次及其全部窗口吗？')
    await OrderApi.deleteOrderSyncBatch(row.id)
    message.success('同步批次已删除')
    await loadSyncMonitor()
  } catch {}
}
const openSyncWindows = async (row: OrderApi.CpsOrderSyncBatchVO) => {
  syncWindowsLoading.value = true
  syncWindowPageNo.value = 1
  syncWindowDialogVisible.value = true
  try {
    syncWindows.value = await loadAllSyncWindows(row.id)
  } finally {
    syncWindowsLoading.value = false
  }
}
const loadAllSyncWindows = async (batchId: number) => {
  const pageSize = 200
  const windows: OrderApi.CpsOrderSyncWindowVO[] = []
  let pageNo = 1
  let total = 0
  do {
    const page = await OrderApi.getOrderSyncBatchWindows(batchId, { pageNo, pageSize })
    const list = page.list || []
    windows.push(...list)
    total = page.total || 0
    pageNo += 1
    if (!list.length) break
  } while (windows.length < total)
  return windows
}
const replaySyncWindow = async (row: OrderApi.CpsOrderSyncWindowVO) => {
  await OrderApi.replayOrderSyncWindow(row.id)
  message.success('窗口已重新加入队列')
  if (row.batchId)
    await openSyncWindows(
      syncBatches.value.find((batch) => batch.id === row.batchId) ||
        ({ id: row.batchId } as OrderApi.CpsOrderSyncBatchVO)
    )
}

onMounted(loadSyncMonitor)
</script>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.page-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.page-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.sync-metrics-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}
.sync-metrics-grid > div {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}
.sync-metrics-grid span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
.sync-metrics-grid b {
  font-size: 18px;
}
.danger-text {
  color: var(--el-color-danger);
}
.sync-window-panel {
  min-height: 180px;
}
.sync-window-legend {
  display: flex;
  align-items: center;
  gap: 18px;
  margin-bottom: 12px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}
.sync-window-legend span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.sync-window-page-hint {
  margin-left: auto;
}
.sync-window-list {
  border-top: 1px solid var(--el-border-color-lighter);
}
.sync-window-day {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  min-height: 56px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.sync-window-date {
  color: var(--el-text-color-regular);
  font-weight: 600;
}
.sync-window-points {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 8px 0;
}
.sync-window-point {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 0;
  border: 0;
  background: transparent;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  font-size: 12px;
}
.sync-window-point:hover span {
  color: var(--el-color-primary);
}
.sync-window-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--el-color-danger);
}
.sync-window-point.is-success .sync-window-dot,
.sync-window-legend .is-success {
  background: var(--el-color-success);
}
.sync-window-point.is-pending .sync-window-dot,
.sync-window-legend .is-pending {
  background: var(--el-color-danger);
}
.sync-window-popover {
  display: grid;
  gap: 4px;
  line-height: 1.5;
}
.sync-window-error {
  color: var(--el-color-danger);
  overflow-wrap: anywhere;
}
.sync-window-pagination {
  justify-content: flex-end;
  margin-top: 16px;
}
@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
  .sync-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .sync-window-day {
    grid-template-columns: 1fr;
    gap: 0;
    padding: 10px 0;
  }
  .sync-window-points {
    gap: 10px;
  }
}
</style>
