<template>
  <ContentWrap>
    <!-- 顶部操作栏 -->
    <div class="mb-4 flex items-center justify-between">
      <span class="text-lg font-semibold">CPS 运营数据看板</span>
      <div class="flex items-center gap-2">
        <el-button-group>
          <el-button :type="dateRange === 7 ? 'primary' : ''" @click="setDateRange(7)">近7天</el-button>
          <el-button :type="dateRange === 30 ? 'primary' : ''" @click="setDateRange(30)">近30天</el-button>
          <el-button :type="dateRange === 0 ? 'primary' : ''" @click="setDateRange(0)">本月</el-button>
        </el-button-group>
        <el-date-picker
          v-model="customRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="width: 240px"
          @change="onCustomRangeChange"
        />
      </div>
    </div>

    <!-- 指标卡片区 -->
    <el-row :gutter="16" class="mb-6">
      <el-col :span="5" v-for="card in dashboardCards" :key="card.key">
        <el-card shadow="never" class="stat-card">
          <div class="stat-title">{{ card.label }}</div>
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-compare" :class="card.trend > 0 ? 'trend-up' : card.trend < 0 ? 'trend-down' : ''">
            <template v-if="card.trend !== null">
              <span>{{ card.trend > 0 ? '↑' : card.trend < 0 ? '↓' : '—' }}</span>
              <span>昨日 {{ card.yestValue }}</span>
            </template>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 趋势折线图 -->
    <el-card shadow="never" class="mb-6">
      <template #header>
        <div class="flex items-center justify-between">
          <span>数据趋势</span>
          <div class="flex items-center gap-2">
            <span class="text-sm text-gray-500">平台：</span>
            <el-select v-model="trendPlatform" style="width: 130px" @change="loadTrend">
              <el-option label="全平台" value="total" />
              <el-option label="淘宝联盟" value="taobao" />
              <el-option label="京东联盟" value="jd" />
              <el-option label="拼多多" value="pdd" />
              <el-option label="抖音联盟" value="douyin" />
            </el-select>
            <el-checkbox-group v-model="trendMetrics" @change="updateTrendChart">
              <el-checkbox label="orderCounts">订单数</el-checkbox>
              <el-checkbox label="commissions">佣金</el-checkbox>
              <el-checkbox label="rebates">返利</el-checkbox>
              <el-checkbox label="profits">利润</el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
      </template>
      <Echart :options="lineOptions" height="360px" />
    </el-card>

    <!-- 平台占比饼图 -->
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>各平台佣金占比</template>
          <Echart :options="commissionPieOptions" height="320px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>各平台订单占比</template>
          <Echart :options="orderPieOptions" height="320px" />
        </el-card>
      </el-col>
    </el-row>
  </ContentWrap>
</template>

<script setup lang="ts">
import { EChartsOption } from 'echarts'
import { Echart } from '@/components/Echart'
import { CpsStatisticsApi, type CpsDashboardVO, type CpsTrendVO, type CpsPlatformSummaryVO } from '@/api/cps/statistics'
import dayjs from 'dayjs'

defineOptions({ name: 'CpsStatistics' })

// ======================== 日期区间 ========================
const dateRange = ref<number>(7)
const customRange = ref<[string, string] | null>(null)

function getDateRange(): [string, string] {
  const end = dayjs().subtract(0, 'day').format('YYYY-MM-DD')
  if (dateRange.value === 0) {
    return [dayjs().startOf('month').format('YYYY-MM-DD'), end]
  }
  return [dayjs().subtract(dateRange.value - 1, 'day').format('YYYY-MM-DD'), end]
}

function setDateRange(days: number) {
  dateRange.value = days
  customRange.value = null
  loadAll()
}

function onCustomRangeChange(val: [string, string] | null) {
  if (val) {
    dateRange.value = -1
    loadAll()
  }
}

function currentRange(): [string, string] {
  if (customRange.value && customRange.value.length === 2) {
    return customRange.value as [string, string]
  }
  return getDateRange()
}

// ======================== 看板指标卡片 ========================
interface StatCard {
  key: string
  label: string
  value: string
  yestValue: string
  trend: number | null
}

const dashboardCards = ref<StatCard[]>([
  { key: 'order', label: '今日订单数', value: '-', yestValue: '-', trend: null },
  { key: 'commission', label: '今日佣金（元）', value: '-', yestValue: '-', trend: null },
  { key: 'rebate', label: '今日返利（元）', value: '-', yestValue: '-', trend: null },
  { key: 'profit', label: '今日利润（元）', value: '-', yestValue: '-', trend: null },
  { key: 'member', label: '今日活跃会员', value: '-', yestValue: '-', trend: null }
])

