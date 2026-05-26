<template>
  <ContentWrap>
    <div class="page-toolbar">
      <div>
        <div class="text-18px font-600">CPX 看板</div>
        <div class="mt-4px text-12px text-gray-500">
          CPS 成交任务优先，汇总曝光、点击、线索、动作、转化和结算
        </div>
      </div>
      <el-button :loading="loading" @click="loadSummary">
        <Icon icon="ep:refresh" class="mr-5px" /> 刷新
      </el-button>
    </div>
  </ContentWrap>

  <el-row :gutter="16">
    <el-col v-for="item in statCards" :key="item.label" :xs="24" :sm="12" :lg="6">
      <ContentWrap>
        <div class="stat-card">
          <div>
            <div class="text-12px text-gray-500">{{ item.label }}</div>
            <div class="mt-8px text-24px font-700">{{ item.value }}</div>
          </div>
          <Icon :icon="item.icon" class="text-28px text-gray-400" />
        </div>
      </ContentWrap>
    </el-col>
  </el-row>

  <el-row :gutter="16">
    <el-col :xs="24" :lg="14">
      <ContentWrap>
        <template #header>
          <span>推广漏斗</span>
        </template>
        <el-table v-loading="loading" :data="funnelRows" stripe>
          <el-table-column label="阶段" prop="name" min-width="120" />
          <el-table-column label="指标" prop="value" width="140" />
          <el-table-column label="说明" prop="desc" min-width="220" />
        </el-table>
      </ContentWrap>
    </el-col>
    <el-col :xs="24" :lg="10">
      <ContentWrap>
        <template #header>
          <span>任务方式分布</span>
        </template>
        <div v-loading="loading" class="method-list">
          <div v-for="item in methodRows" :key="item.method" class="method-row">
            <span>{{ promotionMethodLabel(item.method) }}</span>
            <div class="method-count">
              <el-progress
                :percentage="item.percentage"
                :show-text="false"
                :stroke-width="8"
                class="method-progress"
              />
              <span>{{ item.count }}</span>
            </div>
          </div>
        </div>
      </ContentWrap>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import {
  CPX_PROMOTION_METHOD_OPTIONS,
  CpxDashboardRespVO,
  CpxPromotionMethod,
  CpxTaskApi
} from '@/api/cpx/task'

defineOptions({ name: 'CpxDashboard' })

const loading = ref(false)
const summary = ref<CpxDashboardRespVO>({
  taskCount: 0,
  onlineTaskCount: 0,
  taskCountByMethod: {},
  impressionCount: 0,
  clickCount: 0,
  leadCount: 0,
  actionCount: 0,
  conversionCount: 0,
  settlementCount: 0,
  settlementAmount: 0,
  rewardAmount: 0
})

const statCards = computed(() => [
  { label: '任务总数', value: summary.value.taskCount, icon: 'ep:promotion' },
  { label: '启用任务', value: summary.value.onlineTaskCount, icon: 'ep:open' },
  { label: '结算金额', value: amount(summary.value.settlementAmount), icon: 'ep:money' },
  { label: '会员奖励', value: amount(summary.value.rewardAmount), icon: 'ep:wallet' }
])

const funnelRows = computed(() => [
  { name: '曝光', value: summary.value.impressionCount, desc: 'CPM 有效曝光，已过滤无效曝光' },
  { name: '点击', value: summary.value.clickCount, desc: 'CPC / oCPC 有效点击' },
  { name: '线索', value: summary.value.leadCount, desc: 'CPL 线索提交与审核入口' },
  { name: '动作', value: summary.value.actionCount, desc: 'CPA / oCPA 动作回传' },
  { name: '转化', value: summary.value.conversionCount, desc: '可进入结算状态机的转化' },
  { name: '结算', value: summary.value.settlementCount, desc: '收入、返利、奖励和扣回记录' }
])

const methodRows = computed(() => {
  const total = Math.max(summary.value.taskCount, 1)
  return CPX_PROMOTION_METHOD_OPTIONS.map((option) => {
    const count = summary.value.taskCountByMethod[option.value] || 0
    return {
      method: option.value as CpxPromotionMethod,
      count,
      percentage: Math.round((count / total) * 100)
    }
  }).filter((item) => item.count > 0)
})

const promotionMethodLabel = (method: CpxPromotionMethod) => {
  return CPX_PROMOTION_METHOD_OPTIONS.find((item) => item.value === method)?.label || method
}

const amount = (value?: number) => {
  return `¥${((value || 0) / 100).toFixed(2)}`
}

const loadSummary = async () => {
  loading.value = true
  try {
    summary.value = await CpxTaskApi.getDashboardSummary()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadSummary()
})
</script>

<style scoped>
.page-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.stat-card {
  display: flex;
  min-height: 84px;
  align-items: center;
  justify-content: space-between;
}

.method-list {
  display: flex;
  min-height: 276px;
  flex-direction: column;
  gap: 14px;
}

.method-row {
  display: grid;
  grid-template-columns: minmax(120px, 160px) 1fr;
  align-items: center;
  gap: 12px;
}

.method-count {
  display: flex;
  align-items: center;
  gap: 10px;
}

.method-progress {
  flex: 1;
}
</style>
