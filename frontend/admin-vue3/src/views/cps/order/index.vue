<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="90px"
    >
      <el-form-item label="平台" prop="platformCode">
        <el-select
          v-model="queryParams.platformCode"
          placeholder="请选择平台"
          clearable
          class="!w-160px"
        >
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="orderStatus">
        <el-select
          v-model="queryParams.orderStatus"
          placeholder="请选择状态"
          clearable
          class="!w-160px"
        >
          <el-option label="已下单" value="ordered" />
          <el-option label="已付款" value="paid" />
          <el-option label="已收货" value="received" />
          <el-option label="已结算" value="settled" />
          <el-option label="已到账" value="credited" />
          <el-option label="已退款" value="refunded" />
          <el-option label="已失效" value="invalid" />
        </el-select>
      </el-form-item>
      <el-form-item label="会员名" prop="memberName">
        <el-input
          v-model="queryParams.memberName"
          placeholder="请输入会员名"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="平台单号" prop="platformOrderId">
        <el-input
          v-model="queryParams.platformOrderId"
          placeholder="请输入平台订单号"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品标题" prop="itemTitle">
        <el-input
          v-model="queryParams.itemTitle"
          placeholder="请输入商品关键词"
          clearable
          class="!w-200px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="创建时间" prop="createTime">
        <el-date-picker
          v-model="queryParams.createTime"
          value-format="YYYY-MM-DD HH:mm:ss"
          type="daterange"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
          class="!w-240px"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
        <el-button
          type="danger"
          plain
          :disabled="selectedOrderIds.length === 0"
          @click="handleBatchDelete"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
        <el-dropdown @command="handleSync" class="ml-8px">
          <el-button type="primary" plain>
            <Icon icon="ep:refresh-right" class="mr-5px" /> 同步订单
            <Icon icon="ep:arrow-down" class="ml-5px" />
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="taobao">同步淘宝</el-dropdown-item>
              <el-dropdown-item command="jd">同步京东</el-dropdown-item>
              <el-dropdown-item command="pdd">同步拼多多</el-dropdown-item>
              <el-dropdown-item command="douyin">同步抖音</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe row-key="id" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="45" />
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="平台" align="center" prop="platformCode" width="80">
        <template #default="scope">
          <el-tag :type="platformTagType(scope.row.platformCode)" size="small">
            {{ platformLabel(scope.row.platformCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="平台单号" align="left" prop="platformOrderId" min-width="180" show-overflow-tooltip />
      <el-table-column label="商品" align="left" min-width="200">
        <template #default="scope">
          <div class="flex items-center gap-2">
            <el-image
              v-if="scope.row.itemPic"
              :src="scope.row.itemPic"
              style="width: 40px; height: 40px; flex-shrink: 0"
              fit="cover"
            />
            <span class="text-sm truncate">{{ scope.row.itemTitle || '-' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="会员名" align="center" prop="memberNickname" width="120" show-overflow-tooltip>
        <template #default="scope">
          <span>{{ scope.row.memberNickname || scope.row.memberId || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="归因" align="center" width="105">
        <template #default="scope">
          <el-tag size="small" type="info">
            {{ attributionSourceLabel(scope.row.attributionSource) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="券后价" align="center" width="90">
        <template #default="scope">
          ¥{{ formatAmount(scope.row.finalPrice) }}
        </template>
      </el-table-column>
      <el-table-column label="预估返利" align="center" width="90">
        <template #default="scope">
          <span class="text-green-600">¥{{ formatAmount(scope.row.estimateRebate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实际返利" align="center" width="90">
        <template #default="scope">
          <span class="text-orange-500">¥{{ formatAmount(scope.row.realRebate) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" prop="orderStatus" width="90">
        <template #default="scope">
          <el-tag :type="orderStatusTagType(scope.row.orderStatus)" size="small">
            {{ orderStatusLabel(scope.row.orderStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="创建时间"
        align="center"
        prop="createTime"
        width="165"
        :formatter="dateFormatter"
      />
      <el-table-column label="操作" align="center" fixed="right" width="120">
        <template #default="scope">
          <el-button
            type="primary"
            link
            @click="openDetail(scope.row)"
            v-hasPermi="['cps:order:query']"
          >
            详情
          </el-button>
          <el-button
            type="danger"
            link
            @click="handleDelete(scope.row.id)"
          >
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 详情弹窗 -->
  <el-dialog
    v-model="detailVisible"
    title="订单详情"
    width="960px"
    class="order-detail-dialog"
    destroy-on-close
  >
    <el-descriptions
      v-loading="detailLoading"
      :column="2"
      border
      class="order-detail-descriptions"
      v-if="detailData"
    >
      <el-descriptions-item label="订单ID">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="平台">{{ platformLabel(detailData.platformCode) }}</el-descriptions-item>
      <el-descriptions-item label="平台单号" :span="2">{{ detailData.platformOrderId }}</el-descriptions-item>
      <el-descriptions-item label="父订单号" :span="2">{{ detailData.parentOrderId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="会员名">{{ detailData.memberNickname || detailData.memberId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="归因来源">{{ attributionSourceLabel(detailData.attributionSource) }}</el-descriptions-item>
      <el-descriptions-item label="推广位ID" :span="2">{{ detailData.adzoneId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="外部追踪">{{ detailData.externalInfo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="淘宝订单场景">{{ orderSceneLabel(detailData.orderScene) }}</el-descriptions-item>
      <el-descriptions-item label="会员运营ID">{{ detailData.specialId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="渠道关系ID">{{ detailData.relationId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="商品标题" :span="2">{{ detailData.itemTitle || '-' }}</el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            商品原价
            <el-tooltip
              content="平台订单返回的商品原价；若平台未返回原价且返回券后价/优惠券金额，后端会用券后价 + 优惠券金额推导。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.itemPrice) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            券后价
            <el-tooltip
              content="平台订单返回的实付/付款金额，优先用于订单展示和佣金核对。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.finalPrice) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            优惠券金额
            <el-tooltip
              content="平台返回的优惠券金额；若缺失且原价/券后价齐全，后端会用商品原价 - 券后价推导。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.couponAmount) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            佣金比例
            <el-tooltip content="平台返回或商品快照中的佣金比例，用于估算佣金展示。" placement="top">
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        {{ detailData.commissionRate }}%
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            预估佣金
            <el-tooltip
              content="平台预估佣金：优先使用平台订单返回的佣金金额；商品搜索/转链场景可能按券后价 x 佣金比例估算。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.commissionAmount) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            预估返利
            <el-tooltip content="预估返利 = 预估佣金 x 80%，用于订单未正式结算前展示。" placement="top">
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.estimateRebate) }}
      </el-descriptions-item>
      <el-descriptions-item>
        <template #label>
          <span class="detail-label-with-tip">
            实际返利
            <el-tooltip
              content="实际返利按返利配置结算后入账，可能受会员等级、平台配置、封顶/保底和退款扣回影响。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        ¥{{ formatAmount(detailData.realRebate) }}
      </el-descriptions-item>
      <el-descriptions-item label="订单状态">
        <el-tag :type="orderStatusTagType(detailData.orderStatus)" size="small">
          {{ orderStatusLabel(detailData.orderStatus) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="同步时间">{{ formatDate(detailData.syncTime) }}</el-descriptions-item>
      <el-descriptions-item label="确认收货时间">{{ formatDate(detailData.confirmReceiptTime) }}</el-descriptions-item>
      <el-descriptions-item label="结算时间">{{ formatDate(detailData.settleTime) }}</el-descriptions-item>
      <el-descriptions-item label="返利入账时间">{{ formatDate(detailData.rebateTime) }}</el-descriptions-item>
      <el-descriptions-item label="退款时间">{{ formatDate(detailData.refundTime) }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDate(detailData.createTime) }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import * as OrderApi from '@/api/cps/order'
import type { CpsOrderVO, CpsOrderPageReqVO } from '@/api/cps/order'

defineOptions({ name: 'CpsOrder' })

type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<CpsOrderVO[]>([])
const selectedOrderIds = ref<number[]>([])
const queryFormRef = ref()

const queryParams = reactive<CpsOrderPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  memberName: undefined,
  orderStatus: undefined,
  itemTitle: undefined,
  platformOrderId: undefined,
  createTime: undefined
})

/** 平台标签类型 */
const platformTagType = (code: string): ElTagType => {
  const map: Record<string, ElTagType> = { taobao: 'danger', jd: 'primary', pdd: 'warning', douyin: 'info' }
  return map[code] || 'info'
}
const platformLabel = (code: string) => {
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return map[code] || code
}

/** 订单状态 */
const orderStatusTagType = (status: string): ElTagType => {
  const map: Record<string, ElTagType> = {
    created: 'info',
    ordered: 'info',
    paid: 'primary',
    received: 'warning',
    settled: 'success',
    rebate_received: 'success',
    credited: 'success',
    refunded: 'danger',
    invalid: 'info'
  }
  return map[status] || 'info'
}
const orderStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    created: '已下单',
    ordered: '已下单',
    paid: '已付款',
    received: '已收货',
    settled: '已结算',
    rebate_received: '已到账',
    credited: '已到账',
    refunded: '已退款',
    invalid: '已失效'
  }
  return map[status] || status
}

/** 金额格式化（直接为元） */
const formatAmount = (val?: number) => {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

/** 日期格式化 */
const formatDate = (val?: Date | string) => {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN')
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await OrderApi.getCpsOrderPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

/** 搜索 */
const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

const attributionSourceLabel = (source?: string) => {
  const map: Record<string, string> = {
    specialId: '会员运营ID',
    relationId: '渠道ID',
    externalId: '外部追踪',
    adzone: '专属PID',
    transferRecord: '转链记录'
  }
  return source ? map[source] || source : '-'
}

const orderSceneLabel = (scene?: number) => {
  const map: Record<number, string> = {
    1: '常规订单',
    2: '渠道订单',
    3: '会员运营订单'
  }
  return scene ? map[scene] || String(scene) : '-'
}

/** 表格选择 */
const handleSelectionChange = (rows: CpsOrderVO[]) => {
  selectedOrderIds.value = rows.map((row) => row.id)
}

/** 删除 */
const handleDelete = async (id: number) => {
  try {
    await message.delConfirm()
    await OrderApi.deleteCpsOrder(id)
    message.success('删除成功')
    selectedOrderIds.value = selectedOrderIds.value.filter((selectedId) => selectedId !== id)
    await getList()
  } catch {}
}

/** 批量删除 */
const handleBatchDelete = async () => {
  const ids = [...selectedOrderIds.value]
  if (ids.length === 0) return
  try {
    await message.confirm(`确认删除选中的 ${ids.length} 条订单？`)
    await OrderApi.deleteCpsOrderList(ids)
    message.success('批量删除成功')
    selectedOrderIds.value = []
    await getList()
  } catch {}
}

/** 手动同步 */
const handleSync = async (command: string) => {
  const platformCode = command
  const syncTaobaoWithStatus = platformCode === 'taobao'
  const queryType = syncTaobaoWithStatus ? 4 : 1
  const hours = syncTaobaoWithStatus ? 24 : 2
  const actionText = syncTaobaoWithStatus ? '订单及状态' : '订单'
  try {
    await message.confirm(`确认同步 ${platformLabel(platformCode)} 最近${hours}小时${actionText}？`)
    const result = await OrderApi.syncCpsOrders(platformCode, hours, queryType)
    message.success(result || '同步任务已触发')
    await getList()
  } catch {}
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref<CpsOrderVO | null>(null)
const openDetail = async (row: CpsOrderVO) => {
  detailData.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    detailData.value = await OrderApi.getCpsOrder(row.id)
  } finally {
    detailLoading.value = false
  }
}

onMounted(getList)
</script>

<style scoped>
.order-detail-descriptions :deep(.el-descriptions__label) {
  width: 112px;
  min-width: 112px;
  white-space: nowrap;
}

.order-detail-descriptions :deep(.el-descriptions__content) {
  min-width: 220px;
}

.detail-label-with-tip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  white-space: nowrap;
}

.detail-tip-icon {
  color: var(--el-text-color-secondary);
  font-size: 14px;
}

@media (max-width: 768px) {
  .order-detail-dialog {
    width: calc(100vw - 24px) !important;
  }

  .order-detail-descriptions :deep(.el-descriptions__label) {
    width: 96px;
    min-width: 96px;
  }
}
</style>
