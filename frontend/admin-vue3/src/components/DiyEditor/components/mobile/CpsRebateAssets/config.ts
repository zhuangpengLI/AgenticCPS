import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export interface CpsRebateAssetsProperty {
  title: string
  showAvailable: boolean
  showFrozen: boolean
  showTotal: boolean
  guestText: string
  style: ComponentStyle
}

export const component = {
  id: 'CpsRebateAssets',
  name: '返利资产',
  icon: 'ep:wallet',
  property: {
    title: '我的返利',
    showAvailable: true,
    showFrozen: true,
    showTotal: true,
    guestText: '登录后查看返利资产',
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
} as DiyComponent<CpsRebateAssetsProperty>
