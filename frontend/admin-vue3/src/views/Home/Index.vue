<template>
  <div class="cps-home">
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-content">
        <div class="welcome-copy">
          <el-avatar :src="avatar" :size="56"><img src="@/assets/imgs/avatar.gif" alt="" /></el-avatar>
          <div><div class="welcome-title">{{ t('workplace.welcome') }} {{ username }}，欢迎回到 CPS 工作台</div><div class="welcome-subtitle">实时掌握订单、佣金与返利表现，今天也要高效运营。</div></div>
        </div><div class="welcome-date">数据更新时间：{{ updateTime }}</div>
      </div>
    </el-card>

    <el-row :gutter="12" class="metric-row">
      <el-col v-for="item in metrics" :key="item.key" :xl="5" :lg="8" :md="12" :sm="12" :xs="24">
        <el-card shadow="never" class="metric-card" :class="`metric-${item.tone}`">
          <div class="metric-icon"><Icon :icon="item.icon" :size="21" /></div><div><div class="metric-label">{{ item.label }}</div><CountTo class="metric-value" :start-val="0" :end-val="item.value" :duration="900" :decimals="item.decimals" /><div class="metric-foot">较昨日 <span :class="item.change >= 0 ? 'up' : 'down'">{{ item.changeText }}</span></div></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" class="main-row">
      <el-col :xl="16" :lg="16" :md="24" :sm="24" :xs="24">
        <el-card shadow="never" class="panel-card trend-card">
          <template #header><div class="panel-header"><div><div class="panel-title">经营趋势</div><div class="panel-caption">近 7 天 CPS 订单与佣金变化</div></div><el-button link type="primary" @click="router.push('/cps/statistics')">查看完整报表 <Icon icon="ep:arrow-right" /></el-button></div></template>
          <Echart :options="trendOptions" height="300px" />
        </el-card>
        <el-row :gutter="12">
          <el-col :span="12" :xs="24"><el-card shadow="never" class="panel-card">
            <template #header><div class="panel-header"><span class="panel-title">平台佣金分布</span><span class="panel-caption">近 7 天</span></div></template>
            <div v-if="platforms.length" class="platform-list"><div v-for="platform in platforms" :key="platform.name" class="platform-item"><div class="platform-name"><span class="platform-dot" :style="{ background: platform.color }"></span>{{ platform.name }} <span class="platform-value">¥ {{ platform.value.toFixed(2) }}</span></div><el-progress :percentage="platform.percent" :show-text="false" :stroke-width="6" :color="platform.color" /></div></div><el-empty v-else description="暂无平台数据" :image-size="60" />
          </el-card></el-col>
          <el-col :span="12" :xs="24"><el-card shadow="never" class="panel-card">
            <template #header><div class="panel-header"><span class="panel-title">运营提醒</span><el-tag type="success" effect="plain" size="small">{{ pendingCount }} 条待处理</el-tag></div></template>
            <div class="activity-list"><div v-for="item in activities" :key="item.title" class="activity-item"><div class="activity-mark" :class="`mark-${item.type}`"><Icon :icon="item.icon" :size="15" /></div><div><div class="activity-title">{{ item.title }}</div><div class="activity-time">{{ item.time }}</div></div></div></div>
          </el-card></el-col>
        </el-row>
      </el-col>

      <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24">
        <el-card shadow="never" class="panel-card mobile-panel">
          <template #header><div class="panel-header"><div><div class="panel-title">移动端 H5 演示</div><div class="panel-caption">预览 CPS 会员端核心页面</div></div><el-button link type="primary" @click="openMobile">打开移动端</el-button></div></template>
          <div class="phone-stage"><div class="phone-shell"><div class="phone-notch"></div><div class="phone-screen"><div class="phone-topbar"><span>Agentic CPS</span><Icon icon="ep:search" :size="16" /></div><div class="phone-banner"><div class="banner-title">今日返利</div><div class="banner-amount">¥ {{ mobileRebate }}</div><div class="banner-tip">购物省钱 · 分享赚钱</div></div><div class="phone-grid"><div v-for="item in mobileActions" :key="item.label" class="phone-action"><Icon :icon="item.icon" :size="20" /><span>{{ item.label }}</span></div></div><div class="phone-section-title">精选好物 <span>查看更多 ›</span></div><div class="phone-goods"><div v-for="item in mobileGoods" :key="item.title" class="phone-good"><div class="good-image" :style="{ background: item.color }">{{ item.badge }}</div><div class="good-title">{{ item.title }}</div><div class="good-price">{{ item.price }}</div></div></div><div class="phone-tabs"><div v-for="item in mobileTabs" :key="item.label" class="phone-tab" :class="{ active: activeMobileTab === item.label }" @click="activeMobileTab = item.label"><Icon :icon="item.icon" :size="17" /><span>{{ item.label }}</span></div></div></div></div><div class="phone-tip">点击底部导航可切换演示状态</div></div>
        </el-card>
        <el-card shadow="never" class="panel-card shortcut-card"><template #header><div class="panel-header"><span class="panel-title">常用功能</span></div></template><div class="shortcut-grid"><div v-for="item in shortcuts" :key="item.name" class="shortcut-item" @click="router.push(item.url)"><span class="shortcut-icon" :style="{ color: item.color, background: `${item.color}15` }"><Icon :icon="item.icon" /></span><span>{{ item.name }}</span></div></div></el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" setup>
