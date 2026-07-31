import request from '@/config/axios'

export interface CpsRebateDebtVO {
  id: number
  memberId: number
  orderId?: number
  platformOrderId?: string
  sourceBusinessId: string
  originalDebtCent: number
  repaidDebtCent: number
  waivedDebtCent: number
  outstandingDebtCent: number
  status: string
  lastReminderTime?: Date
  lastSmsTime?: Date
  nextReminderTime?: Date
  createTime: Date
}

export interface CpsRebateAssetLedgerVO {
  id: number
  memberId: number
  businessType: string
  businessId: string
  orderId?: number
  platformOrderId?: string
  idempotencyKey: string
  availableChangeCent: number
  frozenChangeCent: number
  debtChangeCent: number
  availableAfterCent: number
  frozenAfterCent: number
  debtAfterCent: number
  operatorType: string
  operatorId?: string
  reason: string
  createTime: Date
}

export interface CpsRebateAssetPolicyVO {
  v2Enabled: boolean
  migrationReady?: boolean
  readOnly: boolean
  largeDebtThresholdCent: number
  reminderIntervalDays: number
  normalReminderDays: number
  largeReminderDays: number
  smsIntervalDays: number
}

export interface CpsOrderAttributionLogVO {
  id: number
  orderId?: number
  platformCode: string
  platformOrderId: string
  candidateMemberId?: number
  attributedMemberId?: number
  attributionSource?: string
  bindingType?: string
  bindingId?: string
  action: string
  result: string
  rejectReason?: string
  operatorType?: string
  operatorId?: string
  idempotencyKey?: string
  reviewStatus?: string
  reviewAuditNote?: string
  reviewOperatorId?: number
  reviewTime?: Date
  createTime: Date
}

export interface CpsOrderSyncCheckpointVO {
  id: number
  platformCode: string
  vendorCode: string
  orderScene: number
  queryType: string
  paginationMode: string
  nextCursor?: string
  nextPageNo?: number
  watermarkTime?: Date
  lastSyncStatus: string
  lastSuccessCount: number
  lastFailureCount: number
  failureSummary?: string
  updateTime: Date
}

export const getDebtPage = (params: Record<string, any>) =>
  request.get({ url: '/cps/rebate-asset/debt/page', params })

export const adjustDebt = (data: {
  memberId: number
  action: 'WAIVE' | 'INCREASE'
  amountCent: number
  reason: string
  idempotencyKey: string
}) => request.post({ url: '/cps/rebate-asset/debt/adjust', data })

export const getAssetLedgerPage = (params: Record<string, any>) =>
  request.get({ url: '/cps/rebate-asset/ledger/page', params })

export const getAssetPolicy = () => request.get({ url: '/cps/rebate-asset/policy' })

export const saveAssetPolicy = (data: CpsRebateAssetPolicyVO) =>
  request.put({ url: '/cps/rebate-asset/policy', data })

export const backfillOpeningBalances = () =>
  request.post({ url: '/cps/rebate-asset/migration/opening-balances' })

export const getAttributionLogPage = (params: Record<string, any>) =>
  request.get({ url: '/cps/order/attribution-log/page', params })

export const getSyncCheckpointPage = (params: Record<string, any>) =>
  request.get({ url: '/cps/order/sync-checkpoint/page', params })
