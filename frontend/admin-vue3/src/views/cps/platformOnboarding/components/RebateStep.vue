<!-- eslint-disable vue/no-mutating-props -- the wizard intentionally shares one draft model across steps -->
<template>
  <div>
    <el-alert title="默认平台返利优先展示；高级设置可配置会员等级规则。金额输入单位为元。" type="info" show-icon class="mb-12px" />
    <el-button type="primary" @click="openEditor()">添加返利规则</el-button>
    <el-table :data="sortedRules" border class="mt-12px"><el-table-column label="范围"><template #default="{ row }">{{ row.memberLevelId ? `会员等级 #${row.memberLevelId}` : '平台默认' }}</template></el-table-column><el-table-column prop="rebateRate" label="返利比例"><template #default="{ row }">{{ row.rebateRate }}%</template></el-table-column><el-table-column prop="priority" label="优先级" /><el-table-column label="匹配说明"><template #default="{ row }">{{ row.memberLevelId ? '按会员等级匹配，未命中回退平台默认' : '平台默认规则' }}</template></el-table-column><el-table-column label="操作"><template #default="{ row }"><el-button link @click="openEditor(row, draft.rebateRules.indexOf(row))">编辑</el-button><el-button link type="danger" @click="draft.rebateRules.splice(draft.rebateRules.indexOf(row), 1)">移除</el-button></template></el-table-column></el-table>
    <RebateRuleDialog v-model="dialogVisible" :row="editingRow" :platform-code="draft.platform.platformCode || draft.platformCode" @save="saveRule" />
  </div>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { PlatformOnboardingDraft, RebateRuleForm } from '@/api/cps/platformOnboarding'
import RebateRuleDialog from './RebateRuleDialog.vue'
const props = defineProps<{ draft: PlatformOnboardingDraft }>()
const sortedRules = computed(() => [...props.draft.rebateRules].sort((left, right) => {
  const leftDefault = !left.memberId && !left.memberLevelId ? 0 : 1
  const rightDefault = !right.memberId && !right.memberLevelId ? 0 : 1
  return leftDefault - rightDefault
}))
const dialogVisible = ref(false); const editingRow = ref<RebateRuleForm>(); const editingIndex = ref(-1)
const openEditor = (row?: RebateRuleForm, index = -1) => { editingRow.value = row; editingIndex.value = index; dialogVisible.value = true }
const saveRule = (row: RebateRuleForm) => { if (editingIndex.value >= 0) props.draft.rebateRules.splice(editingIndex.value, 1, row); else props.draft.rebateRules.push(row) }
const validate = async () => { const invalidScope = props.draft.rebateRules.some((row) => (row as any).scope === 'GLOBAL' || (row as any).scope === 'PERSONAL' || Boolean(row.memberId)); if (invalidScope) { ElMessage.error('工作台拒绝全局和个人 scope'); return false }; const defaults = props.draft.rebateRules.filter((row) => !row.memberId && !row.memberLevelId); return defaults.length === 1 && defaults[0].status === 1 && defaults[0].rebateRate != null && defaults[0].rebateRate >= 0 && defaults[0].rebateRate <= 100 }
defineExpose({ validate })
</script>
