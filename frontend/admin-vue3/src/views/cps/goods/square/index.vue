<template>
  <ContentWrap>
    <div class="square-toolbar">
      <div class="toolbar-title">
        <div class="text-18px font-600">返利商品广场</div>
        <div class="mt-4px text-12px text-gray-500">
          {{ metaInfo?.capabilityDesc || '淘宝选品优先，其他平台保留通用搜索与转链' }}
        </div>
      </div>
      <div class="toolbar-status">
        <el-tag :type="metaInfo?.usingVendorMeta ? 'success' : 'info'" effect="plain">
          {{ metaInfo?.usingVendorMeta ? `实时接口 · ${vendorLabel(metaInfo.vendorCode)}` : '默认推荐' }}
        </el-tag>
        <el-tag effect="plain">共 {{ total }} 个结果</el-tag>
        <el-button type="primary" @click="goToolbox">
          <Icon icon="ep:tools" class="mr-5px" /> 返利工具箱
        </el-button>
      </div>
    </div>

    <el-form :model="queryParams" label-width="72px" class="mt-18px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :lg="7">
          <el-form-item label="关键词">
            <el-input
              v-model="queryParams.keyword"
              placeholder="默认今日精选"
              clearable
              @keyup.enter="handleSearch"
            >
              <template #prepend>
                <Icon icon="ep:search" />
              </template>
            </el-input>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="5">
          <el-form-item label="平台">
            <el-select
              v-model="queryParams.platformCode"
              placeholder="选择平台"
              filterable
              class="w-full"
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
        <el-col :xs="24" :sm="12" :lg="5">
          <el-form-item label="供应商">
            <el-select
              v-model="queryParams.vendorCode"
              placeholder="默认路由"
              clearable
              filterable
              class="w-full"
              :loading="vendorLoading"
              @change="handleVendorChange"
            >
              <el-option
                v-for="item in vendorOptions"
                :key="`${item.vendorCode}:${item.platformCode}`"
                :label="formatVendorLabel(item)"
                :value="item.vendorCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="7">
          <el-form-item label-width="0">
            <div class="toolbar-actions">
              <el-button type="primary" :loading="loading" @click="handleSearch">
                <Icon icon="ep:search" class="mr-5px" /> 搜索
              </el-button>
              <el-button @click="handleReset">
                <Icon icon="ep:refresh" class="mr-5px" /> 重置
              </el-button>
              <el-checkbox
                :model-value="queryParams.hasCoupon === 1"
                @change="handleCouponOnlyChange"
              >
                只看有券
              </el-checkbox>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div v-if="showTaobaoSelection" class="selection-panel" v-loading="metaLoading">
      <div class="selection-row">
        <span class="selection-label">活动入口</span>
        <el-button
          v-for="item in metaInfo?.activities || []"
          :key="item.value"
          :type="queryParams.channelCode === item.value ? 'primary' : 'default'"
          plain
          @click="selectActivity(item)"
        >
          <span v-if="item.tag" class="activity-tag">{{ item.tag }}</span>{{ item.label }}
        </el-button>
      </div>
      <div class="selection-row">
        <span class="selection-label">热词</span>
        <el-tag
          v-for="item in metaInfo?.hotKeywords || []"
          :key="item.value"
          class="clickable-tag"
          :type="queryParams.keyword === item.label ? 'success' : 'info'"
          effect="plain"
          @click="selectKeyword(item.label)"
        >
          {{ item.label }}
        </el-tag>
      </div>
      <div class="selection-row">
        <span class="selection-label">类目</span>
        <el-segmented
          v-model="queryParams.categoryId"
          :options="categorySegmentOptions"
          @change="handleSearch"
        />
      </div>
      <div class="selection-row">
        <span class="selection-label">排序</span>
        <el-radio-group v-model="queryParams.sortType" @change="handleSearch">
          <el-radio-button
            v-for="item in GOODS_SORT_TYPE_OPTIONS"
            :key="item.value"
            :label="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <el-collapse v-if="showTaobaoSelection" class="advanced-filter">
      <el-collapse-item name="filter">
        <template #title>
          <Icon icon="ep:filter" class="mr-6px" /> 高级筛选
        </template>
        <el-form :model="queryParams" label-width="96px">
          <el-row :gutter="12">
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="最低价">
                <el-input-number v-model="queryParams.priceLowerLimit" :min="0" :precision="2" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="最高价">
                <el-input-number v-model="queryParams.priceUpperLimit" :min="0" :precision="2" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="最低佣金率">
                <el-input-number v-model="queryParams.minCommissionRate" :min="0" :precision="2" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="最低销量">
                <el-input-number v-model="queryParams.minMonthSales" :min="0" :precision="0" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="最低券额">
                <el-input-number v-model="queryParams.couponAmountMin" :min="0" :precision="2" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="商品属性">
                <div class="switch-group">
                  <el-switch v-model="queryParams.tmallOnly" active-text="天猫" />
                  <el-switch v-model="queryParams.brandOnly" active-text="品牌" />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </el-collapse-item>
    </el-collapse>
  </ContentWrap>

  <ContentWrap>
    <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />
    <div v-else v-loading="loading" class="goods-grid">
      <div v-for="item in goodsList" :key="`${item.platformCode}:${item.goodsId}`" class="goods-card">
        <div class="goods-image">
          <el-image v-if="item.mainPic" :src="item.mainPic" fit="cover" lazy />
          <div v-else class="goods-image-placeholder">{{ platformLabel(item.platformCode) }}</div>
          <div class="image-tags">
            <el-tag size="small" type="danger" effect="dark">{{ platformLabel(item.platformCode) }}</el-tag>
            <el-tag v-if="item.rankTag" size="small" type="warning" effect="dark">{{ item.rankTag }}</el-tag>
          </div>
        </div>
        <div class="goods-body">
          <div class="goods-title">{{ item.title || '-' }}</div>
          <div v-if="item.sellingPoint" class="selling-point">{{ item.sellingPoint }}</div>
          <div class="tag-row">
            <el-tag v-if="item.activityTag" size="small" type="success" effect="plain">
              {{ item.activityTag }}
            </el-tag>
            <el-tag v-if="item.categoryName" size="small" effect="plain">{{ item.categoryName }}</el-tag>
            <el-tag v-if="item.source" size="small" effect="plain">{{ item.source }}</el-tag>
          </div>
          <div class="price-row">
            <div>
              <div class="meta-label">券后价</div>
              <div class="price-main">{{ formatMoney(item.actualPrice) }}</div>
            </div>
            <div>
              <div class="meta-label">预估佣金</div>
              <div class="commission-main">{{ formatMoney(item.commissionAmount) }}</div>
            </div>
          </div>
          <div class="metrics-grid">
            <div>
              <span>券</span>
              <b>{{ formatMoney(item.couponPrice) }}</b>
            </div>
            <div>
              <span>佣金率</span>
              <b>{{ formatPercent(item.commissionRate) }}</b>
            </div>
            <div>
              <span>销量</span>
              <b>{{ formatCount(item.monthSales) }}</b>
            </div>
            <div>
              <span>供应商</span>
              <b>{{ vendorLabel(effectiveVendorCode(item)) }}</b>
            </div>
          </div>
          <div class="shop-line">
            <Icon icon="ep:shop" />
            <span>{{ item.shopName || item.brandName || '未知店铺' }}</span>
          </div>
          <div v-if="item.couponEndTime" class="coupon-time">券有效期至 {{ item.couponEndTime }}</div>
          <div class="card-actions">
            <el-tooltip content="打开原始链接" placement="top">
              <el-button :disabled="!item.itemLink" @click="openOriginalLink(item.itemLink)">
                <Icon icon="ep:link" />
              </el-button>
            </el-tooltip>
            <el-button type="primary" @click="openLinkDialog(item)">
              <Icon icon="ep:connection" class="mr-5px" /> 转链
            </el-button>
          </div>
        </div>
      </div>
    </div>
    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getGoodsList"
    />
  </ContentWrap>

  <el-dialog v-model="linkVisible" title="生成推广内容" width="720px">
    <el-form ref="linkFormRef" :model="linkForm" :rules="linkFormRules" label-width="100px">
      <el-form-item label="商品">
        <div class="min-w-0">
          <div class="font-600">{{ selectedGoods?.title || '-' }}</div>
          <div class="mt-4px text-12px text-gray-500">
            {{ platformLabel(selectedGoods?.platformCode) }} · {{ selectedGoods?.goodsId }} ·
            {{ vendorLabel(linkForm.vendorCode) }}
          </div>
        </div>
      </el-form-item>
      <el-form-item label="会员" prop="memberId">
        <el-select
          v-model="linkForm.memberId"
          placeholder="搜索手机号、昵称或姓名"
          clearable
          filterable
          remote
          reserve-keyword
          class="w-full"
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
      <el-form-item label="推广位">
        <el-select
          v-model="linkForm.adzoneId"
          placeholder="默认推广位"
          clearable
          filterable
          class="w-full"
          :loading="adzoneLoading"
        >
          <el-option
            v-for="item in adzoneOptions"
            :key="item.adzoneId"
            :label="formatAdzoneLabel(item)"
            :value="item.adzoneId"
          />
        </el-select>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="linkResult"
      class="mt-16px"
      :type="linkResult.linkStatus === 'SUCCESS' ? 'success' : 'warning'"
      :title="linkResult.linkMessage || linkResult.linkStatus"
      show-icon
      :closable="false"
    />
    <el-table v-if="linkResult?.linkStatus === 'SUCCESS'" :data="linkRows" class="mt-16px" border>
      <el-table-column label="类型" prop="label" width="110" />
      <el-table-column label="内容" min-width="300">
        <template #default="{ row }">
          <el-input v-model="row.value" readonly />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="88" align="center">
        <template #default="{ row }">
          <el-tooltip content="复制" placement="top">
            <el-button :disabled="!row.value" @click="handleCopy(row.value)">
              <Icon icon="ep:copy-document" />
            </el-button>
          </el-tooltip>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="linkVisible = false">关闭</el-button>
      <el-button v-if="linkResult?.promotionContent" @click="handleCopy(linkResult.promotionContent)">
        <Icon icon="ep:copy-document" class="mr-5px" /> 复制推广文案
      </el-button>
      <el-button type="primary" :loading="linkLoading" @click="handleGenerateLink">生成链接</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'
