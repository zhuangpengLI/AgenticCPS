<template>
  <ContentWrap class="detail-shell">
    <div class="detail-toolbar">
      <el-button @click="goBack">
        <Icon icon="ep:arrow-left" class="mr-5px" /> 返回广场
      </el-button>
      <div class="toolbar-actions">
        <el-button :disabled="!goods?.itemLink" @click="openOriginalLink(goods?.itemLink)">
          <Icon icon="ep:link" class="mr-5px" /> 原始链接
        </el-button>
        <el-button type="primary" :disabled="!goods" @click="openLinkDialog">
          <Icon icon="ep:connection" class="mr-5px" /> 推广赚{{ formatMoney(goods?.commissionAmount) }}
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && !goods" description="未找到商品详情" />

    <div v-else v-loading="loading" class="detail-layout">
      <section class="overview-panel">
        <div class="media-column">
          <el-image v-if="goods?.mainPic" class="main-image" :src="goods.mainPic" fit="cover" />
          <div v-else class="main-image image-placeholder">{{ platformLabel(goods?.platformCode) }}</div>

          <div class="shop-strip">
            <span>{{ goods?.shopName || goods?.brandName || '未知店铺' }}</span>
            <el-tag size="small" effect="plain">{{ platformLabel(goods?.platformCode) }}</el-tag>
          </div>
        </div>

        <div class="info-column">
          <div class="title-row">
            <el-tag type="warning" effect="dark">{{ vendorLabel(effectiveVendorCode) }}</el-tag>
            <h1>{{ goods?.title || '商品详情' }}</h1>
          </div>

          <div v-if="goods?.sellingPoint" class="selling-point">{{ goods.sellingPoint }}</div>

          <div class="tag-row">
            <el-tag v-if="goods?.activityTag" type="success" effect="plain">{{ goods.activityTag }}</el-tag>
            <el-tag v-if="goods?.rankTag" type="danger" effect="plain">{{ goods.rankTag }}</el-tag>
            <el-tag v-if="goods?.categoryName" effect="plain">{{ goods.categoryName }}</el-tag>
            <el-tag v-if="goods?.source" effect="plain">{{ goods.source }}</el-tag>
          </div>

          <div class="price-matrix">
            <div>
              <span>券后价</span>
              <b class="danger">{{ formatMoney(goods?.actualPrice) }}</b>
            </div>
            <div>
              <span>单价</span>
              <b>{{ formatMoney(goods?.originalPrice) }}</b>
            </div>
            <div>
              <span>佣金率</span>
              <b class="danger">{{ formatPercent(goods?.commissionRate) }}</b>
            </div>
            <div>
              <span>预估佣金</span>
              <b class="warning">{{ formatMoney(goods?.commissionAmount) }}</b>
            </div>
            <div>
              <span>优惠券</span>
              <b class="coupon">{{ formatMoney(goods?.couponPrice) }}</b>
            </div>
            <div>
              <span>月销量</span>
              <b>{{ formatCount(goods?.monthSales) }}</b>
            </div>
          </div>

          <div class="coupon-line">
            <Icon icon="ep:ticket" />
            <span>{{ couponText }}</span>
          </div>

          <div class="trend-panel">
            <div class="section-title">热卖趋势</div>
            <div class="trend-grid">
              <div v-for="item in trendItems" :key="item.label" class="trend-item">
                <span>{{ item.label }}</span>
                <b>{{ item.value }}</b>
                <div class="trend-bar"><i :style="{ width: item.percent }"></i></div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="promotion-panel">
        <div class="section-title">推广模板</div>
        <el-tabs v-model="activeTemplate">
          <el-tab-pane label="社群推广" name="community">
            <div class="template-box">{{ communityCopy }}</div>
          </el-tab-pane>
          <el-tab-pane label="朋友圈推广" name="moments">
            <div class="template-box">{{ momentsCopy }}</div>
          </el-tab-pane>
          <el-tab-pane label="长图推广" name="poster">
            <div class="template-box">{{ posterCopy }}</div>
          </el-tab-pane>
        </el-tabs>
        <div class="template-actions">
          <el-button @click="copyTemplate">
            <Icon icon="ep:document-copy" class="mr-5px" /> 复制文案
          </el-button>
          <el-button type="primary" :disabled="!goods" @click="openLinkDialog">
            <Icon icon="ep:connection" class="mr-5px" /> 生成推广内容
          </el-button>
        </div>
      </section>

      <section class="content-grid">
        <div class="material-panel">
          <div class="section-title">图片素材</div>
          <div class="material-grid">
            <button
              v-for="item in imageMaterials"
              :key="item.label"
              type="button"
              class="material-item"
              @click="previewImage(item.url)"
            >
              <el-image v-if="item.url" :src="item.url" fit="cover" />
              <span>{{ item.label }}</span>
            </button>
          </div>
        </div>

        <div class="similar-panel">
          <div class="section-title">相似推荐</div>
          <el-empty v-if="!similarLoading && similarGoods.length === 0" description="暂无相似商品" />
          <div v-else v-loading="similarLoading" class="similar-list">
            <button
              v-for="item in similarGoods"
              :key="`${item.platformCode}:${item.goodsId}`"
              type="button"
              class="similar-item"
              @click="switchGoods(item)"
            >
              <el-image v-if="item.mainPic" :src="item.mainPic" fit="cover" />
              <div>
                <b>{{ item.title }}</b>
                <span>{{ formatMoney(item.actualPrice) }} · 佣金 {{ formatPercent(item.commissionRate) }}</span>
              </div>
            </button>
          </div>
        </div>
      </section>
    </div>
  </ContentWrap>

  <el-dialog v-model="linkVisible" title="生成推广内容" width="720px">
    <el-form ref="linkFormRef" :model="linkForm" :rules="linkFormRules" label-width="100px">
      <el-form-item label="商品">
        <div class="min-w-0">
          <div class="font-600">{{ goods?.title || '-' }}</div>
          <div class="mt-4px text-12px text-gray-500">
            {{ platformLabel(goods?.platformCode) }} · {{ goods?.goodsId }} · {{ vendorLabel(linkForm.vendorCode) }}
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
      class="mb-12px"
      :type="linkResult.linkStatus === 'SUCCESS' ? 'success' : 'warning'"
      :title="linkResult.linkMessage || '已生成'"
      show-icon
      :closable="false"
    />
    <div v-if="linkResult" class="link-result-list">
      <div v-for="row in linkRows" :key="row.label" class="link-result-row">
        <span>{{ row.label }}</span>
        <el-input :model-value="row.value" readonly>
          <template #append>
            <el-button :disabled="!row.value" @click="handleCopy(row.value)">复制</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <template #footer>
      <el-button @click="linkVisible = false">关闭</el-button>
      <el-button type="primary" :loading="linkLoading" @click="handleGenerateLink">生成</el-button>
    </template>
  </el-dialog>

  <el-image-viewer
    v-if="previewVisible && previewUrl"
    :url-list="[previewUrl]"
    @close="previewVisible = false"
  />
