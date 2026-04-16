<template>
  <el-dialog
    v-model="visible"
    title="选择推广位"
    width="900px"
    :close-on-click-modal="true"
    destroy-on-close
    @keydown.esc="visible = false"
  >
    <!-- 筛选表单 -->
    <el-form :model="filterParams" :inline="true" class="mb-15px" label-width="80px">
      <el-form-item label="平台编码">
        <el-select
          v-model="filterParams.platformCode"
          placeholder="全部平台"
          clearable
          class="!w-140px"
          @change="handleFilter"
        >
          <el-option
            v-for="item in PLATFORM_CODE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="推广位名称">
        <el-input
          v-model="filterParams.adzoneName"
          placeholder="请输入名称"
          clearable
          class="!w-160px"
          @keyup.enter="handleFilter"
          @clear="handleFilter"
        />
      </el-form-item>
      <el-form-item label="推广位类型">
        <el-select
          v-model="filterParams.adzoneType"
          placeholder="全部类型"
          clearable
          class="!w-140px"
          @change="handleFilter"
        >
          <el-option label="通用" value="general" />
          <el-option label="渠道" value="channel" />
          <el-option label="会员专属" value="member" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select
          v-model="filterParams.status"
          placeholder="全部状态"
          clearable
          class="!w-110px"
          @change="handleFilter"
        >
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleFilter">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetFilter">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 推广位列表 -->
    <el-table
      v-loading="loading"
      :data="list"
      stripe
      highlight-current-row
      @current-change="handleCurrentChange"
      @row-dblclick="handleRowDblclick"
      style="width: 100%"
    >
      <el-table-column width="50" align="center">
        <template #default="scope">
          <el-radio
            v-model="selectedId"
            :label="scope.row.id"
            @click.stop
          >
            <span></span>
          </el-radio>
        </template>
      </el-table-column>
      <el-table-column label="推广位ID" prop="adzoneId" min-width="160" show-overflow-tooltip />
      <el-table-column label="推广位名称" prop="adzoneName" min-width="130" show-overflow-tooltip />
      <el-table-column label="平台编码" align="center" width="100">
        <template #default="scope">
          <el-tag type="info" size="small">{{ platformLabel(scope.row.platformCode) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="推广位类型" align="center" width="110">
        <template #default="scope">
          <el-tag :type="adzoneTypeTagType(scope.row.adzoneType)" size="small">
            {{ adzoneTypeLabel(scope.row.adzoneType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="160">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty v-if="!loading && list.length === 0" description="暂无推广位数据，请先创建推广位" />

    <!-- 分页 -->
    <div v-if="total > 0" class="mt-10px flex justify-end">
      <el-pagination
        v-model:current-page="filterParams.pageNo"
        v-model:page-size="filterParams.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @size-change="handleFilter"
        @current-change="loadList"
      />
    </div>

    <template #footer>
      <div class="flex items-center justify-between">
        <span v-if="selectedRow" class="text-sm text-gray-500">
          已选：<span class="text-blue-500 font-medium">{{ selectedRow.adzoneId }}</span>
          <span v-if="selectedRow.adzoneName" class="ml-1 text-gray-400">（{{ selectedRow.adzoneName }}）</span>
        </span>
        <span v-else class="text-sm text-gray-400">请从列表中选择一个推广位，或双击行直接确认</span>
        <div>
          <el-button @click="visible = false">取 消</el-button>
          <el-button type="primary" :disabled="!selectedRow" @click="handleConfirm">
            确 定
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { CpsAdzoneApi, type CpsAdzoneVO } from '@/api/cps/adzone'
import { PLATFORM_CODE_OPTIONS } from '@/api/cps/apiVendor'
import { formatDate } from '@/utils/formatTime'

defineOptions({ name: 'AdzoneSelectDialog' })

// ===== Props / Emits =====
interface Props {
  /** 预设平台过滤（打开时默认筛选当前平台） */
  defaultPlatformCode?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  /** 用户确认选择，返回 adzoneId 字符串 */
  (e: 'select', adzoneId: string): void
}>()

// ===== 弹窗可见性（供父组件通过 ref 调用 open() 控制）=====
const visible = ref(false)

const open = (platformCode?: string) => {
  // 重置状态
  selectedId.value = undefined
  selectedRow.value = null
  Object.assign(filterParams, defaultFilter())
  if (platformCode) {
    filterParams.platformCode = platformCode
  } else if (props.defaultPlatformCode) {
    filterParams.platformCode = props.defaultPlatformCode
  }
  visible.value = true
  loadList()
}

defineExpose({ open })

// ===== 列表数据 =====
const loading = ref(false)
const list = ref<CpsAdzoneVO[]>([])
const total = ref(0)

const defaultFilter = () => ({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined as string | undefined,
  adzoneName: undefined as string | undefined,
  adzoneType: undefined as string | undefined,
  status: undefined as number | undefined
})

const filterParams = reactive(defaultFilter())

const loadList = async () => {
  loading.value = true
  try {
    const data = await CpsAdzoneApi.getAdzonePage({
      pageNo: filterParams.pageNo,
      pageSize: filterParams.pageSize,
      platformCode: filterParams.platformCode,
      adzoneName: filterParams.adzoneName,
      status: filterParams.status
    })
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  filterParams.pageNo = 1
  loadList()
}

const resetFilter = () => {
  Object.assign(filterParams, defaultFilter())
  loadList()
}

// ===== 选择逻辑 =====
const selectedId = ref<number | undefined>(undefined)
const selectedRow = ref<CpsAdzoneVO | null>(null)

const handleCurrentChange = (row: CpsAdzoneVO | null) => {
  if (row) {
    selectedId.value = row.id
    selectedRow.value = row
  }
}

const handleRowDblclick = (row: CpsAdzoneVO) => {
  selectedId.value = row.id
  selectedRow.value = row
  handleConfirm()
}

const handleConfirm = () => {
  if (!selectedRow.value) return
  emit('select', selectedRow.value.adzoneId)
  visible.value = false
}

// ===== 辅助方法 =====
const platformLabel = (code?: string) => {
  return PLATFORM_CODE_OPTIONS.find((item) => item.value === code)?.label ?? code ?? '-'
}

const adzoneTypeLabel = (type?: string) => {
  const map: Record<string, string> = {
    general: '通用',
    channel: '渠道',
    member: '会员专属'
  }
  return map[type ?? ''] ?? type ?? '-'
}

const adzoneTypeTagType = (type?: string): '' | 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, '' | 'success' | 'warning' | 'info' | 'danger'> = {
    general: '',
    channel: 'warning',
    member: 'success'
  }
  return map[type ?? ''] ?? 'info'
}
</script>
