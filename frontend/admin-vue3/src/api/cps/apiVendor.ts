import request from '@/config/axios'

// ===== 供应商类型/平台编码 常量 =====

/** 供应商类型选项 */
export const VENDOR_TYPE_OPTIONS = [
  { label: '聚合平台', value: 'aggregator' },
  { label: '官方API', value: 'official' }
]

/** 供应商编码选项 */
export const VENDOR_CODE_OPTIONS = [
  { label: '大淘客', value: 'dataoke', type: 'aggregator' },
  { label: '好单库', value: 'haodanku', type: 'aggregator' },
  { label: '喵有卷', value: 'miaoyouquan', type: 'aggregator' },
  { label: '实惠猪', value: 'shihuizhu', type: 'aggregator' },
  { label: '官方API', value: 'official', type: 'official' }
]

/** 电商平台选项 */
export const PLATFORM_CODE_OPTIONS = [
  { label: '淘宝联盟', value: 'taobao' },
  { label: '京东联盟', value: 'jd' },
  { label: '拼多多联盟', value: 'pdd' },
  { label: '抖音联盟', value: 'douyin' },
  { label: '唯品会联盟', value: 'vip' },
  { label: '美团联盟', value: 'meituan' }
]

// ===== VO 类型定义 =====

/** CPS API供应商 Response VO */
export interface CpsApiVendorVO {
  id: number
  vendorCode: string
  vendorName: string
  vendorType: string
  platformCode: string
  appKey?: string
  apiBaseUrl?: string
  authToken?: string
  defaultAdzoneId?: string
  extraConfig?: string
  priority?: number
  status: number
  remark?: string
  createTime: Date
}

/** CPS API供应商 Save Request VO */
export interface CpsApiVendorSaveVO {
  id?: number
  vendorCode: string
  vendorName: string
  vendorType: string
  platformCode: string
  appKey: string
  appSecret: string
  apiBaseUrl: string
  authToken?: string
  defaultAdzoneId?: string
  extraConfig?: string
  priority?: number
  status: number
  remark?: string
}

/** CPS API供应商 分页请求 VO */
export interface CpsApiVendorPageReqVO {
  pageNo: number
  pageSize: number
  vendorCode?: string
  vendorName?: string
  vendorType?: string
  platformCode?: string
  status?: number
}

// ===== API 接口 =====

export const CpsApiVendorApi = {
  /** 创建供应商配置 */
  createVendor: (data: CpsApiVendorSaveVO) =>
    request.post({ url: '/cps/api-vendor/create', data }),

  /** 更新供应商配置 */
  updateVendor: (data: CpsApiVendorSaveVO) =>
    request.put({ url: '/cps/api-vendor/update', data }),

  /** 删除供应商配置 */
  deleteVendor: (id: number) =>
    request.delete({ url: '/cps/api-vendor/delete', params: { id } }),

  /** 获取供应商配置详情 */
  getVendor: (id: number) =>
    request.get<CpsApiVendorVO>({ url: '/cps/api-vendor/get', params: { id } }),

  /** 获取供应商配置分页列表 */
  getVendorPage: (params: CpsApiVendorPageReqVO) =>
    request.get<{ list: CpsApiVendorVO[]; total: number }>({
      url: '/cps/api-vendor/page',
      params
    }),

  /** 获取已启用的供应商列表 */
  getEnabledVendorList: () =>
    request.get<CpsApiVendorVO[]>({ url: '/cps/api-vendor/list-enabled' }),

  /** 按平台编码获取供应商列表 */
  getVendorListByPlatform: (platformCode: string) =>
    request.get<CpsApiVendorVO[]>({
      url: '/cps/api-vendor/list-by-platform',
      params: { platformCode }
    })
}
