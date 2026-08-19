import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export const REBATE_MENU_DESTINATIONS = [
  { key: 'goods', title: '商品查询', icon: 'ep:search', requiresAuth: false },
  { key: 'orders', title: '返利订单', icon: 'ep:tickets', requiresAuth: true },
  { key: 'wallet', title: '返利钱包', icon: 'ep:wallet', requiresAuth: true },
  { key: 'withdraw', title: '申请提现', icon: 'ep:bank-card', requiresAuth: true },
  { key: 'exchange', title: '兑换 Token', icon: 'ep:refresh', requiresAuth: true },
  { key: 'activities', title: '返利活动', icon: 'ep:discount', requiresAuth: false },
  { key: 'selection', title: '主题好价', icon: 'ep:goods', requiresAuth: false },
  { key: 'history', title: '返利足迹', icon: 'ep:clock', requiresAuth: true },
  { key: 'favorites', title: '我的收藏', icon: 'ep:star', requiresAuth: true },
  { key: 'aiot', title: 'AIoT 推荐', icon: 'ep:cpu', requiresAuth: false },
  { key: 'transfer', title: '返利转链', icon: 'ep:connection', requiresAuth: true },
  { key: 'aiChat', title: '返利 AI 对话', icon: 'ep:chat-dot-round', requiresAuth: true }
] as const

export type CpsRebateMenuKey = (typeof REBATE_MENU_DESTINATIONS)[number]['key']

export interface CpsRebateMenuItemProperty {
  key: CpsRebateMenuKey
  title: string
  icon: string
  enabled: boolean
}

export interface CpsRebateMenuProperty {
  title: string
  columns: number
  items: CpsRebateMenuItemProperty[]
  style: ComponentStyle
}

const defaultItems = REBATE_MENU_DESTINATIONS.slice(0, 8).map(({ key, title, icon }) => ({
  key,
  title,
  icon,
  enabled: true
}))

export const component = {
  id: 'CpsRebateMenu',
  name: '返利入口',
  icon: 'ep:grid',
  property: {
    title: '返利服务',
    columns: 4,
    items: defaultItems,
    style: {
      bgType: 'color',
      bgColor: '#ffffff',
      marginLeft: 10,
      marginRight: 10,
      marginBottom: 8,
      paddingTop: 14,
      paddingRight: 10,
      paddingBottom: 14,
      paddingLeft: 10,
      borderRadius: 8
    } as ComponentStyle
  }
} as DiyComponent<CpsRebateMenuProperty>
