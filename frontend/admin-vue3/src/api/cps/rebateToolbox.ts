import request from '@/config/axios'
import type { CpsGoodsRebateQueryRespVO } from './goodsTool'

export interface CpsGoodsParseReqVO {
  platformCode: string
  originalContent: string
}

export interface CpsGoodsParseRespVO {
  platformCode: string
  supported: boolean
  goodsId?: string
  goodsSign?: string
  itemLink?: string
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

export const CpsRebateToolboxApi = {
  parseContent: (data: CpsGoodsParseReqVO) =>
    request.post<CpsGoodsParseRespVO>({ url: '/cps/goods/parse', data }),

  batchTransfer: (data: CpsGoodsBatchTransferReqVO) =>
    request.post<CpsGoodsBatchTransferRespVO>({ url: '/cps/goods/batch-transfer', data })
}
