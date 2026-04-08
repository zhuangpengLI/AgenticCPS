import request from '@/config/axios'

// CPS 推广�?VO
export interface CpsAdzoneVO {
  id: number
  platformCode: string
  adzoneId: string
  adzoneName?: string
  adzoneType?: string
  relationType?: string
  relationId?: number
  isDefault: number
  status: number
  createTime: Date
}

// CPS 推广位保�?VO
export interface CpsAdzoneSaveVO {
  id?: number
  platformCode: string
  adzoneId: string
  adzoneName?: string
  adzoneType?: string
  relationType?: string
  relationId?: number
  isDefault?: number
  status: number
}

// CPS 推广位分页请�?VO
export interface CpsAdzonePageReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  adzoneName?: string
  status?: number
}

// ===== 推广�?API =====

export const CpsAdzoneApi = {
  /** 创建推广�?*/
  createAdzone: (data: CpsAdzoneSaveVO) =>
    request.post({ url: '/cps/adzone/create', data }),

  /** 更新推广�?*/
  updateAdzone: (data: CpsAdzoneSaveVO) =>
    request.put({ url: '/cps/adzone/update', data }),

  /** 删除推广�?*/
  deleteAdzone: (id: number) =>
    request.delete({ url: '/cps/adzone/delete', params: { id } }),

  /** 获取推广位详�?*/
  getAdzone: (id: number) =>
    request.get<CpsAdzoneVO>({ url: '/cps/adzone/get', params: { id } }),

  /** 获取推广位分页列�?*/
  getAdzonePage: (params: CpsAdzonePageReqVO) =>
    request.get<{ list: CpsAdzoneVO[]; total: number }>({
      url: '/cps/adzone/page',
      params
    }),

  /** 按平台获取推广位列表（不分页�?*/
  getAdzoneListByPlatform: (platformCode: string) =>
    request.get<CpsAdzoneVO[]>({
      url: '/cps/adzone/list-by-platform',
      params: { platformCode }
    })
}
