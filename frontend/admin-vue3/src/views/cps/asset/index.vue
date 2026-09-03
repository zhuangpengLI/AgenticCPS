<template>
  <el-alert
    title="资金变化仅允许通过统一资产服务执行；资产流水、归因日志和同步水位均为只读审计数据。"
    type="warning"
    show-icon
    :closable="false"
    class="mb-12px"
  />
  <el-tabs v-model="activeTab" type="border-card" @tab-change="loadActiveTab">
    <el-tab-pane label="会员欠款" name="debt">
      <ContentWrap>
        <el-form :model="debtQuery" inline class="-mb-15px">
          <el-form-item label="会员ID">
            <el-select
              v-model="debtQuery.memberId"
              filterable
              remote
              reserve-keyword
              clearable
              class="!w-220px"
              placeholder="昵称或手机号"
              :remote-method="searchMemberOptions"
              :loading="memberLoading"
              @visible-change="handleMemberDropdownVisible"
            >
              <el-option
                v-for="item in memberOptions"
                :key="item.id"
                :label="formatMemberLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="debtQuery.status" clearable class="!w-140px">
              <el-option label="待偿还" value="OPEN" />
              <el-option label="部分偿还" value="PARTIAL" />
              <el-option label="已偿还" value="PAID" />
              <el-option label="已减免" value="WAIVED" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="loadDebt"><Icon icon="ep:search" class="mr-5px" />查询</el-button>
            <el-button type="primary" v-hasPermi="['cps:rebate-debt:adjust']" @click="openAdjust">人工调整</el-button>
          </el-form-item>
        </el-form>
      </ContentWrap>
      <ContentWrap>
        <el-table v-loading="debtLoading" :data="debtList">
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="会员ID" prop="memberId" width="100" />
          <el-table-column label="来源订单" prop="platformOrderId" min-width="150" show-overflow-tooltip />
          <el-table-column label="原始欠款" width="110"><template #default="s">{{ money(s.row.originalDebtCent) }}</template></el-table-column>
          <el-table-column label="已偿还" width="100"><template #default="s">{{ money(s.row.repaidDebtCent) }}</template></el-table-column>
          <el-table-column label="已减免" width="100"><template #default="s">{{ money(s.row.waivedDebtCent) }}</template></el-table-column>
          <el-table-column label="未偿还" width="110"><template #default="s"><b class="text-red-600">{{ money(s.row.outstandingDebtCent) }}</b></template></el-table-column>
          <el-table-column label="状态" prop="status" width="100" />
          <el-table-column label="下次提醒" prop="nextReminderTime" width="170"><template #default="s">{{ date(s.row.nextReminderTime) }}</template></el-table-column>
          <el-table-column label="创建时间" prop="createTime" width="170"><template #default="s">{{ date(s.row.createTime) }}</template></el-table-column>
        </el-table>
        <Pagination :total="debtTotal" v-model:page="debtQuery.pageNo" v-model:limit="debtQuery.pageSize" @pagination="loadDebt" />
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="资产流水" name="ledger">
      <ContentWrap>
        <el-form :model="ledgerQuery" inline class="-mb-15px">
          <el-form-item label="会员ID">
            <el-select
              v-model="ledgerQuery.memberId"
              filterable
              remote
              reserve-keyword
              clearable
              class="!w-220px"
              placeholder="昵称或手机号"
              :remote-method="searchMemberOptions"
              :loading="memberLoading"
              @visible-change="handleMemberDropdownVisible"
            >
              <el-option
                v-for="item in memberOptions"
                :key="item.id"
                :label="formatMemberLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="业务类型"><el-input v-model="ledgerQuery.businessType" clearable class="!w-180px" /></el-form-item>
          <el-form-item><el-button @click="loadLedger"><Icon icon="ep:search" class="mr-5px" />查询</el-button></el-form-item>
        </el-form>
      </ContentWrap>
      <ContentWrap>
        <el-table v-loading="ledgerLoading" :data="ledgerList">
          <el-table-column label="ID" prop="id" width="80" />
          <el-table-column label="会员ID" prop="memberId" width="100" />
          <el-table-column label="业务类型" prop="businessType" width="190" />
          <el-table-column label="业务单号" prop="businessId" min-width="140" show-overflow-tooltip />
          <el-table-column label="可用变更" width="100"><template #default="s">{{ signedMoney(s.row.availableChangeCent) }}</template></el-table-column>
          <el-table-column label="冻结变更" width="100"><template #default="s">{{ signedMoney(s.row.frozenChangeCent) }}</template></el-table-column>
          <el-table-column label="欠款变更" width="100"><template #default="s">{{ signedMoney(s.row.debtChangeCent) }}</template></el-table-column>
          <el-table-column label="幂等键" prop="idempotencyKey" min-width="180" show-overflow-tooltip />
          <el-table-column label="主体" width="120"><template #default="s">{{ s.row.operatorType }} {{ s.row.operatorId || '' }}</template></el-table-column>
          <el-table-column label="原因" prop="reason" min-width="180" show-overflow-tooltip />
          <el-table-column label="时间" width="170"><template #default="s">{{ date(s.row.createTime) }}</template></el-table-column>
        </el-table>
        <Pagination :total="ledgerTotal" v-model:page="ledgerQuery.pageNo" v-model:limit="ledgerQuery.pageSize" @pagination="loadLedger" />
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="归因日志" name="attribution">
      <ContentWrap>
        <el-form :model="attributionQuery" inline class="-mb-15px">
          <el-form-item label="平台订单号"><el-input v-model="attributionQuery.platformOrderId" clearable /></el-form-item>
          <el-form-item label="结果"><el-select v-model="attributionQuery.result" clearable class="!w-130px"><el-option v-for="v in ['BOUND','CONFLICT','REJECTED','UNATTRIBUTED']" :key="v" :label="v" :value="v" /></el-select></el-form-item>
          <el-form-item><el-button @click="loadAttribution"><Icon icon="ep:search" class="mr-5px" />查询</el-button></el-form-item>
        </el-form>
      </ContentWrap>
      <ContentWrap>
        <el-table v-loading="attributionLoading" :data="attributionList">
          <el-table-column label="订单ID" prop="orderId" width="100" />
          <el-table-column label="平台" prop="platformCode" width="100" />
          <el-table-column label="平台订单号" prop="platformOrderId" min-width="160" />
          <el-table-column label="候选会员" prop="candidateMemberId" width="100" />
          <el-table-column label="归因会员" prop="attributedMemberId" width="100" />
          <el-table-column label="来源" prop="attributionSource" width="130" />
          <el-table-column label="动作" prop="action" width="100" />
          <el-table-column label="结果" prop="result" width="120" />
          <el-table-column label="拒绝/冲突原因" prop="rejectReason" min-width="220" show-overflow-tooltip />
          <el-table-column label="时间" width="170"><template #default="s">{{ date(s.row.createTime) }}</template></el-table-column>
        </el-table>
        <Pagination :total="attributionTotal" v-model:page="attributionQuery.pageNo" v-model:limit="attributionQuery.pageSize" @pagination="loadAttribution" />
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="同步水位" name="checkpoint">
      <ContentWrap>
        <el-form :model="checkpointQuery" inline class="-mb-15px">
          <el-form-item label="平台"><el-input v-model="checkpointQuery.platformCode" clearable class="!w-130px" /></el-form-item>
          <el-form-item label="状态"><el-select v-model="checkpointQuery.lastSyncStatus" clearable class="!w-130px"><el-option v-for="v in ['SUCCESS','PARTIAL','FAILED']" :key="v" :label="v" :value="v" /></el-select></el-form-item>
          <el-form-item><el-button @click="loadCheckpoint"><Icon icon="ep:search" class="mr-5px" />查询</el-button></el-form-item>
        </el-form>
      </ContentWrap>
      <ContentWrap>
        <el-table v-loading="checkpointLoading" :data="checkpointList">
          <el-table-column label="平台" prop="platformCode" width="100" />
          <el-table-column label="供应商" prop="vendorCode" width="120" />
          <el-table-column label="场景" prop="orderScene" width="70" />
          <el-table-column label="查询类型" prop="queryType" width="90" />
          <el-table-column label="分页模式" prop="paginationMode" width="100" />
          <el-table-column label="下一页/游标" min-width="160"><template #default="s">{{ s.row.nextPageNo || s.row.nextCursor || '-' }}</template></el-table-column>
          <el-table-column label="状态" prop="lastSyncStatus" width="100" />
          <el-table-column label="成功/失败" width="100"><template #default="s">{{ s.row.lastSuccessCount }}/{{ s.row.lastFailureCount }}</template></el-table-column>
          <el-table-column label="失败摘要" prop="failureSummary" min-width="220" show-overflow-tooltip />
          <el-table-column label="更新时间" width="170"><template #default="s">{{ date(s.row.updateTime) }}</template></el-table-column>
        </el-table>
        <Pagination :total="checkpointTotal" v-model:page="checkpointQuery.pageNo" v-model:limit="checkpointQuery.pageSize" @pagination="loadCheckpoint" />
      </ContentWrap>
    </el-tab-pane>

    <el-tab-pane label="租户资产策略" name="policy">
      <ContentWrap>
        <el-alert
          v-if="!policy.v2Enabled"
          title="当前租户尚未启用返利资产，结算返利不会写入统一资产账户；完成下方迁移流程后才能启用。启用后，冻结返利会先进入冻结余额，解冻后再转为可用余额。"
          type="info"
          :closable="false"
          class="mb-12px"
        />
        <el-form v-loading="policyLoading" :model="policy" label-width="190px" class="max-w-720px">
          <el-form-item label="发布 B 迁移核验">
            <el-tag :type="policy.migrationReady ? 'success' : 'danger'">{{ policy.migrationReady ? '已核验' : '未核验' }}</el-tag>
            <span class="ml-12px text-gray-500">预检 READY 后，请确认发布 B 变更，再保存策略启用返利资产</span>
          </el-form-item>
          <el-form-item label="启用返利资产"><el-switch v-model="policy.v2Enabled" :disabled="policy.v2Enabled || !policy.migrationReady" /></el-form-item>
          <el-form-item label="资产只读熔断"><el-switch v-model="policy.readOnly" /></el-form-item>
          <el-form-item label="大额欠款阈值（分）"><el-input-number v-model="policy.largeDebtThresholdCent" :min="1" /></el-form-item>
          <el-form-item label="站内提醒间隔（天）"><el-input-number v-model="policy.reminderIntervalDays" :min="1" /></el-form-item>
          <el-form-item label="普通提醒持续（天）"><el-input-number v-model="policy.normalReminderDays" :min="1" /></el-form-item>
          <el-form-item label="大额提醒持续（天）"><el-input-number v-model="policy.largeReminderDays" :min="1" /></el-form-item>
          <el-form-item label="短信最小间隔（天）"><el-input-number v-model="policy.smsIntervalDays" :min="1" /></el-form-item>
          <el-form-item>
            <el-button type="primary" v-hasPermi="['cps:rebate-asset-policy:update']" :loading="policyLoading" @click="bootstrapPolicy">一键准备返利资产</el-button>
            <el-button v-if="migrationReport?.ready && !policy.migrationReady" v-hasPermi="['cps:rebate-asset-policy:update']" @click="confirmMigration">确认发布 B 核验</el-button>
            <el-button type="primary" v-hasPermi="['cps:rebate-asset-policy:update']" @click="savePolicy">保存策略</el-button>
          </el-form-item>
          <el-form-item v-if="migrationReport" label="最近一次预检">
            <el-tag :type="migrationReport.ready ? 'success' : 'danger'">{{ migrationReport.ready ? 'READY' : 'BLOCKED' }}</el-tag>
            <span class="ml-12px">{{ migrationReport.summary }}{{ migrationReport.ready ? '' : `（差异 ${migrationIssueCount} 项）` }}</span>
          </el-form-item>
        </el-form>
      </ContentWrap>
    </el-tab-pane>
  </el-tabs>

  <el-dialog v-model="adjustVisible" title="人工调整会员欠款" width="520px">
    <el-form :model="adjust" label-width="100px">
      <el-form-item label="会员ID" prop="memberId">
        <el-select
          v-model="adjust.memberId"
          filterable
          remote
          reserve-keyword
          clearable
          class="w-full"
          placeholder="请选择会员"
          :remote-method="searchMemberOptions"
          :loading="memberLoading"
          @visible-change="handleMemberDropdownVisible"
        >
          <el-option
            v-for="item in memberOptions"
            :key="item.id"
            :label="formatMemberLabel(item)"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="动作"><el-radio-group v-model="adjust.action"><el-radio value="WAIVE">减免</el-radio><el-radio value="INCREASE">增加</el-radio></el-radio-group></el-form-item>
      <el-form-item label="金额（分）"><el-input-number v-model="adjust.amountCent" :min="1" /></el-form-item>
      <el-form-item label="原因"><el-input v-model="adjust.reason" type="textarea" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="adjustVisible=false">取消</el-button><el-button type="primary" @click="submitAdjust">确定</el-button></template>
  </el-dialog>
