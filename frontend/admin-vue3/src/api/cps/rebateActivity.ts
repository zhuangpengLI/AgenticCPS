import request from '@/config/axios'

export interface CpsRebateActivityVO {
  id: number
  activityName: string
  activityType: string
  platformCode: string
  mainPic?: string
  shortDesc?: string
  rebateDesc?: string
  billingType?: string
  promotionCount?: number
  sourceType?: string
  externalActivityId?: string
  tagText?: string
  jumpType: string
  jumpUrl?: string
  searchKeyword?: string
  sort: number
  status: number
  startTime?: Date | string | number
  endTime?: Date | string | number
  remark?: string
  createTime?: Date
  supportsList?: boolean
  supportsPromotionLink?: boolean
  supportsOrders?: boolean
  supportsMiniProgram?: boolean
  supportsLocalLife?: boolean
}

export interface CpsRebateActivitySaveVO {
  id?: number
  activityName: string
  activityType: string
  platformCode: string
  mainPic?: string
  shortDesc?: string
  rebateDesc?: string
  billingType?: string
  promotionCount?: number
  sourceType?: string
  externalActivityId?: string
  tagText?: string
  jumpType: string
  jumpUrl?: string
  searchKeyword?: string
  sort: number
  status: number
  startTime?: Date | string | number
  endTime?: Date | string | number
  remark?: string
}

export interface CpsRebateActivityPageReqVO {
  pageNo: number
  pageSize: number
  activityName?: string
  activityType?: string
  platformCode?: string
  billingType?: string
  status?: number
}

export const ACTIVITY_TYPE_OPTIONS = [
  { label: '会员权益', value: '会员权益' },
  { label: '外卖', value: '外卖' },
  { label: '电影票', value: '电影票' },
  { label: '本地生活', value: '本地生活' },
  { label: '旅行', value: '旅行' },
  { label: '其他', value: '其他' }
]

export const ACTIVITY_BILLING_TYPE_OPTIONS = [
  { label: 'CPS', value: 'CPS' },
  { label: 'CPA', value: 'CPA' },
  { label: 'CPS+CPA', value: 'CPS+CPA' }
]

export const ACTIVITY_JUMP_TYPE_OPTIONS = [
  { label: '搜索关键词', value: 'search' },
  { label: '外部链接', value: 'url' },
  { label: '无跳转', value: 'none' }
]

export interface CpsRebateActivityCenterReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  sourceType?: string
  billingType?: string
  keyword?: string
  sortMode?: string
  localLifeOnly?: boolean
}

export interface CpsRebateActivityCenterTabVO {
  platformCode: string
  platformName: string
  platformLogo?: string
  activityCount: number
}

export interface CpsRebateActivityCenterOptionVO {
  value: string
  label: string
  count: number
}

export interface CpsRebateActivityCenterCardVO {
  id: number
  activityName: string
  activityType: string
  platformCode: string
  platformName: string
  platformLogo?: string
  mainPic?: string
  shortDesc?: string
  rebateDesc?: string
  billingType?: string
  promotionCount?: number
  sourceType?: string
  externalActivityId?: string
  tagText?: string
  jumpType: string
  jumpUrl?: string
  searchKeyword?: string
  startTime?: Date | string | number
  endTime?: Date | string | number
  supportsList?: boolean
  supportsPromotionLink?: boolean
  supportsOrders?: boolean
  supportsMiniProgram?: boolean
  supportsLocalLife?: boolean
}

export interface CpsRebateActivityCenterRespVO {
  tabs: CpsRebateActivityCenterTabVO[]
  billingTypeOptions: CpsRebateActivityCenterOptionVO[]
  cards: CpsRebateActivityCenterCardVO[]
  total: number
  pageNo: number
  pageSize: number
}

export interface CpsRebateActivitySyncReqVO {
  vendorCode: string
  platformCode?: string
  keyword?: string
  pageSize?: number
  maxPages?: number
}

export interface CpsRebateActivitySyncRespVO {
  insertedCount: number
  updatedCount: number
  skippedCount: number
}

export interface CpsRebateActivityPromotionReqVO {
  activityId: number
  memberId: number
  adzoneId?: string
  channelTag?: string
}

export interface CpsRebateActivityPromotionRespVO {
  linkStatus: 'SUCCESS' | 'INTERNAL_FALLBACK' | 'FAILED'
  linkType: 'EXTERNAL_PROMOTION' | 'INTERNAL_LANDING' | 'NONE'
  linkMessage?: string
  attributionStatus?: 'MEMBER_TRACKED' | 'CHANNEL_TRACKED' | 'UNTRACKED'
  attributionMessage?: string
  activityId: number
  activityName: string
  platformCode: string
  adzoneId?: string
  channelTag?: string
  promotionUrl?: string
  tpwd?: string
  longTpwd?: string
  promotionContent?: string
}

export const CpsRebateActivityApi = {
  createActivity: (data: CpsRebateActivitySaveVO) =>
    request.post({ url: '/cps/rebate-activity/create', data }),

  updateActivity: (data: CpsRebateActivitySaveVO) =>
    request.put({ url: '/cps/rebate-activity/update', data }),

  deleteActivity: (id: number) =>
    request.delete({ url: '/cps/rebate-activity/delete', params: { id } }),

  getActivity: (id: number) =>
    request.get<CpsRebateActivityVO>({ url: '/cps/rebate-activity/get', params: { id } }),

  getActivityPage: (params: CpsRebateActivityPageReqVO) =>
    request.get<{ list: CpsRebateActivityVO[]; total: number }>({
      url: '/cps/rebate-activity/page',
      params
    }),

  getEnabledActivityList: () =>
    request.get<CpsRebateActivityVO[]>({ url: '/cps/rebate-activity/list-enabled' }),

  getActivityCenter: (params: CpsRebateActivityCenterReqVO) =>
    request.get<CpsRebateActivityCenterRespVO>({ url: '/cps/rebate-activity/center', params }),

  generatePromotion: (data: CpsRebateActivityPromotionReqVO) =>
    request.post<CpsRebateActivityPromotionRespVO>({
      url: '/cps/rebate-activity/promotion',
      data
    }),

  syncActivities: (data: CpsRebateActivitySyncReqVO) =>
    request.post<CpsRebateActivitySyncRespVO>({ url: '/cps/rebate-activity/sync', data })
}
