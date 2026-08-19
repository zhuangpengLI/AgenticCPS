<template>
  <div
    class="user-coupon-preview"
    :style="{ backgroundImage: `url(${resolveMallStaticUrl('/static/img/diy/CouponCard.png')})` }"
  >
    <div v-for="item in couponItems" :key="item.title" class="coupon-item">
      <div class="coupon-value"><strong>{{ item.value }}</strong><span>元</span></div>
      <div class="coupon-copy">
        <b>{{ item.title }}</b>
        <span>满 99 元可用</span>
      </div>
      <button type="button">去使用</button>
    </div>
  </div>
</template>
<script setup lang="ts">
import { resolveMallStaticUrl } from '@/utils/mallAsset'
import { UserCouponProperty } from './config'

/** 用户卡券 */
defineOptions({ name: 'UserCoupon' })
// 定义属性
defineProps<{ property: UserCouponProperty }>()

const couponItems = [
  { title: '商城优惠券', value: '10' },
  { title: '新人专享券', value: '20' }
]
</script>

<style scoped lang="scss">
.user-coupon-preview {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  padding: 8px;
  background-color: #fff8f0;
  background-repeat: repeat;
}

.coupon-item {
  display: grid;
  min-width: 0;
  padding: 10px;
  color: #6b4a2d;
  background: #fff;
  border: 1px solid #ffd7b0;
  border-radius: 6px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 7px;
}

.coupon-value {
  color: #ff6b00;
  white-space: nowrap;
}

.coupon-value strong {
  font-size: 22px;
}

.coupon-value span,
.coupon-copy span {
  font-size: 10px;
}

.coupon-copy {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 3px;
}

.coupon-copy b {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.coupon-copy span {
  color: #999;
}

.coupon-item button {
  padding: 5px 7px;
  font-size: 10px;
  color: #fff;
  white-space: nowrap;
  background: #ff6b00;
  border: 0;
  border-radius: 4px;
}
</style>
