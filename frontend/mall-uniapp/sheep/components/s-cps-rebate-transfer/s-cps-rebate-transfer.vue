<template>
  <view class="transfer-card" :style="panelStyle">
    <view class="transfer-title"><text class="transfer-icon _icon-link" /><text>{{ content.title }}</text></view>
    <view class="transfer-description">{{ content.description }}</view>
    <view class="transfer-input-wrap">
      <textarea v-model="state.input" class="transfer-input" :placeholder="content.placeholder" :maxlength="500" />
      <button class="ss-reset-button transfer-button" :disabled="state.loading" @tap="transfer">{{ state.loading ? '处理中...' : content.buttonText }}</button>
    </view>
    <view v-if="state.result" class="transfer-result">
      <view class="result-title">{{ state.result.title || '已生成返利链接' }}</view>
      <view class="result-meta">券后价 ¥{{ money(state.result.actualPrice) }} · 佣金 {{ state.result.commissionRate || '--' }}%</view>
      <button class="ss-reset-button result-button" @tap="copyResult">复制推广链接</button>
    </view>
  </view>
</template>

<script setup>
  import { computed, reactive } from 'vue';
  import sheep from '@/sheep';
  import { showAuthModal } from '@/sheep/hooks/useModal';
  import CpsGoodsApi from '@/sheep/api/cps/goods';
  import { copyPromotionValue, formatMoney } from '@/sheep/helper/cps';

  const props = defineProps({ data: { type: Object, default: () => ({}) }, styles: { type: Object, default: () => ({}) } });
  const content = computed(() => ({ title: props.data.title || '一键转链', description: props.data.description || '粘贴商品链接或口令，生成专属返利链接', placeholder: props.data.placeholder || '粘贴商品链接、商品 ID 或口令', buttonText: props.data.buttonText || '立即转链', platformCode: props.data.platformCode || 'taobao' }));
  const state = reactive({ input: '', loading: false, result: null });
  const panelStyle = computed(() => ({ borderRadius: `${Number(props.styles.borderRadius || 8)}px` }));
  const money = (value) => formatMoney(value);
  async function transfer() {
    const originalContent = state.input.trim();
    if (!originalContent || state.loading) return;
    if (!sheep.$store('user').isLogin) { showAuthModal(); return; }
    state.loading = true; state.result = null;
    try {
      const parsed = await CpsGoodsApi.parseContent({ platformCode: content.value.platformCode, originalContent });
      if (parsed?.code !== 0 || !parsed.data?.supported) { sheep.$helper.toast(parsed?.data?.failureReason || '暂不支持该商品内容'); return; }
      const linked = await CpsGoodsApi.generateLink({ platformCode: parsed.data.platformCode || content.value.platformCode, goodsId: parsed.data.goodsId, goodsSign: parsed.data.goodsSign });
      if (linked?.code !== 0 || !linked.data) { sheep.$helper.toast('生成推广链接失败，请重试'); return; }
      state.result = { ...linked.data, title: parsed.data.title };
    } catch (error) { sheep.$helper.toast('转链失败，请稍后重试'); } finally { state.loading = false; }
  }
  async function copyResult() {
    const value = state.result?.shortUrl || state.result?.longUrl || state.result?.tpwd;
    if (!value) return;
    await copyPromotionValue(value); sheep.$helper.toast('推广链接已复制');
  }
</script>

<style lang="scss" scoped>
  .transfer-card { padding: 28rpx; background: #fffaf8; }
  .transfer-title { display: flex; align-items: center; gap: 12rpx; color: #f4513b; font-size: 32rpx; font-weight: 600; }
  .transfer-icon { font-size: 30rpx; }
  .transfer-description { margin-top: 10rpx; color: #8c8c8c; font-size: 23rpx; }
  .transfer-input-wrap { margin-top: 20rpx; padding: 16rpx; border: 1rpx solid #f0e1dd; border-radius: 14rpx; background: #fff; }
  .transfer-input { width: 100%; min-height: 108rpx; box-sizing: border-box; color: #333; font-size: 25rpx; }
  .transfer-button { width: 100%; height: 70rpx; margin-top: 14rpx; border-radius: 35rpx; color: #fff; background: #f4513b; font-size: 25rpx; line-height: 70rpx; }
  .transfer-result { margin-top: 18rpx; padding: 18rpx; border-radius: 12rpx; background: #fff2ed; }
  .result-title { color: #333; font-size: 25rpx; font-weight: 600; }
  .result-meta { margin-top: 8rpx; color: #a16b55; font-size: 22rpx; }
  .result-button { height: 58rpx; margin-top: 14rpx; border: 1rpx solid #f4513b; border-radius: 29rpx; color: #f4513b; background: #fff; font-size: 23rpx; line-height: 56rpx; }
</style>
