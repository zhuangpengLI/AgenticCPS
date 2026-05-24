<template>
  <div class="activity-center">
    <section class="center-hero">
      <div>
        <div class="hero-title">活动中心</div>
        <div class="hero-subtitle">
          覆盖主流电商 / 餐饮外卖 / 本地服务 / 生活特权全场景，实时更新全网活动
        </div>
      </div>
      <el-button type="primary" class="hero-action" @click="openForm('create')">
        <Icon icon="ep:plus" class="mr-5px" /> 新增活动
      </el-button>
    </section>

    <ContentWrap class="tab-wrap">
      <div class="platform-tabs">
        <button
          v-for="item in centerData.tabs"
          :key="item.platformCode"
          class="platform-tab"
          :class="{ active: queryParams.platformCode === item.platformCode }"
          @click="selectPlatform(item.platformCode)"
        >
          <el-avatar
            v-if="item.platformLogo"
            :src="item.platformLogo"
            :size="22"
            shape="square"
          />
          <Icon v-else :icon="platformIcon(item.platformCode)" />
          <span>{{ item.platformName }}</span>
          <em v-if="item.activityCount">{{ item.activityCount }}</em>
        </button>
      </div>
    </ContentWrap>

    <ContentWrap class="filter-wrap">
      <div class="filter-line">
        <el-segmented
          v-model="queryParams.billingType"
          :options="billingSegmentOptions"
          @change="handleQuery"
        />
        <el-segmented
          v-model="queryParams.sortMode"
          :options="sortOptions"
          @change="handleQuery"
        />
        <div class="filter-search">
          <el-input
            v-model="queryParams.keyword"
            placeholder="请输入搜索关键字"
            clearable
            @keyup.enter="handleQuery"
            @clear="handleQuery"
          >
            <template #append>
              <el-button type="primary" :loading="loading" @click="handleQuery">
                <Icon icon="ep:search" />
              </el-button>
            </template>
          </el-input>
          <div class="page-summary">
            <b>{{ queryParams.pageNo }}</b> / {{ pageCount }} 共{{ centerData.total }}个活动
          </div>
          <el-button :disabled="queryParams.pageNo <= 1" @click="changePage(-1)">
            <Icon icon="ep:arrow-left" />
          </el-button>
          <el-button :disabled="queryParams.pageNo >= pageCount" @click="changePage(1)">
            <Icon icon="ep:arrow-right" />
          </el-button>
        </div>
      </div>
    </ContentWrap>

    <ContentWrap>
      <el-empty v-if="!loading && centerData.cards.length === 0" description="暂无活动" />
      <div v-else v-loading="loading" class="activity-grid">
        <article
          v-for="item in centerData.cards"
          :key="item.id"
          class="activity-card"
          @click="handleCardClick(item)"
        >
          <div class="card-cover" :class="`theme-${platformTheme(item.platformCode)}`">
            <el-image v-if="item.mainPic" :src="item.mainPic" fit="cover" lazy />
            <div v-else class="cover-fallback">
              <div class="cover-platform">{{ item.platformName || platformLabel(item.platformCode) }}</div>
              <div class="cover-title">{{ item.activityName }}</div>
            </div>
            <el-tag class="billing-badge" effect="plain">{{ item.billingType || 'CPS' }}</el-tag>
            <div class="promotion-count">推广数:{{ item.promotionCount || 0 }}</div>
          </div>
          <div class="card-body">
            <div class="card-title-row">
              <h3>{{ item.activityName }}</h3>
              <Icon icon="ep:arrow-right" />
            </div>
            <p>{{ item.shortDesc || '活动持续更新中' }}</p>
            <div class="card-meta">
              <span>活动奖励</span>
              <b>{{ item.rebateDesc || '--' }}</b>
            </div>
            <div class="card-meta">
              <span>活动时间</span>
              <b>{{ formatWindow(item) }}</b>
            </div>
            <div class="card-footer">
              <el-tag size="small" effect="plain">{{ item.activityType }}</el-tag>
              <el-tag v-if="item.tagText" size="small" type="success" effect="plain">
                {{ item.tagText }}
              </el-tag>
              <div class="card-actions">
                <el-button link type="primary" @click.stop="openForm('update', item)">
                  编辑
                </el-button>
                <el-button link type="danger" @click.stop="handleDelete(item.id)">
                  删除
                </el-button>
              </div>
            </div>
          </div>
        </article>
      </div>
      <Pagination
        v-model:limit="queryParams.pageSize"
        v-model:page="queryParams.pageNo"
        :total="centerData.total"
        @pagination="getCenter"
      />
    </ContentWrap>

    <el-dialog v-model="formVisible" :title="formTitle" width="760px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="活动名称" prop="activityName">
              <el-input v-model="formData.activityName" placeholder="请输入活动名称" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="专题类型" prop="activityType">
              <el-select v-model="formData.activityType" class="w-full">
                <el-option
                  v-for="item in ACTIVITY_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="平台编码" prop="platformCode">
              <el-input v-model="formData.platformCode" placeholder="taobao/meituan/eleme" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="计费类型" prop="billingType">
              <el-select v-model="formData.billingType" class="w-full">
                <el-option
                  v-for="item in ACTIVITY_BILLING_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="推广数">
              <el-input-number v-model="formData.promotionCount" :min="0" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="状态" prop="status">
              <el-radio-group v-model="formData.status">
                <el-radio :label="1">启用</el-radio>
                <el-radio :label="0">禁用</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="主图" prop="mainPic">
              <el-input v-model="formData.mainPic" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="短描述" prop="shortDesc">
              <el-input v-model="formData.shortDesc" maxlength="120" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="奖励文案" prop="rebateDesc">
              <el-input v-model="formData.rebateDesc" maxlength="160" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="来源类型">
              <el-select v-model="formData.sourceType" class="w-full">
                <el-option label="运营配置" value="configured" />
                <el-option label="实时接口" value="vendor" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="外部活动ID">
              <el-input v-model="formData.externalActivityId" placeholder="供应商活动ID" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="标签">
              <el-input v-model="formData.tagText" placeholder="热门/最新/票券" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="跳转类型" prop="jumpType">
              <el-select v-model="formData.jumpType" class="w-full">
                <el-option
                  v-for="item in ACTIVITY_JUMP_TYPE_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="跳转地址">
              <el-input v-model="formData.jumpUrl" placeholder="外部链接或内部落地页地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="搜索关键词">
              <el-input v-model="formData.searchKeyword" placeholder="用于跳转商品广场搜索" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="排序" prop="sort">
              <el-input-number v-model="formData.sort" :min="0" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="上线时间">
              <el-date-picker
                v-model="formData.startTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="8">
            <el-form-item label="下线时间">
              <el-date-picker
                v-model="formData.endTime"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="formData.remark" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="formLoading" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import {
  ACTIVITY_BILLING_TYPE_OPTIONS,
  ACTIVITY_JUMP_TYPE_OPTIONS,
  ACTIVITY_TYPE_OPTIONS,
  CpsRebateActivityApi,
  type CpsRebateActivityCenterCardVO,
  type CpsRebateActivityCenterRespVO,
  type CpsRebateActivitySaveVO
} from '@/api/cps/rebateActivity'

