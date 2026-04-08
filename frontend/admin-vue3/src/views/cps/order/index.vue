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
      <el-form-item label="会员ID" prop="memberId">
        <el-input
          v-model.number="queryParams.memberId"
          placeholder="请输入会员ID"
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
    <el-table v-loading="loading" :data="list" stripe>
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
      <el-table-column label="会员" align="center" prop="memberNickname" width="100" show-overflow-tooltip />
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
      <el-table-column label="操作" align="center" fixed="right" width="80">
        <template #default="scope">
          <el-button
            type="primary"
            link
            @click="openDetail(scope.row)"
            v-hasPermi="['cps:order:query']"
          >
            详情
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
    width="700px"
    destroy-on-close
  >
    <el-descriptions :column="2" border v-if="detailData">
      <el-descriptions-item label="订单ID">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="平台">{{ platformLabel(detailData.platformCode) }}</el-descriptions-item>
      <el-descriptions-item label="平台单号" :span="2">{{ detailData.platformOrderId }}</el-descriptions-item>
      <el-descriptions-item label="父订单号" :span="2">{{ detailData.parentOrderId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="会员ID">{{ detailData.memberId }}</el-descriptions-item>
      <el-descriptions-item label="会员昵称">{{ detailData.memberNickname || '-' }}</el-descriptions-item>
      <el-descriptions-item label="商品标题" :span="2">{{ detailData.itemTitle || '-' }}</el-descriptions-item>
      <el-descriptions-item label="商品原价">¥{{ formatAmount(detailData.itemPrice) }}</el-descriptions-item>
      <el-descriptions-item label="券后价">¥{{ formatAmount(detailData.finalPrice) }}</el-descriptions-item>
      <el-descriptions-item label="优惠券金额">¥{{ formatAmount(detailData.couponAmount) }}</el-descriptions-item>
      <el-descriptions-item label="佣金比例">{{ detailData.commissionRate }}%</el-descriptions-item>
      <el-descriptions-item label="预估佣金">¥{{ formatAmount(detailData.commissionAmount) }}</el-descriptions-item>
      <el-descriptions-item label="预估返利">¥{{ formatAmount(detailData.estimateRebate) }}</el-descriptions-item>
      <el-descriptions-item label="实际返利">¥{{ formatAmount(detailData.realRebate) }}</el-descriptions-item>
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

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<CpsOrderVO[]>([])
const queryFormRef = ref()

const queryParams = reactive<CpsOrderPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  memberId: undefined,
  orderStatus: undefined,
  itemTitle: undefined,
  platformOrderId: undefined,
  createTime: undefined
})

/** 平台标签类型 */
const platformTagType = (code: string) => {
  const map: Record<string, string> = { taobao: 'danger', jd: 'primary', pdd: 'warning', douyin: '' }
  return map[code] || 'info'
}
const platformLabel = (code: string) => {
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return map[code] || code
}

/** 订单状态 */
const orderStatusTagType = (status: string) => {
  const map: Record<string, string> = {
    ordered: 'info',
    paid: 'primary',
    received: 'warning',
    settled: 'success',
    credited: 'success',
    refunded: 'danger',
    invalid: 'info'
  }
  return map[status] || 'info'
}
const orderStatusLabel = (status: string) => {
  const map: Record<string, string> = {
    ordered: '已下单',
    paid: '已付款',
    received: '已收货',
    settled: '已结算',
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

/** 手动同步 */
const handleSync = async (platformCode: string) => {
  try {
    await message.confirm(`确认同步 ${platformLabel(platformCode)} 最近2小时订单？`)
    const result = await OrderApi.syncCpsOrders(platformCode, 2)
    message.success(result || '同步任务已触发')
    await getList()
  } catch {}
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailData = ref<CpsOrderVO | null>(null)
const openDetail = (row: CpsOrderVO) => {
  detailData.value = row
  detailVisible.value = true
}

onMounted(getList)
</script>
