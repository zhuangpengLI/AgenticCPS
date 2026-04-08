import request from '@/config/axios'

// CPS 订单 VO
export interface CpsOrderVO {
  id: number
  platformCode: string
  platformOrderId: string
  parentOrderId?: string
  memberId: number
  memberNickname?: string
  itemId?: string
  itemTitle?: string
  itemPic?: string
  itemPrice?: number
  finalPrice?: number
  couponAmount?: number
  commissionRate?: number
  commissionAmount?: number
  estimateRebate?: number
  realRebate?: number
  adzoneId?: string
  externalInfo?: string
  orderStatus: string
  syncTime?: Date
  settleTime?: Date
  rebateTime?: Date
  refundTime?: Date
  confirmReceiptTime?: Date
  rebateFreezeStatus?: string
  planUnfreezeTime?: Date
  createTime: Date
}

// CPS 订单分页请求 VO
export interface CpsOrderPageReqVO {
  pageNo: number
  pageSize: number
  platformCode?: string
  memberId?: number
  orderStatus?: string
  itemTitle?: string
  platformOrderId?: string
  createTime?: [Date, Date]
}

// 查询订单分页列表
export const getCpsOrderPage = async (params: CpsOrderPageReqVO) => {
  return await request.get({ url: '/cps/order/page', params })
}

// 查询订单详情
export const getCpsOrder = async (id: number) => {
  return await request.get({ url: '/cps/order/get', params: { id } })
}

// 手动触发订单同步
export const syncCpsOrders = async (platformCode: string, hours = 2) => {
  return await request.post({
    url: '/cps/order/sync',
    params: { platformCode, hours }
  })
}
