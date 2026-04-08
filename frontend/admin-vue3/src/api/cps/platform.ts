import request from '@/config/axios'

// CPS 平台配置 VO
export interface CpsPlatformVO {
  id: number
  platformCode: string
  platformName: string
  platformLogo?: string
  appKey?: string
  apiBaseUrl?: string
  defaultAdzoneId?: string
  platformServiceRate?: number
  sort?: number
  status: number
  extraConfig?: string
  remark?: string
  createTime: Date
}

// CPS 平台配置保存 VO
export interface CpsPlatformSaveVO {
  id?: number
  platformCode: string
  platformName: string
  platformLogo?: string
  appKey: string
  appSecret: string
  apiBaseUrl?: string
  authToken?: string
  defaultAdzoneId: string
  platformServiceRate?: number
  sort?: number
  status: number
  extraConfig?: string
  remark?: string
}

// CPS 平台配置分页请求 VO
export interface CpsPlatformPageReqVO {
  pageNo: number
  pageSize: number
  platformName?: string
  status?: number
}

// ===== 平台配置 API =====

export const CpsPlatformApi = {
  /** 创建平台配置 */
  createPlatform: (data: CpsPlatformSaveVO) =>
    request.post({ url: '/cps/platform/create', data }),

  /** 更新平台配置 */
  updatePlatform: (data: CpsPlatformSaveVO) =>
    request.put({ url: '/cps/platform/update', data }),

  /** 删除平台配置 */
  deletePlatform: (id: number) =>
    request.delete({ url: '/cps/platform/delete', params: { id } }),

  /** 获取平台配置详情 */
  getPlatform: (id: number) =>
    request.get<CpsPlatformVO>({ url: '/cps/platform/get', params: { id } }),

  /** 获取平台配置分页列表 */
  getPlatformPage: (params: CpsPlatformPageReqVO) =>
    request.get<{ list: CpsPlatformVO[]; total: number }>({
      url: '/cps/platform/page',
      params
    }),

  /** 获取已启用的平台列表（供其他页面下拉选择�?*/
  getEnabledPlatformList: () =>
    request.get<CpsPlatformVO[]>({ url: '/cps/platform/list-enabled' })
}
