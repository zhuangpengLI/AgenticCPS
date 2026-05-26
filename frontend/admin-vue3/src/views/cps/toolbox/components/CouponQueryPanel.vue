<template>
  <div class="tool-panel">
    <el-form :model="queryParams" label-width="88px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="平台">
            <el-select v-model="queryParams.platformCode" filterable class="w-full" @change="loadVendorOptions">
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
          <el-form-item label="供应商">
            <el-select v-model="queryParams.vendorCode" clearable filterable class="w-full" placeholder="默认路由">
              <el-option
                v-for="item in vendorOptions"
                :key="`${item.vendorCode}:${item.platformCode}`"
                :label="item.vendorName || vendorLabel(item.vendorCode)"
                :value="item.vendorCode"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="最低券额">
            <el-input-number v-model="queryParams.couponAmountMin" :min="0" :precision="2" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label-width="0">
            <div class="panel-actions">
              <el-button type="primary" :loading="loading" @click="handleSearch">
                <Icon icon="ep:ticket" class="mr-5px" /> 查券
              </el-button>
              <el-button @click="handleReset">
                <Icon icon="ep:refresh" class="mr-5px" /> 重置
              </el-button>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="查询内容">
            <el-input
              v-model="queryParams.queryText"
              clearable
              placeholder="输入关键词、商品ID或商品链接"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="goods-summary">
      <el-tag effect="plain">{{ result?.summary || '仅展示有券商品' }}</el-tag>
      <el-tag effect="plain">搜索词：{{ result?.keyword || queryParams.queryText }}</el-tag>
    </div>

    <el-table v-loading="loading" :data="goodsList" border>
      <el-table-column label="商品" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          <div class="goods-title">{{ row.title || row.goodsId }}</div>
          <div class="goods-sub">{{ platformLabel(row.platformCode) }} / {{ row.shopName || row.brandName || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="券后价" width="110" align="center">
        <template #default="{ row }">{{ formatMoney(row.actualPrice) }}</template>
      </el-table-column>
      <el-table-column label="券额" width="100" align="center">
        <template #default="{ row }">{{ formatMoney(row.couponPrice) }}</template>
      </el-table-column>
      <el-table-column label="佣金" width="110" align="center">
        <template #default="{ row }">{{ formatMoney(row.commissionAmount) }}</template>
      </el-table-column>
      <el-table-column label="有效期" prop="couponEndTime" width="150" show-overflow-tooltip />
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" @click="sendToTransfer(row)">带入转链</el-button>
        </template>
      </el-table-column>
    </el-table>

    <Pagination
      v-model:limit="queryParams.pageSize"
      v-model:page="queryParams.pageNo"
      :total="total"
      @pagination="getCouponList"
    />
  </div>
</template>

<script setup lang="ts">
import { CpsApiVendorApi, VENDOR_CODE_OPTIONS, type CpsApiVendorVO } from '@/api/cps/apiVendor'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import {
  CpsRebateToolboxApi,
  type CpsGoodsCouponQueryRespVO
} from '@/api/cps/rebateToolbox'
import type { CpsGoodsSquareGoodsVO } from '@/api/cps/goodsSquare'

const emit = defineEmits<{
  transfer: [value: { platformCode: string; originalContent: string; vendorCode?: string }]
}>()

const message = useMessage()
const loading = ref(false)
const platformOptions = ref<CpsPlatformVO[]>([])
const vendorOptions = ref<CpsApiVendorVO[]>([])
const result = ref<CpsGoodsCouponQueryRespVO>()
const goodsList = computed(() => result.value?.list || [])
const total = computed(() => result.value?.total || 0)

const queryParams = reactive({
  platformCode: 'taobao',
  vendorCode: undefined as string | undefined,
  queryText: '今日精选',
  couponAmountMin: undefined as number | undefined,
  pageNo: 1,
  pageSize: 10
})

const fallbackPlatformOptions: CpsPlatformVO[] = [
  { id: 0, platformCode: 'taobao', platformName: '淘宝', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'jd', platformName: '京东', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'pdd', platformName: '拼多多', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'douyin', platformName: '抖音', status: 1, createTime: new Date() }
]

const handleSearch = () => {
  queryParams.pageNo = 1
  getCouponList()
}

const handleReset = async () => {
  Object.assign(queryParams, {
    platformCode: 'taobao',
    vendorCode: undefined,
    queryText: '今日精选',
    couponAmountMin: undefined,
    pageNo: 1,
    pageSize: 10
  })
  await loadVendorOptions()
  await getCouponList()
}

const getCouponList = async () => {
  if (!queryParams.queryText.trim()) {
    message.warning('请输入查询内容')
    return
  }
  loading.value = true
  try {
    result.value = await CpsRebateToolboxApi.queryCoupons({ ...queryParams })
  } finally {
    loading.value = false
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
  queryParams.vendorCode = undefined
  if (!queryParams.platformCode) return
  vendorOptions.value = await CpsApiVendorApi.getVendorListByPlatform(queryParams.platformCode)
}

const sendToTransfer = (item: CpsGoodsSquareGoodsVO) => {
  emit('transfer', {
    platformCode: item.platformCode,
    vendorCode: item.vendorCode || queryParams.vendorCode,
    originalContent: item.itemLink || item.goodsId
  })
}

const platformLabel = (platformCode?: string) => {
  const platform = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (platform?.platformName) return platform.platformName
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const vendorLabel = (vendorCode?: string) =>
  vendorCode ? VENDOR_CODE_OPTIONS.find((item) => item.value === vendorCode)?.label || vendorCode : '默认'

const formatMoney = (value?: number) =>
  value === undefined || value === null ? '-' : `￥${Number(value).toFixed(2)}`

onMounted(async () => {
  await loadPlatformOptions()
  await loadVendorOptions()
  await getCouponList()
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

.goods-summary {
  margin-bottom: 12px;
}

.goods-title {
  font-weight: 600;
}

.goods-sub {
  margin-top: 4px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}
</style>
