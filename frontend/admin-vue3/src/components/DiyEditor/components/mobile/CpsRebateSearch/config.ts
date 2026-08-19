import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export interface CpsRebateSearchProperty {
  kicker: string
  title: string
  description: string
  placeholder: string
  buttonText: string
  style: ComponentStyle
}

export const component = {
  id: 'CpsRebateSearch',
  name: '返利搜索',
  icon: 'ep:search',
  property: {
    kicker: 'AgenticCPS · 今日高返',
    title: '先查优惠，再下单',
    description: '搜索商品或粘贴链接/口令，查看券后价与预估返利',
    placeholder: '搜商品或粘贴链接/口令',
    buttonText: '查优惠',
    style: {
      bgType: 'color',
      bgColor: '#f7f7f8',
      marginBottom: 8,
      paddingTop: 12,
      paddingRight: 10,
      paddingBottom: 12,
      paddingLeft: 10,
      borderRadius: 0
    } as ComponentStyle
  }
} as DiyComponent<CpsRebateSearchProperty>
