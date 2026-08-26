<template>
  <div v-if="items.length" class="product-list">
    <article v-for="(item, index) in items" :key="productKey(item, index)" class="product-card">
      <el-image v-if="item.mainPic" :src="item.mainPic" fit="cover" class="product-card__image">
        <template #error><div class="product-card__image-placeholder">暂无图片</div></template>
      </el-image>
      <div v-else class="product-card__image product-card__image-placeholder">暂无图片</div>

      <div class="product-card__body">
        <div class="product-card__title-row">
          <strong :title="item.title">{{ item.title || '未命名商品' }}</strong>
          <el-tag v-if="item.platformName || item.platformCode" size="small" effect="plain">
            {{ item.platformName || item.platformCode }}
          </el-tag>
        </div>
        <div class="product-card__meta">
          <span v-if="price(item) !== undefined" class="product-card__price">
            ¥{{ formatNumber(price(item)) }}
          </span>
          <del v-if="item.originalPrice">¥{{ formatNumber(item.originalPrice) }}</del>
          <span v-if="item.commissionRate !== undefined">佣金 {{ formatPercent(item.commissionRate) }}</span>
          <span v-if="item.commissionAmount !== undefined">
            预估 ¥{{ formatNumber(item.commissionAmount) }}
          </span>
          <span v-if="item.monthSales !== undefined">月销 {{ item.monthSales }}</span>
        </div>
        <div v-if="item.shopName || item.couponConditions" class="product-card__minor">
          <span v-if="item.shopName">{{ item.shopName }}</span>
          <span v-if="item.couponConditions">{{ item.couponConditions }}</span>
        </div>
        <div v-if="compare" class="product-card__source">来源：{{ item.vendorCode || '平台商品库' }}</div>
        <div v-if="item.actions?.length || detailUrl(item)" class="product-card__actions">
          <a
            v-if="detailUrl(item)"
            :href="detailUrl(item)"
            target="_blank"
            rel="noopener noreferrer"
          >
            查看详情
          </a>
          <template v-for="action in item.actions || []" :key="`${action.type}-${action.label}`">
            <el-tag v-if="action.type !== 'OPEN_DETAIL'" size="small" effect="plain">
              {{ action.label }}
            </el-tag>
          </template>
        </div>
      </div>
    </article>
  </div>
  <el-empty v-else description="暂无商品结果" :image-size="64" />
</template>

<script setup lang="ts">
import type { ChatMessageProductItem } from '@/api/ai/chat/message'

defineProps<{
  items: ChatMessageProductItem[]
  compare?: boolean
}>()

const productKey = (item: ChatMessageProductItem, index: number) =>
  `${item.platformCode || 'unknown'}-${item.goodsId || item.goodsSign || index}`

const price = (item: ChatMessageProductItem) => item.netPrice ?? item.actualPrice ?? item.couponPrice

const formatNumber = (value: unknown) => {
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric.toFixed(2).replace(/\.00$/, '') : String(value ?? '-')
}

const formatPercent = (value: unknown) => {
  const text = String(value ?? '-')
  return text.endsWith('%') ? text : `${text}%`
}

const safeUrl = (value: unknown) => {
  if (typeof value !== 'string') return undefined
  return /^https?:\/\//i.test(value) ? value : undefined
}

const detailUrl = (item: ChatMessageProductItem) => safeUrl(item.promotionUrl) || safeUrl(item.itemLink)
</script>

<style scoped>
.product-list {
  display: grid;
  gap: 10px;
  padding: 0 14px 14px;
}

.product-card {
  display: flex;
  gap: 12px;
  padding: 10px;
  border-radius: 10px;
  background: var(--el-fill-color-lighter);
}

.product-card__image {
  width: 76px;
  height: 76px;
  flex: 0 0 76px;
  border-radius: 8px;
}

.product-card__image-placeholder {
  display: grid;
  place-items: center;
  background: var(--el-fill-color);
  color: var(--el-text-color-placeholder);
  font-size: 11px;
}

.product-card__body {
  min-width: 0;
  flex: 1;
}

.product-card__title-row,
.product-card__meta,
.product-card__minor,
.product-card__actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.product-card__title-row strong {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-card__meta,
.product-card__minor,
.product-card__source,
.product-card__actions {
  margin-top: 7px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.product-card__price {
  color: var(--el-color-danger);
  font-size: 16px;
  font-weight: 700;
}

.product-card__actions a {
  color: var(--el-color-primary);
  text-decoration: none;
}
</style>
