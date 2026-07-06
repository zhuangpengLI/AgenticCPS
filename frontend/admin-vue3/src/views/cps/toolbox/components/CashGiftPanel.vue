<template>
  <div class="cash-gift-panel">
    <el-tabs v-model="activeMode" class="cash-tabs">
      <el-tab-pane label="创建" name="create" />
      <el-tab-pane label="已创建" name="created" />
      <el-tab-pane label="待自动创建" name="pending" />
      <el-tab-pane label="媒体信息管理" name="media" />
    </el-tabs>

    <template v-if="activeMode === 'create'">
      <div class="mode-switch">
        <el-radio-group v-model="createMode" size="large">
          <el-radio-button label="cashGift">创建淘礼金</el-radio-button>
          <el-radio-button label="landing">淘礼金单页</el-radio-button>
        </el-radio-group>
        <span class="mode-hint">
          {{ createMode === 'cashGift' ? '先生成淘礼金计划，再用于社群、直播间或单页分发。' : '生成一个券+淘礼金抢购单页，用户领券时自动创建。' }}
        </span>
      </div>

      <section v-if="createMode === 'cashGift'" class="cash-layout">
        <div class="create-column">
          <div class="section-title">创建淘礼金</div>
          <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="112px">
            <el-form-item label="媒体信息" prop="mediaId" required>
              <el-select v-model="createForm.mediaId" placeholder="请选择媒体账号" class="field-wide" clearable>
                <el-option label="默认淘宝联盟媒体" value="default-media" />
                <el-option label="私域社群媒体" value="community-media" />
              </el-select>
            </el-form-item>

            <el-form-item label="商品链接" prop="goodsLink" required>
              <el-input
                v-model="createForm.goodsLink"
                class="field-wide"
                placeholder="支持淘口令、加密商品ID或商品链接"
                clearable
              />
              <div class="field-tip danger-tip">
                因淘宝联盟升级，数字ID商品链接不再支持，请使用加密商品ID链接。
              </div>
            </el-form-item>

            <el-form-item label="快速填写">
              <el-radio-group v-model="createForm.quickType">
                <el-radio label="normal">普通礼金</el-radio>
                <el-radio label="free">免单活动</el-radio>
                <el-radio label="zero">0佣福利</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="特殊活动">
              <el-radio-group v-model="createForm.specialType">
                <el-radio label="none">非特殊活动</el-radio>
                <el-radio label="first">首单礼金</el-radio>
              </el-radio-group>
              <el-link type="primary" class="ml-10px" :underline="false">特殊活动玩法教程 &gt;</el-link>
            </el-form-item>

            <el-form-item label="淘礼金名称" prop="campaignName" required>
              <el-input v-model="createForm.campaignName" class="field-medium" maxlength="30" />
            </el-form-item>

            <el-form-item label="淘礼金份数" prop="totalQuantity" required>
              <el-input-number v-model="createForm.totalQuantity" :min="1" class="number-field" controls-position="right" />
              <span class="unit-text">份</span>
            </el-form-item>

            <el-form-item label="淘礼金面额" prop="giftAmount" required>
              <el-input-number v-model="createForm.giftAmount" :min="0.01" :precision="2" class="number-field" controls-position="right" />
              <span class="unit-text">元</span>
            </el-form-item>

            <el-form-item label="单个用户可领次数" prop="perUserLimit" required>
              <el-input-number v-model="createForm.perUserLimit" :min="1" class="number-field" controls-position="right" />
              <span class="unit-text">次</span>
            </el-form-item>

            <el-form-item label="使用门槛">
              <span class="inline-label">实付款 >=</span>
              <el-input-number v-model="createForm.thresholdAmount" :min="0" :precision="2" class="short-number" controls-position="right" />
              <span class="unit-text">元可用</span>
            </el-form-item>

            <el-form-item label="领取时间" prop="receiveRange" required>
              <el-date-picker
                v-model="createForm.receiveRange"
                type="datetimerange"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="date-range"
              />
            </el-form-item>

            <el-form-item label="使用时间">
              <el-radio-group v-model="createForm.useTimeType">
                <el-radio label="custom">自定义时间可用</el-radio>
                <el-radio label="afterReceive">领取后N天可用</el-radio>
              </el-radio-group>
              <el-date-picker
                v-if="createForm.useTimeType === 'custom'"
                v-model="createForm.useRange"
                type="datetimerange"
                start-placeholder="开始时间"
                end-placeholder="结束时间"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="date-range mt-10px"
              />
            </el-form-item>

            <el-form-item label="佣金类型">
              <el-radio-group v-model="createForm.commissionType">
                <el-radio label="marketing">营销计划</el-radio>
                <el-radio label="fixed">定向计划</el-radio>
              </el-radio-group>
              <span class="field-tip orange-tip">若商品有定向计划，可在申请通过后选择定向计划。</span>
            </el-form-item>

            <el-form-item label="皮肤模板">
              <el-radio-group v-model="createForm.skinTemplate">
                <el-radio label="daily">日常模板</el-radio>
                <el-radio label="elastic">弹窗模板</el-radio>
                <el-radio label="activity">活动模板</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="创建时间" required>
              <el-radio-group v-model="createForm.createTimeType">
                <el-radio label="now">立即创建</el-radio>
                <el-radio label="scheduled">定时创建</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="批量创建">
              <el-switch v-model="createForm.batchCreate" />
            </el-form-item>
          </el-form>

          <div class="panel-actions">
            <el-button type="primary" :loading="loading" @click="handleCreatePlan">
              <Icon icon="ep:present" class="mr-5px" /> 生成淘礼金计划
            </el-button>
            <el-button :disabled="!result?.promotionContent" @click="emitCopy">
              <Icon icon="ep:edit-pen" class="mr-5px" /> 写入文案区
            </el-button>
          </div>
        </div>

        <aside class="guide-column">
          <div class="guide-block">
            <div class="guide-title">
              使用说明
              <el-link type="primary" :underline="false">使用前必读 &gt;</el-link>
            </div>
            <div class="guide-alert">《淘宝联盟玩法钱包老账户下线公告》</div>
            <ol class="guide-list">
              <li>点击查看“营销账户不存在”的解决方案。</li>
              <li>点击查看不支持创建淘礼金的商品类型。</li>
              <li>点击查看 App Key 权限不足的解决办法。</li>
              <li>点击查看如何取消 PID 白名单。</li>
            </ol>
            <el-link type="danger" :underline="false">去淘礼金专区逛逛 &gt;&gt;</el-link>
          </div>

          <div class="calculator">
            <div class="calculator-title">
              淘礼金计算器
              <Icon icon="ep:info-filled" />
            </div>
            <div class="calc-row">
              <span>商品券后价</span>
              <el-input-number v-model="calculator.couponPrice" :min="0" :precision="2" controls-position="right" />
            </div>
            <div class="calc-row">
              <span>佣金比例</span>
              <el-input-number v-model="calculator.commissionRate" :min="0" :precision="2" controls-position="right" />
            </div>
            <div class="calc-row">
              <span>单份淘礼金面额</span>
              <el-input-number v-model="createForm.giftAmount" :min="0.01" :precision="2" controls-position="right" />
            </div>
            <div class="calc-row">
              <span>淘礼金份数</span>
              <el-input-number v-model="createForm.totalQuantity" :min="1" controls-position="right" />
            </div>
            <div class="calc-summary">
              <span>单份收益</span>
              <strong>￥{{ unitProfit.toFixed(2) }}</strong>
            </div>
            <div class="calc-summary">
              <span>最大总收益</span>
              <strong>￥{{ maxProfit.toFixed(2) }}</strong>
            </div>
          </div>
        </aside>
      </section>

      <section v-else class="landing-layout">
        <div class="landing-form">
          <div class="notice-line">
            <Icon icon="ep:sunny" />
            <span>生成一个券+淘礼金的抢购单页，淘礼金无需提前创建，只需设置好比例，在用户领券时自动创建。</span>
          </div>
          <div class="landing-title-row">
            <span class="section-title">创建</span>
            <span class="red-note">关注公众号，活动页失效将做提醒。</span>
            <Icon icon="ep:thumb" />
            <Icon icon="ep:grid" />
          </div>

          <el-form ref="landingFormRef" :model="landingForm" :rules="landingRules" label-width="98px">
            <div class="landing-card">
              <div class="attention-box">
                <strong>注意事项</strong>
                <p>1、你需要先在“大淘客淘礼金工具”中完成一次任意商品的淘礼金创建。</p>
                <p>2、请保证玩法钱包中资金充足，活动中创建失败的商品将提示“已抢光”。</p>
              </div>

              <el-form-item label="媒体信息" prop="mediaId" required>
                <el-select v-model="landingForm.mediaId" placeholder="请选择媒体账号" class="landing-field" clearable>
                  <el-option label="默认淘宝联盟媒体" value="default-media" />
                  <el-option label="私域社群媒体" value="community-media" />
                </el-select>
              </el-form-item>

              <div class="step-title"><span>1</span>选择商品</div>
              <el-form-item label="商品来源">
                <el-radio-group v-model="landingForm.goodsSource">
                  <el-radio label="official">官方推荐</el-radio>
                  <el-radio label="self">自选</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item v-if="landingForm.goodsSource === 'self'" label="商品链接">
                <el-input v-model="landingForm.goodsLink" class="landing-field" placeholder="粘贴商品链接或淘口令" clearable />
              </el-form-item>

              <div class="step-title"><span>2</span>淘礼金</div>
              <el-form-item label="淘礼金的比例" prop="rebateRatio">
                <span>券后价 * 佣金比例 *</span>
                <el-input-number v-model="landingForm.rebateRatio" :min="1" :max="100" class="ratio-field" controls-position="right" />
                <span>%</span>
              </el-form-item>
              <div class="example-copy">
                例：填入 50% 表示，一个商品如果可以赚 10 元佣金，拿出 50%（即 5 元）给用户作为淘礼金抵扣。
              </div>

              <div class="step-title"><span>3</span>推广</div>
              <el-button type="primary" :loading="loading" @click="handleLandingPlan">生成抢购单页</el-button>
              <div class="landing-date-row">
                <span>有效期：</span>
                <el-date-picker v-model="landingForm.startDate" type="date" value-format="YYYY-MM-DD" />
                <span>-</span>
                <el-date-picker v-model="landingForm.endDate" type="date" value-format="YYYY-MM-DD" />
              </div>
              <div class="landing-preview">
                <div class="poster">
                  <div class="poster-title">粉丝福利·补贴清单</div>
                  <div class="poster-subtitle">低至1元包邮</div>
                  <div class="red-envelope"><span>￥</span></div>
                </div>
                <div class="copy-preview">
                  <strong>速度！</strong>
                  <p>群主发福利，每个商品额外补贴！</p>
                  <p>手慢拍大腿：</p>
                  <p>#快抢短链#</p>
                </div>
              </div>
            </div>
          </el-form>
        </div>

        <aside class="landing-result">
          <div class="result-title">
            结果
            <span>提示：点击可直接复制</span>
          </div>
          <div v-if="landingResult" class="result-content" @click="handleCopy(landingResult)">
            {{ landingResult }}
          </div>
          <div v-else class="empty-state">
            <Icon icon="ep:document" />
            <span>暂无数据</span>
          </div>
        </aside>
      </section>
    </template>

    <div v-else class="placeholder-panel">
      <Icon icon="ep:document" />
      <div>{{ emptyModeTitle }}</div>
      <span>后续接入真实数据后在这里展示。</span>
    </div>

    <div v-if="result && createMode === 'cashGift' && activeMode === 'create'" class="result-panel">
      <div class="result-header">
        <span>计划结果</span>
        <el-tag :type="result.planStatus === 'READY' ? 'success' : 'warning'" effect="plain">
          {{ result.planStatus === 'READY' ? '可进入发放准备' : '需处理风险' }}
        </el-tag>
      </div>
      <div class="result-grid">
        <div><span>总预算</span><strong>￥{{ result.budgetAmount }}</strong></div>
        <div><span>单份金额</span><strong>￥{{ result.giftAmount }}</strong></div>
        <div><span>发放份数</span><strong>{{ result.totalQuantity }}</strong></div>
        <div><span>预算缺口</span><strong>￥{{ result.budgetGap }}</strong></div>
      </div>
      <div class="checklist">
        <el-check-tag v-for="item in result.checklist" :key="item" checked>{{ item }}</el-check-tag>
      </div>
      <el-alert
        v-for="item in result.warnings"
        :key="item"
        class="mt-10px"
        type="warning"
        show-icon
        :closable="false"
        :title="item"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'