</template>

<script setup lang="ts">
import * as AssetApi from '@/api/cps/asset'
import type { CpsOrderAttributionLogVO, CpsOrderSyncCheckpointVO, CpsRebateAssetLedgerVO, CpsRebateAssetMigrationCheckReportVO, CpsRebateAssetPolicyVO, CpsRebateDebtVO } from '@/api/cps/asset'
import { getUserPage, type UserVO } from '@/api/member/user/index'

defineOptions({ name: 'CpsAssetSafety' })
const message = useMessage()
const activeTab = ref('debt')
const money = (cent?: number) => `￥${((cent || 0) / 100).toFixed(2)}`
const signedMoney = (cent?: number) => `${(cent || 0) >= 0 ? '+' : ''}${money(cent)}`
const date = (value?: Date | string) => value ? new Date(value).toLocaleString('zh-CN') : '-'

const memberLoading = ref(false)
const memberOptions = ref<UserVO[]>([])
const searchMemberOptions = async (keyword = '') => {
  memberLoading.value = true
  try {
    const queryText = keyword.trim()
    const data = await getUserPage({
      pageNo: 1,
      pageSize: 20,
      mobile: /^\d+$/.test(queryText) ? queryText : undefined,
      nickname: queryText && !/^\d+$/.test(queryText) ? queryText : undefined
    })
    memberOptions.value = data?.list || []
  } finally {
    memberLoading.value = false
  }
}
const handleMemberDropdownVisible = (visible: boolean) => {
  if (visible && memberOptions.value.length === 0) searchMemberOptions()
}
const formatMemberLabel = (item: UserVO) => {
  const parts = [`ID:${item.id}`]
  if (item.nickname) parts.push(item.nickname)
  if (item.name) parts.push(item.name)
  if (item.mobile) parts.push(item.mobile)
  return parts.join(' / ')
}

