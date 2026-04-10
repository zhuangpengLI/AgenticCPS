<template>
  <ContentWrap>
    <!-- 搜索栏 -->
    <el-form
      class="-mb-15px"
      :model="queryParams"
      ref="queryFormRef"
      :inline="true"
      label-width="100px"
    >
      <el-form-item label="供应商名称" prop="vendorName">
        <el-input
          v-model="queryParams.vendorName"
          placeholder="请输入供应商名称"
          clearable
          class="!w-180px"
          @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item label="供应商类型" prop="vendorType">
        <el-select v-model="queryParams.vendorType" placeholder="全部类型" clearable class="!w-140px">
          <el-option
            v-for="item in VENDOR_TYPE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="所属平台" prop="platformCode">
        <el-select v-model="queryParams.platformCode" placeholder="全部平台" clearable class="!w-160px">
          <el-option
            v-for="item in PLATFORM_CODE_OPTIONS"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="!w-120px">
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
        <el-button type="primary" @click="openForm()" v-hasPermi="['cps:api-vendor:create']">
          <Icon icon="ep:plus" class="mr-5px" /> 新增供应商
        </el-button>
      </el-form-item>
    </el-form>
  </ContentWrap>

  <!-- 列表 -->
  <ContentWrap>
    <el-table v-loading="loading" :data="list" stripe>
      <el-table-column label="ID" align="center" prop="id" width="70" />
      <el-table-column label="供应商编码" align="center" prop="vendorCode" width="120" />
      <el-table-column label="供应商名称" align="center" prop="vendorName" width="120" />
      <el-table-column label="供应商类型" align="center" width="110">
        <template #default="scope">
          <el-tag :type="scope.row.vendorType === 'aggregator' ? 'primary' : 'success'" size="small">
            {{ vendorTypeLabel(scope.row.vendorType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="所属平台" align="center" width="120">
        <template #default="scope">
          <el-tag type="info" size="small">
            {{ platformLabel(scope.row.platformCode) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AppKey" align="center" width="140">
        <template #default="scope">
          <span class="text-gray-500">{{ maskSecret(scope.row.appKey) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="API基础地址" align="center" prop="apiBaseUrl" min-width="200" show-overflow-tooltip />
      <el-table-column label="推广位ID" align="center" prop="defaultAdzoneId" min-width="140" show-overflow-tooltip />
      <el-table-column label="优先级" align="center" prop="priority" width="80" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template #default="scope">
          <el-switch
            v-model="scope.row.status"
            :active-value="1"
            :inactive-value="0"
            @change="handleStatusChange(scope.row)"
            v-hasPermi="['cps:api-vendor:update']"
          />
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
            v-hasPermi="['cps:api-vendor:update']"
          >
            编辑
          </el-button>
          <el-button
            link
            type="danger"
            @click="handleDelete(scope.row.id)"
            v-hasPermi="['cps:api-vendor:delete']"
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
    :title="formData.id ? '编辑供应商配置' : '新增供应商配置'"
    width="650px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="供应商编码" prop="vendorCode">
            <el-select
              v-model="formData.vendorCode"
              placeholder="请选择供应商"
              :disabled="!!formData.id"
              class="w-full"
              @change="handleVendorCodeChange"
            >
              <el-option
                v-for="item in filteredVendorOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="供应商名称" prop="vendorName">
            <el-input v-model="formData.vendorName" placeholder="如: 大淘客" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="供应商类型" prop="vendorType">
            <el-select v-model="formData.vendorType" placeholder="请选择类型" class="w-full" disabled>
              <el-option
                v-for="item in VENDOR_TYPE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="所属平台" prop="platformCode">
            <el-select v-model="formData.platformCode" placeholder="请选择平台" class="w-full">
              <el-option
                v-for="item in PLATFORM_CODE_OPTIONS"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="AppKey" prop="appKey">
            <el-input v-model="formData.appKey" placeholder="请输入 AppKey" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="AppSecret" prop="appSecret">
            <el-input
              v-model="formData.appSecret"
              type="password"
              show-password
              :placeholder="formData.id ? '留空表示不修改' : '请输入 AppSecret'"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="API基础地址" prop="apiBaseUrl">
            <el-input v-model="formData.apiBaseUrl" placeholder="如: https://openapi.dataoke.com/api" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="授权令牌" prop="authToken">
            <el-input
              v-model="formData.authToken"
              type="password"
              show-password
              placeholder="请输入授权令牌（可选）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="默认推广位ID" prop="defaultAdzoneId">
            <el-input v-model="formData.defaultAdzoneId" placeholder="如: mm_xxx_xxx_xxx（可选）" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="优先级" prop="priority">
            <el-input-number v-model="formData.priority" :min="0" :max="9999" class="w-full" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="formData.status">
              <el-radio :label="1">启用</el-radio>
              <el-radio :label="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="扩展配置" prop="extraConfig">
            <el-input
              v-model="formData.extraConfig"
              type="textarea"
              :rows="3"
              placeholder="JSON 格式扩展配置（可选）"
            />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注" prop="remark">
            <el-input
              v-model="formData.remark"
              type="textarea"
              :rows="2"
              placeholder="请输入备注（可选）"
            />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button type="primary" :loading="formLoading" @click="handleSubmit">确 定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import {
  CpsApiVendorApi,
  VENDOR_TYPE_OPTIONS,
  VENDOR_CODE_OPTIONS,
  PLATFORM_CODE_OPTIONS,
  type CpsApiVendorVO,
  type CpsApiVendorSaveVO,
  type CpsApiVendorPageReqVO
} from '@/api/cps/apiVendor'
import { formatDate } from '@/utils/formatTime'
import { ElMessage, ElMessageBox } from 'element-plus'

defineOptions({ name: 'CpsApiVendor' })

const loading = ref(false)
const list = ref<CpsApiVendorVO[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const formLoading = ref(false)

const queryFormRef = ref()
const formRef = ref()

const queryParams = reactive<CpsApiVendorPageReqVO>({
  pageNo: 1,
  pageSize: 10,
  vendorCode: undefined,
  vendorName: undefined,
  vendorType: undefined,
  platformCode: undefined,
  status: undefined
})

const defaultFormData = (): CpsApiVendorSaveVO => ({
  id: undefined,
  vendorCode: '',
  vendorName: '',
  vendorType: '',
  platformCode: '',
  appKey: '',
  appSecret: '',
  apiBaseUrl: '',
  authToken: undefined,
  defaultAdzoneId: undefined,
  extraConfig: undefined,
  priority: 0,
  status: 1,
  remark: undefined
})

const formData = reactive<CpsApiVendorSaveVO>(defaultFormData())

const formRules = computed(() => ({
  vendorCode: [{ required: true, message: '供应商编码不能为空', trigger: 'change' }],
  vendorName: [{ required: true, message: '供应商名称不能为空', trigger: 'blur' }],
  vendorType: [{ required: true, message: '供应商类型不能为空', trigger: 'change' }],
  platformCode: [{ required: true, message: '所属平台不能为空', trigger: 'change' }],
  appKey: [{ required: true, message: 'AppKey 不能为空', trigger: 'blur' }],
  appSecret: formData.id
    ? []
    : [{ required: true, message: 'AppSecret 不能为空', trigger: 'blur' }],
  apiBaseUrl: [{ required: true, message: 'API基础地址不能为空', trigger: 'blur' }],
  status: [{ required: true, message: '状态不能为空', trigger: 'change' }]
}))

/** 根据供应商类型过滤供应商编码选项 */
const filteredVendorOptions = computed(() => {
  if (!formData.vendorType) return VENDOR_CODE_OPTIONS
  return VENDOR_CODE_OPTIONS.filter((item) => item.type === formData.vendorType)
})

/** 供应商编码变更时自动填充名称和类型 */
const handleVendorCodeChange = (code: string) => {
  const vendor = VENDOR_CODE_OPTIONS.find((item) => item.value === code)
  if (vendor) {
    formData.vendorName = vendor.label
    formData.vendorType = vendor.type
  }
}

/** 供应商类型文本 */
const vendorTypeLabel = (type?: string) => {
  return VENDOR_TYPE_OPTIONS.find((item) => item.value === type)?.label ?? type ?? '-'
}

/** 平台名称文本 */
const platformLabel = (code?: string) => {
  return PLATFORM_CODE_OPTIONS.find((item) => item.value === code)?.label ?? code ?? '-'
}

/** 脱敏显示密钥 */
const maskSecret = (val?: string) => {
  if (!val) return '-'
  if (val.length <= 8) return '****'
  return val.substring(0, 4) + '****' + val.substring(val.length - 4)
}

// ===== 列表操作 =====

const getList = async () => {
  loading.value = true
  try {
    const data = await CpsApiVendorApi.getVendorPage(queryParams)
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

// ===== 表单操作 =====

const openForm = (row?: CpsApiVendorVO) => {
  Object.assign(formData, defaultFormData())
  if (row) {
    Object.assign(formData, {
      id: row.id,
      vendorCode: row.vendorCode,
      vendorName: row.vendorName,
      vendorType: row.vendorType,
      platformCode: row.platformCode,
      appKey: row.appKey,
      appSecret: '', // 编辑时不回显 Secret
      apiBaseUrl: row.apiBaseUrl,
      authToken: row.authToken,
      defaultAdzoneId: row.defaultAdzoneId,
      extraConfig: row.extraConfig,
      priority: row.priority,
      status: row.status,
      remark: row.remark
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  await formRef.value?.validate()
  formLoading.value = true
  try {
    if (formData.id) {
      await CpsApiVendorApi.updateVendor(formData)
      ElMessage.success('更新成功')
    } else {
      await CpsApiVendorApi.createVendor(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    getList()
  } finally {
    formLoading.value = false
  }
}

/** 状态快速切换 */
const handleStatusChange = async (row: CpsApiVendorVO) => {
  const text = row.status === 1 ? '启用' : '禁用'
  try {
    await CpsApiVendorApi.updateVendor({
      id: row.id,
      vendorCode: row.vendorCode,
      vendorName: row.vendorName,
      vendorType: row.vendorType,
      platformCode: row.platformCode,
      appKey: row.appKey ?? '',
      appSecret: '',
      apiBaseUrl: row.apiBaseUrl ?? '',
      status: row.status
    })
    ElMessage.success(`已${text}供应商：${row.vendorName}`)
  } catch {
    // 还原状态
    row.status = row.status === 1 ? 0 : 1
  }
}

/** 删除 */
const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该供应商配置吗？删除后不可恢复！', '警告', { type: 'warning' })
  await CpsApiVendorApi.deleteVendor(id)
  ElMessage.success('删除成功')
  getList()
}

onMounted(getList)
</script>
