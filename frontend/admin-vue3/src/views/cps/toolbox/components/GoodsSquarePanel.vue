<template>
  <div class="tool-panel">
    <el-form :model="queryParams" label-width="72px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :lg="7">
          <el-form-item label="关键词">
            <el-input
              v-model="queryParams.keyword"
              clearable
              placeholder="默认今日精选"
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
            <el-select v-model="queryParams.platformCode" filterable class="w-full" @change="handlePlatformChange">
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
              clearable
              filterable
              class="w-full"
              placeholder="默认路由"
              :loading="vendorLoading"
              @change="handleSearch"
            >
              <el-option
                v-for="item in vendorOptions"
                :key="`${item.vendorCode}:${item.platformCode}`"
                :label="item.vendorName || vendorLabel(item.vendorCode)"
                :value="item.vendorCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="7">
          <el-form-item label-width="0">
            <div class="panel-actions">
              <el-button type="primary" :loading="loading" @click="handleSearch">
                <Icon icon="ep:search" class="mr-5px" /> 搜索
              </el-button>
              <el-button @click="handleReset">
                <Icon icon="ep:refresh" class="mr-5px" /> 重置
              </el-button>
              <el-checkbox :model-value="queryParams.hasCoupon === 1" @change="handleCouponOnlyChange">
                只看有券
              </el-checkbox>
            </div>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div v-if="showTaobaoSelection" class="selection-panel" v-loading="metaLoading">
      <div class="selection-row">
        <span class="selection-label">活动</span>
        <el-button
          v-for="item in metaInfo?.activities || []"
          :key="item.value"
          :type="queryParams.channelCode === item.value ? 'primary' : 'default'"
          plain
          @click="selectActivity(item.value)"
        >
          {{ item.label }}
        </el-button>
      </div>
      <div class="selection-row">
        <span class="selection-label">热词</span>
        <el-tag
          v-for="item in metaInfo?.hotKeywords || []"
          :key="item.value"
          class="clickable-tag"
          effect="plain"
          @click="selectKeyword(item.label)"
        >
          {{ item.label }}
        </el-tag>
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

    <div class="goods-summary">
      <el-tag effect="plain">{{ metaInfo?.capabilityDesc || '通用搜索与转链' }}</el-tag>
      <el-tag effect="plain">共 {{ total }} 个结果</el-tag>
    </div>

    <el-empty v-if="!loading && goodsList.length === 0" description="暂无商品" />
    <div v-else v-loading="loading" class="goods-grid">
      <div v-for="item in goodsList" :key="`${item.platformCode}:${item.goodsId}`" class="goods-card">
        <div class="goods-image">
          <el-image v-if="item.mainPic" :src="item.mainPic" fit="cover" lazy />
          <div v-else class="goods-image-placeholder">{{ platformLabel(item.platformCode) }}</div>
        </div>
        <div class="goods-body">
          <div class="goods-title">{{ item.title || '-' }}</div>
          <div class="price-row">
            <div>
              <span>券后价</span>
              <b class="price-main">{{ formatMoney(item.actualPrice) }}</b>
            </div>
            <div>
              <span>预估佣金</span>
              <b class="commission-main">{{ formatMoney(item.commissionAmount) }}</b>
            </div>
          </div>
          <div class="goods-meta">
            <el-tag size="small" effect="plain">{{ platformLabel(item.platformCode) }}</el-tag>
            <el-tag v-if="item.categoryName" size="small" effect="plain">{{ item.categoryName }}</el-tag>
            <el-tag v-if="item.monthSales" size="small" effect="plain">销量 {{ formatCount(item.monthSales) }}</el-tag>
          </div>
          <div class="shop-line">{{ item.shopName || item.brandName || '未知店铺' }}</div>
          <div class="card-actions">
            <el-button :disabled="!item.itemLink" @click="openOriginalLink(item.itemLink)">
              <Icon icon="ep:link" />
            </el-button>
            <el-button type="primary" @click="sendToTransfer(item)">
              <Icon icon="ep:connection" class="mr-5px" /> 带入转链
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
  </div>
</template>

<script setup lang="ts">
import { CpsApiVendorApi, VENDOR_CODE_OPTIONS, type CpsApiVendorVO } from '@/api/cps/apiVendor'
import {
  CpsGoodsSquareApi,
  GOODS_SORT_TYPE_OPTIONS,
  type CpsGoodsSquareGoodsVO,
  type CpsGoodsSquareMetaRespVO,
  type CpsGoodsSquareSearchReqVO
} from '@/api/cps/goodsSquare'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'

const emit = defineEmits<{
  transfer: [value: { platformCode: string; originalContent: string; vendorCode?: string }]
}>()

