<template>
  <div>
    <el-alert title="默认平台返利优先展示；高级设置可配置会员等级规则。金额输入单位为元。" type="info" show-icon class="mb-12px" />
    <el-button type="primary" @click="openEditor()">添加返利规则</el-button>
    <el-table :data="draft.rebateRules" border class="mt-12px"><el-table-column label="范围"><template #default="{ row }">{{ row.memberLevelId ? `会员等级 #${row.memberLevelId}` : '平台默认' }}</template></el-table-column><el-table-column prop="rebateRate" label="返利比例"><template #default="{ row }">{{ row.rebateRate }}%</template></el-table-column><el-table-column prop="priority" label="优先级" /><el-table-column label="匹配说明"><template #default="{ row }">{{ row.memberLevelId ? '按会员等级匹配，未命中回退平台默认' : '平台默认规则' }}</template></el-table-column><el-table-column label="操作"><template #default="{ $index }"><el-button link @click="openEditor(draft.rebateRules[$index], $index)">编辑</el-button><el-button link type="danger" @click="draft.rebateRules.splice($index, 1)">移除</el-button></template></el-table-column></el-table>
    <RebateRuleDialog v-model="dialogVisible" :row="editingRow" :platform-code="draft.platformCode" @save="saveRule" />
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import type { PlatformOnboardingDraft, RebateRuleForm } from '@/api/cps/platformOnboarding'
import RebateRuleDialog from './RebateRuleDialog.vue'
const props = defineProps<{ draft: PlatformOnboardingDraft }>()
const dialogVisible = ref(false); const editingRow = ref<RebateRuleForm>(); const editingIndex = ref(-1)
const openEditor = (row?: RebateRuleForm, index = -1) => { editingRow.value = row; editingIndex.value = index; dialogVisible.value = true }
const saveRule = (row: RebateRuleForm) => { if (editingIndex.value >= 0) props.draft.rebateRules.splice(editingIndex.value, 1, row); else props.draft.rebateRules.push(row) }
const validate = async () => { const defaults = props.draft.rebateRules.filter((row) => !row.memberId && !row.memberLevelId); return defaults.length > 0 && defaults.every((row) => row.rebateRate != null && row.rebateRate >= 0 && row.rebateRate <= 100) }
defineExpose({ validate })
</script>
