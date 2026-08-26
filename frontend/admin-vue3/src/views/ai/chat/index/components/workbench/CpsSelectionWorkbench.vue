<template>
  <section
    class="mx-20px mt-12px rounded-14px border border-solid border-[var(--el-border-color-light)] bg-[linear-gradient(120deg,var(--el-color-primary-light-9),var(--el-bg-color)_62%)]"
    :class="hasResults ? 'px-16px py-10px' : 'px-24px py-20px'"
    data-testid="cps-selection-workbench"
  >
    <div class="flex items-center justify-between gap-16px">
      <div class="min-w-0">
        <div class="flex items-center gap-8px">
          <span class="text-18px font-700">CPS AI 选品工作台</span>
          <el-tag size="small" effect="plain">数据驱动</el-tag>
        </div>
        <p
          v-if="!hasResults"
          class="mb-0 mt-7px text-13px leading-20px text-[var(--el-text-color-secondary)]"
        >
          用自然语言组合商品、榜单与成交数据，输出带来源、理由和风险提示的分析结果。
        </p>
      </div>

      <div
        class="shrink-0 rounded-9px bg-[var(--el-fill-color-light)] p-3px"
        data-testid="cps-workbench-mode-switch"
      >
        <el-button
          :type="mode === 'SELECTION' ? 'primary' : ''"
          :plain="mode !== 'SELECTION'"
          size="small"
          data-testid="cps-workbench-mode-selection"
          @click="changeMode('SELECTION')"
        >
          <Icon icon="ep:goods" class="mr-4px" />
          选品分析
        </el-button>
        <el-button
          :type="mode === 'ORDER' ? 'primary' : ''"
          :plain="mode !== 'ORDER'"
          size="small"
          class="!ml-2px"
          data-testid="cps-workbench-mode-order"
          @click="changeMode('ORDER')"
        >
          <Icon icon="ep:data-analysis" class="mr-4px" />
          订单分析
        </el-button>
      </div>
    </div>

    <div class="mt-10px flex flex-wrap items-center justify-end gap-8px">
      <el-button
        size="small"
        plain
        :disabled="disabled || !currentPrompt?.trim()"
        data-testid="cps-workbench-save-filter"
        @click="openSaveDialog"
      >
        <Icon icon="ep:collection-tag" class="mr-4px" />保存筛选条件
      </el-button>
      <el-button
        size="small"
        plain
        data-testid="cps-workbench-saved-filters"
        @click="openSavedDialog"
      >
        <Icon icon="ep:collection" class="mr-4px" />已保存条件{{ savedFilters.length ? ` (${savedFilters.length})` : '' }}
      </el-button>
    </div>

    <div
      v-if="taskProgress"
      class="mt-16px rounded-10px bg-[var(--el-bg-color)] p-14px"
      data-testid="cps-workbench-task-progress"
    >
      <div class="flex items-center justify-between gap-12px">
        <div class="min-w-0">
          <div class="font-600">{{ taskTitle }}</div>
          <p class="mb-0 mt-4px text-12px text-[var(--el-text-color-secondary)]">
            {{ taskProgress.currentStep || taskProgress.summary || '正在准备分析结果' }}
          </p>
        </div>
        <el-button
          v-if="taskProgress.retryable"
          type="primary"
          plain
          size="small"
          :disabled="disabled"
          data-testid="cps-workbench-retry"
          @click="emits('retry')"
        >
          <Icon icon="ep:refresh-right" class="mr-4px" />重试
        </el-button>
        <el-tag v-else :type="taskTagType" effect="plain" size="small">{{ taskStatusText }}</el-tag>
      </div>
      <el-progress
        class="mt-12px"
        :percentage="taskProgress.percent"
        :status="taskProgress.status === 'FAILED' ? 'exception' : taskProgress.status === 'SUCCEEDED' ? 'success' : undefined"
        :show-text="false"
      />
      <div class="mt-12px grid gap-7px" data-testid="cps-workbench-task-steps">
        <div v-for="step in visibleSteps" :key="step.id" class="flex items-center gap-7px text-12px">
          <Icon :icon="stepIcon(step.status)" :class="stepClass(step.status)" />
          <span :class="step.status === 'PENDING' ? 'text-[var(--el-text-color-secondary)]' : ''">
            {{ step.label }}
          </span>
          <span v-if="step.message" class="truncate text-[var(--el-text-color-secondary)]">
            {{ step.message }}
          </span>
        </div>
      </div>
      <el-alert
        v-if="taskProgress.error"
        class="mt-12px"
        :title="taskProgress.error"
        type="error"
        :closable="false"
        show-icon
      />
      <p v-else-if="taskProgress.summary" class="mb-0 mt-12px text-12px text-[var(--el-text-color-secondary)]">
        {{ taskProgress.summary }}
      </p>
    </div>

    <template v-else-if="!hasResults">
      <div class="mt-18px grid grid-cols-4 gap-10px" data-testid="cps-workbench-cases">
        <button
          v-for="item in currentCases"
          :key="item.id"
          type="button"
          class="min-h-104px cursor-pointer rounded-10px border border-solid border-[var(--el-border-color-light)] bg-[var(--el-bg-color)] p-13px text-left transition hover:border-[var(--el-color-primary-light-5)] hover:shadow-sm disabled:cursor-not-allowed disabled:opacity-60"
          :disabled="disabled"
          :data-testid="`cps-workbench-case-${item.id}`"
          @click="selectCase(item)"
        >
          <div class="flex items-center gap-7px font-600">
            <Icon :icon="item.icon" class="text-18px text-[var(--el-color-primary)]" />
            <span>{{ item.title }}</span>
          </div>
          <div class="mt-8px text-12px leading-18px text-[var(--el-text-color-secondary)]">
            {{ item.description }}
          </div>
        </button>
      </div>

      <div
        class="mt-14px flex flex-wrap items-center gap-x-14px gap-y-6px text-12px text-[var(--el-text-color-secondary)]"
        data-testid="cps-workbench-evidence-guide"
      >
        <span class="font-600 text-[var(--el-text-color-regular)]">结果包含</span>
        <span><Icon icon="ep:filter" class="mr-3px" />结构化筛选条件</span>
        <span><Icon icon="ep:trend-charts" class="mr-3px" />商品与成交证据</span>
        <span><Icon icon="ep:warning" class="mr-3px" />风险提示</span>
        <span><Icon icon="ep:pointer" class="mr-3px" />下一步运营动作</span>
      </div>
    </template>

    <div
      v-else
      class="mt-7px flex items-center gap-12px text-12px text-[var(--el-text-color-secondary)]"
      data-testid="cps-workbench-result-summary"
    >
      <span class="font-600 text-[var(--el-text-color-regular)]">分析结果工作区</span>
      <span class="min-w-0 truncate">{{ resultSummary || '已呈现执行步骤、数据证据、推荐理由与风险提示。' }}</span>
    </div>

    <el-dialog v-model="saveDialogVisible" title="保存 AI 筛选条件" width="420px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="条件名称" required>
          <el-input
            v-model="saveForm.themeName"
            maxlength="64"
            show-word-limit
            placeholder="例如：宝妈社群厨房用品"
          />
        </el-form-item>
        <el-form-item label="当前分析条件">
          <el-input :model-value="currentPrompt || ''" type="textarea" :rows="4" readonly />
        </el-form-item>
        <template v-if="mode === 'SELECTION'">
          <el-divider content-position="left">结构化刷新规则</el-divider>
          <el-form-item label="商品关键词">
            <el-input v-model="saveForm.keyword" placeholder="例如：厨房收纳" clearable />
          </el-form-item>
          <div class="grid grid-cols-2 gap-x-10px">
            <el-form-item label="最高券后价（元）">
              <el-input-number v-model="saveForm.priceUpperLimit" :min="0" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="最低佣金率（%）">
              <el-input-number v-model="saveForm.minCommissionRate" :min="0" :max="100" :precision="2" controls-position="right" />
            </el-form-item>
            <el-form-item label="最低月销量">
              <el-input-number v-model="saveForm.minMonthSales" :min="0" :precision="0" controls-position="right" />
            </el-form-item>
            <el-form-item label="候选数量">
              <el-input-number v-model="saveForm.pullCount" :min="1" :max="100" :precision="0" controls-position="right" />
            </el-form-item>
          </div>
          <div class="flex items-center justify-between gap-12px">
            <el-checkbox v-model="saveForm.onlyCoupon">仅看有券商品</el-checkbox>
            <el-switch v-model="saveForm.autoRefresh" active-text="自动刷新" />
          </div>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingFilter" @click="saveFilter">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="savedDialogVisible" title="已保存的 AI 筛选条件" width="680px" append-to-body>
      <div v-if="savedFilters.length" class="grid gap-8px">
        <article
          v-for="filter in savedFilters"
          :key="filter.id"
          class="rounded-10px border border-solid border-[var(--el-border-color-light)] p-10px"
        >
          <div class="flex items-center justify-between gap-8px">
            <div class="min-w-0">
              <div class="truncate font-600">{{ filter.themeName }}</div>
              <div class="mt-4px text-12px text-[var(--el-text-color-secondary)]">
                {{ filter.updateTime || filter.createTime || '已保存' }}
              </div>
              <div v-if="filter.lastRefreshTime || filter.refreshMessage" class="mt-3px text-12px text-[var(--el-text-color-secondary)]">
                刷新：{{ savedFilterRefreshText(filter) }}
              </div>
            </div>
            <el-tag size="small" effect="plain">{{ savedFilterMode(filter) }}</el-tag>
          </div>
          <p class="mb-8px mt-8px line-clamp-2 text-12px leading-18px text-[var(--el-text-color-secondary)]">
            {{ savedFilterPrompt(filter) }}
          </p>
          <div class="flex justify-end gap-8px">
            <el-button
              v-if="hasStructuredFilter(filter)"
              size="small"
              plain
              :loading="refreshingFilterId === filter.id"
              @click="refreshSavedFilter(filter)"
            >
              <Icon icon="ep:refresh" class="mr-4px" />刷新快照
            </el-button>
            <el-button size="small" plain :disabled="disabled" @click="useSavedFilter(filter)">
              <Icon icon="ep:refresh-right" class="mr-4px" />再次分析
            </el-button>
            <el-button size="small" type="danger" plain @click="removeSavedFilter(filter)">
              <Icon icon="ep:delete" class="mr-4px" />删除
            </el-button>
          </div>
        </article>
      </div>
      <el-empty v-else description="还没有保存筛选条件" :image-size="72" />
    </el-dialog>
  </section>
