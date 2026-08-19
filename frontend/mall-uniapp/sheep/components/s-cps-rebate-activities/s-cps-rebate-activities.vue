<template>
  <view class="rebate-activities" :style="panelStyle">
    <view class="section-head">
      <text class="section-title">{{ content.title }}</text>
      <text v-if="content.showMore" class="more" @tap="goMore">更多 ›</text>
    </view>
    <view v-if="state.loading" class="state loading">活动加载中...</view>
    <view v-else-if="state.error" class="state error">
      <text>{{ state.error }}</text>
      <text class="retry" @tap="loadActivities">重新加载</text>
    </view>
    <view v-else-if="!state.list.length" class="state empty">暂无可用返利活动</view>
    <view v-else class="activity-list" :class="`activity-list--${content.layout}`">
      <view
        v-for="item in state.list"
        :key="item.id"
        class="activity-card"
        @tap="goActivity(item.id)"
      >
        <image
          class="activity-image"
          :src="item.imageFailed ? emptyImage : item.mainPic || emptyImage"
          mode="aspectFill"
          @error="item.imageFailed = true"
        />
        <view class="activity-body">
          <view class="title-row">
            <text class="activity-title">{{ item.activityName }}</text>
            <text v-if="item.tagText" class="tag">{{ item.tagText }}</text>
          </view>
          <text v-if="plainText(item.shortDesc)" class="description">
            {{ plainText(item.shortDesc) }}
          </text>
          <text v-if="plainText(item.rebateDesc)" class="rebate">
            {{ plainText(item.rebateDesc) }}
          </text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
  import { computed, onMounted, reactive, watch } from 'vue';
  import sheep from '@/sheep';
  import CpsMarketingApi from '@/sheep/api/cps/marketing';

  const props = defineProps({
    data: { type: Object, default: () => ({}) },
    styles: { type: Object, default: () => ({}) },
  });
  const emptyImage = '/static/goods-empty.png';
  const state = reactive({ loading: false, error: '', list: [] });
  let loadVersion = 0;

  const activityIds = computed(() => {
    const values = Array.isArray(props.data.activityIds) ? props.data.activityIds : [];
    return [
      ...new Set(values.map(Number).filter((id) => Number.isSafeInteger(id) && id > 0)),
    ].slice(0, 10);
  });
  const content = computed(() => ({
    title: props.data.title || '热门返利活动',
    layout: props.data.layout === 'list' ? 'list' : 'grid',
    limit: Math.min(10, Math.max(1, Number(props.data.limit) || 6)),
    showMore: props.data.showMore !== false,
  }));
  const idsKey = computed(() => activityIds.value.join(','));
  const panelStyle = computed(() => ({
    borderRadius: `${Number(props.styles.borderRadius || 0)}px`,
  }));

  async function loadActivities() {
    const version = ++loadVersion;
    const ids = activityIds.value;
    state.error = '';
    state.list = [];
    if (!ids.length) {
      state.loading = false;
      return;
    }
    state.loading = true;
    try {
      const res = await CpsMarketingApi.getActivitiesByIds(ids);
      if (!res || res.code !== 0) throw new Error(res?.msg || '活动加载失败');
      if (version !== loadVersion) return;
      const list = Array.isArray(res.data) ? res.data : [];
      const byId = new Map(list.map((item) => [Number(item.id), item]));
      state.list = ids
        .map((id) => byId.get(id))
        .filter(Boolean)
        .slice(0, content.value.limit);
    } catch (error) {
      if (version !== loadVersion) return;
      state.error = error?.msg || error?.message || '活动加载失败，请稍后重试';
    } finally {
      if (version === loadVersion) state.loading = false;
    }
  }

  function plainText(value) {
    return String(value || '')
      .replace(/<br\s*\/?\s*>/gi, ' ')
      .replace(/<[^>]*>/g, '')
      .replace(/&nbsp;/gi, ' ')
      .replace(/&amp;/gi, '&')
      .replace(/\s+/g, ' ')
      .trim();
  }
  function goActivity(activityId) {
    sheep.$router.go('/pages/cps/activity', { activityId });
  }
  function goMore() {
    sheep.$router.go('/pages/cps/activity');
  }

  onMounted(loadActivities);
  watch(idsKey, loadActivities);
</script>

<style lang="scss" scoped>
  .rebate-activities {
    box-sizing: border-box;
    overflow: hidden;
  }
  .section-head,
  .title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  .section-title {
    color: #24262d;
    font-size: 30rpx;
    font-weight: 650;
  }
  .more {
    padding: 8rpx 0 8rpx 20rpx;
    color: #8b8e97;
    font-size: 23rpx;
  }
  .state {
    display: flex;
    min-height: 150rpx;
    align-items: center;
    justify-content: center;
    flex-direction: column;
    color: #999ca5;
    font-size: 24rpx;
    text-align: center;
  }
  .retry {
    margin-top: 14rpx;
    color: #e6533c;
    font-weight: 600;
  }
  .activity-list {
    display: flex;
    gap: 18rpx;
    margin-top: 20rpx;
    flex-wrap: wrap;
  }
  .activity-card {
    min-width: 0;
    overflow: hidden;
    border: 1rpx solid #eceef2;
    border-radius: 14rpx;
    background: #fff;
    box-sizing: border-box;
  }
  .activity-list--grid .activity-card {
    width: calc(50% - 9rpx);
  }
  .activity-list--list .activity-card {
    display: flex;
    width: 100%;
  }
  .activity-image {
    width: 100%;
    height: 180rpx;
    background: #f1f2f4;
  }
  .activity-list--list .activity-image {
    width: 210rpx;
    height: 180rpx;
    flex: 0 0 210rpx;
  }
  .activity-body {
    min-width: 0;
    padding: 18rpx;
    flex: 1;
  }
  .title-row {
    gap: 8rpx;
  }
  .activity-title {
    min-width: 0;
    overflow: hidden;
    flex: 1;
    color: #292b32;
    font-size: 26rpx;
    font-weight: 650;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .tag {
    flex: 0 0 auto;
    padding: 3rpx 8rpx;
    border-radius: 6rpx;
    color: #e6533c;
    background: #fff0eb;
    font-size: 18rpx;
  }
  .description,
  .rebate {
    display: -webkit-box;
    margin-top: 9rpx;
    overflow: hidden;
    font-size: 21rpx;
    line-height: 1.4;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }
  .description {
    color: #7b7e87;
  }
  .rebate {
    color: #e6533c;
    font-weight: 600;
  }
</style>
