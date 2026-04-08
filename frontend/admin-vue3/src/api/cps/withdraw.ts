import request from '@/config/axios'

// CPS 提现 VO
export interface CpsWithdrawVO {
  id: number
  memberId: number
  memberNickname?: string
  withdrawNo: string
  withdrawType: string
  withdrawAccount: string
  withdrawAccountName?: string
  amount: number
  feeAmount?: number
  actualAmount?: number
  status: string
  auditUserId?: number
  auditTime?: Date
  reviewNote?: string
  transactionNo?: string
  transferStatus?: string
  transferTime?: Date
  transferError?: string
  createTime: Date
}

// CPS 提现分页请求 VO
export interface CpsWithdrawPageReqVO {
  pageNo: number
  pageSize: number
  memberId?: number
  status?: string
  withdrawType?: string
  createTime?: [Date, Date]
}

// ===== 提现管理 API =====

export const CpsWithdrawApi = {
  /** 获取提现分页列表 */
  getWithdrawPage: (params: CpsWithdrawPageReqVO) =>
    request.get<{ list: CpsWithdrawVO[]; total: number }>({
      url: '/cps/withdraw/page',
      params
    }),

  /** 获取提现详情 */
  getWithdraw: (id: number) =>
    request.get<CpsWithdrawVO>({ url: '/cps/withdraw/get', params: { id } }),

  /** 审核通过 */
  approveWithdraw: (id: number, reviewNote?: string) =>
    request.put({ url: '/cps/withdraw/approve', params: { id, reviewNote } }),

  /** 驳回提现 */
  rejectWithdraw: (id: number, reviewNote: string) =>
    request.put({ url: '/cps/withdraw/reject', params: { id, reviewNote } })
}
