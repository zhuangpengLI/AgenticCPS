<template>
  <div class="user-wallet-preview">
    <div v-for="item in walletItems" :key="item.label" class="wallet-item">
      <template v-if="item.icon">
        <img :src="resolveMallStaticUrl(item.icon)" :alt="item.label" />
      </template>
      <strong v-else>{{ item.value }}</strong>
      <span>{{ item.label }}</span>
    </div>
  </div>
</template>
<script setup lang="ts">
import { resolveMallStaticUrl } from '@/utils/mallAsset'
import { UserWalletProperty } from './config'

/** 用户资产 */
defineOptions({ name: 'UserWallet' })
// 定义属性
defineProps<{ property: UserWalletProperty }>()

const walletItems = [
  { label: '账户余额', value: '0.00' },
  { label: '积分', value: '0' },
  { label: '优惠券', value: '0' },
  { label: '我的钱包', icon: '/static/img/shop/user/wallet_icon.png' }
]
</script>

<style scoped lang="scss">
.user-wallet-preview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  min-height: 80px;
  background: #fff;
}

.wallet-item {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 12px;
  color: #333;
}

.wallet-item strong {
  font-size: 15px;
  font-weight: 500;
}

.wallet-item img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}
</style>
