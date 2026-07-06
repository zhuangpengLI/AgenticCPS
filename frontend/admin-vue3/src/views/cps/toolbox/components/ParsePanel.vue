<template>
  <div class="tool-panel parse-workbench">
    <section class="parse-pane parse-input-pane">
      <div class="pane-title-row">
        <div>
          <div class="pane-title">口令解析</div>
          <div class="pane-subtitle">粘贴淘口令、商品链接或商品 ID，解析后可直接送去转链。</div>
        </div>
      </div>
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="76px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="平台" prop="platformCode">
              <el-select
                v-model="formData.platformCode"
                filterable
                class="w-full"
                placeholder="请选择平台"
                @change="handlePlatformChange"
              >
                <el-option
                  v-for="item in platformOptions"
                  :key="item.platformCode"
                  :label="item.platformName || platformLabel(item.platformCode)"
                  :value="item.platformCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="供应商">
              <el-select
                v-model="formData.vendorCode"
                clearable
                filterable
                class="w-full"
                placeholder="默认供应商"
                :loading="vendorLoading"
              >
                <el-option
                  v-for="item in enabledVendorOptions"
                  :key="`${item.vendorCode}:${item.platformCode}`"
                  :label="formatVendorLabel(item)"
                  :value="item.vendorCode"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="解析内容" prop="originalContent">
              <el-input
                v-model="formData.originalContent"
                type="textarea"
                :rows="9"
                maxlength="2000"
                show-word-limit
                placeholder="粘贴商品链接、淘口令、商品链接，或商品 ID。"
                @keyup.ctrl.enter="handleParse"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <div class="panel-actions">
        <el-button type="primary" :loading="loading" @click="handleParse">
          <Icon icon="ep:search" class="mr-5px" /> 开始解析
        </el-button>
        <el-button :disabled="!canSendToTransfer" @click="sendToTransfer">
          <Icon icon="ep:connection" class="mr-5px" /> 去转链
        </el-button>
        <el-button @click="formData.originalContent = ''">
          <Icon icon="ep:refresh" class="mr-5px" /> 清空
        </el-button>
      </div>
    </section>

    <section class="parse-pane parse-result-pane">
      <div class="pane-title-row">
        <div>
          <div class="pane-title">解析结果</div>
          <div class="pane-subtitle">平台解析会走当前选择的 API 供应商。</div>
        </div>
        <el-tag v-if="result" :type="result.supported ? 'success' : 'warning'" effect="plain">
          {{ result.supported ? '解析成功' : '解析失败' }}
        </el-tag>
      </div>

      <div v-if="result" class="parse-result-grid">
        <div class="result-item">
          <span>解析来源</span>
          <strong>{{ parseSourceText(result.parseSource) }}</strong>
        </div>
        <div class="result-item">
          <span>平台</span>
          <strong>{{ platformLabel(result.platformCode) }}</strong>
        </div>
        <div class="result-item">
          <span>商品ID</span>
          <strong>{{ result.goodsId || '-' }}</strong>
        </div>
        <div class="result-item">
          <span>goodsSign</span>
          <strong>{{ result.goodsSign || '-' }}</strong>
        </div>
        <div class="result-item full">
          <span>标题</span>
          <strong>{{ result.title || '-' }}</strong>
        </div>
        <div class="result-item full">
          <span>商品地址</span>
          <a v-if="result.itemLink" :href="result.itemLink" target="_blank" class="text-blue-500">
            {{ result.itemLink }}
          </a>
          <strong v-else>-</strong>
        </div>
        <div class="result-item full">
          <span>优惠券地址</span>
          <a v-if="result.couponLink" :href="result.couponLink" target="_blank" class="text-blue-500">
            {{ result.couponLink }}
          </a>
          <strong v-else>-</strong>
        </div>
        <div class="result-item full">
          <span>原二合一长链</span>
          <a v-if="result.sourceLink" :href="result.sourceLink" target="_blank" class="text-blue-500">
            {{ result.sourceLink }}
          </a>
          <strong v-else>-</strong>
        </div>
        <div v-if="!result.supported" class="result-item full error">
          <span>失败原因</span>
          <strong>{{ result.failureReason || '-' }}</strong>
        </div>
      </div>

      <el-empty v-else description="等待解析内容" :image-size="92" />
    </section>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { CpsApiVendorApi, VENDOR_CODE_OPTIONS, type CpsApiVendorVO } from '@/api/cps/apiVendor'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import { CpsRebateToolboxApi, type CpsGoodsParseRespVO } from '@/api/cps/rebateToolbox'

