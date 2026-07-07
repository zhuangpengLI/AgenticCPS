<template>
  <ContentWrap class="qlist-shell">
    <div class="qlist-hero">
      <div class="brand-line">
        <span class="brand-main">返利商品广场</span>
      </div>
      <div class="qlist-search">
        <el-input
          v-model="queryParams.keyword"
          size="large"
          placeholder="请输入关键词/商品加密ID/商品链接"
          clearable
          @input="handleKeywordInput"
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-upload
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleImageFileChange"
              accept="image/png,image/jpeg,image/webp"
            >
              <el-tooltip content="图片搜商品" placement="top">
                <el-button :loading="imageSearchLoading">
                  <Icon icon="ep:picture" />
                </el-button>
              </el-tooltip>
            </el-upload>
            <el-button type="primary" :loading="loading" @click="handleSearch">
              <Icon icon="ep:search" />
            </el-button>
          </template>
        </el-input>
        <div class="hero-search-tips">
          <span
            v-for="item in headlineKeywords"
            :key="item.label"
            :class="['headline-chip', item.tone]"
            @click="selectKeyword(item.label)"
          >
            <Icon :icon="item.icon" />
            {{ item.label }}
          </span>
        </div>
      </div>
    </div>

    <div v-if="searchSuggestionOptions.length > 0" class="suggestion-row">
      <span class="suggestion-label">联想词</span>
      <el-tag
        v-for="item in searchSuggestionOptions"
        :key="item.value"
        class="clickable-tag"
        effect="plain"
        @click="selectKeyword(item.label)"
      >
        {{ item.label }}
        <span v-if="item.description" class="suggestion-desc">{{ item.description }}</span>
      </el-tag>
    </div>

    <div v-if="showTaobaoSelection" class="selection-panel" v-loading="metaLoading">
      <div class="selection-card activity-card">
        <span class="selection-label">推荐活动</span>
        <button
          v-for="item in metaInfo?.activities || []"
          :key="item.value"
          type="button"
          :class="['activity-banner', queryParams.channelCode === item.value ? 'active' : '']"
          @click="selectActivity(item)"
        >
          <span v-if="item.tag" class="activity-tag">{{ item.tag }}</span>
          {{ item.label }}
        </button>
      </div>
      <div class="selection-card topic-card">
        <span class="selection-label">更多主题</span>
        <el-tag
          v-for="item in topicOptions"
          :key="item.value"
          class="clickable-tag"
          :type="queryParams.activityTag === item.value ? 'danger' : 'info'"
          effect="plain"
          @click="selectTopic(item.value, item.label)"
        >
          {{ item.label }}
        </el-tag>
      </div>
      <div class="selection-card category-card">
        <span class="selection-label">热门分类</span>
        <el-button
          v-for="item in categorySegmentOptions"
          :key="item.value"
          :type="queryParams.categoryId === item.value ? 'primary' : 'default'"
          plain
          @click="selectCategory(String(item.value))"
        >
          {{ item.label }}
        </el-button>
        <button type="button" class="more-link">+多选</button>
      </div>
      <div class="selection-card hot-card">
        <span class="selection-label hot-label"><Icon icon="ep:hot-water" /> 持续热销</span>
        <el-tag
          v-for="item in lastingHotKeywords"
          :key="item"
          class="clickable-tag"
          effect="plain"
          @click="selectKeyword(item)"
        >
          {{ item }}
        </el-tag>
        <span class="selection-label hot-label"><Icon icon="ep:trend-charts" /> 短期飙升</span>
        <el-tag
          v-for="item in risingKeywords"
          :key="item"
          class="clickable-tag"
          effect="plain"
          @click="selectKeyword(item)"
        >
          {{ item }}
        </el-tag>
      </div>
      <div class="selection-card ranking-card">
        <span class="selection-label">爆品榜单</span>
        <el-tag
          v-for="item in rankingOptions"
          :key="item.value"
          class="clickable-tag"
          :type="queryParams.activityTag === item.value ? 'warning' : 'info'"
          effect="plain"
          @click="selectRanking(item)"
        >
          {{ item.label }}
        </el-tag>
      </div>
      <div class="selection-card keyword-card">
        <span class="selection-label">热词</span>
        <el-tag
          v-for="item in displayHotKeywords"
          :key="item.value"
          class="clickable-tag"
          :type="queryParams.keyword === item.label ? 'success' : 'info'"
          effect="plain"
          @click="selectKeyword(item.label)"
        >
          {{ item.label }}
        </el-tag>
      </div>
      <div class="selection-card sort-card">
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

    <div v-if="showTaobaoSelection" class="advanced-filter">
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
              <el-form-item label="最高券额">
                <el-input-number v-model="queryParams.couponPriceUpperLimit" :min="0" :precision="2" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="热销排名≥">
                <el-input-number v-model="queryParams.hotRankMin" :min="0" :precision="0" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="券到期≤">
                <el-input-number v-model="queryParams.couponExpireDays" :min="0" :precision="0" class="w-full" />
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="店铺类型">
                <el-select v-model="queryParams.shopType" clearable class="w-full" @change="handleShopTypeChange">
                  <el-option label="淘宝" value="taobao" />
                  <el-option label="天猫" value="tmall" />
                  <el-option label="天猫超市" value="tchaoshi" />
                  <el-option label="金牌卖家" value="gold" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="商品表现">
                <el-select
                  v-model="queryParams.goodsPerformance"
                  clearable
                  class="w-full"
                  @change="handleGoodsPerformanceChange"
                >
                  <el-option label="今日热销" value="daily" />
                  <el-option label="近2小时热销" value="two_hours" />
                  <el-option label="推广人数" value="promoter" />
                  <el-option label="推广社群数" value="community" />
                  <el-option label="领券量" value="coupon" />
                  <el-option label="最新上架" value="new" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :xs="24" :sm="12" :lg="4">
              <el-form-item label="商品属性">
                <div class="switch-group">
                  <el-switch v-model="queryParams.commercialOnly" active-text="商单" @change="handleSearch" />
                  <el-switch v-model="queryParams.preSaleOnly" active-text="预告" @change="handleSearch" />
                  <el-switch v-model="queryParams.tmallOnly" active-text="天猫" @change="handleSearch" />
                  <el-switch v-model="queryParams.brandOnly" active-text="品牌" @change="handleSearch" />
                  <el-switch v-model="queryParams.haitaoOnly" active-text="海淘" @change="handleSearch" />
                  <el-switch v-model="queryParams.goldSellerOnly" active-text="金牌" @change="handleSearch" />
                  <el-switch v-model="queryParams.tchaoshiOnly" active-text="天猫超市" @change="handleSearch" />
                  <el-switch v-model="queryParams.juhuasuanOnly" active-text="聚划算" @change="handleSearch" />
                  <el-switch v-model="queryParams.taoqianggouOnly" active-text="淘抢购" @change="handleSearch" />
                  <el-switch v-model="queryParams.inspectedGoodsOnly" active-text="验货" @change="handleSearch" />
                  <el-switch
                    v-model="queryParams.freeshipRemoteDistrict"
                    active-text="偏远包邮"
                    @change="handleSearch"
                  />
                </div>
              </el-form-item>
            </el-col>
          </el-row>
          <div class="quick-filter-row">
            <el-checkbox :model-value="queryParams.hasCoupon === 1" @change="handleCouponOnlyChange">
              只看有券
            </el-checkbox>
            <el-button @click="loadTopKeywords">
              <Icon icon="ep:refresh-right" class="mr-5px" /> 热搜
            </el-button>
            <el-button @click="handleReset">
              <Icon icon="ep:refresh" class="mr-5px" /> 重置
            </el-button>
          </div>
        </el-form>
    </div>

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
import type { FormInstance, FormRules, UploadFile } from 'element-plus'
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