</template>

<script setup lang="ts">
import type { ChatTaskProgress } from '@/api/ai/chat/message'
import { CpsSelectionThemeApi, type CpsAiSavedSelectionFilterVO } from '@/api/cps/selectionTheme'
import type { RecommendedPrompt } from '../../toolActions'

export type CpsWorkbenchMode = 'SELECTION' | 'ORDER'

interface WorkbenchCase extends RecommendedPrompt {
  id: string
  title: string
  description: string
  icon: string
}

defineOptions({ name: 'CpsSelectionWorkbench' })

const message = useMessage()

const props = defineProps<{
  mode: CpsWorkbenchMode
  hasResults?: boolean
  disabled?: boolean
  taskProgress?: ChatTaskProgress
  resultSummary?: string
  currentPrompt?: string
  currentToolIntent?: string
}>()

const emits = defineEmits<{
  (e: 'update:mode', value: CpsWorkbenchMode): void
  (e: 'prompt', value: RecommendedPrompt): void
  (e: 'retry'): void
}>()

const selectionCases: WorkbenchCase[] = [
  {
    id: 'resonance',
    title: '从榜单选品',
    description: '交叉参考近期榜单与主题，寻找重复出现的潜力商品。',
    prompt:
      '从当前已发布的选品主题和近期榜单中，找出重复出现、适合优先测试的 10 款商品，并说明榜单来源、推荐理由和风险。',
    toolIntent: 'FIND_RESONANCE_GOODS',
    icon: 'ep:histogram'
  },
  {
    id: 'conditions',
    title: '指定条件筛选',
    description: '组合品类、价格、佣金、销量和店铺条件精准找货。',
    prompt:
      '帮我找适合宝妈社群推广的厨房用品，券后价 50 元以内、佣金率 20% 以上，优先近 7 天销量上涨的商品，返回 10 款并列出数据证据。',
    toolIntent: 'SEARCH_GOODS',
    icon: 'ep:filter'
  },
  {
    id: 'seasonal',
    title: '找应季商品',
    description: '结合当前节令、活动与搜索需求寻找推广机会。',
    prompt:
      '结合当前季节和近期平台活动，推荐 10 款未来两周适合推广的应季商品，说明适用人群、价格带、佣金和机会依据。',
    toolIntent: 'SEARCH_GOODS',
    icon: 'ep:sunny'
  },
  {
    id: 'alternative',
    title: '找高佣替代品',
    description: '在同类和相近价格带中，比较收益提升与转化风险。',
    prompt:
      '请根据我接下来提供的商品，寻找同品类、相近价格带且佣金更高的替代品，同时比较销量、到手价、预计佣金和替代风险。',
    toolIntent: 'FIND_ALTERNATIVES',
    icon: 'ep:switch'
  },
  {
    id: 'deep-analysis',
    title: '商品深度分析',
    description: '拆解价格带、优惠券、佣金、销量和平台覆盖，辅助人工决策。',
    prompt:
      '请对我接下来提供的商品关键词做深度分析，拆解价格带、优惠券覆盖、佣金、销量和平台分布，并给出候选商品与数据限制；只做运营分析，不生成推广链接。',
    toolIntent: 'ANALYZE_GOODS_DETAIL',
    icon: 'ep:data-analysis'
  }
]