import type { FormInstance, FormRules } from 'element-plus'
import { CpsAdzoneApi, type CpsAdzoneVO } from '@/api/cps/adzone'
import { CpsApiVendorApi, VENDOR_CODE_OPTIONS, type CpsApiVendorVO } from '@/api/cps/apiVendor'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import {
  CpsGoodsSquareApi,
  GOODS_SORT_TYPE_OPTIONS,
  type CpsGoodsSquareGoodsVO,
  type CpsGoodsSquareLinkReqVO,
  type CpsGoodsSquareLinkRespVO,
  type CpsGoodsSquareMetaItemVO,
  type CpsGoodsSquareMetaRespVO,
  type CpsGoodsSquareSearchReqVO
} from '@/api/cps/goodsSquare'
import { getUserPage, type UserVO } from '@/api/member/user/index'

defineOptions({ name: 'CpsGoodsSquare' })

const message = useMessage()
const { copy } = useClipboard()
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const metaLoading = ref(false)
const vendorLoading = ref(false)
const adzoneLoading = ref(false)
const memberLoading = ref(false)
const goodsList = ref<CpsGoodsSquareGoodsVO[]>([])
const total = ref(0)
const metaInfo = ref<CpsGoodsSquareMetaRespVO>()
const platformOptions = ref<CpsPlatformVO[]>([])
const vendorOptions = ref<CpsApiVendorVO[]>([])
const adzoneOptions = ref<CpsAdzoneVO[]>([])
const memberOptions = ref<UserVO[]>([])