defineOptions({ name: 'CpsRebateActivitySquare' })

const message = useMessage()
const router = useRouter()

const loading = ref(false)
const centerData = reactive<CpsRebateActivityCenterRespVO>({
  tabs: [],
  billingTypeOptions: [],
  cards: [],
  total: 0,
  pageNo: 1,
  pageSize: 12
})
const queryParams = reactive({
  pageNo: 1,
  pageSize: 12,
  platformCode: 'hot',
  billingType: 'all',
  keyword: '',
  sortMode: 'hot'
})

const formRef = ref<FormInstance>()
const formVisible = ref(false)
const formLoading = ref(false)
const formType = ref<'create' | 'update'>('create')
const formTitle = computed(() => (formType.value === 'create' ? '新增活动卡片' : '编辑活动卡片'))
const formData = reactive<CpsRebateActivitySaveVO>(buildDefaultForm())
const sortOptions = [
  { label: '热门', value: 'hot' },
  { label: '最新', value: 'latest' }
]
const formRules = reactive<FormRules>({
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  activityType: [{ required: true, message: '请选择专题类型', trigger: 'change' }],
  platformCode: [{ required: true, message: '请输入平台编码', trigger: 'blur' }],
  billingType: [{ required: true, message: '请选择计费类型', trigger: 'change' }],
  jumpType: [{ required: true, message: '请选择跳转类型', trigger: 'change' }],
  sort: [{ required: true, message: '请输入排序', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
})

const billingSegmentOptions = computed(() => {
  const options = centerData.billingTypeOptions?.length
    ? centerData.billingTypeOptions
    : [
        { label: '全部', value: 'all', count: 0 },
        { label: 'CPS', value: 'CPS', count: 0 },
        { label: 'CPA', value: 'CPA', count: 0 },
        { label: 'CPS+CPA', value: 'CPS+CPA', count: 0 }
      ]
  return options.map((item) => ({
    label: item.count ? `${item.label} ${item.count}` : item.label,
    value: item.value
  }))
})

const pageCount = computed(() => Math.max(1, Math.ceil(centerData.total / queryParams.pageSize)))

const getCenter = async () => {
  loading.value = true
  try {
    const data = await CpsRebateActivityApi.getActivityCenter({ ...queryParams })
    Object.assign(centerData, {
      tabs: data.tabs || [],
      billingTypeOptions: data.billingTypeOptions || [],
      cards: data.cards || [],
      total: data.total || 0,
      pageNo: data.pageNo || queryParams.pageNo,
      pageSize: data.pageSize || queryParams.pageSize
    })
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getCenter()
}

const selectPlatform = (platformCode: string) => {
  queryParams.platformCode = platformCode
  handleQuery()
}

const changePage = (delta: number) => {
  queryParams.pageNo = Math.min(pageCount.value, Math.max(1, queryParams.pageNo + delta))
  getCenter()
}

const openForm = async (type: 'create' | 'update', row?: CpsRebateActivityCenterCardVO) => {
  formType.value = type
  if (type === 'update' && row?.id) {
    const detail = await CpsRebateActivityApi.getActivity(row.id)
    Object.assign(formData, buildDefaultForm(), detail)
  } else {
    Object.assign(formData, buildDefaultForm(), {
      platformCode: queryParams.platformCode === 'hot' ? '' : queryParams.platformCode,
      billingType: queryParams.billingType === 'all' ? 'CPS' : queryParams.billingType
    })
  }
  formVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formType.value === 'create') {
      await CpsRebateActivityApi.createActivity(formData)
      message.success('新增成功')
    } else {
      await CpsRebateActivityApi.updateActivity(formData)
      message.success('保存成功')
    }
    formVisible.value = false
    await getCenter()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  await message.delConfirm()
  await CpsRebateActivityApi.deleteActivity(id)
  message.success('删除成功')
  await getCenter()
}

const handleCardClick = (item: CpsRebateActivityCenterCardVO) => {
  if (item.jumpType === 'url' && item.jumpUrl) {
    window.open(item.jumpUrl, '_blank')
    return
  }
  if (item.jumpType === 'search') {
    router.push({
      path: '/cps/goods/square',
      query: {
        platformCode: item.platformCode,
        keyword: item.searchKeyword || item.activityName,
        activityTag: item.tagText || item.externalActivityId || item.activityName
      }
    })
  }
}

const formatWindow = (item: CpsRebateActivityCenterCardVO) => {
  if (!item.startTime && !item.endTime) return '长期'
  if (!item.endTime) return `${formatDateText(item.startTime)}起`
  if (!item.startTime) return `截至${formatDateText(item.endTime)}`
  return `${formatDateText(item.startTime)}~${formatDateText(item.endTime)}`
}

const formatDateText = (value?: Date | string) => {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 10)
}

const platformLabel = (platformCode?: string) => {
  const map: Record<string, string> = {
    hot: '热门',
    meituan: '美团',
    eleme: '饿了么',
    douyin: '抖音',
    local_life: '本地生活',
    fliggy: '飞猪旅行',
    pdd: '拼多多',
    taobao: '淘宝',
    jd: '京东',
    vip: '唯品会'
  }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const platformIcon = (platformCode?: string) => {
  const map: Record<string, string> = {
    hot: 'ep:star-filled',
    meituan: 'ep:food',
    eleme: 'ep:bowl',
    douyin: 'ep:video-camera-filled',
    local_life: 'ep:shop',
    fliggy: 'ep:place',
    pdd: 'ep:goods-filled',
    taobao: 'ep:shopping-bag',
    jd: 'ep:van'
  }
  return platformCode ? map[platformCode] || 'ep:collection-tag' : 'ep:collection-tag'
}

const platformTheme = (platformCode?: string) => {
  const themes: Record<string, string> = {
    taobao: 'orange',
    jd: 'red',
    meituan: 'yellow',
    eleme: 'blue',
    pdd: 'rose',
    douyin: 'dark'
  }
  return platformCode ? themes[platformCode] || 'green' : 'green'
}

function buildDefaultForm(): CpsRebateActivitySaveVO {
  return {
    activityName: '',
    activityType: '外卖',
    platformCode: '',
    mainPic: '',
    shortDesc: '',
    rebateDesc: '',
    billingType: 'CPS',
    promotionCount: 0,
    sourceType: 'configured',
    externalActivityId: '',
    tagText: '',
    jumpType: 'search',
    jumpUrl: '',
    searchKeyword: '',
    sort: 0,
    status: 1,
    startTime: undefined,
    endTime: undefined,
    remark: ''
  }
}

onMounted(getCenter)
</script>

<style scoped>
.activity-center {
  min-height: 100%;
}

.center-hero {
  display: flex;
  min-height: 180px;
  align-items: flex-start;
  justify-content: space-between;
  padding: 34px 48px;
  color: #fff;
  background:
    linear-gradient(115deg, rgba(37, 99, 235, 0.96), rgba(30, 64, 175, 0.88)),
    linear-gradient(45deg, transparent 0 48%, rgba(255, 255, 255, 0.12) 49% 51%, transparent 52% 100%);
  background-size: auto, 42px 42px;
}

.hero-title {
  font-size: 34px;
  font-weight: 700;
  line-height: 1.2;
}

.hero-subtitle {
  margin-top: 18px;
  font-size: 15px;
  line-height: 1.7;
}

.hero-action {
  margin-top: 8px;
  border-color: rgba(255, 255, 255, 0.36);
  background: rgba(255, 255, 255, 0.16);
}

.tab-wrap {
  margin-top: -40px;
}

.platform-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(116px, 1fr));
  gap: 0;
}

