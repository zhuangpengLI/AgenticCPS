import request from '@/config/axios'

export interface CpsDashboardVO {
  todayOrderCount: number
  todayCommission: number
  todayRebate: number
  todayProfit: number
  todayActiveMembers: number
  yesterdayOrderCount: number
  yesterdayCommission: number
  yesterdayRebate: number
  yesterdayProfit: number
  totalPendingCommission: number
  totalSettledCommission: number
}

export interface CpsTrendVO {
  dates: string[]
  orderCounts: number[]
  commissions: number[]
  rebates: number[]
  profits: number[]
}

export interface CpsPlatformSummaryVO {
  platformCode: string
  platformName: string
  orderCount: number
  commissionAmount: number
  rebateAmount: number
  profitAmount: number
}

export const CpsStatisticsApi = {
  /** 运营数据看板 */
  getDashboard: () =>
    request.get<CpsDashboardVO>({ url: '/admin-api/cps/statistics/dashboard' }),

  /** 趋势折线图数据 */
  getTrend: (params: { startDate: string; endDate: string; platformCode?: string }) =>
    request.get<CpsTrendVO>({ url: '/admin-api/cps/statistics/trend', params }),

  /** 平台占比饼图数据 */
  getPlatformSummary: (params: { startDate: string; endDate: string }) =>
    request.get<CpsPlatformSummaryVO[]>({
      url: '/admin-api/cps/statistics/platform-summary',
      params
    })
}
