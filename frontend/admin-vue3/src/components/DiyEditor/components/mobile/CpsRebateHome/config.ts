import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

/** 返利首页属性 */
export interface CpsRebateHomeProperty {
  kicker: string
  title: string
  description: string
  searchPlaceholder: string
  searchButtonText: string
  showFeatured: boolean
  style: ComponentStyle
}

export const component = {
  id: 'CpsRebateHome',
  name: '返利首页',
  icon: 'ep:discount',
  property: {
    kicker: 'AgenticCPS · 今日高返',
    title: '最高 ¥186.50',
    description: '领券下单，订单结算后返利到账',
    searchPlaceholder: '搜商品或粘贴链接/口令',
    searchButtonText: '查优惠',
    showFeatured: true,
    style: {
      bgType: 'color',
      bgColor: '#f7f7f8',
      marginBottom: 0,
      paddingTop: 0,
      paddingRight: 0,
      paddingBottom: 0,
      paddingLeft: 0,
      borderRadius: 0
    } as ComponentStyle
  }
} as DiyComponent<CpsRebateHomeProperty>
