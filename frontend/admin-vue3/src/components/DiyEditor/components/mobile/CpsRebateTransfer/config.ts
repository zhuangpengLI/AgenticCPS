import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export interface CpsRebateTransferProperty {
  title: string
  description: string
  placeholder: string
  buttonText: string
  platformCode: string
  style: ComponentStyle
}

export const component = {
  id: 'CpsRebateTransfer',
  name: '返利转链',
  icon: 'ep:connection',
  property: {
    title: '一键转链',
    description: '粘贴商品链接或口令，生成专属返利链接',
    placeholder: '粘贴商品链接、商品 ID 或口令',
    buttonText: '立即转链',
    platformCode: 'taobao',
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
} as DiyComponent<CpsRebateTransferProperty>
