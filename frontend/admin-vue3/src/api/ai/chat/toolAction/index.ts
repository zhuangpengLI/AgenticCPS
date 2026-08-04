import request from '@/config/axios'

export type ToolActionRiskLevel = 'READ_ONLY' | 'ATTRIBUTION_WRITE' | 'ASSET_WRITE'
export type ToolActionInteractionType = 'DIRECT' | 'SIMPLE_FORM' | 'FORM'

export interface ToolActionOptionVO {
  label: string
  value: string | number | boolean
}

export interface ToolActionFieldVO {
  name: string
  label: string
  type: string
  required: boolean
  placeholder?: string
  defaultValue?: unknown
  options?: ToolActionOptionVO[]
}

export interface ToolActionVO {
  intent: string
  label: string
  group: string
  riskLevel: ToolActionRiskLevel
  interactionType: ToolActionInteractionType
  runningMessage: string
  successMessage: string
  promptTemplate: string
  fields: ToolActionFieldVO[]
}

export const ChatToolActionApi = {
  getToolActions: async (conversationId: number) => {
    return await request.get<ToolActionVO[]>({
      url: '/ai/chat/conversation/tool-actions',
      params: { conversationId }
    })
  }
}