const loading = ref(false)
const metaLoading = ref(false)
const vendorLoading = ref(false)
const adzoneLoading = ref(false)
const memberLoading = ref(false)
const imageSearchLoading = ref(false)
const goodsList = ref<CpsGoodsSquareGoodsVO[]>([])
const total = ref(0)
const metaInfo = ref<CpsGoodsSquareMetaRespVO>()
const platformOptions = ref<CpsPlatformVO[]>([])
const vendorOptions = ref<CpsApiVendorVO[]>([])
const adzoneOptions = ref<CpsAdzoneVO[]>([])
const memberOptions = ref<UserVO[]>([])
const hotKeywordOptions = ref<CpsGoodsSquareMetaItemVO[]>([])
const searchSuggestionOptions = ref<CpsGoodsSquareMetaItemVO[]>([])
let suggestionTimer: ReturnType<typeof setTimeout> | undefined

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
  searchField: 'title',
  hasCoupon: undefined,
  categoryId: '0',
  channelCode: routeText(route.query.activityTag),
  activityTag: routeText(route.query.activityTag)
})

const headlineKeywords = [
  { label: '高佣肉单精选集', icon: 'ep:money', tone: 'danger' },
  { label: '佣金补贴', icon: 'ep:coin', tone: 'warning' },
  { label: '爆款早餐', icon: 'ep:hot-water', tone: 'danger' },
  { label: '社群热推风扇', icon: 'ep:promotion', tone: 'default' },
  { label: '夏季清凉内裤', icon: 'ep:sunny', tone: 'success' }
]

