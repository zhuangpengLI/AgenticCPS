<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="80px"
    >
      <el-form-item label="平台" prop="platformCode">
        <el-select v-model="queryParams.platformCode" placeholder="请选择平台" filterable clearable class="!w-180px">
          <el-option
            v-for="platform in platformOptions"
            :key="platform.platformCode"
            :label="`${platform.platformName}（${platform.platformCode}）`"
            :value="platform.platformCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable class="!w-120px">
          <el-option label="启用" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button @click="handleQuery"><Icon icon="ep:search" class="mr-5px" /> 搜索</el-button>
        <el-button @click="resetQuery"><Icon icon="ep:refresh" class="mr-5px" /> 重置</el-button>
        <el-button type="primary" @click="openForm(undefined)">
          <Icon icon="ep:plus" class="mr-5px" /> 新增
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list">
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
          <el-button link type="primary" @click="openForm(scope.row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <Pagination
      :total="total"
      v-model:page="queryParams.pageNo"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />
  </ContentWrap>

  <!-- 新增/编辑弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    :title="formData.id ? '编辑冻结配置' : '新增冻结配置'"
    width="500px"
    :close-on-click-modal="false"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="平台编码" prop="platformCode">
        <el-select v-model="formData.platformCode" placeholder="留空表示全平台默认配置" filterable clearable class="!w-260px">
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
        <el-input-number v-model="formData.unfreezeDays" :min="1" :max="365" />
        <span class="ml-10px text-gray-500">天（确认收货后自动解冻）</span>
      </el-form-item>
      <el-form-item label="金额下限" prop="minAmountYuan">
        <el-input-number v-model="formData.minAmountYuan" :min="0" :step="0.01" :precision="2" />
        <span class="ml-10px text-gray-500">元</span>
      </el-form-item>
      <el-form-item label="金额上限" prop="maxAmountYuan">
        <el-input-number v-model="formData.maxAmountYuan" :min="(formData.minAmountYuan ?? 0) + 0.01" :step="0.01" :precision="2" />
        <span class="ml-10px text-gray-500">元</span>
        <span class="ml-10px text-gray-500">留空表示无上限，区间左闭右开</span>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  getCpsFreezeConfigPage,
  createCpsFreezeConfig,
  updateCpsFreezeConfig,
  deleteCpsFreezeConfig,
  type CpsFreezeConfigPageReqVO,
  type CpsFreezeConfigSaveVO
} from '@/api/cps/freeze'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CpsPlatformApi, type CpsPlatformVO } from '@/api/cps/platform'

defineOptions({ name: 'CpsFreezeConfig' })

const loading = ref(true)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formLoading = ref(false)
const platformOptions = ref<CpsPlatformVO[]>([])

const queryParams = reactive<CpsFreezeConfigPageReqVO>({
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

const formData = reactive<FreezeConfigFormData>({
  id: undefined,
  platformCode: undefined,
  minAmountYuan: 10,
  maxAmountYuan: undefined,
  unfreezeDays: 7,
  status: 1,
  remark: undefined
})

const formRules = {
  unfreezeDays: [{ required: true, message: '解冻天数不能为空', trigger: 'blur' }],
  minAmountYuan: [{ required: true, message: '金额下限不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

const queryFormRef = ref()
const formRef = ref()

const getList = async () => {
  loading.value = true
  try {
    const data = await getCpsFreezeConfigPage(queryParams)
    list.value = data.list
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  queryParams.pageNo = 1
  getList()
}

const resetQuery = () => {
  queryFormRef.value?.resetFields()
  handleQuery()
}

const openForm = (row: any) => {
  if (row) {
    Object.assign(formData, {
      ...row,
      platformCode: row.platformCode || '',
      minAmountYuan: row.minAmountCent == null ? 0 : row.minAmountCent / 100,
      maxAmountYuan: row.maxAmountCent == null ? undefined : row.maxAmountCent / 100
    })
  } else {
    Object.assign(formData, { id: undefined, platformCode: '', minAmountYuan: 10, maxAmountYuan: undefined, unfreezeDays: 7, status: 1, remark: undefined })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  const payload: CpsFreezeConfigSaveVO = {
    id: formData.id,
    platformCode: formData.platformCode || undefined,
    minAmountCent: Math.round((formData.minAmountYuan ?? 0) * 100),
    maxAmountCent: formData.maxAmountYuan == null ? undefined : Math.round(formData.maxAmountYuan * 100),
    unfreezeDays: formData.unfreezeDays,
    status: formData.status,
    remark: formData.remark
  }
  formLoading.value = true
  try {
    if (formData.id) {
      await updateCpsFreezeConfig(payload)
      ElMessage.success('更新成功')
    } else {
      await createCpsFreezeConfig(payload)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该冻结配置吗？', '提示', { type: 'warning' })
  await deleteCpsFreezeConfig(id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(() => {
  getList()
  CpsPlatformApi.getEnabledPlatformList().then((data) => {
    platformOptions.value = data || []
  }).catch(() => {
    platformOptions.value = []
  })
})
</script>