const routeText = (value: unknown) => {
  if (Array.isArray(value)) {
    return value[0] as string | undefined
  }
  return value as string | undefined
}

const queryParams = reactive<CpsGoodsSquareSearchReqVO>({
  keyword: routeText(route.query.keyword) || '今日精选',
  platformCode: routeText(route.query.platformCode) || 'taobao',
  vendorCode: undefined,
  pageNo: 1,
  pageSize: 20,
  sortType: 0,
  hasCoupon: undefined,
  categoryId: '0',
  channelCode: routeText(route.query.activityTag),
  activityTag: routeText(route.query.activityTag)
})

const linkVisible = ref(false)
const linkLoading = ref(false)
const selectedGoods = ref<CpsGoodsSquareGoodsVO>()
const linkResult = ref<CpsGoodsSquareLinkRespVO>()
const linkFormRef = ref<FormInstance>()
const linkForm = reactive<CpsGoodsSquareLinkReqVO>({
  platformCode: '',
  goodsId: '',
  goodsSign: undefined,
  memberId: undefined as unknown as number,
  adzoneId: undefined,
  vendorCode: undefined,
  title: '',
  originalContent: ''
})
const linkFormRules = reactive<FormRules>({
  memberId: [{ required: true, message: '请选择会员', trigger: 'change' }]
})

const fallbackPlatformOptions: CpsPlatformVO[] = [
  { id: 0, platformCode: 'taobao', platformName: '淘宝', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'jd', platformName: '京东', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'pdd', platformName: '拼多多', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'douyin', platformName: '抖音', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'vip', platformName: '唯品会', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'meituan', platformName: '美团', status: 1, createTime: new Date() }
]

