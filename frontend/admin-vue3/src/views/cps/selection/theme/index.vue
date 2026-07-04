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
          <el-button :loading="dataokeSyncLoading" @click="openDataokeSyncDialog">
            <Icon icon="ep:connection" class="mr-5px" /> 大淘客同步
          </el-button>
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

      <div class="stat-strip">
        <div class="stat-card">
          <span>主题总数</span>
          <b>{{ themeTotal }}</b>
          <em>当前筛选</em>
        </div>
        <div class="stat-card success">
          <span>已发布</span>
          <b>{{ themeStats.published }}</b>
          <em>MCP 可见</em>
        </div>
        <div class="stat-card warning">
          <span>草稿</span>
          <b>{{ themeStats.draft }}</b>
          <em>待确认</em>
        </div>
        <div class="stat-card primary">
          <span>当前商品</span>
          <b>{{ itemStats.total }}</b>
          <em>{{ itemStats.enabled }} 启用 / {{ itemStats.disabled }} 停用</em>
        </div>
      </div>
    </ContentWrap>

    <div class="selection-layout">
      <ContentWrap class="theme-pane">
        <div class="pane-head">
          <span>主题列表</span>
          <el-tag effect="plain">{{ themeTotal }}</el-tag>
        </div>
        <div class="quick-filters">
          <el-check-tag
            v-for="item in quickStatusFilters"
            :key="item.value"
            :checked="queryParams.status === item.value"
            @change="setQuickStatus(item.value)"
          >
            {{ item.label }}
          </el-check-tag>
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
            <div v-if="item.description" class="theme-desc">{{ item.description }}</div>
            <div class="theme-tags">
              <el-tag v-if="item.promotionEvent" size="small" type="danger" effect="plain">
                {{ item.promotionEvent }}
              </el-tag>
              <el-tag v-if="item.themeType" size="small" effect="plain">
                {{ themeTypeLabel(item.themeType) }}
              </el-tag>
              <el-tag v-if="item.refreshStatus" size="small" effect="plain">
                {{ refreshStatusLabel(item.refreshStatus) }}
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
            <div v-if="selectedTheme" class="selected-meta">
              <el-tag v-for="item in selectedPlatforms" :key="item" size="small" effect="plain">
                {{ platformLabel(item) }}
              </el-tag>
              <el-tag
                v-for="item in selectedTags"
                :key="item"
                size="small"
                type="success"
                effect="plain"
              >
                {{ item }}
              </el-tag>
              <span v-if="selectedTheme.lastRefreshTime">
                最近刷新 {{ formatDateTime(selectedTheme.lastRefreshTime) }}
              </span>
            </div>
          </div>
          <div class="toolbar-actions">
            <el-segmented v-model="viewMode" :options="viewOptions" />
            <el-button :disabled="!selectedTheme" :loading="itemLoading" @click="getItems">
              <Icon icon="ep:refresh" />
            </el-button>
            <el-button :disabled="!selectedTheme" @click="openThemeForm('update')">
              <Icon icon="ep:edit" />
            </el-button>
            <el-button type="danger" plain :disabled="!selectedTheme" @click="handleDeleteTheme">
              <Icon icon="ep:delete" />
            </el-button>
            <el-button type="success" :disabled="!selectedTheme" @click="handlePublish">
              <Icon icon="ep:upload" class="mr-5px" /> 发布
            </el-button>
            <el-button :disabled="!selectedTheme" @click="handleOffline">
              <Icon icon="ep:turn-off" class="mr-5px" /> 下线
            </el-button>
          </div>
        </div>

        <div v-if="selectedTheme" class="theme-insight">
          <div>
            <span>主题编码</span>
            <b>{{ selectedTheme.themeCode }}</b>
          </div>
          <div>
            <span>来源供应商</span>
            <b>{{ selectedTheme.vendorCode || '默认路由' }}</b>
          </div>
          <div>
            <span>商品快照</span>
            <b>{{ itemStats.total }}</b>
          </div>
          <div>
            <span>推荐均分</span>
            <b>{{ averageScore }}</b>
          </div>
        </div>

        <el-alert
          v-if="selectedTheme && themeTips.length"
          class="theme-alert"
          type="warning"
          :closable="false"
          show-icon
        >
          <template #title>{{ themeTips.join('；') }}</template>
        </el-alert>

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
            批量启用
          </el-button>
          <el-button
            :disabled="selectedItemIds.length === 0"
            @click="batchUpdateItemStatus('DISABLED')"
          >
            批量停用
          </el-button>
          <span v-if="selectedItemIds.length" class="selection-count">
            已选 {{ selectedItemIds.length }} 个商品
          </span>
        </div>

        <el-empty v-if="!selectedTheme" description="从左侧选择一个主题">
          <el-button type="primary" @click="openThemeForm('create')">新建主题</el-button>
        </el-empty>
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
            <el-table-column label="来源/快照" width="150">
              <template #default="{ row }">
                <div>{{ sourceLabel(row.sourceType) }}</div>
                <div class="text-12px text-gray-500">{{ formatDateTime(row.snapshotTime) }}</div>
              </template>
            </el-table-column>
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
            <template #empty>
              <el-empty description="该主题还没有商品快照">
                <div class="empty-actions">
                  <el-button type="primary" @click="openAiDrawer">AI 推荐</el-button>
                  <el-button @click="openVendorDrawer">第三方拉取</el-button>
                  <el-button @click="openImportDialog">人工添加</el-button>
                </div>
              </el-empty>
            </template>
          </el-table>

          <div v-else v-loading="itemLoading" class="goods-grid">
            <article v-for="item in itemList" :key="item.id" class="goods-card">
              <div class="card-image">
                <el-image v-if="item.mainPic" :src="item.mainPic" fit="cover" lazy />
                <div v-else class="goods-placeholder">{{ platformLabel(item.platformCode) }}</div>
                <div class="card-badges">
                  <el-tag size="small" effect="dark">{{ platformLabel(item.platformCode) }}</el-tag>
                  <el-tag v-if="item.topFlag === 1" size="small" type="warning" effect="dark">
                    置顶
                  </el-tag>
                </div>
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
                <div class="card-footer">
                  <el-tag size="small" :type="itemStatusMeta(item.status).type" effect="plain">
                    {{ itemStatusMeta(item.status).label }}
                  </el-tag>
                  <div>
                    <el-button link type="primary" @click="toggleTop(item)">
                      {{ item.topFlag === 1 ? '取消置顶' : '置顶' }}
                    </el-button>
                    <el-button link type="primary" @click="toggleItemStatus(item)">
                      {{ item.status === 'ENABLED' ? '停用' : '启用' }}
                    </el-button>
                  </div>
                </div>
              </div>
            </article>
            <el-empty
              v-if="!itemLoading && itemList.length === 0"
              description="该主题还没有商品快照"
            />
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
          <el-col :xs="24" :sm="12">
            <el-form-item label="封面图">
              <el-input v-model="themeForm.coverPic" placeholder="https://..." />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="标签">
              <el-input v-model="themeForm.tags" placeholder="高佣,有券,社群" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="状态">
              <el-select v-model="themeForm.status" class="w-full">
                <el-option
                  v-for="item in SELECTION_THEME_STATUS_OPTIONS"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="排序">
              <el-input-number v-model="themeForm.sort" :min="0" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="上线时间">
              <el-date-picker
                v-model="themeForm.startTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="w-full"
              />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="下线时间">
              <el-date-picker
                v-model="themeForm.endTime"
                type="datetime"
                value-format="YYYY-MM-DD HH:mm:ss"
                class="w-full"
              />
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
          <div class="rule-summary">
            <el-tag v-for="item in parsedRuleKeywords" :key="item" size="small" effect="plain">
              {{ item }}
            </el-tag>
            <span>拉取 {{ parsedRulePullCount }} 个 · {{ parsedRulePlatforms }}</span>
          </div>
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

    <el-dialog v-model="dataokeSyncVisible" title="同步大淘客主题" width="520px">
      <el-form label-position="top">
        <el-form-item label="关键词">
          <el-input v-model="dataokeSyncForm.keyword" placeholder="可选，按大淘客主题关键词过滤" clearable />
        </el-form-item>
        <el-row :gutter="12">
          <el-col :xs="24" :sm="12">
            <el-form-item label="同步页数">
              <el-input-number v-model="dataokeSyncForm.maxPages" :min="1" :max="20" class="w-full" />
            </el-form-item>
          </el-col>
          <el-col :xs="24" :sm="12">
            <el-form-item label="每页主题数">
              <el-input-number v-model="dataokeSyncForm.pageSize" :min="1" :max="100" class="w-full" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="同步主题商品">
          <el-switch v-model="dataokeSyncForm.syncGoods" />
        </el-form-item>
        <el-form-item v-if="dataokeSyncForm.syncGoods" label="每主题商品数">
          <el-input-number
            v-model="dataokeSyncForm.goodsPullCount"
            :min="1"
            :max="100"
            class="w-full"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dataokeSyncVisible = false">取消</el-button>
        <el-button type="primary" :loading="dataokeSyncLoading" @click="submitDataokeSync">
          同步
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="人工添加商品快照" width="640px">
      <el-form label-position="top">
        <el-form-item label="商品 JSON 数组">
          <el-alert
            class="mb-12px"
            type="info"
            :closable="false"
            title="字段至少包含 platformCode、goodsId；价格、券、佣金、销量为运营快照，不参与资金结算。"
          />
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
  type CpsSelectionThemeSyncReqVO,
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
const quickStatusFilters = [
  { label: '全部', value: '' as SelectionThemeStatus | '' },
  { label: '草稿', value: 'DRAFT' as SelectionThemeStatus },
  { label: '已发布', value: 'PUBLISHED' as SelectionThemeStatus },
  { label: '已下线', value: 'OFFLINE' as SelectionThemeStatus }
]
const themeStats = computed(() => ({
  draft: themeList.value.filter((item) => item.status === 'DRAFT').length,
  published: themeList.value.filter((item) => item.status === 'PUBLISHED').length,
  offline: themeList.value.filter((item) => item.status === 'OFFLINE').length
}))
const itemStats = computed(() => ({
  total: itemList.value.length,
  enabled: itemList.value.filter((item) => item.status === 'ENABLED').length,
  disabled: itemList.value.filter((item) => item.status === 'DISABLED').length
}))
const averageScore = computed(() => {
  if (!itemList.value.length) return '0.00'
  const sum = itemList.value.reduce((total, item) => total + Number(item.recommendScore || 0), 0)
  return (sum / itemList.value.length).toFixed(2)
})
const selectedPlatforms = computed(() => splitTextList(selectedTheme.value?.platformCodes))
const selectedTags = computed(() => splitTextList(selectedTheme.value?.tags))
const themeTips = computed(() => {
  if (!selectedTheme.value) return []
  const tips: string[] = []
  if (selectedTheme.value.status === 'PUBLISHED' && itemStats.value.enabled === 0) {
    tips.push('已发布主题缺少启用商品，MCP 推荐会为空')
  }
  if (!selectedTheme.value.ruleJson) {
    tips.push('缺少规则 JSON，AI 推荐和第三方拉取将使用默认规则')
  }
  if (selectedTheme.value.status === 'DRAFT' && itemStats.value.total > 0) {
    tips.push('草稿已有商品，确认后可发布给 MCP 使用')
  }
  return tips
})
const parsedOperateRule = computed(() => parseJsonRecord(operateRuleJson.value))
const parsedRuleKeywords = computed(() => {
  const keywords = parsedOperateRule.value?.keywords
  return Array.isArray(keywords) ? keywords.map(String).filter(Boolean).slice(0, 5) : []
})
const parsedRulePullCount = computed(() => Number(parsedOperateRule.value?.pullCount || 30))
const parsedRulePlatforms = computed(() => {
  const platforms = parsedOperateRule.value?.platforms
  return Array.isArray(platforms) && platforms.length
    ? platforms.map((item) => platformLabel(String(item))).join('、')
    : '全平台'
})

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

