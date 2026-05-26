<template>
  <ContentWrap>
    <div class="page-toolbar">
      <div>
        <div class="text-18px font-600">CPX 任务中心</div>
        <div class="mt-4px text-12px text-gray-500">
          统一管理 CPS / CPA / CPL / CPM / CPC / oCPA / oCPC 任务，CPS 默认优先展示
        </div>
      </div>
      <el-button type="primary" @click="openForm()">
        <Icon icon="ep:plus" class="mr-5px" /> 新建任务
      </el-button>
    </div>

    <el-form :model="queryParams" class="mt-18px" label-width="84px" inline>
      <el-form-item label="关键词">
        <el-input
          v-model="queryParams.keyword"
          placeholder="任务名/标题/标签"
          clearable
          class="!w-220px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="平台">
        <el-input
          v-model="queryParams.platformCode"
          placeholder="taobao / jd / douyin"
          clearable
          class="!w-180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="计费方式">
        <el-select v-model="queryParams.promotionMethod" clearable class="!w-180px">
          <el-option
            v-for="item in CPX_PROMOTION_METHOD_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" clearable class="!w-140px">
          <el-option label="草稿" :value="0" />
          <el-option label="启用" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 查询
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="任务" min-width="220">
        <template #default="scope">
          <div class="font-600">{{ scope.row.taskName }}</div>
          <div class="text-xs text-gray-500">{{ scope.row.title || scope.row.shortDesc || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="平台" prop="platformCode" width="110">
        <template #default="scope">
          <el-tag type="info" effect="plain">{{ scope.row.platformCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="计费方式" width="120">
        <template #default="scope">
          <el-tag>{{ promotionMethodLabel(scope.row.promotionMethod) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="预算" width="120">
        <template #default="scope">
          {{ amount(scope.row.budgetAmount) }}
        </template>
      </el-table-column>
      <el-table-column label="奖励" min-width="180">
        <template #default="scope">
          {{ scope.row.rewardDesc || formatReward(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="奖励会员" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.memberRewardEnabled ? 'success' : 'info'" effect="plain">
            {{ scope.row.memberRewardEnabled ? '开启' : '关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'">
            {{ scope.row.status === 1 ? '启用' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="有效期" min-width="220">
        <template #default="scope">
          {{ formatWindow(scope.row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button link type="primary" @click="openForm(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </ContentWrap>

  <el-dialog v-model="dialogVisible" :title="formData.id ? '编辑任务' : '新建任务'" width="820px">
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-row :gutter="16">
        <el-col :xs="24" :sm="12">
          <el-form-item label="任务名称" prop="taskName">
            <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="平台编码" prop="platformCode">
            <el-input v-model="formData.platformCode" placeholder="taobao / jd / pdd" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="计费方式" prop="promotionMethod">
            <el-select v-model="formData.promotionMethod" class="w-full">
              <el-option
                v-for="item in CPX_PROMOTION_METHOD_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="任务类型">
            <el-input v-model="formData.taskType" placeholder="选填" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="offer 类型">
            <el-input v-model="formData.offerType" placeholder="选填" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="状态">
            <el-radio-group v-model="formData.status">
              <el-radio :label="0">草稿</el-radio>
              <el-radio :label="1">启用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="标题">
            <el-input v-model="formData.title" placeholder="展示标题" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="短描述">
            <el-input v-model="formData.shortDesc" maxlength="120" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="奖励文案">
            <el-input v-model="formData.rewardDesc" maxlength="160" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="总预算">
            <el-input-number v-model="formData.budgetAmount" :min="0" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="日预算">
            <el-input-number v-model="formData.dailyBudgetAmount" :min="0" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="奖励金额">
            <el-input-number v-model="formData.rewardAmount" :min="0" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="奖励比例(‰)">
            <el-input-number v-model="formData.rewardRate" :min="0" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="去重窗口(s)">
            <el-input-number v-model="formData.dedupeWindowSeconds" :min="0" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="8">
          <el-form-item label="频控">
            <el-input-number v-model="formData.frequencyLimit" :min="0" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="会员奖励">
            <el-switch v-model="formData.memberRewardEnabled" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="12">
          <el-form-item label="优先级">
            <el-input-number v-model="formData.priority" :min="0" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="落地页">
            <el-input v-model="formData.landingUrl" placeholder="https://..." />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="标签">
            <el-input v-model="formData.tags" placeholder="多个标签用逗号分隔" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="素材 JSON">
            <el-input v-model="formData.materialJson" type="textarea" :rows="3" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="规则 JSON">
            <el-input v-model="formData.ruleJson" type="textarea" :rows="3" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注">
            <el-input v-model="formData.remark" type="textarea" :rows="2" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { CpxTaskApi, CPX_PROMOTION_METHOD_OPTIONS, type CpxPromotionMethod, type CpxTaskSaveVO, type CpxTaskVO } from '@/api/cpx/task'
import { ElMessage } from 'element-plus'

defineOptions({ name: 'CpxTaskIndex' })

const loading = ref(false)
const submitLoading = ref(false)
const list = ref<CpxTaskVO[]>([])
const dialogVisible = ref(false)
const formRef = ref()

const queryParams = reactive({
  keyword: '',
  platformCode: '',
  promotionMethod: '' as CpxPromotionMethod | '',
  status: undefined as number | undefined,
  limit: 100
})

const formData = reactive<Partial<CpxTaskSaveVO>>({
  status: 0,
  memberRewardEnabled: false,
  budgetAmount: 0,
  dailyBudgetAmount: 0,
  rewardAmount: 0,
  rewardRate: 0,
  dedupeWindowSeconds: 0,
  frequencyLimit: 0,
  priority: 20,
  promotionMethod: 'CPS'
})

const formRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  platformCode: [{ required: true, message: '请输入平台编码', trigger: 'blur' }],
  promotionMethod: [{ required: true, message: '请选择计费方式', trigger: 'change' }]
}

const getList = async () => {
  loading.value = true
  try {
    const data = await CpxTaskApi.listTasks({
      keyword: queryParams.keyword,
      promotionMethod: queryParams.promotionMethod || undefined,
      limit: queryParams.limit
    })
    list.value = data.filter((item: CpxTaskVO) => matchQuery(item))
  } finally {
    loading.value = false
  }
}

const matchQuery = (item: CpxTaskVO) => {
  if (queryParams.keyword) {
    const keyword = queryParams.keyword.toLowerCase()
    const haystack = [item.taskName, item.title, item.shortDesc, item.tags].filter(Boolean).join(' ').toLowerCase()
    if (!haystack.includes(keyword)) {
      return false
    }
  }
  if (queryParams.platformCode && item.platformCode !== queryParams.platformCode) {
    return false
  }
  if (queryParams.promotionMethod && item.promotionMethod !== queryParams.promotionMethod) {
    return false
  }
  if (queryParams.status !== undefined && item.status !== queryParams.status) {
    return false
  }
  return true
}

const handleQuery = () => {
  getList()
}

const resetQuery = () => {
  queryParams.keyword = ''
  queryParams.platformCode = ''
  queryParams.promotionMethod = ''
  queryParams.status = undefined
  getList()
}

const openForm = (row?: CpxTaskVO) => {
  if (row) {
    Object.assign(formData, row)
  } else {
    Object.assign(formData, {
      id: undefined,
      taskName: '',
      platformCode: '',
      promotionMethod: 'CPS',
      taskType: '',
      offerType: '',
      title: '',
      shortDesc: '',
      rewardDesc: '',
      budgetAmount: 0,
      dailyBudgetAmount: 0,
      rewardAmount: 0,
      rewardRate: 0,
      memberRewardEnabled: false,
      dedupeWindowSeconds: 0,
      frequencyLimit: 0,
      status: 0,
      priority: 20,
      tags: '',
      materialJson: '',
      ruleJson: '',
      landingUrl: '',
      remark: ''
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  submitLoading.value = true
  try {
    if (formData.id) {
      await CpxTaskApi.updateTask(formData as CpxTaskSaveVO)
      ElMessage.success('任务已更新')
    } else {
      await CpxTaskApi.createTask(formData as CpxTaskSaveVO)
      ElMessage.success('任务已创建')
    }
    dialogVisible.value = false
    getList()
  } finally {
    submitLoading.value = false
  }
}

const amount = (value?: number) => value == null ? '-' : `¥${value}`
const promotionMethodLabel = (value?: string) =>
  CPX_PROMOTION_METHOD_OPTIONS.find((item) => item.value === value)?.label ?? value ?? '-'

const formatReward = (row: CpxTaskVO) => {
  const parts: string[] = []
  if (row.rewardAmount) parts.push(`¥${row.rewardAmount}`)
  if (row.rewardRate) parts.push(`${row.rewardRate}‰`)
  return parts.length ? parts.join(' / ') : '-'
}

const formatWindow = (row: CpxTaskVO) => {
  const start = row.startTime ? String(row.startTime).slice(0, 16).replace('T', ' ') : '不限'
  const end = row.endTime ? String(row.endTime).slice(0, 16).replace('T', ' ') : '不限'
  return `${start} ~ ${end}`
}

onMounted(getList)
</script>
