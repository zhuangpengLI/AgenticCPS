import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export interface CpsRebateAiChatProperty {
  title: string
  description: string
  placeholder: string
  buttonText: string
  roleId?: number
  style: ComponentStyle
}

export const component = {
  id: 'CpsRebateAiChat',
  name: '返利 AI 对话',
  icon: 'ep:chat-dot-round',
  property: {
    title: '返利 AI 助手',
    description: '问价格、优惠和返利，帮你快速找到合适商品',
    placeholder: '例如：帮我找一款高返蓝牙耳机',
    buttonText: '开始对话',
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
} as DiyComponent<CpsRebateAiChatProperty>
