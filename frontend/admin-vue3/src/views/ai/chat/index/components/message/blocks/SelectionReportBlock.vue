<template>
  <div class="selection-report">
    <el-alert
      v-if="block.error"
      title="选品分析失败"
      :description="String(block.error)"
      type="error"
      :closable="false"
      show-icon
    />

    <p v-if="block.summary" class="selection-report__summary">{{ block.summary }}</p>

    <section v-if="criteria.length">
      <h5>筛选条件</h5>
      <div class="selection-report__tags">
        <el-tag v-for="criterion in criteria" :key="criterion" size="small" effect="plain">
          {{ criterion }}
        </el-tag>
      </div>
    </section>

    <el-empty
      v-if="!block.items?.length && !block.error"
      description="暂无符合条件的商品"
      :image-size="64"
    />

    <section v-if="block.items?.length">
      <h5>推荐商品</h5>
      <div class="selection-report__items">
        <article v-for="(item, index) in block.items" :key="productKey(item, index)">
          <div class="selection-report__item-title">
            <strong>{{ item.title || '未命名商品' }}</strong>
            <el-tag v-if="item.resonanceScore !== undefined" size="small" type="success">
              共振分 {{ item.resonanceScore }}
            </el-tag>
            <el-tag v-else-if="item.alternativeScore !== undefined" size="small" type="warning">
              替代分 {{ item.alternativeScore }}
            </el-tag>
            <el-tag v-else-if="item.analysisScore !== undefined" size="small" type="success">
              分析分 {{ item.analysisScore }}
            </el-tag>
            <el-tag v-if="isConfirmed(item, index)" size="small" type="success" effect="plain">
              已确认
            </el-tag>
          </div>
          <div class="selection-report__item-metrics">
            <span v-if="item.actualPrice !== undefined">到手价 ¥{{ item.actualPrice }}</span>
            <span v-if="item.commissionRate !== undefined">佣金 {{ percent(item.commissionRate) }}</span>
            <span v-if="item.monthSales !== undefined">月销 {{ item.monthSales }}</span>
            <span v-if="item.priceDelta !== undefined">价差 {{ signedMoney(item.priceDelta) }}</span>
            <span v-if="item.commissionDelta !== undefined">预计佣金 ¥{{ item.commissionDelta }}</span>
          </div>
          <div v-if="item.rankSources?.length" class="selection-report__tags">
            <el-tag v-for="source in item.rankSources" :key="source" size="small" type="warning" effect="plain">
              {{ source }}
            </el-tag>
          </div>
          <ul v-if="item.evidence?.length" class="selection-report__evidence">
            <li v-for="(evidence, evidenceIndex) in item.evidence" :key="evidenceIndex">
              {{ displayValue(evidence) }}
            </li>
          </ul>
          <el-alert
            v-if="item.riskNotes?.length"
            :title="item.riskNotes.join('；')"
            type="warning"
            :closable="false"
            show-icon
          />
          <div class="selection-report__item-actions">
            <el-button
              size="small"
              :type="isConfirmed(item, index) ? 'success' : 'primary'"
              plain
              :loading="isReviewSaving(item, index)"
              :disabled="!item.goodsId || !reviewContextId"
              @click="toggleConfirmed(item, index)"
            >
              <Icon :icon="isConfirmed(item, index) ? 'ep:close' : 'ep:check'" class="mr-4px" />
              {{ isConfirmed(item, index) ? '撤销确认' : '确认入选' }}
            </el-button>
            <span class="text-12px text-[var(--el-text-color-secondary)]">
              {{ reviewStatusText }}
            </span>
          </div>
        </article>
      </div>
    </section>

    <section v-if="block.evidence?.length">
      <h5>数据证据</h5>
      <ul class="selection-report__evidence">
        <li v-for="(evidence, index) in block.evidence" :key="index">
          {{ displayValue(evidence) }}
        </li>
      </ul>
    </section>

    <el-alert
      v-if="riskNotes.length"
      title="风险提示"
      :description="riskNotes.join('；')"
      type="warning"
      :closable="false"
      show-icon
    />

    <div v-if="block.actions?.length" class="selection-report__tags">
      <el-tag v-for="action in block.actions" :key="`${action.type}-${action.label}`" effect="plain">
        {{ action.label }}
      </el-tag>
    </div>

    <div v-if="confirmedItems.length" class="selection-report__confirm-summary">
      <span>已确认 {{ confirmedItems.length }} 款商品</span>
      <el-button size="small" type="primary" plain @click="openConfirmedThemeDialog">
        <Icon icon="ep:collection" class="mr-4px" />保存为选品主题草稿
      </el-button>
    </div>

    <el-dialog v-model="confirmedThemeDialogVisible" title="保存人工确认结果" width="420px" append-to-body>
      <el-form label-position="top">
        <el-form-item label="选品主题名称" required>
          <el-input v-model="confirmedThemeName" maxlength="64" show-word-limit />
        </el-form-item>
      </el-form>
      <p class="text-12px leading-18px text-[var(--el-text-color-secondary)]">
        只保存已确认商品的运营快照，后续仍需在选品库中审核；不会生成推广链接或修改返利资产。
      </p>
      <template #footer>
        <el-button @click="confirmedThemeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingConfirmedTheme" @click="saveConfirmedTheme">
          保存草稿
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { CpsSelectionThemeApi } from '@/api/cps/selectionTheme'
import type { ChatMessageBlock, ChatMessageProductItem } from '@/api/ai/chat/message'