const loading = ref(false)
const metaLoading = ref(false)
const vendorLoading = ref(false)
const goodsList = ref<CpsGoodsSquareGoodsVO[]>([])
const total = ref(0)
const metaInfo = ref<CpsGoodsSquareMetaRespVO>()
const platformOptions = ref<CpsPlatformVO[]>([])
const vendorOptions = ref<CpsApiVendorVO[]>([])

const queryParams = reactive<CpsGoodsSquareSearchReqVO>({
  keyword: '今日精选',
  platformCode: 'taobao',
  vendorCode: undefined,
  pageNo: 1,
  pageSize: 20,
  sortType: 0,
  hasCoupon: undefined,
  categoryId: '0'
})

const fallbackPlatformOptions: CpsPlatformVO[] = [
  { id: 0, platformCode: 'taobao', platformName: '淘宝', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'jd', platformName: '京东', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'pdd', platformName: '拼多多', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'douyin', platformName: '抖音', status: 1, createTime: new Date() }
]

const showTaobaoSelection = computed(() => queryParams.platformCode === 'taobao')

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
    channelCode: undefined,
    categoryId: '0'
  })
  await handlePlatformChange()
}

const handlePlatformChange = async () => {
  queryParams.vendorCode = undefined
  queryParams.channelCode = undefined
  queryParams.categoryId = queryParams.platformCode === 'taobao' ? '0' : undefined
  await Promise.all([loadVendorOptions(), loadMeta()])
  handleSearch()
}

const handleCouponOnlyChange = (checked: string | number | boolean) => {
  queryParams.hasCoupon = checked ? 1 : undefined
  handleSearch()
}

const selectActivity = (value: string) => {
  queryParams.channelCode = queryParams.channelCode === value ? undefined : value
  queryParams.activityTag = queryParams.channelCode
  handleSearch()
}

const selectKeyword = (keyword: string) => {
  queryParams.keyword = keyword
  handleSearch()
}

const getGoodsList = async () => {
  loading.value = true
  try {
    const data = await CpsGoodsSquareApi.searchGoods(queryParams)
    goodsList.value = data.list || []
    total.value = data.total || goodsList.value.length
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

const loadPlatformOptions = async () => {
  try {
    const data = await CpsPlatformApi.getEnabledPlatformList()
    platformOptions.value = data?.length ? data : fallbackPlatformOptions
  } catch {
    platformOptions.value = fallbackPlatformOptions
  }
}

const loadVendorOptions = async () => {
  if (!queryParams.platformCode) return
  vendorLoading.value = true
  try {
    vendorOptions.value = await CpsApiVendorApi.getVendorListByPlatform(queryParams.platformCode)
  } finally {
    vendorLoading.value = false
  }
}

const sendToTransfer = (item: CpsGoodsSquareGoodsVO) => {
  emit('transfer', {
    platformCode: item.platformCode,
    vendorCode: item.vendorCode || queryParams.vendorCode || metaInfo.value?.vendorCode,
    originalContent: item.itemLink || item.goodsId
  })
}

const openOriginalLink = (url?: string) => {
  if (url) window.open(url, '_blank')
}

const platformLabel = (platformCode?: string) => {
  const option = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (option?.platformName) return option.platformName
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const vendorLabel = (vendorCode?: string) => {
  if (!vendorCode) return '默认'
  return VENDOR_CODE_OPTIONS.find((item) => item.value === vendorCode)?.label || vendorCode
}

const formatMoney = (value?: number) =>
  value === undefined || value === null ? '-' : `￥${Number(value).toFixed(2)}`

const formatCount = (value?: number) => {
  if (!value) return '0'
  return value >= 10000 ? `${(value / 10000).toFixed(1)}万` : `${value}`
}

onMounted(async () => {
  await loadPlatformOptions()
  await Promise.all([loadVendorOptions(), loadMeta()])
  await getGoodsList()
})
</script>

<style scoped>
.tool-panel {
  display: flex;
  flex-direction: column;
}

.panel-actions,
.goods-summary {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.selection-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.selection-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.selection-label {
  width: 44px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.clickable-tag {
  cursor: pointer;
}

.goods-summary {
  margin: 2px 0 14px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(246px, 1fr));
  gap: 14px;
}

.goods-card {
  overflow: hidden;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
}

.goods-image {
  height: 150px;
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
  color: var(--el-text-color-secondary);
  font-weight: 600;
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
  font-weight: 600;
  line-height: 21px;
}

.price-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-top: 10px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.price-row b {
  display: block;
  margin-top: 2px;
  font-size: 18px;
}

.price-main {
  color: var(--el-color-danger);
}

.commission-main {
  color: var(--el-color-warning);
}

.goods-meta {
  display: flex;
  height: 25px;
  flex-wrap: nowrap;
  gap: 6px;
  margin-top: 8px;
  overflow: hidden;
}

.shop-line {
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
</style>
