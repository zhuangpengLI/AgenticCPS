<template>
  <section class="transfer-preview">
    <div class="transfer-heading"><Icon icon="ep:connection" /><strong>{{ property.title }}</strong></div>
    <p>{{ property.description }}</p>
    <div class="transfer-input"><span>{{ property.placeholder }}</span><b>{{ property.buttonText }}</b></div>
  </section>
</template>

<script setup lang="ts">
import { CpsRebateTransferProperty } from './config'

defineOptions({ name: 'CpsRebateTransfer' })
defineProps<{ property: CpsRebateTransferProperty }>()
</script>

<style scoped lang="scss">
.transfer-preview { padding: 16px; border: 1px solid #f4d8d2; border-radius: 8px; background: #fffaf8; }
.transfer-heading { display: flex; align-items: center; gap: 8px; color: #f4513b; font-size: 16px; }
p { margin: 8px 0 12px; color: #8c8c8c; font-size: 12px; }
.transfer-input { display: flex; align-items: center; gap: 8px; padding: 6px 6px 6px 12px; border: 1px solid #ebeef5; border-radius: 22px; color: #a8abb2; font-size: 11px; }
.transfer-input span { min-width: 0; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.transfer-input b { padding: 7px 12px; border-radius: 16px; color: #fff; background: #f4513b; font-size: 10px; font-weight: 500; }
</style>
