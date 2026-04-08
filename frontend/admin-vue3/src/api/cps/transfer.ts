import request from '@/config/axios'

// 转链记录 VO
export interface CpsTransferRecordVO {
  id: number
  memberId: number
  platformCode: string
  itemId?: string
  itemTitle?: string
  originalContent?: string
  promotionUrl?: string
  taoCommand?: string
  platformOrderId?: string
  adzoneId?: string
  expireTime?: Date
  status: number
  createTime: Date
}

// 转链记录分页请求 VO
export interface CpsTransferRecordPageReqVO {
  pageNo: number
  pageSize: number
  memberId?: number
  platformCode?: string
  itemTitle?: string
  status?: number
  createTime?: [Date, Date]
}

// 获取转链记录分页列表
export const getCpsTransferRecordPage = async (params: CpsTransferRecordPageReqVO) => {
  return await request.get({ url: '/cps/transfer-record/page', params })
}