import type { FormInstance, FormRules } from 'element-plus'
import {
  CpsRebateToolboxApi,
  type CpsGoodsCashGiftPlanRespVO
} from '@/api/cps/rebateToolbox'

type CreateMode = 'cashGift' | 'landing'

const emit = defineEmits<{
  promotion: [value: string]
}>()

const message = useMessage()
const { copy } = useClipboard()
const createFormRef = ref<FormInstance>()
const landingFormRef = ref<FormInstance>()
const activeMode = ref('create')
const createMode = ref<CreateMode>('cashGift')
const loading = ref(false)
const result = ref<CpsGoodsCashGiftPlanRespVO>()
const landingResult = ref('')

const defaultStart = '2026-07-06T00:00:00'
const defaultEnd = '2026-07-07T23:59:00'

const createForm = reactive({
  mediaId: '',
  goodsLink: '',
  quickType: 'normal',
  specialType: 'none',
  campaignName: '淘礼金福利',
  totalQuantity: 1,
  giftAmount: 1,
  perUserLimit: 1,
  thresholdAmount: 0,
  receiveRange: [defaultStart, defaultEnd] as [string, string],
  useTimeType: 'custom',
  useRange: [defaultStart, defaultEnd] as [string, string],
  commissionType: 'marketing',
  skinTemplate: 'daily',
  createTimeType: 'now',
  batchCreate: false
})