const props = defineProps<{ block: ChatMessageBlock; reviewContextId: string }>()
const confirmedKeys = ref<Record<string, boolean>>({})
const reviewSavingKeys = ref<Record<string, boolean>>({})
const reviewSyncAvailable = ref(true)
const confirmedThemeDialogVisible = ref(false)
const savingConfirmedTheme = ref(false)
const confirmedThemeName = ref('AI 人工确认选品')
const message = useMessage()
let reviewLoadSequence = 0

const loadReviews = async () => {
  const sequence = ++reviewLoadSequence
  if (!props.reviewContextId) {
    confirmedKeys.value = {}
    reviewSyncAvailable.value = false
    return
  }
  try {
    const reviews = await CpsSelectionThemeApi.getAiReviews(props.reviewContextId)
    if (sequence !== reviewLoadSequence) return
    confirmedKeys.value = Object.fromEntries(
      reviews.map((review) => [
        `${review.platformCode || 'unknown'}|${review.vendorCode || ''}|${review.goodsId}|${review.goodsSign || ''}`,
        review.reviewStatus === 'CONFIRMED'
      ])
    )
  } catch {
    if (sequence !== reviewLoadSequence) return
    confirmedKeys.value = {}
    reviewSyncAvailable.value = false
  }
}

watch(() => props.reviewContextId, loadReviews, { immediate: true })

const displayValue = (value: unknown): string => {
  if (value === null || value === undefined) return ''
  if (typeof value !== 'object') return String(value)
  return Object.entries(value as Record<string, unknown>)
    .map(([key, item]) => `${key}：${displayValue(item)}`)
    .join('，')
}

const criteria = computed(() => {
  const value = props.block.criteria
  if (!value) return []
  if (Array.isArray(value)) return value.map(displayValue).filter(Boolean)
  return Object.entries(value)
    .filter(([, item]) => item !== undefined && item !== null && item !== '')
    .map(([key, item]) => `${key}：${displayValue(item)}`)
})

const riskNotes = computed(() => props.block.riskNotes || props.block.risks || [])
const reviewStatusText = computed(() => {
  if (!props.reviewContextId) return '消息保存后可记录复核状态'
  return reviewSyncAvailable.value ? '复核状态已同步并记录操作人' : '复核记录暂时无法同步'
})

const productKey = (item: ChatMessageProductItem, index: number) =>
  `${item.platformCode || 'unknown'}-${item.goodsId || item.goodsSign || index}`

const confirmationKey = (item: ChatMessageProductItem, index: number) =>
  `${item.platformCode || 'unknown'}|${item.vendorCode || ''}|${item.goodsId || item.title || index}|${item.goodsSign || ''}`

const isConfirmed = (item: ChatMessageProductItem, index: number) =>
  Boolean(confirmedKeys.value[confirmationKey(item, index)])

const confirmedItems = computed(() =>
  (props.block.items || []).filter((item, index) => isConfirmed(item, index) && item.goodsId)
)

const isReviewSaving = (item: ChatMessageProductItem, index: number) =>
  Boolean(reviewSavingKeys.value[confirmationKey(item, index)])

const toggleConfirmed = async (item: ChatMessageProductItem, index: number) => {
  if (!props.reviewContextId) {
    message.warning('消息尚未持久化，请稍后再确认')
    return
  }
  if (!item.goodsId) {
    message.warning('商品缺少可审计的商品 ID，无法确认入选')
    return
  }
  const key = confirmationKey(item, index)
  const nextConfirmed = !confirmedKeys.value[key]
  reviewSavingKeys.value = { ...reviewSavingKeys.value, [key]: true }
  try {
    await CpsSelectionThemeApi.saveAiReview({
      reviewContextId: props.reviewContextId,
      platformCode: String(item.platformCode || 'unknown'),
      vendorCode: item.vendorCode,
      goodsId: String(item.goodsId),
      goodsSign: item.goodsSign,
      title: item.title,
      mainPic: item.mainPic,
      reviewStatus: nextConfirmed ? 'CONFIRMED' : 'WITHDRAWN',
      remark: nextConfirmed ? 'AI 选品工作台人工确认入选' : 'AI 选品工作台撤销确认'
    })
    confirmedKeys.value = { ...confirmedKeys.value, [key]: nextConfirmed }
    reviewSyncAvailable.value = true
  } catch (error) {
    reviewSyncAvailable.value = false
    message.error(error instanceof Error ? error.message : '复核状态保存失败')
  } finally {
    reviewSavingKeys.value = { ...reviewSavingKeys.value, [key]: false }
  }
}

