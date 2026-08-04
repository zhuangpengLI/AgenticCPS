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

// 手动触发订单同步
export const syncCpsOrders = async (platformCode: string, hours = 2, queryType = 1) => {
  return await request.post({
    url: '/cps/order/sync',
    params: { platformCode, hours, queryType }
  })
}

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