const landingForm = reactive({
  mediaId: '',
  goodsSource: 'official',
  goodsLink: '',
  rebateRatio: 50,
  startDate: '2026-07-06',
  endDate: '2026-07-08'
})

const calculator = reactive({
  couponPrice: 0,
  commissionRate: 0
})

const createRules = reactive<FormRules>({
  mediaId: [{ required: true, message: '请选择媒体信息', trigger: 'change' }],
  goodsLink: [{ required: true, message: '请输入商品链接', trigger: 'blur' }],
  campaignName: [{ required: true, message: '请输入淘礼金名称', trigger: 'blur' }],
  totalQuantity: [{ required: true, message: '请输入淘礼金份数', trigger: 'blur' }],
  giftAmount: [{ required: true, message: '请输入淘礼金面额', trigger: 'blur' }],
  perUserLimit: [{ required: true, message: '请输入单个用户可领次数', trigger: 'blur' }],
  receiveRange: [{ required: true, message: '请选择领取时间', trigger: 'change' }]
})

const landingRules = reactive<FormRules>({
  mediaId: [{ required: true, message: '请选择媒体信息', trigger: 'change' }],
  rebateRatio: [{ required: true, message: '请输入淘礼金比例', trigger: 'blur' }]
})

const unitProfit = computed(() => {
  const commission = calculator.couponPrice * (calculator.commissionRate / 100)
  return Math.max(commission - createForm.giftAmount, 0)
})

