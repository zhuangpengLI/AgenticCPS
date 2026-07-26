<template>
  <div>
    <div class="flex gap-8px"><el-button type="primary" @click="openEditor()">添加推广位</el-button><el-button @click="batchVisible = true">批量粘贴</el-button></div>
    <el-alert title="运行时默认推广位只能有一个；渠道和会员推广位必须填写对应关系字段" type="info" show-icon class="my-12px" />
    <el-table :data="draft.adzones" border>
      <el-table-column prop="adzoneId" label="推广位 ID" /><el-table-column prop="adzoneName" label="名称" /><el-table-column label="类型"><template #default="{ row }">{{ normalizeAdzoneType(row.adzoneType) }}</template></el-table-column><el-table-column label="运行时默认"><template #default="{ row }"><el-tag v-if="row.adzoneId === draft.runtimeDefaultAdzoneId" type="success">默认</el-tag></template></el-table-column><el-table-column label="操作"><template #default="{ $index, row }"><el-button link @click="openEditor(row, $index)">编辑</el-button><el-button link type="danger" @click="remove($index)">移除</el-button><el-button link v-if="row.status === 0" @click="draft.runtimeDefaultAdzoneId = row.adzoneId">设为默认</el-button></template></el-table-column>
    </el-table>
    <AdzoneEditorDialog v-model="editorVisible" :row="editingRow" :platform-code="draft.platformCode" @save="saveRow" /><AdzoneBatchDialog v-model="batchVisible" :platform-code="draft.platformCode" @save="saveBatch" />
  </div>
</template>
<script lang="ts" setup>
import { ref } from 'vue'
import type { AdzoneForm, PlatformOnboardingDraft } from '@/api/cps/platformOnboarding'
import { normalizeAdzoneType, validateAdzoneDraft } from '@/views/cps/components/adzoneRules'
import AdzoneEditorDialog from './AdzoneEditorDialog.vue'
import AdzoneBatchDialog from './AdzoneBatchDialog.vue'
const props = defineProps<{ draft: PlatformOnboardingDraft }>()
const editorVisible = ref(false); const batchVisible = ref(false); const editingRow = ref<AdzoneForm>(); const editingIndex = ref(-1)
const openEditor = (row?: AdzoneForm, index = -1) => { editingRow.value = row; editingIndex.value = index; editorVisible.value = true }
const saveRow = (row: AdzoneForm) => { if (editingIndex.value >= 0) props.draft.adzones.splice(editingIndex.value, 1, row); else props.draft.adzones.push(row); if (!props.draft.runtimeDefaultAdzoneId) props.draft.runtimeDefaultAdzoneId = row.adzoneId }
const saveBatch = (rows: AdzoneForm[]) => { const errors = validateAdzoneDraft([...props.draft.adzones, ...rows], props.draft.runtimeDefaultAdzoneId || rows[0]?.adzoneId || ''); if (errors.length) { ElMessage.error(errors.join('；')); return }; props.draft.adzones.push(...rows); if (!props.draft.runtimeDefaultAdzoneId) props.draft.runtimeDefaultAdzoneId = rows[0]?.adzoneId || '' }
const remove = (index: number) => { const row = props.draft.adzones.splice(index, 1)[0]; if (row?.adzoneId === props.draft.runtimeDefaultAdzoneId) props.draft.runtimeDefaultAdzoneId = '' }
const validate = async () => validateAdzoneDraft(props.draft.adzones, props.draft.runtimeDefaultAdzoneId).length === 0
defineExpose({ validate })
</script>