const dataokeSyncVisible = ref(false)
const dataokeSyncLoading = ref(false)
const dataokeSyncForm = reactive<CpsSelectionThemeSyncReqVO>({
  keyword: '',
  maxPages: 1,
  pageSize: 20,
  syncGoods: true,
  goodsPullCount: 20
})

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

const setQuickStatus = (status: SelectionThemeStatus | '') => {
  queryParams.status = status
  handleQuery()
}

const selectTheme = async (theme: CpsSelectionThemeVO) => {
  selectedThemeId.value = theme.id
  selectedItemIds.value = []
  await getItems()
}

const openThemeForm = (type: 'create' | 'update') => {
  themeFormType.value = type
  Object.assign(
    themeForm,
    buildDefaultThemeForm(),
    type === 'create' ? {} : selectedTheme.value || {}
  )
  themeFormVisible.value = true
}

const submitThemeForm = async () => {
  await themeFormRef.value?.validate()
  if (!validateJsonObject(themeForm.ruleJson, '规则 JSON')) return
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

const handleDeleteTheme = async () => {
  if (!selectedThemeId.value) return
  await ElMessageBox.confirm('删除主题会移除主题配置，确认删除？', '删除主题', { type: 'warning' })
  await CpsSelectionThemeApi.deleteTheme(selectedThemeId.value)
  ElMessage.success('删除成功')
  selectedThemeId.value = undefined
  itemList.value = []
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
  if (!validateJsonObject(operateRuleJson.value, '本次规则 JSON')) return
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

const openDataokeSyncDialog = () => {
  dataokeSyncVisible.value = true
}

const submitDataokeSync = async () => {
  dataokeSyncLoading.value = true
  try {
    const data = await CpsSelectionThemeApi.syncDataokeThemes({
      ...dataokeSyncForm,
      keyword: dataokeSyncForm.keyword?.trim() || undefined
    })
    dataokeSyncVisible.value = false
    ElMessage.success(data.message || '大淘客主题同步完成')
    await getThemePage()
    if (selectedThemeId.value) {
      await getItems()
    }
  } finally {
    dataokeSyncLoading.value = false
  }
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
    if (!Array.isArray(items) || items.length === 0) {
      throw new Error('商品 JSON 必须是非空数组')
    }
    items.forEach((item, index) => {
      if (!item.platformCode || !item.goodsId) {
        throw new Error(`第 ${index + 1} 个商品缺少 platformCode 或 goodsId`)
      }
    })
    await CpsSelectionThemeApi.importItems({
      themeId: selectedThemeId.value,
      sourceType: 'MANUAL',
      items
    })
    ElMessage.success('导入成功')
    importVisible.value = false
    await getItems()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '商品 JSON 格式不正确')
  } finally {
    manualImportLoading.value = false
  }
}

