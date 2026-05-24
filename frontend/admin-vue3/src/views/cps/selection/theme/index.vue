<template>
  <div class="selection-page">
    <ContentWrap>
      <div class="page-toolbar">
        <div>
          <div class="text-18px font-600">选品库</div>
          <div class="mt-4px text-12px text-gray-500">
            主题规则 + 商品快照，支持 AI 推荐、第三方拉取和 MCP 只读推荐
          </div>
        </div>
        <div class="toolbar-actions">
          <el-button @click="loadTemplates">
            <Icon icon="ep:present" class="mr-5px" /> 大促模板
          </el-button>
          <el-button type="primary" @click="openThemeForm('create')">
            <Icon icon="ep:plus" class="mr-5px" /> 新建主题
          </el-button>
        </div>
      </div>

      <el-form :model="queryParams" label-width="76px" class="mt-18px">
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12" :lg="5">
            <el-form-item label="主题名称">
              <el-input
                v-model="queryParams.themeName"
                placeholder="搜索主题"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="4">
            <el-form-item label="状态">
              <el-select
                v-model="queryParams.status"
                clearable
                class="w-full"
                @change="handleQuery"
              >
                <el-option
                  v-for="item in SELECTION_THEME_STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="4">
            <el-form-item label="大促">
              <el-input
                v-model="queryParams.promotionEvent"
                placeholder="618/双11"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="4">
            <el-form-item label="平台">
              <el-input
                v-model="queryParams.platformCode"
                placeholder="taobao/jd"
                clearable
                @keyup.enter="handleQuery"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12" :lg="7">
            <el-form-item label-width="0">
              <div class="toolbar-actions">
                <el-button type="primary" :loading="themeLoading" @click="handleQuery">
                  <Icon icon="ep:search" class="mr-5px" /> 查询
                </el-button>
                <el-button @click="resetQuery">
                  <Icon icon="ep:refresh" class="mr-5px" /> 重置
                </el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </ContentWrap>

    <div class="selection-layout">
      <ContentWrap class="theme-pane">
        <div class="pane-head">
          <span>主题列表</span>
          <el-tag effect="plain">{{ themeTotal }}</el-tag>
        </div>
        <el-empty v-if="!themeLoading && themeList.length === 0" description="暂无主题" />
        <div v-else v-loading="themeLoading" class="theme-list">
          <button
            v-for="item in themeList"
            :key="item.id"
            class="theme-item"
            :class="{ active: selectedThemeId === item.id }"
            @click="selectTheme(item)"
          >
            <div class="theme-line">
              <span>{{ item.themeName }}</span>
              <el-tag size="small" :type="themeStatusMeta(item.status).type" effect="plain">
                {{ themeStatusMeta(item.status).label }}
              </el-tag>
            </div>
            <div class="theme-meta">
              {{ item.themeCode }} · {{ item.platformCodes || '全平台' }}
            </div>
            <div class="theme-tags">
              <el-tag v-if="item.promotionEvent" size="small" type="danger" effect="plain">
                {{ item.promotionEvent }}
              </el-tag>
              <el-tag v-if="item.refreshStatus" size="small" effect="plain">
                {{ item.refreshStatus }}
              </el-tag>
            </div>
          </button>
        </div>
        <Pagination
          v-model:limit="queryParams.pageSize"
          v-model:page="queryParams.pageNo"
          :total="themeTotal"
          small
          @pagination="getThemePage"
        />
      </ContentWrap>

      <ContentWrap class="content-pane">
        <div class="selected-head">
          <div class="min-w-0">
            <div class="selected-title">
              {{ selectedTheme?.themeName || '请选择主题' }}
              <el-tag
                v-if="selectedTheme"
                :type="themeStatusMeta(selectedTheme.status).type"
                effect="plain"
              >
                {{ themeStatusMeta(selectedTheme.status).label }}
              </el-tag>
            </div>
            <div class="selected-desc">
              {{ selectedTheme?.description || '创建主题后可导入商品快照并发布给 MCP 使用' }}
            </div>
          </div>
          <div class="toolbar-actions">
            <el-segmented v-model="viewMode" :options="viewOptions" />
            <el-button :disabled="!selectedTheme" @click="openThemeForm('update')">
              <Icon icon="ep:edit" />
            </el-button>
            <el-button type="success" :disabled="!selectedTheme" @click="handlePublish">
              <Icon icon="ep:upload" class="mr-5px" /> 发布
            </el-button>
            <el-button :disabled="!selectedTheme" @click="handleOffline">
              <Icon icon="ep:turn-off" class="mr-5px" /> 下线
            </el-button>
          </div>
        </div>

        <div class="action-panel">
          <el-button type="primary" :disabled="!selectedTheme" @click="openAiDrawer">
            <Icon icon="ep:magic-stick" class="mr-5px" /> AI 推荐
          </el-button>
          <el-button :disabled="!selectedTheme" @click="openVendorDrawer">
            <Icon icon="ep:download" class="mr-5px" /> 第三方拉取
          </el-button>
          <el-button :disabled="!selectedTheme" @click="openImportDialog">
            <Icon icon="ep:plus" class="mr-5px" /> 人工添加
          </el-button>
          <el-button
            :disabled="selectedItemIds.length === 0"
            @click="batchUpdateItemStatus('ENABLED')"
          >
            启用
          </el-button>
          <el-button
            :disabled="selectedItemIds.length === 0"
            @click="batchUpdateItemStatus('DISABLED')"
          >
            停用
          </el-button>
        </div>

        <el-empty v-if="!selectedTheme" description="从左侧选择一个主题" />
        <template v-else>
          <el-table
            v-if="viewMode === 'table'"
            v-loading="itemLoading"
            :data="itemList"
            row-key="id"
            @selection-change="handleItemSelectionChange"
          >
            <el-table-column type="selection" width="44" />
            <el-table-column label="商品" min-width="280">
              <template #default="{ row }">
                <div class="goods-cell">
                  <el-image v-if="row.mainPic" :src="row.mainPic" fit="cover" lazy />
                  <div v-else class="goods-placeholder">{{ platformLabel(row.platformCode) }}</div>
                  <div class="min-w-0">
                    <div class="goods-title">{{ row.title || '-' }}</div>
                    <div class="goods-meta"
                      >{{ row.goodsId }} · {{ row.shopName || row.categoryName || '-' }}</div
                    >
                    <div class="goods-tags">
                      <el-tag size="small" effect="plain">{{
                        platformLabel(row.platformCode)
                      }}</el-tag>
                      <el-tag v-if="row.sourceType" size="small" effect="plain">
                        {{ sourceLabel(row.sourceType) }}
                      </el-tag>
                      <el-tag v-if="row.topFlag === 1" size="small" type="warning" effect="plain">
                        置顶
                      </el-tag>
                    </div>
                  </div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="价格" width="120">
              <template #default="{ row }">
                <div class="price-main">{{ formatMoney(row.actualPrice) }}</div>
                <div class="text-12px text-gray-500">券 {{ formatMoney(row.couponPrice) }}</div>
              </template>
            </el-table-column>
            <el-table-column label="佣金" width="130">
              <template #default="{ row }">
                <div class="commission-main">{{ formatMoney(row.commissionAmount) }}</div>
                <div class="text-12px text-gray-500">{{ formatPercent(row.commissionRate) }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="monthSales" label="销量" width="110" />
            <el-table-column label="推荐" min-width="220">
              <template #default="{ row }">
                <div class="score-line">
                  <el-progress :percentage="normalizeScore(row.recommendScore)" :stroke-width="8" />
                  <span>{{ row.recommendScore || 0 }}</span>
                </div>
                <div class="recommend-reason">{{ row.recommendReason || '-' }}</div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="itemStatusMeta(row.status).type" effect="plain">
                  {{ itemStatusMeta(row.status).label }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="toggleTop(row)">
                  {{ row.topFlag === 1 ? '取消置顶' : '置顶' }}
                </el-button>
                <el-button link type="primary" @click="toggleItemStatus(row)">
                  {{ row.status === 'ENABLED' ? '停用' : '启用' }}
                </el-button>
                <el-button link type="danger" @click="deleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else v-loading="itemLoading" class="goods-grid">
            <article v-for="item in itemList" :key="item.id" class="goods-card">
              <div class="card-image">
                <el-image v-if="item.mainPic" :src="item.mainPic" fit="cover" lazy />
                <div v-else class="goods-placeholder">{{ platformLabel(item.platformCode) }}</div>
              </div>
              <div class="card-body">
                <div class="goods-title">{{ item.title || '-' }}</div>
                <div class="goods-tags">
                  <el-tag size="small" effect="plain">{{
                    platformLabel(item.platformCode)
                  }}</el-tag>
                  <el-tag v-if="item.activityTag" size="small" type="success" effect="plain">
                    {{ item.activityTag }}
                  </el-tag>
                </div>
                <div class="card-metrics">
                  <span>券后 {{ formatMoney(item.actualPrice) }}</span>
                  <span>佣金 {{ formatMoney(item.commissionAmount) }}</span>
                  <span>分 {{ item.recommendScore || 0 }}</span>
                </div>
                <p>{{ item.recommendReason || item.sellingPoint || '暂无推荐理由' }}</p>
              </div>
            </article>
          </div>
        </template>
      </ContentWrap>
    </div>

    <el-dialog v-model="themeFormVisible" :title="themeFormTitle" width="760px">
      <el-form ref="themeFormRef" :model="themeForm" :rules="themeRules" label-width="110px">
        <el-row :gutter="16">
          <el-col :xs="24" :sm="12">
            <el-form-item label="主题编码" prop="themeCode">
              <el-input v-model="themeForm.themeCode" placeholder="618_PRE" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="主题名称" prop="themeName">
              <el-input v-model="themeForm.themeName" placeholder="618预售" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="主题类型">
              <el-select v-model="themeForm.themeType" class="w-full">
                <el-option label="大促主题" value="PROMOTION" />
                <el-option label="日常主题" value="CUSTOM" />
                <el-option label="类目主题" value="CATEGORY" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="大促标识">
              <el-input v-model="themeForm.promotionEvent" placeholder="618/双11/年货节" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="平台范围">
              <el-input v-model="themeForm.platformCodes" placeholder="taobao,jd,pdd" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="供应商">
              <el-input v-model="themeForm.vendorCode" placeholder="dataoke/haodanku" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="描述">
              <el-input v-model="themeForm.description" maxlength="180" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="规则 JSON">
              <el-input v-model="themeForm.ruleJson" type="textarea" :rows="6" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="AI Prompt">
              <el-input v-model="themeForm.aiPrompt" type="textarea" :rows="3" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="themeFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="themeFormLoading" @click="submitThemeForm"
          >保存</el-button
        >
      </template>
    </el-dialog>

    <el-drawer v-model="operateDrawerVisible" :title="operateTitle" size="520px">
      <el-form label-position="top">
        <el-form-item v-if="operateMode === 'ai'" label="运营目标">
          <el-input
            v-model="operateObjective"
            type="textarea"
            :rows="4"
            placeholder="例如：618 防晒爆品，高券高佣优先"
          />
        </el-form-item>
        <el-form-item label="本次规则 JSON">
          <el-input v-model="operateRuleJson" type="textarea" :rows="12" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="operateDrawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="operateLoading" @click="submitOperate">
          执行
        </el-button>
      </template>
    </el-drawer>

    <el-dialog v-model="templateVisible" title="大促主题模板" width="820px">
      <div v-loading="templateLoading" class="template-grid">
        <article v-for="item in templates" :key="item.templateCode" class="template-card">
          <div class="template-title">{{ item.themeName }}</div>
          <p>{{ item.description }}</p>
          <div class="template-tags">
            <el-tag size="small" type="danger" effect="plain">{{ item.promotionEvent }}</el-tag>
            <el-tag size="small" effect="plain">{{ item.templateCode }}</el-tag>
          </div>
          <el-button type="primary" plain @click="createThemeFromTemplate(item)">
            创建草稿
          </el-button>
        </article>
      </div>
    </el-dialog>

    <el-dialog v-model="importVisible" title="人工添加商品快照" width="640px">
      <el-form label-position="top">
        <el-form-item label="商品 JSON 数组">
          <el-input v-model="manualImportJson" type="textarea" :rows="12" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :loading="manualImportLoading" @click="submitManualImport">
          导入
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CpsSelectionThemeApi,
  SELECTION_SOURCE_OPTIONS,
  SELECTION_THEME_ITEM_STATUS_OPTIONS,
  SELECTION_THEME_STATUS_OPTIONS,
  type CpsSelectionThemeImportItemVO,
  type CpsSelectionThemeItemVO,
  type CpsSelectionThemeSaveVO,
  type CpsSelectionThemeTemplateVO,
  type CpsSelectionThemeVO,
  type SelectionThemeItemStatus,
  type SelectionThemeSourceType,
  type SelectionThemeStatus
} from '@/api/cps/selectionTheme'

defineOptions({ name: 'CpsSelectionTheme' })

const themeLoading = ref(false)
const itemLoading = ref(false)
const themeList = ref<CpsSelectionThemeVO[]>([])
const itemList = ref<CpsSelectionThemeItemVO[]>([])
const themeTotal = ref(0)
const selectedThemeId = ref<number>()
const selectedItemIds = ref<number[]>([])
const viewMode = ref<'table' | 'card'>('table')
const viewOptions = [
  { label: '表格', value: 'table' },
  { label: '卡片', value: 'card' }
]

const queryParams = reactive({
  pageNo: 1,
  pageSize: 10,
  themeName: '',
  promotionEvent: '',
  platformCode: '',
  status: '' as SelectionThemeStatus | ''
})

const selectedTheme = computed(() =>
  themeList.value.find((item) => item.id === selectedThemeId.value)
)

const themeFormRef = ref<FormInstance>()
const themeFormVisible = ref(false)
const themeFormLoading = ref(false)
const themeFormType = ref<'create' | 'update'>('create')
const themeFormTitle = computed(() =>
  themeFormType.value === 'create' ? '新建选品主题' : '编辑选品主题'
)
const themeForm = reactive<CpsSelectionThemeSaveVO>(buildDefaultThemeForm())
const themeRules = reactive<FormRules>({
  themeCode: [{ required: true, message: '请输入主题编码', trigger: 'blur' }],
  themeName: [{ required: true, message: '请输入主题名称', trigger: 'blur' }]
})

const operateDrawerVisible = ref(false)
const operateLoading = ref(false)
const operateMode = ref<'ai' | 'vendor'>('ai')
const operateObjective = ref('')
const operateRuleJson = ref('')
const operateTitle = computed(() => (operateMode.value === 'ai' ? 'AI 推荐' : '第三方拉取'))

const templateVisible = ref(false)
const templateLoading = ref(false)
const templates = ref<CpsSelectionThemeTemplateVO[]>([])

const importVisible = ref(false)
const manualImportLoading = ref(false)
const manualImportJson = ref(defaultManualImportJson())

const getThemePage = async () => {
  themeLoading.value = true
  try {
    const data = await CpsSelectionThemeApi.getThemePage(queryParams)
    themeList.value = data.list || []
    themeTotal.value = data.total || 0
    if (!selectedThemeId.value && themeList.value.length > 0) {
      selectedThemeId.value = themeList.value[0].id
      await getItems()
    } else if (
      selectedThemeId.value &&
      !themeList.value.some((item) => item.id === selectedThemeId.value)
    ) {
      selectedThemeId.value = themeList.value[0]?.id
      await getItems()
    }
  } finally {
    themeLoading.value = false
  }
}

const getItems = async () => {
  if (!selectedThemeId.value) {
    itemList.value = []
    return
  }
  itemLoading.value = true
  try {
    itemList.value = await CpsSelectionThemeApi.listItems(selectedThemeId.value)
  } finally {
    itemLoading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getThemePage()
}

const resetQuery = () => {
  queryParams.pageNo = 1
  queryParams.themeName = ''
  queryParams.promotionEvent = ''
  queryParams.platformCode = ''
  queryParams.status = ''
  getThemePage()
}

const selectTheme = async (theme: CpsSelectionThemeVO) => {
  selectedThemeId.value = theme.id
  selectedItemIds.value = []
  await getItems()
}

const openThemeForm = (type: 'create' | 'update') => {
  themeFormType.value = type
  Object.assign(themeForm, type === 'create' ? buildDefaultThemeForm() : selectedTheme.value)
  themeFormVisible.value = true
}

const submitThemeForm = async () => {
  await themeFormRef.value?.validate()
  themeFormLoading.value = true
  try {
    if (themeFormType.value === 'create') {
      selectedThemeId.value = await CpsSelectionThemeApi.createTheme(themeForm)
    } else {
      await CpsSelectionThemeApi.updateTheme(themeForm)
    }
    ElMessage.success('保存成功')
    themeFormVisible.value = false
    await getThemePage()
  } finally {
    themeFormLoading.value = false
  }
}

const handlePublish = async () => {
  if (!selectedThemeId.value) return
  await ElMessageBox.confirm('发布后 MCP 可查询该主题，确认发布？', '发布主题', { type: 'warning' })
  await CpsSelectionThemeApi.publishTheme(selectedThemeId.value)
  ElMessage.success('发布成功')
  await getThemePage()
}

const handleOffline = async () => {
  if (!selectedThemeId.value) return
  await ElMessageBox.confirm('下线后 MCP 不再返回该主题，确认下线？', '下线主题', {
    type: 'warning'
  })
  await CpsSelectionThemeApi.offlineTheme(selectedThemeId.value)
  ElMessage.success('已下线')
  await getThemePage()
}

const openAiDrawer = () => {
  operateMode.value = 'ai'
  operateObjective.value = selectedTheme.value?.aiPrompt || ''
  operateRuleJson.value = selectedTheme.value?.ruleJson || defaultRuleJson()
  operateDrawerVisible.value = true
}

const openVendorDrawer = () => {
  operateMode.value = 'vendor'
  operateObjective.value = ''
  operateRuleJson.value = selectedTheme.value?.ruleJson || defaultRuleJson()
  operateDrawerVisible.value = true
}

const submitOperate = async () => {
  if (!selectedThemeId.value) return
  operateLoading.value = true
  try {
    const data =
      operateMode.value === 'ai'
        ? await CpsSelectionThemeApi.aiRecommend({
            themeId: selectedThemeId.value,
            objective: operateObjective.value,
            ruleJson: operateRuleJson.value
          })
        : await CpsSelectionThemeApi.vendorPull({
            themeId: selectedThemeId.value,
            ruleJson: operateRuleJson.value
          })
    ElMessage.success(data.message || '操作完成')
    operateDrawerVisible.value = false
    await getThemePage()
    await getItems()
  } finally {
    operateLoading.value = false
  }
}

const loadTemplates = async () => {
  templateVisible.value = true
  templateLoading.value = true
  try {
    templates.value = await CpsSelectionThemeApi.listTemplates()
  } finally {
    templateLoading.value = false
  }
}

const createThemeFromTemplate = async (template: CpsSelectionThemeTemplateVO) => {
  const id = await CpsSelectionThemeApi.createFromTemplate({ templateCode: template.templateCode })
  selectedThemeId.value = id
  templateVisible.value = false
  ElMessage.success('已创建主题草稿')
  await getThemePage()
}

const openImportDialog = () => {
  manualImportJson.value = defaultManualImportJson()
  importVisible.value = true
}

const submitManualImport = async () => {
  if (!selectedThemeId.value) return
  manualImportLoading.value = true
  try {
    const items = JSON.parse(manualImportJson.value) as CpsSelectionThemeImportItemVO[]
    await CpsSelectionThemeApi.importItems({
      themeId: selectedThemeId.value,
      sourceType: 'MANUAL',
      items
    })
    ElMessage.success('导入成功')
    importVisible.value = false
    await getItems()
  } finally {
    manualImportLoading.value = false
  }
}

const handleItemSelectionChange = (rows: CpsSelectionThemeItemVO[]) => {
  selectedItemIds.value = rows.map((item) => item.id)
}

const batchUpdateItemStatus = async (status: SelectionThemeItemStatus) => {
  await CpsSelectionThemeApi.updateItemStatus({ ids: selectedItemIds.value, status })
  ElMessage.success('状态已更新')
  selectedItemIds.value = []
  await getItems()
}

const toggleItemStatus = async (row: CpsSelectionThemeItemVO) => {
  await CpsSelectionThemeApi.updateItemStatus({
    ids: [row.id],
    status: row.status === 'ENABLED' ? 'DISABLED' : 'ENABLED'
  })
  await getItems()
}

const toggleTop = async (row: CpsSelectionThemeItemVO) => {
  if (!selectedThemeId.value) return
  await CpsSelectionThemeApi.updateItemSort({
    themeId: selectedThemeId.value,
    items: [{ id: row.id, sort: row.sort || 0, topFlag: row.topFlag === 1 ? 0 : 1 }]
  })
  await getItems()
}

const deleteItem = async (row: CpsSelectionThemeItemVO) => {
  await ElMessageBox.confirm('确认删除该商品快照？', '删除商品', { type: 'warning' })
  await CpsSelectionThemeApi.deleteItem(row.id)
  ElMessage.success('删除成功')
  await getItems()
}

function buildDefaultThemeForm(): CpsSelectionThemeSaveVO {
  return {
    themeCode: '',
    themeName: '',
    themeType: 'CUSTOM',
    platformCodes: 'taobao',
    vendorCode: 'dataoke',
    status: 'DRAFT',
    sort: 0,
    ruleJson: defaultRuleJson()
  }
}

function defaultRuleJson() {
  return JSON.stringify(
    {
      keywords: ['防晒霜'],
      platforms: ['taobao'],
      vendorCode: 'dataoke',
      priceLowerLimit: 0,
      priceUpperLimit: 300,
      minCommissionRate: 10,
      minCommissionAmount: 1,
      minMonthSales: 100,
      couponAmountMin: 5,
      onlyCoupon: true,
      categoryId: '',
      channelCode: 'hot',
      activityTags: ['618'],
      sortType: 0,
      pullCount: 30
    },
    null,
    2
  )
}

function defaultManualImportJson() {
  return JSON.stringify(
    [
      {
        platformCode: 'taobao',
        vendorCode: 'dataoke',
        goodsId: 'example-goods-id',
        title: '示例商品',
        actualPrice: 49.9,
        couponPrice: 10,
        commissionRate: 20,
        commissionAmount: 9.98,
        monthSales: 5000,
        recommendReason: '人工精选，高券高佣'
      }
    ],
    null,
    2
  )
}

const themeStatusMeta = (status?: SelectionThemeStatus | '') =>
  SELECTION_THEME_STATUS_OPTIONS.find((item) => item.value === status) || {
    label: status || '-',
    value: status || '',
    type: 'info'
  }

const itemStatusMeta = (status?: SelectionThemeItemStatus) =>
  SELECTION_THEME_ITEM_STATUS_OPTIONS.find((item) => item.value === status) || {
    label: status || '-',
    value: status || '',
    type: 'info'
  }

const sourceLabel = (source?: SelectionThemeSourceType) =>
  SELECTION_SOURCE_OPTIONS.find((item) => item.value === source)?.label || source || '-'

const platformLabel = (platformCode?: string) => {
  const map: Record<string, string> = {
    taobao: '淘宝',
    jd: '京东',
    pdd: '拼多多',
    douyin: '抖音'
  }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const formatMoney = (value?: number) => (value == null ? '-' : `¥${Number(value).toFixed(2)}`)
const formatPercent = (value?: number) => (value == null ? '-' : `${Number(value).toFixed(2)}%`)
const normalizeScore = (value?: number) => Math.max(0, Math.min(100, Number(value || 0)))

onMounted(() => {
  getThemePage()
})
</script>

<style scoped>
.selection-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-toolbar,
.selected-head,
.pane-head,
.toolbar-actions,
.action-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.toolbar-actions,
.action-panel {
  flex-wrap: wrap;
  justify-content: flex-start;
}

.selection-layout {
  display: grid;
  grid-template-columns: minmax(280px, 340px) minmax(0, 1fr);
  gap: 16px;
}

.theme-pane,
.content-pane {
  min-width: 0;
}

.theme-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.theme-item {
  width: 100%;
  padding: 12px;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  background: var(--el-bg-color);
  text-align: left;
  cursor: pointer;
}

.theme-item.active {
  border-color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.theme-line,
.theme-tags {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.theme-line span,
.goods-title,
.selected-title {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.theme-line span,
.selected-title {
  font-weight: 600;
}

.theme-meta,
.selected-desc,
.goods-meta,
.recommend-reason {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.selected-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.action-panel {
  margin: 16px 0;
  padding: 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}

.goods-cell {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.goods-cell .el-image,
.goods-placeholder,
.card-image {
  width: 64px;
  height: 64px;
  border-radius: 6px;
  overflow: hidden;
  flex: none;
}

.goods-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color);
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.goods-tags,
.template-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
}

.price-main {
  color: var(--el-color-danger);
  font-weight: 700;
}

.commission-main {
  color: var(--el-color-success);
  font-weight: 700;
}

.score-line {
  display: grid;
  grid-template-columns: minmax(80px, 1fr) 42px;
  gap: 8px;
  align-items: center;
}

.goods-grid,
.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.goods-card,
.template-card {
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.card-image {
  width: 100%;
  aspect-ratio: 1.2;
  height: auto;
}

.card-image .el-image {
  width: 100%;
  height: 100%;
}

.card-body,
.template-card {
  padding: 12px;
}

.card-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 6px;
  margin-top: 10px;
  font-size: 12px;
}

.card-body p,
.template-card p {
  min-height: 40px;
  margin: 10px 0 0;
  color: var(--el-text-color-secondary);
  font-size: 12px;
  line-height: 1.6;
}

.template-title {
  font-weight: 700;
}

.template-card .el-button {
  margin-top: 12px;
}

@media (max-width: 1100px) {
  .selection-layout {
    grid-template-columns: 1fr;
  }
}
</style>
