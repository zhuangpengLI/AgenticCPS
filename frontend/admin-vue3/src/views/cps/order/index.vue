<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form
      class="order-query-form"
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
      <el-form-item class="query-search-actions">
        <el-button @click="handleQuery"> <Icon icon="ep:search" class="mr-5px" /> 搜索 </el-button>
        <el-button @click="resetQuery"> <Icon icon="ep:refresh" class="mr-5px" /> 重置 </el-button>
      </el-form-item>
      <el-form-item class="query-actions">
        <el-button
          type="danger"
          plain
          :disabled="selectedOrderIds.length === 0"
          @click="handleBatchDelete"
        >
          <Icon icon="ep:delete" class="mr-5px" /> 批量删除
        </el-button>
        <el-button plain @click="router.push('/cps/order/sync-monitor')">
          <Icon icon="ep:data-analysis" class="mr-5px" /> 同步任务监控
        </el-button>
        <el-button
          v-hasPermi="['cps:order:attribution-bind']"
          plain
          @click="router.push('/cps/order/review')"
        >
          <Icon icon="ep:document-checked" class="mr-5px" /> 审核订单
        </el-button>
        <el-button type="primary" plain class="ml-8px" @click="openSyncDialog">
          <Icon icon="ep:refresh-right" class="mr-5px" /> 同步订单
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <el-dialog v-model="syncDialogVisible" title="同步订单" width="560px" destroy-on-close>
    <div class="sync-dialog-platform">
      <span class="sync-dialog-label">同步平台</span>
      <el-tag :type="platformTagType(syncForm.platformCode)">
        {{ platformLabel(syncForm.platformCode) }}
      </el-tag>
      <span class="sync-dialog-hint">{{ syncForm.queryType === 4 ? '订单及状态' : '订单' }}</span>
    </div>
    <el-form label-width="112px" class="sync-form">
      <el-form-item label="电商平台" required>
        <el-select
          v-model="syncForm.platformCode"
          class="w-full"
          filterable
          placeholder="请选择已接入平台"
          :loading="syncOptionLoading"
          @change="loadSyncVendors"
        >
          <el-option
            v-for="platform in syncPlatformOptions"
            :key="platform.platformCode"
            :label="platform.platformName || platformLabel(platform.platformCode)"
            :value="platform.platformCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="API 供应商" required>
        <el-select
          v-model="syncForm.vendorCode"
          class="w-full"
          filterable
          placeholder="请选择大淘客、聚推客、好单库或官方 API"
          :loading="syncVendorLoading"
          :disabled="!syncForm.platformCode"
        >
          <el-option
            v-for="vendor in syncVendorOptions"
            :key="`${vendor.vendorCode}:${vendor.platformCode}`"
            :label="syncVendorLabel(vendor)"
            :value="vendor.vendorCode"
          />
        </el-select>
        <div
          v-if="syncForm.platformCode && !syncVendorLoading && !syncVendorOptions.length"
          class="sync-field-tip sync-field-tip--danger"
        >
          当前平台没有已启用的 API 供应商，请先完成配置和连接测试。
        </div>
      </el-form-item>
      <el-form-item label="订单同步类型" required>
        <el-select v-model="syncForm.queryType" class="w-full">
          <el-option label="全部订单（按下单时间）" :value="1" />
          <el-option label="付款订单（按付款时间）" :value="2" />
          <el-option label="结算订单（按结算时间）" :value="3" />
          <el-option label="状态变更订单（按更新时间）" :value="4" />
        </el-select>
        <div class="sync-field-tip">四种方式均按大淘客接口的对应时间字段查询；日常单次不超过 3 小时，大促期间建议不超过 20 分钟，超长范围会由后端自动拆分。</div>
      </el-form-item>
      <el-form-item label="供应商状态">
        <el-input-number
          v-model="syncForm.orderStatus"
          :min="0"
          :max="99"
          :controls="false"
          class="w-full"
          placeholder="可选，填写供应商原始状态码"
        />
        <div class="sync-field-tip">留空同步全部状态；不同供应商状态码以其 API 文档为准。</div>
      </el-form-item>
      <el-form-item :label="isDataokeSync ? '开始时间' : '时间范围'" required>
        <el-date-picker
          v-if="isDataokeSync"
          v-model="syncForm.startTime"
          type="datetime"
          value-format="YYYY-MM-DD HH:mm:ss"
          placeholder="选择同步开始时间"
          class="w-full"
        />
        <div v-else class="sync-range-picker">
          <el-button-group class="sync-presets">
            <el-button
              v-for="preset in syncPresets"
              :key="preset.key"
              :type="syncForm.preset === preset.key ? 'primary' : ''"
              @click="selectSyncPreset(preset.key)"
              >{{ preset.label }}</el-button
            >
          </el-button-group>
          <el-date-picker
            v-model="syncForm.dateRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            class="sync-date-picker"
            @change="onSyncDateRangeChange"
          />
        </div>
        <div v-if="syncForm.vendorCode === 'dataoke'" class="sync-field-tip sync-field-tip--warning">
          大淘客固定同步从开始时间起的 3 小时订单；更早的漏单请创建同步补偿批次，由监控任务分窗口处理。
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="syncDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="syncLoading" @click="confirmSync">开始同步</el-button>
    </template>
  </el-dialog>

  <!-- 列表 -->
  <ContentWrap>
    <el-table
      v-loading="loading"
      :data="list"
      stripe
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" />
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="平台" align="center" prop="platformCode" width="80">
        <template #default="scope">
          <el-tag :type="platformTagType(scope.row.platformCode)" size="small">
            {{ platformLabel(scope.row.platformCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        label="平台单号"
        align="left"
        prop="platformOrderId"
        min-width="180"
        show-overflow-tooltip
      />
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
      <el-table-column
        label="会员名"
        align="center"
        prop="memberNickname"
        width="120"
        show-overflow-tooltip
      >
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
        <template #default="scope"> ¥{{ formatAmount(scope.row.finalPrice) }} </template>
      </el-table-column>
      <el-table-column label="预估返利" align="center" width="90">
        <template #default="scope">
          <span :class="scope.row.memberId ? 'text-green-600' : 'text-gray-500'">
            {{ formatEstimateRebate(scope.row) }}
          </span>
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
          <el-button type="danger" link @click="handleDelete(scope.row.id)"> 删除 </el-button>
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
      <el-descriptions-item label="平台">{{
        platformLabel(detailData.platformCode)
      }}</el-descriptions-item>
      <el-descriptions-item label="平台单号" :span="2">{{
        detailData.platformOrderId
      }}</el-descriptions-item>
      <el-descriptions-item label="父订单号" :span="2">{{
        detailData.parentOrderId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="会员名">{{
        detailData.memberNickname || detailData.memberId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="归因来源">{{
        attributionSourceLabel(detailData.attributionSource)
      }}</el-descriptions-item>
      <el-descriptions-item label="推广位ID" :span="2">{{
        detailData.adzoneId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="外部追踪">{{
        detailData.externalInfo || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="淘宝订单场景">{{
        orderSceneLabel(detailData.orderScene)
      }}</el-descriptions-item>
      <el-descriptions-item label="special_id">
        <div class="attribution-field">
          <span>{{ detailData.specialId || '-' }}</span>
          <el-button
            v-if="detailData.specialId"
            type="primary"
            link
            :disabled="!detailData.adzoneId || bindSpecialIdLoading"
            @click="handleBindSpecialId"
            v-hasPermi="['cps:order:attribution-bind']"
          >
            手动绑定会员
          </el-button>
        </div>
      </el-descriptions-item>
      <el-descriptions-item label="relation_id">{{
        detailData.relationId || '-'
      }}</el-descriptions-item>
      <el-descriptions-item label="商品标题" :span="2">{{
        detailData.itemTitle || '-'
      }}</el-descriptions-item>
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
            <el-tooltip
              content="平台返回或商品快照中的佣金比例，用于估算佣金展示。"
              placement="top"
            >
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
            <el-tooltip
              content="预估返利使用与正式结算一致的会员、等级与平台返利规则；未可信归因时不做比例猜测。"
              placement="top"
            >
              <Icon icon="ep:question-filled" class="detail-tip-icon" />
            </el-tooltip>
          </span>
        </template>
        {{ formatEstimateRebate(detailData) }}
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
      <el-descriptions-item label="同步时间">{{
        formatDate(detailData.syncTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="确认收货时间">{{
        formatDate(detailData.confirmReceiptTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="结算时间">{{
        formatDate(detailData.settleTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="返利入账时间">{{
        formatDate(detailData.rebateTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="退款时间">{{
        formatDate(detailData.refundTime)
      }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{
        formatDate(detailData.createTime)
      }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>

  <el-dialog
    v-model="bindSpecialIdDialogVisible"
    title="手动绑定会员"
    width="420px"
    destroy-on-close
  >
    <el-form label-width="88px">
      <el-form-item label="special_id">
        <el-input :model-value="bindSpecialIdForm.specialId" disabled />
      </el-form-item>
      <el-form-item label="会员">
        <el-select
          v-model="bindSpecialIdForm.memberId"
          filterable
          remote
          reserve-keyword
          clearable
          class="w-full"
          placeholder="请输入会员名搜索"
          :remote-method="searchBindMemberOptions"
          :loading="bindMemberLoading"
          @visible-change="handleBindMemberVisibleChange"
        >
          <el-option
            v-for="item in bindMemberOptions"
            :key="item.id"
            :label="formatMemberLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="复核说明">
        <el-input
          v-model="bindSpecialIdForm.auditNote"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="请输入平台截图、申诉单或复核依据"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="bindSpecialIdDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="bindSpecialIdLoading" @click="handleConfirmBindSpecialId">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import * as OrderApi from '@/api/cps/order'
import type { CpsOrderPageReqVO, CpsOrderVO } from '@/api/cps/order'
import { CpsApiVendorApi, type CpsApiVendorVO } from '@/api/cps/apiVendor'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import { PlatformOnboardingApi } from '@/api/cps/platformOnboarding'
import { getUserPage, type UserVO } from '@/api/member/user/index'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'

defineOptions({ name: 'CpsOrder' })

type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'

const message = useMessage()
const router = useRouter()

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

type SyncPresetKey = '3h' | 'today' | 'yesterday' | '7d' | '30d' | 'month' | 'custom'
const syncPresets: Array<{ key: SyncPresetKey; label: string }> = [
  { key: '3h', label: '近3小时' },
  { key: 'today', label: '今天' },
  { key: 'yesterday', label: '昨天' },
  { key: '7d', label: '近7天' },
  { key: '30d', label: '近30天' },
  { key: 'month', label: '本月' },
  { key: 'custom', label: '自定义' }
]
const syncDialogVisible = ref(false)
const syncLoading = ref(false)
const syncOptionLoading = ref(false)
const syncVendorLoading = ref(false)
const syncPlatformOptions = ref<CpsPlatformVO[]>([])
const syncVendorOptions = ref<CpsApiVendorVO[]>([])
const syncForm = reactive<{
  platformCode: string
  vendorCode: string
  queryType: number
  orderStatus?: number
  preset: SyncPresetKey
  dateRange: [string, string] | null
  startTime: string
}>({
  platformCode: '',
  vendorCode: '',
  queryType: 4,
  orderStatus: undefined,
  preset: '3h',
  dateRange: null,
  startTime: ''
})

const isDataokeSync = computed(() => syncForm.vendorCode === 'dataoke')

const syncDateFormat = 'YYYY-MM-DD HH:mm:ss'
const presetRange = (key: SyncPresetKey): [string, string] | null => {
  const now = dayjs()
  if (key === 'custom') return syncForm.dateRange
  if (key === '3h')
    return [now.subtract(3, 'hour').format(syncDateFormat), now.format(syncDateFormat)]
  if (key === 'today')
    return [now.startOf('day').format(syncDateFormat), now.format(syncDateFormat)]
  if (key === 'yesterday') {
    return [
      now.subtract(1, 'day').startOf('day').format(syncDateFormat),
      now.subtract(1, 'day').endOf('day').format(syncDateFormat)
    ]
  }
  if (key === 'month')
    return [now.startOf('month').format(syncDateFormat), now.format(syncDateFormat)]
  const days = key === '7d' ? 7 : 30
  return [
    now
      .subtract(days - 1, 'day')
      .startOf('day')
      .format(syncDateFormat),
    now.format(syncDateFormat)
  ]
}

const selectSyncPreset = (key: SyncPresetKey) => {
  syncForm.preset = key
  if (key !== 'custom') syncForm.dateRange = presetRange(key)
}

const onSyncDateRangeChange = (value: [string, string] | null) => {
  syncForm.dateRange = value
  syncForm.preset = 'custom'
}

/** 平台标签类型 */
const platformTagType = (code: string): ElTagType => {
  const map: Record<string, ElTagType> = {
    taobao: 'danger',
    jd: 'primary',
    pdd: 'warning',
    douyin: 'info',
    eleme: 'success'
  }
  return map[code] || 'info'
}
const platformLabel = (code: string) => {
  const map: Record<string, string> = {
    taobao: '淘宝',
    jd: '京东',
    pdd: '拼多多',
    douyin: '抖音',
    eleme: '淘宝闪购'
  }
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

/** 未完成可信会员归因时，预计返利金额尚不能确定。 */
const formatEstimateRebate = (order?: CpsOrderVO | null) => {
  if (!order?.memberId) return '待归因'
  return `¥${formatAmount(order.estimateRebate)}`
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
    transferRecord: '转链记录',
    sid: '闪购SID'
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

const syncVendorLabel = (vendor: CpsApiVendorVO) => {
  const typeLabel = vendor.vendorType === 'official' ? '官方 API' : '聚合 API'
  return `${vendor.vendorName || vendor.vendorCode}（${typeLabel}）`
}

const loadSyncVendors = async () => {
  syncForm.vendorCode = ''
  syncVendorOptions.value = []
  if (!syncForm.platformCode) return
  syncVendorLoading.value = true
  try {
    const [vendors, descriptors] = await Promise.all([
      CpsApiVendorApi.getVendorListByPlatform(syncForm.platformCode),
      PlatformOnboardingApi.getVendorDescriptors(syncForm.platformCode)
    ])
    const orderQueryVendors = new Set(
      (descriptors || [])
        .filter((descriptor) => descriptor.capabilities?.includes('order_query'))
        .map((descriptor) => descriptor.vendorCode)
    )
    syncVendorOptions.value = (vendors || []).filter(
      (vendor) => vendor.status === 1 && orderQueryVendors.has(vendor.vendorCode)
    )
    const platform = syncPlatformOptions.value.find(
      (item) => item.platformCode === syncForm.platformCode
    )
    const preferred = syncVendorOptions.value.find(
      (vendor) => vendor.vendorCode === platform?.activeVendorCode
    )
    syncForm.vendorCode = preferred?.vendorCode || syncVendorOptions.value[0]?.vendorCode || ''
  } finally {
    syncVendorLoading.value = false
  }
}

const openSyncDialog = async () => {
  syncOptionLoading.value = true
  try {
    syncPlatformOptions.value = (await CpsPlatformApi.getEnabledPlatformList()) || []
    const preferredPlatform = syncPlatformOptions.value.find(
      (platform) => platform.platformCode === queryParams.platformCode
    )
    syncForm.platformCode =
      preferredPlatform?.platformCode || syncPlatformOptions.value[0]?.platformCode || ''
    syncForm.queryType = 4
    syncForm.orderStatus = undefined
    syncForm.preset = '3h'
    syncForm.dateRange = presetRange('3h')
    syncForm.startTime = dayjs().subtract(3, 'hour').format(syncDateFormat)
    syncDialogVisible.value = true
    await loadSyncVendors()
  } finally {
    syncOptionLoading.value = false
  }
}

const confirmSync = async () => {
  if (!syncForm.platformCode) {
    message.warning('请选择同步平台')
    return
  }
  if (!syncForm.vendorCode) {
    message.warning('请选择已启用的 API 供应商')
    return
  }
  const [rangeStartTime, rangeEndTime] = syncForm.dateRange || []
  const startTime = isDataokeSync.value ? syncForm.startTime : rangeStartTime
  const endTime = isDataokeSync.value
    ? dayjs(startTime).add(3, 'hour').format(syncDateFormat)
    : rangeEndTime
  if (!startTime || !endTime) {
    message.warning('请选择同步时间范围')
    return
  }
  const start = dayjs(startTime)
  const end = dayjs(endTime)
  if (!start.isValid() || !end.isValid() || !start.isBefore(end)) {
    message.warning('同步开始时间必须早于结束时间')
    return
  }
  const hours = isDataokeSync.value ? 3 : Math.max(1, Math.ceil(end.diff(start, 'minute') / 60))
  try {
    await message.confirm(
      `确认通过 ${syncVendorOptions.value.find((item) => item.vendorCode === syncForm.vendorCode)?.vendorName || syncForm.vendorCode} 同步 ${platformLabel(syncForm.platformCode)}订单？\n${start.format(syncDateFormat)} 至 ${end.format(syncDateFormat)}`
    )
    syncLoading.value = true
    const result = await OrderApi.syncCpsOrders({
      platformCode: syncForm.platformCode,
      vendorCode: syncForm.vendorCode,
      hours,
      queryType: syncForm.queryType,
      orderStatus: syncForm.orderStatus,
      startTime: start.format(syncDateFormat),
      endTime: end.format(syncDateFormat)
    })
    message.success(result || '同步任务已触发')
    syncDialogVisible.value = false
    await getList()
  } catch {
  } finally {
    syncLoading.value = false
  }
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailLoading = ref(false)
const bindSpecialIdLoading = ref(false)
const bindSpecialIdDialogVisible = ref(false)
const bindMemberLoading = ref(false)
const bindMemberOptions = ref<UserVO[]>([])
const bindSpecialIdForm = reactive<{
  orderId?: number
  specialId?: string
  memberId?: number
  auditNote?: string
}>({})
const MEMBER_OPTION_PAGE_SIZE = 100
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

const handleBindSpecialId = async () => {
  if (!detailData.value?.id || !detailData.value.specialId) return
  if (!detailData.value.adzoneId) {
    message.warning('订单缺少推广位ID，不能建立 special_id 绑定关系')
    return
  }
  bindSpecialIdForm.orderId = detailData.value.id
  bindSpecialIdForm.specialId = detailData.value.specialId
  bindSpecialIdForm.memberId = undefined
  bindSpecialIdForm.auditNote = undefined
  bindSpecialIdDialogVisible.value = true
  await searchBindMemberOptions('')
}

const searchBindMemberOptions = async (keyword: string) => {
  bindMemberLoading.value = true
  try {
    const queryText = keyword?.trim()
    const params = {
      mobile: /^\d+$/.test(queryText || '') ? queryText : undefined,
      nickname: queryText && !/^\d+$/.test(queryText) ? queryText : undefined
    }
    const firstPage = await getUserPage({
      pageNo: 1,
      pageSize: MEMBER_OPTION_PAGE_SIZE,
      ...params
    })
    const options = firstPage?.list || []
    const total = firstPage?.total || options.length
    const pageCount = Math.ceil(total / MEMBER_OPTION_PAGE_SIZE)
    for (let pageNo = 2; pageNo <= pageCount; pageNo++) {
      const data = await getUserPage({
        pageNo,
        pageSize: MEMBER_OPTION_PAGE_SIZE,
        ...params
      })
      options.push(...(data?.list || []))
    }
    bindMemberOptions.value = options
  } finally {
    bindMemberLoading.value = false
  }
}

const handleBindMemberVisibleChange = (visible: boolean) => {
  if (visible && bindMemberOptions.value.length === 0) {
    searchBindMemberOptions('')
  }
}

const handleConfirmBindSpecialId = async () => {
  if (!bindSpecialIdForm.orderId || !bindSpecialIdForm.memberId) {
    message.warning('请选择会员')
    return
  }
  try {
    bindSpecialIdLoading.value = true
    await OrderApi.bindSpecialIdToMember({
      orderId: bindSpecialIdForm.orderId,
      memberId: bindSpecialIdForm.memberId,
      idempotencyKey: createManualBindIdempotencyKey(bindSpecialIdForm.orderId),
      auditNote: bindSpecialIdForm.auditNote?.trim() || undefined
    })
    message.success('绑定成功')
    bindSpecialIdDialogVisible.value = false
    detailData.value = await OrderApi.getCpsOrder(bindSpecialIdForm.orderId)
    await getList()
  } catch {
  } finally {
    bindSpecialIdLoading.value = false
  }
}

const formatMemberLabel = (item: UserVO) => {
  const parts = [`ID: ${item.id}`]
  if (item.nickname) parts.push(item.nickname)
  if (item.name && item.name !== item.nickname) parts.push(item.name)
  if (item.mobile) parts.push(item.mobile)
  return parts.join(' / ')
}

const createManualBindIdempotencyKey = (orderId: number) => {
  const randomId =
    typeof crypto !== 'undefined' && 'randomUUID' in crypto
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2)}`
  return `manual-bind:${orderId}:${randomId}`
}

onMounted(() => {
  getList()
})
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

.attribution-field {
  display: flex;
  min-width: 0;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.attribution-field span {
  min-width: 0;
  overflow-wrap: anywhere;
}

.order-query-form {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  column-gap: 16px;
}

.order-query-form :deep(.el-form-item) {
  margin-right: 0;
  margin-bottom: 16px;
}

.query-actions {
  flex: 1 0 100%;
  padding-left: 20px;
}

.query-search-actions {
  flex: 0 0 auto;
}

.sync-dialog-platform {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  margin-bottom: 18px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.sync-dialog-label {
  color: var(--el-text-color-secondary);
}

.sync-dialog-hint {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.sync-range-picker {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.sync-presets {
  display: flex;
  flex-wrap: wrap;
}

.sync-date-picker {
  width: 100%;
}

.sync-field-tip {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

.sync-field-tip--danger {
  color: var(--el-color-danger);
}

.sync-field-tip--warning {
  color: var(--el-color-warning-dark-2);
}

@media (max-width: 768px) {
  .sync-monitor-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .sync-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-query-form {
    display: block;
  }

  .order-query-form :deep(.el-form-item) {
    width: 100%;
  }

  .order-query-form :deep(.el-form-item .el-input),
  .order-query-form :deep(.el-form-item .el-select),
  .order-query-form :deep(.el-form-item .el-date-editor) {
    width: 100% !important;
  }

  .query-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    padding-left: 0;
  }

  .query-search-actions {
    display: flex;
    gap: 8px;
  }

  .order-detail-dialog {
    width: calc(100vw - 24px) !important;
  }

  .order-detail-descriptions :deep(.el-descriptions__label) {
    width: 96px;
    min-width: 96px;
  }
}
</style>
