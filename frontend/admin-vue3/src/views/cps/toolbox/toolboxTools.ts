export type ToolKey =
  | 'universal-transfer'
  | 'pdd-transfer'
  | 'meituan-waimai-transfer'
  | 'parse'
  | 'goods-square'
  | 'ownership-check'
  | 'coupon-query'
  | 'copy-editor'
  | 'promo-image'
  | 'cash-gift'

export interface ToolItem {
  key: ToolKey
  label: string
  icon: string
  desc: string
  tag?: string
  disabled?: boolean
  lockedPlatformCode?: string
}

export interface ToolGroup {
  label: string
  children: ToolItem[]
}

export const toolGroups: ToolGroup[] = [
  {
    label: '基础工具',
    children: [
      {
        key: 'universal-transfer',
        label: '万能转链',
        icon: 'ep:connection',
        desc: '批量处理商品链接、商品ID或口令，生成推广链接和返利预估。'
      },
      {
        key: 'pdd-transfer',
        label: '拼多多转链',
        icon: 'ep:shopping-bag',
        desc: '粘贴拼多多商品链接、goodsSign 或多多进宝内容，批量生成推广链接。',
        tag: '支持批量',
        lockedPlatformCode: 'pdd'
      },
      {
        key: 'meituan-waimai-transfer',
        label: '美团外卖转链',
        icon: 'ep:food',
        desc: '粘贴美团外卖活动链接或活动 ID，生成可分发的美团推广链接。',
        lockedPlatformCode: 'meituan'
      },
      {
        key: 'parse',
        label: '口令解析',
        icon: 'ep:search',
        desc: '解析商品链接、商品ID或口令，可一键带入转链。'
      },
      {
        key: 'ownership-check',
        label: '归属检测',
        icon: 'ep:aim',
        desc: '按链接、口令或记录ID检查推广位和会员归因。'
      }
    ]
  },
  {
    label: '选品工具',
    children: [
      {
        key: 'goods-square',
        label: '返利商品广场',
        icon: 'ep:goods',
        desc: '按活动、热词、类目和排序筛选商品，并带入转链。'
      },
      {
        key: 'coupon-query',
        label: '优惠券查询',
        icon: 'ep:ticket',
        desc: '按商品、链接或关键词查询有券商品，并带入转链。'
      }
    ]
  },
  {
    label: '创作工具',
    children: [
      {
        key: 'promo-image',
        label: '推广图制作',
        icon: 'ep:picture',
        desc: '按模板、标签和商品素材生成营销推广图。'
      },
      {
        key: 'copy-editor',
        label: '文案编辑',
        icon: 'ep:edit-pen',
        desc: '右侧推广文案编辑器已常驻展示。',
        disabled: true
      }
    ]
  },
  {
    label: '玩法工具',
    children: [
      {
        key: 'cash-gift',
        label: '淘礼金',
        icon: 'ep:present',
        desc: '生成淘礼金活动模板、补贴预算和上线检查清单。'
      }
    ]
  }
]

export const allTools = toolGroups.flatMap((group) => group.children)

export const promotionEditorTools: ToolKey[] = [
  'universal-transfer',
  'pdd-transfer',
  'meituan-waimai-transfer',
  'promo-image',
  'cash-gift'
]

export const resolveToolKey = (value: unknown): ToolKey => {
  const toolKey = Array.isArray(value) ? value[0] : value
  const matched = allTools.find((tool) => tool.key === toolKey && !tool.disabled)
  return matched?.key || 'universal-transfer'
}