const debtLoading = ref(false), debtList = ref<CpsRebateDebtVO[]>([]), debtTotal = ref(0)
const debtQuery = reactive({ pageNo: 1, pageSize: 10, memberId: undefined as number | undefined, status: undefined as string | undefined })
const loadDebt = async () => { debtLoading.value = true; try { const data = await AssetApi.getDebtPage(debtQuery); debtList.value = data.list; debtTotal.value = data.total } finally { debtLoading.value = false } }

const ledgerLoading = ref(false), ledgerList = ref<CpsRebateAssetLedgerVO[]>([]), ledgerTotal = ref(0)
const ledgerQuery = reactive({ pageNo: 1, pageSize: 10, memberId: undefined as number | undefined, businessType: undefined as string | undefined })
const loadLedger = async () => { ledgerLoading.value = true; try { const data = await AssetApi.getAssetLedgerPage(ledgerQuery); ledgerList.value = data.list; ledgerTotal.value = data.total } finally { ledgerLoading.value = false } }

const attributionLoading = ref(false), attributionList = ref<CpsOrderAttributionLogVO[]>([]), attributionTotal = ref(0)
const attributionQuery = reactive({ pageNo: 1, pageSize: 10, platformOrderId: undefined as string | undefined, result: undefined as string | undefined })
const loadAttribution = async () => { attributionLoading.value = true; try { const data = await AssetApi.getAttributionLogPage(attributionQuery); attributionList.value = data.list; attributionTotal.value = data.total } finally { attributionLoading.value = false } }