</template>

<script setup lang="ts">
import { useClipboard } from '@vueuse/core'
import type { FormInstance, FormRules } from 'element-plus'
import { CpsAdzoneApi, type CpsAdzoneVO } from '@/api/cps/adzone'
import { VENDOR_CODE_OPTIONS } from '@/api/cps/apiVendor'
import {
  CpsGoodsSquareApi,
  type CpsGoodsSquareGoodsVO,
  type CpsGoodsSquareLinkReqVO,
  type CpsGoodsSquareLinkRespVO
} from '@/api/cps/goodsSquare'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import { getUserPage, type UserVO } from '@/api/member/user/index'

defineOptions({ name: 'CpsGoodsSquareDetail' })

const message = useMessage()
const route = useRoute()
const router = useRouter()
const { copy } = useClipboard()
const goodsSquareDetailCacheKey = 'cps:goods-square:detail'

const loading = ref(false)
const similarLoading = ref(false)
const adzoneLoading = ref(false)
const memberLoading = ref(false)
const linkVisible = ref(false)
const linkLoading = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
const activeTemplate = ref('community')
const goods = ref<CpsGoodsSquareGoodsVO>()
const similarGoods = ref<CpsGoodsSquareGoodsVO[]>([])
const platformOptions = ref<CpsPlatformVO[]>([])
const adzoneOptions = ref<CpsAdzoneVO[]>([])
const memberOptions = ref<UserVO[]>([])
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

