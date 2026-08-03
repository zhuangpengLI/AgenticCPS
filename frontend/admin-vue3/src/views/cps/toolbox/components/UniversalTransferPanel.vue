<template>
  <div class="tool-panel">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="76px">
      <el-row :gutter="12" class="transfer-topbar">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="平台" prop="platformCode">
            <el-select
              v-model="formData.platformCode"
              filterable
              class="w-full"
              placeholder="请选择平台"
              :disabled="platformLocked"
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
      </el-row>

      <div class="route-strip">
        <span>联盟账号：{{ selectedVendorName }}</span>
        <span>PID：{{ selectedAdzoneText }}</span>
        <el-tag size="small" type="success" effect="plain">{{ selectedRouteText }}</el-tag>
      </div>

      <el-alert
        class="toolbox-alert"
        type="warning"
        show-icon
        :closable="false"
        title="支持商品、店铺、会场链接或淘口令批量转链；淘宝纯数字 ID 受官方规则限制，请优先粘贴原始链接或口令。"
      />

      <div class="transfer-workbench">
        <section class="transfer-pane input-pane">
          <div class="pane-title-row">
            <div>
              <div class="pane-title">需要转链的内容</div>
              <div class="pane-subtitle">一行一条，最多 20 条，原文顺序会保留。</div>
            </div>
            <el-button text type="primary" @click="formData.originalText = ''">清空</el-button>
          </div>
          <el-form-item label-width="0" prop="originalText">
            <el-input
              v-model="formData.originalText"
              type="textarea"
              :rows="12"
              maxlength="5000"
              show-word-limit
              placeholder="粘贴商品链接、商品ID、淘口令或平台口令，每行一条。"
            />
          </el-form-item>
          <div class="input-hint">已识别 {{ contentLines.length }} 条；含券商品会优先按券后价转链。</div>
        </section>

        <section class="action-rail">
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">
            <Icon icon="ep:connection" class="mr-5px" /> 批量转链
          </el-button>
          <el-button
            v-if="formData.platformCode === 'taobao'"
            type="warning"
            size="large"
            plain
            :loading="loading"
            @click="handleSubsidySubmit"
          >
            <Icon icon="ep:present" class="mr-5px" /> 转百亿补贴
          </el-button>
          <el-button size="large" @click="handleReset">
            <Icon icon="ep:refresh" class="mr-5px" /> 重置
          </el-button>
          <el-button size="large" :disabled="successRows.length === 0" @click="copyAllSuccess">
            <Icon icon="ep:copy-document" class="mr-5px" /> 复制成功项
          </el-button>

          <div v-if="formData.platformCode === 'taobao'" class="play-mode-box">
            <div class="play-mode-title">淘宝玩法</div>
            <el-radio-group v-model="taobaoPlayMode" class="play-mode-radios">
              <el-radio label="normal">普通转链</el-radio>
              <el-radio label="super_red">品带超红</el-radio>
              <el-radio label="taojinbi">淘金币玩法</el-radio>
            </el-radio-group>
            <div class="play-mode-tip">品带超红和淘金币互斥，当前作为转链路由提示保留。</div>
          </div>
        </section>

        <section class="transfer-pane output-pane">
          <div class="pane-title-row">
            <div>
              <div class="pane-title">转链结果</div>
              <div class="pane-subtitle">可按运营风格选择输出字段。</div>
            </div>
          </div>
          <el-form-item label="输出格式" label-width="70px">
            <el-checkbox-group v-model="formatOptions" class="format-checks">
              <el-checkbox label="original">原格式</el-checkbox>
              <el-checkbox label="tpwd">淘口令</el-checkbox>
              <el-checkbox label="shortUrl">链接</el-checkbox>
              <el-checkbox label="promotionContent">推广文案</el-checkbox>
              <el-checkbox label="longUrl">长链</el-checkbox>
              <el-checkbox label="mobileUrl">移动端链接</el-checkbox>
              <el-checkbox label="kuaizhan" disabled>快站中转</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-input
            :model-value="resultOutput"
            type="textarea"
            :rows="12"
            readonly
            placeholder="批量转链后这里展示可复制结果。"
          />
          <div class="input-hint">可以继续在右侧文案编辑区做二次整理。</div>
        </section>
      </div>
    </el-form>

    <section v-if="visibleResultRows.length" class="transfer-detail">
      <div class="detail-title-row">
        <div>
          <div class="detail-title">转链明细</div>
          <div v-if="successRows.length" class="detail-subtitle">
            转链成功 {{ successRows.length }} 条<span v-if="failureRows.length">，转链失败 {{ failureRows.length }} 条</span>；成功商品已去重并加入转链记录。
          </div>
          <div v-else-if="allRowsParseFailed" class="detail-subtitle">
            全部内容均解析失败；请查看原文和失败原因。
          </div>
          <div v-else class="detail-subtitle">转链失败 {{ failureRows.length }} 条；请查看失败原因。</div>
        </div>
        <el-button type="primary" plain :disabled="successRows.length === 0" @click="copyAllSuccess">
          <Icon icon="ep:copy-document" class="mr-5px" /> 一键复制
        </el-button>
      </div>

      <div class="transfer-detail-head">
        <span>转链信息</span>
        <span>原链接/口令</span>
        <span>转链淘口令</span>
        <span>转链链接</span>
        <span>移动端链接</span>
        <span>操作</span>
      </div>

      <div
        v-for="row in visibleResultRows"
        :key="`${row.inputIndex}:${row.originalContent}`"
        class="transfer-detail-row"
      >
        <div class="goods-summary">
          <el-image
            v-if="row.goods?.mainPic"
            :src="row.goods.mainPic"
            fit="cover"
            class="goods-thumb"
            lazy
          />
          <div v-else class="goods-thumb goods-thumb-empty">无图</div>
          <div class="goods-meta">
            <div class="goods-title" :title="row.goods?.title || row.message || row.originalContent">
              {{ row.goods?.title || row.message || '未解析到商品' }}
            </div>
            <div class="goods-stats">
              <span>{{ formatMoney(row.goods?.actualPrice) }}</span>
              <span v-if="row.rebate?.commissionRate">佣金 {{ formatRate(row.rebate.commissionRate) }}</span>
              <span v-if="row.rebate?.estimateRebateAmount || row.rebate?.commissionAmount">
                返利 {{ formatMoney(row.rebate?.estimateRebateAmount || row.rebate?.commissionAmount) }}
              </span>
            </div>
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : 'warning'" effect="plain" size="small">
              {{ statusText(row.status) }}
            </el-tag>
          </div>
        </div>

        <div class="copy-cell">
          <span class="copy-text" :title="row.originalContent">{{ row.originalContent }}</span>
          <el-button link type="primary" @click="handleCopy(row.originalContent)">复制</el-button>
        </div>

        <div class="copy-cell">
          <span class="copy-text" :title="row.links?.tpwd || ''">{{ row.links?.tpwd || '-' }}</span>
          <el-button v-if="row.links?.tpwd" link type="primary" @click="handleCopy(row.links.tpwd)">复制</el-button>
        </div>

        <div class="copy-cell">
          <span class="copy-text" :title="row.links?.shortUrl || row.links?.longUrl || ''">
            {{ row.links?.shortUrl || row.links?.longUrl || '-' }}
          </span>
          <el-button
            v-if="row.links?.shortUrl || row.links?.longUrl"
            link
            type="primary"
            @click="handleCopy(row.links?.shortUrl || row.links?.longUrl)"
          >
            复制
          </el-button>
        </div>

        <div class="copy-cell">
          <span class="copy-text" :title="row.links?.mobileUrl || ''">{{ row.links?.mobileUrl || '-' }}</span>
          <el-button v-if="row.links?.mobileUrl" link type="primary" @click="handleCopy(row.links.mobileUrl)">复制</el-button>
        </div>

        <div class="detail-actions">
          <el-button link type="primary" :disabled="row.status !== 'SUCCESS'" @click="sendToEditor(row)">
            发文案区
          </el-button>
          <el-button link type="primary" :disabled="row.status !== 'SUCCESS'" @click="handleCopy(buildOutputText(row))">
            复制全部
          </el-button>
        </div>
      </div>
    </section>
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
import { selectVisibleTransferRows } from './transferResultRows.mjs'
import { getUserPage, type UserVO } from '@/api/member/user/index'

