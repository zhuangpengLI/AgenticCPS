<template>
  <el-alert title="草稿仅影响运行时配置，发布前必须通过当前指纹的连接测试" type="info" show-icon />
  <el-descriptions v-if="runtimePayload || draftPayload" :column="2" border class="mt-12px">
    <el-descriptions-item label="当前运行配置">{{ runtimePayload ? '已加载' : '未发布' }}</el-descriptions-item>
    <el-descriptions-item label="待发布草稿">{{ draftPayload ? '已加载，可继续完善' : '无' }}</el-descriptions-item>
  </el-descriptions>
</template>
<script lang="ts" setup>
import type { PlatformOnboardingPayload } from '@/api/cps/platformOnboarding'
defineProps<{ runtimePayload?: PlatformOnboardingPayload; draftPayload?: PlatformOnboardingPayload }>()
</script>