const routeText = (value: unknown) => {
  if (Array.isArray(value)) {
    return value[0] as string | undefined
  }
  return value as string | undefined
}

const routeGoodsId = computed(() => routeText(route.query.goodsId))
const routePlatformCode = computed(() => routeText(route.query.platformCode) || 'taobao')
const routeVendorCode = computed(() => routeText(route.query.vendorCode))
const routeGoodsSign = computed(() => routeText(route.query.goodsSign))
const effectiveVendorCode = computed(() => goods.value?.vendorCode || routeVendorCode.value)
const couponText = computed(() => {
  if (!goods.value?.couponPrice) return '当前商品暂无可展示优惠券'
  const time = goods.value.couponEndTime ? `，有效期至 ${goods.value.couponEndTime}` : ''
  return `优惠券 ${formatMoney(goods.value.couponPrice)}${time}`
})
const trendItems = computed(() => {
  const sales = Number(goods.value?.monthSales || 0)
  const commission = Number(goods.value?.commissionRate || 0)
  const coupon = Number(goods.value?.couponPrice || 0)
  return [
    { label: '热销量级', value: formatCount(sales), percent: `${Math.min(100, Math.max(8, sales / 1000))}%` },
    { label: '佣金吸引力', value: formatPercent(commission), percent: `${Math.min(100, Math.max(8, commission * 4))}%` },
    { label: '券额力度', value: formatMoney(coupon), percent: `${Math.min(100, Math.max(8, coupon * 4))}%` }
  ]
})
const imageMaterials = computed(() => {
  const imageUrl = goods.value?.mainPic || ''
  return [
    { label: '商品主图', url: imageUrl },
    { label: '社群素材', url: imageUrl },
    { label: '朋友圈素材', url: imageUrl },
    { label: '长图封面', url: imageUrl }
  ].filter((item) => item.url)
})
const communityCopy = computed(() => {
  if (!goods.value) return ''
  return `【${goods.value.title}】券后 ${formatMoney(goods.value.actualPrice)}，券 ${formatMoney(goods.value.couponPrice)}，预估佣金 ${formatMoney(goods.value.commissionAmount)}。${goods.value.sellingPoint || '适合社群快速推荐。'}`
})
const momentsCopy = computed(() => {
  if (!goods.value) return ''
  return `${goods.value.title}\n券后价 ${formatMoney(goods.value.actualPrice)}｜月销 ${formatCount(goods.value.monthSales)}｜${couponText.value}`
})
const posterCopy = computed(() => {
  if (!goods.value) return ''
  return `${goods.value.title}\n到手价 ${formatMoney(goods.value.actualPrice)}\n佣金率 ${formatPercent(goods.value.commissionRate)}`
})
const activeCopy = computed(() => {
  const copyMap: Record<string, string> = {
    community: communityCopy.value,
    moments: momentsCopy.value,
    poster: posterCopy.value
  }
  return copyMap[activeTemplate.value] || communityCopy.value
})
const linkRows = computed(() => [
  { label: '短链', value: linkResult.value?.shortUrl || '' },
  { label: '长链', value: linkResult.value?.longUrl || '' },
  { label: '口令', value: linkResult.value?.tpwd || '' },
  { label: '移动端链接', value: linkResult.value?.mobileUrl || '' },
  { label: '推广文案', value: linkResult.value?.promotionContent || '' }
])

