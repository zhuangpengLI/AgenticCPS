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
        <el-select
          v-model="queryParams.platformCode"
          placeholder="请选择平台"
          clearable
          class="!w-160px"
        >
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="推广位名称" prop="adzoneName">
        <el-input
          v-model="queryParams.adzoneName"
          placeholder="请输入推广位名称"
          clearable
          class="!w-180px"
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
        <el-button @click="handleQuery">
          <Icon icon="ep:search" class="mr-5px" /> 搜索
        </el-button>
        <el-button @click="resetQuery">
          <Icon icon="ep:refresh" class="mr-5px" /> 重置
        </el-button>
        <el-button type="primary" @click="openForm()" v-hasPermi="['cps:adzone:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增推广位
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="平台" align="center" prop="platformCode" width="100">
        <template #default="scope">
          <el-tag :type="platformTagType(scope.row.platformCode)" size="small">
            {{ platformLabel(scope.row.platformCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="推广位ID" align="left" prop="adzoneId" min-width="180" show-overflow-tooltip />
      <el-table-column label="推广位名称" align="center" prop="adzoneName" width="140" show-overflow-tooltip>
        <template #default="scope">{{ scope.row.adzoneName || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" align="center" prop="adzoneType" width="100">
        <template #default="scope">{{ scope.row.adzoneType || '-' }}</template>
      </el-table-column>
      <el-table-column label="是否默认" align="center" prop="isDefault" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.isDefault === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.isDefault === 1 ? '默认' : '非默认' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template #default="scope">{{ formatDate(scope.row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" fixed="right" width="150">
        <template #default="scope">
          <el-button
            link
            type="primary"
            @click="openForm(scope.row)"
            v-hasPermi="['cps:adzone:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['cps:adzone:delete']"
          >
            删除
          </el-button>
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
    :title="formData.id ? '编辑推广位' : '新增推广位'"
    width="500px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-form-item label="平台" prop="platformCode">
        <el-select v-model="formData.platformCode" placeholder="请选择平台" class="w-full">
          <el-option label="淘宝" value="taobao" />
          <el-option label="京东" value="jd" />
          <el-option label="拼多多" value="pdd" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="推广位ID" prop="adzoneId">
        <el-input v-model="formData.adzoneId" placeholder="请输入推广位ID（PID）" />
      </el-form-item>
      <el-form-item label="推广位名称" prop="adzoneName">
        <el-input v-model="formData.adzoneName" placeholder="请输入推广位名称（可选）" />
      </el-form-item>
      <el-form-item label="类型" prop="adzoneType">
        <el-input v-model="formData.adzoneType" placeholder="如: general / member（可选）" />
      </el-form-item>
      <el-form-item label="是否默认" prop="isDefault">
        <el-switch
          v-model="formData.isDefault"
          :active-value="1"
          :inactive-value="0"
          active-text="默认推广位"
          inactive-text="非默认"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio :label="1">启用</el-radio>
          <el-radio :label="0">禁用</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleSubmit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  CpsAdzoneApi,
  type CpsAdzoneVO,
  type CpsAdzoneSaveVO,
  type CpsAdzonePageReqVO
} from '@/api/cps/adzone'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'CpsAdzone' })

const loading = ref(false)
const list = ref<CpsAdzoneVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const formLoading = ref(false)

const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive<CpsAdzonePageReqVO>({
  pageNo: 1,
  pageSize: 10,
  platformCode: undefined,
  adzoneName: undefined,
  status: undefined
})

const defaultFormData = (): CpsAdzoneSaveVO => ({
  id: undefined,
  platformCode: '',
  adzoneId: '',
  adzoneName: undefined,
  adzoneType: undefined,
  relationType: undefined,
  relationId: undefined,
  isDefault: 0,
  status: 1
})

const formData = reactive<CpsAdzoneSaveVO>(defaultFormData())

const formRules = {
  platformCode: [{ required: true, message: '平台不能为空', trigger: 'change' }],
  adzoneId: [{ required: true, message: '推广位ID不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}

/** 平台标签 */
const platformTagType = (code: string) => {
  const map: Record<string, string> = { taobao: 'danger', jd: 'primary', pdd: 'warning', douyin: '' }
  return map[code] || 'info'
}
const platformLabel = (code: string) => {
  const map: Record<string, string> = { taobao: '淘宝', jd: '京东', pdd: '拼多多', douyin: '抖音' }
  return map[code] || code
}

/** 查询列表 */
const getList = async () => {
  loading.value = true
  try {
    const data = await CpsAdzoneApi.getAdzonePage(queryParams)
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

/** 打开弹窗 */
const openForm = (row?: CpsAdzoneVO) => {
  Object.assign(formData, defaultFormData())
  if (row) {
    Object.assign(formData, row)
  }
  dialogVisible.value = true
}

/** 提交 */
const handleSubmit = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formData.id) {
      await CpsAdzoneApi.updateAdzone(formData)
      ElMessage.success('更新成功')
    } else {
      await CpsAdzoneApi.createAdzone(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该推广位吗？', '提示', { type: 'warning' })
  await CpsAdzoneApi.deleteAdzone(id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(getList)
</script>
