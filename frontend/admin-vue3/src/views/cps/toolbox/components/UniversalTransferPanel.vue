<template>
  <div class="tool-panel">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="88px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :lg="6">
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
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="会员" prop="memberId">
            <el-select
              v-model="formData.memberId"
              filterable
              remote
              reserve-keyword
              clearable
              class="w-full"
              placeholder="手机号或昵称"
              :remote-method="searchMemberOptions"
              :loading="memberLoading"
              @visible-change="handleMemberDropdownVisible"
            >
              <el-option
                v-for="item in memberOptions"
                :key="item.id"
                :label="formatMemberLabel(item)"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="供应商">
            <el-select
              v-model="formData.vendorCode"
              clearable
              filterable
              class="w-full"
              placeholder="默认路由"
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
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="推广位">
            <el-select
              v-model="formData.adzoneId"
              clearable
              filterable
              class="w-full"
              placeholder="默认推广位"
              :loading="adzoneLoading"
            >
              <el-option
                v-for="item in enabledAdzoneOptions"
                :key="item.adzoneId"
                :label="formatAdzoneLabel(item)"
                :value="item.adzoneId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="输出格式">
            <el-checkbox-group v-model="formatOptions">
              <el-checkbox label="promotionContent">完整文案</el-checkbox>
              <el-checkbox label="shortUrl">短链</el-checkbox>
              <el-checkbox label="longUrl">长链</el-checkbox>
              <el-checkbox label="tpwd">淘口令</el-checkbox>
              <el-checkbox label="mobileUrl">移动端链接</el-checkbox>
              <el-checkbox label="original">保留原文</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="原始内容" prop="originalText">
            <el-input
              v-model="formData.originalText"
              type="textarea"
              :rows="7"
              maxlength="5000"
              show-word-limit
              placeholder="粘贴商品链接、商品ID或平台口令，每行一条，最多 20 条"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel-actions">
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        <Icon icon="ep:connection" class="mr-5px" /> 批量转链
      </el-button>
      <el-button @click="handleReset">
        <Icon icon="ep:refresh" class="mr-5px" /> 重置
      </el-button>
      <el-button :disabled="successRows.length === 0" @click="copyAllSuccess">
        <Icon icon="ep:copy-document" class="mr-5px" /> 复制成功项
      </el-button>
      <span class="limit-text">已识别 {{ contentLines.length }} 条</span>
    </div>

    <el-table v-if="resultRows.length" :data="resultRows" border class="mt-16px">
      <el-table-column label="#" prop="inputIndex" width="60" align="center" />
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'warning'" effect="plain">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.goods?.title || row.goods?.goodsId || row.originalContent }}
        </template>
      </el-table-column>
      <el-table-column label="佣金" width="110" align="center">
        <template #default="{ row }">
          {{ formatMoney(row.rebate?.commissionAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="返利" width="110" align="center">
        <template #default="{ row }">
          {{ formatMoney(row.rebate?.estimateRebateAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="记录ID" prop="transferRecordId" width="100" align="center" />
      <el-table-column label="消息" min-width="180" show-overflow-tooltip>
        <template #default="{ row }">{{ row.message || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button text :disabled="row.status !== 'SUCCESS'" @click="sendToEditor(row)">
            编辑文案
          </el-button>
          <el-button text :disabled="!row.links?.shortUrl" @click="handleCopy(row.links?.shortUrl)">
            复制短链
          </el-button>
          <el-button text :disabled="!row.links?.tpwd" @click="handleCopy(row.links?.tpwd)">
            淘口令
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'
import type { FormInstance, FormRules } from 'element-plus'
import { CpsAdzoneApi, type CpsAdzoneVO } from '@/api/cps/adzone'
import { CpsApiVendorApi, VENDOR_CODE_OPTIONS, type CpsApiVendorVO } from '@/api/cps/apiVendor'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import {
  CpsRebateToolboxApi,
  type CpsGoodsBatchTransferItemVO
} from '@/api/cps/rebateToolbox'
import { getUserPage, type UserVO } from '@/api/member/user/index'

interface TransferDraft {
  platformCode?: string
  originalContent?: string
  vendorCode?: string
}

const props = defineProps<{
  draft?: TransferDraft | null
}>()

const emit = defineEmits<{
  promotion: [value: string]
}>()

const message = useMessage()
const { copy } = useClipboard()
const formRef = ref<FormInstance>()
const loading = ref(false)
const vendorLoading = ref(false)
const adzoneLoading = ref(false)
const memberLoading = ref(false)
const platformOptions = ref<CpsPlatformVO[]>([])
const vendorOptions = ref<CpsApiVendorVO[]>([])
const adzoneOptions = ref<CpsAdzoneVO[]>([])
const memberOptions = ref<UserVO[]>([])
const resultRows = ref<CpsGoodsBatchTransferItemVO[]>([])
const formatOptions = ref(['promotionContent', 'shortUrl', 'tpwd'])

const formData = reactive({
  platformCode: 'taobao',
  memberId: undefined as number | undefined,
  vendorCode: undefined as string | undefined,
  adzoneId: undefined as string | undefined,
  originalText: ''
})

const formRules = reactive<FormRules>({
  platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
  memberId: [{ required: true, message: '请选择会员', trigger: 'change' }],
  originalText: [{ required: true, message: '请输入原始内容', trigger: 'blur' }]
})

const fallbackPlatformOptions: CpsPlatformVO[] = [
  { id: 0, platformCode: 'taobao', platformName: '淘宝', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'jd', platformName: '京东', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'pdd', platformName: '拼多多', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'douyin', platformName: '抖音', status: 1, createTime: new Date() }
]

const contentLines = computed(() =>
  formData.originalText
    .split(/\r?\n/)
    .map((item) => item.trim())
    .filter(Boolean)
)
const enabledVendorOptions = computed(() => vendorOptions.value.filter((item) => item.status === 1))
const enabledAdzoneOptions = computed(() => adzoneOptions.value.filter((item) => item.status === 1))
const successRows = computed(() => resultRows.value.filter((item) => item.status === 'SUCCESS'))

watch(
  () => props.draft,
  async (draft) => {
    if (!draft) return
    if (draft.platformCode) {
      formData.platformCode = draft.platformCode
      await handlePlatformChange()
    }
    if (draft.vendorCode) {
      formData.vendorCode = draft.vendorCode
    }
    if (draft.originalContent) {
      formData.originalText = formData.originalText
        ? `${formData.originalText.trim()}\n${draft.originalContent}`
        : draft.originalContent
    }
  }
)

const handleSubmit = async () => {
  if (contentLines.value.length === 0) {
    message.warning('请输入原始内容')
    return
  }
  if (contentLines.value.length > 20) {
    message.warning('每次最多支持 20 条内容批量转链')
    return
  }
  await formRef.value?.validate()
  loading.value = true
  try {
    const data = await CpsRebateToolboxApi.batchTransfer({
      platformCode: formData.platformCode,
      memberId: formData.memberId as number,
      vendorCode: formData.vendorCode,
      adzoneId: formData.adzoneId,
      originalContents: contentLines.value
    })
    resultRows.value = data.items || []
    const firstSuccess = resultRows.value.find((item) => item.status === 'SUCCESS')
    if (firstSuccess) {
      emit('promotion', buildOutputText(firstSuccess))
    }
    message.success(`转链完成：成功 ${data.successCount} 条，失败 ${data.failureCount} 条`)
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formData.vendorCode = undefined
  formData.adzoneId = undefined
  formData.originalText = ''
  resultRows.value = []
}

const handlePlatformChange = async () => {
  formData.vendorCode = undefined
  formData.adzoneId = undefined
  await Promise.all([loadVendorOptions(formData.platformCode), loadAdzoneOptions(formData.platformCode)])
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

const loadAdzoneOptions = async (platformCode: string) => {
  adzoneLoading.value = true
  try {
    adzoneOptions.value = await CpsAdzoneApi.getAdzoneListByPlatform(platformCode)
  } finally {
    adzoneLoading.value = false
  }
}

const searchMemberOptions = async (keyword: string) => {
  memberLoading.value = true
  try {
    const queryText = keyword?.trim()
    const data = await getUserPage({
      pageNo: 1,
      pageSize: 20,
      mobile: /^\d+$/.test(queryText || '') ? queryText : undefined,
      nickname: queryText && !/^\d+$/.test(queryText) ? queryText : undefined
    })
    memberOptions.value = data?.list || []
  } finally {
    memberLoading.value = false
  }
}

const handleMemberDropdownVisible = (visible: boolean) => {
  if (visible && memberOptions.value.length === 0) {
    searchMemberOptions('')
  }
}

const sendToEditor = (row: CpsGoodsBatchTransferItemVO) => {
  emit('promotion', buildOutputText(row))
}

const copyAllSuccess = async () => {
  const text = successRows.value.map((item) => buildOutputText(item)).filter(Boolean).join('\n\n')
  await handleCopy(text)
}

const handleCopy = async (text?: string) => {
  if (!text) return
  await copy(text)
  message.success('复制成功')
}

const buildOutputText = (row: CpsGoodsBatchTransferItemVO) => {
  const pieces: string[] = []
  if (formatOptions.value.includes('original')) pieces.push(row.originalContent)
  if (formatOptions.value.includes('promotionContent') && row.promotionContent) pieces.push(row.promotionContent)
  if (formatOptions.value.includes('shortUrl') && row.links?.shortUrl) pieces.push(row.links.shortUrl)
  if (formatOptions.value.includes('longUrl') && row.links?.longUrl) pieces.push(row.links.longUrl)
  if (formatOptions.value.includes('mobileUrl') && row.links?.mobileUrl) pieces.push(row.links.mobileUrl)
  if (formatOptions.value.includes('tpwd') && row.links?.tpwd) pieces.push(row.links.tpwd)
  return Array.from(new Set(pieces.filter(Boolean))).join('\n')
}

const platformLabel = (platformCode?: string) => {
  const platform = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (platform?.platformName) return platform.platformName
  return ({ taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }[platformCode || ''] || platformCode || '-')
}

const vendorLabel = (vendorCode?: string) => {
  if (!vendorCode) return '默认'
  return VENDOR_CODE_OPTIONS.find((item) => item.value === vendorCode)?.label || vendorCode
}

const formatVendorLabel = (item: CpsApiVendorVO) => item.vendorName || vendorLabel(item.vendorCode)

const formatMemberLabel = (item: UserVO) => {
  const parts = [`ID:${item.id}`]
  if (item.nickname) parts.push(item.nickname)
  if (item.name) parts.push(item.name)
  if (item.mobile) parts.push(item.mobile)
  return parts.join(' / ')
}

const formatAdzoneLabel = (item: CpsAdzoneVO) => {
  const name = item.adzoneName ? `${item.adzoneName} / ` : ''
  return `${name}${item.adzoneId}${item.isDefault === 1 ? ' / 默认' : ''}`
}

const statusText = (status?: string) =>
  ({ SUCCESS: '成功', PARSE_FAILED: '解析失败', LINK_FAILED: '转链失败' }[status || ''] || '失败')

const formatMoney = (value?: number) =>
  value === undefined || value === null ? '-' : `￥${Number(value).toFixed(2)}`

onMounted(async () => {
  await loadPlatformOptions()
  await handlePlatformChange()
})
</script>

<style scoped>
.tool-panel {
  display: flex;
  flex-direction: column;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.limit-text {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