const showTaobaoSelection = computed(() => queryParams.platformCode === 'taobao')
const categorySegmentOptions = computed(() => {
  const categories = metaInfo.value?.categories?.length
    ? metaInfo.value.categories
    : [{ value: '0', label: '全部' }]
  return categories.map((item) => ({ label: item.label, value: item.value }))
})
const linkRows = computed(() => [
  { label: '短链', value: linkResult.value?.shortUrl || '' },
  { label: '长链', value: linkResult.value?.longUrl || '' },
  { label: '口令', value: linkResult.value?.tpwd || '' },
  { label: '移动端链接', value: linkResult.value?.mobileUrl || '' },
  { label: '推广文案', value: linkResult.value?.promotionContent || '' }
])

const getGoodsList = async () => {
  loading.value = true
  try {
    const data = await CpsGoodsSquareApi.searchGoods({ ...queryParams })
    goodsList.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const loadMeta = async () => {
  metaLoading.value = true
  try {
    metaInfo.value = await CpsGoodsSquareApi.getMeta({
      platformCode: queryParams.platformCode,
      vendorCode: queryParams.vendorCode
    })
  } finally {
    metaLoading.value = false
  }
}

const handleSearch = () => {
  queryParams.pageNo = 1
  getGoodsList()
}

const handleReset = async () => {
  Object.assign(queryParams, {
    keyword: '今日精选',
    platformCode: 'taobao',
    vendorCode: undefined,
    pageNo: 1,
    pageSize: 20,
    sortType: 0,
    hasCoupon: undefined,
    priceLowerLimit: undefined,
    priceUpperLimit: undefined,
    channelCode: undefined,
    categoryId: '0',
    minCommissionRate: undefined,
    minCommissionAmount: undefined,
    minMonthSales: undefined,
    couponAmountMin: undefined,
    tmallOnly: undefined,
    brandOnly: undefined,
    shopType: undefined,
    activityTag: undefined
  })
  await loadVendorOptions('taobao')
  await loadMeta()
  await getGoodsList()
}

const handlePlatformChange = async () => {
  queryParams.vendorCode = undefined
  vendorOptions.value = []
  resetTaobaoOnlyFilters()
  if (queryParams.platformCode) {
    await loadVendorOptions(queryParams.platformCode)
  }
  await loadMeta()
  handleSearch()
}

const handleVendorChange = async () => {
  await loadMeta()
  handleSearch()
}

const handleCouponOnlyChange = (checked: string | number | boolean) => {
  queryParams.hasCoupon = checked ? 1 : undefined
}

const selectActivity = (item: CpsGoodsSquareMetaItemVO) => {
  queryParams.channelCode = queryParams.channelCode === item.value ? undefined : item.value
  queryParams.activityTag = queryParams.channelCode
  handleSearch()
}

const selectKeyword = (keyword: string) => {
  queryParams.keyword = keyword
  handleSearch()
}

const resetTaobaoOnlyFilters = () => {
  queryParams.channelCode = undefined
  queryParams.categoryId = queryParams.platformCode === 'taobao' ? '0' : undefined
  queryParams.minCommissionRate = undefined
  queryParams.minCommissionAmount = undefined
  queryParams.minMonthSales = undefined
  queryParams.couponAmountMin = undefined
  queryParams.tmallOnly = undefined
  queryParams.brandOnly = undefined
  queryParams.shopType = undefined
  queryParams.activityTag = undefined
}

const openLinkDialog = async (item: CpsGoodsSquareGoodsVO) => {
  selectedGoods.value = item
  linkResult.value = undefined
  Object.assign(linkForm, {
    platformCode: item.platformCode,
    goodsId: item.goodsId,
    goodsSign: item.goodsSign,
    memberId: undefined,
    adzoneId: undefined,
    vendorCode: effectiveVendorCode(item),
    title: item.title,
    originalContent: item.itemLink || item.goodsId
  })
  linkVisible.value = true
  await loadAdzoneOptions(item.platformCode)
  nextTick(() => linkFormRef.value?.clearValidate())
}

const handleGenerateLink = async () => {
  await linkFormRef.value?.validate()
  linkLoading.value = true
  try {
    linkResult.value = await CpsGoodsSquareApi.generateLink(linkForm)
    if (linkResult.value.linkStatus === 'SUCCESS') {
      message.success('转链成功')
    } else {
      message.warning(linkResult.value.linkMessage || '转链失败')
    }
  } finally {
    linkLoading.value = false
  }
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
    const data = await getUserPage({ pageNo: 1, pageSize: 20, keyword })
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

const openOriginalLink = (url?: string) => {
  if (url) {
    window.open(url, '_blank')
  }
}

const handleCopy = async (text?: string) => {
  if (!text) return
  await copy(text)
  message.success('复制成功')
}

const goToolbox = () => {
  router.push('/cps/toolbox?tool=goods-square')
}

const effectiveVendorCode = (item: CpsGoodsSquareGoodsVO) => {
  return item.vendorCode || queryParams.vendorCode || metaInfo.value?.vendorCode
}

const platformLabel = (platformCode?: string) => {
  const option = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (option?.platformName) return option.platformName
  const map: Record<string, string> = {
    taobao: '淘宝',
    jd: '京东',
    pdd: '拼多多',
    douyin: '抖音',
    vip: '唯品会',
    meituan: '美团'
  }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const vendorLabel = (vendorCode?: string) => {
  if (!vendorCode) return '默认'
  return VENDOR_CODE_OPTIONS.find((item) => item.value === vendorCode)?.label || vendorCode
}

const formatVendorLabel = (item: CpsApiVendorVO) => {
  return `${item.vendorName || vendorLabel(item.vendorCode)} · ${item.vendorCode}`
}

const formatAdzoneLabel = (item: CpsAdzoneVO) => {
  return `${item.adzoneName || item.adzoneId}${item.isDefault === 1 ? ' · 默认' : ''}`
}

const formatMemberLabel = (item: UserVO) => {
  return `${item.nickname || item.name || item.mobile || item.id}（ID: ${item.id}）`
}

const formatMoney = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `¥${Number(value).toFixed(2)}`
}

const formatPercent = (value?: number) => {
  if (value === undefined || value === null) return '-'
  return `${Number(value).toFixed(2)}%`
}

const formatCount = (value?: number) => {
  if (!value) return '0'
  return value >= 10000 ? `${(value / 10000).toFixed(1)}万` : `${value}`
}

onMounted(async () => {
  await loadPlatformOptions()
  await loadVendorOptions('taobao')
  await loadMeta()
  await getGoodsList()
})
</script>

<style scoped>
.square-toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-status,
.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.toolbar-title {
  min-width: 0;
}

.selection-panel {
  margin-top: 8px;
  padding: 12px 0 4px;
  border-top: 1px solid var(--el-border-color-lighter);
}

.selection-row {
  display: flex;
  min-height: 36px;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.selection-label {
  width: 64px;
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.activity-tag {
  display: inline-flex;
  height: 18px;
  min-width: 18px;
  align-items: center;
  justify-content: center;
  margin-right: 4px;
  border-radius: 4px;
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
  font-size: 12px;
}

.clickable-tag {
  cursor: pointer;
}

.advanced-filter {
  margin-top: 6px;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 0;
}

.switch-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
}

.goods-card {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  transition: border-color 0.16s ease, box-shadow 0.16s ease;
}

.goods-card:hover {
  border-color: var(--el-color-primary-light-5);
  box-shadow: 0 8px 22px rgb(31 45 61 / 10%);
}

.goods-image {
  position: relative;
  height: 176px;
  overflow: hidden;
  background: #f5f7fa;
}

.goods-image :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.goods-image-placeholder {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  color: #606266;
  font-size: 18px;
  font-weight: 600;
}

.image-tags {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  gap: 6px;
}

.goods-body {
  padding: 12px;
}

.goods-title {
  display: -webkit-box;
  height: 42px;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--el-text-color-primary);
  font-size: 14px;
  font-weight: 600;
  line-height: 21px;
}

.selling-point {
  height: 18px;
  overflow: hidden;
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag-row {
  display: flex;
  height: 26px;
  flex-wrap: nowrap;
  gap: 6px;
  margin-top: 8px;
  overflow: hidden;
}

.price-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
}

.meta-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.price-main,
.commission-main {
  margin-top: 2px;
  font-size: 19px;
  font-weight: 700;
  line-height: 24px;
}

.price-main {
  color: var(--el-color-danger);
}

.commission-main {
  color: var(--el-color-warning);
}

.metrics-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
  font-size: 12px;
}

.metrics-grid div {
  display: flex;
  min-width: 0;
  justify-content: space-between;
  gap: 8px;
  padding: 6px 8px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.metrics-grid span {
  color: var(--el-text-color-secondary);
}

.metrics-grid b {
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-line,
.coupon-time {
  display: flex;
  align-items: center;
  gap: 5px;
  height: 20px;
  margin-top: 8px;
  overflow: hidden;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

@media (max-width: 768px) {
  .square-toolbar {
    flex-direction: column;
  }

  .selection-label {
    width: 100%;
  }
}
</style>
