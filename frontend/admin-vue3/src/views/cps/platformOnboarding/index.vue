<template>
  <workspace
    v-if="route.query.mode"
    :platform-code="route.query.platformCode as string | undefined"
    :mode="route.query.mode as 'create' | 'edit'"
    @close="closeWorkspace"
    @published="handlePublished"
  />
  <template v-else>
    <ContentWrap>
      <el-form :model="queryParams" :inline="true" @submit.prevent>
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            clearable
            placeholder="平台名称或编码"
            class="!w-220px"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" clearable placeholder="全部状态" class="!w-150px">
            <el-option label="全部" value="ALL" />
            <el-option label="待完善" value="INCOMPLETE" />
            <el-option label="已就绪" value="READY" />
            <el-option label="已启用" value="ENABLED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">
            <Icon icon="ep:search" class="mr-5px" />搜索
          </el-button>
          <el-button @click="resetQuery">
            <Icon icon="ep:refresh" class="mr-5px" />重置
          </el-button>
          <el-button type="success" v-hasPermi="['cps:platform-onboarding:create']" @click="openCreate">
            <Icon icon="ep:plus" class="mr-5px" />接入新平台
          </el-button>
        </el-form-item>
      </el-form>
    </ContentWrap>

    <ContentWrap>
      <el-table v-loading="loading" :data="list" stripe>
        <el-table-column label="平台" min-width="180">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <span class="font-medium">{{ row.platformName || row.platformCode }}</span>
              <el-tag size="small" type="info">{{ row.platformCode }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="主供应商" prop="primaryVendorCode" min-width="140" show-overflow-tooltip />
        <el-table-column label="备用供应商" prop="backupVendorCount" width="110" align="center" />
        <el-table-column label="默认推广位" prop="runtimeDefaultAdzoneId" min-width="150" show-overflow-tooltip />
        <el-table-column label="默认返利" width="100" align="right">
          <template #default="{ row }">
            {{ row.defaultRebateRate == null ? '-' : `${row.defaultRebateRate}%` }}
          </template>
        </el-table-column>
        <el-table-column label="配置完整度" min-width="220">
          <template #default="{ row }">
            <CompletionBadge :percentage="row.completionPercent" :missing-items="row.missingItems" />
          </template>
        </el-table-column>
        <el-table-column label="连接状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="connectionTagType(row.connectionStatus)" size="small">
              {{ connectionLabel(row.connectionStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="运行状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.runtimeStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.runtimeStatus === 1 ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="更新时间" prop="updateTime" width="170" />
        <el-table-column label="操作" fixed="right" width="260" align="center">
          <template #default="{ row }">
            <el-button link type="primary" v-hasPermi="['cps:platform-onboarding:update']" @click="openEdit(row.platformCode)">
              配置
            </el-button>
            <el-button
              link
              type="primary"
              v-hasPermi="['cps:platform-onboarding:test']"
              @click="openEdit(row.platformCode)"
            >
              连接测试
            </el-button>
            <el-button
              v-if="row.runtimeStatus === 1"
              link
              type="warning"
              v-hasPermi="['cps:platform-onboarding:update']"
              @click="handleDisable(row)"
            >
              禁用
            </el-button>
            <el-button
              v-else
              link
              type="success"
              v-hasPermi="['cps:platform-onboarding:publish']"
              @click="handleEnable(row)"
            >
              启用
            </el-button>
            <el-dropdown
              v-if="row.draftStatus || row.runtimeStatus !== 1"
              v-hasPermi="['cps:platform-onboarding:delete']"
            >
              <el-button link type="danger">删除<Icon icon="ep:arrow-down" class="ml-1" /></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item v-if="row.draftStatus" @click="handleDeleteDraft(row)">
                    删除草稿
                  </el-dropdown-item>
                  <el-dropdown-item v-if="row.runtimeStatus !== 1" @click="handleDeleteBundle(row)">
                    删除运行配置
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>
      <Pagination
        v-model:page="queryParams.pageNo"
        v-model:limit="queryParams.pageSize"
        :total="total"
        @pagination="getList"
      />
    </ContentWrap>
  </template>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import {
  PlatformOnboardingApi,
  type OnboardingPageItem,
  type OnboardingPageReq
} from '@/api/cps/platformOnboarding'
import CompletionBadge from './components/CompletionBadge.vue'

defineOptions({ name: 'CpsPlatformOnboarding' })

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const list = ref<OnboardingPageItem[]>([])
const total = ref(0)
const queryParams = reactive<OnboardingPageReq>({
  pageNo: 1,
  pageSize: 10,
  keyword: undefined,
  status: undefined
})

const getList = async () => {
  loading.value = true
  try {
    const data = await PlatformOnboardingApi.getPage({
      ...queryParams,
      keyword: queryParams.keyword?.trim() || undefined,
      status: queryParams.status || undefined
    })
    list.value = data.list ?? []
    total.value = data.total ?? 0
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  void getList()
}

const resetQuery = () => {
  queryParams.keyword = undefined
  queryParams.status = undefined
  handleQuery()
}

const openCreate = () => router.replace({ query: { mode: 'create' } })
const openEdit = (platformCode: string) => router.replace({ query: { mode: 'edit', platformCode } })
const closeWorkspace = () => router.replace({ query: {} }).then(() => getList())
const handlePublished = () => closeWorkspace()

const confirm = (message: string) => ElMessageBox.confirm(message, '请确认', { type: 'warning' })

const handleDisable = async (row: OnboardingPageItem) => {
  await confirm(`确定禁用平台“${row.platformName || row.platformCode}”吗？`)
  await PlatformOnboardingApi.disable(row.platformCode)
  ElMessage.success('平台已禁用')
  await getList()
}

const handleEnable = async (row: OnboardingPageItem) => {
  await PlatformOnboardingApi.enable(row.platformCode)
  ElMessage.success('平台已启用')
  await getList()
}

const handleDeleteDraft = async (row: OnboardingPageItem) => {
  await confirm(`确定删除平台“${row.platformName || row.platformCode}”的接入草稿吗？`)
  await PlatformOnboardingApi.deleteDraft(row.platformCode)
  ElMessage.success('草稿已删除')
  await getList()
}

const handleDeleteBundle = async (row: OnboardingPageItem) => {
  if (row.runtimeStatus === 1) return
  await confirm(`确定删除平台“${row.platformName || row.platformCode}”的运行配置吗？`)
  await PlatformOnboardingApi.deleteBundle(row.platformCode)
  ElMessage.success('运行配置已删除')
  await getList()
}

const connectionLabel = (status?: string) => {
  if (!status) return '未测试'
  const normalized = status.toUpperCase()
  if (normalized === 'SUCCESS' || normalized === 'CONNECTED' || normalized === 'PASS') return '已连接'
  if (normalized === 'FAILED' || normalized === 'FAIL') return '失败'
  return status
}

const connectionTagType = (status?: string): 'success' | 'danger' | 'info' | 'warning' => {
  if (!status) return 'info'
  const normalized = status.toUpperCase()
  if (normalized === 'SUCCESS' || normalized === 'CONNECTED' || normalized === 'PASS') return 'success'
  if (normalized === 'FAILED' || normalized === 'FAIL') return 'danger'
  return 'warning'
}

onMounted(getList)
</script>