const loadGoodsFromCache = () => {
  const raw = sessionStorage.getItem(goodsSquareDetailCacheKey)
  if (!raw) return false
  try {
    const cached = JSON.parse(raw) as CpsGoodsSquareGoodsVO
    if (cached.goodsId && cached.goodsId === routeGoodsId.value) {
      goods.value = cached
      return true
    }
  } catch {
    sessionStorage.removeItem(goodsSquareDetailCacheKey)
  }
  return false
}

const loadGoods = async () => {
  if (loadGoodsFromCache()) return
  if (!routeGoodsId.value) return
  loading.value = true
  try {
    const data = await CpsGoodsSquareApi.searchGoods({
      keyword: routeGoodsId.value,
      searchField: 'goods_id',
      platformCode: routePlatformCode.value,
      vendorCode: routeVendorCode.value,
      pageNo: 1,
      pageSize: 1,
      goodsSign: routeGoodsSign.value
    })
    goods.value = data.list?.[0]
  } finally {
    loading.value = false
  }
}

const loadSimilarGoods = async () => {
  if (!goods.value) return
  similarLoading.value = true
  try {
    const keyword = goods.value.categoryName || goods.value.title?.slice(0, 12) || '今日精选'
    const data = await CpsGoodsSquareApi.searchGoods({
      keyword,
      platformCode: goods.value.platformCode,
      vendorCode: effectiveVendorCode.value,
      pageNo: 1,
      pageSize: 6,
      sortType: 0
    })
    similarGoods.value = (data.list || []).filter((item) => item.goodsId !== goods.value?.goodsId).slice(0, 5)
  } finally {
    similarLoading.value = false
  }
}

const switchGoods = async (item: CpsGoodsSquareGoodsVO) => {
  sessionStorage.setItem(goodsSquareDetailCacheKey, JSON.stringify(item))
  goods.value = item
  linkResult.value = undefined
  await router.replace({
    name: 'CpsGoodsSquareDetail',
    query: {
      platformCode: item.platformCode,
      vendorCode: item.vendorCode,
      goodsId: item.goodsId,
      goodsSign: item.goodsSign
    }
  })
  await loadSimilarGoods()
}

const openLinkDialog = async () => {
  if (!goods.value) return
  linkResult.value = undefined
  Object.assign(linkForm, {
    platformCode: goods.value.platformCode,
    goodsId: goods.value.goodsId,
    goodsSign: goods.value.goodsSign,
    memberId: undefined,
    adzoneId: undefined,
    vendorCode: effectiveVendorCode.value,
    title: goods.value.title,
    originalContent: goods.value.itemLink || goods.value.goodsId
  })
  linkVisible.value = true
  await loadAdzoneOptions(goods.value.platformCode)
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
    platformOptions.value = await CpsPlatformApi.getEnabledPlatformList()
  } catch {
    platformOptions.value = []
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

const copyTemplate = async () => {
  if (!activeCopy.value) return
  await copy(activeCopy.value)
  message.success('复制成功')
}

const handleCopy = async (text?: string) => {
  if (!text) return
  await copy(text)
  message.success('复制成功')
}

const previewImage = (url?: string) => {
  if (!url) return
  previewUrl.value = url
  previewVisible.value = true
}

const openOriginalLink = (url?: string) => {
  if (url) window.open(url, '_blank')
}

const goBack = () => {
  router.push({ name: 'CpsGoodsSquare' })
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
  await loadGoods()
  await loadSimilarGoods()
})
</script>

<style scoped>
.detail-shell {
  padding: 22px 26px 30px;
  background: linear-gradient(135deg, #effaff 0%, #f3f7ff 52%, #eef3ff 100%);
}

.detail-toolbar,
.detail-layout {
  max-width: 1480px;
  margin-right: auto;
  margin-left: auto;
}

.detail-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.detail-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview-panel,
.promotion-panel,
.material-panel,
.similar-panel {
  background: rgb(255 255 255 / 92%);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
}

.overview-panel {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 22px;
  padding: 18px;
}

.main-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background: #f5f7fa;
  border-radius: 8px;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--el-text-color-secondary);
  font-size: 22px;
  font-weight: 700;
}