const orderCases: WorkbenchCase[] = [
  {
    id: 'order-profile',
    title: '30 天成交画像',
    description: '分析成交品类、价格带和商品偏好，形成运营画像。',
    prompt:
      '分析当前会员近 30 天的成交订单，汇总成交品类、价格带、订单数和主要商品，并给出下一轮选品建议。',
    toolIntent: 'ANALYZE_ORDER_PROFILE',
    icon: 'ep:user'
  },
  {
    id: 'best-sellers',
    title: '成交商品复盘',
    description: '找出近期表现最好的商品，并解释可能的成交原因。',
    prompt:
      '查询当前会员近 30 天已付款订单，找出成交最好的 5 款商品，比较订单数、成交金额和预计返利，并分析为什么卖得好。',
    toolIntent: 'QUERY_ORDERS',
    icon: 'ep:trophy'
  },
  {
    id: 'category-trend',
    title: '成交趋势',
    description: '按日或按周对比成交额、订单数和返利走势。',
    prompt:
      '分析当前会员近 30 天的成交趋势，按日比较订单数、成交金额和实际返利，指出最近周期的变化和运营注意事项。',
    toolIntent: 'ANALYZE_ORDER_TREND',
    icon: 'ep:trend-charts'
  },
  {
    id: 'rebate-performance',
    title: '佣金收益分析',
    description: '结合返利账户与成交表现，识别实际收益贡献。',
    prompt:
      '汇总当前会员的返利账户和最近返利记录，分析主要收益来源、冻结金额和可用余额，并给出提升有效佣金的选品建议。',
    toolIntent: 'GET_REBATE_SUMMARY',
    icon: 'ep:coin'
  }
]

