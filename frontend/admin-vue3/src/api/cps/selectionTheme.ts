import request from '@/config/axios'

export type SelectionThemeStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
export type SelectionThemeItemStatus = 'ENABLED' | 'DISABLED'
export type SelectionThemeSourceType =
  | 'MANUAL'
  | 'AI_RECOMMEND'
  | 'VENDOR_PULL'
  | 'PROMOTION_TEMPLATE'
  | 'AUTO_REFRESH'

export interface CpsSelectionThemeVO {
  id: number
  themeCode: string
  themeName: string
  themeType?: string
  promotionEvent?: string
  platformCodes?: string
  vendorCode?: string
  coverPic?: string
  description?: string
  tags?: string
  ruleJson?: string
  aiPrompt?: string
  aiSummary?: string
  status: SelectionThemeStatus
  goodsSquareVisible?: number
  startTime?: string
  endTime?: string
  refreshStatus?: string
  lastRefreshTime?: string
  refreshMessage?: string
  sort?: number
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface CpsSelectionThemeSaveVO {
  id?: number
  themeCode: string
  themeName: string
  themeType?: string
  promotionEvent?: string
  platformCodes?: string
  vendorCode?: string
  coverPic?: string
  description?: string
  tags?: string
  ruleJson?: string
  aiPrompt?: string
  aiSummary?: string
  status?: SelectionThemeStatus
  goodsSquareVisible?: number
  startTime?: string
  endTime?: string
  sort?: number
  remark?: string
}

export interface CpsSelectionThemePageReqVO {
  pageNo: number
  pageSize: number
  themeCode?: string
  themeName?: string
  themeType?: string
  promotionEvent?: string
  platformCode?: string
  vendorCode?: string
  status?: SelectionThemeStatus | ''
  goodsSquareVisible?: number
}

export interface CpsSelectionThemeStatsVO {
  total: number
  draft: number
  published: number
  offline: number
}

export interface CpsSelectionThemeItemPageReqVO {
  pageNo: number
  pageSize: number
  themeId: number
}

export interface CpsSelectionThemeItemVO {
  id: number
  themeId: number
  platformCode: string
  vendorCode?: string
  goodsId: string
  goodsSign?: string
  title?: string
  mainPic?: string
  originalPrice?: number
  actualPrice?: number
  couponPrice?: number
  commissionRate?: number
  commissionAmount?: number
  monthSales?: number
  shopName?: string
  brandName?: string
  categoryName?: string
  activityTag?: string
  rankTag?: string
  sellingPoint?: string
  recommendScore?: number
  recommendReason?: string
  topFlag?: number
  manualAdjusted?: number
  status: SelectionThemeItemStatus
  sourceType?: SelectionThemeSourceType
  itemLink?: string
  snapshotTime?: string
  sort?: number
  createTime?: string
}

export interface CpsSelectionThemeImportItemVO {
  platformCode: string
  vendorCode?: string
  goodsId: string
  goodsSign?: string
  title?: string
  mainPic?: string
  originalPrice?: number
  actualPrice?: number
  couponPrice?: number
  commissionRate?: number
  commissionAmount?: number
  monthSales?: number
  shopName?: string
  categoryName?: string
  activityTag?: string
  recommendScore?: number
  recommendReason?: string
  itemLink?: string
}

export interface CpsSelectionThemeOperationRespVO {
  themeId?: number
  status: string
  pulledCount?: number
  importedCount?: number
  message?: string
}

export interface CpsSelectionAiReviewVO {
  id: number
  reviewContextId: string
  platformCode: string
  vendorCode?: string
  goodsId: string
  goodsSign?: string
  title?: string
  mainPic?: string
  reviewStatus: 'CONFIRMED' | 'WITHDRAWN'
  reviewerId?: number
  reviewTime?: string
  remark?: string
}

export interface CpsSelectionAiReviewSaveVO {
  reviewContextId: string
  platformCode: string
  vendorCode?: string
  goodsId: string
  goodsSign?: string
  title?: string
  mainPic?: string
  reviewStatus: 'CONFIRMED' | 'WITHDRAWN'
  remark?: string
}

export interface CpsSelectionThemeSyncReqVO {
  vendorCode?: string
  sourceCode?: string
  themeNamePrefix?: string
  themeListUrl?: string
  themeListParamsJson?: string
  goodsListUrl?: string
  goodsListParamsJson?: string
  keyword?: string
  maxPages?: number
  pageSize?: number
  syncGoods?: boolean
  goodsPullCount?: number
}

export interface CpsSelectionThemeTemplateVO {
  templateCode: string
  themeName: string
  promotionEvent?: string
  description?: string
  tags?: string
  ruleJson?: string
  aiPrompt?: string
}

/**
 * AI 工作台保存的筛选条件复用选品主题的草稿存储，避免把营销分析快照混入订单/返利事实表。
 */
export interface CpsAiSavedSelectionFilterVO extends CpsSelectionThemeVO {
  themeType: 'AI_SAVED_FILTER'
}

export const SELECTION_THEME_STATUS_OPTIONS = [
  { label: '草稿', value: 'DRAFT', type: 'info' },
  { label: '已发布', value: 'PUBLISHED', type: 'success' },
  { label: '已下线', value: 'OFFLINE', type: 'warning' }
] as const

export const SELECTION_THEME_ITEM_STATUS_OPTIONS = [
  { label: '启用', value: 'ENABLED', type: 'success' },
  { label: '停用', value: 'DISABLED', type: 'info' }
] as const

export const SELECTION_SOURCE_OPTIONS = [
  { label: '人工添加', value: 'MANUAL' },
  { label: 'AI 推荐', value: 'AI_RECOMMEND' },
  { label: '第三方拉取', value: 'VENDOR_PULL' },
  { label: '大促模板', value: 'PROMOTION_TEMPLATE' }
] as const

export const CpsSelectionThemeApi = {
  getThemePage: (params: CpsSelectionThemePageReqVO) =>
    request.get<{ list: CpsSelectionThemeVO[]; total: number }>({
      url: '/cps/selection-theme/page',
      params
    }),

  getThemeStats: (params: CpsSelectionThemePageReqVO) =>
    request.get<CpsSelectionThemeStatsVO>({
      url: '/cps/selection-theme/stats',
      params
    }),

  getTheme: (id: number) =>
    request.get<CpsSelectionThemeVO>({ url: '/cps/selection-theme/get', params: { id } }),

  getAiSavedFilters: (params: { pageNo?: number; pageSize?: number; themeName?: string } = {}) =>
    request.get<{ list: CpsAiSavedSelectionFilterVO[]; total: number }>({
      url: '/cps/selection-theme/page',
      params: { pageNo: 1, pageSize: 50, themeType: 'AI_SAVED_FILTER', status: 'DRAFT', ...params }
    }),

  createAiSavedFilter: (data: {
    themeName: string
    toolIntent: string
    prompt: string
    mode: 'SELECTION' | 'ORDER'
    structuredRule?: {
      keywords?: string[]
      platforms?: string[]
      priceLowerLimit?: number
      priceUpperLimit?: number
      minCommissionRate?: number
      minMonthSales?: number
      onlyCoupon?: boolean
      pullCount?: number
      autoRefresh?: boolean
    }
  }) =>
    request.post<number>({
      url: '/cps/selection-theme/create',
      data: {
        themeCode: `AI_FILTER_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
        themeName: data.themeName,
        themeType: 'AI_SAVED_FILTER',
        platformCodes: 'taobao',
        vendorCode: 'dataoke',
        description: 'AI 选品工作台保存的筛选条件',
        tags: 'AI选品,保存条件',
        ruleJson: JSON.stringify({
          prompt: data.prompt,
          toolIntent: data.toolIntent,
          mode: data.mode,
          ...data.structuredRule
        }),
        aiPrompt: data.prompt,
        status: 'DRAFT',
        goodsSquareVisible: 0,
        remark: '仅保存分析条件，不代表已确认商品或已生成推广链接'
      }
    }),

  refreshAiSavedFilter: (id: number) =>
    request.post<CpsSelectionThemeOperationRespVO>({
      url: '/cps/selection-theme/ai-saved-filters/refresh',
      params: { id }
    }),

  getAiReviews: (reviewContextId: string) =>
    request.get<CpsSelectionAiReviewVO[]>({
      url: '/cps/selection-theme/ai-reviews/list',
      params: { reviewContextId }
    }),

  saveAiReview: (data: CpsSelectionAiReviewSaveVO) =>
    request.put<number>({ url: '/cps/selection-theme/ai-reviews', data }),

  createTheme: (data: CpsSelectionThemeSaveVO) =>
    request.post<number>({ url: '/cps/selection-theme/create', data }),

  updateTheme: (data: CpsSelectionThemeSaveVO) =>
    request.put<boolean>({ url: '/cps/selection-theme/update', data }),

  deleteTheme: (id: number) =>
    request.delete<boolean>({ url: '/cps/selection-theme/delete', params: { id } }),

  deleteThemeList: (ids: number[]) =>
    request.delete<boolean>({
      url: '/cps/selection-theme/delete-list',
      params: { ids: ids.join(',') }
    }),

  publishTheme: (id: number) =>
    request.put<boolean>({ url: '/cps/selection-theme/publish', params: { id } }),

  offlineTheme: (id: number) =>
    request.put<boolean>({ url: '/cps/selection-theme/offline', params: { id } }),

  aiRecommend: (data: { themeId: number; objective?: string; ruleJson?: string }) =>
    request.post<CpsSelectionThemeOperationRespVO>({
      url: '/cps/selection-theme/ai-recommend',
      data
    }),

  vendorPull: (data: { themeId: number; ruleJson?: string }) =>
    request.post<CpsSelectionThemeOperationRespVO>({
      url: '/cps/selection-theme/vendor-pull',
      data
    }),

  syncDataokeThemes: (data: CpsSelectionThemeSyncReqVO) =>
    request.post<CpsSelectionThemeOperationRespVO>({
      url: '/cps/selection-theme/dataoke-theme-sync',
      data
    }),

  syncVendorThemes: (data: CpsSelectionThemeSyncReqVO) =>
    request.post<CpsSelectionThemeOperationRespVO>({
      url: '/cps/selection-theme/vendor-theme-sync',
      data
    }),

  listItems: (themeId: number) =>
    request.get<CpsSelectionThemeItemVO[]>({
      url: '/cps/selection-theme/items/list',
      params: { themeId }
    }),

  getItemPage: (params: CpsSelectionThemeItemPageReqVO) =>
    request.get<{ list: CpsSelectionThemeItemVO[]; total: number }>({
      url: '/cps/selection-theme/items/page',
      params
    }),

  importItems: (data: {
    themeId: number
    sourceType?: SelectionThemeSourceType
    items: CpsSelectionThemeImportItemVO[]
  }) => request.post<number>({ url: '/cps/selection-theme/items/import', data }),

  updateItemSort: (data: {
    themeId: number
    items: { id: number; sort?: number; topFlag?: number }[]
  }) => request.put<boolean>({ url: '/cps/selection-theme/items/sort', data }),

  updateItemStatus: (data: { ids: number[]; status: SelectionThemeItemStatus }) =>
    request.put<boolean>({ url: '/cps/selection-theme/items/status', data }),

  deleteItem: (id: number) =>
    request.delete<boolean>({ url: '/cps/selection-theme/items/delete', params: { id } }),

  listTemplates: () =>
    request.get<CpsSelectionThemeTemplateVO[]>({ url: '/cps/selection-theme/templates' }),

  createFromTemplate: (data: { templateCode: string; themeCode?: string }) =>
    request.post<number>({ url: '/cps/selection-theme/templates/create', data })
}
