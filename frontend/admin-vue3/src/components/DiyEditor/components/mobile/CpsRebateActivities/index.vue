<template>
  <section class="rebate-activities-preview">
    <header>
      <strong>{{ property.title }}</strong>
      <span v-if="property.showMore">更多 ›</span>
    </header>
    <div v-if="loading" class="state"><el-icon class="is-loading"><Loading /></el-icon> 正在加载活动</div>
    <el-alert v-else-if="error" :title="error" type="warning" :closable="false" show-icon />
    <el-empty v-else-if="!activities.length" description="请选择返利活动" :image-size="48" />
    <div v-else :class="['activity-list', property.layout]">
      <article v-for="activity in activities" :key="activity.id">
        <el-image v-if="activity.mainPic" :src="activity.mainPic" fit="cover" />
        <div class="activity-content">
          <b>{{ activity.activityName }}</b>
          <p>{{ activity.shortDesc || activity.rebateDesc || '返利活动' }}</p>
          <el-tag v-if="activity.status !== 1" size="small" type="warning">不可用</el-tag>
          <el-tag v-else-if="activity.tagText" size="small" type="danger">{{ activity.tagText }}</el-tag>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup lang="ts">
import { CpsRebateActivityApi, CpsRebateActivityVO } from '@/api/cps/rebateActivity'
import { CpsRebateActivitiesProperty } from './config'

defineOptions({ name: 'CpsRebateActivities' })
const props = defineProps<{ property: CpsRebateActivitiesProperty }>()
const activities = ref<CpsRebateActivityVO[]>([])
const loading = ref(false)
const error = ref('')
let requestVersion = 0

watch(
  () => [props.property.activityIds, props.property.limit] as const,
  async () => {
    const version = ++requestVersion
    const ids = [...new Set(props.property.activityIds || [])].slice(0, props.property.limit || 10)
    activities.value = []
    error.value = ''
    loading.value = false
    if (!ids.length) return
    loading.value = true
    const results = await Promise.allSettled(ids.map((id) => CpsRebateActivityApi.getActivity(id)))
    if (version !== requestVersion) return
    activities.value = results.flatMap((result) => (result.status === 'fulfilled' ? [result.value] : []))
    if (!activities.value.length) error.value = '活动已下线或暂时无法加载'
    loading.value = false
  },
  { immediate: true, deep: true }
)
</script>

<style scoped lang="scss">
.rebate-activities-preview { border-radius: 8px; background: #fff; color: #333; }
header { display: flex; align-items: center; justify-content: space-between; font-size: 14px; }
header span { color: #999; font-size: 10px; }
.state { padding: 24px 0; color: #999; font-size: 11px; text-align: center; }
.activity-list { display: grid; gap: 8px; margin-top: 12px; }
.activity-list.grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
article { display: flex; min-width: 0; overflow: hidden; border: 1px solid #f1f1f1; border-radius: 6px; background: #fff; }
.grid article { flex-direction: column; }
.el-image { width: 70px; height: 58px; flex: none; }
.grid .el-image { width: 100%; height: 70px; }
.activity-content { min-width: 0; padding: 8px; }
.activity-content b, .activity-content p { display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.activity-content b { font-size: 11px; }
.activity-content p { margin: 4px 0; color: #999; font-size: 9px; }
</style>
