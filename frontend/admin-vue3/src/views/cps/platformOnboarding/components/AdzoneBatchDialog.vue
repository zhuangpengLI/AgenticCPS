<template>
  <el-dialog v-model="visible" title="批量添加推广位" width="640px"><el-alert title="支持每行：ID、名称、类型、relationId、externalRelationId、externalSpecialId；整批本地校验，全有或全无" type="info" show-icon /><el-input v-model="text" type="textarea" :rows="10" class="mt-12px" placeholder="adzone-001\t主推广位\tGENERAL" /><template #footer><el-button @click="visible = false">取消</el-button><el-button type="primary" @click="submit">校验并添加</el-button></template></el-dialog>
</template>
<script lang="ts" setup>
import { computed, ref } from 'vue'
import type { AdzoneForm } from '@/api/cps/platformOnboarding'
import { parseAdzoneBatch, validateAdzoneRow } from '@/views/cps/components/adzoneRules'
const props = defineProps<{ modelValue: boolean; platformCode: string }>()
const emit = defineEmits<{ 'update:modelValue': [boolean]; save: [AdzoneForm[]] }>()
const visible = computed({ get: () => props.modelValue, set: (v) => emit('update:modelValue', v) })
const text = ref('')
const submit = () => { const rows = parseAdzoneBatch(text.value, props.platformCode); const errors = rows.flatMap((row, i) => validateAdzoneRow(row).map((message) => `第 ${i + 1} 行：${message}`)); if (errors.length) { ElMessage.error(errors.join('；')); return }; emit('save', rows); visible.value = false; text.value = '' }
</script>