const handleItemSelectionChange = (rows: CpsSelectionThemeItemVO[]) => {
  selectedItemIds.value = rows.map((item) => item.id)
}

const batchUpdateItemStatus = async (status: SelectionThemeItemStatus) => {
  if (selectedItemIds.value.length === 0) return
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

const themeTypeLabel = (type?: string) => {
  const map: Record<string, string> = {
    PROMOTION: '大促',
    CUSTOM: '自定义',
    CATEGORY: '类目'
  }
  return type ? map[type] || type : '-'
}

const refreshStatusLabel = (status?: string) => {
  const map: Record<string, string> = {
    PROCESSING: '刷新中',
    SUCCESS: '已刷新',
    PARTIAL_SUCCESS: '部分成功',
    FAILED: '刷新失败'
  }
  return status ? map[status] || status : '-'
}

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
const formatDateTime = (value?: string) => (value ? value.replace('T', ' ').slice(0, 16) : '-')

function splitTextList(value?: string) {
  return (value || '')
    .split(/[,，]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function parseJsonRecord(value?: string): Record<string, unknown> | undefined {
  if (!value) return undefined
  try {
    const data = JSON.parse(value)
    return data && typeof data === 'object' && !Array.isArray(data) ? data : undefined
  } catch {
    return undefined
  }
}

function validateJsonObject(value: string | undefined, label: string) {
  if (!value) return true
  try {
    const data = JSON.parse(value)
    if (!data || typeof data !== 'object' || Array.isArray(data)) {
      ElMessage.error(`${label} 必须是 JSON 对象`)
      return false
    }
    return true
  } catch {
    ElMessage.error(`${label} 格式不正确`)
    return false
  }
}

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

.stat-strip {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.stat-card {
  min-width: 0;
  padding: 12px 14px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-lighter);
}

.stat-card span,
.stat-card em,
.theme-insight span,
.selection-count {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  font-style: normal;
}

.stat-card b {
  display: block;
  margin: 4px 0;
  color: var(--el-text-color-primary);
  font-size: 22px;
  line-height: 1;
}

.stat-card.success {
  border-color: var(--el-color-success-light-7);
  background: var(--el-color-success-light-9);
}

.stat-card.warning {
  border-color: var(--el-color-warning-light-7);
  background: var(--el-color-warning-light-9);
}

.stat-card.primary {
  border-color: var(--el-color-primary-light-7);
  background: var(--el-color-primary-light-9);
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

.quick-filters,
.selected-meta,
.empty-actions,
.rule-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.quick-filters {
  margin: 12px 0;
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
.theme-desc,
.selected-desc,
.goods-meta,
.recommend-reason {
  margin-top: 6px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.theme-desc {
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.selected-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
}

.selected-meta {
  margin-top: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.theme-insight {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.theme-insight > div {
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  background: var(--el-fill-color-extra-light);
}

.theme-insight b {
  display: block;
  overflow: hidden;
  margin-top: 4px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.theme-alert {
  margin-top: 12px;
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

.empty-actions {
  justify-content: center;
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
  position: relative;
  border: 1px solid var(--el-border-color-light);
  border-radius: 8px;
  overflow: hidden;
  background: var(--el-bg-color);
}

.card-image {
  position: relative;
  width: 100%;
  aspect-ratio: 1.2;
  height: auto;
}

.card-image .el-image {
  width: 100%;
  height: 100%;
}

.card-badges {
  position: absolute;
  top: 8px;
  left: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
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

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
}

.rule-summary {
  width: 100%;
  margin-bottom: 8px;
  color: var(--el-text-color-secondary);
  font-size: 12px;
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

@media (max-width: 900px) {
  .stat-strip,
  .theme-insight {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .stat-strip,
  .theme-insight {
    grid-template-columns: 1fr;
  }

  .selected-head {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
