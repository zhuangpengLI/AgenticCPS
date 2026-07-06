<template>
  <div class="coupon-query-panel">
    <div class="query-tip">
      <Icon icon="ep:sunny" />
      <span>填入同时带有商品和优惠券信息的内容即可查询该优惠券信息（如有券的二合一、淘口令、券链接+商品链接），带有其他文字也不影响查询！</span>
    </div>

    <div class="coupon-layout">
      <section class="query-section">
        <div class="pane-title">优惠券信息查询</div>
        <el-input
          v-model="queryText"
          type="textarea"
          :rows="9"
          maxlength="3000"
          show-word-limit
          placeholder="输入有券的二合一链接、淘口令，或同时输入商品+优惠券链接即可查询。"
        />
        <div class="query-actions">
          <el-button text type="primary" @click="clearQuery">清空</el-button>
          <el-button type="primary" :loading="loading" @click="handleQuery">查询</el-button>
        </div>
      </section>

      <section class="result-section">
        <div class="pane-title">优惠券结果</div>
        <div class="coupon-stage">
          <div class="coupon-card" :class="{ active: Boolean(couponCard.goods) }">
            <div class="coupon-side">优惠券</div>
            <div class="coupon-main">
              <div class="coupon-title">
                <template v-if="couponCard.goods">
                  {{ couponAmountText }}元优惠券
                </template>
                <template v-else>***元优惠券</template>
              </div>
              <template v-if="couponCard.goods">
                <div class="coupon-line">剩余 {{ couponCard.remainingText }} 张（已领用 {{ couponCard.usedText }} 张）</div>
                <div class="coupon-line">满 {{ couponCard.thresholdText }} 可用</div>
                <div class="coupon-line">活动时间：{{ couponCard.startText }} 至 {{ couponCard.endText }}</div>
              </template>
              <template v-else>
                <div class="coupon-line">剩余 *** 张（已领用 *** 张）</div>
                <div class="coupon-line">满 *** 可用</div>
                <div class="coupon-line">活动时间： *** 至 ***</div>
              </template>
              <div v-if="couponCard.goods" class="coupon-goods">{{ couponCard.goods.title || couponCard.goods.goodsId }}</div>
            </div>
          </div>
        </div>

        <div v-if="couponCard.goods" class="result-actions">
          <el-button type="primary" @click="sendToTransfer(couponCard.goods)">带入转链</el-button>
          <el-button @click="copyCouponText">复制优惠券信息</el-button>
        </div>
      </section>
    </div>

    <div v-if="goodsList.length" class="coupon-list">
      <el-table :data="goodsList" border>
        <el-table-column label="商品" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="goods-title">{{ row.title || row.goodsId }}</div>
            <div class="goods-sub">{{ platformLabel(row.platformCode) }} / {{ row.shopName || row.brandName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="券后价" width="110" align="center">
          <template #default="{ row }">{{ formatMoney(row.actualPrice) }}</template>
        </el-table-column>
        <el-table-column label="券额" width="100" align="center">
          <template #default="{ row }">{{ formatMoney(row.couponPrice) }}</template>
        </el-table-column>
        <el-table-column label="佣金" width="110" align="center">
          <template #default="{ row }">{{ formatMoney(row.commissionAmount) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="sendToTransfer(row)">带入转链</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'
import { CpsRebateToolboxApi } from '@/api/cps/rebateToolbox'
import type { CpsGoodsSquareGoodsVO } from '@/api/cps/goodsSquare'

const emit = defineEmits<{
  transfer: [value: { platformCode: string; originalContent: string; vendorCode?: string }]
}>()

const message = useMessage()
const { copy } = useClipboard()
const loading = ref(false)
const queryText = ref('')
const goodsList = ref<CpsGoodsSquareGoodsVO[]>([])

const couponCard = computed(() => {
  const goods = goodsList.value[0]
  return {
    goods,
    remainingText: goods ? '***' : '***',
    usedText: goods ? '***' : '***',
    thresholdText: goods ? `${Number(goods.actualPrice || 0).toFixed(2)} 元` : '***',
    startText: goods ? '以平台展示为准' : '***',
    endText: goods?.couponEndTime || '***'
  }
})

const couponAmountText = computed(() => {
  const value = couponCard.value.goods?.couponPrice
  return value === undefined || value === null ? '***' : Number(value).toFixed(0)
})

const handleQuery = async () => {
  const content = queryText.value.trim()
  if (!content) {
    message.warning('请输入优惠券查询内容')
    return
  }
  loading.value = true
  try {
    const result = await CpsRebateToolboxApi.queryCoupons({
      platformCode: guessPlatform(content),
      queryText: content,
      pageNo: 1,
      pageSize: 10
    })
    goodsList.value = result.list || []
    if (!goodsList.value.length) {
      message.warning(result.summary || '未找到符合条件的优惠券商品')
    } else {
      message.success(result.summary || `已找到 ${goodsList.value.length} 个有券商品`)
    }
  } finally {
    loading.value = false
  }
}

const clearQuery = () => {
  queryText.value = ''
  goodsList.value = []
}

const sendToTransfer = (item: CpsGoodsSquareGoodsVO) => {
  emit('transfer', {
    platformCode: item.platformCode,
    vendorCode: item.vendorCode,
    originalContent: item.itemLink || item.goodsId
  })
}

const copyCouponText = async () => {
  const goods = couponCard.value.goods
  if (!goods) return
  await copy([
    `${couponAmountText.value}元优惠券`,
    goods.title || goods.goodsId,
    `券后价：${formatMoney(goods.actualPrice)}`,
    `有效期：${couponCard.value.endText}`
  ].join('\n'))
  message.success('优惠券信息已复制')
}

const guessPlatform = (content: string) => {
  if (/pinduoduo|yangkeduo|pdd|拼多多/i.test(content)) return 'pdd'
  if (/jd\.com|京东/i.test(content)) return 'jd'
  if (/douyin|抖音/i.test(content)) return 'douyin'
  return 'taobao'
}

const platformLabel = (platformCode?: string) => {
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const formatMoney = (value?: number) =>
  value === undefined || value === null ? '-' : `￥${Number(value).toFixed(2)}`
</script>

<style scoped>
.coupon-query-panel {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.query-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--el-text-color-primary);
}

.coupon-layout {
  display: grid;
  grid-template-columns: minmax(460px, 0.95fr) minmax(430px, 1fr);
  gap: 46px;
  align-items: flex-start;
}

.pane-title {
  margin-bottom: 14px;
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 700;
}

.query-actions {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  margin-top: 8px;
}

.coupon-stage {
  display: flex;
  min-height: 210px;
  align-items: center;
  padding: 18px 22px;
  background: #d8d8d8;
}

.coupon-card {
  display: grid;
  width: 100%;
  min-height: 170px;
  grid-template-columns: 52px 1fr;
  overflow: hidden;
  border-radius: 8px;
  background: #fff;
}

.coupon-side {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #ff705e;
  color: #fff;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: 8px;
  line-height: 1.55;
  writing-mode: vertical-rl;
}

.coupon-main {
  padding: 22px 34px;
}

.coupon-title {
  margin-bottom: 18px;
  color: var(--el-text-color-primary);
  font-size: 28px;
}

.coupon-title::first-letter {
  color: var(--el-color-danger);
}

.coupon-line {
  margin-top: 8px;
  color: var(--el-text-color-primary);
  font-size: 16px;
}

.coupon-goods {
  max-width: 460px;
  margin-top: 12px;
  color: var(--el-text-color-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}

.coupon-list {
  max-width: 980px;
}

.goods-title {
  font-weight: 600;
}

.goods-sub {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

@media (max-width: 1180px) {
  .coupon-layout {
    grid-template-columns: 1fr;
  }
}
</style>
