<template>
  <div>
    <el-descriptions :column="2" border><el-descriptions-item label="平台">{{ draft.platform.platformName || draft.platformCode }}</el-descriptions-item><el-descriptions-item label="主供应商">{{ draft.primaryVendorCode || '未设置' }}</el-descriptions-item><el-descriptions-item label="供应商数量">{{ draft.vendors.length }}（含备用）</el-descriptions-item><el-descriptions-item label="运行时默认推广位">{{ draft.runtimeDefaultAdzoneId || '未设置' }}</el-descriptions-item><el-descriptions-item label="返利规则">{{ draft.rebateRules.length }} 条</el-descriptions-item><el-descriptions-item label="状态">{{ draft.status }}</el-descriptions-item></el-descriptions>
    <el-alert v-if="missing.length" :title="`仍缺少：${missing.join('、')}`" type="warning" show-icon class="mt-12px" /><el-alert v-else title="配置摘要完整，可以执行连接测试" type="success" show-icon class="mt-12px" />
    <CheckResultPanel :result="draft.checkResult" />
  </div>
</template>
<script lang="ts" setup>
import { computed } from 'vue'
import type { PlatformOnboardingDraft } from '@/api/cps/platformOnboarding'
import CheckResultPanel from './CheckResultPanel.vue'
const props = defineProps<{ draft: PlatformOnboardingDraft }>()
const missing = computed(() => [!props.draft.platform.platformCode && '平台', !props.draft.primaryVendorCode && '主供应商', !props.draft.runtimeDefaultAdzoneId && '默认推广位', !props.draft.rebateRules.some((row) => !row.memberId && !row.memberLevelId) && '默认返利'].filter(Boolean) as string[])
</script>