interface TransferDraft {
  platformCode?: string
  originalContent?: string
  vendorCode?: string
}

const props = defineProps<{
  draft?: TransferDraft | null
  lockedPlatformCode?: string
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
const taobaoPlayMode = ref<'normal' | 'super_red' | 'taojinbi'>('normal')
const platformLocked = computed(() => Boolean(props.lockedPlatformCode))

const formData = reactive({
  platformCode: props.lockedPlatformCode || 'taobao',
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
  { id: 0, platformCode: 'meituan', platformName: '美团', status: 1, createTime: new Date() },
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
const visibleResultRows = computed(() => selectVisibleTransferRows(resultRows.value))
const successRows = computed(() => visibleResultRows.value.filter((item) => item.status === 'SUCCESS'))
const failureRows = computed(() => visibleResultRows.value.filter((item) => item.status !== 'SUCCESS'))
const allRowsParseFailed = computed(
  () => resultRows.value.length > 0 && resultRows.value.every((item) => item.status === 'PARSE_FAILED')
)
const resultOutput = computed(() => successRows.value.map((item) => buildOutputText(item)).filter(Boolean).join('\n\n'))
const selectedVendor = computed(() =>
  enabledVendorOptions.value.find((item) => item.vendorCode === formData.vendorCode)
)
const selectedAdzone = computed(() =>
  enabledAdzoneOptions.value.find((item) => item.adzoneId === formData.adzoneId)
)
const selectedVendorName = computed(() => {
  if (selectedVendor.value) return formatVendorLabel(selectedVendor.value)
  const activeVendor = vendorOptions.value.find((item) => item.priority === 0 || item.remark?.includes('默认'))
  return activeVendor ? formatVendorLabel(activeVendor) : '默认供应商'
})
const selectedAdzoneText = computed(() => {
  if (selectedAdzone.value) return formatAdzoneLabel(selectedAdzone.value)
  const defaultAdzone = enabledAdzoneOptions.value.find((item) => item.isDefault === 1)
  return defaultAdzone ? formatAdzoneLabel(defaultAdzone) : '默认推广位'
})
const selectedRouteText = computed(
  () => `${platformLabel(formData.platformCode)} / ${vendorLabel(formData.vendorCode)}`
)

watch(
  () => props.draft,
  async (draft) => {
    if (!draft) return
    const nextPlatformCode = props.lockedPlatformCode || draft.platformCode
    if (nextPlatformCode) {
      formData.platformCode = nextPlatformCode
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

watch(
  () => props.lockedPlatformCode,
  async (platformCode) => {
    if (!platformCode) return
    formData.platformCode = platformCode
    platformOptions.value = ensurePlatformOption(platformOptions.value, platformCode)
    await handlePlatformChange()
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
    const firstSuccess = successRows.value[0]
    if (firstSuccess) {
      emit('promotion', buildOutputText(firstSuccess))
      message.success(`转链完成：成功 ${successRows.value.length} 条`)
    } else if (allRowsParseFailed.value) {
      message.warning(`解析失败：${failureRows.value.length} 条内容均未识别到商品`)
    } else {
      message.warning(`转链失败：${failureRows.value.length} 条`)
    }
  } finally {
    loading.value = false
  }
}

const handleSubsidySubmit = async () => {
  taobaoPlayMode.value = 'super_red'
  await handleSubmit()
}

const handleReset = () => {
  formData.platformCode = props.lockedPlatformCode || formData.platformCode
  formData.vendorCode = undefined
  formData.adzoneId = undefined
  formData.originalText = ''
  taobaoPlayMode.value = 'normal'
  resultRows.value = []
}

const handlePlatformChange = async () => {
  formData.vendorCode = undefined
  formData.adzoneId = undefined
  taobaoPlayMode.value = 'normal'
  await Promise.all([loadVendorOptions(formData.platformCode), loadAdzoneOptions(formData.platformCode)])
}

const loadPlatformOptions = async () => {
  try {
    const data = await CpsPlatformApi.getEnabledPlatformList()
    platformOptions.value = ensurePlatformOption(data?.length ? data : fallbackPlatformOptions, props.lockedPlatformCode)
  } catch {
    platformOptions.value = ensurePlatformOption(fallbackPlatformOptions, props.lockedPlatformCode)
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
  const text = resultOutput.value
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
  return ({ taobao: '淘宝', jd: '京东', pdd: '拼多多', meituan: '美团', douyin: '抖音' }[platformCode || ''] || platformCode || '-')
}

const ensurePlatformOption = (options: CpsPlatformVO[], platformCode?: string): CpsPlatformVO[] => {
  if (!platformCode || options.some((item) => item.platformCode === platformCode)) {
    return options
  }
  return [
    ...options,
    {
      id: 0,
      platformCode,
      platformName: platformLabel(platformCode),
      status: 1,
      createTime: new Date()
    }
  ]
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

const formatRate = (value?: number) =>
  value === undefined || value === null ? '-' : `${Number(value).toFixed(2)}%`

onMounted(async () => {
  if (props.lockedPlatformCode) {
    formData.platformCode = props.lockedPlatformCode
  }
  await loadPlatformOptions()
  await handlePlatformChange()
})
</script>

<style scoped>
.tool-panel {
  display: flex;
  flex-direction: column;
}

.transfer-topbar :deep(.el-form-item) {
  margin-bottom: 12px;
}

.transfer-workbench {
  display: grid;
  grid-template-columns: minmax(320px, 1.1fr) 190px minmax(320px, 1fr);
  gap: 14px;
  margin-top: 12px;
}

.transfer-pane {
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

.pane-subtitle,
.input-hint {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.pane-subtitle {
  margin-top: 3px;
}

.input-hint {
  margin-top: 8px;
}

.action-rail {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 10px;
}

.action-rail .el-button {
  width: 100%;
  margin-left: 0;
}

.play-mode-box {
  padding: 12px;
  border: 1px solid var(--el-color-danger-light-7);
  border-radius: 8px;
  background: var(--el-color-danger-light-9);
}

.play-mode-title {
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 700;
}

.play-mode-radios {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-top: 6px;
}

.format-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 0 12px;
}

.route-strip {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  color: var(--el-text-color-regular);
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  font-size: 13px;
}

.toolbox-alert {
  margin-top: 12px;
}

.transfer-detail {
  margin-top: 16px;
}

.detail-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-title {
  color: var(--el-text-color-primary);
  font-size: 16px;
  font-weight: 700;
}

.detail-subtitle {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.transfer-detail-head,
.transfer-detail-row {
  display: grid;
  grid-template-columns: minmax(280px, 1.4fr) minmax(190px, 1fr) minmax(160px, 0.9fr) minmax(190px, 1fr) minmax(190px, 1fr) 120px;
  gap: 12px;
  align-items: center;
}

.transfer-detail-head {
  padding: 10px 14px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
  border: 1px solid var(--el-border-color-lighter);
  font-size: 13px;
  font-weight: 600;
}

.transfer-detail-row {
  min-height: 96px;
  padding: 14px;
  border-right: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
  border-left: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
}

.goods-summary {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 12px;
}

.goods-thumb {
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  border-radius: 6px;
  background: var(--el-fill-color-light);
}

.goods-thumb-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
}

.goods-meta {
  min-width: 0;
}

.goods-title {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
  margin: 6px 0;
  color: var(--el-text-color-regular);
  font-size: 12px;
}

.copy-cell {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 8px;
}

.copy-text {
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-regular);
  font-size: 13px;
  line-height: 1.5;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 4px 8px;
}

.detail-actions .el-button {
  margin-left: 0;
}

.play-mode-tip {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.5;
}

@media (max-width: 1280px) {
  .transfer-workbench {
    grid-template-columns: 1fr;
  }

  .action-rail {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  }

  .play-mode-box {
    grid-column: 1 / -1;
  }

  .transfer-detail-head {
    display: none;
  }

  .transfer-detail-row {
    grid-template-columns: 1fr;
    gap: 10px;
    margin-bottom: 10px;
    border-top: 1px solid var(--el-border-color-lighter);
  }

  .detail-actions {
    justify-content: flex-start;
  }
}
</style>
