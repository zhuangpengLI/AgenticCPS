import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export interface CpsRebateGuideProperty {
  title: string
  steps: string[]
  notice: string
  style: ComponentStyle
}

export const component = {
  id: 'CpsRebateGuide',
  name: '返利说明',
  icon: 'ep:document',
  property: {
    title: '如何获得返利',
    steps: ['搜索商品或粘贴商品链接', '查看券后价和预估返利', '领券购买，结算后返利到账'],
    notice: '预估返利仅供参考，实际金额以订单结算结果为准。',
    style: {
      bgType: 'color',
      bgColor: '#ffffff',
      marginLeft: 10,
      marginRight: 10,
      marginBottom: 8,
      paddingTop: 14,
      paddingRight: 14,
      paddingBottom: 14,
      paddingLeft: 14,
      borderRadius: 8
    } as ComponentStyle
  }
} as DiyComponent<CpsRebateGuideProperty>
