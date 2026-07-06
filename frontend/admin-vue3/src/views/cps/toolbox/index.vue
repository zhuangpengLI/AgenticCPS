<template>
  <div class="toolbox-page">
    <div class="toolbox-header">
      <div>
        <div class="page-title">返利工具箱</div>
        <div class="page-subtitle">选品、解析、转链、返利预估和推广文案集中处理</div>
      </div>
      <div class="header-tags">
        <el-tag type="success" effect="plain">批量最多 20 条</el-tag>
        <el-tag effect="plain">复用 CPS 转链记录</el-tag>
      </div>
    </div>

    <el-alert
      class="toolbox-alert"
      type="warning"
      show-icon
      :closable="false"
      title="请确认平台授权、供应商配置和推广位可用；后台选择会员仅用于运营工具归因，不开放给用户端。"
    />

    <div class="toolbox-layout">
      <aside class="tool-nav">
        <div v-for="group in toolGroups" :key="group.label" class="tool-group">
          <div class="group-title">{{ group.label }}</div>
          <button
            v-for="tool in group.children"
            :key="tool.key"
            class="tool-item"
            :class="{ active: activeTool === tool.key, disabled: tool.disabled }"
            :disabled="tool.disabled"
            @click="activeTool = tool.key"
          >
            <Icon :icon="tool.icon" />
            <span>{{ tool.label }}</span>
            <el-tag v-if="tool.tag" size="small" type="danger" effect="plain">{{ tool.tag }}</el-tag>
          </button>
        </div>
      </aside>

      <main class="tool-main">
        <ContentWrap>
          <div class="tool-heading">
            <div>
              <div class="tool-title">{{ currentTool?.label }}</div>
              <div class="tool-desc">{{ currentTool?.desc }}</div>
            </div>
          </div>
          <UniversalTransferPanel
            v-if="activeTool === 'universal-transfer'"
            :draft="transferDraft"
            @promotion="promotionContent = $event"
          />
          <ParsePanel
            v-else-if="activeTool === 'parse'"
            @transfer="handleTransferDraft"
          />
          <GoodsSquarePanel
            v-else-if="activeTool === 'goods-square'"
            @transfer="handleTransferDraft"
          />
          <OwnershipCheckPanel
            v-else-if="activeTool === 'ownership-check'"
          />
          <CouponQueryPanel
            v-else-if="activeTool === 'coupon-query'"
            @transfer="handleTransferDraft"
          />
          <CashGiftPanel
            v-else-if="activeTool === 'cash-gift'"
            @promotion="promotionContent = $event"
          />
          <PromoImagePanel
            v-else-if="activeTool === 'promo-image'"
            @promotion="promotionContent = $event"
          />
          <div v-else class="coming-soon">
            <Icon icon="ep:tools" />
            <span>该工具已规划，后续版本接入真实能力。</span>
          </div>
        </ContentWrap>
      </main>

      <aside class="result-side">
        <ContentWrap>
          <PromotionResultEditor v-model:content="promotionContent" />
        </ContentWrap>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import CashGiftPanel from './components/CashGiftPanel.vue'
import CouponQueryPanel from './components/CouponQueryPanel.vue'
import GoodsSquarePanel from './components/GoodsSquarePanel.vue'
import OwnershipCheckPanel from './components/OwnershipCheckPanel.vue'
import ParsePanel from './components/ParsePanel.vue'
import PromotionResultEditor from './components/PromotionResultEditor.vue'
import PromoImagePanel from './components/PromoImagePanel.vue'
import UniversalTransferPanel from './components/UniversalTransferPanel.vue'

type ToolKey =
  | 'universal-transfer'
  | 'parse'
  | 'goods-square'
  | 'ownership-check'
  | 'coupon-query'
  | 'copy-editor'
  | 'promo-image'
  | 'cash-gift'

const route = useRoute()
const promotionContent = ref('')
const transferDraft = ref<{ platformCode: string; originalContent: string; vendorCode?: string } | null>(null)

const toolGroups: Array<{
  label: string
  children: Array<{ key: ToolKey; label: string; icon: string; desc: string; tag?: string; disabled?: boolean }>
}> = [
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

const allTools = computed(() => toolGroups.flatMap((group) => group.children))
const resolveToolKey = (value: unknown): ToolKey => {
  const toolKey = Array.isArray(value) ? value[0] : value
  const matched = allTools.value.find((tool) => tool.key === toolKey && !tool.disabled)
  return matched?.key || 'universal-transfer'
}

const activeTool = ref<ToolKey>(resolveToolKey(route.query.tool))
const currentTool = computed(() => allTools.value.find((tool) => tool.key === activeTool.value))

watch(
  () => route.query.tool,
  (tool) => {
    activeTool.value = resolveToolKey(tool)
  }
)

const handleTransferDraft = (value: { platformCode: string; originalContent: string; vendorCode?: string }) => {
  transferDraft.value = { ...value }
  activeTool.value = 'universal-transfer'
}
</script>

<style scoped>
.toolbox-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.toolbox-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-title {
  color: var(--el-text-color-primary);
  font-size: 22px;
  font-weight: 700;
}

.page-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.header-tags,
.toolbox-layout {
  display: flex;
  gap: 10px;
}

.toolbox-alert {
  margin-bottom: 2px;
}

.toolbox-layout {
  align-items: flex-start;
}

.tool-nav {
  position: sticky;
  top: 72px;
  width: 210px;
  flex: 0 0 210px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.tool-group + .tool-group {
  margin-top: 14px;
}

.group-title {
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-weight: 600;
}

.tool-item {
  display: flex;
  width: 100%;
  min-height: 36px;
  align-items: center;
  gap: 8px;
  padding: 7px 8px;
  border: 0;
  border-radius: 6px;
  margin-bottom: 4px;
  background: transparent;
  color: var(--el-text-color-primary);
  cursor: pointer;
  font-size: 14px;
  text-align: left;
}

.tool-item.active {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
  font-weight: 600;
}

.tool-item.disabled {
  color: var(--el-text-color-placeholder);
  cursor: not-allowed;
}

.tool-main {
  min-width: 0;
  flex: 1 1 auto;
}

.result-side {
  width: 330px;
  flex: 0 0 330px;
}

.tool-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.tool-title {
  color: var(--el-text-color-primary);
  font-size: 17px;
  font-weight: 700;
}

.tool-desc {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.coming-soon {
  display: flex;
  min-height: 240px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 1180px) {
  .toolbox-layout {
    flex-direction: column;
  }

  .tool-nav,
  .result-side {
    position: static;
    width: 100%;
    flex: none;
  }

  .tool-nav {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
    gap: 12px;
  }

  .tool-group + .tool-group {
    margin-top: 0;
  }
}

@media (max-width: 768px) {
  .toolbox-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .header-tags {
    flex-wrap: wrap;
  }
}
</style>
