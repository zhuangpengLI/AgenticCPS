import request from '@/config/axios'
import { fetchEventSource } from '@microsoft/fetch-event-source'
import { getAccessToken } from '@/utils/auth'
import { config } from '@/config/axios/config'

export type ChatMessageBlockType =
  | 'PRODUCT_RECOMMEND'
  | 'PRODUCT_COMPARE'
  | 'REBATE_SUMMARY'
  | 'FOLLOW_UP'
  | 'SELECTION_REPORT'
  | 'ALTERNATIVES_REPORT'
  | 'GOODS_ANALYSIS'
  | 'ORDER_PROFILE'
  | 'ORDER_TREND'
  | (string & {})

export interface ChatMessageBlockAction {
  type: string
  label: string
  riskLevel?: string
  payload?: Record<string, unknown>
}

export interface ChatMessageProductItem {
  platformCode?: string
  platformName?: string
  goodsId?: string | number
  goodsSign?: string
  title?: string
  mainPic?: string
  originalPrice?: string | number
  actualPrice?: string | number
  couponPrice?: string | number
  couponConditions?: string
  couponStartTime?: string
  couponEndTime?: string
  netPrice?: string | number
  commissionAmount?: string | number
  commissionRate?: string | number
  monthSales?: string | number
  shopName?: string
  vendorCode?: string
  itemLink?: string
  promotionUrl?: string
  resonanceScore?: string | number
  alternativeScore?: string | number
  priceDelta?: string | number
  commissionDelta?: string | number
  rankSources?: string[]
  evidence?: Array<string | Record<string, unknown>>
  riskNotes?: string[]
  actions?: ChatMessageBlockAction[]
  [key: string]: unknown
}

export interface ChatMessageBlock {
  id: string
  version: number
  type: ChatMessageBlockType
  title?: string
  subtitle?: string
  content?: string
  summary?: string
  items?: ChatMessageProductItem[]
  criteria?: Record<string, unknown> | Array<string | Record<string, unknown>>
  evidence?: Array<string | Record<string, unknown>>
  riskNotes?: string[]
  risks?: string[]
  actions?: ChatMessageBlockAction[]
  availableBalance?: string | number
  frozenBalance?: string | number
  totalRebate?: string | number
  withdrawnAmount?: string | number
  accountStatus?: string
  actualPrice?: string | number
  commissionAmount?: string | number
  shortUrl?: string
  mobileUrl?: string
  promotionUrl?: string
  tpwd?: string
  commandLabel?: string
  command?: string
  [key: string]: unknown
}

export type ChatTaskStatus = 'QUEUED' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'CANCELLED'
export type ChatTaskStepStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED' | 'SKIPPED'

export interface ChatTaskStep {
  id: string
  label: string
  status: ChatTaskStepStatus
  message?: string
}

/**
 * 前端流式任务状态。该字段只存在于当前页面的临时消息，不改变后端消息接口。
 * 用于把工具事件转换成可理解的任务进度，并为失败任务提供重试入口。
 */
export interface ChatTaskProgress {
  status: ChatTaskStatus
  percent: number
  currentStep?: string
  summary?: string
  error?: string
  retryable?: boolean
  steps: ChatTaskStep[]
}

// 聊天VO
export interface ChatMessageVO {
  id: number // 编号
  conversationId: number // 对话编号
  type: string // 消息类型
  userId: string // 用户编号
  roleId: string // 角色编号
  model: number // 模型标志
  modelId: number // 模型编号
  content: string // 聊天内容
  reasoningContent?: string // 推理内容
  attachmentUrls?: string[] // 附件 URL 数组
  tokens: number // 消耗 Token 数量
  segmentIds?: number[] // 段落编号
  segments?: {
    id: number // 段落编号
    content: string // 段落内容
    documentId: number // 文档编号
    documentName: string // 文档名称
  }[]
  webSearchPages?: {
    name: string // 名称
    icon: string // 图标
    title: string // 标题
    url: string // URL
    snippet: string // 内容的简短描述
    summary: string // 内容的文本摘要
  }[]
  createTime: Date // 创建时间
  roleAvatar: string // 角色头像
  userAvatar: string // 用户头像
  blocks?: ChatMessageBlock[] // 允许列表业务工具生成的结构化展示块
  toolExecutions?: ToolExecutionVO[] // 当前回答的工具执行状态（仅当前页面保留）
  taskProgress?: ChatTaskProgress // 当前页面的任务进度（仅流式执行期间/结果摘要使用）
  toolIntent?: string // 仅发送时使用的隐藏路由意图
  intentRequestId?: string // 仅发送时使用的幂等请求编号
}

export type ChatStreamEventType =
  | 'MESSAGE_DELTA'
  | 'TOOL_STARTED'
  | 'TOOL_SUCCEEDED'
  | 'TOOL_FAILED'

export interface ToolExecutionVO {
  executionId: string
  intent: string
  label: string
  status: string
  message: string
}

export interface ChatMessageStreamData {
  eventType?: ChatStreamEventType
  send?: ChatMessageVO
  receive?: ChatMessageVO
  toolExecution?: ToolExecutionVO
}

// AI chat 聊天
export const ChatMessageApi = {
  // 消息列表
  getChatMessageListByConversationId: async (conversationId: number | null) => {
    return await request.get({
      url: `/ai/chat/message/list-by-conversation-id?conversationId=${conversationId}`
    })
  },

  // 发送 Stream 消息
  // 为什么不用 axios 呢？因为它不支持 SSE 调用
  sendChatMessageStream: async (
    conversationId: number,
    content: string,
    ctrl,
    enableContext: boolean,
    enableWebSearch: boolean,
    onMessage,
    onError,
    onClose,
    attachmentUrls?: string[],
    toolIntent?: string,
    intentRequestId?: string
  ) => {
    const token = getAccessToken()
    return fetchEventSource(`${config.base_url}/ai/chat/message/send-stream`, {
      method: 'post',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`
      },
      openWhenHidden: true,
      body: JSON.stringify({
        conversationId,
        content,
        useContext: enableContext,
        useSearch: enableWebSearch,
        attachmentUrls: attachmentUrls || [],
        toolIntent,
        intentRequestId
      }),
      onmessage: onMessage,
      onerror: onError,
      onclose: onClose,
      signal: ctrl.signal
    })
  },

  // 删除消息
  deleteChatMessage: async (id: string) => {
    return await request.delete({ url: `/ai/chat/message/delete?id=${id}` })
  },

  // 删除指定对话的消息
  deleteByConversationId: async (conversationId: number) => {
    return await request.delete({
      url: `/ai/chat/message/delete-by-conversation-id?conversationId=${conversationId}`
    })
  },

  // 获得消息分页
  getChatMessagePage: async (params: any) => {
    return await request.get({ url: '/ai/chat/message/page', params })
  },

  // 管理员删除消息
  deleteChatMessageByAdmin: async (id: number) => {
    return await request.delete({ url: `/ai/chat/message/delete-by-admin?id=${id}` })
  }
}
