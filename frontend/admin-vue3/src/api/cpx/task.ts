import request from '@/config/axios'

export type CpxPromotionMethod = 'CPS' | 'CPA' | 'CPL' | 'CPM' | 'CPC' | 'OCPA' | 'OCPC' | 'MIXED'

export const CPX_PROMOTION_METHOD_OPTIONS = [
  { label: 'CPS 成交返利', value: 'CPS' },
  { label: 'CPA 有效动作', value: 'CPA' },
  { label: 'CPL 有效线索', value: 'CPL' },
  { label: 'CPM 有效曝光', value: 'CPM' },
  { label: 'CPC 有效点击', value: 'CPC' },
  { label: 'oCPA 优化转化', value: 'OCPA' },
  { label: 'oCPC 优化点击', value: 'OCPC' },
  { label: 'MIXED 混合展示', value: 'MIXED' }
]

export interface CpxTaskVO {
  id: number
  taskNo: string
  taskName: string
  platformCode: string
  promotionMethod: CpxPromotionMethod
  taskType?: string
  offerType?: string
  title?: string
  shortDesc?: string
  rewardDesc?: string
  budgetAmount?: number
  dailyBudgetAmount?: number
  rewardAmount?: number
  rewardRate?: number
  memberRewardEnabled?: boolean
  dedupeWindowSeconds?: number
  frequencyLimit?: number
  startTime?: Date | string
  endTime?: Date | string
  status: number
  priority?: number
  tags?: string
  materialJson?: string
  ruleJson?: string
  landingUrl?: string
  remark?: string
}

export interface CpxTaskSaveVO extends Partial<CpxTaskVO> {
  taskName: string
  platformCode: string
  promotionMethod: CpxPromotionMethod
}

export interface CpxArticleSaveVO {
  id?: number
  title: string
  category?: string
  summary?: string
  coverUrl?: string
  content?: string
  platformCode?: string
  promotionMethod?: CpxPromotionMethod
  relatedTaskId?: number
  tags?: string
  status?: number
  publishTime?: Date | string
}

export interface CpxPlatformProfileSaveVO {
  id?: number
  platformCode: string
  platformName: string
  platformLogo?: string
  supportedMethods?: string
  apiBaseUrl?: string
  callbackUrl?: string
  importTemplate?: string
  healthStatus?: string
  status?: number
  remark?: string
  extraConfig?: string
}

export interface CpxDashboardRespVO {
  taskCount: number
  onlineTaskCount: number
  taskCountByMethod: Partial<Record<CpxPromotionMethod, number>>
  impressionCount: number
  clickCount: number
  leadCount: number
  actionCount: number
  conversionCount: number
  settlementCount: number
  settlementAmount: number
  rewardAmount: number
}

export const CpxTaskApi = {
  getDashboardSummary: () => request.get<CpxDashboardRespVO>({ url: '/cpx/dashboard/summary' }),
  createTask: (data: CpxTaskSaveVO) => request.post({ url: '/cpx/task/create', data }),
  updateTask: (data: CpxTaskSaveVO) => request.put({ url: '/cpx/task/update', data }),
  getTask: (id: number) => request.get<CpxTaskVO>({ url: '/cpx/task/get', params: { id } }),
  listTasks: (params?: { keyword?: string; promotionMethod?: CpxPromotionMethod; limit?: number }) =>
    request.get<CpxTaskVO[]>({ url: '/cpx/task/list', params }),
  createArticle: (data: CpxArticleSaveVO) => request.post({ url: '/cpx/article/create', data }),
  updateArticle: (data: CpxArticleSaveVO) => request.put({ url: '/cpx/article/update', data }),
  getArticle: (id: number) => request.get({ url: '/cpx/article/get', params: { id } }),
  listArticles: (params?: {
    keyword?: string
    category?: string
    promotionMethod?: CpxPromotionMethod
    limit?: number
  }) => request.get({ url: '/cpx/article/list', params }),
  createPlatformProfile: (data: CpxPlatformProfileSaveVO) =>
    request.post({ url: '/cpx/platform-profile/create', data }),
  updatePlatformProfile: (data: CpxPlatformProfileSaveVO) =>
    request.put({ url: '/cpx/platform-profile/update', data }),
  getPlatformProfile: (id: number) =>
    request.get({ url: '/cpx/platform-profile/get', params: { id } }),
  listPlatformProfiles: () => request.get({ url: '/cpx/platform-profile/list' }),
  listEnabledPlatformProfiles: () => request.get({ url: '/cpx/platform-profile/list-enabled' })
}
