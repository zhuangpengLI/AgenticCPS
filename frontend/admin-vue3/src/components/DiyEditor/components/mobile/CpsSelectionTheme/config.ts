import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export interface CpsSelectionThemeProperty {
  title: string
  themeId?: number
  limit: number
  columns: number
  showMore: boolean
  style: ComponentStyle
}

export const component = {
  id: 'CpsSelectionTheme',
  name: '选品主题',
  icon: 'ep:goods',
  property: {
    title: '主题好价',
    themeId: undefined,
    limit: 6,
    columns: 2,
    showMore: true,
    style: {
      bgType: 'color',
      bgColor: '#f7f7f8',
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
} as DiyComponent<CpsSelectionThemeProperty>
