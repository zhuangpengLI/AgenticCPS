import { expect, test } from '@playwright/test'
import type { ToolActionVO } from '../src/api/ai/chat/toolAction'
import {
  buildToolPrompt,
  COMMON_TOOL_INTENTS,
  RECOMMENDED_PROMPTS,
  sortToolActions,
  toFriendlyToolText,
  upsertToolExecution
} from '../src/views/ai/chat/index/toolActions'

const action = (intent: string, overrides: Partial<ToolActionVO> = {}): ToolActionVO => ({
  intent,
  label: intent,
  group: '测试分组',
  riskLevel: 'READ_ONLY',
  interactionType: 'DIRECT',
  runningMessage: '正在处理',
  successMessage: '处理完成',
  promptTemplate: '请执行',
  fields: [],
  ...overrides
})

test('六条推荐问题只携带稳定业务意图且文本参数充分', () => {
  expect(RECOMMENDED_PROMPTS).toEqual([
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
  ])
})

test('常用六项固定排在更多能力之前', () => {
  const otherActions = Array.from({ length: 14 }, (_, index) => action(`OTHER_${index}`))
  const unordered = [...otherActions, ...[...COMMON_TOOL_INTENTS].reverse().map((intent) => action(intent))]
  const sorted = sortToolActions(unordered)

  expect(sorted.slice(0, 6).map((item) => item.intent)).toEqual([...COMMON_TOOL_INTENTS])
  expect(sorted.slice(6)).toHaveLength(14)
})

test('动态表单生成可编辑自然语言', () => {
  const search = action('SEARCH_GOODS', {
    promptTemplate: '帮我找 {keyword}，预算 {{budget}} 元以内，返回 {limit} 件。',
    interactionType: 'SIMPLE_FORM',
    fields: [
      { name: 'keyword', label: '商品', type: 'TEXT', required: true },
      { name: 'budget', label: '预算', type: 'NUMBER', required: false },
      { name: 'limit', label: '数量', type: 'NUMBER', required: true }
    ]
  })

  expect(buildToolPrompt(search, { keyword: '无线耳机', budget: 500, limit: 5 })).toBe(
    '帮我找 无线耳机，预算 500 元以内，返回 5 件。'
  )
})

test('执行状态不会向页面泄露内部工具名称并按执行编号更新', () => {
  const started = upsertToolExecution([], {
    executionId: 'one',
    intent: 'SEARCH_GOODS',
    label: 'cps_search_goods',
    status: 'RUNNING',
    message: '正在调用 cps_search_goods'
  })
  const succeeded = upsertToolExecution(started, {
    executionId: 'one',
    intent: 'SEARCH_GOODS',
    label: '商品搜索',
    status: 'SUCCEEDED',
    message: '搜索完成'
  })

  expect(started[0].label).toBe('业务能力')
  expect(started[0].message).toBe('正在调用 业务能力')
  expect(succeeded).toHaveLength(1)
  expect(succeeded[0].status).toBe('SUCCEEDED')
  expect(toFriendlyToolText('cpx_generate_tracking_link 执行中')).toBe('业务能力 执行中')
})
