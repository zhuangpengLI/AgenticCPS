<template>
  <ContentWrap>
    <div class="mb-16px flex flex-wrap items-center justify-between gap-12px">
      <div>
        <div class="text-16px font-600">商品返利查询</div>
        <div class="mt-4px text-12px text-gray-500">单品查询继续保留；批量转链、解析和文案编辑请使用返利工具箱。</div>
      </div>
      <el-button type="primary" @click="goToolbox">
        <Icon icon="ep:tools" class="mr-5px" /> 进入返利工具箱
      </el-button>
    </div>
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="渠道" prop="platformCode">
            <el-select
              v-model="formData.platformCode"
              placeholder="请选择渠道"
              class="w-full"
              filterable
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
          <el-form-item label="会员ID" prop="memberId">
            <el-select
              v-model="formData.memberId"
              placeholder="请输入手机号或昵称搜索会员"
              clearable
              filterable
              remote
              reserve-keyword
              class="w-full"
              :remote-method="searchMemberOptions"
              :loading="memberLoading"
              @visible-change="handleMemberDropdownVisible"
              @change="handleMemberChange"
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
          <el-form-item label="API供应商" prop="vendorCode">
            <el-select
              v-model="formData.vendorCode"
              :placeholder="vendorPlaceholder"
              clearable
              filterable
              class="w-full"
              :disabled="!formData.platformCode"
              :loading="vendorLoading"
              @change="handleVendorChange"
            >
              <el-option
                v-for="item in enabledVendorOptions"
                :key="`${item.vendorCode}:${item.platformCode}`"
                :label="formatVendorOptionLabel(item)"
                :value="item.vendorCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="8">
          <el-form-item label="推广位" prop="adzoneId">
            <el-select
              v-model="formData.adzoneId"
              :placeholder="adzonePlaceholder"
              clearable
              filterable
              class="w-full"
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
        <el-col :xs="24" :sm="12" :lg="4">
          <el-form-item label-width="0">
            <div class="flex gap-2">
              <el-button type="primary" :loading="queryLoading" @click="handleQuery">
                <Icon icon="ep:search" class="mr-5px" /> 查询
              </el-button>
              <el-button @click="handleReset">
                <Icon icon="ep:refresh" class="mr-5px" /> 重置
              </el-button>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="原始内容" prop="originalContent">
            <el-input
              v-model="formData.originalContent"
              type="textarea"
              :rows="4"
              maxlength="2000"
              show-word-limit
              placeholder="粘贴商品链接、商品ID或平台口令"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
  </ContentWrap>

  <ContentWrap v-if="result">
    <el-alert
      :type="result.parseStatus === 'SUCCESS' ? 'success' : 'warning'"
      :title="statusText(result.parseStatus)"
      :description="result.parseMessage"
      show-icon
      :closable="false"
      class="mb-16px"
    />

    <el-row v-if="result.parseStatus === 'SUCCESS'" :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-descriptions title="商品信息" :column="1" border>
          <el-descriptions-item label="渠道">
            {{ platformLabel(result.goods?.platformCode || formData.platformCode) }}
          </el-descriptions-item>
          <el-descriptions-item label="商品ID">
            {{ result.goods?.goodsId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="goodsSign">
            {{ result.goods?.goodsSign || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="标题">
            {{ result.goods?.title || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="店铺">
            {{ result.goods?.shopName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="券后价">
            {{ formatMoney(result.goods?.actualPrice) }}
          </el-descriptions-item>
          <el-descriptions-item label="优惠券">
            {{ result.goods?.couponInfo || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="商品链接">
            <a
              v-if="result.goods?.itemLink"
              :href="result.goods.itemLink"
              target="_blank"
              class="text-blue-500"
            >
              打开
            </a>
            <span v-else>-</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-descriptions title="返利信息" :column="1" border>
          <el-descriptions-item label="佣金比例">
            {{ formatPercent(result.rebate?.commissionRate) }}
          </el-descriptions-item>
          <el-descriptions-item label="预估佣金">
            {{ formatMoney(result.rebate?.commissionAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="会员预估返利">
            {{ formatMoney(result.rebate?.estimateRebateAmount) }}
          </el-descriptions-item>
          <el-descriptions-item label="使用推广位">
            {{ result.rebate?.usedAdzoneId || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="API供应商">
            {{ vendorLabel(result.rebate?.usedVendorCode || formData.vendorCode) }}
          </el-descriptions-item>
          <el-descriptions-item label="转链记录">
            {{ result.transferRecordId || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </el-col>
    </el-row>

    <el-table v-if="result.parseStatus === 'SUCCESS'" :data="linkRows" class="mt-20px" border>
      <el-table-column label="内容" prop="label" width="120" />
      <el-table-column label="值" min-width="260">
        <template #default="{ row }">
          <el-input v-model="row.value" readonly />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90" align="center">
        <template #default="{ row }">
          <el-tooltip content="复制" placement="top">
            <el-button :disabled="!row.value" @click="handleCopy(row.value)">
              <Icon icon="ep:copy-document" />
            </el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'
import type { FormInstance, FormRules } from 'element-plus'
import { CpsAdzoneApi, type CpsAdzoneVO } from '@/api/cps/adzone'
import { CpsApiVendorApi, VENDOR_CODE_OPTIONS, type CpsApiVendorVO } from '@/api/cps/apiVendor'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import {
  CpsGoodsToolApi,
  type CpsGoodsRebateQueryReqVO,
  type CpsGoodsRebateQueryRespVO
} from '@/api/cps/goodsTool'
import { getUserPage, type UserVO } from '@/api/member/user/index'

defineOptions({ name: 'CpsGoodsRebateQuery' })

const message = useMessage()
const { copy } = useClipboard()
const router = useRouter()

const formRef = ref<FormInstance>()
const queryLoading = ref(false)
const adzoneLoading = ref(false)
const memberLoading = ref(false)
const vendorLoading = ref(false)
const platformOptions = ref<CpsPlatformVO[]>([])
const adzoneOptions = ref<CpsAdzoneVO[]>([])
const memberOptions = ref<UserVO[]>([])
const vendorOptions = ref<CpsApiVendorVO[]>([])
const result = ref<CpsGoodsRebateQueryRespVO>()

const formData = reactive<CpsGoodsRebateQueryReqVO>({
  platformCode: '',
  originalContent: '',
  memberId: undefined as unknown as number,
  vendorCode: undefined,
  adzoneId: undefined
})

const formRules = reactive<FormRules>({
  platformCode: [{ required: true, message: '请选择渠道', trigger: 'change' }],
  memberId: [{ required: true, message: '请选择会员', trigger: 'change' }],
  originalContent: [{ required: true, message: '请输入商品链接、商品ID或口令', trigger: 'blur' }]
})

const fallbackPlatformOptions: CpsPlatformVO[] = [
  { id: 0, platformCode: 'taobao', platformName: '淘宝', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'jd', platformName: '京东', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'pdd', platformName: '拼多多', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'douyin', platformName: '抖音', status: 1, createTime: new Date() }
]

const linkRows = computed(() => [
  { label: '短链接', value: result.value?.links?.shortUrl || '' },
  { label: '长链接', value: result.value?.links?.longUrl || '' },
  { label: '淘口令', value: result.value?.links?.tpwd || '' },
  { label: '移动端链接', value: result.value?.links?.mobileUrl || '' }
])

const currentPlatform = computed(() =>
  platformOptions.value.find((item) => item.platformCode === formData.platformCode)
)

const defaultVendorCode = computed(() => currentPlatform.value?.activeVendorCode)
const defaultAdzoneId = computed(() => currentPlatform.value?.defaultAdzoneId)
const enabledVendorOptions = computed(() => vendorOptions.value.filter((item) => item.status === 1))
const enabledAdzoneOptions = computed(() => adzoneOptions.value.filter((item) => item.status === 1))

const vendorPlaceholder = computed(() =>
  defaultVendorCode.value
    ? `未指定则使用平台默认：${vendorLabel(defaultVendorCode.value)}`
    : '未指定则使用平台默认供应商'
)

const adzonePlaceholder = computed(() =>
  defaultAdzoneId.value ? `未指定则使用平台默认：${defaultAdzoneId.value}` : '未指定则使用平台默认推广位'
)

const getPlatformOptions = async () => {
  try {
    const data = await CpsPlatformApi.getEnabledPlatformList()
    platformOptions.value = data?.length ? data : fallbackPlatformOptions
  } catch {
    platformOptions.value = fallbackPlatformOptions
  }
}

const getAdzoneOptions = async (platformCode?: string) => {
  adzoneOptions.value = []
  if (!platformCode) {
    return
  }
  adzoneLoading.value = true
  try {
    adzoneOptions.value = await CpsAdzoneApi.getAdzoneListByPlatform(platformCode)
  } finally {
    adzoneLoading.value = false
  }
}

const getVendorOptions = async (platformCode?: string) => {
  vendorOptions.value = []
  if (!platformCode) {
    return
  }
  vendorLoading.value = true
  try {
    vendorOptions.value = await CpsApiVendorApi.getVendorListByPlatform(platformCode)
  } finally {
    vendorLoading.value = false
  }
}

const getMemberOptions = async (keyword?: string) => {
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

const searchMemberOptions = (keyword: string) => {
  getMemberOptions(keyword)
}

const handleMemberDropdownVisible = (visible: boolean) => {
  if (visible && memberOptions.value.length === 0) {
    getMemberOptions()
  }
}

const handleMemberChange = () => {
  result.value = undefined
}

const handlePlatformChange = async () => {
  formData.adzoneId = undefined
  formData.vendorCode = undefined
  result.value = undefined
  await Promise.all([getAdzoneOptions(formData.platformCode), getVendorOptions(formData.platformCode)])
}

const handleVendorChange = () => {
  result.value = undefined
}

const handleQuery = async () => {
  await formRef.value?.validate()
  queryLoading.value = true
  try {
    result.value = await CpsGoodsToolApi.queryRebate(formData)
  } finally {
    queryLoading.value = false
  }
}

const handleReset = () => {
  formRef.value?.resetFields()
  formData.vendorCode = undefined
  formData.adzoneId = undefined
  adzoneOptions.value = []
  vendorOptions.value = []
  memberOptions.value = []
  result.value = undefined
}

const handleCopy = async (value: string) => {
  await copy(value)
  message.success('复制成功')
}

const goToolbox = () => {
  router.push('/cps/toolbox')
}

const platformLabel = (platformCode?: string) => {
  if (!platformCode) return '-'
  const platform = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (platform?.platformName) return platform.platformName
  return (
    {
      taobao: '淘宝',
      jd: '京东',
      pdd: '拼多多',
      douyin: '抖音'
    }[platformCode] || platformCode
  )
}

const vendorLabel = (vendorCode?: string) => {
  if (!vendorCode) return '-'
  return VENDOR_CODE_OPTIONS.find((item) => item.value === vendorCode)?.label || vendorCode
}

const formatVendorOptionLabel = (item: CpsApiVendorVO) => {
  const defaultText = item.vendorCode === defaultVendorCode.value ? ' / 平台默认' : ''
  return `${item.vendorName || vendorLabel(item.vendorCode)}${defaultText}`
}

const formatMemberLabel = (item: UserVO) => {
  const parts = [`ID:${item.id}`]
  if (item.nickname) parts.push(item.nickname)
  if (item.name) parts.push(item.name)
  if (item.mobile) parts.push(item.mobile)
  return parts.join(' / ')
}

const formatAdzoneLabel = (item: CpsAdzoneVO) => {
  const name = item.adzoneName ? `${item.adzoneName} / ` : ''
  const defaultText = item.adzoneId === defaultAdzoneId.value ? ' / 平台默认' : item.isDefault === 1 ? ' / 默认' : ''
  return `${name}${item.adzoneId}${defaultText}`
}

const statusText = (status?: string) => {
  return (
    {
      SUCCESS: '解析成功',
      PARSE_FAILED: '解析失败',
      LINK_FAILED: '转链失败'
    }[status || ''] || '处理失败'
  )
}

const formatMoney = (value?: number) => {
  return value === undefined || value === null ? '-' : `￥${Number(value).toFixed(2)}`
}

const formatPercent = (value?: number) => {
  return value === undefined || value === null ? '-' : `${Number(value).toFixed(2)}%`
}

onMounted(() => {
  getPlatformOptions()
})
</script>