const currentCases = computed(() => (props.mode === 'SELECTION' ? selectionCases : orderCases))
const savedFilters = ref<CpsAiSavedSelectionFilterVO[]>([])
const savedDialogVisible = ref(false)
const saveDialogVisible = ref(false)
const savingFilter = ref(false)
const saveForm = reactive({
  themeName: '',
  keyword: '',
  priceUpperLimit: undefined as number | undefined,
  minCommissionRate: undefined as number | undefined,
  minMonthSales: undefined as number | undefined,
  pullCount: 20,
  onlyCoupon: false,
  autoRefresh: false
})
const refreshingFilterId = ref<number>()

const loadSavedFilters = async () => {
  try {
    const response = await CpsSelectionThemeApi.getAiSavedFilters()
    savedFilters.value = Array.isArray(response?.list) ? response.list : []
  } catch {
    savedFilters.value = []
  }
}

onMounted(loadSavedFilters)

const openSaveDialog = () => {
  saveForm.themeName = props.currentPrompt?.slice(0, 64) || '我的 AI 选品条件'
  saveForm.keyword = ''
  saveForm.priceUpperLimit = undefined
  saveForm.minCommissionRate = undefined
  saveForm.minMonthSales = undefined
  saveForm.pullCount = 20
  saveForm.onlyCoupon = false
  saveForm.autoRefresh = false
  saveDialogVisible.value = true
}

const openSavedDialog = async () => {
  await loadSavedFilters()
  savedDialogVisible.value = true
}

const saveFilter = async () => {
  const prompt = props.currentPrompt?.trim()
  const toolIntent = props.currentToolIntent
  const themeName = saveForm.themeName.trim()
  if (!prompt || !toolIntent || !themeName) {
    message.warning('请先输入分析条件，并填写条件名称')
    return
  }
  if (saveForm.autoRefresh && !saveForm.keyword.trim()) {
    message.warning('开启自动刷新时必须填写商品关键词')
    return
  }
  savingFilter.value = true
  try {
    await CpsSelectionThemeApi.createAiSavedFilter({
      themeName,
      prompt,
      toolIntent,
      mode: props.mode,
      structuredRule:
        props.mode === 'SELECTION' && saveForm.keyword.trim()
          ? {
              keywords: [saveForm.keyword.trim()],
              platforms: ['taobao'],
              priceUpperLimit: saveForm.priceUpperLimit,
              minCommissionRate: saveForm.minCommissionRate,
              minMonthSales: saveForm.minMonthSales,
              onlyCoupon: saveForm.onlyCoupon,
              pullCount: saveForm.pullCount,
              autoRefresh: saveForm.autoRefresh
            }
          : undefined
    })
    message.success('筛选条件已保存')
    saveDialogVisible.value = false
    await loadSavedFilters()
  } finally {
    savingFilter.value = false
  }
}