import { EChartsOption } from 'echarts'
import dayjs from 'dayjs'
import { CpsStatisticsApi, type CpsDashboardVO, type CpsPlatformSummaryVO, type CpsTrendVO } from '@/api/cps/statistics'
import { useUserStore } from '@/store/modules/user'
import { Echart } from '@/components/Echart'
import { useRouter } from 'vue-router'

defineOptions({ name: 'Index' })
const { t } = useI18n(); const router = useRouter(); const userStore = useUserStore(); const avatar = userStore.getUser.avatar; const username = userStore.getUser.nickname
const updateTime = ref(dayjs().format('MM月DD日 HH:mm')); const activeMobileTab = ref('首页'); const pendingCount = ref(3)
type Metric = { key: string; label: string; value: number; decimals: number; change: number; changeText: string; icon: string; tone: string }
const metrics = ref<Metric[]>([
  { key: 'orders', label: '今日订单数', value: 0, decimals: 0, change: 0, changeText: '—', icon: 'ep:shopping-cart', tone: 'blue' }, { key: 'commission', label: '今日佣金（元）', value: 0, decimals: 2, change: 0, changeText: '—', icon: 'ep:money', tone: 'orange' }, { key: 'rebate', label: '今日返利（元）', value: 0, decimals: 2, change: 0, changeText: '—', icon: 'ep:wallet', tone: 'green' }, { key: 'profit', label: '今日利润（元）', value: 0, decimals: 2, change: 0, changeText: '—', icon: 'ep:trend-charts', tone: 'purple' }, { key: 'members', label: '活跃会员数', value: 0, decimals: 0, change: 0, changeText: '实时', icon: 'ep:user', tone: 'cyan' }
])
const trendOptions = ref<EChartsOption>({ tooltip: { trigger: 'axis' }, legend: { data: ['订单数', '佣金（元）'], right: 0 }, grid: { left: 8, right: 12, top: 35, bottom: 8, containLabel: true }, xAxis: { type: 'category', boundaryGap: false, data: [] }, yAxis: [{ type: 'value', name: '金额（元）' }, { type: 'value', name: '订单', splitLine: { show: false } }], series: [] })
const platforms = ref<{ name: string; value: number; percent: number; color: string }[]>([]); const colors = ['#2563eb', '#f97316', '#22c55e', '#8b5cf6', '#06b6d4']
const activities = [{ title: '有 3 笔订单待确认收货', time: '建议及时跟进订单状态', type: 'warning', icon: 'ep:bell' }, { title: '本月返利结算进度 86%', time: '最后同步：刚刚', type: 'success', icon: 'ep:circle-check' }, { title: '平台连接状态正常', time: '淘宝联盟 · 京东 · 多多', type: 'info', icon: 'ep:connection' }]
const shortcuts = [{ name: '订单管理', icon: 'ep:list', url: '/cps/order', color: '#2563eb' }, { name: '商品选品', icon: 'ep:goods', url: '/cps/goods-square', color: '#f97316' }, { name: '推广链接', icon: 'ep:link', url: '/cps/toolbox', color: '#22c55e' }, { name: '返利结算', icon: 'ep:wallet', url: '/cps/settlement', color: '#8b5cf6' }]
const mobileActions = [{ label: '搜好物', icon: 'ep:search' }, { label: '比价', icon: 'ep:data-analysis' }, { label: '返利', icon: 'ep:wallet' }, { label: '邀请', icon: 'ep:share' }]; const mobileGoods = [{ title: '夏日清凉好物', price: '最高返 ¥18', badge: '精选', color: '#dbeafe' }, { title: '居家生活专场', price: '最高返 ¥26', badge: '热卖', color: '#ffedd5' }]; const mobileTabs = [{ label: '首页', icon: 'ep:home-filled' }, { label: '精选', icon: 'ep:star' }, { label: '订单', icon: 'ep:tickets' }, { label: '我的', icon: 'ep:user' }]; const mobileRebate = computed(() => metrics.value[2].value.toFixed(2))
const money = (value: unknown) => Number(value || 0); const changeText = (today: number, yesterday: number) => !yesterday ? (today ? '新增' : '—') : `${today >= yesterday ? '+' : ''}${(((today - yesterday) / yesterday) * 100).toFixed(1)}%`
async function loadDashboard() { try { const data = (await CpsStatisticsApi.getDashboard()) as unknown as CpsDashboardVO; const values = [data.todayOrderCount, money(data.todayCommission), money(data.todayRebate), money(data.todayProfit), data.todayActiveMembers]; const previous = [data.yesterdayOrderCount, money(data.yesterdayCommission), money(data.yesterdayRebate), money(data.yesterdayProfit), 0]; const changes = [changeText(values[0], previous[0]), changeText(values[1], previous[1]), changeText(values[2], previous[2]), changeText(values[3], previous[3]), '实时']; metrics.value = metrics.value.map((item, index) => ({ ...item, value: values[index] || 0, change: (values[index] || 0) - previous[index], changeText: changes[index] })) } catch { /* 空态 */ } }
async function loadTrend() { try { const end = dayjs().format('YYYY-MM-DD'); const start = dayjs().subtract(6, 'day').format('YYYY-MM-DD'); const data = (await CpsStatisticsApi.getTrend({ startDate: start, endDate: end, platformCode: 'total' })) as unknown as CpsTrendVO; trendOptions.value = { ...trendOptions.value, xAxis: { ...(trendOptions.value.xAxis as object), data: data.dates || [] }, series: [{ name: '订单数', type: 'line', smooth: true, yAxisIndex: 1, data: data.orderCounts || [], areaStyle: { opacity: 0.08 }, itemStyle: { color: '#2563eb' } }, { name: '佣金（元）', type: 'line', smooth: true, data: data.commissions || [], itemStyle: { color: '#f97316' } }] } } catch { /* 空图表 */ } }
async function loadPlatforms() { try { const end = dayjs().format('YYYY-MM-DD'); const start = dayjs().subtract(6, 'day').format('YYYY-MM-DD'); const data = (await CpsStatisticsApi.getPlatformSummary({ startDate: start, endDate: end })) as unknown as CpsPlatformSummaryVO[]; const total = data.reduce((sum, item) => sum + money(item.commissionAmount), 0); platforms.value = data.slice(0, 5).map((item, index) => ({ name: item.platformName || item.platformCode, value: money(item.commissionAmount), percent: total ? Math.round((money(item.commissionAmount) / total) * 100) : 0, color: colors[index] })) } catch { platforms.value = [] } }
function openMobile() { const domain = import.meta.env.VITE_MALL_H5_DOMAIN || window.location.origin; window.open(`${domain}?page=/pages/cps/index`, '_blank') }
onMounted(async () => { await Promise.all([loadDashboard(), loadTrend(), loadPlatforms()]); updateTime.value = dayjs().format('MM月DD日 HH:mm') })
</script>