const checkpointLoading = ref(false), checkpointList = ref<CpsOrderSyncCheckpointVO[]>([]), checkpointTotal = ref(0)
const checkpointQuery = reactive({ pageNo: 1, pageSize: 10, platformCode: undefined as string | undefined, lastSyncStatus: undefined as string | undefined })
const loadCheckpoint = async () => { checkpointLoading.value = true; try { const data = await AssetApi.getSyncCheckpointPage(checkpointQuery); checkpointList.value = data.list; checkpointTotal.value = data.total } finally { checkpointLoading.value = false } }

const policyLoading = ref(false)
const migrationReport = ref<CpsRebateAssetMigrationCheckReportVO>()
const migrationIssueCount = computed(() => {
  const report = migrationReport.value
  if (!report) return 0
  return [report.duplicateAccountCount, report.duplicateOrderCount, report.duplicateRebateRecordCount,
    report.duplicateLedgerIdempotencyCount, report.duplicateFreezeIdempotencyCount,
    report.accountLedgerMismatchCount, report.freezeAccountMismatchCount, report.missingOpeningBalanceCount,
    report.orphanLedgerCount, report.orphanActiveFreezeCount].reduce((sum, value) => sum + (value || 0), 0)
})
const policy = reactive<CpsRebateAssetPolicyVO>({ v2Enabled: false, migrationReady: false, readOnly: false, largeDebtThresholdCent: 10000, reminderIntervalDays: 7, normalReminderDays: 30, largeReminderDays: 180, smsIntervalDays: 30 })
const loadPolicy = async () => {
  policyLoading.value = true
  try {
    // 幂等初始化：新租户无需手工插入策略或冻结配置；已有策略不会被覆盖。
    try { Object.assign(policy, await AssetApi.initializeAssetPolicy()) } catch { /* 仅有查询权限时继续读取策略 */ }
    Object.assign(policy, await AssetApi.getAssetPolicy())
  } catch { message.error('资产策略加载失败，请确认已执行 CPS 增量脚本且账号具备策略查询权限') }
  finally { policyLoading.value = false }
}
const bootstrapPolicy = async () => {
  policyLoading.value = true
  try {
    const result = await AssetApi.bootstrapAssetPolicy()
    Object.assign(policy, result.policy)
    migrationReport.value = result.migrationReport
    if (result.enabled) message.success('返利资产已启用')
    else if (result.migrationReport.ready) message.success(result.nextStep)
    else message.warning(`${result.nextStep}（差异 ${migrationIssueCount.value} 项）`)
  } finally { policyLoading.value = false }
}
const confirmMigration = async () => {
  try {
    await message.confirm('请确认发布 B 变更已完成，并继续迁移核验。', '确认发布 B 核验')
    policyLoading.value = true
    // 不再要求手工输入凭证，仍提交固定标记以保留服务端审计字段。
    Object.assign(policy, await AssetApi.confirmAssetMigration('ADMIN_UI_CONFIRM'))
    message.success('发布 B 迁移核验已确认，现在可以保存策略启用返利资产')
  } catch {
    // 取消确认或接口失败均由全局请求拦截器提示
  } finally { policyLoading.value = false }
}
const savePolicy = async () => { await AssetApi.saveAssetPolicy(policy); message.success('租户资产策略已保存') }
const backfillOpeningBalances = async () => {
  await message.confirm('仅允许在发布 B 启用前执行。该操作不会修改账户余额，只会追加可审计期初流水。')
  const count = await AssetApi.backfillOpeningBalances()
  message.success(`已追加 ${count} 条期初流水`)
}

const adjustVisible = ref(false)
const adjust = reactive<{ memberId?: number; action: 'WAIVE' | 'INCREASE'; amountCent: number; reason: string }>({ action: 'WAIVE', amountCent: 1, reason: '' })
const openAdjust = () => { adjust.memberId = debtQuery.memberId; adjust.amountCent = 1; adjust.reason = ''; adjustVisible.value = true }
const submitAdjust = async () => {
  if (!adjust.memberId || !adjust.reason.trim()) return message.warning('会员ID和调整原因不能为空')
  await AssetApi.adjustDebt({ memberId: adjust.memberId, action: adjust.action, amountCent: adjust.amountCent, reason: adjust.reason, idempotencyKey: crypto.randomUUID() })
  adjustVisible.value = false; message.success('欠款调整已写入审计流水'); await loadDebt()
}

const loaders: Record<string, () => Promise<void>> = { debt: loadDebt, ledger: loadLedger, attribution: loadAttribution, checkpoint: loadCheckpoint, policy: loadPolicy }
const loadActiveTab = () => loaders[activeTab.value]?.()
onMounted(loadDebt)
</script>