const openConfirmedThemeDialog = () => {
  confirmedThemeName.value = props.block.title ? `${props.block.title}（人工确认）` : 'AI 人工确认选品'
  confirmedThemeDialogVisible.value = true
}

const saveConfirmedTheme = async () => {
  if (!confirmedItems.value.length || !confirmedThemeName.value.trim()) {
    message.warning('请至少确认一款商品，并填写主题名称')
    return
  }
  savingConfirmedTheme.value = true
  let themeId: number | undefined
  try {
    const createdThemeId = await CpsSelectionThemeApi.createTheme({
      themeCode: `AI_CONFIRMED_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
      themeName: confirmedThemeName.value.trim(),
      themeType: 'AI_CONFIRMED_SELECTION',
      platformCodes: [...new Set(confirmedItems.value.map((item) => item.platformCode).filter(Boolean))].join(','),
      vendorCode: confirmedItems.value[0]?.vendorCode || 'dataoke',
      description: '来自 AI 选品报告的人工确认商品快照',
      tags: 'AI选品,人工确认',
      ruleJson: JSON.stringify({ reviewContextId: props.reviewContextId, reviewOnly: true }),
      status: 'DRAFT',
      goodsSquareVisible: 0,
      remark: '商品价格、佣金、销量均为分析快照，不作为资金事实'
    })
    themeId = createdThemeId
    await CpsSelectionThemeApi.importItems({
      themeId: createdThemeId,
      sourceType: 'AI_RECOMMEND',
      items: confirmedItems.value.map((item) => ({
        platformCode: String(item.platformCode || 'taobao'),
        vendorCode: item.vendorCode,
        goodsId: String(item.goodsId),
        goodsSign: item.goodsSign,
        title: item.title,
        mainPic: item.mainPic,
        originalPrice: numeric(item.originalPrice),
        actualPrice: numeric(item.actualPrice),
        couponPrice: numeric(item.couponPrice),
        commissionRate: numeric(item.commissionRate),
        commissionAmount: numeric(item.commissionAmount),
        monthSales: integer(item.monthSales),
        shopName: item.shopName,
        itemLink: item.itemLink
      }))
    })
    message.success('已保存为选品主题草稿')
    confirmedThemeDialogVisible.value = false
  } catch (error) {
    if (themeId) {
      try {
        await CpsSelectionThemeApi.deleteTheme(themeId)
      } catch {
        // 删除失败时保留草稿，避免丢失已创建的运营记录。
      }
    }
    message.error(error instanceof Error ? error.message : '保存确认结果失败')
  } finally {
    savingConfirmedTheme.value = false
  }
}

const numeric = (value: unknown) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : undefined
}

const integer = (value: unknown) => {
  const number = Number(value)
  return Number.isFinite(number) ? Math.round(number) : undefined
}

const percent = (value: unknown) => {
  const text = String(value)
  return text.endsWith('%') ? text : `${text}%`
}

const signedMoney = (value: unknown) => {
  const number = Number(value)
  if (!Number.isFinite(number)) return String(value)
  return `${number > 0 ? '+' : ''}¥${number}`
}
</script>

<style scoped>
.selection-report {
  display: grid;
  gap: 14px;
  padding: 0 14px 14px;
}

.selection-report__summary {
  margin: 0;
  padding: 12px;
  border-radius: 10px;
  background: var(--el-color-primary-light-9);
  line-height: 1.65;
}

.selection-report h5 {
  margin: 0 0 8px;
  color: var(--el-text-color-primary);
}

.selection-report__tags,
.selection-report__item-title,
.selection-report__item-metrics {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.selection-report__item-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.selection-report__confirm-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--el-color-success-light-9);
  color: var(--el-color-success-dark-2);
  font-size: 12px;
}

.selection-report__items {
  display: grid;
  gap: 10px;
}

.selection-report__items article {
  display: grid;
  gap: 8px;
  padding: 12px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.selection-report__item-title strong {
  min-width: 0;
  flex: 1;
}

.selection-report__item-metrics {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.selection-report__evidence {
  margin: 0;
  padding-left: 20px;
  color: var(--el-text-color-regular);
  line-height: 1.7;
}
</style>
