import request from '@/config/axios'

// CPS 订单 VO
export interface CpsOrderVO {
  id: number
  platformCode: string
  platformOrderId: string
  parentOrderId?: string
  memberId?: number
  memberNickname?: string
  itemId?: string
  itemTitle?: string
  itemPic?: string
  itemPrice?: number
  finalPrice?: number
  couponAmount?: number
  commissionRate?: number
  commissionAmount?: number
  estimateRebate?: number
  realRebate?: number
  adzoneId?: string
  externalInfo?: string
  specialId?: string
  relationId?: string
  orderScene?: number
  attributionSource?: string
  orderStatus: string
  syncTime?: Date
  settleTime?: Date
  rebateTime?: Date
  refundTime?: Date
  confirmReceiptTime?: Date
  rebateFreezeStatus?: string
  planUnfreezeTime?: Date
  createTime: Date
}

// CPS 订单分页请求 VO
export interface CpsOrderPageReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  memberId?: number
  memberName?: string
  orderStatus?: string
  itemTitle?: string
  platformOrderId?: string
  createTime?: [Date, Date]
}

export interface CpsOrderBindSpecialIdReqVO {
  orderId: number
  memberId: number
  idempotencyKey: string
  auditNote?: string
}

export interface CpsOrderClaimVO {
  id: number
  orderId?: number
  platformCode: string
  platformOrderId: string
  candidateMemberId?: number
  attributedMemberId?: number
  attributionSource?: string
  result: string
  rejectReason?: string
  reviewStatus?: string
  reviewAuditNote?: string
  reviewOperatorId?: number
  reviewTime?: Date
  createTime: Date
}

export interface CpsOrderClaimPageReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  platformOrderId?: string
  reviewStatus?: string
}

export interface CpsOrderClaimReviewReqVO {
  claimId: number
  approved: boolean
  auditNote: string
}

/** 订单同步批次 */
export interface CpsOrderSyncBatchVO {
  id: number
  batchType: string
  queryType: number
  platformCode: string
  vendorCode?: string
  orderScene?: number
  startTime: number
  endTime: number
  status: string
  totalWindows: number
  successWindows: number
  failedWindows: number
  retryWindows?: number
  createdBy?: string
  createTime: string
  updateTime?: string
  lastErrorMessage?: string
}

export interface CpsOrderSyncWindowVO {
  id: number
  batchId: number
  windowStart: string
  windowEnd: string
  status: string
  pageNo?: number
  positionIndex?: string
  retryCount?: number
  nextRetryTime?: string
  leaseUntil?: string
  lastErrorCode?: string
  lastErrorMessage?: string
  updateTime?: string
}

export interface CpsOrderSyncBatchPageReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  status?: string
  batchType?: string
  queryType?: number
}

export interface CpsOrderSyncBatchCreateReqVO {
  platformCode: string
  vendorCode?: string
  orderScene?: number
  batchType?: string
  queryType: number
  startTime: string | number
  endTime: string | number
}

export interface CpsOrderSyncMetricsVO {
  runningBatches: number
  pendingWindows: number
  retryWindows: number
  deadWindows: number
  successRate: number
  latestWatermark?: string
  maxDelayMinutes?: number
}

export interface CpsOrderSyncPolicyVO {
  id?: number
  platformCode: string
  vendorCode?: string
  orderScene?: number
  realtimePaymentEnabled: boolean
  realtimeSettlementEnabled: boolean
  nightlyPaymentEnabled: boolean
  monthlySettlementEnabled: boolean
  updateCatchupEnabled: boolean
  catchupDays: number
  overlapMinutes: number
  maxConcurrency: number
  requestsPerMinute: number
}

// 查询订单分页列表
export const getCpsOrderPage = async (params: CpsOrderPageReqVO) => {
  return await request.get({ url: '/cps/order/page', params })
}

// 查询订单详情
export const getCpsOrder = async (id: number) => {
  return await request.get({ url: '/cps/order/get', params: { id } })
}

// 删除订单
export const deleteCpsOrder = async (id: number) => {
  return await request.delete({ url: '/cps/order/delete', params: { id } })
}

// 批量删除订单
export const deleteCpsOrderList = async (ids: number[]) => {
  return await request.delete({ url: '/cps/order/delete-list', params: { ids: ids.join(',') } })
}

export interface CpsOrderSyncReqVO {
  platformCode: string
  vendorCode?: string
  hours?: number
  queryType?: number
  orderStatus?: number
  startTime?: string
  endTime?: string
}

// 按平台和 API 供应商手动触发订单同步
export const syncCpsOrders = async (params: CpsOrderSyncReqVO) => {
  return await request.post({
    url: '/cps/order/sync',
    params
  })
}

// 订单同步批次与监控
export const createOrderSyncBatch = async (data: CpsOrderSyncBatchCreateReqVO) =>
  await request.post({ url: '/cps/order/sync/batches', data })

export const getOrderSyncBatchPage = async (params: CpsOrderSyncBatchPageReqVO) =>
  await request.get({ url: '/cps/order/sync/batches', params })

export const getOrderSyncBatchWindows = async (
  batchId: number,
  params?: { pageNo?: number; pageSize?: number }
) => await request.get({ url: `/cps/order/sync/batches/${batchId}/windows`, params })

export const pauseOrderSyncBatch = async (batchId: number) =>
  await request.post({ url: `/cps/order/sync/batches/${batchId}/pause` })

export const resumeOrderSyncBatch = async (batchId: number) =>
  await request.post({ url: `/cps/order/sync/batches/${batchId}/resume` })

export const cancelOrderSyncBatch = async (batchId: number) =>
  await request.post({ url: `/cps/order/sync/batches/${batchId}/cancel` })

export const deleteOrderSyncBatch = async (batchId: number) =>
  await request.delete({ url: `/cps/order/sync/batches/${batchId}` })

export const replayOrderSyncWindow = async (windowId: number) =>
  await request.post({ url: `/cps/order/sync/windows/${windowId}/replay` })

export const getOrderSyncMetrics = async () => await request.get({ url: '/cps/order/sync/metrics' })

export const getOrderSyncPolicy = async (params: {
  platformCode: string
  vendorCode?: string
  orderScene?: number
}) => await request.get({ url: '/cps/order/sync/policy/get', params })

export const saveOrderSyncPolicy = async (data: CpsOrderSyncPolicyVO) =>
  await request.put({ url: '/cps/order/sync/policy/save', data })

// 手动绑定订单 special_id 到会员
export const bindSpecialIdToMember = async (data: CpsOrderBindSpecialIdReqVO) => {
  return await request.post({ url: '/cps/order/bind-special-id-member', data })
}

export const getOrderClaimPage = async (params: CpsOrderClaimPageReqVO) => {
  return await request.get({ url: '/cps/order/claim/page', params })
}

export const reviewOrderClaim = async (data: CpsOrderClaimReviewReqVO) => {
  return await request.post({ url: '/cps/order/claim/review', data })
}
