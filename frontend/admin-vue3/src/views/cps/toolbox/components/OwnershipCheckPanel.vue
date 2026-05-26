<template>
  <div class="tool-panel">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="88px">
      <el-row :gutter="12">
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="平台" prop="platformCode">
            <el-select v-model="formData.platformCode" filterable class="w-full">
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
          <el-form-item label="会员">
            <el-select
              v-model="formData.memberId"
              clearable
              filterable
              remote
              reserve-keyword
              class="w-full"
              placeholder="可选，手机号或昵称"
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
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="推广位">
            <el-input v-model="formData.adzoneId" clearable placeholder="可选，期望推广位" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12" :lg="6">
          <el-form-item label="记录ID">
            <el-input-number v-model="formData.transferRecordId" :min="1" class="w-full" controls-position="right" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="检测内容" prop="originalContent">
            <el-input
              v-model="formData.originalContent"
              type="textarea"
              :rows="4"
              placeholder="粘贴推广链接、淘口令、商品链接，或输入转链记录ID后辅助核验"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="panel-actions">
      <el-button type="primary" :loading="loading" @click="handleCheck">
        <Icon icon="ep:aim" class="mr-5px" /> 开始检测
      </el-button>
      <el-button @click="handleReset">
        <Icon icon="ep:refresh" class="mr-5px" /> 重置
      </el-button>
    </div>

    <el-descriptions v-if="result" class="mt-16px" :column="1" border>
      <el-descriptions-item label="检测结论">
        <el-tag :type="statusType(result.checkStatus)" effect="plain">
          {{ statusText(result.checkStatus) }}
        </el-tag>
        <span class="ml-8px">{{ result.message }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="转链记录">{{ result.transferRecordId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="商品">{{ result.itemTitle || result.itemId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="记录会员">{{ result.recordMemberId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="记录推广位">{{ result.recordAdzoneId || '-' }}</el-descriptions-item>
      <el-descriptions-item label="推广链接">
        <a v-if="result.promotionUrl" :href="result.promotionUrl" target="_blank" class="text-blue-500">打开</a>
        <span v-else>-</span>
      </el-descriptions-item>
      <el-descriptions-item label="淘口令">{{ result.taoCommand || '-' }}</el-descriptions-item>
      <el-descriptions-item label="不一致项">
        <el-tag v-for="item in result.mismatches || []" :key="item" class="mr-6px" type="warning" effect="plain">
          {{ mismatchText(item) }}
        </el-tag>
        <span v-if="!result.mismatches?.length">-</span>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup lang="ts">
import type { FormInstance, FormRules } from 'element-plus'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'
import { CpsRebateToolboxApi, type CpsGoodsOwnershipCheckRespVO } from '@/api/cps/rebateToolbox'
import { getUserPage, type UserVO } from '@/api/member/user/index'

const message = useMessage()
const formRef = ref<FormInstance>()
const loading = ref(false)
const memberLoading = ref(false)
const result = ref<CpsGoodsOwnershipCheckRespVO>()
const platformOptions = ref<CpsPlatformVO[]>([])
const memberOptions = ref<UserVO[]>([])

const formData = reactive({
  platformCode: 'taobao',
  originalContent: '',
  memberId: undefined as number | undefined,
  adzoneId: '',
  transferRecordId: undefined as number | undefined
})

const formRules = reactive<FormRules>({
  platformCode: [{ required: true, message: '请选择平台', trigger: 'change' }],
  originalContent: [{ required: true, message: '请输入检测内容', trigger: 'blur' }]
})

const fallbackPlatformOptions: CpsPlatformVO[] = [
  { id: 0, platformCode: 'taobao', platformName: '淘宝', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'jd', platformName: '京东', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'pdd', platformName: '拼多多', status: 1, createTime: new Date() },
  { id: 0, platformCode: 'douyin', platformName: '抖音', status: 1, createTime: new Date() }
]

const handleCheck = async () => {
  await formRef.value?.validate()
  loading.value = true
  try {
    result.value = await CpsRebateToolboxApi.checkOwnership({
      platformCode: formData.platformCode,
      originalContent: formData.originalContent,
      memberId: formData.memberId,
      adzoneId: formData.adzoneId || undefined,
      transferRecordId: formData.transferRecordId
    })
    if (result.value.checkStatus === 'MATCH') message.success('归属匹配')
    else if (result.value.checkStatus === 'MISMATCH') message.warning('归属存在不一致项')
    else message.warning(result.value.message || '未找到匹配记录')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  formData.originalContent = ''
  formData.memberId = undefined
  formData.adzoneId = ''
  formData.transferRecordId = undefined
  result.value = undefined
}

const searchMemberOptions = async (keyword?: string) => {
  memberLoading.value = true
  try {
    const data = await getUserPage({ pageNo: 1, pageSize: 20, nickname: keyword || undefined })
    memberOptions.value = data.list || []
  } finally {
    memberLoading.value = false
  }
}

const handleMemberDropdownVisible = (visible: boolean) => {
  if (visible && memberOptions.value.length === 0) searchMemberOptions()
}

const loadPlatformOptions = async () => {
  try {
    const data = await CpsPlatformApi.getEnabledPlatformList()
    platformOptions.value = data?.length ? data : fallbackPlatformOptions
  } catch {
    platformOptions.value = fallbackPlatformOptions
  }
}

const formatMemberLabel = (item: UserVO) =>
  `${item.nickname || item.mobile || item.id}（ID:${item.id}）`

const platformLabel = (platformCode?: string) => {
  const platform = platformOptions.value.find((item) => item.platformCode === platformCode)
  if (platform?.platformName) return platform.platformName
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return platformCode ? map[platformCode] || platformCode : '-'
}

const statusText = (status?: string) =>
  ({ MATCH: '匹配', MISMATCH: '不一致', NOT_FOUND: '未找到' })[status || ''] || status || '-'

const statusType = (status?: string) =>
  status === 'MATCH' ? 'success' : status === 'MISMATCH' ? 'warning' : 'info'

const mismatchText = (value: string) =>
  ({ memberId: '会员', adzoneId: '推广位', platformCode: '平台', transferRecord: '转链记录' })[value] || value

onMounted(loadPlatformOptions)
</script>

<style scoped>
.tool-panel {
  display: flex;
  flex-direction: column;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}
</style>
