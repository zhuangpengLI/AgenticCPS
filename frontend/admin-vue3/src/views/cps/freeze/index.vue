<template>
  <el-tabs v-model="activeTab" type="border-card">
    <!-- 冻结配置 -->
    <el-tab-pane label="冻结配置" name="config">
      <ContentWrap>
        <el-form
          class="-mb-15px"
          :model="configQuery"
          ref="configQueryFormRef"
          :inline="true"
          label-width="80px"
        >
          <el-form-item label="平台" prop="platformCode">
            <el-select
              v-model="configQuery.platformCode"
              placeholder="请选择平台"
              filterable
              clearable
              class="!w-180px"
            >
              <el-option
                v-for="platform in platformOptions"
                :key="platform.platformCode"
                :label="`${platform.platformName}（${platform.platformCode}）`"
                :value="platform.platformCode"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="configQuery.status" placeholder="请选择状态" clearable class="!w-120px">
              <el-option label="启用" :value="1" />
              <el-option label="禁用" :value="0" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button @click="handleConfigQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
            <el-button @click="resetConfigQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
            <el-button type="primary" @click="openConfigForm(undefined)" v-hasPermi="['cps:freeze-config:update']">
              <Icon icon="ep:plus" class="mr-5px" /> 新增
            </el-button>
          </el-form-item>
        </el-form>
      </ContentWrap>

      <ContentWrap>
        <el-table v-loading="configLoading" :data="configList">
          <el-table-column label="ID" align="center" prop="id" width="80" />
          <el-table-column label="平台编码" align="center" prop="platformCode" width="140">
            <template #default="scope">
              <span>{{ scope.row.platformCode || '全平台（默认）' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="解冻天数" align="center" prop="unfreezeDays" width="100">
            <template #default="scope">{{ scope.row.unfreezeDays }} 天</template>
          </el-table-column>
          <el-table-column label="金额区间（元）" align="center" min-width="180">
            <template #default="scope">
              {{ (scope.row.minAmountCent / 100).toFixed(2) }} ～
              {{ scope.row.maxAmountCent == null ? '无上限' : (scope.row.maxAmountCent / 100).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template #default="scope">
              <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="备注" align="center" prop="remark" min-width="160" show-overflow-tooltip />
          <el-table-column label="创建时间" align="center" prop="createTime" width="160">
            <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="160">
            <template #default="scope">
              <el-button link type="primary" @click="openConfigForm(scope.row)" v-hasPermi="['cps:freeze-config:update']">编辑</el-button>
              <el-button link type="danger" @click="handleConfigDelete(scope.row.id)" v-hasPermi="['cps:freeze-config:update']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="configTotal"
          v-model:page="configQuery.pageNo"
          v-model:limit="configQuery.pageSize"
          @pagination="getConfigList"
        />
      </ContentWrap>
    </el-tab-pane>

    <!-- 冻结记录 -->
    <el-tab-pane label="冻结记录" name="record">
      <ContentWrap>
        <el-form
          class="-mb-15px"
          :model="recordQuery"
          ref="recordQueryFormRef"
          :inline="true"
          label-width="80px"
        >
          <el-form-item label="会员ID" prop="memberId">
            <el-select
              v-model="recordQuery.memberId"
              filterable
              remote
              reserve-keyword
              clearable
              placeholder="请选择会员"
              class="!w-220px"
              :remote-method="searchMemberOptions"
              :loading="memberLoading"
              @focus="loadMemberOptions"
            >
              <el-option
                v-for="member in memberOptions"
                :key="member.id"
                :label="formatMemberLabel(member)"
                :value="member.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="recordQuery.status" placeholder="请选择状态" clearable class="!w-140px">
              <el-option label="待冻结" value="pending" />
              <el-option label="已冻结" value="frozen" />
              <el-option label="解冻中" value="unfreezing" />
              <el-option label="已解冻" value="unfreezed" />
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间" prop="createTime">
            <el-date-picker
              v-model="recordQuery.createTime"
              value-format="YYYY-MM-DD HH:mm:ss"
              type="daterange"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :default-time="[new Date('1 00:00:00'), new Date('1 23:59:59')]"
              class="!w-240px"
            />
          </el-form-item>
          <el-form-item>
            <el-button @click="handleRecordQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
            <el-button @click="resetRecordQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
          </el-form-item>
        </el-form>
      </ContentWrap>

      <ContentWrap>
        <el-table v-loading="recordLoading" :data="recordList">
          <el-table-column label="ID" align="center" prop="id" width="80" />
          <el-table-column label="会员ID" align="center" prop="memberId" width="100" />
          <el-table-column label="订单ID" align="center" prop="orderId" width="100" />
          <el-table-column label="平台订单号" align="center" prop="platformOrderId" width="160" show-overflow-tooltip />
          <el-table-column label="冻结金额" align="center" prop="freezeAmount" width="120">
            <template #default="scope">
              <span class="text-orange-500 font-bold">￥{{ scope.row.freezeAmount }}</span>
            </template>
          </el-table-column>
          <el-table-column label="计划解冻时间" align="center" prop="unfreezeTime" width="160">
            <template #default="scope">{{ scope.row.unfreezeTime ? formatDate(scope.row.unfreezeTime) : '-' }}</template>
          </el-table-column>
          <el-table-column label="实际解冻时间" align="center" prop="actualUnfreezeTime" width="160">
            <template #default="scope">{{ scope.row.actualUnfreezeTime ? formatDate(scope.row.actualUnfreezeTime) : '-' }}</template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="100">
            <template #default="scope">
              <el-tag :type="getStatusTagType(scope.row.status)">{{ getStatusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="160">
            <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" align="center" width="120">
            <template #default="scope">
              <el-button
                v-if="scope.row.status === 'frozen'"
                link
                type="primary"
                @click="handleManualUnfreeze(scope.row.id)"
                v-hasPermi="['cps:freeze-record:unfreeze']"
              >
                手动解冻
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <Pagination
          :total="recordTotal"
          v-model:page="recordQuery.pageNo"
          v-model:limit="recordQuery.pageSize"
          @pagination="getRecordList"
        />
      </ContentWrap>
    </el-tab-pane>
  </el-tabs>

  <!-- 冻结配置 新增/编辑弹窗 -->
  <el-dialog
    v-model="configDialogVisible"
    :title="configFormData.id ? '编辑冻结配置' : '新增冻结配置'"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form ref="configFormRef" :model="configFormData" :rules="configFormRules" label-width="100px">
      <el-form-item label="平台编码" prop="platformCode">
        <el-select
          v-model="configFormData.platformCode"
          placeholder="留空表示全平台默认配置"
          filterable
          clearable
          class="!w-260px"
        >
          <el-option label="全平台（默认）" value="" />
          <el-option
            v-for="platform in platformOptions"
            :key="platform.platformCode"
            :label="`${platform.platformName}（${platform.platformCode}）`"
            :value="platform.platformCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="解冻天数" prop="unfreezeDays">
        <el-input-number v-model="configFormData.unfreezeDays" :min="1" :max="365" />
        <span class="ml-10px text-gray-500">天（收货与平台结算时间取晚后计算）</span>
      </el-form-item>
      <el-form-item label="金额下限" prop="minAmountYuan">
        <el-input-number v-model="configFormData.minAmountYuan" :min="0" :step="0.01" :precision="2" />
        <span class="ml-10px text-gray-500">元</span>
      </el-form-item>
      <el-form-item label="金额上限" prop="maxAmountYuan">
        <el-input-number v-model="configFormData.maxAmountYuan" :min="(configFormData.minAmountYuan ?? 0) + 0.01" :step="0.01" :precision="2" />
        <span class="ml-10px text-gray-500">元</span>
        <span class="ml-10px text-gray-500">留空表示无上限，区间左闭右开</span>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="configFormData.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="configFormData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="configDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="configFormLoading" @click="handleConfigSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  getCpsFreezeConfigPage,
  createCpsFreezeConfig,
  updateCpsFreezeConfig,
  deleteCpsFreezeConfig,
  getCpsFreezeRecordPage,
  manualUnfreeze,
  type CpsFreezeConfigPageReqVO,
  type CpsFreezeConfigSaveVO,
  type CpsFreezeRecordPageReqVO
} from '@/api/cps/freeze'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import { getUser, getUserPage, type UserVO } from '@/api/member/user'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'CpsFreeze' })

const activeTab = ref('config')

// ===== 冻结配置 =====
const configLoading = ref(false)
const configList = ref([])
const configTotal = ref(0)
const configQueryFormRef = ref()
const configFormRef = ref()
const configDialogVisible = ref(false)
const configFormLoading = ref(false)
const platformOptions = ref<CpsPlatformVO[]>([])

const configQuery = reactive<CpsFreezeConfigPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  status: undefined
})

interface FreezeConfigFormData {
  id?: number
  platformCode?: string
  minAmountYuan: number
  maxAmountYuan?: number
  unfreezeDays: number
  status: number
  remark?: string
}

const configFormData = reactive<FreezeConfigFormData>({
  id: undefined,
  platformCode: undefined,
  minAmountYuan: 10,
  maxAmountYuan: undefined,
  unfreezeDays: 7,
  status: 1,
  remark: undefined
})

const configFormRules = {
  unfreezeDays: [{ required: true, message: '解冻天数不能为空', trigger: 'blur' }],
  minAmountYuan: [{ required: true, message: '金额下限不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

const getConfigList = async () => {
  configLoading.value = true
  try {
    const data = await getCpsFreezeConfigPage(configQuery)
    configList.value = data.list
    configTotal.value = data.total
  } finally {
    configLoading.value = false
  }
}

const handleConfigQuery = () => {
  configQuery.pageNo = 1
  getConfigList()
}

const resetConfigQuery = () => {
  configQueryFormRef.value?.resetFields()
  handleConfigQuery()
}

const openConfigForm = (row: any) => {
  if (row) {
    Object.assign(configFormData, {
      ...row,
      platformCode: row.platformCode || '',
      minAmountYuan: row.minAmountCent == null ? 0 : row.minAmountCent / 100,
      maxAmountYuan: row.maxAmountCent == null ? undefined : row.maxAmountCent / 100
    })
  } else {
    Object.assign(configFormData, { id: undefined, platformCode: '', minAmountYuan: 10, maxAmountYuan: undefined, unfreezeDays: 7, status: 1, remark: undefined })
  }
  configDialogVisible.value = true
}

const handleConfigSubmit = async () => {
  await configFormRef.value?.validate()
  const payload: CpsFreezeConfigSaveVO = {
    id: configFormData.id,
    platformCode: configFormData.platformCode || undefined,
    minAmountCent: Math.round((configFormData.minAmountYuan ?? 0) * 100),
    maxAmountCent: configFormData.maxAmountYuan == null ? undefined : Math.round(configFormData.maxAmountYuan * 100),
    unfreezeDays: configFormData.unfreezeDays,
    status: configFormData.status,
    remark: configFormData.remark
  }
  configFormLoading.value = true
  try {
    if (configFormData.id) {
      await updateCpsFreezeConfig(payload)
      ElMessage.success('更新成功')
    } else {
      await createCpsFreezeConfig(payload)
      ElMessage.success('创建成功')
    }
    configDialogVisible.value = false
    getConfigList()
  } finally {
    configFormLoading.value = false
  }
}

const handleConfigDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该冻结配置吗？', '提示', { type: 'warning' })
  await deleteCpsFreezeConfig(id)
  ElMessage.success('删除成功')
  getConfigList()
}

// ===== 冻结记录 =====
const recordLoading = ref(false)
const recordList = ref([])
const recordTotal = ref(0)
const recordQueryFormRef = ref()
const memberOptions = ref<UserVO[]>([])
const memberLoading = ref(false)

const recordQuery = reactive<CpsFreezeRecordPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  memberId: undefined,
  status: undefined,
  createTime: undefined
})

const formatMemberLabel = (member: UserVO) => {
  const profile = member.nickname || member.name || member.mobile
  return profile ? `${member.id}（${profile}）` : String(member.id)
}

const searchMemberOptions = async (keyword = '') => {
  const query = keyword.trim()
  memberLoading.value = true
  try {
    let options: UserVO[] = []
    if (/^\d+$/.test(query)) {
      const user = await getUser(Number(query))
      options = user ? [user] : []
    } else {
      const data = await getUserPage({ pageNo: 1, pageSize: 20, nickname: query || undefined })
      options = data?.list || []
    }
    const selected = memberOptions.value.find((member) => member.id === recordQuery.memberId)
    if (selected && !options.some((member) => member.id === selected.id)) options.unshift(selected)
    memberOptions.value = options
  } catch {
    memberOptions.value = []
  } finally {
    memberLoading.value = false
  }
}

const loadMemberOptions = () => {
  if (!memberOptions.value.length) searchMemberOptions()
}

const loadPlatformOptions = async () => {
  try {
    platformOptions.value = (await CpsPlatformApi.getEnabledPlatformList()) || []
  } catch {
    platformOptions.value = []
  }
}

const statusMap: Record<string, { label: string; type: string }> = {
  pending: { label: '待冻结', type: 'info' },
  frozen: { label: '已冻结', type: 'warning' },
  unfreezing: { label: '解冻中', type: '' },
  unfreezed: { label: '已解冻', type: 'success' }
}

const getStatusLabel = (status: string) => statusMap[status]?.label ?? status
const getStatusTagType = (status: string) => statusMap[status]?.type ?? ''

const getRecordList = async () => {
  recordLoading.value = true
  try {
    const data = await getCpsFreezeRecordPage(recordQuery)
    recordList.value = data.list
    recordTotal.value = data.total
  } finally {
    recordLoading.value = false
  }
}

const handleRecordQuery = () => {
  recordQuery.pageNo = 1
  getRecordList()
}

const resetRecordQuery = () => {
  recordQueryFormRef.value?.resetFields()
  handleRecordQuery()
}

const handleManualUnfreeze = async (id: number) => {
  const { value: reason } = await ElMessageBox.prompt('手动解冻只能跳过剩余冻结时间，请填写审计原因。', '手动解冻', {
    type: 'warning',
    confirmButtonText: '确定解冻',
    inputPattern: /\S+/,
    inputErrorMessage: '解冻原因不能为空'
  })
  await manualUnfreeze({ recordId: id, reason, idempotencyKey: crypto.randomUUID() })
  ElMessage.success('解冻成功')
  getRecordList()
}

onMounted(() => {
  getConfigList()
  loadPlatformOptions()
})
</script>