const maxProfit = computed(() => unitProfit.value * createForm.totalQuantity)

const emptyModeTitle = computed(() => {
  return (
    {
      created: '已创建淘礼金',
      pending: '待自动创建',
      media: '媒体信息管理'
    }[activeMode.value] || '暂无数据'
  )
})

const handleCreatePlan = async () => {
  await createFormRef.value?.validate()
  loading.value = true
  try {
    result.value = await CpsRebateToolboxApi.planCashGift({
      templateCode: createForm.quickType,
      campaignName: createForm.campaignName,
      platformCode: 'taobao',
      goodsId: createForm.goodsLink,
      title: createForm.campaignName,
      budgetAmount: createForm.giftAmount * createForm.totalQuantity,
      giftAmount: createForm.giftAmount,
      totalQuantity: createForm.totalQuantity,
      perUserLimit: createForm.perUserLimit,
      startTime: createForm.receiveRange[0],
      endTime: createForm.receiveRange[1]
    })
    if (result.value.planStatus === 'READY') message.success('淘礼金计划已生成')
    else message.warning('计划已生成，请处理风险提示')
  } finally {
    loading.value = false
  }
}

const handleLandingPlan = async () => {
  await landingFormRef.value?.validate()
  loading.value = true
  try {
    const giftAmount = Math.max(Number((landingForm.rebateRatio / 100).toFixed(2)), 0.01)
    const response = await CpsRebateToolboxApi.planCashGift({
      templateCode: 'landing-page',
      campaignName: '淘礼金单页',
      platformCode: 'taobao',
      goodsId: landingForm.goodsLink || undefined,
      title: '粉丝福利补贴清单',
      budgetAmount: giftAmount * 100,
      giftAmount,
      totalQuantity: 100,
      perUserLimit: 1,
      startTime: `${landingForm.startDate}T00:00:00`,
      endTime: `${landingForm.endDate}T23:59:00`
    })
    landingResult.value = [
      response.promotionContent,
      `有效期：${landingForm.startDate} 至 ${landingForm.endDate}`,
      `淘礼金比例：${landingForm.rebateRatio}%`
    ].filter(Boolean).join('\n')
    emit('promotion', landingResult.value)
    message.success('抢购单页文案已生成')
  } finally {
    loading.value = false
  }
}