const emit = defineEmits<{
  transfer: [value: { platformCode: string; originalContent: string; vendorCode?: string }]
}>()

const message = useMessage()
const formRef = ref<FormInstance>()
const loading = ref(false)
const vendorLoading = ref(false)
const result = ref<CpsGoodsParseRespVO>()
const platformOptions = ref<CpsPlatformVO[]>([])
const vendorOptions = ref<CpsApiVendorVO[]>([])

const formData = reactive({
  platformCode: 'taobao',
  vendorCode: undefined as string | undefined,
  originalContent: ''
})

const formRules = reactive<FormRules>({
  platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
  originalContent: [{ required: true, message: '请输入原始内容', trigger: 'blur' }]
})

const fallbackPlatformOptions: CpsPlatformVO[] = [
  { id: 0, platformCode: 'taobao', platformName: '淘宝', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'jd', platformName: '京东', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'pdd', platformName: '拼多多', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'douyin', platformName: '抖音', status: 1, createTime: new Date() }
]

const canSendToTransfer = computed(() => result.value?.supported && formData.originalContent)
const enabledVendorOptions = computed(() => vendorOptions.value.filter((item) => item.status === 1))

const handleParse = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    result.value = await CpsRebateToolboxApi.parseContent(formData)
    if (result.value.supported) {
      message.success('解析成功')
    } else {
      message.warning(result.value.failureReason || '解析失败')
    }
  } finally {
    loading.value = false
  }
}

const sendToTransfer = () => {
  if (!canSendToTransfer.value) return
  emit('transfer', {
    platformCode: formData.platformCode,
    vendorCode: formData.vendorCode,
    originalContent: formData.originalContent
  })
}

const handlePlatformChange = async () => {
  formData.vendorCode = undefined
  await loadVendorOptions(formData.platformCode)
}

const loadPlatformOptions = async () => {
  try {
    const data = await CpsPlatformApi.getEnabledPlatformList()
    platformOptions.value = data?.length ? data : fallbackPlatformOptions
  } catch {
    platformOptions.value = fallbackPlatformOptions
  }
}

const loadVendorOptions = async (platformCode: string) => {
  vendorLoading.value = true
  try {
    vendorOptions.value = await CpsApiVendorApi.getVendorListByPlatform(platformCode)
  } finally {
    vendorLoading.value = false
  }
}

const platformLabel = (platformCode?: string) => {
  const platform = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (platform?.platformName) return platform.platformName
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const vendorLabel = (vendorCode?: string) => {
  if (!vendorCode) return '默认供应商'
  return VENDOR_CODE_OPTIONS.find((item) => item.value === vendorCode)?.label || vendorCode
}

const formatVendorLabel = (item: CpsApiVendorVO) => item.vendorName || vendorLabel(item.vendorCode)

const parseSourceText = (source?: string) => {
  return source === 'platform' ? '平台/供应商解析' : source === 'local' ? '本地解析' : source || '-'
}

onMounted(async () => {
  await loadPlatformOptions()
  await loadVendorOptions(formData.platformCode)
})
</script>

<style scoped>
.tool-panel {
  display: flex;
  flex-direction: column;
}

.parse-workbench {
  display: grid;
  grid-template-columns: minmax(360px, 1.05fr) minmax(320px, 0.95fr);
  gap: 14px;
}

.parse-pane {
  min-width: 0;
  padding: 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-bg-color-page);
}

.pane-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.pane-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.pane-subtitle {
  margin-top: 3px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.parse-result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.result-item {
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: var(--el-bg-color);
}

.result-item.full {
  grid-column: 1 / -1;
}

.result-item span {
  display: block;
  margin-bottom: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.result-item strong,
.result-item a {
  overflow-wrap: anywhere;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
}

.result-item.error strong {
  color: var(--el-color-warning);
}

@media (max-width: 1180px) {
  .parse-workbench {
    grid-template-columns: 1fr;
  }
}
</style>
