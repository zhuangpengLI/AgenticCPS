<template>
  <el-card v-if="result" shadow="never" class="mt-12px">
    <template #header>检查结果</template>
    <el-alert :type="result.success ? 'success' : 'error'" :title="result.success ? '检查成功' : '检查失败'" show-icon />
    <el-table v-if="result.items?.length" :data="result.items" class="mt-12px" size="small">
      <el-table-column prop="section" label="供应商 / 检测能力" min-width="160" />
      <el-table-column label="成功或失败" min-width="110"><template #default="{ row }">{{ row.code?.includes('FAIL') ? '失败' : (result.success ? '成功' : '失败') }}</template></el-table-column>
      <el-table-column prop="message" label="结果" min-width="240" />
      <el-table-column label="耗时" min-width="90"><template #default>—</template></el-table-column>
      <el-table-column label="脱敏原因" min-width="160"><template #default="{ row }">{{ row.code?.includes('SECRET') ? '凭证字段已脱敏' : '不展示请求 payload' }}</template></el-table-column>
      <el-table-column label="建议" min-width="180">
        <template #default="{ row }">{{ row.code?.includes('FAIL') ? '请修正后重新检查' : '已通过，可继续发布' }}</template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
<script lang="ts" setup>
import type { OnboardingCheckResult } from '@/api/cps/platformOnboarding'

defineProps<{ result?: OnboardingCheckResult }>()
</script>