const emitCopy = () => {
  if (result.value?.promotionContent) emit('promotion', result.value.promotionContent)
}

const handleCopy = async (text: string) => {
  if (!text) return
  await copy(text)
  message.success('复制成功')
}
</script>

<style scoped>
.cash-gift-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cash-tabs {
  --el-tabs-header-height: 38px;
}

.mode-switch {
  display: flex;
  align-items: center;
  gap: 14px;
}

.mode-hint,
.field-tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.cash-layout,
.landing-layout {
  display: grid;
  align-items: flex-start;
  gap: 28px;
}

.cash-layout {
  grid-template-columns: minmax(560px, 1fr) 320px;
}

.landing-layout {
  grid-template-columns: minmax(580px, 1fr) 430px;
}

.create-column {
  padding-right: 24px;
  border-right: 1px dashed var(--el-border-color);
}

.section-title {
  margin-bottom: 16px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
}

.field-wide {
  max-width: 360px;
  width: 100%;
}

.field-medium {
  max-width: 250px;
  width: 100%;
}

.number-field {
  width: 250px;
}

.short-number {
  width: 150px;
}

.date-range {
  max-width: 390px;
  width: 100%;
}

.unit-text,
.inline-label {
  margin-left: 8px;
  color: var(--el-text-color-regular);
}

.danger-tip {
  width: 100%;
  margin-top: 8px;
  color: var(--el-color-danger);
}

.orange-tip {
  margin-left: 12px;
  color: #f56c00;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-left: 112px;
}

