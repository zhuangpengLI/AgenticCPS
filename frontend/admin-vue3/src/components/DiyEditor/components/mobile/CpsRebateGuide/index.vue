<template>
  <section class="rebate-guide-preview">
    <strong>{{ property.title }}</strong>
    <div v-for="(step, index) in visibleSteps" :key="`${index}-${step}`" class="step">
      <span>{{ index + 1 }}</span><p>{{ step }}</p>
    </div>
    <div v-if="property.notice" class="notice"><Icon icon="ep:warning" />{{ property.notice }}</div>
  </section>
</template>

<script setup lang="ts">
import { CpsRebateGuideProperty } from './config'

defineOptions({ name: 'CpsRebateGuide' })
const props = defineProps<{ property: CpsRebateGuideProperty }>()
const visibleSteps = computed(() => (props.property.steps || []).filter((step) => step.trim()).slice(0, 5))
</script>

<style scoped lang="scss">
.rebate-guide-preview { border-radius: 8px; background: #fff; color: #333; }
strong { font-size: 14px; }
.step { display: flex; align-items: center; gap: 8px; margin-top: 10px; }
.step span { display: flex; width: 20px; height: 20px; flex: none; align-items: center; justify-content: center; border-radius: 50%; color: #fff; background: #f4513b; font-size: 10px; }
.step p { margin: 0; color: #555; font-size: 11px; }
.notice { display: flex; gap: 5px; margin-top: 12px; padding: 8px; border-radius: 4px; color: #8a5a00; background: #fff7e8; font-size: 9px; line-height: 1.5; }
</style>
