<template>
  <ContentWrap v-hasPermi="['cps:order:attribution-bind']">
    <div class="page-header">
      <div>
        <div class="page-title">审核订单</div>
        <div class="page-subtitle">审核会员提交的订单归属申领</div>
      </div>
      <el-button @click="router.push('/cps/order')"
        ><Icon icon="ep:arrow-left" class="mr-5px" />返回订单管理</el-button
      >
    </div>
    <div class="toolbar">
      <el-select v-model="claimQuery.reviewStatus" class="status-select" @change="getClaimList"
        ><el-option label="待审核" value="PENDING_REVIEW" /><el-option
          label="已通过"
          value="APPROVED" /><el-option label="已拒绝" value="REJECTED" /><el-option
          label="冲突"
          value="CONFLICT"
      /></el-select>
      <el-input
        v-model="claimQuery.platformOrderId"
        clearable
        class="order-search"
        placeholder="平台订单号"
        @keyup.enter="getClaimList"
      />
      <el-button type="primary" @click="getClaimList"
        ><Icon icon="ep:search" class="mr-1" />查询</el-button
      >
    </div>
    <el-table v-loading="claimLoading" :data="claimList">
      <el-table-column label="申领ID" prop="id" width="90" />
      <el-table-column label="平台" width="100"
        ><template #default="scope">{{
          platformLabel(scope.row.platformCode)
        }}</template></el-table-column
      >
      <el-table-column
        label="平台订单号"
        prop="platformOrderId"
        min-width="190"
        show-overflow-tooltip
      />
      <el-table-column label="申领会员" prop="candidateMemberId" width="110" />
      <el-table-column label="状态" width="110"
        ><template #default="scope"
          ><el-tag :type="claimStatusTagType(scope.row.reviewStatus)" size="small">{{
            claimStatusLabel(scope.row.reviewStatus)
          }}</el-tag></template
        ></el-table-column
      >
      <el-table-column label="申领原因" prop="rejectReason" min-width="220" show-overflow-tooltip />
      <el-table-column label="申领时间" prop="createTime" width="165" :formatter="dateFormatter" />
      <el-table-column label="操作" fixed="right" width="150"
        ><template #default="scope"
          ><template v-if="scope.row.reviewStatus === 'PENDING_REVIEW'"
            ><el-button type="success" link @click="openClaimReview(scope.row, true)"
              >通过</el-button
            ><el-button type="danger" link @click="openClaimReview(scope.row, false)"
              >拒绝</el-button
            ></template
          ><span v-else>-</span></template
        ></el-table-column
      >
    </el-table>
    <Pagination
      :total="claimTotal"
      v-model:page="claimQuery.pageNo"
      v-model:limit="claimQuery.pageSize"
      @pagination="getClaimList"
    />
  </ContentWrap>

  <el-dialog
    v-model="claimReviewVisible"
    :title="claimReviewForm.approved ? '通过订单申领' : '拒绝订单申领'"
    width="460px"
    destroy-on-close
  >
    <el-form label-width="88px">
      <el-form-item label="平台单号"
        ><el-input :model-value="claimReviewForm.platformOrderId" disabled
      /></el-form-item>
      <el-form-item label="申领会员"
        ><el-input :model-value="claimReviewForm.candidateMemberId" disabled
      /></el-form-item>
      <el-form-item label="审核说明" required
        ><el-input
          v-model="claimReviewForm.auditNote"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="请输入联盟后台核验依据"
      /></el-form-item>
    </el-form>
    <template #footer
      ><el-button @click="claimReviewVisible = false">取消</el-button
      ><el-button
        :type="claimReviewForm.approved ? 'success' : 'danger'"
        :loading="claimReviewLoading"
        @click="submitClaimReview"
        >确认{{ claimReviewForm.approved ? '通过' : '拒绝' }}</el-button
      ></template
    >
  </el-dialog>
</template>

<script setup lang="ts">
import * as OrderApi from '@/api/cps/order'
import type { CpsOrderClaimPageReqVO, CpsOrderClaimVO } from '@/api/cps/order'
import { dateFormatter } from '@/utils/formatTime'
import { useRouter } from 'vue-router'

defineOptions({ name: 'CpsOrderReview' })
type ElTagType = 'primary' | 'success' | 'warning' | 'danger' | 'info'
const router = useRouter()
const message = useMessage()
const claimLoading = ref(false)
const claimTotal = ref(0)
const claimList = ref<CpsOrderClaimVO[]>([])
const claimReviewVisible = ref(false)
const claimReviewLoading = ref(false)
const claimQuery = reactive<CpsOrderClaimPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  reviewStatus: 'PENDING_REVIEW',
  platformOrderId: undefined
})
const claimReviewForm = reactive<{
  claimId?: number
  platformOrderId?: string
  candidateMemberId?: number
  approved: boolean
  auditNote: string
}>({ approved: true, auditNote: '' })
const platformLabel = (code: string) =>
  ({ taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音', eleme: '淘宝闪购' })[code] || code
const claimStatusLabel = (status?: string) =>
  ({
    PENDING_REVIEW: '待审核',
    APPROVED: '已通过',
    REJECTED: '已拒绝',
    CONFLICT: '冲突',
    PENDING_SYNC: '等待同步',
    ASSET_LOCKED: '资金锁定'
  })[status || ''] ||
  status ||
  '-'
const claimStatusTagType = (status?: string): ElTagType =>
  (({
    PENDING_REVIEW: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    CONFLICT: 'danger',
    PENDING_SYNC: 'info',
    ASSET_LOCKED: 'danger'
  })[status || ''] as ElTagType) || 'info'
const getClaimList = async () => {
  claimLoading.value = true
  try {
    const data = await OrderApi.getOrderClaimPage(claimQuery)
    claimList.value = data.list
    claimTotal.value = data.total
  } finally {
    claimLoading.value = false
  }
}
const openClaimReview = (claim: CpsOrderClaimVO, approved: boolean) => {
  claimReviewForm.claimId = claim.id
  claimReviewForm.platformOrderId = claim.platformOrderId
  claimReviewForm.candidateMemberId = claim.candidateMemberId
  claimReviewForm.approved = approved
  claimReviewForm.auditNote = ''
  claimReviewVisible.value = true
}
const submitClaimReview = async () => {
  if (!claimReviewForm.claimId || !claimReviewForm.auditNote.trim())
    return message.warning('请输入审核说明')
  claimReviewLoading.value = true
  try {
    const result = await OrderApi.reviewOrderClaim({
      claimId: claimReviewForm.claimId,
      approved: claimReviewForm.approved,
      auditNote: claimReviewForm.auditNote.trim()
    })
    message.success(result.message || '审核完成')
    claimReviewVisible.value = false
    await getClaimList()
  } finally {
    claimReviewLoading.value = false
  }
}
onMounted(getClaimList)
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
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}
.status-select {
  width: 140px;
}
.order-search {
  width: min(320px, 100%);
}
@media (max-width: 768px) {
  .page-header {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
