import { ComponentStyle, DiyComponent } from '@/components/DiyEditor/util'

export type CpsRebateActivitiesLayout = 'grid' | 'list'

export interface CpsRebateActivitiesProperty {
  title: string
  activityIds: number[]
  layout: CpsRebateActivitiesLayout
  limit: number
  showMore: boolean
  style: ComponentStyle
}

export const component = {
  id: 'CpsRebateActivities',
  name: '返利活动',
  icon: 'ep:discount',
  property: {
    title: '热门返利活动',
    activityIds: [],
    layout: 'grid',
    limit: 6,
    showMore: true,
    style: {
      bgType: 'color',
      bgColor: '#ffffff',
      marginLeft: 10,
      marginRight: 10,
      marginBottom: 8,
      paddingTop: 14,
      paddingRight: 12,
      paddingBottom: 14,
      paddingLeft: 12,
      borderRadius: 8
    } as ComponentStyle
  }
} as DiyComponent<CpsRebateActivitiesProperty>