<style scoped>
.cps-home { min-height: 100%; padding-bottom: 16px; }.welcome-card, .panel-card, .metric-card { border: 1px solid var(--el-border-color-light); border-radius: 10px; }.welcome-card { background: linear-gradient(115deg, #eff6ff, #fff 62%); }.welcome-content, .welcome-copy, .panel-header { display: flex; align-items: center; justify-content: space-between; }.welcome-copy { justify-content: flex-start; gap: 14px; }.welcome-title { color: #172554; font-size: 19px; font-weight: 600; }.welcome-subtitle, .welcome-date, .panel-caption { color: #64748b; font-size: 12px; }.welcome-subtitle { margin-top: 7px; }.welcome-date { align-self: flex-start; margin-top: 4px; }.metric-row, .main-row { margin-top: 12px; }.metric-card { display: flex; align-items: center; min-height: 105px; padding: 18px; }.metric-icon { display: grid; place-items: center; width: 42px; height: 42px; margin-right: 12px; border-radius: 12px; }.metric-blue .metric-icon { color: #2563eb; background: #dbeafe; }.metric-orange .metric-icon { color: #f97316; background: #ffedd5; }.metric-green .metric-icon { color: #16a34a; background: #dcfce7; }.metric-purple .metric-icon { color: #7c3aed; background: #ede9fe; }.metric-cyan .metric-icon { color: #0891b2; background: #cffafe; }.metric-label { color: #64748b; font-size: 13px; }.metric-value { display: block; margin-top: 4px; color: #0f172a; font-size: 24px; font-weight: 650; }.metric-foot { margin-top: 3px; color: #94a3b8; font-size: 11px; }.up { color: #16a34a; }.down { color: #ef4444; }.panel-card { margin-bottom: 12px; }.panel-card :deep(.el-card__header) { padding: 15px 18px; }.panel-card :deep(.el-card__body) { padding: 16px 18px; }.panel-title { color: #1e293b; font-size: 15px; font-weight: 600; }.trend-card :deep(.el-card__body) { padding-top: 4px; }.platform-item + .platform-item { margin-top: 13px; }.platform-name { color: #475569; font-size: 13px; }.platform-dot { display: inline-block; width: 8px; height: 8px; margin-right: 7px; border-radius: 50%; }.platform-value { float: right; color: #334155; font-size: 12px; }.platform-item :deep(.el-progress) { clear: both; padding-top: 5px; }.activity-item { display: flex; gap: 10px; padding: 4px 0 14px; }.activity-item + .activity-item { padding-top: 14px; border-top: 1px solid #f1f5f9; }.activity-mark { display: grid; flex: 0 0 28px; place-items: center; height: 28px; border-radius: 8px; }.mark-warning { color: #d97706; background: #fef3c7; }.mark-success { color: #16a34a; background: #dcfce7; }.mark-info { color: #2563eb; background: #dbeafe; }.activity-title { color: #334155; font-size: 13px; }.activity-time { margin-top: 4px; color: #94a3b8; font-size: 11px; }.mobile-panel :deep(.el-card__body) { padding: 12px; }.phone-stage { display: flex; flex-direction: column; align-items: center; }.phone-shell { width: 238px; padding: 7px; border: 3px solid #1e293b; border-radius: 30px; background: #1e293b; box-shadow: 0 10px 24px rgb(15 23 42 / 16%); }.phone-notch { width: 74px; height: 14px; margin: -1px auto 2px; border-radius: 0 0 10px 10px; background: #1e293b; }.phone-screen { position: relative; overflow: hidden; min-height: 405px; border-radius: 21px; background: #f8fafc; }.phone-topbar { display: flex; justify-content: space-between; padding: 15px 13px 10px; color: #fff; font-size: 12px; background: linear-gradient(135deg, #2563eb, #60a5fa); }.phone-banner { margin: 9px; padding: 14px; border-radius: 12px; color: #fff; background: linear-gradient(135deg, #2563eb, #38bdf8); }.banner-title { font-size: 11px; opacity: .9; }.banner-amount { margin-top: 5px; font-size: 25px; font-weight: 700; }.banner-tip { margin-top: 3px; font-size: 10px; opacity: .8; }.phone-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 4px; padding: 3px 9px 8px; }.phone-action { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #475569; font-size: 10px; }.phone-section-title { display: flex; justify-content: space-between; padding: 8px 11px 5px; color: #334155; font-size: 12px; font-weight: 600; }.phone-section-title span { color: #94a3b8; font-size: 10px; font-weight: 400; }.phone-goods { display: grid; grid-template-columns: repeat(2, 1fr); gap: 7px; padding: 0 9px; }.phone-good { overflow: hidden; border-radius: 8px; background: #fff; }.good-image { display: grid; height: 66px; place-items: center; color: #1e3a8a; font-size: 12px; font-weight: 600; }.good-title { overflow: hidden; padding: 5px 6px 1px; color: #475569; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }.good-price { padding: 1px 6px 7px; color: #ef4444; font-size: 10px; }.phone-tabs { position: absolute; right: 0; bottom: 0; left: 0; display: grid; grid-template-columns: repeat(4, 1fr); padding: 8px 5px 6px; background: rgb(255 255 255 / 95%); border-top: 1px solid #e2e8f0; }.phone-tab { display: flex; flex-direction: column; align-items: center; gap: 2px; color: #94a3b8; font-size: 9px; cursor: pointer; }.phone-tab.active { color: #2563eb; }.phone-tip { margin-top: 9px; color: #94a3b8; font-size: 11px; }.shortcut-card { margin-top: 12px; }.shortcut-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px 6px; }.shortcut-item { display: flex; flex-direction: column; align-items: center; gap: 6px; color: #475569; font-size: 12px; cursor: pointer; }.shortcut-icon { display: grid; place-items: center; width: 34px; height: 34px; border-radius: 10px; font-size: 17px; }
@media (max-width: 768px) { .welcome-content { display: block; }.welcome-date { margin-top: 12px; }.welcome-title { font-size: 16px; }.metric-card { margin-bottom: 12px; }.phone-shell { width: 260px; } }

/* 首页采用紧凑栅格，避免指标卡片在宽屏下意外换行并产生大片空白 */
.metric-row { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 12px; margin-right: 0 !important; margin-left: 0 !important; }
.metric-row > .el-col { width: auto !important; max-width: none !important; padding-right: 0 !important; padding-left: 0 !important; }
.metric-card { min-height: 92px; padding: 13px 16px; }
.metric-value { font-size: 22px; }
.main-row { margin-top: 12px; }
.trend-card :deep(.el-card__body) { min-height: 250px; }
.mobile-panel { margin-bottom: 8px; }
.mobile-panel :deep(.el-card__body) { padding-top: 8px; padding-bottom: 9px; }
.phone-tip { margin-top: 5px; }
.shortcut-card { margin-top: 8px; }
.shortcut-card :deep(.el-card__body) { padding-top: 12px; padding-bottom: 12px; }
@media (max-width: 1200px) { .metric-row { grid-template-columns: repeat(3, minmax(0, 1fr)); } }
@media (max-width: 768px) { .metric-row { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 8px; } .metric-card { margin-bottom: 0; } }
@media (max-width: 480px) { .metric-row { grid-template-columns: 1fr; } }
</style>
