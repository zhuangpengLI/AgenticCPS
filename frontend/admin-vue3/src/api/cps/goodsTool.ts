import request from '@/config/axios'

export interface CpsGoodsRebateQueryReqVO {
  platformCode: string
  originalContent: string
  memberId: number
  adzoneId?: string
}

export interface CpsGoodsRebateQueryRespVO {
  parseStatus: string
  parseMessage: string
  goods?: {
    platformCode?: string
    goodsId?: string
    goodsSign?: string
    itemLink?: string
    title?: string
    mainPic?: string
    shopName?: string
    actualPrice?: number
    couponInfo?: string
  }
  rebate?: {
    commissionRate?: number
    commissionAmount?: number
    estimateRebateAmount?: number
    usedAdzoneId?: string
  }
  links?: {
    shortUrl?: string
    longUrl?: string
    tpwd?: string
    mobileUrl?: string
  }
  transferRecordId?: number
}

export const CpsGoodsToolApi = {
  queryRebate: (data: CpsGoodsRebateQueryReqVO) =>
    request.post<CpsGoodsRebateQueryRespVO>({ url: '/cps/goods/rebate-query', data })
}
