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
        <el-input
          v-model="queryParams.platformCode"
          placeholder="请输入平台编码"
          clearable
          class="!w-160px"
          @keyup.enter="handleQuery"
        />
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
        <el-input v-model="formData.platformCode" placeholder="留空表示全平台默认配置" clearable />
      </el-form-item>
      <el-form-item label="解冻天数" prop="unfreezeDays">
        <el-input-number v-model="formData.unfreezeDays" :min="1" :max="365" />
        <span class="ml-10px text-gray-500">天（确认收货后自动解冻）</span>
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

defineOptions({ name: 'CpsFreezeConfig' })

const loading = ref(true)
const list = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const formLoading = ref(false)

const queryParams = reactive<CpsFreezeConfigPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  status: undefined
})

const formData = reactive<CpsFreezeConfigSaveVO>({
  id: undefined,
  platformCode: undefined,
  unfreezeDays: 7,
  status: 1,
  remark: undefined
})

const formRules = {
  unfreezeDays: [{ required: true, message: '解冻天数不能为空', trigger: 'blur' }],
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
    Object.assign(formData, row)
  } else {
    Object.assign(formData, { id: undefined, platformCode: undefined, unfreezeDays: 7, status: 1, remark: undefined })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formData.id) {
      await updateCpsFreezeConfig(formData)
      ElMessage.success('更新成功')
    } else {
      await createCpsFreezeConfig(formData)
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
})
</script>
