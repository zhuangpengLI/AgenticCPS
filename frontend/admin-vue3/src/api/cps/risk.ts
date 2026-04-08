import request from '@/config/axios'

// 风控规则 VO
export interface CpsRiskRuleVO {
  id: number
  ruleType: string
  targetType: string
  targetValue?: string
  limitCount?: number
  status: number
  remark?: string
  createTime: Date
}

// 风控规则保存 VO
export interface CpsRiskRuleSaveVO {
  id?: number
  ruleType: string
  targetType: string
  targetValue?: string
  limitCount?: number
  status: number
  remark?: string
}

// 风控规则分页请求 VO
export interface CpsRiskRulePageReqVO {
  pageNo: number
  pageSize: number
  ruleType?: string
  status?: number
}

// ===== 风控规则 API =====

export const createCpsRiskRule = async (data: CpsRiskRuleSaveVO) => {
  return await request.post({ url: '/admin-api/cps/risk/rule/create', data })
}

export const updateCpsRiskRule = async (data: CpsRiskRuleSaveVO) => {
  return await request.put({ url: '/admin-api/cps/risk/rule/update', data })
}

export const deleteCpsRiskRule = async (id: number) => {
  return await request.delete({ url: '/admin-api/cps/risk/rule/delete', params: { id } })
}

export const getCpsRiskRulePage = async (params: CpsRiskRulePageReqVO) => {
  return await request.get({ url: '/admin-api/cps/risk/rule/page', params })
}
