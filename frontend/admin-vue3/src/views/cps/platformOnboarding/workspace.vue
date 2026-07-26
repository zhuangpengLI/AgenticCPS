<template>
  <div class="p-20px">
    <div class="flex items-center justify-between mb-16px"><h2 class="text-18px">平台接入工作台</h2><el-button @click="close">关闭</el-button></div>
    <el-steps :active="currentStep" finish-status="success" class="mb-20px"><el-step v-for="(item, index) in steps" :key="item.title" :title="item.title" @click="goToStep(index)" /></el-steps>
    <WorkspaceSummary />
    <el-card class="mt-12px" shadow="never">
      <PlatformStep v-if="currentStep === 0" ref="platformRef" :draft="draft" :mode="draft.mode" />
      <VendorStep v-else-if="currentStep === 1" ref="vendorRef" :draft="draft" :descriptors="descriptors" />
      <AdzoneStep v-else-if="currentStep === 2" ref="adzoneRef" :draft="draft" />
      <RebateStep v-else-if="currentStep === 3" ref="rebateRef" :draft="draft" />
      <ReviewStep v-else ref="reviewRef" :draft="draft" />
    </el-card>
    <CheckResultPanel :result="draft.checkResult" />
    <div class="flex justify-between mt-16px">
      <el-button :disabled="currentStep === 0" @click="currentStep--">上一步</el-button>
      <div class="flex gap-8px"><el-button @click="saveDraft" :loading="saving">保存草稿</el-button><el-button v-if="currentStep === 4" type="warning" :loading="testing" :disabled="!draft.draftVersion" @click="runTest">连接测试</el-button><el-button v-if="currentStep < 4" type="primary" @click="next">下一步</el-button><template v-else><el-button type="primary" :loading="publishing" :disabled="!canPublish" @click="publish(false)">发布但保持禁用</el-button><el-button type="success" :loading="publishing" :disabled="!canPublish" @click="publish(true)">发布并启用</el-button></template></div>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { computed, onMounted, onBeforeUnmount, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { onBeforeRouteLeave, useRoute } from 'vue-router'
import type { PlatformOnboardingDraft, VendorDescriptor } from '@/api/cps/platformOnboarding'
import { PlatformOnboardingApi } from '@/api/cps/platformOnboarding'
import { createEmptyDraft, isDirty, maskConfiguredSecrets, normalizeDraftForSave, stepForFieldPath } from './model'
import PlatformStep from './components/PlatformStep.vue'
import VendorStep from './components/VendorStep.vue'
import AdzoneStep from './components/AdzoneStep.vue'
import RebateStep from './components/RebateStep.vue'
import ReviewStep from './components/ReviewStep.vue'
import CheckResultPanel from './components/CheckResultPanel.vue'
import WorkspaceSummary from './components/WorkspaceSummary.vue'

const props = withDefaults(defineProps<{ platformCode?: string; mode?: 'create' | 'edit' }>(), { mode: 'create' })
const emit = defineEmits<{ close: []; published: [PlatformOnboardingDraft] }>()
const route = useRoute()
const steps = [{ title: '平台信息' }, { title: 'API供应商' }, { title: '推广位' }, { title: '返利配置' }, { title: '检测与启用' }]
const currentStep = ref(0)
const originalDraft = shallowRef<PlatformOnboardingDraft>()
const draft = ref<PlatformOnboardingDraft>(createEmptyDraft(props.platformCode || ''))
const descriptors = ref<VendorDescriptor[]>([])
const saving = ref(false); const testing = ref(false); const publishing = ref(false)
const platformRef = ref(); const vendorRef = ref(); const adzoneRef = ref(); const rebateRef = ref(); const reviewRef = ref()
const dirty = computed(() => isDirty(originalDraft.value, draft.value))
const canPublish = computed(() => draft.value.status === 'READY' && Boolean(draft.value.draftVersion && draft.value.configFingerprint && draft.value.validatedFingerprint && draft.value.configFingerprint === draft.value.validatedFingerprint))
const payloadFromDetail = (detail: any): PlatformOnboardingDraft => { const payload = detail?.payload || detail?.draftPayload || detail?.runtimePayload || {}; return maskConfiguredSecrets({ ...createEmptyDraft(detail?.platformCode || props.platformCode || ''), ...detail, ...payload, platformCode: detail?.platformCode || payload.platform?.platformCode || props.platformCode || '', mode: detail?.mode || (props.mode === 'edit' ? 'RECONFIGURE' : 'CREATE'), status: detail?.status || 'DRAFT', checkResult: detail?.checkResult }) }
const load = async () => { if (props.platformCode) { const detail = await PlatformOnboardingApi.getDetail(props.platformCode); draft.value = payloadFromDetail(detail); originalDraft.value = JSON.parse(JSON.stringify(draft.value)); descriptors.value = await PlatformOnboardingApi.getVendorDescriptors(props.platformCode) } else { descriptors.value = await PlatformOnboardingApi.getVendorDescriptors() } }
const saveDraft = async () => { saving.value = true; try { const response = await PlatformOnboardingApi.saveDraft(normalizeDraftForSave(draft.value)); const next = payloadFromDetail(response); draft.value = { ...draft.value, ...next, status: next.status || 'DRAFT', checkResult: undefined, validatedFingerprint: undefined }; originalDraft.value = JSON.parse(JSON.stringify(draft.value)); ElMessage.success('草稿已保存') } finally { saving.value = false } }
const validateStep = async (index: number) => { const target = [platformRef, vendorRef, adzoneRef, rebateRef, reviewRef][index].value; return !target?.validate || await target.validate() }
const next = async () => { if (props.mode === 'create' && !(await validateStep(currentStep.value))) { ElMessage.warning('请先完成当前步骤'); return }; if (currentStep.value < 4) currentStep.value++ }
const goToStep = async (index: number) => { if (props.mode === 'edit' || index <= currentStep.value || (await validateStep(currentStep.value))) currentStep.value = Math.max(0, Math.min(4, index)) }
const runTest = async () => { if (dirty.value || !draft.value.draftVersion) await saveDraft(); if (!draft.value.draftVersion) return; testing.value = true; try { const result = await PlatformOnboardingApi.test(draft.value.platformCode, draft.value.draftVersion); draft.value.checkResult = result; draft.value.status = result.success ? 'READY' : 'FAILED'; draft.value.validatedFingerprint = result.success ? draft.value.configFingerprint : undefined; if (result.items?.length) { const first = result.items.find((item) => item.fieldPath); if (first) currentStep.value = stepForFieldPath(first.fieldPath) }; ElMessage[result.success ? 'success' : 'error'](result.success ? '连接测试通过' : '连接测试失败') } finally { testing.value = false } }
const publish = async (enableAfterPublish: boolean) => { publishing.value = true; try { const response = await PlatformOnboardingApi.publish({ platformCode: draft.value.platformCode, draftVersion: draft.value.draftVersion!, configFingerprint: draft.value.configFingerprint!, enableAfterPublish }); const nextDraft = payloadFromDetail(response); draft.value = nextDraft; originalDraft.value = JSON.parse(JSON.stringify(nextDraft)); emit('published', nextDraft); ElMessage.success(enableAfterPublish ? '已发布并启用' : '已发布但保持禁用') } finally { publishing.value = false } }
const close = async () => { if (dirty.value && !(await ElMessageBox.confirm('草稿尚未保存，确定离开吗？', '提示', { type: 'warning' }).catch(() => false))) return; emit('close') }
onBeforeRouteLeave(async () => { if (!dirty.value) return true; try { await ElMessageBox.confirm('草稿尚未保存，确定离开吗？', '提示', { type: 'warning' }); return true } catch { return false } })
onMounted(load); onBeforeUnmount(() => undefined)
</script>
