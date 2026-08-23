<template>
  <scroll-view class="scroll" scroll-x :show-scrollbar="false">
    <view v-for="item in block.items || []" :key="item.goodsId || item.title" class="card">
      <image v-if="item.mainPic" class="image" :src="item.mainPic" mode="aspectFill" />
      <view class="info">
        <view class="platform">{{ item.platformName || item.platformCode || '全平台' }}</view>
        <view class="title">{{ item.title || '推荐商品' }}</view>
        <view class="meta">{{ item.shopName || '联盟精选' }}<text v-if="item.monthSales"> · {{ item.monthSales }}+人购买</text></view>
        <view class="prices">
          <text class="price-label">券后价</text>
          <text class="price">¥{{ price(displayPrice(item)) }}</text>
          <text v-if="item.originalPrice" class="origin">¥{{ price(item.originalPrice) }}</text>
        </view>
        <view v-if="item.couponPrice" class="coupon-row">
          <text class="coupon-badge">优惠券 ¥{{ price(item.couponPrice) }}</text>
          <text v-if="item.couponConditions" class="coupon-condition">满 ¥{{ price(item.couponConditions) }} 可用</text>
        </view>
        <view v-if="couponValidity(item)" class="coupon-validity">有效期：{{ couponValidity(item) }}</view>
        <view class="decision-row">
          <text v-if="saving(item) > 0" class="saving">节省 ¥{{ price(saving(item)) }}</text>
          <text v-if="netPrice(item) !== null" class="net-price">预计净价 ¥{{ price(netPrice(item)) }}</text>
        </view>
        <view class="advice">购买建议：{{ purchaseAdvice(item) }}</view>
        <view class="bottom">
          <text v-if="item.commissionAmount" class="rebate">返 ¥{{ price(item.commissionAmount) }}</text>
          <view class="buttons">
            <button @tap.stop="$emit('action', { action: { type: 'OPEN_DETAIL' }, item })">详情</button>
            <button v-if="item.platformCode && item.goodsId" class="buy" @tap.stop="$emit('action', { action: buyAction(item), item })">去{{ platformText(item.platformCode) }}购买</button>
            <button v-if="command(item)" class="copy" @tap.stop="$emit('action', { action: copyAction(item), item })">复制{{ item.commandLabel || '口令' }}</button>
          </view>
        </view>
      </view>
    </view>
  </scroll-view>
</template>

<script setup>
  import { platformText, promotionUrl } from '@/sheep/helper/cps';
  defineProps({ block: { type: Object, default: () => ({}) } });
  defineEmits(['action']);
  const price = (value) => value === undefined || value === null || value === '' ? '--' : Number(value).toFixed(2);
  const displayPrice = (item) => item.actualPrice !== undefined && item.actualPrice !== null && item.actualPrice !== ''
    ? item.actualPrice
    : item.couponPrice;
  const numeric = (value) => {
    if (value === undefined || value === null || value === '') return null;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : null;
  };
  const saving = (item) => {
    const original = numeric(item.originalPrice);
    const actual = numeric(displayPrice(item));
    return original !== null && actual !== null ? Math.max(0, original - actual) : 0;
  };
  const netPrice = (item) => {
    const reported = numeric(item.netPrice);
    if (reported !== null) return Math.max(0, reported);
    const actual = numeric(displayPrice(item));
    const rebate = numeric(item.commissionAmount);
    return actual === null ? null : Math.max(0, actual - (rebate || 0));
  };
  const purchaseAdvice = (item) => {
    if (item.couponPrice && item.commissionAmount) return '先领券，再通过返利链接下单';
    if (item.couponPrice) return '先领取优惠券，按券后价购买';
    if (item.commissionAmount) return '通过返利链接下单，可获得预计返利';
    return '建议核对规格、库存和售后后再下单';
  };
  const couponValidity = (item) => {
    const start = item.couponStartTime || '';
    const end = item.couponEndTime || '';
    if (start && end) return `${start} 至 ${end}`;
    return end || start;
  };
  const command = (item) => item.tpwd || item.command || '';
  const buyAction = (item) => promotionUrl(item)
    ? { type: 'OPEN_PROMOTION', label: `去${platformText(item.platformCode)}购买`, riskLevel: 'ATTRIBUTION_WRITE', payload: { url: promotionUrl(item), platformCode: item.platformCode } }
    : { type: 'GENERATE_LINK', label: '生成购买链接', riskLevel: 'ATTRIBUTION_WRITE', payload: { platformCode: item.platformCode, goodsId: item.goodsId, goodsSign: item.goodsSign, vendorCode: item.vendorCode } };
  const copyAction = (item) => ({ type: 'COPY_COMMAND', label: `复制${item.commandLabel || '口令'}`, payload: { value: command(item), platformCode: item.platformCode } });
</script>

<style scoped lang="scss">
  .scroll { margin-top: 18rpx; white-space: nowrap; }
  .card { display: inline-flex; width: 580rpx; margin-right: 16rpx; padding: 18rpx; vertical-align: top; border: 1rpx solid #edf0f5; border-radius: 18rpx; background: #fbfcfe; white-space: normal; }
  .image { width: 148rpx; height: 148rpx; border-radius: 14rpx; background: #f0f3f8; }
  .info { min-width: 0; margin-left: 18rpx; flex: 1; }
  .platform { display: inline-block; padding: 5rpx 12rpx; border-radius: 8rpx; color: #103ea6; background: #edf3ff; font-size: 20rpx; }
  .title { display: -webkit-box; margin-top: 10rpx; overflow: hidden; color: #1c2b45; font-size: 26rpx; font-weight: 700; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
  .meta { margin-top: 6rpx; overflow: hidden; color: #7b8798; font-size: 21rpx; text-overflow: ellipsis; white-space: nowrap; }
  .prices { display: flex; margin-top: 10rpx; align-items: baseline; }
  .price-label { margin-right: 8rpx; color: #8a6b4b; font-size: 20rpx; }
  .price { color: #d66b08; font-size: 34rpx; font-weight: 800; }
  .origin { margin-left: 10rpx; color: #7e8794; font-size: 21rpx; text-decoration: line-through; }
  .coupon-row { display: flex; margin-top: 8rpx; align-items: center; gap: 8rpx; }
  .coupon-badge { padding: 4rpx 10rpx; border-radius: 6rpx; color: #c75b14; background: #fff0df; font-size: 20rpx; }
  .coupon-condition { color: #9a7b62; font-size: 19rpx; }
  .coupon-validity { margin-top: 6rpx; color: #9a7b62; font-size: 18rpx; }
  .decision-row { display: flex; margin-top: 8rpx; align-items: center; gap: 12rpx; }
  .saving { color: #c75b14; font-size: 20rpx; }
  .net-price { color: #11845b; font-size: 20rpx; font-weight: 700; }
  .advice { margin-top: 8rpx; color: #63748e; font-size: 20rpx; line-height: 1.45; }
  .bottom { display: flex; margin-top: 10rpx; align-items: center; justify-content: space-between; }
  .rebate { color: #11845b; font-size: 21rpx; }
  .buttons { display: flex; gap: 8rpx; }
  button { margin: 0; padding: 0 16rpx; border-radius: 26rpx; color: #fff; background: #103ea6; font-size: 21rpx; line-height: 52rpx; }
  button::after { border: 0; }
  button.buy { background: #d66b08; }
  button.copy { color: #a85a08; background: #fff2df; }
</style>