const topicOptions = [
  { value: 'prediction', label: '爆品预测' },
  { value: 'return', label: '爆品返场' },
  { value: 'selection', label: '小客精选单' },
  { value: 'gift', label: '淘礼金补贴' },
  { value: 'vip', label: '88VIP立减' }
]
const rankingOptions = [
  { value: 'top', label: '爆品榜单', sortType: 0, keyword: '爆款', sourceCode: 'RANKING' },
  { value: 'two-hour-hot', label: '2小时热销榜', sortType: 1, keyword: '热销', sourceCode: 'HOT_SALE_RANK' },
  { value: 'must-promote', label: '必推榜', sortType: 4, keyword: '高佣' },
  { value: 'day-hot', label: '全天热销榜', sortType: 1, keyword: '销量' },
  { value: 'new-potential', label: '新品潜力榜', sortType: 6, keyword: '新品' },
  { value: 'hot-push', label: '热推榜', sortType: 5, keyword: '领券' }
]
const lastingHotKeywords = ['卫生巾', '纸巾', '软辅', '牛奶', '洗衣液']
const risingKeywords = ['防晒霜', '酱油', '面膜', '拖鞋', '口红']

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
const displayHotKeywords = computed(() =>
  hotKeywordOptions.value.length > 0 ? hotKeywordOptions.value : metaInfo.value?.hotKeywords || []
)
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

const loadTopKeywords = () => {
  const first = displayHotKeywords.value?.[0]?.label || headlineKeywords[0].label
  queryParams.keyword = first
  handleSearch()
}

const loadVendorGoods = async (sourceCode: string, label: string) => {
  loading.value = true
  try {
    const data = await CpsGoodsSquareApi.getVendorGoods({
      sourceCode,
      platformCode: 'taobao',
      vendorCode: queryParams.vendorCode || 'dataoke',
      pageSize: queryParams.pageSize
    })
    queryParams.platformCode = 'taobao'
    queryParams.vendorCode = queryParams.vendorCode || 'dataoke'
    queryParams.keyword = label
    queryParams.activityTag = sourceCode
    queryParams.channelCode = sourceCode
    queryParams.pageNo = 1
    goodsList.value = data.list || []
    total.value = data.total || 0
  } finally {
    loading.value = false
  }
}

const selectVendorSource = (sourceCode: string, label: string) => {
  loadVendorGoods(sourceCode, label)
}

const loadHotKeywords = async () => {
  try {
    hotKeywordOptions.value = await CpsGoodsSquareApi.getHotKeywords({
      platformCode: queryParams.platformCode,
      vendorCode: queryParams.vendorCode || 'dataoke',
      type: 1
    })
  } catch {
    hotKeywordOptions.value = []
  }
}

const handleKeywordInput = () => {
  if (suggestionTimer) {
    clearTimeout(suggestionTimer)
  }
  suggestionTimer = setTimeout(loadSuggestions, 250)
}

const loadSuggestions = async () => {
  const keyword = queryParams.keyword?.trim()
  if (!keyword || keyword.length < 2 || queryParams.searchField === 'goods_id') {
    searchSuggestionOptions.value = []
    return
  }
  try {
    searchSuggestionOptions.value = await CpsGoodsSquareApi.suggestKeywords({
      platformCode: queryParams.platformCode,
      vendorCode: queryParams.vendorCode || 'dataoke',
      keyword,
      type: 1
    })
  } catch {
    searchSuggestionOptions.value = []
  }
}