function updateDashboard(data: CpsDashboardVO) {
  const fmt = (v: number | undefined) => (v !== undefined && v !== null ? v : 0)
  const fmtMoney = (v: number | undefined) => Number(fmt(v)).toFixed(2)
  const trend = (today: number, yest: number) => (yest === 0 ? 0 : today - yest)

  dashboardCards.value[0] = {
    key: 'order', label: '今日订单数',
    value: String(fmt(data.todayOrderCount)),
    yestValue: String(fmt(data.yesterdayOrderCount)),
    trend: trend(data.todayOrderCount, data.yesterdayOrderCount)
  }
  dashboardCards.value[1] = {
    key: 'commission', label: '今日佣金（元）',
    value: fmtMoney(data.todayCommission),
    yestValue: fmtMoney(data.yesterdayCommission),
    trend: trend(Number(data.todayCommission), Number(data.yesterdayCommission))
  }
  dashboardCards.value[2] = {
    key: 'rebate', label: '今日返利（元）',
    value: fmtMoney(data.todayRebate),
    yestValue: fmtMoney(data.yesterdayRebate),
    trend: trend(Number(data.todayRebate), Number(data.yesterdayRebate))
  }
  dashboardCards.value[3] = {
    key: 'profit', label: '今日利润（元）',
    value: fmtMoney(data.todayProfit),
    yestValue: fmtMoney(data.yesterdayProfit),
    trend: trend(Number(data.todayProfit), Number(data.yesterdayProfit))
  }
  dashboardCards.value[4] = {
    key: 'member', label: '今日活跃会员',
    value: String(fmt(data.todayActiveMembers)),
    yestValue: '-',
    trend: null
  }
}

// ======================== 趋势折线图 ========================
const trendPlatform = ref('total')
const trendMetrics = ref<string[]>(['orderCounts', 'commissions', 'profits'])
const trendData = ref<CpsTrendVO | null>(null)

const metricConfig: Record<string, { label: string; yAxisIndex: number }> = {
  orderCounts: { label: '订单数', yAxisIndex: 1 },
  commissions: { label: '佣金（元）', yAxisIndex: 0 },
  rebates: { label: '返利（元）', yAxisIndex: 0 },
  profits: { label: '利润（元）', yAxisIndex: 0 }
}

const lineOptions = ref<EChartsOption>({})

function updateTrendChart() {
  if (!trendData.value) return
  const data = trendData.value
  const series = trendMetrics.value.map((key) => ({
    name: metricConfig[key].label,
    type: 'line' as const,
    smooth: true,
    yAxisIndex: metricConfig[key].yAxisIndex,
    data: (data[key as keyof CpsTrendVO] as number[]) || []
  }))

  lineOptions.value = {
    tooltip: { trigger: 'axis' },
    legend: { data: trendMetrics.value.map((k) => metricConfig[k].label) },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: data.dates },
    yAxis: [
      { type: 'value', name: '金额（元）', axisLabel: { formatter: '{value}' } },
      { type: 'value', name: '订单数', axisLabel: { formatter: '{value}' } }
    ],
    series
  }
}

async function loadTrend() {
  const [startDate, endDate] = currentRange()
  const res = await CpsStatisticsApi.getTrend({ startDate, endDate, platformCode: trendPlatform.value })
  trendData.value = res as unknown as CpsTrendVO
  updateTrendChart()
}

// ======================== 平台占比饼图 ========================
const commissionPieOptions = ref<EChartsOption>({})
const orderPieOptions = ref<EChartsOption>({})

function updatePieCharts(list: CpsPlatformSummaryVO[]) {
  const commData = list.map((p) => ({ name: p.platformName || p.platformCode, value: Number(p.commissionAmount) }))
  const orderData = list.map((p) => ({ name: p.platformName || p.platformCode, value: p.orderCount }))

  const basePie = (title: string, data: { name: string; value: number }[]): EChartsOption => ({
    tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
    legend: { orient: 'vertical', left: 'left' },
    series: [{ name: title, type: 'pie', radius: ['40%', '70%'], data, label: { show: true, formatter: '{b}: {d}%' } }]
  })

  commissionPieOptions.value = basePie('佣金占比', commData)
  orderPieOptions.value = basePie('订单占比', orderData)
}

async function loadPlatformSummary() {
  const [startDate, endDate] = currentRange()
  const res = await CpsStatisticsApi.getPlatformSummary({ startDate, endDate })
  updatePieCharts(res as unknown as CpsPlatformSummaryVO[])
}

// ======================== 加载全部 ========================
async function loadAll() {
  await Promise.all([
    CpsStatisticsApi.getDashboard().then((res) => updateDashboard(res as unknown as CpsDashboardVO)),
    loadTrend(),
    loadPlatformSummary()
  ])
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.stat-card {
  text-align: center;
  padding: 8px 0;
}
.stat-title {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}
.stat-value {
  font-size: 26px;
  font-weight: 600;
  color: #303133;
}
.stat-compare {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
  height: 18px;
}
.trend-up {
  color: #f56c6c;
}
.trend-down {
  color: #67c23a;
}
</style>
