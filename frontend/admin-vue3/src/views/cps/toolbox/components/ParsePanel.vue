<template>
  <div class="tool-panel">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="88px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="10" :lg="7">
          <el-form-item label="平台" prop="platformCode">
            <el-select v-model="formData.platformCode" filterable class="w-full" placeholder="请选择平台">
              <el-option
                v-for="item in platformOptions"
                :key="item.platformCode"
                :label="item.platformName || platformLabel(item.platformCode)"
                :value="item.platformCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="14" :lg="17">
          <el-form-item label="原始内容" prop="originalContent">
            <el-input
              v-model="formData.originalContent"
              clearable
              placeholder="粘贴商品链接、商品ID或淘口令"
              @keyup.enter="handleParse"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel-actions">
      <el-button type="primary" :loading="loading" @click="handleParse">
        <Icon icon="ep:search" class="mr-5px" /> 解析
      </el-button>
      <el-button :disabled="!canSendToTransfer" @click="sendToTransfer">
        <Icon icon="ep:connection" class="mr-5px" /> 带入转链
      </el-button>
    </div>

    <el-descriptions v-if="result" class="mt-16px" :column="1" border>
      <el-descriptions-item label="解析状态">
        <el-tag :type="result.supported ? 'success' : 'warning'" effect="plain">
          {{ result.supported ? '解析成功' : '解析失败' }}
        </el-tag>
      </el-descriptions-item>
      <el-descriptions-item label="解析来源">{{ parseSourceText(result.parseSource) }}</el-descriptions-item>
      <el-descriptions-item label="平台">{{ platformLabel(result.platformCode) }}</el-descriptions-item>
      <el-descriptions-item label="商品ID">{{ result.goodsId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="goodsSign">{{ result.goodsSign || '-' }}</el-descriptions-item>
      <el-descriptions-item label="标题">{{ result.title || '-' }}</el-descriptions-item>
      <el-descriptions-item label="原始链接">
        <a v-if="result.itemLink" :href="result.itemLink" target="_blank" class="text-blue-500">打开</a>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item v-if="!result.supported" label="失败原因">
        {{ result.failureReason || '-' }}
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import { CpsRebateToolboxApi, type CpsGoodsParseRespVO } from '@/api/cps/rebateToolbox'

const emit = defineEmits<{
  transfer: [value: { platformCode: string; originalContent: string }]
}>()

const message = useMessage()
const formRef = ref<FormInstance>()
const loading = ref(false)
const result = ref<CpsGoodsParseRespVO>()
const platformOptions = ref<CpsPlatformVO[]>([])

const formData = reactive({
  platformCode: 'taobao',
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
    originalContent: formData.originalContent
  })
}

const loadPlatformOptions = async () => {
  try {
    const data = await CpsPlatformApi.getEnabledPlatformList()
    platformOptions.value = data?.length ? data : fallbackPlatformOptions
  } catch {
    platformOptions.value = fallbackPlatformOptions
  }
}

const platformLabel = (platformCode?: string) => {
  const platform = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (platform?.platformName) return platform.platformName
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const parseSourceText = (source?: string) => {
  return source === 'platform' ? '平台/供应商解析' : source === 'local' ? '本地解析' : source || '-'
}

onMounted(loadPlatformOptions)
</script>

<style scoped>
.tool-panel {
  display: flex;
  flex-direction: column;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
