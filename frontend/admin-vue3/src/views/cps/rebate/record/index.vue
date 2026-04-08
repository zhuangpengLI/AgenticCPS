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
      <el-form-item label="会员ID" prop="memberId">
        <el-input
          v-model.number="queryParams.memberId"
          placeholder="请输入会员ID"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="平台" prop="platformCode">
        <el-select
          v-model="queryParams.platformCode"
          placeholder="请选择平台"
          clearable
          class="!w-140px"
        >
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="返利类型" prop="rebateType">
        <el-select
          v-model="queryParams.rebateType"
          placeholder="请选择类型"
          clearable
          class="!w-140px"
        >
          <el-option label="正常返利" value="rebate" />
          <el-option label="退款扣回" value="reverse" />
        </el-select>
      </el-form-item>
      <el-form-item label="返利状态" prop="rebateStatus">
        <el-select
          v-model="queryParams.rebateStatus"
          placeholder="请选择状态"
          clearable
          class="!w-140px"
        >
          <el-option label="已到账" value="received" />
          <el-option label="已扣回" value="refunded" />
        </el-select>
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
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="会员ID" align="center" prop="memberId" width="80" />
      <el-table-column label="平台" align="center" prop="platformCode" width="80">
        <template #default="scope">
          <el-tag :type="platformTagType(scope.row.platformCode)" size="small">
            {{ platformLabel(scope.row.platformCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="平台单号" align="left" prop="platformOrderId" min-width="160" show-overflow-tooltip />
      <el-table-column label="商品标题" align="left" prop="itemTitle" min-width="180" show-overflow-tooltip />
      <el-table-column label="订单金额" align="center" width="100">
        <template #default="scope">
          ¥{{ formatAmount(scope.row.orderAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="返利比例" align="center" width="90">
        <template #default="scope">
          {{ scope.row.rebateRate }}%
        </template>
      </el-table-column>
      <el-table-column label="返利金额" align="center" width="100">
        <template #default="scope">
          <span class="text-green-600 font-medium">¥{{ formatAmount(scope.row.rebateAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="rebateType" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.rebateType === 'rebate' ? 'success' : 'danger'" size="small">
            {{ scope.row.rebateType === 'rebate' ? '正常返利' : '退款扣回' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="rebateStatus" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.rebateStatus === 'received' ? 'success' : 'info'" size="small">
            {{ scope.row.rebateStatus === 'received' ? '已到账' : '已扣回' }}
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
      <el-table-column label="操作" align="center" fixed="right" width="130">
        <template #default="scope">
          <el-button
            type="primary"
            link
            @click="openDetail(scope.row)"
            v-hasPermi="['cps:rebate-record:query']"
          >
            详情
          </el-button>
          <el-button
            type="danger"
            link
            v-if="scope.row.rebateType === 'rebate' && scope.row.rebateStatus === 'received'"
            @click="handleReverse(scope.row)"
            v-hasPermi="['cps:rebate-record:reverse']"
          >
            扣回
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
    title="返利记录详情"
    width="620px"
    destroy-on-close
  >
    <el-descriptions :column="2" border v-if="detailData">
      <el-descriptions-item label="记录ID">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="会员ID">{{ detailData.memberId }}</el-descriptions-item>
      <el-descriptions-item label="订单ID">{{ detailData.orderId }}</el-descriptions-item>
      <el-descriptions-item label="平台">{{ platformLabel(detailData.platformCode) }}</el-descriptions-item>
      <el-descriptions-item label="平台单号" :span="2">{{ detailData.platformOrderId }}</el-descriptions-item>
      <el-descriptions-item label="商品标题" :span="2">{{ detailData.itemTitle || '-' }}</el-descriptions-item>
      <el-descriptions-item label="订单金额">¥{{ formatAmount(detailData.orderAmount) }}</el-descriptions-item>
      <el-descriptions-item label="可分配佣金">¥{{ formatAmount(detailData.commissionAmount) }}</el-descriptions-item>
      <el-descriptions-item label="返利比例">{{ detailData.rebateRate }}%</el-descriptions-item>
      <el-descriptions-item label="返利金额">
        <span class="text-green-600 font-medium">¥{{ formatAmount(detailData.rebateAmount) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="返利类型">
        <el-tag :type="detailData.rebateType === 'rebate' ? 'success' : 'danger'" size="small">
          {{ detailData.rebateType === 'rebate' ? '正常返利' : '退款扣回' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="返利状态">
        <el-tag :type="detailData.rebateStatus === 'received' ? 'success' : 'info'" size="small">
          {{ detailData.rebateStatus === 'received' ? '已到账' : '已扣回' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item v-if="detailData.precedingRebateId" label="关联原始记录">
        {{ detailData.precedingRebateId }}
      </el-descriptions-item>
      <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      <el-descriptions-item label="创建时间">{{ formatDate(detailData.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="更新时间">{{ formatDate(detailData.updateTime) }}</el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { dateFormatter } from '@/utils/formatTime'
import * as RebateApi from '@/api/cps/rebate'
import type { CpsRebateRecordVO, CpsRebateRecordPageReqVO } from '@/api/cps/rebate'

defineOptions({ name: 'CpsRebateRecord' })

const message = useMessage()

const loading = ref(false)
const total = ref(0)
const list = ref<CpsRebateRecordVO[]>([])
const queryFormRef = ref()

const queryParams = reactive<CpsRebateRecordPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  memberId: undefined,
  platformCode: undefined,
  rebateType: undefined,
  rebateStatus: undefined,
  createTime: undefined
})

const platformTagType = (code: string) => {
  const map: Record<string, string> = { taobao: 'danger', jd: 'primary', pdd: 'warning', douyin: '' }
  return map[code] || 'info'
}
const platformLabel = (code: string) => {
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return map[code] || code
}
const formatAmount = (val?: number) => {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}
const formatDate = (val?: Date | string) => {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN')
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await RebateApi.getCpsRebateRecordPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}
const resetQuery = () => {
  queryFormRef.value.resetFields()
  handleQuery()
}

/** 退款扣回 */
const handleReverse = async (row: CpsRebateRecordVO) => {
  try {
    await message.confirm(`确认对订单 ${row.orderId} 执行退款扣回返利操作？`)
    await RebateApi.reverseCpsRebate(row.orderId)
    message.success('扣回操作成功')
    await getList()
  } catch {}
}

/** 详情弹窗 */
const detailVisible = ref(false)
const detailData = ref<CpsRebateRecordVO | null>(null)
const openDetail = (row: CpsRebateRecordVO) => {
  detailData.value = row
  detailVisible.value = true
}

onMounted(getList)
</script>