const savedFilterRule = (filter: CpsAiSavedSelectionFilterVO) => {
  try {
    return JSON.parse(filter.ruleJson || '{}') as {
      prompt?: string
      toolIntent?: string
      mode?: CpsWorkbenchMode
      keywords?: string[]
      priceUpperLimit?: number
      minCommissionRate?: number
      minMonthSales?: number
      autoRefresh?: boolean
    }
  } catch {
    return {}
  }
}

const savedFilterPrompt = (filter: CpsAiSavedSelectionFilterVO) =>
  savedFilterRule(filter).prompt || filter.aiPrompt || filter.description || '未记录分析条件'

const savedFilterMode = (filter: CpsAiSavedSelectionFilterVO) =>
  savedFilterRule(filter).mode === 'ORDER' ? '订单分析' : '选品分析'

const hasStructuredFilter = (filter: CpsAiSavedSelectionFilterVO) =>
  Boolean(savedFilterRule(filter).keywords?.length)

const savedFilterRefreshText = (filter: CpsAiSavedSelectionFilterVO) =>
  filter.refreshMessage || `${filter.refreshStatus || '未刷新'} ${filter.lastRefreshTime || ''}`.trim()

const refreshSavedFilter = async (filter: CpsAiSavedSelectionFilterVO) => {
  refreshingFilterId.value = filter.id
  try {
    const result = await CpsSelectionThemeApi.refreshAiSavedFilter(filter.id)
    if (result.status === 'FAILED') message.error(result.message || '刷新失败')
    else if (result.status === 'SKIPPED') message.warning(result.message || '当前条件不可刷新')
    else message.success(`刷新完成，写入 ${result.importedCount || 0} 个商品快照`)
    await loadSavedFilters()
  } finally {
    refreshingFilterId.value = undefined
  }
}

const useSavedFilter = (filter: CpsAiSavedSelectionFilterVO) => {
  const rule = savedFilterRule(filter)
  if (rule.mode && rule.mode !== props.mode) {
    emits('update:mode', rule.mode)
  }
  emits('prompt', {
    prompt: savedFilterPrompt(filter),
    toolIntent: (rule.toolIntent || 'SEARCH_GOODS') as RecommendedPrompt['toolIntent']
  })
  savedDialogVisible.value = false
}

const removeSavedFilter = async (filter: CpsAiSavedSelectionFilterVO) => {
  try {
    await message.delConfirm(`确认删除“${filter.themeName}”吗？`)
    await CpsSelectionThemeApi.deleteTheme(filter.id)
    message.success('已删除')
    await loadSavedFilters()
  } catch {}
}
const visibleSteps = computed(() => props.taskProgress?.steps.slice(0, 5) || [])
const taskStatusText = computed(() => {
  const status = props.taskProgress?.status
  if (status === 'SUCCEEDED') return '分析完成'
  if (status === 'FAILED') return '分析失败'
  if (status === 'CANCELLED') return '已停止'
  if (status === 'QUEUED') return '等待执行'
  return '分析中'
})
const taskTitle = computed(() => (props.mode === 'SELECTION' ? '选品分析任务' : '订单分析任务'))
const taskTagType = computed(() => {
  const status = props.taskProgress?.status
  if (status === 'SUCCEEDED') return 'success'
  if (status === 'FAILED') return 'danger'
  if (status === 'CANCELLED') return 'info'
  return 'primary'
})

const stepIcon = (status: string) => {
  if (status === 'SUCCEEDED') return 'ep:circle-check-filled'
  if (status === 'FAILED') return 'ep:circle-close-filled'
  if (status === 'SKIPPED') return 'ep:remove-filled'
  if (status === 'RUNNING') return 'ep:loading'
  return 'ep:circle-close'
}

const stepClass = (status: string) => {
  if (status === 'SUCCEEDED') return 'text-[var(--el-color-success)]'
  if (status === 'FAILED') return 'text-[var(--el-color-danger)]'
  if (status === 'RUNNING') return 'animate-spin text-[var(--el-color-primary)]'
  return 'text-[var(--el-text-color-placeholder)]'
}

const changeMode = (mode: CpsWorkbenchMode) => {
  if (!props.disabled) emits('update:mode', mode)
}

const selectCase = (item: WorkbenchCase) => {
  emits('prompt', { prompt: item.prompt, toolIntent: item.toolIntent })
}
</script>
