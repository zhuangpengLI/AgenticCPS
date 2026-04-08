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
          class="!w-140px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="提现类型" prop="withdrawType">
        <el-select
          v-model="queryParams.withdrawType"
          placeholder="请选择类型"
          clearable
          class="!w-140px"
        >
          <el-option label="支付宝" value="alipay" />
          <el-option label="微信" value="wechat" />
          <el-option label="银行卡" value="bank" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择状态"
          clearable
          class="!w-140px"
        >
          <el-option label="待审核" value="created" />
          <el-option label="审核通过" value="approved" />
          <el-option label="已驳回" value="rejected" />
          <el-option label="已到账" value="transferred" />
          <el-option label="打款失败" value="failed" />
        </el-select>
      </el-form-item>
      <el-form-item label="申请时间" prop="createTime">
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
      <el-table-column label="提现单号" align="center" prop="withdrawNo" min-width="180" show-overflow-tooltip />
      <el-table-column label="会员ID" align="center" prop="memberId" width="90" />
      <el-table-column label="提现方式" align="center" prop="withdrawType" width="90">
        <template #default="scope">
          <el-tag :type="withdrawTypeTagType(scope.row.withdrawType)" size="small">
            {{ withdrawTypeLabel(scope.row.withdrawType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提现账户" align="center" prop="withdrawAccount" width="160" show-overflow-tooltip />
      <el-table-column label="提现金额" align="center" width="110">
        <template #default="scope">
          <span class="font-medium text-orange-500">¥{{ formatAmount(scope.row.amount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="手续费" align="center" width="90">
        <template #default="scope">
          ¥{{ formatAmount(scope.row.feeAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="实际到账" align="center" width="110">
        <template #default="scope">
          <span class="text-green-600">¥{{ formatAmount(scope.row.actualAmount) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template #default="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="审核时间" align="center" prop="auditTime" width="160">
        <template #default="scope">{{ scope.row.auditTime ? formatDate(scope.row.auditTime) : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="180">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openDetail(scope.row)"
            v-hasPermi="['cps:withdraw:query']"
          >
            详情
          </el-button>
          <el-button
            v-if="scope.row.status === 'created'"
            link
            type="success"
            @click="openAudit(scope.row, 'approve')"
            v-hasPermi="['cps:withdraw:update']"
          >
            通过
          </el-button>
          <el-button
            v-if="scope.row.status === 'created'"
            link
            type="danger"
            @click="openAudit(scope.row, 'reject')"
            v-hasPermi="['cps:withdraw:update']"
          >
            驳回
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
    title="提现详情"
    width="680px"
    destroy-on-close
  >
    <el-descriptions :column="2" border v-if="detailData">
      <el-descriptions-item label="提现ID">{{ detailData.id }}</el-descriptions-item>
      <el-descriptions-item label="提现单号" :span="2">{{ detailData.withdrawNo }}</el-descriptions-item>
      <el-descriptions-item label="会员ID">{{ detailData.memberId }}</el-descriptions-item>
      <el-descriptions-item label="提现方式">
        <el-tag :type="withdrawTypeTagType(detailData.withdrawType)" size="small">
          {{ withdrawTypeLabel(detailData.withdrawType) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="提现账户">{{ detailData.withdrawAccount }}</el-descriptions-item>
      <el-descriptions-item label="账户姓名">{{ detailData.withdrawAccountName || '-' }}</el-descriptions-item>
      <el-descriptions-item label="提现金额">
        <span class="text-orange-500 font-medium">¥{{ formatAmount(detailData.amount) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="手续费">¥{{ formatAmount(detailData.feeAmount) }}</el-descriptions-item>
      <el-descriptions-item label="实际到账">
        <span class="text-green-600 font-medium">¥{{ formatAmount(detailData.actualAmount) }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="状态">
        <el-tag :type="statusTagType(detailData.status)" size="small">
          {{ statusLabel(detailData.status) }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="申请时间">{{ formatDate(detailData.createTime) }}</el-descriptions-item>
      <el-descriptions-item label="审核时间">{{ detailData.auditTime ? formatDate(detailData.auditTime) : '-' }}</el-descriptions-item>
      <el-descriptions-item label="审核备注" :span="2">{{ detailData.reviewNote || '-' }}</el-descriptions-item>
      <el-descriptions-item label="转账单号" :span="2">{{ detailData.transactionNo || '-' }}</el-descriptions-item>
      <el-descriptions-item label="转账状态">{{ detailData.transferStatus || '-' }}</el-descriptions-item>
      <el-descriptions-item label="转账时间">{{ detailData.transferTime ? formatDate(detailData.transferTime) : '-' }}</el-descriptions-item>
      <el-descriptions-item v-if="detailData.transferError" label="转账错误" :span="2">
        <span class="text-red-500">{{ detailData.transferError }}</span>
      </el-descriptions-item>
    </el-descriptions>
    <template #footer>
      <el-button @click="detailVisible = false">关 闭</el-button>
    </template>
  </el-dialog>

  <!-- 审核弹窗 -->
  <el-dialog
    v-model="auditVisible"
    :title="auditType === 'approve' ? '审核通过确认' : '驳回提现'"
    width="440px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <div class="mb-16px" v-if="auditTarget">
      <el-alert
        :type="auditType === 'approve' ? 'success' : 'warning'"
        :title="auditType === 'approve' ? `确认审核通过会员 ${auditTarget.memberId} 的提现申请 ¥${formatAmount(auditTarget.amount)} 吗？` : `确认驳回会员 ${auditTarget.memberId} 的提现申请 ¥${formatAmount(auditTarget.amount)} 吗？`"
        show-icon
        :closable="false"
      />
    </div>
    <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
      <el-form-item label="审核备注" prop="reviewNote">
        <el-input
          v-model="auditForm.reviewNote"
          type="textarea"
          :rows="3"
          :placeholder="auditType === 'approve' ? '审核备注（可选）' : '请输入驳回原因（必填）'"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="auditVisible = false">取 消</el-button>
      <el-button
        :type="auditType === 'approve' ? 'success' : 'danger'"
        :loading="auditLoading"
        @click="handleAuditSubmit"
      >
        {{ auditType === 'approve' ? '确认通过' : '确认驳回' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  CpsWithdrawApi,
  type CpsWithdrawVO,
  type CpsWithdrawPageReqVO
} from '@/api/cps/withdraw'
import { formatDate } from '@/utils/formatTime'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'CpsWithdraw' })

const loading = ref(false)
const list = ref<CpsWithdrawVO[]>([])
const total = ref(0)

const queryFormRef = ref()

const queryParams = reactive<CpsWithdrawPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  memberId: undefined,
  status: undefined,
  withdrawType: undefined,
  createTime: undefined
})

/** 金额格式化（后端字段为分，此处根据实际业务而定）*/
const formatAmount = (val?: number) => {
  if (val == null) return '0.00'
  return Number(val).toFixed(2)
}

/** 提现方式标签 */
const withdrawTypeTagType = (type: string) => {
  const map: Record<string, string> = { alipay: 'primary', wechat: 'success', bank: 'warning' }
  return map[type] || 'info'
}
const withdrawTypeLabel = (type: string) => {
  const map: Record<string, string> = { alipay: '支付宝', wechat: '微信', bank: '银行卡' }
  return map[type] || type
}

/** 状态标签 */
const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    created: 'warning',
    approved: 'primary',
    rejected: 'danger',
    transferred: 'success',
    failed: 'danger'
  }
  return map[status] || 'info'
}
const statusLabel = (status: string) => {
  const map: Record<string, string> = {
    created: '待审核',
    approved: '审核通过',
    rejected: '已驳回',
    transferred: '已到账',
    failed: '打款失败'
  }
  return map[status] || status
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CpsWithdrawApi.getWithdrawPage(queryParams)
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
  queryFormRef.value?.resetFields()
  handleQuery()
}

// ===== 详情弹窗 =====
const detailVisible = ref(false)
const detailData = ref<CpsWithdrawVO | null>(null)

const openDetail = (row: CpsWithdrawVO) => {
  detailData.value = row
  detailVisible.value = true
}

// ===== 审核弹窗 =====
const auditVisible = ref(false)
const auditLoading = ref(false)
const auditType = ref<'approve' | 'reject'>('approve')
const auditTarget = ref<CpsWithdrawVO | null>(null)
const auditFormRef = ref()
const auditForm = reactive({ reviewNote: '' })

const auditRules = computed(() => ({
  reviewNote: auditType.value === 'reject'
    ? [{ required: true, message: '驳回原因不能为空', trigger: 'blur' }]
    : []
}))

const openAudit = (row: CpsWithdrawVO, type: 'approve' | 'reject') => {
  auditTarget.value = row
  auditType.value = type
  auditForm.reviewNote = ''
  auditVisible.value = true
}

const handleAuditSubmit = async () => {
  await auditFormRef.value?.validate()
  if (!auditTarget.value) return
  auditLoading.value = true
  try {
    if (auditType.value === 'approve') {
      await CpsWithdrawApi.approveWithdraw(auditTarget.value.id, auditForm.reviewNote || undefined)
      ElMessage.success('已审核通过')
    } else {
      await CpsWithdrawApi.rejectWithdraw(auditTarget.value.id, auditForm.reviewNote)
      ElMessage.success('已驳回提现申请')
    }
    auditVisible.value = false
    getList()
  } finally {
    auditLoading.value = false
  }
}

onMounted(getList)
</script>
