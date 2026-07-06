import request from '@/config/axios'

export interface CpsGoodsSquareSearchReqVO {
  keyword?: string
  platformCode?: string
  vendorCode?: string
  pageNo: number
  pageSize: number
  priceLowerLimit?: number
  priceUpperLimit?: number
  sortType?: number
  hasCoupon?: number
  channelCode?: string
  categoryId?: string
  minCommissionRate?: number
  minCommissionAmount?: number
  minMonthSales?: number
  couponAmountMin?: number
  tmallOnly?: boolean
  brandOnly?: boolean
  shopType?: string
  activityTag?: string
}

export interface CpsGoodsSquareGoodsVO {
  goodsId: string
  platformCode: string
  vendorCode?: string
  title: string
  mainPic?: string
  originalPrice?: number
  actualPrice?: number
  couponPrice?: number
  couponConditions?: number
  couponTotalNum?: number
  couponRemainNum?: number
  couponReceiveNum?: number
  commissionRate?: number
  commissionAmount?: number
  monthSales?: number
  shopName?: string
  itemLink?: string
  brandName?: string
  goodsSign?: string
  source?: string
  activityTag?: string
  categoryName?: string
  couponStartTime?: string
  couponEndTime?: string
  rankTag?: string
  sellingPoint?: string
}

export interface CpsGoodsSquareSearchRespVO {
  list: CpsGoodsSquareGoodsVO[]
  total: number
  nextPageId?: string
  pageNo: number
  pageSize: number
}

export interface CpsGoodsSquareLinkReqVO {
  platformCode: string
  goodsId: string
  goodsSign?: string
  memberId: number
  adzoneId?: string
  vendorCode?: string
  title?: string
  originalContent?: string
}

export interface CpsGoodsSquareLinkRespVO {
  linkStatus: 'SUCCESS' | 'FAILED'
  linkMessage?: string
  transferRecordId?: number
  adzoneId?: string
  shortUrl?: string
  longUrl?: string
  tpwd?: string
  mobileUrl?: string
  actualPrice?: number
  commissionRate?: number
  commissionAmount?: number
  couponInfo?: string
  promotionContent?: string
}

export interface CpsGoodsSquareMetaItemVO {
  value: string
  label: string
  tag?: string
  imageUrl?: string
  description?: string
}

export interface CpsGoodsSquareMetaRespVO {
  platformCode: string
  vendorCode: string
  metaSource: string
  usingVendorMeta: boolean
  taobaoSelectionSupported: boolean
  capabilityDesc?: string
  activities: CpsGoodsSquareMetaItemVO[]
  hotKeywords: CpsGoodsSquareMetaItemVO[]
  categories: CpsGoodsSquareMetaItemVO[]
  sortOptions: CpsGoodsSquareMetaItemVO[]
  filterOptions: CpsGoodsSquareMetaItemVO[]
}

export const GOODS_SORT_TYPE_OPTIONS = [
  { label: '综合排序', value: 0 },
  { label: '销量优先', value: 1 },
  { label: '券后价升序', value: 2 },
  { label: '券后价降序', value: 3 },
  { label: '佣金优先', value: 4 }
]

export const CpsGoodsSquareApi = {
  getMeta: (params: { platformCode?: string; vendorCode?: string }) =>
    request.get<CpsGoodsSquareMetaRespVO>({ url: '/cps/goods-square/meta', params }),

  searchGoods: (params: CpsGoodsSquareSearchReqVO) =>
    request.get<CpsGoodsSquareSearchRespVO>({ url: '/cps/goods-square/search', params }),

  generateLink: (data: CpsGoodsSquareLinkReqVO) =>
    request.post<CpsGoodsSquareLinkRespVO>({ url: '/cps/goods-square/link', data })
}
