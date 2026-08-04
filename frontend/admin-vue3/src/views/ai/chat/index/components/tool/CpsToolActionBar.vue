<template>
  <div v-if="sortedActions.length" class="mx-20px pt-8px" data-testid="cps-tool-actions">
    <div class="flex items-center gap-8px overflow-x-auto pb-2px">
      <span class="shrink-0 text-13px text-[var(--el-text-color-secondary)]">快捷操作</span>
      <el-button
        v-for="action in commonActions"
        :key="action.intent"
        round
        plain
        size="small"
        :disabled="disabled"
        @click="handleActionClick(action)"
      >
        {{ friendly(action.label, '业务操作') }}
      </el-button>
      <el-popover v-if="moreActions.length" v-model:visible="moreVisible" width="460" trigger="click">
        <template #reference>
          <el-button round plain size="small" :disabled="disabled">
            更多
            <Icon icon="ep:arrow-down" class="ml-3px" />
          </el-button>
        </template>
        <div class="max-h-420px overflow-y-auto" data-testid="cps-tool-actions-more">
          <section v-for="group in groupedMoreActions" :key="group.name" class="mb-12px last:mb-0">
            <div class="mb-6px text-12px text-[var(--el-text-color-secondary)]">
              {{ friendly(group.name, '其他能力') }}
            </div>
            <div class="grid grid-cols-3 gap-6px">
              <el-button
                v-for="action in group.actions"
                :key="action.intent"
                class="!ml-0"
                plain
                size="small"
                :disabled="disabled"
                @click="handleActionClick(action)"
              >
                {{ friendly(action.label, '业务操作') }}
              </el-button>
            </div>
          </section>
        </div>
      </el-popover>
    </div>
  </div>

  <el-dialog
    v-model="formVisible"
    :title="friendly(activeAction?.label, '完善请求信息')"
    width="520px"
    append-to-body
    destroy-on-close
  >
    <el-form v-if="activeAction" label-position="top" @submit.prevent>
      <el-form-item
        v-for="field in activeAction.fields || []"
        :key="field.name"
        :label="friendly(field.label, field.name)"
        :required="field.required"
      >
        <el-input-number
          v-if="fieldType(field.type) === 'NUMBER'"
          v-model="formValues[field.name]"
          class="!w-full"
          :min="0"
          controls-position="right"
        />
        <el-select
          v-else-if="fieldType(field.type) === 'SELECT'"
          v-model="formValues[field.name]"
          class="w-full"
          clearable
          :placeholder="field.placeholder"
        >
          <el-option
            v-for="option in field.options || []"
            :key="String(option.value)"
            :label="friendly(option.label, String(option.value))"
            :value="option.value"
          />
        </el-select>
        <el-date-picker
          v-else-if="fieldType(field.type) === 'DATE_RANGE'"
          v-model="formValues[field.name]"
          class="!w-full"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-date-picker
          v-else-if="fieldType(field.type) === 'DATE'"
          v-model="formValues[field.name]"
          class="!w-full"
          type="date"
          value-format="YYYY-MM-DD"
          :placeholder="field.placeholder"
        />
        <el-input
          v-else
          v-model="formValues[field.name]"
          :type="fieldType(field.type) === 'TEXTAREA' ? 'textarea' : 'text'"
          :rows="fieldType(field.type) === 'TEXTAREA' ? 3 : undefined"
          :placeholder="field.placeholder"
          clearable
        />
      </el-form-item>
      <el-alert
        v-if="activeAction.riskLevel !== 'READ_ONLY'"
        :title="
          activeAction.riskLevel === 'ASSET_WRITE'
            ? '该操作会影响返利资产，提交前将进行两次确认。'
            : '该操作会生成归因或跟踪信息，提交前需要确认。'
        "
        type="warning"
        :closable="false"
        show-icon
      />
    </el-form>
    <template #footer>
      <el-button @click="formVisible = false">取消</el-button>
      <el-button type="primary" @click="submitForm">
        {{ activeAction?.riskLevel === 'READ_ONLY' ? '生成到输入框' : '确认并发送' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ElMessageBox } from 'element-plus'
import type { ToolActionFieldVO, ToolActionVO } from '@/api/ai/chat/toolAction'
import {
  buildToolPrompt,
  COMMON_TOOL_INTENTS,
  createIntentRequestId,
  getDefaultFieldValues,
  sortToolActions,
  toFriendlyToolText
} from '../../toolActions'

defineOptions({ name: 'CpsToolActionBar' })

const props = defineProps<{
  actions: ToolActionVO[]
  disabled?: boolean
}>()

const emits = defineEmits<{
  (
    e: 'submit',
    value: { action: ToolActionVO; prompt: string; intentRequestId: string; autoSend: boolean }
  ): void
}>()

const message = useMessage()
const moreVisible = ref(false)
const formVisible = ref(false)
const activeAction = ref<ToolActionVO>()
const formValues = reactive<Record<string, any>>({})

const sortedActions = computed(() => sortToolActions(props.actions || []))
const commonIntentSet = new Set<string>(COMMON_TOOL_INTENTS)
const commonActions = computed(() =>
  sortedActions.value.filter((action) => commonIntentSet.has(action.intent))
)
const moreActions = computed(() =>
  sortedActions.value.filter((action) => !commonIntentSet.has(action.intent))
)
const groupedMoreActions = computed(() => {
  const groups = new Map<string, ToolActionVO[]>()
  moreActions.value.forEach((action) => {
    const name = action.group || '其他能力'
    groups.set(name, [...(groups.get(name) || []), action])
  })
  return [...groups.entries()].map(([name, actions]) => ({ name, actions }))
})

const friendly = (value?: string, fallback?: string) => toFriendlyToolText(value, fallback)
const fieldType = (type?: string) => (type || 'TEXT').toUpperCase().replace('-', '_')

const resetFormValues = (fields: ToolActionFieldVO[]) => {
  Object.keys(formValues).forEach((key) => delete formValues[key])
  Object.assign(formValues, getDefaultFieldValues(fields || []))
}

const confirmRisk = async (action: ToolActionVO, intentRequestId: string, prompt: string) => {
  const promptSummary = friendly(prompt, '请核对本次业务请求').slice(0, 300)
  if (action.riskLevel === 'ATTRIBUTION_WRITE') {
    await ElMessageBox.confirm(
      `即将执行“${friendly(action.label, '归因操作')}”：${promptSummary}。确认继续吗？`,
      '确认业务操作',
      { type: 'warning', confirmButtonText: '确认执行' }
    )
  }
  if (action.riskLevel === 'ASSET_WRITE') {
    await ElMessageBox.confirm(
      `请核对“${friendly(action.label, '资产操作')}”：${promptSummary}。确认提交吗？`,
      '第一次确认',
      { type: 'warning', confirmButtonText: '内容无误' }
    )
    await ElMessageBox.confirm(
      `该操作会影响返利资产：${promptSummary}。请求编号 ${intentRequestId}，是否最终执行？`,
      '资产变更二次确认',
      { type: 'error', confirmButtonText: '最终确认执行' }
    )
  }
}

const emitAction = async (action: ToolActionVO, values: Record<string, unknown>) => {
  const prompt = buildToolPrompt(action, values)
  if (!prompt) {
    message.error('无法生成请求，请补充必要信息')
    return
  }
  const intentRequestId = createIntentRequestId()
  await confirmRisk(action, intentRequestId, prompt)
  emits('submit', {
    action,
    prompt,
    intentRequestId,
    autoSend: action.interactionType === 'DIRECT' || action.riskLevel !== 'READ_ONLY'
  })
}

const handleActionClick = async (action: ToolActionVO) => {
  moreVisible.value = false
  activeAction.value = action
  resetFormValues(action.fields || [])
  if (action.interactionType === 'DIRECT' && !action.fields?.length) {
    try {
      await emitAction(action, {})
    } catch {
      // 用户取消风险确认
    }
    return
  }
  formVisible.value = true
}

const submitForm = async () => {
  const action = activeAction.value
  if (!action) return
  const missingField = (action.fields || []).find((field) => {
    const value = formValues[field.name]
    return field.required && (value === '' || value === undefined || value === null)
  })
  if (missingField) {
    message.warning(`请填写${friendly(missingField.label, missingField.name)}`)
    return
  }
  try {
    await emitAction(action, { ...formValues })
    formVisible.value = false
  } catch {
    // 用户取消风险确认
  }
}
</script>
