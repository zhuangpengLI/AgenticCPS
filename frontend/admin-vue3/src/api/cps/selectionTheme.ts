import request from '@/config/axios'

export type SelectionThemeStatus = 'DRAFT' | 'PUBLISHED' | 'OFFLINE'
export type SelectionThemeItemStatus = 'ENABLED' | 'DISABLED'
export type SelectionThemeSourceType =
  | 'MANUAL'
  | 'AI_RECOMMEND'
  | 'VENDOR_PULL'
  | 'PROMOTION_TEMPLATE'

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
  startTime?: string
  endTime?: string
  refreshStatus?: string
  lastRefreshTime?: string
  sort?: number
  remark?: string
  createTime?: string
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

export interface CpsSelectionThemeSyncReqVO {
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

  getTheme: (id: number) =>
    request.get<CpsSelectionThemeVO>({ url: '/cps/selection-theme/get', params: { id } }),

  createTheme: (data: CpsSelectionThemeSaveVO) =>
    request.post<number>({ url: '/cps/selection-theme/create', data }),

  updateTheme: (data: CpsSelectionThemeSaveVO) =>
    request.put<boolean>({ url: '/cps/selection-theme/update', data }),

  deleteTheme: (id: number) =>
    request.delete<boolean>({ url: '/cps/selection-theme/delete', params: { id } }),

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

  listItems: (themeId: number) =>
    request.get<CpsSelectionThemeItemVO[]>({
      url: '/cps/selection-theme/items/list',
      params: { themeId }
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