const handleReset = async () => {
  Object.assign(queryParams, {
    keyword: '今日精选',
    platformCode: 'taobao',
    vendorCode: undefined,
    pageNo: 1,
    pageSize: 20,
    sortType: 0,
    searchField: 'title',
    hasCoupon: undefined,
    priceLowerLimit: undefined,
    priceUpperLimit: undefined,
    channelCode: undefined,
    categoryId: '0',
    minCommissionRate: undefined,
    minCommissionAmount: undefined,
    minMonthSales: undefined,
    couponAmountMin: undefined,
    couponPriceUpperLimit: undefined,
    hotRankMin: undefined,
    couponExpireDays: undefined,
    tmallOnly: undefined,
    brandOnly: undefined,
    haitaoOnly: undefined,
    goldSellerOnly: undefined,
    tchaoshiOnly: undefined,
    juhuasuanOnly: undefined,
    taoqianggouOnly: undefined,
    inspectedGoodsOnly: undefined,
    freeshipRemoteDistrict: undefined,
    shopType: undefined,
    goodsPerformance: undefined,
    commercialOnly: undefined,
    preSaleOnly: undefined,
    activityTag: undefined
  })
  await loadVendorOptions('taobao')
  await loadMeta()
  await loadHotKeywords()
  await getGoodsList()
}

const handleCouponOnlyChange = (checked: string | number | boolean) => {
  queryParams.hasCoupon = checked ? 1 : undefined
  handleSearch()
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

const selectTopic = (value: string, label: string) => {
  queryParams.activityTag = queryParams.activityTag === value ? undefined : value
  queryParams.channelCode = queryParams.activityTag
  queryParams.keyword = label
  handleSearch()
}

const selectRanking = (item: {
  value: string
  label: string
  sortType: number
  keyword: string
  sourceCode?: string
}) => {
  if (item.sourceCode) {
    selectVendorSource(item.sourceCode, item.label)
    return
  }
  queryParams.activityTag = queryParams.activityTag === item.value ? undefined : item.value
  queryParams.channelCode = queryParams.activityTag
  queryParams.sortType = item.sortType
  queryParams.keyword = item.keyword
  handleSearch()
}

const selectCategory = (value: string) => {
  queryParams.categoryId = value
  handleSearch()
}

const handleShopTypeChange = () => {
  queryParams.tmallOnly = queryParams.shopType === 'tmall' ? true : queryParams.tmallOnly
  queryParams.tchaoshiOnly = queryParams.shopType === 'tchaoshi' ? true : queryParams.tchaoshiOnly
  queryParams.goldSellerOnly = queryParams.shopType === 'gold' ? true : queryParams.goldSellerOnly
  if (queryParams.shopType === 'taobao') {
    queryParams.tmallOnly = undefined
    queryParams.tchaoshiOnly = undefined
  }
  handleSearch()
}

const handleGoodsPerformanceChange = () => {
  const performanceSortMap: Record<string, number> = {
    daily: 1,
    two_hours: 1,
    promoter: 0,
    community: 0,
    coupon: 5,
    new: 6
  }
  if (queryParams.goodsPerformance && performanceSortMap[queryParams.goodsPerformance] !== undefined) {
    queryParams.sortType = performanceSortMap[queryParams.goodsPerformance]
  }
  handleSearch()
}

const handleImageFileChange = async (uploadFile: UploadFile) => {
  const raw = uploadFile.raw
  if (!raw) return
  if (raw.size > 1024 * 1024) {
    message.warning('图片不能超过 1MB')
    return
  }
  imageSearchLoading.value = true
  try {
    const imageBase64 = await readFileAsDataUrl(raw)
    const data = await CpsGoodsSquareApi.searchByImage({
      ...queryParams,
      platformCode: 'taobao',
      vendorCode: queryParams.vendorCode || 'dataoke',
      searchMode: 'dataoke_image',
      imageBase64,
      pageNo: 1,
      pageSize: queryParams.pageSize
    })
    queryParams.platformCode = 'taobao'
    queryParams.vendorCode = queryParams.vendorCode || 'dataoke'
    queryParams.keyword = '图片搜商品'
    queryParams.pageNo = 1
    goodsList.value = data.list || []
    total.value = data.total || 0
    message.success(`图片搜商品完成，找到 ${total.value} 个结果`)
  } finally {
    imageSearchLoading.value = false
  }
}

const readFileAsDataUrl = (file: File) =>
  new Promise<string>((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsDataURL(file)
  })

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
  await loadHotKeywords()
  await getGoodsList()
})
</script>

