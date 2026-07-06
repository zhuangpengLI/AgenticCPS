import request from '@/config/axios'
import type { CpsGoodsRebateQueryRespVO } from './goodsTool'
import type { CpsGoodsSquareGoodsVO } from './goodsSquare'

export interface CpsGoodsParseReqVO {
  platformCode: string
  vendorCode?: string
  originalContent: string
}

export interface CpsGoodsParseRespVO {
  platformCode: string
  supported: boolean
  goodsId?: string
  goodsSign?: string
  itemLink?: string
  couponLink?: string
  sourceLink?: string
  title?: string
  parseSource?: 'local' | 'platform' | string
  failureCode?: string
  failureReason?: string
}

export interface CpsGoodsBatchTransferReqVO {
  platformCode: string
  originalContents: string[]
  memberId: number
  vendorCode?: string
  adzoneId?: string
}

export interface CpsGoodsBatchTransferItemVO {
  inputIndex: number
  originalContent: string
  status: string
  message?: string
  goods?: CpsGoodsRebateQueryRespVO['goods']
  rebate?: CpsGoodsRebateQueryRespVO['rebate']
  links?: CpsGoodsRebateQueryRespVO['links']
  transferRecordId?: number
  promotionContent?: string
}

export interface CpsGoodsBatchTransferRespVO {
  items: CpsGoodsBatchTransferItemVO[]
  successCount: number
  failureCount: number
}

export interface CpsGoodsOwnershipCheckReqVO {
  platformCode: string
  originalContent: string
  memberId?: number
  adzoneId?: string
  transferRecordId?: number
}

export interface CpsGoodsOwnershipCheckRespVO {
  checkStatus: 'MATCH' | 'MISMATCH' | 'NOT_FOUND' | string
  message?: string
  ownershipResult?: string
  platformCode?: string
  itemId?: string
  itemTitle?: string
  transferRecordId?: number
  recordMemberId?: number
  recordMemberNickname?: string
  recordMemberMobile?: string
  recordAdzoneId?: string
  pid?: string
  promotionUrl?: string
  taoCommand?: string
  recordStatus?: number
  createTime?: string
  mismatches?: string[]
}

export interface CpsGoodsCouponQueryReqVO {
  platformCode: string
  queryText: string
  vendorCode?: string
  couponAmountMin?: number
  pageNo: number
  pageSize: number
}

export interface CpsGoodsCouponQueryRespVO {
  platformCode: string
  vendorCode?: string
  keyword: string
  list: CpsGoodsSquareGoodsVO[]
  total: number
  pageNo: number
  pageSize: number
  summary?: string
}

export interface CpsGoodsCashGiftPlanReqVO {
  templateCode: string
  campaignName: string
  platformCode?: string
  goodsId?: string
  title?: string
  budgetAmount: number
  giftAmount: number
  totalQuantity: number
  perUserLimit?: number
  startTime?: string
  endTime?: string
}

export interface CpsGoodsCashGiftPlanRespVO {
  planStatus: 'PLAN_ONLY' | 'READY' | 'RISK' | string
  message?: string
  templateCode: string
  campaignName: string
  budgetAmount: number
  giftAmount: number
  totalQuantity: number
  budgetGap: number
  budgetEnough: boolean
  promotionContent?: string
  checklist: string[]
  warnings: string[]
  templates: Array<{
    code: string
    name: string
    scene: string
    suggestion: string
  }>
}

export const CpsRebateToolboxApi = {
  parseContent: (data: CpsGoodsParseReqVO) =>
    request.post<CpsGoodsParseRespVO>({ url: '/cps/goods/parse', data }),

  batchTransfer: (data: CpsGoodsBatchTransferReqVO) =>
    request.post<CpsGoodsBatchTransferRespVO>({ url: '/cps/goods/batch-transfer', data }),

  checkOwnership: (data: CpsGoodsOwnershipCheckReqVO) =>
    request.post<CpsGoodsOwnershipCheckRespVO>({ url: '/cps/goods/ownership-check', data }),

  queryCoupons: (data: CpsGoodsCouponQueryReqVO) =>
    request.post<CpsGoodsCouponQueryRespVO>({ url: '/cps/goods/coupon-query', data }),

  planCashGift: (data: CpsGoodsCashGiftPlanReqVO) =>
    request.post<CpsGoodsCashGiftPlanRespVO>({ url: '/cps/goods/cash-gift/plan', data })
}
