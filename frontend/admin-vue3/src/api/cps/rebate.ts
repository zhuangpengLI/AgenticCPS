import request from '@/config/axios'

// 返利配置 VO
export interface CpsRebateConfigVO {
  id: number
  memberLevelId?: number
  platformCode?: string
  rebateRate: number
  maxRebateAmount?: number
  minRebateAmount?: number
  status: number
  priority: number
  createTime: Date
  updateTime: Date
}

// 返利配置保存 VO
export interface CpsRebateConfigSaveVO {
  id?: number
  memberLevelId?: number
  platformCode?: string
  rebateRate: number
  maxRebateAmount?: number
  minRebateAmount?: number
  status: number
  priority?: number
}

// 返利配置分页请求
export interface CpsRebateConfigPageReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  status?: number
}

// 返利记录 VO
export interface CpsRebateRecordVO {
  id: number
  memberId: number
  orderId: number
  platformCode: string
  platformOrderId: string
  itemId?: string
  itemTitle?: string
  orderAmount: number
  commissionAmount: number
  rebateRate: number
  rebateAmount: number
  rebateType: string
  rebateStatus: string
  precedingRebateId?: number
  remark?: string
  createTime: Date
  updateTime: Date
}

// 返利记录分页请求
export interface CpsRebateRecordPageReqVO {
  pageNo: number
  pageSize: number
  memberId?: number
  platformCode?: string
  rebateType?: string
  rebateStatus?: string
  createTime?: [Date, Date]
}

// ===== 返利配置 API =====

// 创建返利配置
export const createCpsRebateConfig = async (data: CpsRebateConfigSaveVO) => {
  return await request.post({ url: '/cps/rebate-config/create', data })
}

// 更新返利配置
export const updateCpsRebateConfig = async (data: CpsRebateConfigSaveVO) => {
  return await request.put({ url: '/cps/rebate-config/update', data })
}

// 删除返利配置
export const deleteCpsRebateConfig = async (id: number) => {
  return await request.delete({ url: '/cps/rebate-config/delete', params: { id } })
}

// 获取返利配置详情
export const getCpsRebateConfig = async (id: number) => {
  return await request.get({ url: '/cps/rebate-config/get', params: { id } })
}

// 获取返利配置分页列表
export const getCpsRebateConfigPage = async (params: CpsRebateConfigPageReqVO) => {
  return await request.get({ url: '/cps/rebate-config/page', params })
}

// 获取所有启用的返利配置列表
export const getEnabledCpsRebateConfigList = async () => {
  return await request.get({ url: '/cps/rebate-config/list-enabled' })
}

// ===== 返利记录 API =====

// 获取返利记录分页列表
export const getCpsRebateRecordPage = async (params: CpsRebateRecordPageReqVO) => {
  return await request.get({ url: '/cps/rebate-record/page', params })
}

// 获取返利记录详情
export const getCpsRebateRecord = async (id: number) => {
  return await request.get({ url: '/cps/rebate-record/get', params: { id } })
}

// 退款回扣（逆向扣回返利）
export const reverseCpsRebate = async (orderId: number) => {
  return await request.post({ url: '/cps/rebate-record/reverse', params: { orderId } })
}
