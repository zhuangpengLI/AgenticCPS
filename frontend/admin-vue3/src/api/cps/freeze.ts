import request from '@/config/axios'

// 冻结配置 VO
export interface CpsFreezeConfigVO {
  id: number
  platformCode?: string
  unfreezeDays: number
  status: number
  remark?: string
  createTime: Date
}

// 冻结配置保存 VO
export interface CpsFreezeConfigSaveVO {
  id?: number
  platformCode?: string
  unfreezeDays: number
  status: number
  remark?: string
}

// 冻结配置分页请求 VO
export interface CpsFreezeConfigPageReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  status?: number
}

// 冻结记录 VO
export interface CpsFreezeRecordVO {
  id: number
  memberId: number
  orderId: number
  platformOrderId?: string
  freezeAmount: number
  unfreezeTime?: Date
  actualUnfreezeTime?: Date
  status: string
  createTime: Date
}

// 冻结记录分页请求 VO
export interface CpsFreezeRecordPageReqVO {
  pageNo: number
  pageSize: number
  memberId?: number
  status?: string
  createTime?: [Date, Date]
}

// ===== 冻结配置 API =====

export const createCpsFreezeConfig = async (data: CpsFreezeConfigSaveVO) => {
  return await request.post({ url: '/cps/freeze/config/create', data })
}

export const updateCpsFreezeConfig = async (data: CpsFreezeConfigSaveVO) => {
  return await request.put({ url: '/cps/freeze/config/update', data })
}

export const deleteCpsFreezeConfig = async (id: number) => {
  return await request.delete({ url: '/cps/freeze/config/delete', params: { id } })
}

export const getCpsFreezeConfigPage = async (params: CpsFreezeConfigPageReqVO) => {
  return await request.get({ url: '/cps/freeze/config/page', params })
}

// ===== 冻结记录 API =====

export const getCpsFreezeRecordPage = async (params: CpsFreezeRecordPageReqVO) => {
  return await request.get({ url: '/cps/freeze/record/page', params })
}

export const manualUnfreeze = async (id: number) => {
  return await request.put({ url: '/cps/freeze/record/manual-unfreeze', params: { id } })
}