.shop-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  padding: 10px 12px;
  background: var(--el-fill-color-lighter);
  border-radius: 6px;
  color: var(--el-text-color-regular);
  font-size: 13px;
}

.info-column {
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.title-row h1 {
  margin: 0;
  color: #1f2a44;
  font-size: 22px;
  font-weight: 800;
  line-height: 31px;
}

.selling-point {
  margin-top: 10px;
  color: #64748b;
  line-height: 22px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.price-matrix {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.price-matrix div {
  min-width: 0;
  padding: 12px 14px;
  background: #f7f9fc;
  border-radius: 8px;
}

.price-matrix span {
  display: block;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.price-matrix b {
  display: block;
  margin-top: 4px;
  color: #1f2a44;
  font-size: 22px;
  line-height: 28px;
}

.price-matrix .danger {
  color: #ff4d39;
}

.price-matrix .warning,
.price-matrix .coupon {
  color: #ff8a00;
}

.coupon-line {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  color: #667085;
}

.trend-panel {
  margin-top: 18px;
  padding: 14px;
  background: #f7f9ff;
  border-radius: 8px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2a44;
  font-size: 16px;
  font-weight: 800;
}

.section-title::before {
  width: 4px;
  height: 18px;
  background: #1d6dff;
  border-radius: 4px;
  content: '';
}

.trend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-top: 12px;
}

.trend-item {
  min-width: 0;
}

.trend-item span {
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.trend-item b {
  display: block;
  margin-top: 3px;
  color: #1f2a44;
  font-size: 17px;
}

.trend-bar {
  height: 6px;
  margin-top: 7px;
  overflow: hidden;
  background: #e7ecf5;
  border-radius: 999px;
}

.trend-bar i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #1d6dff 0%, #ff8a00 100%);
  border-radius: inherit;
}

.promotion-panel {
  padding: 16px 18px;
}

.template-box {
  min-height: 96px;
  padding: 14px;
  background: #f8fafc;
  border: 1px solid #edf2f7;
  border-radius: 8px;
  color: #334155;
  line-height: 24px;
  white-space: pre-wrap;
}

.template-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 12px;
}

.content-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 16px;
}

.material-panel,
.similar-panel {
  padding: 16px 18px;
}

.material-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(132px, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.material-item,
.similar-item,
.detail-entry {
  cursor: pointer;
}

.material-item {
  padding: 0;
  overflow: hidden;
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  text-align: left;
}

.material-item :deep(.el-image) {
  width: 100%;
  aspect-ratio: 1 / 1;
}

.material-item span {
  display: block;
  padding: 8px 10px;
  color: #475569;
  font-size: 12px;
}

.similar-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 14px;
}

.similar-item {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 10px;
  width: 100%;
  padding: 8px;
  background: #fff;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  text-align: left;
}

.similar-item :deep(.el-image) {
  width: 72px;
  height: 72px;
  border-radius: 6px;
}

.similar-item b {
  display: -webkit-box;
  overflow: hidden;
  color: #1f2a44;
  font-size: 13px;
  line-height: 19px;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.similar-item span {
  display: block;
  margin-top: 7px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.link-result-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.link-result-row {
  display: grid;
  grid-template-columns: 80px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
}

.link-result-row span {
  color: var(--el-text-color-secondary);
}

@media (width <= 960px) {
  .overview-panel,
  .content-grid {
    grid-template-columns: 1fr;
  }

  .price-matrix,
  .trend-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (width <= 640px) {
  .detail-shell {
    padding: 14px;
  }

  .detail-toolbar,
  .toolbar-actions,
  .template-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .price-matrix,
  .trend-grid {
    grid-template-columns: 1fr;
  }
}
</style>