.guide-column {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.guide-title,
.calculator-title,
.result-title,
.landing-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.guide-alert {
  margin-top: 20px;
  color: var(--el-color-danger);
  font-size: 16px;
  font-weight: 700;
}

.guide-list {
  padding-left: 18px;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.8;
}

.calculator {
  width: 260px;
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.calculator-title {
  justify-content: center;
  color: var(--el-color-primary);
}

.calc-row,
.calc-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
}

.calc-row span {
  flex: 1;
  font-size: 13px;
}

.calc-row :deep(.el-input-number) {
  width: 100px;
}

.calc-summary {
  border-top: 1px solid var(--el-border-color-lighter);
  padding-top: 12px;
}

.calc-summary strong {
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.notice-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 28px;
  color: var(--el-text-color-primary);
}

.red-note,
.result-title span {
  color: var(--el-color-danger);
  font-size: 12px;
  font-weight: 400;
}

.landing-card {
  max-width: 620px;
  padding: 24px 28px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-lighter);
}

.attention-box {
  padding: 14px 24px;
  border: 1px solid #f5cf7b;
  margin-bottom: 24px;
  background: #fff8e1;
  color: var(--el-text-color-primary);
  font-size: 13px;
  line-height: 1.65;
}

.attention-box p {
  margin: 4px 0 0;
}

.landing-field {
  max-width: 260px;
  width: 100%;
}

.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 24px 0 16px;
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.step-title span {
  display: inline-flex;
  width: 20px;
  height: 20px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--el-color-primary);
  color: #fff;
  font-size: 13px;
}

.ratio-field {
  width: 90px;
  margin: 0 8px;
}

.example-copy {
  margin: -6px 0 18px 98px;
  color: #ff4d00;
  font-size: 12px;
  line-height: 1.8;
}

.landing-date-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 18px;
}

.landing-preview {
  display: flex;
  gap: 16px;
  margin-top: 20px;
}

.poster {
  position: relative;
  width: 190px;
  height: 280px;
  overflow: hidden;
  border-radius: 2px;
  background: linear-gradient(160deg, #ffcc20 0%, #ff6a00 42%, #ff3b1f 100%);
  color: #fff;
  text-align: center;
}

.poster-title {
  margin-top: 34px;
  font-size: 17px;
  font-weight: 700;
}

.poster-subtitle {
  margin-top: 8px;
}

.red-envelope {
  position: absolute;
  right: 34px;
  bottom: 34px;
  left: 34px;
  height: 108px;
  border-radius: 12px 12px 4px 4px;
  background: #ff472f;
  box-shadow: inset 0 -28px 0 rgba(180, 0, 0, 0.16);
}

.red-envelope span {
  position: absolute;
  top: 48px;
  left: 50%;
  display: inline-flex;
  width: 40px;
  height: 40px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f4c545;
  color: #fff;
  transform: translateX(-50%);
}

.copy-preview {
  width: 180px;
  padding-top: 12px;
  color: var(--el-text-color-regular);
  line-height: 1.7;
}

.landing-result {
  min-height: 620px;
  padding: 22px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  box-shadow: var(--el-box-shadow-light);
}

.result-title {
  justify-content: space-between;
  margin-bottom: 18px;
}

.result-content {
  min-height: 220px;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
  cursor: pointer;
  line-height: 1.7;
  white-space: pre-wrap;
}

.empty-state,
.placeholder-panel {
  display: flex;
  min-height: 260px;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  gap: 10px;
  color: var(--el-text-color-secondary);
}

.empty-state :deep(.el-icon),
.placeholder-panel :deep(.el-icon) {
  font-size: 54px;
  color: var(--el-text-color-placeholder);
}

.result-panel {
  padding: 16px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}

.result-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-weight: 700;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 10px;
}

.result-grid div {
  display: flex;
  min-height: 58px;
  justify-content: center;
  flex-direction: column;
  padding: 10px 12px;
  border-radius: 6px;
  background: var(--el-bg-color);
}

.result-grid span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.result-grid strong {
  margin-top: 4px;
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.checklist {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 1180px) {
  .cash-layout,
  .landing-layout {
    grid-template-columns: 1fr;
  }

  .create-column {
    padding-right: 0;
    border-right: 0;
  }

  .calculator {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .mode-switch,
  .landing-preview,
  .landing-date-row {
    align-items: flex-start;
    flex-direction: column;
  }

  .panel-actions,
  .example-copy {
    margin-left: 0;
  }
}
</style>
