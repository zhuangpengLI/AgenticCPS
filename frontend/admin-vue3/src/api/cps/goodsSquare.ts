import request from '@/config/axios'

export interface CpsGoodsSquareSearchReqVO {
  keyword?: string
  searchMode?: string
  searchField?: string
  imageBase64?: string
  goodsSign?: string
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
  couponPriceUpperLimit?: number
  hotRankMin?: number
  couponExpireDays?: number
  tmallOnly?: boolean
  brandOnly?: boolean
  haitaoOnly?: boolean
  goldSellerOnly?: boolean
  tchaoshiOnly?: boolean
  juhuasuanOnly?: boolean
  taoqianggouOnly?: boolean
  inspectedGoodsOnly?: boolean
  freeshipRemoteDistrict?: boolean
  shopType?: string
  goodsPerformance?: string
  commercialOnly?: boolean
  preSaleOnly?: boolean
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
  { label: '综合', value: 0 },
  { label: '月热销', value: 1 },
  { label: '券后价升序', value: 2 },
  { label: '券后价降序', value: 3 },
  { label: '佣金比例', value: 4 },
  { label: '领券量', value: 5 },
  { label: '最新上架', value: 6 }
]

export const CpsGoodsSquareApi = {
  getMeta: (params: { platformCode?: string; vendorCode?: string }) =>
    request.get<CpsGoodsSquareMetaRespVO>({ url: '/cps/goods-square/meta', params }),

  getHotKeywords: (params: { platformCode?: string; vendorCode?: string; type?: number }) =>
    request.get<CpsGoodsSquareMetaItemVO[]>({ url: '/cps/goods-square/hot-keywords', params }),

  suggestKeywords: (params: {
    platformCode?: string
    vendorCode?: string
    keyword: string
    type?: number
  }) => request.get<CpsGoodsSquareMetaItemVO[]>({ url: '/cps/goods-square/suggestions', params }),

  getVendorGoods: (params: {
    sourceCode: string
    platformCode?: string
    vendorCode?: string
    pageSize?: number
  }) => request.get<CpsGoodsSquareSearchRespVO>({ url: '/cps/goods-square/vendor-goods', params }),

  searchGoods: (params: CpsGoodsSquareSearchReqVO) =>
    request.get<CpsGoodsSquareSearchRespVO>({ url: '/cps/goods-square/search', params }),

  searchByImage: (data: CpsGoodsSquareSearchReqVO) =>
    request.post<CpsGoodsSquareSearchRespVO>({ url: '/cps/goods-square/search-by-image', data }),

  generateLink: (data: CpsGoodsSquareLinkReqVO) =>
    request.post<CpsGoodsSquareLinkRespVO>({ url: '/cps/goods-square/link', data })
}
