<template>
  <div class="user-order-preview">
    <div v-for="item in orderItems" :key="item.label" class="order-item">
      <img :src="resolveMallStaticUrl(item.icon)" :alt="item.label" />
      <span>{{ item.label }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { resolveMallStaticUrl } from '@/utils/mallAsset'
import { UserOrderProperty } from './config'

/** 用户订单 */
defineOptions({ name: 'UserOrder' })
// 定义属性
defineProps<{ property: UserOrderProperty }>()

const orderItems = [
  { label: '待付款', icon: '/static/img/shop/order/no_pay.png' },
  { label: '待收货', icon: '/static/img/shop/order/no_take.png' },
  { label: '待评价', icon: '/static/img/shop/order/no_comment.png' },
  { label: '售后单', icon: '/static/img/shop/order/change_order.png' },
  { label: '全部订单', icon: '/static/img/shop/order/all_order.png' }
]
</script>

<style scoped lang="scss">
.user-order-preview {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  min-height: 80px;
  background: #fff;
}

.order-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 12px;
  color: #333;
}

.order-item img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}
</style>
