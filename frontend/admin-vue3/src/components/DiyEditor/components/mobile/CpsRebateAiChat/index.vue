<template>
  <section class="ai-preview">
    <div class="ai-heading"><span class="ai-mark"><Icon icon="ep:chat-dot-round" /></span><strong>{{ property.title }}</strong></div>
    <p>{{ property.description }}</p>
    <div class="ai-suggestion"><Icon icon="ep:magic-stick" /> {{ property.placeholder }}</div>
    <div class="ai-button">{{ property.buttonText }} <Icon icon="ep:arrow-right" /></div>
  </section>
</template>

<script setup lang="ts">
import { CpsRebateAiChatProperty } from './config'

defineOptions({ name: 'CpsRebateAiChat' })
defineProps<{ property: CpsRebateAiChatProperty }>()
</script>

<style scoped lang="scss">
.ai-preview { padding: 16px; border-radius: 8px; background: linear-gradient(135deg, #202833, #394d5e); color: #fff; }
.ai-heading { display: flex; align-items: center; gap: 8px; font-size: 16px; }
.ai-mark { display: inline-flex; padding: 6px; border-radius: 8px; background: #f4513b; }
p { margin: 8px 0 12px; color: rgba(255,255,255,.72); font-size: 12px; }
.ai-suggestion { padding: 10px 12px; border-radius: 8px; color: rgba(255,255,255,.76); background: rgba(255,255,255,.1); font-size: 11px; }
.ai-button { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; padding: 9px 12px; border-radius: 8px; color: #fff; background: #f4513b; font-size: 12px; }
</style>
