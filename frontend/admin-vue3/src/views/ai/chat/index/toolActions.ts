import type {
  ToolActionFieldVO,
  ToolActionVO
} from '@/api/ai/chat/toolAction'
import type { ToolExecutionVO } from '@/api/ai/chat/message'

export const COMMON_TOOL_INTENTS = [
  'SEARCH_GOODS',
  'COMPARE_PRICES',
  'QUERY_ORDERS',
  'GET_REBATE_SUMMARY',
  'GET_REBATE_BALANCE',
  'LIST_SELECTION_THEMES'
] as const

export interface RecommendedPrompt {
  prompt: string
  toolIntent: (typeof COMMON_TOOL_INTENTS)[number]
}

export const RECOMMENDED_PROMPTS: RecommendedPrompt[] = [
  {
    prompt: '我想买扫地机器人，预算 2500 元，结合近期口碑、优惠和全平台到手价推荐 3 款。',
    toolIntent: 'SEARCH_GOODS'
  },
  {
    prompt: '帮我在全平台找无线降噪耳机，预算 500 元以内，返回 5 件，按券后价从低到高排序。',
    toolIntent: 'SEARCH_GOODS'
  },
  {
    prompt: '比较 iPhone 16 256G 在各平台的到手价和返利，每个平台取前 5 条，按预计净价排序。',
    toolIntent: 'COMPARE_PRICES'
  },
  {
    prompt: '查询当前测试会员近 30 天的京东已付款订单，最多返回 20 条。',
    toolIntent: 'QUERY_ORDERS'
  },
  {
    prompt: '汇总当前测试会员的返利账户，并列出最近 5 条返利记录。',
    toolIntent: 'GET_REBATE_SUMMARY'
  },
  {
    prompt: '查询当前测试会员可兑换的返利余额，并说明冻结余额。',
    toolIntent: 'GET_REBATE_BALANCE'
  },
  {
    prompt: '列出当前已发布的选品主题，优先展示近期大促主题。',
    toolIntent: 'LIST_SELECTION_THEMES'
  }
]

const INTERNAL_TOOL_NAME_RE = /\b(?:cps|cpx)_[a-z0-9_]+\b/gi

/** 后端本应只返回业务文案；这里再做一次防御，避免技术名称进入用户界面。 */
export const toFriendlyToolText = (value?: string, fallback = '正在处理业务请求') => {
  const text = value?.replace(INTERNAL_TOOL_NAME_RE, '业务能力').trim()
  return text || fallback
}

export const sortToolActions = (actions: ToolActionVO[]) => {
  const index = new Map<string, number>(COMMON_TOOL_INTENTS.map((intent, i) => [intent, i]))
  return [...actions].sort((left, right) => {
    const leftIndex = index.get(left.intent) ?? Number.MAX_SAFE_INTEGER
    const rightIndex = index.get(right.intent) ?? Number.MAX_SAFE_INTEGER
    return leftIndex - rightIndex || left.group.localeCompare(right.group) || left.label.localeCompare(right.label)
  })
}

export const getDefaultFieldValues = (fields: ToolActionFieldVO[]) =>
  Object.fromEntries(fields.map((field) => [field.name, field.defaultValue ?? '']))

const formatTemplateValue = (value: unknown) => {
  if (Array.isArray(value)) {
    return value.filter(Boolean).join(' 至 ')
  }
  return value === undefined || value === null ? '' : String(value).trim()
}

export const buildToolPrompt = (action: ToolActionVO, values: Record<string, unknown>) => {
  const template = action.promptTemplate || `请帮我${action.label}`
  const rendered = action.fields.reduce((result, field) => {
    const value = formatTemplateValue(values[field.name])
    return result
      .replaceAll(`{{${field.name}}}`, value)
      .replaceAll(`{${field.name}}`, value)
  }, template)
  return rendered.replace(/\{\{?[a-zA-Z0-9_]+\}?\}/g, '').replace(/\s+/g, ' ').trim()
}

export const upsertToolExecution = (
  current: ToolExecutionVO[],
  next: ToolExecutionVO
): ToolExecutionVO[] => {
  const safeNext = {
    ...next,
    label: toFriendlyToolText(next.label, '业务处理'),
    message: toFriendlyToolText(next.message)
  }
  const index = current.findIndex((item) => item.executionId === next.executionId)
  if (index < 0) {
    return [...current, safeNext]
  }
  const result = [...current]
  result[index] = { ...result[index], ...safeNext }
  return result
}

export const createIntentRequestId = () => {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}