<style scoped>
.qlist-shell {
  background:
    radial-gradient(circle at 28% 0%, rgb(255 255 255 / 90%) 0, rgb(255 255 255 / 0%) 320px),
    linear-gradient(135deg, #eefaff 0%, #edf4ff 52%, #e7f0ff 100%);
  padding: 24px 28px 30px;
}

.qlist-hero {
  display: grid;
  grid-template-columns: minmax(260px, 320px) minmax(360px, 1fr);
  gap: 28px;
  align-items: center;
  max-width: 1500px;
  margin: 0 auto;
}

.brand-line {
  display: flex;
  align-items: baseline;
  gap: 20px;
  white-space: nowrap;
}

.brand-main {
  color: #1d6dff;
  font-size: 34px;
  font-weight: 800;
  line-height: 1;
}

.qlist-search :deep(.el-input__wrapper) {
  height: 54px;
  border-radius: 999px 0 0 999px;
  box-shadow: 0 0 0 2px #2f7dff inset;
  padding-left: 24px;
}

.qlist-search :deep(.el-input-group__append) {
  overflow: hidden;
  border-radius: 0 999px 999px 0;
  background: #fff;
  box-shadow: 0 0 0 2px #2f7dff inset;
}

.qlist-search :deep(.el-input-group__append .el-button) {
  min-width: 52px;
  height: 54px;
  border: 0;
  font-size: 22px;
}

.hero-search-tips {
  display: flex;
  min-height: 32px;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-top: 8px;
}

.headline-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #697386;
  font-size: 14px;
  cursor: pointer;
}

.headline-chip.danger {
  color: #ff4d6d;
}

.headline-chip.warning {
  color: #f39800;
}

.headline-chip.success {
  color: #16c979;
}

.suggestion-row,
.selection-panel,
.advanced-filter,
.goods-grid,
.el-pagination {
  max-width: 1500px;
  margin-right: auto;
  margin-left: auto;
}

.suggestion-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
}

.suggestion-label {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.suggestion-desc {
  margin-left: 4px;
  color: var(--el-text-color-placeholder);
}

.selection-panel {
  margin-top: 18px;
}

.selection-card {
  display: flex;
  min-height: 52px;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  border-radius: 12px;
  background: rgb(255 255 255 / 82%);
  padding: 13px 24px;
}

.selection-card + .selection-card {
  margin-top: 12px;
}

.activity-card,
.topic-card {
  border-bottom-right-radius: 0;
  border-bottom-left-radius: 0;
}

.topic-card {
  margin-top: 0;
  border-top: 1px solid #edf2f7;
  border-top-left-radius: 0;
  border-top-right-radius: 0;
}

.selection-label {
  display: inline-flex;
  width: 76px;
  flex: 0 0 auto;
  align-items: center;
  gap: 4px;
  color: #8a93a3;
  font-size: 14px;
}

.activity-banner {
  min-width: 136px;
  height: 42px;
  border: 0;
  border-radius: 6px;
  background: linear-gradient(135deg, #ff3f4c 0%, #ff8a2a 100%);
  color: #fff;
  cursor: pointer;
  font-size: 15px;
  font-weight: 700;
  padding: 0 14px;
}

.activity-banner:nth-of-type(2n) {
  background: linear-gradient(135deg, #ff6aa2 0%, #ffb12a 100%);
}

.activity-banner.active {
  box-shadow: 0 0 0 3px rgb(35 120 255 / 22%);
}

.activity-tag {
  display: inline-flex;
  height: 18px;
  min-width: 18px;
  align-items: center;
  justify-content: center;
  margin-right: 4px;
  border-radius: 4px;
  background: rgb(255 255 255 / 22%);
  color: #fff;
  font-size: 12px;
}

.hot-label {
  width: auto;
  margin-left: 8px;
  color: #606266;
}

.hot-label:first-child {
  margin-left: 0;
  color: #ff4d6d;
}

.more-link {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: #2378ff;
  cursor: pointer;
}

.clickable-tag {
  cursor: pointer;
}

.advanced-filter {
  margin-top: 12px;
  border-radius: 12px;
  background: rgb(255 255 255 / 82%);
  padding: 18px 24px 14px;
}

.quick-filter-row,
.switch-group {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.quick-filter-row {
  margin-top: 4px;
}

.goods-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  margin-top: 18px;
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
  .qlist-shell {
    padding: 16px;
  }

  .qlist-hero {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .brand-main {
    font-size: 28px;
  }

  .selection-label {
    width: 100%;
  }
}
</style>