.platform-tab {
  display: inline-flex;
  min-height: 54px;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 0;
  color: #303133;
  background: transparent;
  cursor: pointer;
}

.platform-tab.active {
  color: #fff;
  background: #4d83ff;
}

.platform-tab em {
  min-width: 20px;
  padding: 1px 6px;
  border-radius: 999px;
  color: #4d83ff;
  background: #eef4ff;
  font-style: normal;
  font-size: 12px;
}

.platform-tab.active em {
  color: #fff;
  background: rgba(255, 255, 255, 0.22);
}

.filter-line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 16px;
}

.filter-search {
  display: flex;
  min-width: 320px;
  flex: 1;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.filter-search :deep(.el-input) {
  max-width: 420px;
}

.page-summary {
  color: #606266;
  white-space: nowrap;
}

.page-summary b {
  color: #3f6ff5;
}

.activity-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 18px;
}

.activity-card {
  overflow: hidden;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.activity-card:hover {
  border-color: #b9cbff;
  box-shadow: 0 10px 28px rgba(59, 89, 152, 0.12);
  transform: translateY(-2px);
}

.card-cover {
  position: relative;
  height: 126px;
  overflow: hidden;
}

.card-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.cover-fallback {
  display: flex;
  height: 100%;
  flex-direction: column;
  justify-content: center;
  padding: 18px;
  color: #fff;
}

.cover-platform {
  font-size: 13px;
  opacity: 0.86;
}

.cover-title {
  margin-top: 8px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.25;
}

.theme-orange {
  background: linear-gradient(135deg, #ff7a1a, #ff3d2e);
}

.theme-red {
  background: linear-gradient(135deg, #d71920, #f97316);
}

.theme-yellow {
  background: linear-gradient(135deg, #f5b300, #ff7a1a);
}

.theme-blue {
  background: linear-gradient(135deg, #1677ff, #22c3ee);
}

.theme-rose {
  background: linear-gradient(135deg, #ef4444, #ec4899);
}

.theme-dark {
  background: linear-gradient(135deg, #111827, #475569);
}

.theme-green {
  background: linear-gradient(135deg, #10b981, #2563eb);
}

.billing-badge {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(255, 255, 255, 0.9);
}

.promotion-count {
  position: absolute;
  bottom: 0;
  left: 0;
  padding: 6px 14px;
  color: #fff;
  background: rgba(37, 99, 235, 0.72);
  font-size: 13px;
}

.card-body {
  padding: 16px;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.card-title-row h3 {
  overflow: hidden;
  margin: 0;
  color: #1f2937;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-body p {
  display: -webkit-box;
  height: 38px;
  margin: 10px 0 12px;
  overflow: hidden;
  color: #606266;
  font-size: 13px;
  line-height: 19px;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.card-meta span {
  flex: 0 0 auto;
  padding: 3px 7px;
  border-radius: 4px;
  color: #7b8794;
  background: #f5f7fb;
  font-size: 12px;
}

.card-meta b {
  min-width: 0;
  overflow: hidden;
  color: #2563eb;
  font-size: 14px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
}

.card-actions {
  display: flex;
  margin-left: auto;
  gap: 4px;
}

@media (max-width: 768px) {
  .center-hero {
    min-height: 160px;
    flex-direction: column;
    gap: 18px;
    padding: 26px 22px;
  }

  .hero-title {
    font-size: 28px;
  }

  .filter-search {
    min-width: 100%;
    justify-content: flex-start;
  }
}
</style>
